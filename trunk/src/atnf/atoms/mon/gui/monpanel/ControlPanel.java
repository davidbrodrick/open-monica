// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.DataMaintainer;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.PointSourceSelector;
import atnf.atoms.mon.util.RSA;

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

	private String username = "";
	private RSA passwordEncryptor = new RSA(1024);
	private String encryptedPassword = "";

	// //// NESTED CLASS: ControlSetupPanel ///////
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3194244600789453530L;

		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private PointSourceSelector itsPointSelector = new PointSourceSelector();
		private JButton login;
		private JLabel loginState;

		public ControlSetupPanel(ControlPanel panel, JFrame frame) {
			super(panel, frame);

			// Authentication
			login = new JButton("Login");
			login.addActionListener(this);
			JPanel authenticate = new JPanel();
			authenticate.setLayout(new BoxLayout(authenticate, BoxLayout.Y_AXIS));
			login.setAlignmentX(Component.CENTER_ALIGNMENT);

			authenticate.add(login);
			loginState = new JLabel();
			loginState.setAlignmentX(Component.CENTER_ALIGNMENT);
			if (username.equals("")){
				loginState.setText("Not Logged In");
			} else {
				loginState.setText("Logged in OK as " + username);
				loginState.setForeground(new Color(0x6E8B3D));
			}
			authenticate.add(loginState);

			//itsMainPanel.setLayout(new GridLayout(0, 1));
			itsMainPanel.setLayout(new BorderLayout());

			itsMainPanel.add(authenticate, BorderLayout.NORTH);
			itsMainPanel.add(itsPointSelector, BorderLayout.CENTER);

			this.add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);
		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.ControlPanel");
			ss.setName("controlSetup");

			Vector<?> points = itsPointSelector.getSelections();
			String p = "";
			if (points.size() > 0) {
				p += points.get(0);
				// Then add rest of point names with a delimiter
				for (int i = 1; i < points.size(); i++) {
					p += ":" + points.get(i);
				}
			}
			ss.put("points", p);

			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			// data validation and verification
			itsInitialSetup = setup;
			if (setup == null) {
				System.err.println("ControlSetupPanel:showSetup: Setup is NULL");
				return;
			}
			if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.ControlPanel")) {
				System.err.println("ControlPanel:showSetup: Setup is for wrong class");
				return;
			}

			// get the stored points from the saved setup
			String p = setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			Vector<String> points = new Vector<String>(stp.countTokens());
			while (stp.hasMoreTokens()) {
				points.add(stp.nextToken());
			}
			itsPointSelector.setSelections(points);

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd == null) {
				return;
			}
			// If the command was one of ours, deal with it.
			if (cmd.equals("Peek")) {
				peekClicked();
			} else if (cmd.equals("Apply")) {
				applyClicked();
			} else if (cmd.equals("Cancel")) {
				cancelClicked();
			} else if (cmd.equals("OK")) {
				okClicked();
			}

			Object source = e.getSource();
			if (source.equals(login)){
				JPanel inputs = new JPanel();
				inputs.setLayout(new BoxLayout(inputs, BoxLayout.Y_AXIS));
				JPanel usernameLine = new JPanel();
				JLabel usernameLabel = new JLabel("Username: ");
				JTextField usernameField = new JTextField(20);
				usernameLine.setLayout(new BoxLayout(usernameLine, BoxLayout.X_AXIS));
				usernameLine.add(usernameLabel);
				usernameLine.add(usernameField);
				JPanel passwordLine = new JPanel();
				JLabel passwordLabel = new JLabel("Password: ");
				JPasswordField passwordField = new JPasswordField(20);
				passwordLine.setLayout(new BoxLayout(passwordLine, BoxLayout.X_AXIS));
				passwordLine.add(passwordLabel);
				passwordLine.add(passwordField);
				inputs.add(usernameLine);
				inputs.add(passwordLine);

				int result = JOptionPane.showConfirmDialog(this, inputs, "Authentication", JOptionPane.OK_CANCEL_OPTION);

				if (result == JOptionPane.OK_OPTION){
					username = usernameField.getText();
					encryptedPassword = passwordEncryptor.encrypt(new String(passwordField.getPassword()));
				}

				if (username.equals("")){
					loginState.setText("Not Logged In");
				} else {
					loginState.setText("Logged in OK as " + username);
					loginState.setForeground(new Color(0x6E8B3D));
				}
			}
		}
	}

	// ///// END NESTED CLASS ///////

	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;
	private Vector<String> itsPoints = new Vector<String>();
	private ControlDisplayPanel contents;
	private JScrollPane scroll;

	/** Constructor. */
	public ControlPanel() {
		this.setLayout(new BorderLayout());
		contents = new ControlDisplayPanel();
		contents.setSize(5000, 5000);
		scroll =  new JScrollPane(contents);
		this.add(scroll, BorderLayout.CENTER);

	}

	// ///// NESTED CLASS: ControlDisplayPanel ///////

	public class ControlDisplayPanel extends JPanel implements ActionListener, PointListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5755259256750234800L;

		public HashMap<JLabel, JLabel> components = new HashMap<JLabel, JLabel>();
		public ControlDisplayPanel(){
			super();
			this.setLayout(new FlowLayout());
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			contents.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
			for (String str : itsPoints){
				JPanel panel = new JPanel();
				panel.setLayout(new FlowLayout());
				JLabel pointLabel = new JLabel("Point: ");
				JLabel point = new JLabel(str);
				JLabel pointvalLabel = new JLabel("Value: ");
				JLabel currValue = new JLabel("#NODATA");
				JLabel valueLabel = new JLabel("Update Value: ");
				JTextField value = new JTextField(20);
				JButton confirm = new JButton("Confirm");
				confirm.setActionCommand(str);
				confirm.addActionListener(this);

				panel.add(pointLabel);
				panel.add(point);
				panel.add(Box.createHorizontalStrut(20));
				panel.add(pointvalLabel);
				panel.add(currValue);
				panel.add(valueLabel);
				panel.add(value);
				panel.add(Box.createHorizontalStrut(20));
				panel.add(confirm);

				components.put(point, currValue);

				contents.add(panel);
			}
			this.add(contents);

		}

		public void register(){
			DataMaintainer.subscribe(itsPoints, this);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String cmd = arg0.getActionCommand();
			Pattern pattern = Pattern.compile("[[a-zA-Z0-9]+\\.]+[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(cmd);
			if (matcher.find()){
				// Valid pointname found
				System.out.println("Match Found! for " + cmd);
				// Send data off however that's done
			}
		}

		@Override
		public void onPointEvent(Object source, PointEvent evt) {
			PointData data = evt.getPointData();
			for (JLabel j : components.keySet()){
				if (j.getText().equals(data.getName())){
					components.get(j).setText(data.getData().toString());
				}
			}
		}
	}

	// ///// END NESTED CLASS ///////


	private void updateContents(){
		contents = new ControlDisplayPanel();
		scroll.setViewportView(contents);

		this.revalidate();
		this.repaint();
	}

	public MonPanelSetupPanel getControls() {
		return new ControlSetupPanel(this, itsFrame);
	}

	public boolean loadSetup(SavedSetup setup) {
		try {
			// check if the setup is suitable for our class
			if (!setup.checkClass(this)) {
				System.err.println("AlarmManagerPanel:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}

			String p = (String) setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			while (stp.hasMoreTokens()) {
				itsPoints.add(stp.nextToken());
			}

			this.updateContents();
			contents.register();

		} catch (final Exception e) {
			e.printStackTrace();
			if (itsFrame != null) {
				JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
						+ "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
						JOptionPane.WARNING_MESSAGE);
			} else {
				System.err.println("AlarmManagerPanel:loadData: " + e.getClass().getName());
			}
			blankSetup();
			return false;
		}
		return true;
	}

	private void blankSetup() {
		itsPoints = new Vector<String>();

	}

	public synchronized SavedSetup getSetup() {
		return itsSetup;
	}

	public void vaporise() {
	}

	public void export(PrintStream p) {
	}

	public String getLabel() {
		return "Control Panel";
	}

}
