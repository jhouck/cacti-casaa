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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * This class creates the empty shell GUI for the Audio Player interface.
 * 
 * @author UNM CASAA
 *
 */
public class PlayerView extends JFrame {

	private static final long serialVersionUID = 1L;

	//====================================================================
	// Fields
	//====================================================================

	// Window Constants and Variables
	private String strWindowTitle = "CACTI | The CASAA Application for Coding Treatment Interactions | v" + Version.versionString();
	private static final int X_LOCATION			= 100;
	private static final int Y_LOCATION			= 5;
	private static final int WINDOW_MIN_WIDTH	= 700;
	private static final int WINDOW_MIN_HEIGHT	= 0;

	// GUI Components
	private JPanel topLayoutPanel					= null;
	private static final int TOP_PANEL_WIDTH		= 600;
	private static final int TOP_PANEL_HEIGHT		= 270;
	private Border borderTop						= null;
	private JPanel bottomLayoutPanel				= null;
	private static final int BOTTOM_PANEL_WIDTH		= 600;
	private static final int BOTTOM_PANEL_HEIGHT	= 450;
	private Border borderBottom						= null;
	private JPanel panelTemplate					= null;
	private JPanel panelPlayerControls				= null;
	private JPanel panelFileInfo					= null;
	private Timeline timeline						= null;

	// Player Controls
	private Dimension dimPlayerButtonSize		= null;
	private static final int BUTTON_WIDTH		= 100;
	private static final int BUTTON_HEIGHT		= 25;
	private JButton buttonPlay					= null;
	private JButton buttonReplay				= null;
	private JButton buttonUncode				= null;
	private JButton buttonUncodeAndReplay		= null;
	private JButton buttonRewind5s				= null;
	private JSlider sliderSeek					= null;
	private JLabel labelPlayerStatus			= null;
	private JLabel labelTime					= null;

	private JMenuBar menuBarPlayer				= null;
	private JMenu menuFile						= null;
	private JMenuItem menuItemLoadAudio			= null;	
    private JMenuItem menuItemOptions           = null;
	private JMenuItem menuItemExit				= null;
	private JMenu menuCode						= null;
	private JMenuItem menuItemNewCode			= null;
	private JMenuItem menuItemLoadCode			= null;
	private JMenu menuGlobals					= null;
	private JMenuItem menuItemCodeGlobals		= null;
	private JMenu menuAbout						= null;
	private JMenuItem menuItemHelp				= null;
	private JMenuItem menuItemAbout				= null;


	// Slider Control Constants
	private static final int SEEK_MIN_VAL		= 0;
	public static final int SEEK_MAX_VAL		= 1000;
	private static final int SEEK_INIT_VAL		= 0;
	private TitledBorder borderSeek				= null;

	//====================================================================
	// Constructor and Initialization Methods
	//====================================================================

	public PlayerView() {
        setTitle( strWindowTitle );

        setMinimumSize( new Dimension( WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT ) );
        setLocation( X_LOCATION, Y_LOCATION );

        setIconImage( new ImageIcon( "images/UNM_Color.jpg" ).getImage() );
        setJMenuBar( getMenuBarPlayer() );
        setResizable( true );

        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( getTopLayoutPanel(), BorderLayout.NORTH );
        getContentPane().add( getBottomLayoutPanel(), BorderLayout.CENTER );
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public static void setLookAndFeel(){
		// Set the look and feel to the native platform
		String lookAndFeel = UIManager.getSystemLookAndFeelClassName();

		// This check avoids a bug where gtk can't display properly.
		if( !lookAndFeel.equalsIgnoreCase( "com.sun.java.swing.plaf.windows.WindowsLookAndFeel" ) ) {
			lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
		}

		try {
			UIManager.setLookAndFeel( lookAndFeel );
		} catch (InstantiationException e) {
		} catch (ClassNotFoundException e) {
		} catch (UnsupportedLookAndFeelException e) {
		} catch (IllegalAccessException e) {
		}
	}

	//====================================================================
	// Public Getter and Setter Methods
	//====================================================================

	/**
	 * Returns a new player button using given action and optional key binding.
	 * @param actionCommand
	 * @param keyBinding - if non-null, assign given key stroke as key binding.
	 * @return a JButton used to start playing the audio file
	 */
	private JButton newPlayerButton( String actionCommand, KeyStroke keyBinding ) {
		JButton button = new JButton( MainController.instance.getActionTable().get( actionCommand ) );

		button.setPreferredSize( getDimPlayerButtonSize() );
		if( keyBinding != null ) {
			button.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( keyBinding, "pressed" );
			button.getActionMap().put( "pressed", button.getAction() );
		}
		return button;
	}

	/**
	 * Returns the Player's Play Button
	 * @return a JButton used to start playing the audio file
	 */
	public JButton getButtonPlay() {
		if( buttonPlay == null ) {
			buttonPlay = newPlayerButton( "play", KeyStroke.getKeyStroke( KeyEvent.VK_P, 0 ) );
		}
		return buttonPlay;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Returns the Player's Replay Button
	 * @return a JButton used to replay audio from beginning of current utterance.
	 */
	public JButton getButtonReplay(){
		if( buttonReplay == null ){
			buttonReplay = newPlayerButton( "replay", KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 ) );
		}
		return buttonReplay;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Returns the Player's Uncode Button
	 * @return a JButton used to undo last parse, without affecting playback position.
	 */
	public JButton getButtonUncode(){
		if( buttonUncode == null ){
			buttonUncode = newPlayerButton( "uncode", KeyStroke.getKeyStroke( KeyEvent.VK_N, 0 ) );
		}
		return buttonUncode;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Returns the Player's UncodeAndReplay Button
	 * @return a JButton used to undo last code, and rewind playback position.
	 */
	public JButton getButtonUncodeAndReplay(){
		if( buttonUncodeAndReplay == null ){
			buttonUncodeAndReplay = newPlayerButton( "uncodeAndReplay", KeyStroke.getKeyStroke( KeyEvent.VK_U, 0 ) );

			// Button needs more space for label.
			buttonUncodeAndReplay.setPreferredSize( new Dimension( (int) (BUTTON_WIDTH * 1.5), BUTTON_HEIGHT ) );
		}
		return buttonUncodeAndReplay;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Returns the Player's Rewind5s Button
	 * @return a JButton used to rewind the audio file five seconds, without affecting parse.
	 */
	public JButton getButtonRewind5s(){
		if( buttonRewind5s == null ){
			buttonRewind5s = newPlayerButton( "rewind5s", KeyStroke.getKeyStroke( KeyEvent.VK_5, 0 ) );
		}
		return buttonRewind5s;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Returns the Player's Seek Slider
	 * @return a JSlider used to control and show the current play position
	 */
	public JSlider getSliderSeek(){
		if( sliderSeek == null ){
			sliderSeek = new JSlider(SwingConstants.HORIZONTAL, 
					SEEK_MIN_VAL, SEEK_MAX_VAL, 
					SEEK_INIT_VAL);
			sliderSeek.setBorder(getBorderSeek());
			//sliderSeek.setToolTipText("Seek the Audio File");
			sliderSeek.setDoubleBuffered(true);
		}
		return sliderSeek;
	}
	
	private Border getBorderSeek(){
		if( borderSeek == null ){
			borderSeek = BorderFactory.createTitledBorder("Seek");
			borderSeek.setTitleJustification(TitledBorder.LEADING);
		}
		return borderSeek;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Sets the Player's Seek Slider to a value between 0 and 1000.
	 * @param int value
	 */
	public void setSliderSeek( int value ){
		getSliderSeek().setValue( value );
	}

	//====================================================================
	// Private Helper Methods
	//====================================================================

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getTopLayoutPanel(){
		if( topLayoutPanel == null ){
			topLayoutPanel = new JPanel();
			topLayoutPanel.setMaximumSize(new Dimension(TOP_PANEL_WIDTH, TOP_PANEL_HEIGHT));
			topLayoutPanel.setMinimumSize(new Dimension(TOP_PANEL_WIDTH, TOP_PANEL_HEIGHT));
			topLayoutPanel.setLayout(new BorderLayout());
			topLayoutPanel.setBorder(getBorderTop());
			topLayoutPanel.add(getPanelPlayerControls(), BorderLayout.NORTH);
			topLayoutPanel.add(getPanelFileInfo(), BorderLayout.CENTER);
		}
		return topLayoutPanel;
	}

	private Border getBorderTop(){
		if( borderTop == null ){
			borderTop = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		}
		return borderTop;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getBottomLayoutPanel(){
		if( bottomLayoutPanel == null ){
			bottomLayoutPanel = new JPanel();
			bottomLayoutPanel.setMaximumSize(new Dimension(BOTTOM_PANEL_WIDTH,
					BOTTOM_PANEL_HEIGHT));
			bottomLayoutPanel.setMinimumSize(new Dimension(BOTTOM_PANEL_WIDTH,
					BOTTOM_PANEL_HEIGHT));
			bottomLayoutPanel.setBorder(getBorderBottom());
			bottomLayoutPanel.setVisible(true);
		}
		return bottomLayoutPanel;
	}

	private Border getBorderBottom(){
		if( borderBottom == null ){
			borderBottom = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		}
		return borderBottom;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public void setPanelTemplate(JPanel template){
		if( panelTemplate != null ){
			getBottomLayoutPanel().remove(panelTemplate);
			getBottomLayoutPanel().setVisible(false);
		}	
		if( template != null ){
			panelTemplate = template;
			getBottomLayoutPanel().add(panelTemplate);
			getBottomLayoutPanel().setVisible(true);
		}		
		getBottomLayoutPanel().validate();
	}
	
	public JPanel getPanelTemplate(){
		if( panelTemplate == null ){
			return getBottomLayoutPanel();
		}
		else{
			return panelTemplate;
		}
	}
	
	public String getTemplateType(){
		if( panelTemplate != null ){
			return panelTemplate.toString();
		}
		else{
			return null;
		}		
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelPlayerControls(){
		if( panelPlayerControls == null ){
			panelPlayerControls = new JPanel();
			panelPlayerControls.setLayout(new FlowLayout());
			panelPlayerControls.add(getButtonPlay());
			panelPlayerControls.add(getButtonReplay());
			panelPlayerControls.add(getButtonUncode());
			panelPlayerControls.add(getButtonUncodeAndReplay());
			panelPlayerControls.add(getButtonRewind5s());
		}
		return panelPlayerControls;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelFileInfo(){
		if( panelFileInfo == null ){
			panelFileInfo = new JPanel();
			panelFileInfo.setLayout(new BoxLayout(panelFileInfo, BoxLayout.Y_AXIS));
			panelFileInfo.add(getLabelTime());
			panelFileInfo.add(getLabelPlayerStatus());
			panelFileInfo.add(getSliderSeek());
			panelFileInfo.add(getTimeline());
		}
		return panelFileInfo;
	}

	public Timeline getTimeline() {
		if( timeline == null ){
			timeline = new Timeline( MainController.instance );
		}
		return timeline;
	}
	
	private JLabel getLabelPlayerStatus(){
		if( labelPlayerStatus == null ){
			labelPlayerStatus = new JLabel("Player Status");
		}
		return labelPlayerStatus;
	}

	public void setLabelPlayerStatus(String strStatus){
		getLabelPlayerStatus().setText(strStatus);
	}

	private JLabel getLabelTime(){
		if( labelTime == null ){
			labelTime = new JLabel("Time");
			labelTime.setDoubleBuffered(true);
		}
		return labelTime;
	}

	public void setLabelTime(String strTime){
		getLabelTime().setText(strTime);
		getLabelTime().validate();
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JMenuBar getMenuBarPlayer(){
		if( menuBarPlayer == null ){
			menuBarPlayer = new JMenuBar();
			menuBarPlayer.add(getMenuFile());
			menuBarPlayer.add(getMenuCode());
			menuBarPlayer.add(getMenuGlobals());
			menuBarPlayer.add(getMenuAbout());
		}
		return menuBarPlayer;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JMenu getMenuFile(){
		if( menuFile == null ){
			menuFile = new JMenu(" File ");
			menuFile.add(getMenuItemLoadAudio());
            menuFile.add(getMenuItemOptions());
			menuFile.add(getMenuItemExit());
		}
		return menuFile;
	}

	public JMenuItem getMenuItemLoadAudio(){
		if( menuItemLoadAudio == null ){
			menuItemLoadAudio = new JMenuItem("Load Audio File");
		}
		return menuItemLoadAudio;
	}
	
    public JMenuItem getMenuItemOptions(){
        if( menuItemOptions == null ){
            menuItemOptions = new JMenuItem("Options");
        }
        return menuItemOptions;
    }

    public JMenuItem getMenuItemExit(){
		if( menuItemExit == null ){
			menuItemExit = new JMenuItem("Exit");
		}
		return menuItemExit;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JMenu getMenuGlobals(){
		if( menuGlobals == null ){
			menuGlobals = new JMenu(" Global Ratings ");
			menuGlobals.add(getMenuItemCodeGlobals());
		}
		return menuGlobals;
	}
	
	public JMenuItem getMenuItemCodeGlobals(){
		if( menuItemCodeGlobals == null ){
			menuItemCodeGlobals = new JMenuItem("Score Global Ratings");
		}
		return menuItemCodeGlobals;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JMenu getMenuCode(){
		if( menuCode == null ){
			menuCode = new JMenu(" Code Utterances ");
			menuCode.add(getMenuItemNewCode());
			menuCode.add(getMenuItemLoadCode());
		}
		return menuCode;
	}
	
	public JMenuItem getMenuItemNewCode(){
		if( menuItemNewCode == null ){
			menuItemNewCode = new JMenuItem("Start Coding a File");
		}
		return menuItemNewCode;
	}

	public JMenuItem getMenuItemLoadCode(){
		if( menuItemLoadCode == null ){
			menuItemLoadCode = new JMenuItem("Resume Coding a File");
		}
		return menuItemLoadCode;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JMenu getMenuAbout(){
		if( menuAbout == null ){
			menuAbout = new JMenu(" About ");
			menuAbout.add(getMenuItemHelp());
			menuAbout.add(getMenuItemAbout());
		}
		return menuAbout;
	}

	public JMenuItem getMenuItemHelp(){
		if( menuItemHelp == null ){
			menuItemHelp = new JMenuItem("Help");
		}
		return menuItemHelp;
	}

	public JMenuItem getMenuItemAbout(){
		if( menuItemAbout == null ){
			menuItemAbout = new JMenuItem("About this Application");
		}
		return menuItemAbout;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private Dimension getDimPlayerButtonSize(){
		if( dimPlayerButtonSize == null ){
			dimPlayerButtonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
		}
		return dimPlayerButtonSize;
	}

}
