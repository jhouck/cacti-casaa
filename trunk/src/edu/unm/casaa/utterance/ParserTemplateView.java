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

package edu.unm.casaa.utterance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import edu.unm.casaa.main.ActionTable;

import edu.unm.casaa.main.Style;

public class ParserTemplateView extends JPanel {

	private static final long serialVersionUID = 1L;
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	//====================================================================
	// Fields
	//====================================================================
	// Window Constants and Variables
	private static final int PANEL_WIDTH	= 600;
	private static final int PANEL_HEIGHT	= 450;

	// GUI Components and Constants
	private TitledBorder borderWindow		= null;
	private Dimension dimMainPanel			= null;
	private JPanel panelButtons				= null;
	private JPanel panelCurrentText			= null;
	private JPanel panelPrevText			= null;
	
	// Buttons
	private JButton buttonStartParse		= null;
	private JButton buttonEndParse			= null;
	private Dimension dimButtonSize			= null;
	private static final int BUTTON_WIDTH	= 248;
	private static final int BUTTON_HEIGHT	= 100;
	private TitledBorder borderButtons		= null;
	
	// Current utterance display.
	private JTextField textFieldOrder		= null;
	private static final int ORDER_COLS		= 9;
	private JLabel labelOrder				= null;
	private JTextField textFieldStartTime	= null;
	private JTextField textFieldEndTime		= null;
	private static final int TIME_COLS		= 20;
	private JLabel labelStart				= null;
	private JLabel labelEnd					= null;
	private TitledBorder borderTextFields	= null;
	
	// Previous utterance display.
	private JTextField textFieldPrev		= null;
	private static final int PREV_COLS		= 60;
	private TitledBorder borderPrev			= null;
	
	private ActionTable		actionTable		= new ActionTable(); // Communication between GUI and MainController.

	//====================================================================
	// Constructor and Initialization Methods
	//====================================================================

	public ParserTemplateView( ActionTable actionTable ) {
		assert( actionTable != null );
		this.actionTable = actionTable;
		setBorder(getBorderWindow());
		setMaximumSize(getDimMainPanel());
		setMinimumSize(getDimMainPanel());
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(getPanelPrevText());
		add(getPanelCurrentText());
		add(getPanelButtons());
		setVisible(true);
	}

	//====================================================================
	// Getter and Setter Methods
	//====================================================================

	public JButton getButtonStartParse(){
		if( buttonStartParse == null ){
			buttonStartParse = new JButton( actionTable.get( "parseStart" ) );
			buttonStartParse.setPreferredSize( getDimButtonSize() );
			buttonStartParse.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put(
					KeyStroke.getKeyStroke( KeyEvent.VK_COMMA, 0 ), "pressed" );
			buttonStartParse.getActionMap().put( "pressed", buttonStartParse.getAction() );
			/*buttonStartParse.setToolTipText("Start Parsing the Current Utterance\n\n" +
					"Pressing this button a second time will end the current utterance, \n" +
					"and begin parsing a second utterance.");*/
		}
		return buttonStartParse;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public JButton getButtonEndParse(){
		if( buttonEndParse == null ){
			buttonEndParse = new JButton( actionTable.get( "parseEnd" ) );
			buttonEndParse.setPreferredSize( getDimButtonSize() );
			buttonEndParse.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put(
					KeyStroke.getKeyStroke( KeyEvent.VK_PERIOD, 0 ), "pressed" );
			buttonEndParse.getActionMap().put( "pressed", buttonEndParse.getAction() );
			/*buttonEndParse.setToolTipText("End Parsing the Current Utterance\n\n" +
							"Pressing this button will end the current utterance.");*/
		}
		return buttonEndParse;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldOrder(){
		if( textFieldOrder == null ){
			textFieldOrder = new JTextField(ORDER_COLS);
			textFieldOrder.setEditable(false);
			Style.configureStrongText( textFieldOrder );
		}
		return textFieldOrder;
	}
	
	public void setTextFieldOrder(String text){
		getTextFieldOrder().setText(text);
	}
	
	private JLabel getLabelOrder(){
		if( labelOrder == null ){
			labelOrder = new JLabel("Enumeration");
			labelOrder.setLabelFor(getTextFieldOrder());
		}
		return labelOrder;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldStartTime(){
		if( textFieldStartTime == null ){
			textFieldStartTime = new JTextField(TIME_COLS);
			textFieldStartTime.setEditable(false);
			Style.configureStrongText( textFieldStartTime );
		}
		return textFieldStartTime;
	}
	
	public void setTextFieldStartTime(String utteranceString){
		getTextFieldStartTime().setText(utteranceString);
	}
	
	private JLabel getLabelStart(){
		if( labelStart == null ){
			labelStart = new JLabel("Start Time");
			labelStart.setLabelFor(getTextFieldStartTime());
		}
		return labelStart;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldEndTime(){
		if( textFieldEndTime == null ){
			textFieldEndTime = new JTextField(TIME_COLS);
			textFieldEndTime.setEditable(false);
			Style.configureStrongText( textFieldEndTime );
		}
		return textFieldEndTime;
	}
	
	public void setTextFieldEndTime(String utteranceString){
		getTextFieldEndTime().setText(utteranceString);
	}
	
	private JLabel getLabelEnd(){
		if( labelEnd == null ){
			labelEnd = new JLabel("End Time");
			labelEnd.setLabelFor(getTextFieldEndTime());
		}
		return labelEnd;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JTextField getTextFieldPrev(){
		if( textFieldPrev == null ){
			textFieldPrev = new JTextField(PREV_COLS);
			textFieldPrev.setEditable(false);
			Style.configureLightText( textFieldPrev );
		}
		return textFieldPrev;
	}
	
	public void setTextFieldPrev(String utteranceString){
		getTextFieldPrev().setText(utteranceString);
	}
	
	public String toString(){
		return ("PARSER");
	}

	//====================================================================
	// Private Helper Methods
	//====================================================================
	
	private Dimension getDimMainPanel(){
		if( dimMainPanel == null ){
			dimMainPanel = new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
		}
		return dimMainPanel;
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelButtons(){
		if( panelButtons == null ){
			panelButtons = new JPanel();
			panelButtons.setBorder(getBorderButtons());
			panelButtons.setLayout(new FlowLayout());
			panelButtons.add(getButtonStartParse());
			panelButtons.add(getButtonEndParse());
		}
		return panelButtons;
	}

	private TitledBorder getBorderButtons(){
		if( borderButtons == null ){
			borderButtons = BorderFactory.createTitledBorder("Parsing Controls");
			borderButtons.setTitleJustification(TitledBorder.LEADING);
		}
		return borderButtons;
	}
	
	private Dimension getDimButtonSize(){
		if( dimButtonSize == null ){
			dimButtonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
		}
		return dimButtonSize;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelCurrentText(){
		if( panelCurrentText == null ){
			panelCurrentText = new JPanel();
			panelCurrentText.setBorder(getBorderTextFields());
			panelCurrentText.setLayout(new GridBagLayout());
			
			GridBagConstraints orderC = new GridBagConstraints();
			orderC.gridx = 0;
			orderC.gridy = 0;
			orderC.weightx = 1.0;
			orderC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelOrder(), orderC);
			
			GridBagConstraints startC = new GridBagConstraints();
			startC.gridx = 2;
			startC.gridy = 0;
			startC.weightx = 1.0;
			startC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelStart(), startC);

			GridBagConstraints endC = new GridBagConstraints();
			endC.gridx = 3;
			endC.gridy = 0;
			endC.weightx = 1.0;
			endC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getLabelEnd(), endC);

			GridBagConstraints orderTC = new GridBagConstraints();
			orderTC.gridx = 0;
			orderTC.gridy = 1;
			orderTC.weightx = 1.0;
			orderTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldOrder(), orderTC);

			GridBagConstraints startTC = new GridBagConstraints();
			startTC.gridx = 2;
			startTC.gridy = 1;
			startTC.weightx = 1.0;
			startTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldStartTime(), startTC);
			
			GridBagConstraints endTC = new GridBagConstraints();
			endTC.gridx = 3;
			endTC.gridy = 1;
			endTC.weightx = 1.0;
			endTC.anchor = GridBagConstraints.LINE_START;
			panelCurrentText.add(getTextFieldEndTime(), endTC);
		}
		return panelCurrentText;
	}
	
	private TitledBorder getBorderTextFields(){
		if( borderTextFields == null ){
			borderTextFields = BorderFactory.createTitledBorder("Current Utterance");
			borderTextFields.setTitleJustification(TitledBorder.LEADING);
		}
		return borderTextFields;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private JPanel getPanelPrevText(){
		if( panelPrevText == null ){
			panelPrevText = new JPanel();
			panelPrevText.setBorder(getBorderPrev());
			panelPrevText.setLayout(new BorderLayout());
			panelPrevText.add(getTextFieldPrev(), BorderLayout.CENTER);
		}
		return panelPrevText;
	}
	
	private TitledBorder getBorderPrev(){
		if( borderPrev == null ){
			borderPrev = BorderFactory.createTitledBorder("Previous Utterance");
			borderPrev.setTitleJustification(TitledBorder.LEADING);
		}
		return borderPrev;
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private TitledBorder getBorderWindow(){
		if( borderWindow == null ){
			borderWindow = BorderFactory.createTitledBorder("Utterance Parsing Template");
			borderWindow.setTitleJustification(TitledBorder.CENTER);
			borderWindow.setTitleColor(Color.BLACK);
		}
		return borderWindow;
	}
	
}