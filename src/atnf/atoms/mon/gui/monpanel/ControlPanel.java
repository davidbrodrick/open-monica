// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeSelectionModel;

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
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -3585682908515598074L;
		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private SimpleTreeSelector itsSimpleTreeSelector = new SimpleTreeSelector();
		

		public ControlSetupPanel(ControlPanel panel, JFrame frame) {
			//TODO
			super(panel, frame);

			itsSimpleTreeSelector.setSelectionMode(SimpleTreeSelector.SINGLE_TREE_SELECTION);
			itsMainPanel.add(itsSimpleTreeSelector);
			this.add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);
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
