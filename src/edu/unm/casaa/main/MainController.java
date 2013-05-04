/*
This source code file is part of the CASAA Treatment Coding System Utility
    Copyright (C) 2009  UNM CASAA

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.unm.casaa.main;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Date;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import edu.unm.casaa.globals.GlobalCode;
import edu.unm.casaa.globals.GlobalTemplateUiService;
import edu.unm.casaa.globals.GlobalTemplateView;
import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.misc.MiscTemplateUiService;
import edu.unm.casaa.misc.MiscTemplateView;
import edu.unm.casaa.utterance.Utterance;
import edu.unm.casaa.utterance.UtteranceList;

/*
 * IMPROVE:
 * Interface:
 *  - We should label the current mode (parse, code, globals).  This is implicitly illustrated
 *    by the set of UI elements displayed (parse buttons, code buttons, etc).  But it would be
 *    more user-friendly to explicitly label the mode we're in.
 *  - We should label the file (e.g. foo.parse, foo.casaa) we're editing.
 *    - Audio file (all modes).
 *    - Parse file (parse and code modes).
 *    - Code file (code mode only).
 *    - Globals file (globals mode only). 
 */

/*
 Design notes:
 - Save to file whenever we modify data (e.g. utterances), unless that modification leaves
 data in an incomplete state (e.g. parse started, but not yet ended).
 */

public class MainController implements BasicPlayerListener {

	// ====================================================================
	// Fields
	// ====================================================================

    enum Mode {
        PLAYBACK,   // Play audio file.
        CODE,       // Parse and code an audio file.
        GLOBALS     // Assign overall ratings to an audio file.
    };

    public static MainController instance                 = null;             // Singleton.

	// GUI

    private ActionTable          actionTable              = null;             // Communication between GUI and MainController.

    private OptionsWindow        optionsWindow            = null;

    private PlayerView           playerView               = null;
    private JPanel               templateView             = null;
    private TemplateUiService    templateUI               = null;

    private String               filenameMisc             = null;				// CASAA file.
    private String               filenameGlobals          = null;

    private String               globalsLabel             = "Global Ratings";   // Label for global template view.

    // Audio Player back-end
    private BasicPlayer          player                   = new BasicPlayer();
    private String               playerStatus             = "";
    private int                  bytesPerSecond           = 0;                // Cached when we load audio file.

    private String               filenameAudio            = null;

    private UtteranceList        utteranceList            = null;

    // TODO - Can we cut currentUtterance?  I believe we only ever operate on
    // last element in utterance list, so we should be able to implicitly
    // define it as current.
    private int                  currentUtterance         = 0;                // Index of utterance selected for parse or code. May be equal to list size, in
                                                                               // which case current utterance is null.

    private int                  numSaves                 = 0;                // Number of times we've saved since loading current session data.
    private int                  numUninterruptedUncodes  = 0;                // Number of times user has uncoded without doing anything else.

    // The following variables are used for thread-safe handling of player callbacks.
    private boolean              progressReported         = false;            // If true, player thread has called progress(), and we will apply change in
                                                                               // run().

    private int                  statusChangeEventIndex   = 0;                // Track highest player event index that changed our displayed status, since
                                                                               // events can be reported out of order.

	// ====================================================================
	// Main
	// ====================================================================

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * @param args
	 */
	public static void main( String[] args ) {
	    // Create and show splash screen.
	    SplashWindow   splash      = new SplashWindow();
	    Date           date        = new Date();
	    long           startTime   = date.getTime();

	    splash.setVisible( true );

	    // Initialize main controller.
	    MainController.instance = new MainController();
		MainController.instance.init();

		// Delay long enough to ensure splash screen is visible.
		long  elapsed = date.getTime() - startTime;

		try {
		    Thread.sleep( 1000 - elapsed );
		} catch( Exception e ) {
		}

		// Hide splash screen, show main controller.
		splash.setVisible( false );
		MainController.instance.show();
		MainController.instance.run();
	}

	// ====================================================================
	// Constructor and Initialization Methods
	// ====================================================================

	public MainController() {
	}

    public void init() {
        PlayerView.setLookAndFeel(); // Set look and feel first, so any warning dialogs triggered during initialization look right.
        parseUserConfig();
        playerView = new PlayerView();
        player.addBasicPlayerListener( this );
        registerPlayerViewListeners();
        // Handle window closing events.
        playerView.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                actionExit();
            }
        } );

        // Start in playback mode, with no audio file loaded.
        setMode( Mode.PLAYBACK );
    }

    public void show() {
        playerView.setVisible( true );
    }

    // ====================================================================
	// MainController interface
	// ====================================================================

	// Run update loop.
	public void run() {
		while (true) {
			try {
                // Thread-safety: Check for and handle player progress and EOM notifications.
                // Because these callbacks come from a separate thread (i.e. the player thread),
                // and handling them may call player methods and/or modify our state, we need to
                // apply them here from our main thread. We also need to make sure any of our methods
                // which are called from the GUI thread, and which can manipulate our internal state
                // (i.e. most of our methods) are synchronized to ensure thread-safety with the methods
                // we call here.
                if( progressReported )
                    applyPlayerProgress();

                Thread.sleep( 100 );
            } catch( InterruptedException e ) {
                e.printStackTrace();
			}
		}
	}

	// Callbacks for GUI actions.
    public void handleAction( String action ) {
        if( "parseStart".equals( action ) ) {
            parseStart();
        } else if( "play".equals( action ) ) {
            handleActionPlay();
        } else if( "replay".equals( action ) ) {
            handleActionReplay();
        } else if( "uncode".equals( action ) ) {
            handleActionUncode();
        } else if( "uncodeAndReplay".equals( action ) ) {
            handleActionUncodeAndReplay();
        } else if( "rewind5s".equals( action ) ) {
            handleActionRewind5s();
        }
    }

	// Callback when global data changes.
	public void globalDataChanged() {
        assert (templateView instanceof GlobalTemplateView);
		saveSession();
	}

	public ActionTable getActionTable() {
        if( actionTable == null ) {
            actionTable = new ActionTable();
            mapAction( "Start", "parseStart" );
            mapAction( "Play/Pause", "play" );
            mapAction( "Replay", "replay" );
            mapAction( "Uncode", "uncode" );
            mapAction( "Uncode & Replay", "uncodeAndReplay" );
            mapAction( "Rewind 5s", "rewind5s" );
        }
        return actionTable;
	}

	// Access utterances.
	public int numUtterances() {
		return getUtteranceList().size();
	}

    public Utterance utterance( int index ) {
        return getUtteranceList().get( index );
    }

	private void utteranceListChanged() {
		playerView.getTimeline().repaint();
	}

	// Get audio bytes per second.
	public int getBytesPerSecond() {
		return bytesPerSecond;
	}

	// Get audio file length, in bytes.
	public int getAudioLength() {
		return player.getEncodedLength();
	}

	// Get current utterance. May be null.
    public synchronized Utterance getCurrentUtterance() {
        assert (currentUtterance >= 0);
        if( currentUtterance < getUtteranceList().size() )
            return getUtteranceList().get( currentUtterance );
        else
            return null;
	}

	// Handle errors re: user codes XML file. We must be able to find and parse
	// this file
	// successfully, so all of these errors are fatal.
	public void handleUserCodesParseException(File file, SAXParseException e) {
		// Alert and quit.
		JOptionPane.showMessageDialog(playerView,
				"Parse error in " + file.getAbsolutePath() +
				" (line " + e.getLineNumber() + "):\n" + e.getMessage(),
				"Failed to load user codes", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	public void handleUserCodesGenericException(File file, Exception e) {
		JOptionPane.showMessageDialog(playerView,
				"Unknown error parsing file: " + file.getAbsolutePath() +
				"\n" + e.toString(),
				"Failed to load user codes", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	public void handleUserCodesError(File file, String message) {
		JOptionPane.showMessageDialog(playerView,
				"Error loading file: " + file.getAbsolutePath() +
				"\n" + message,
				"Failed to load user codes", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	public void handleUserCodesMissing(File file) {
		// Alert and quit.
		JOptionPane.showMessageDialog(playerView,
				"Failed to find required file." + file.getAbsolutePath(),
				"Failed to load user codes", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	public void showWarning(String title, String message) {
		JOptionPane.showMessageDialog(playerView,
				message,
				title,
				JOptionPane.WARNING_MESSAGE);		
	}

	public String getGlobalsLabel() {
	    return globalsLabel;
	}

	// ====================================================================
	// Private Helper Methods
	// ====================================================================

	private void actionExit() {
		System.exit(0);
	}

	private void mapAction(String text, String command) {
		actionTable.put(command, new MainControllerAction(text, command));
	}

	private void display(String msg) {
		System.out.println(msg);
	}

	private void displayPlayerException(BasicPlayerException e) {
		display("BasicPlayerException: " + e.getMessage());
		e.printStackTrace();
	}

	// Parse user codes and globals from XML.
	private void parseUserConfig() {
		// NOTE: We display parse errors to user, so user can correct XML file, then quit.
        File file = new File( "userConfiguration.xml" );

        if( file.exists() ) {
            try {
                DocumentBuilderFactory  fact    = DocumentBuilderFactory.newInstance();
                DocumentBuilder         builder = fact.newDocumentBuilder();
                Document                doc     = builder.parse( file.getCanonicalFile() );
                Node                    root    = doc.getDocumentElement();

                // Expected format: <userConfiguration> <codes>...</codes> <globals>...</globals> </userConfiguration>
                for( Node node = root.getFirstChild(); node != null; node = node.getNextSibling() ) {

                    if( node.getNodeName().equalsIgnoreCase( "codes" ) )
                        parseUserCodes( file, node );
                    else if( node.getNodeName().equalsIgnoreCase( "globals" ) )
                        parseUserGlobals( file, node );
                    else if( node.getNodeName().equalsIgnoreCase( "globalsBorder" ) )
                        parseUserGlobalsBorder( file, node );
                }
            } catch( SAXParseException e ) {
                handleUserCodesParseException( file, e );
            } catch( Exception e ) {
                handleUserCodesGenericException( file, e );
            }
        } else {
            handleUserCodesMissing( file );
		}
	}

    // Parse codes from given <codes> tag.
    private void parseUserCodes( File file, Node codes ) {
        for( Node n = codes.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "code" ) ) {
                NamedNodeMap    map         = n.getAttributes();
                Node            nodeValue   = map.getNamedItem( "value" );
                int             value       = Integer.parseInt( nodeValue.getTextContent() );
                String          name        = map.getNamedItem( "name" ).getTextContent();

                if( !MiscCode.addCode( new MiscCode( value, name ) ) )
                    handleUserCodesError( file, "Failed to add code." );
            }
        }
    }

    // Parse globals from given <globals> tag.
    private void parseUserGlobals( File file, Node globals ) {
        for( Node n = globals.getFirstChild(); n != null; n = n.getNextSibling() ) {
            if( n.getNodeName().equalsIgnoreCase( "global" ) ) {
                NamedNodeMap    map         = n.getAttributes();
                Node            nodeValue   = map.getNamedItem( "value" );
                int             value       = Integer.parseInt( nodeValue.getTextContent() );
                Node            nodeDefaultRating   = map.getNamedItem( "defaultRating" );
                Node            nodeMinRating       = map.getNamedItem( "minRating" );
                Node            nodeMaxRating       = map.getNamedItem( "maxRating" );
                String          name        = map.getNamedItem( "name" ).getTextContent();
                String          label       = map.getNamedItem( "label" ).getTextContent();
                GlobalCode      code        = new GlobalCode( value, name, label );

                if( nodeDefaultRating != null )
                    code.defaultRating = Integer.parseInt( nodeDefaultRating.getTextContent() );
                if( nodeMinRating != null )
                    code.minRating = Integer.parseInt( nodeMinRating.getTextContent() );
                if( nodeMaxRating != null )
                    code.maxRating = Integer.parseInt( nodeMaxRating.getTextContent() );

                if( code.defaultRating < code.minRating ||
                    code.defaultRating > code.maxRating ||
                    code.maxRating < code.minRating ) {
                    handleUserCodesError( file, "Invalid range for global code: " + code.name +
                                          ", minRating: " + code.minRating +
                                          ", maxRating: " + code.maxRating +
                                          ", defaultRating: " + code.defaultRating );
                }

                if( !GlobalCode.addCode( code ) )
                    handleUserCodesError( file, "Failed to add global code." );
            }
        }
    }

    // Parse globalsLabel from given <globalsBorder> tag.
    private void parseUserGlobalsBorder( File file, Node node ) {
        NamedNodeMap    map = node.getAttributes();

        globalsLabel = map.getNamedItem( "label" ).getTextContent();
    }

    // Get final utterance in list, or null if list is empty.
	private synchronized Utterance getLastUtterance() {
		return getUtteranceList().last();
	}

	// Get next utterance, or null if no next utterance exists.
	private synchronized Utterance getNextUtterance() {
		if (currentUtterance + 1 < getUtteranceList().size()) {
			return getUtteranceList().get(currentUtterance + 1);
		} else {
			return null;
		}
	}

	// Get previous utterance, or null if no previous utterance exists.
	private synchronized Utterance getPreviousUtterance() {
		if (currentUtterance > 0) {
			return getUtteranceList().get(currentUtterance - 1);
		} else {
			return null;
		}
	}

	// Seek player as close as possible to requested bytes. Updates slider and
	// time display.
	private synchronized void playerSeek(int bytes) {
		try {
			player.seek(bytes);
		} catch (BasicPlayerException e) {
			showAudioFileNotSeekableDialog();
			displayPlayerException(e);
		}

		// Set player volume and pan according to sliders, after player line is initialized.
		getOptionsWindow().applyAudioOptions();

		// Update time and seek slider displays.
		updateTimeDisplay();
		updateSeekSliderDisplay();
	}

	// Seek player to position defined by slider. Updates time display, but not
	// slider
	// (as that would create a feedback cycle).
	private synchronized void playerSeekToSlider() {
		if (player.getStatus() == BasicPlayer.UNKNOWN) {
			return;
		}
		double t = playerView.getSliderSeek().getValue()
				/ (double) PlayerView.SEEK_MAX_VAL;
		long bytes = (long) (t * player.getEncodedLength());

		try {
			// Stop before seeking, to minimize UI lag.
			player.stop();
			player.seek(bytes);
		} catch (BasicPlayerException e) {
			displayPlayerException(e);
		}

		// Set player volume and pan according to sliders, after player line is initialized.
		getOptionsWindow().applyAudioOptions();

		// Update time display.
		updateTimeDisplay();
	}

	// Pause/resume/stop/play player. These wrappers are here to clean up
	// exception handling.
	private synchronized void playerPause() {
		try {
			player.pause();
		} catch (BasicPlayerException e) {
			displayPlayerException(e);
		}
	}

	private synchronized void playerResume() {
		try {
			player.resume();
		} catch (BasicPlayerException e) {
			displayPlayerException(e);
		}
        getOptionsWindow().applyAudioOptions();
	}

	private synchronized void playerPlay() {
		try {
			player.play();
		} catch (BasicPlayerException e) {
			displayPlayerException(e);
		}
        getOptionsWindow().applyAudioOptions();
	}

	private void cleanupMode() {
        utteranceList       = null;
        currentUtterance    = 0;
        resetUncodeCount();
	}

	// Switch modes. Hides/shows relevant UI.
	// PRE: filenameAudio is set.
    private void setMode( Mode mode ) {
        setTemplateView( mode );

        playerView.getSliderSeek().setEnabled( filenameAudio != null );
        playerView.getButtonPlay().setEnabled( filenameAudio != null );
        playerView.getButtonReplay().setVisible( mode == Mode.CODE );
        playerView.getButtonUncode().setVisible( mode == Mode.CODE );
        playerView.getButtonUncodeAndReplay().setVisible( mode == Mode.CODE );
        playerView.getButtonRewind5s().setEnabled( filenameAudio != null );
        playerView.getTimeline().setVisible( mode == Mode.CODE );

        // Pack window, resizing to match visible interface.
        playerView.pack();

        // If entering GLOBALS mode, ping callback so we'll save the file.
        if( mode == Mode.GLOBALS )
            globalDataChanged();
	}

	// Synchronize both the GUI (slider, time display) and current utterance
	// index with the
	// most recently reported audio playback position, if any.
	private synchronized void applyPlayerProgress() {
		updateTimeDisplay();
		updateSeekSliderDisplay();

		// Handle MISC utterance and player state.
		if (templateView instanceof MiscTemplateView) {
			Utterance	current = getCurrentUtterance();
			int			bytes 	= player.getEncodedStreamPosition();

			// Move to next utterance, if current is coded.
			// TODO - Can this actually happen?  I think we won't ever create a list
			// of utterances that are not all coded, excluding the final utterance.
			if ((current != null) && (bytes > current.getEndBytes()) && current.isCoded())
				currentUtterance++;
		}
		updateUtteranceDisplays();
		progressReported = false; // Clear flag once (current) progress report is applied.
	}

	// Open file chooser to select audio file. On approve, load audio file.
	// Returns true if audio file was successfully opened.
	private boolean selectAndLoadAudioFile() {
		JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle( "Load Audio File" );
        chooser.setFileFilter( new FileNameExtensionFilter( "WAV Audio only for coding", "wav" ) );
        if( chooser.showOpenDialog( playerView ) == JFileChooser.APPROVE_OPTION ) {
            return loadAudioFile( chooser.getSelectedFile().getAbsolutePath() );
        } else {
            return false;
        }
	}

	// Load audio file from given filename. Records filenameAudio.
	// Returns true on success.
	private boolean loadAudioFile(String filename) {
		filenameAudio = filename;
		bytesPerSecond = 0;
		try {
			player.open(new File(filenameAudio));
			bytesPerSecond = player.getBytesPerSecond();
			updateTimeDisplay();
			updateSeekSliderDisplay();
			return true;
		} catch (BasicPlayerException e) {
			showAudioFileNotFoundDialog();
			displayPlayerException(e);
			return false;
		}
	}

	private void registerPlayerViewListeners() {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Seek Slider
		playerView.getSliderSeek().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				// If value is being adjusted by user, apply to player.
				// Else slider has changed due to call-back from player.
				if (playerView.getSliderSeek().getValueIsAdjusting()) {
					playerSeekToSlider();
				}
			}
		});

		// ================================================================
		// Menu Listeners

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// File Menu: Load Audio File
        playerView.getMenuItemLoadAudio().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                if( selectAndLoadAudioFile() ) {
                    setMode( Mode.PLAYBACK );
                }
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // File Menu: Options
        playerView.getMenuItemOptions().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                getOptionsWindow().setVisible( true );
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // File Menu: Exit
        playerView.getMenuItemExit().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                actionExit();
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Code Utterances Menu: Start New Code File
        playerView.getMenuItemNewCode().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                handleNewCodeFile();
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Code Utterances Menu: Load Code File
        playerView.getMenuItemLoadCode().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                handleLoadCodeFile();
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Global Ratings Menu: Score Global Ratings
        playerView.getMenuItemCodeGlobals().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                handleNewGlobalRatings();
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // About Menu: Help
        playerView.getMenuItemHelp().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showHelpDialog();
            }
        } );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // About Menu: About this Application
        playerView.getMenuItemAbout().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                handleAboutWindow();
            }
        } );
    }

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Player Handlers
	private synchronized void handleActionPlay() {
        if( player.getStatus() == BasicPlayer.PLAYING ) {
            playerPause();
        } else if( player.getStatus() == BasicPlayer.PAUSED ) {
            playerResume();
        } else if( player.getStatus() == BasicPlayer.STOPPED ||
                   player.getStatus() == BasicPlayer.OPENED ) {
            playerPlay();
        }
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized void handleActionReplay() {
        if( templateView instanceof MiscTemplateView ) {
            // Seek to beginning of current utterance.  Seek a little further back
            // to ensure audio synchronization issues don't cause player to actually
            // seek later than beginning of utterance.
            Utterance   utterance   = getCurrentUtterance();
            int         pos         = 0;

            if( utterance != null ) {
                pos = utterance.getStartBytes();
                pos -= bytesPerSecond; // Skip back one extra second.
                pos = Math.max( pos, 0 ); // Clamp.
            }
            playerSeek( pos );
        } else {
            showParsingErrorDialog();
        }
        updateTimeDisplay();
        updateSeekSliderDisplay();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized void handleActionUncodeAndReplay() {
		handleActionUncode();
		handleActionReplay();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private synchronized void handleActionUncode() {
        if( templateView instanceof MiscTemplateView ) {
            uncode();
            saveSession();
        	incrementUncodeCount();
        } else {
            showParsingErrorDialog();
        }
    }

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized void handleActionRewind5s() {
		// Rewind playback position 5 seconds, without affecting utterances.
		assert (bytesPerSecond > 0);

		int pos = streamPosition();

		pos -= 5 * bytesPerSecond;
        pos = Math.max( pos, 0 ); // Clamp to beginning of file.

        playerSeek( pos );
        updateUtteranceDisplays();
        updateTimeDisplay();
        updateSeekSliderDisplay();
	}

    private synchronized void incrementUncodeCount() {
        numUninterruptedUncodes++;
        if( numUninterruptedUncodes >= 4 ) {
            showWarning( "Uncode Warning", "You have uncoded 4 times in a row." );
            numUninterruptedUncodes = 0;
        }
    }

    private synchronized void resetUncodeCount() {
        numUninterruptedUncodes = 0;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized void updateSeekSliderDisplay() {
		// Don't set slider position if user is dragging it.
        if( playerView.getSliderSeek().getValueIsAdjusting() )
            return;

		int       position    = player.getEncodedStreamPosition();
		int       length      = player.getEncodedLength();
		double    t           = (length > 0) ? (position / (double) length) : 0;

        if( t >= 1.0 ) {
            playerView.setSliderSeek( PlayerView.SEEK_MAX_VAL );
        } else if( t == 0 ) {
            playerView.setSliderSeek( 0 );
        } else {
            playerView.setSliderSeek( (int) (t * PlayerView.SEEK_MAX_VAL) );
        }
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public synchronized void handleSliderPan( JSlider slider ) {
        if( player.hasPanControl() ) {
            try {
                player.setPan( slider.getValue() / 10.0 );
            } catch( BasicPlayerException e ) {
                displayPlayerException( e );
            }
        }
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public synchronized void handleSliderGain( JSlider slider ) {
        if( player.hasGainControl() ) {
            try {
                player.setGain( slider.getValue() / 100.0 );
            } catch( BasicPlayerException e ) {
                displayPlayerException( e );
            }
        }
    }

	private synchronized OptionsWindow getOptionsWindow() {
	    if( optionsWindow == null )
	        optionsWindow = new OptionsWindow();

	    return optionsWindow;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Menu Handlers
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Show dialog requesting user confirmation for overwriting given file.
	// Returns true if user clicks "OK".
	private synchronized boolean confirmOverwrite( String filename ) {
        int option = JOptionPane.showConfirmDialog( playerView,
                                                    "File '" + filename + "' already exists.  Overwrite?",
                                                    "File Exists",
                                                    JOptionPane.OK_CANCEL_OPTION );

        return option == JOptionPane.OK_OPTION;
	}

	private synchronized void handleNewCodeFile() {
        if( player.getStatus() == BasicPlayer.PLAYING )
            playerPause();

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle( "Name New CASAA File" );
        chooser.setFileFilter( new FileNameExtensionFilter( "CASAA files", "casaa" ) );
        if( chooser.showSaveDialog( playerView ) != JFileChooser.APPROVE_OPTION )
            return; // User canceled.

        // Check if code filename refers to an existing file.  If so, warn and get user confirmation.
        String  newFilename = chooser.getSelectedFile().getAbsolutePath();

        if( new File( newFilename ).exists() && !confirmOverwrite( newFilename ) )
                return; // User canceled.

        if( selectAndLoadAudioFile() ) {
            cleanupMode();
            filenameMisc = newFilename;
            utteranceListChanged();
            setMode( Mode.CODE );
        }
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized void handleNewGlobalRatings() {
        if( player.getStatus() == BasicPlayer.PLAYING )
            playerPause();

		JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle( "Name New Globals File" );
        chooser.setFileFilter( new FileNameExtensionFilter( "GLOBALS files", "global" ) );
        if( chooser.showSaveDialog( playerView ) != JFileChooser.APPROVE_OPTION )
            return; // User canceled.

        // Check if code filename refers to an existing file.  If so, warn and get user confirmation.
        String  newFilename = chooser.getSelectedFile().getAbsolutePath();

        if( new File( newFilename ).exists() && !confirmOverwrite( newFilename ) )
            return; // User canceled.

        if( selectAndLoadAudioFile() ) {
            cleanupMode();
            filenameGlobals = newFilename;
            setMode( Mode.GLOBALS );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized void handleLoadCodeFile() {
        if( player.getStatus() == BasicPlayer.PLAYING )
            playerPause();

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle( "Load CASAA File" );
        chooser.setFileFilter( new FileNameExtensionFilter( "CASAA files", "casaa" ) );
        if( chooser.showOpenDialog( playerView ) == JFileChooser.APPROVE_OPTION ) {
            cleanupMode();
            filenameMisc = chooser.getSelectedFile().getAbsolutePath();
            filenameAudio = getUtteranceList().loadFromFile( new File( filenameMisc ) ); // Load the code file.
            utteranceListChanged();
            loadAudioFile( filenameAudio ); // Load the audio file.
            setMode( Mode.CODE );
            postLoad();
        }
	}

	// Save current session. Periodically also save backup copy.
    private synchronized void saveSession() {
        // Save normal file.
        saveCurrentTextFile( false );

        // Backup every n'th save.
        if( numSaves % 10 == 0 ) {
            saveCurrentTextFile( true );
        }
        numSaves++;
    }

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private synchronized void saveCurrentTextFile( boolean asBackup ) {
        if( templateView instanceof MiscTemplateView && filenameMisc != null ) {
            String filename = filenameMisc;

            if( asBackup )
                filename += ".backup";
            getUtteranceList().writeToFile( new File( filename ), filenameAudio );
        } else if( templateView instanceof GlobalTemplateView ) {
            String filename = filenameGlobals;

            if( asBackup )
                filename += ".backup";
            ((GlobalTemplateUiService) templateUI).writeGlobalsToFile( new File( filename ), filenameAudio );
        }
    }

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void handleAboutWindow() {
        AboutWindowView aboutWindow = new AboutWindowView();

        aboutWindow.setFocusable( true );
    }

	// ====================================================================
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Other Stuff
    private void showHelpDialog() {
        JOptionPane.showMessageDialog( playerView,
                "Please visit http://casaa.unm.edu for the latest reference manual.",
                "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void showAudioFileNotFoundDialog() {
		JOptionPane.showMessageDialog( playerView,
						"The audio file:\n"
								+ filenameAudio
								+ "\nassociated with this project cannot be located.\n"
								+ "Please verify that this file exists, and that it is named correctly.",
						"Audio File Not Found Error", JOptionPane.ERROR_MESSAGE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void showAudioFileNotSeekableDialog() {
		JOptionPane.showMessageDialog( playerView,
				"The audio file:\n"
				+ filenameAudio
				+ "\nfailed when setting the play position in the file.\n"
				+ "Please try to reload the file.", "Audio File Seek Error",
				JOptionPane.ERROR_MESSAGE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void showQueueNotLoadedDialog() {
		JOptionPane.showMessageDialog(playerView,
				"The Data Queue failed to load.\n"
						+ "Please verify the text file is properly formatted.",
				"Data Queue Loading Error", JOptionPane.ERROR_MESSAGE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void showTemplateNotFoundDialog() {
		JOptionPane.showMessageDialog(playerView,
				"The Coding Template Failed to Load.\n",
				"Coding Template Loading Error", JOptionPane.ERROR_MESSAGE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void showParsingErrorDialog() {
		JOptionPane.showMessageDialog(playerView,
				"An error occurred while parsing this utterance.\n",
				"Utterance Parsing Error", JOptionPane.ERROR_MESSAGE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private void updateTimeDisplay() {
		playerView.getTimeline().repaint();
		if (bytesPerSecond != 0) {
			// Handles constant bit-rates only.
			int bytes = player.getEncodedStreamPosition();
			int seconds = bytes / bytesPerSecond;

			playerView.setLabelTime("Time:  " + TimeCode.toString(seconds));
		} else {
			// EXTEND: Get time based on frames rather than bytes.
			// Need a way to determine current position based on frames.
			// Something like getEncodedStreamPosition(),
			// but that returns frames. This for VBR type compressions.
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void setTemplateView( Mode mode ) {
        templateView    = null;
        templateUI      = null;
        System.gc();

        switch( mode ) {
        case PLAYBACK:
            // No template view.
            break;
        case CODE:
            templateUI      = new MiscTemplateUiService( getActionTable() );
            templateView    = templateUI.getTemplateView();
            break;
        case GLOBALS:
            templateUI      = new GlobalTemplateUiService();
            templateView    = templateUI.getTemplateView();
            break;
        default:
            assert false : "Mode unrecognized: " + mode.toString();
            break;
        }
        playerView.setPanelTemplate( templateView );
    }

	// ====================================================================
	// Parser Template Handlers

	// Get current playback position, in bytes.
	public int streamPosition() {
		int position = player.getEncodedStreamPosition();

		// If playback has reached end of file, position will be -1.
		// In that case, use length - 1.
        if( position < 0 ) {
            int length = player.getEncodedLength() - 1;

            position = (length > 0) ? (length - 1) : 0;
        }
        return position;
	}

	// Mark start of first utterance.
	public synchronized void parseStart() {
	    // Cache stream position, as it may change over repeated queries (because it advances
	    // with player thread).
	    int    position    = streamPosition();

	    if( getUtteranceList().size() > 0 )
	    	return; // Parsing starts only once.

	    if( player.getStatus() != BasicPlayer.PLAYING )
	    	handleActionPlay();

	    // Record start data.
        assert (bytesPerSecond > 0);
        String  startString     = TimeCode.toString( position / bytesPerSecond );

        // Create first utterance.
        Utterance   data    = new MiscDataItem( 0, startString, position );

        getUtteranceList().add( data );
        currentUtterance = 0; // Select this as current utterance.

        resetUncodeCount();
        updateUtteranceDisplays();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MISC Template Handlers

    public synchronized void handleButtonMiscCode( MiscCode miscCode ) {

        assert (miscCode.isValid());

        // Assign code to current utterance, if one exists.
        Utterance utterance = getCurrentUtterance();

        if( utterance == null )
            return; // No current utterance.

        int position = streamPosition();

        if( position <= utterance.getStartBytes() )
            return; // Ignore when playback is outside utterance.

        // TODO - Ignore code if utterance is already coded?
        //  - At the very least, we should not start a new utterance unless we just ended final existing utterance.

        utterance.setMiscCode( miscCode );

        // End utterance.
        assert (bytesPerSecond > 0);
        String positionString = TimeCode.toString( position / bytesPerSecond );

        utterance.setEndTime( positionString );
        utterance.setEndBytes( position );
        saveSession();

        // Start new utterance.
        // NOTE: We save session before starting new utterance, so only coded
        // utterances are saved.  It could work either way, though.
        int         order   = getUtteranceList().size();
        Utterance   data    = new MiscDataItem( order, positionString, position );

        getUtteranceList().add( data );
        currentUtterance = order; // Select new utterance.

        updateUtteranceDisplays();
    }

    // Undo the actions of pressing a MISC code button.
    private synchronized void uncode() {
    	// Remove last utterance, if uncoded (utterance was
    	// generated when user coded the second-to-last utterance).
    	UtteranceList 	list 	= getUtteranceList();
    	Utterance 		u 		= list.last();

    	if( u != null && !u.isCoded() )
    		list.removeLast();

    	// Strip code and end data from last remaining utterance.
    	u = list.last();
    	if( u == null )
    	{
    		// TMP - Handled by uncodeAndReplay.
            // playerSeek( 0 );
        	currentUtterance = 0;
    	}
    	else
    	{
    		u.stripEndData();
    		u.stripMiscCode();

    		// TMP - Handled by uncodeAndReplay.
            // playerSeek( u.getStartBytes() );
        	currentUtterance = u.getEnum();
    	}
    	updateUtteranceDisplays();
    }

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update utterance displays (e.g. current, last, etc) in active template
	// view.
	private synchronized void updateUtteranceDisplays() {
		playerView.getTimeline().repaint();

        if( templateView instanceof MiscTemplateView ) {
			MiscTemplateView view    = (MiscTemplateView) templateView;
			Utterance        current = getCurrentUtterance();
			Utterance        next    = getNextUtterance();
			Utterance        prev    = getPreviousUtterance();

            if( next == null )
                view.setTextFieldNext( "" );
            else
                view.setTextFieldNext( next.toString() );

            if( prev == null )
                view.setTextFieldPrev( "" );
            else
                view.setTextFieldPrev( prev.toString() );

            if( current == null ) {
                view.setTextFieldOrder( "" );
                view.setTextFieldCode( "" );
                view.setTextFieldStartTime( "" );
                view.setTextFieldEndTime( "" );
            } else {
                view.setTextFieldOrder( "" + current.getEnum() );
                if( current.getMiscCode().value == MiscCode.INVALID )
                    view.setTextFieldCode( "" );
                else
                    view.setTextFieldCode( current.getMiscCode().name );

                view.setTextFieldStartTime( current.getStartTime() );
                view.setTextFieldEndTime( current.getEndTime() );

                // Visual indication when in between utterances.
                if( streamPosition() < current.getStartBytes() )
                    view.setTextFieldStartTimeColor( Color.RED );
                else
                    view.setTextFieldStartTimeColor( Color.BLACK );
            }
		}
	}

	// Select current utterance index, seek player, and update UI after loading
	// data from file.
	// PRE: Mode is set, so appropriate templateView is active.
	private synchronized void postLoad() {
		currentUtterance = 0; // Default to first utterance.
        if( templateView instanceof MiscTemplateView ) {
			// Seek to first uncoded utterance. May be one past last utterance in list.
			currentUtterance = getUtteranceList().getLastCodedUtterance();
			currentUtterance++;

            if( getCurrentUtterance() == null ) {
                if( getUtteranceList().isEmpty() )
                    playerSeek( 0 );
                else
                    playerSeek( getLastUtterance().getEndBytes() );
            } else {
                // Special case - if we haven't coded any utterances yet, always seek to zero.
                // Else seek to start of first uncoded utterance.
                if( currentUtterance == 0 )
                    playerSeek( 0 );
                else
                    playerSeek( getCurrentUtterance().getStartBytes() );
            }
        } else if( templateView == null ) {
			showTemplateNotFoundDialog();
		} else {
			showQueueNotLoadedDialog();
		}

		updateUtteranceDisplays();
		numSaves = 0; // Reset save counter, so we backup on next save (i.e. as
						// soon as player saves changes to newly loaded data).
						// Just to be nice.
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private synchronized UtteranceList getUtteranceList() {
		if( utteranceList == null )
			utteranceList = new UtteranceList();

		return utteranceList;
	}

	// ====================================================================
	// BasicPlayerListener interface
	// ====================================================================

	public void opened(Object stream, Map<Object, Object> properties) {
	}

	public void setController(BasicController controller) {
	}

	public void progress(int bytesread, long microseconds, byte[] pcmdata,
			Map<Object, Object> properties) {
		progressReported = true; // Will be handled in main thread's run().
	}

	public void stateUpdated(BasicPlayerEvent event) {
		// Notification of BasicPlayer states (opened, playing, end of media, ...).
		// Modify stored playerStatus string only on "significant" changes (e.g.
		// "Opened", but not "Seeked").
		// Synchronize, so we apply changes before another status update comes in.
		synchronized (this) {
			String oldStatus = new String(playerStatus);

			switch (event.getCode()) {
			case 0:
				playerStatus = "OPENING";
				break;
			case 1:
				playerStatus = "OPENED";
				break;
			case 2:
				playerStatus = "PLAYING";
				break;
			case 3:
				playerStatus = "STOPPED";
				break;
			case 4:
				playerStatus = "PAUSED";
				break;
			case 5:
				// RESUMED
				playerStatus = "PLAYING";
				break;
			case 6:
				// SEEKING
				break;
			case 7:
				// SEEKED
				break;
			case 8:
				// End of media.  TODO - Anything?  Pause?
				break;
			case 9:
				// PAN
				break;
			case 10:
				// GAIN
				break;
			default:
				playerStatus = "UNKNOWN";
			}

			// If status has changed, and no later-ordered event has already
			// changed the status,
			// apply this event's changes.
			if (!playerStatus.equals(oldStatus)) {
				if (event.getIndex() >= statusChangeEventIndex) {
					statusChangeEventIndex = event.getIndex();

					File file = new File(filenameAudio);
					String str = playerStatus.concat(":  " + file.getName()
							+ "  |  Total Time = "
							+ TimeCode.toString(player.getSecondsPerFile()));

					playerView.setLabelPlayerStatus(str);
				}
			}
		}
	}
}
