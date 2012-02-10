// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.Color;
import java.awt.Component;
import java.io.PrintStream;

import javax.swing.*;

import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;

/**
 * Class representing a control panel.
 * 
 * @author Camille Nicodemus
 * @see MonPanel
 */
public class ControlPanel extends MonPanel {
	static {
		MonPanel.registerMonPanel("Control Panel", ControlPanel.class);
	}

	// //// NESTED CLASS: Setup Panel ///////
	public class ControlSetupPanel extends MonPanelSetupPanel {
		/** Allows user to enter number of components to control */
		protected JTextField itsNumComponents = new JTextField(10);
		/** Identify if authentication details are to be cached */
		protected JCheckBox itsCached = new JCheckBox("Cache?");
		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();

		public ControlSetupPanel(ControlPanel panel, JFrame frame) {
			super(panel, frame);

			// Authentication
			JButton login = new JButton("Login");
			JPanel authenticate = new JPanel(new BoxLayout(this,
					BoxLayout.X_AXIS));
			authenticate.setBackground(Color.DARK_GRAY);
			login.setAlignmentX(Component.RIGHT_ALIGNMENT);
			itsCached.setAlignmentX(Component.RIGHT_ALIGNMENT);
			authenticate.add(login);
			authenticate.add(itsCached);

			// Number of components option
			JLabel componentlabel = new JLabel(
					"Enter the number of components to display:");
			JButton create = new JButton("Create components");
			JPanel createcomponents = new JPanel(new BoxLayout(this,
					BoxLayout.X_AXIS));
			createcomponents.add(componentlabel);
			createcomponents.add(create);

			itsMainPanel.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			itsMainPanel.add(authenticate);
			itsMainPanel.add(createcomponents);

			add(new JScrollPane(itsMainPanel));
		}

		@Override
		protected SavedSetup getSetup() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			// TODO Auto-generated method stub

		}
	}

	// ///// END NESTED CLASS ///////

	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;

	/** Constructor. */
	public ControlPanel() {
		
	}

	public MonPanelSetupPanel getControls() {
		return new ControlSetupPanel(this, itsFrame);
	}

	public boolean loadSetup(SavedSetup setup) {
		return false;
	}

	public synchronized SavedSetup getSetup() {
		return itsSetup;
	}

	public void vaporise() {
	}

	public void export(PrintStream p) {
	}

	public String getLabel() {
		return null;
	}
}
