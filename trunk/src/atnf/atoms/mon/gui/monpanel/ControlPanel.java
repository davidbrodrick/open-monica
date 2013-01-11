// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
			
			JPanel bigPanel = new JPanel(new GridLayout(3, 1));
			SimpleTreeSelector itsSimpleTreeSelector;
			JLabel selectedPointLabel = new JLabel("Selected Point: ");
			JLabel selectedPoint = new JLabel("No Point Selected");
			JLabel controlTypeLabel = new JLabel("Control Type: ");
			JComboBox controlType;
			JLabel dataTypeLabel = new JLabel("Data Type: ");
			JComboBox dataType;
			JLabel displayLabel = new JLabel("Label: ");
			JTextField displayField = new JTextField(10);
			
			JPanel selectorPanel = new JPanel(new GridBagLayout());
			GridBagConstraints spc = new GridBagConstraints();
			itsSimpleTreeSelector = new SimpleTreeSelector();
			spc.weightx = 0.5;
			spc.weighty = 0.5;
			spc.gridx = 0;
			spc.gridy = 0;
			spc.gridheight = 4;
			spc.gridwidth = 1;
			selectorPanel.add(itsSimpleTreeSelector, spc);
			spc.gridx = 1;
			spc.gridy = 1;
			spc.gridheight = 1;
			spc.gridwidth = 2;
			selectorPanel.add(selectedPointLabel, spc);
			spc.gridy = 2;
			selectorPanel.add(selectedPoint, spc);


			JPanel typePanel = new JPanel(new GridLayout(1, 4));
			controlType = new JComboBox(controlOptions);
			controlType.setEditable(false);		
			dataType = new JComboBox(dataOptions);
			dataType.setEditable(false);
			typePanel.add(controlTypeLabel);
			typePanel.add(controlType);
			typePanel.add(dataTypeLabel);
			typePanel.add(dataType);

			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
			labelPanel.add(Box.createHorizontalGlue());
			labelPanel.add(displayLabel);
			labelPanel.add(displayField);
			labelPanel.add(Box.createHorizontalGlue());

			bigPanel.add(selectorPanel);
			bigPanel.add(typePanel);
			bigPanel.add(labelPanel);

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
