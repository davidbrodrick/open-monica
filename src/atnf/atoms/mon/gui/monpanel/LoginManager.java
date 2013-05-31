// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;

/**
 * Simple Monpanel to centrally deal with login information for clients.
 * @author Kalinga Hulugalle
 *
 */
public class LoginManager extends MonPanel {

	private static final long serialVersionUID = -2739850206930107266L;

	static {
		MonPanel.registerMonPanel("Login Manager", LoginManager.class);
	}

	private static String username = "";
	private static String password = "";

	// //// NESTED CLASS: MonitorPointEditorSetupPanel ///////
	protected class LoginManagerSetupPanel extends MonPanelSetupPanel implements ActionListener{

		private static final long serialVersionUID = 3063853264462739633L;

		JButton loginBtn;
		JButton logoutBtn;

		protected LoginManagerSetupPanel(LoginManager lm, JFrame frame) {
			super(lm, frame);
			JPanel itsMainPanel = new JPanel(new GridLayout(2,1));
			JPanel logPanel = new JPanel();
			logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.X_AXIS));
			JPanel logStatusPanel = new JPanel();

			loginBtn = new JButton("Login");
			logoutBtn = new JButton("Logout");
			logoutBtn.setEnabled(false);
			loginBtn.addActionListener(this);
			logoutBtn.addActionListener(this);

			logPanel.add(Box.createHorizontalGlue());
			logPanel.add(loginBtn);
			logPanel.add(Box.createHorizontalGlue());
			logPanel.add(logoutBtn);
			logPanel.add((Box.createHorizontalGlue()));

			status = new JLabel("User Logged in: None");
			logStatusPanel.add(setupStatus);

			itsMainPanel.add(logPanel);
			itsMainPanel.add(logStatusPanel);

			this.add(itsMainPanel, BorderLayout.CENTER);
			setStatus();
		}

		public void actionPerformed(ActionEvent e){
			if (e.getSource() instanceof JButton){
				JButton src = (JButton)e.getSource();
				if (src.equals(loginBtn)){
					String[] creds = MonClientUtil.showLogin(this, username, password);
					if (!creds[0].isEmpty() && !creds[1].isEmpty()){
						setCredentials(creds);
						loginBtn.setEnabled(false);
						logoutBtn.setEnabled(true);
					}
				} else if (src.equals(logoutBtn)){
					setCredentials(new String[]{"",""});
					loginBtn.setEnabled(true);
					logoutBtn.setEnabled(false);
				} else {
					super.actionPerformed(e);
				}
			}
		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.LoginManager");
			ss.setName("loginManager");
			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {}
	}

	/**
	 * Returns the login credentials (username and password) for this client instance
	 * @param parent The JComponent to display the login box on
	 * @return A String[] with the username and password at indices 0 and 1 respectively
	 */
	public static String[] getLoginCredentials(JComponent parent){
		if (password.isEmpty()){
			return MonClientUtil.showLogin(parent, username, password);
		} else {
			return new String[]{username, password};
		}
	}

	/**
	 * Sets the login credentials to the values specified in args[0] and args[1]
	 * @param args The login credentials
	 */
	public static void setCredentials(String[] args){
		if (args.length == 2){
			username = args[0];
			password = args[1];
			setStatus();
		}
	}

	static JLabel status;
	static JLabel setupStatus;

	/**
	 * C'tor
	 */
	public LoginManager(){
		JPanel itsMainPanel = new JPanel();
		status = new JLabel();
		setupStatus = new JLabel();
		itsMainPanel.add(status);
		itsMainPanel.setPreferredSize(new Dimension(this.getWidth(), 20));
		this.add(itsMainPanel);
	}

	/**
	 * Sets the JLabels to the respective login status strings depending if the user is logged
	 * in or not
	 */
	private static void setStatus(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if (!username.isEmpty() && !password.isEmpty()){
					try {
						status.setText("User Logged in: " + username);
						setupStatus.setText("User Logged in: " + username);
					} catch (NullPointerException npe){}
				} else {
					try {
					status.setText("User Logged in: None");
					setupStatus.setText("User Logged in None");
					} catch (NullPointerException npe){}
				}
			}
		});
	}

	@Override
	public void export(PrintStream p) {
		// TODO Auto-generated method stub

	}

	@Override
	public MonPanelSetupPanel getControls() {
		return new LoginManagerSetupPanel(this, itsFrame);
	}

	@Override
	public String getLabel() {
		return "Login Manager";
	}

	@Override
	public SavedSetup getSetup() {
		return null;
	}

	@Override
	public boolean loadSetup(SavedSetup setup) {
		if (!setup.checkClass(this)) {
			System.err.println("LoginManager:loadSetup: setup not for " + this.getClass().getName());
			return false;
		}
		setStatus();
		return true;
	}

	@Override
	public void vaporise() {
		// TODO Auto-generated method stub
	}
}
