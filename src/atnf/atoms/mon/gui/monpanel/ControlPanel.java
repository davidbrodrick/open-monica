// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.SimpleTreeSelector;

/**
 * Class representing a control panel.
 * 
 * @author Camille Nicodemus
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
public class ControlPanel extends MonPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5900630541567847520L;

	static {
		MonPanel.registerMonPanel("Control Panel", ControlPanel.class);
	}

	// //// NESTED CLASS: ControlSetupPanel ///////
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener, ChangeListener{

		/**
		 * 
		 */
		private static final long serialVersionUID = -3585682908515598074L;
		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private JPanel spinnerPanel = new JPanel();
		private JScrollPane viewPane = new JScrollPane();

		private JLabel numberControlsLabel = new JLabel("Number of Controls: ");
		private JSpinner numberControls = new JSpinner();
		private SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
		private Integer numControls = 0;

		private String[] controlOptions = {"Button", "Text Field", "Checkbox"};
		private String[] dataOptions = {"Integer", "True/False", "String"};

		private ArrayList<JPanel> panelList = new ArrayList<JPanel>();

		public ControlSetupPanel(ControlPanel panel, JFrame frame) {
			//TODO
			super(panel, frame);

			spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));			
			itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

			numberControls.setMaximumSize(new Dimension(45, 25));
			numberControls.setModel(spinModel);
			numberControls.addChangeListener(this);

			numberControlsLabel.setAlignmentX(CENTER_ALIGNMENT);
			numberControls.setAlignmentX(CENTER_ALIGNMENT);
			spinnerPanel.add(Box.createHorizontalGlue());
			spinnerPanel.add(numberControlsLabel);
			spinnerPanel.add(numberControls);
			spinnerPanel.add(Box.createHorizontalGlue());

			this.add(spinnerPanel, BorderLayout.NORTH);
			viewPane.setViewportView(itsMainPanel);
			this.add(viewPane, BorderLayout.CENTER);
		}

		@Override
		protected SavedSetup getSetup() {
			//TODO
			SavedSetup ss = new SavedSetup();

			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			//TODO

		}

		@Override
		public void stateChanged(ChangeEvent arg0) {

			JSpinner source = (JSpinner) arg0.getSource();
			if (source.getValue() instanceof Integer){
				if (source.getValue().equals(numControls)){
					return;
				} else if ((Integer)source.getValue() > numControls && ((Integer) source.getValue() - numControls) == 1){
					addControlSetup();
				} else if ((Integer) source.getValue() < numControls && ((Integer) source.getValue() - numControls) == -1){
					itsMainPanel.remove(panelList.get(panelList.size()-1));
					if (panelList.size() > 0) panelList.remove(panelList.size()-1);
				} else {
					itsMainPanel = new JPanel();
					itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
					for (int i = 0; i < (Integer)source.getValue(); i++){
						addControlSetup();
					}
				}
				numControls = (Integer)source.getValue();
				itsMainPanel.revalidate();
				itsMainPanel.repaint();
				viewPane.setViewportView(itsMainPanel);
			}

		}

		public void addControlSetup(){
			
			JPanel bigPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			
			SimpleTreeSelector itsSimpleTreeSelector = new SimpleTreeSelector();
			JLabel selectedPointLabel = new JLabel("Selected Point: ");
			selectedPointLabel.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			JLabel selectedPoint = new JLabel("No Point Selected");
			selectedPoint.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			JLabel controlTypeLabel = new JLabel("Control Type: ");
			JComboBox controlType = new JComboBox(controlOptions);
			controlType.setEditable(false);
			JLabel dataTypeLabel = new JLabel("Data Type: ");
			JComboBox dataType = new JComboBox(dataOptions);
			dataType.setEditable(false);
			JLabel displayLabel = new JLabel("Label: ");
			JTextField displayField = new JTextField(10);
			
			gbc.gridheight = 4;
			gbc.gridwidth = 3;
			bigPanel.add(itsSimpleTreeSelector, gbc);
			
			gbc.gridheight = 1;
			gbc.gridwidth = 2;
			gbc.gridx = 3;
			gbc.gridy = 1;
			bigPanel.add(selectedPointLabel, gbc);
			
			gbc.gridy = 2;
			bigPanel.add(selectedPoint, gbc);
			
			gbc.gridwidth = 1;
			gbc.gridx = 1;
			gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(controlTypeLabel, gbc);
			
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.gridx = 2;
			bigPanel.add(controlType, gbc);
			
			gbc.gridx = 3;
			bigPanel.add(dataTypeLabel, gbc);
			
			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 4;
			bigPanel.add(dataType, gbc);
			
			gbc.gridx = 2;
			gbc.gridy = 5;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(displayLabel, gbc);
			
			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 3;
			bigPanel.add(displayField, gbc);
			
			itsMainPanel.add(bigPanel);
			panelList.add(bigPanel);
		}


	}

	// ///// END NESTED CLASS ///////

	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;

	/** Constructor. */
	public ControlPanel() {
		//TODO

	}

	// ///// NESTED CLASS: ControlDisplayPanel ///////

	public class ControlDisplayPanel extends JPanel{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5755259256750234800L;

		public ControlDisplayPanel(){
			//TODO
		}


	}

	// ///// END NESTED CLASS ///////


	public MonPanelSetupPanel getControls() {
		return new ControlSetupPanel(this, itsFrame);
	}

	public boolean loadSetup(SavedSetup setup) {
		try {
			// check if the setup is suitable for our class
			if (!setup.checkClass(this)) {
				System.err.println("ControlPanel:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}


		} catch (final Exception e) {
			e.printStackTrace();
			if (itsFrame != null) {
				JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
						+ "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
						JOptionPane.WARNING_MESSAGE);
			} else {
				System.err.println("ControlPanel:loadData: " + e.getClass().getName());
			}
			blankSetup();
			return false;
		}
		return true;
	}

	private void blankSetup() {
		//TODO
	}

	public synchronized SavedSetup getSetup() {
		return itsSetup;
	}

	public void vaporise() {
		//TODO
	}

	public void export(PrintStream p) {
		//TODO
	}

	public String getLabel() {
		return "Control Panel";
	}


}
