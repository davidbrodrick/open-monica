//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.AlarmMaintainer;
import atnf.atoms.mon.client.DataMaintainer;
import atnf.atoms.mon.gui.AlarmPanel;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.PointSourceSelector;
import atnf.atoms.mon.util.MailSender;
import atnf.atoms.time.AbsTime;
import atnf.atoms.time.RelTime;

/**
 * MonPanel class intended to highlight an alarm should the point 
 * <br/>move to alarming state. It should give the user immediate
 * <br/>notification of an alarm, with options to Notify via email, 
 * <br/> Acknowledge or Shelve the alarm. 
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
@SuppressWarnings("serial")
public class AlarmManagerPanel extends MonPanel implements PointListener, AlarmEventListener{

	//temporary registration to ensure that the panel appears manually, 
	//and looks correct at least
	static {
		MonPanel.registerMonPanel("Alarm Manager", AlarmManagerPanel.class);
	}

	private boolean noPriorityAlarms = false;
	private boolean informationAlarms = false;
	private boolean warningAlarms = false;
	private boolean dangerAlarms = false;
	private boolean severeAlarms = false;

	public final static Color ALL_COLOUR = Color.BLACK;
	public final static Color NOT_ALARMED_COLOUR = Color.GRAY;
	public final static Color ACKNOWLEDGED_COLOUR = new Color(0xCDAD00);
	public final static Color SHELVED_COLOUR = new Color(0x6E8B3D);
	public final static Color ALARMING_COLOUR = new Color(0xEE4000);

	private String noPriAlmStr = "noPriority";
	private String infoAlmStr = "information";
	private String warnAlmStr = "warning";
	private String dangAlmStr = "dangerous";
	private String sevAlmStr = "severe";

	private String username = "";
	private String password = "";

	// /////////////////////// NESTED CLASS ///////////////////////////////
	/** Nested class to provide GUI controls for configuring the AlarmManagerPanel */
	private class AlarmManagerSetupPanel extends MonPanelSetupPanel implements ItemListener, ActionListener{

		private JCheckBox noPriorityCb = new JCheckBox("\"No Priority\" Alarms");
		private JCheckBox informationCb = new JCheckBox("Information Alarms");
		private JCheckBox warningCb = new JCheckBox("Warning Alarms");
		private JCheckBox dangerCb = new JCheckBox("Danger Alarms");
		private JCheckBox severeCb = new JCheckBox("Severe Alarms");
		private JCheckBox allCb = new JCheckBox("All");

		private JButton loginBtn = new JButton("Login");
		private JLabel loginState = new JLabel("Not Logged in");

		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private PointSourceSelector itsPointSelector = new PointSourceSelector();

		/** Constructor for the AlarmManager setup pane **/
		protected AlarmManagerSetupPanel(AlarmManagerPanel panel, JFrame frame) {
			super(panel, frame);

			itsMainPanel.setLayout(new BorderLayout());
			JPanel login = new JPanel();
			login.setLayout(new BoxLayout(login, BoxLayout.X_AXIS));
			JPanel selectCategory = new JPanel();
			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
			selectCategory.setLayout(new BoxLayout(selectCategory, BoxLayout.X_AXIS));

			loginState.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			loginBtn.setAlignmentX(JButton.CENTER_ALIGNMENT);
			loginBtn.addActionListener(this);

			noPriorityCb.addItemListener(this);
			informationCb.addItemListener(this);
			warningCb.addItemListener(this);
			dangerCb.addItemListener(this);
			severeCb.addItemListener(this);
			allCb.addItemListener(this);

			// lots of glue so that the layout doesn't look silly when resized horizontally
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(noPriorityCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(informationCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(warningCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(dangerCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(severeCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(allCb);
			selectCategory.add(Box.createHorizontalGlue());

			itsPointSelector.setPreferredSize(new Dimension(340, 150));

			login.add(loginBtn);
			login.add(Box.createHorizontalStrut(20));
			login.add(loginState);

			topPanel.add(login);
			topPanel.add(selectCategory);
			itsMainPanel.add(topPanel, BorderLayout.NORTH);
			itsMainPanel.add(itsPointSelector, BorderLayout.CENTER);



			this.add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);

			// Display the current setup on the GUI
			if (itsInitialSetup != null) {
				showSetup(itsInitialSetup);
			}

		}


		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.AlarmManagerPanel");
			ss.setName("alarmSetup");

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

			if (noPriorityAlarms){
				ss.put(noPriAlmStr, "true");
			} else {
				ss.put(noPriAlmStr, "false");
			}
			if (informationAlarms){
				ss.put(infoAlmStr, "true");
			} else {
				ss.put(infoAlmStr, "false");
			}
			if (warningAlarms){
				ss.put(warnAlmStr, "true");
			} else {
				ss.put(warnAlmStr, "false");
			}
			if (dangerAlarms){
				ss.put(dangAlmStr, "true");
			} else {
				ss.put(dangAlmStr, "false");
			}
			if (severeAlarms){
				ss.put(sevAlmStr, "true");
			} else {
				ss.put(sevAlmStr, "false");
			}


			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {

			// data validation and verification
			itsInitialSetup = setup;
			if (setup == null) {
				System.err.println("AlarmManagerSetupPanel:showSetup: Setup is NULL");
				return;
			}
			if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.AlarmManagerPanel")) {
				System.err.println("AlarmManagerSetupPanel:showSetup: Setup is for wrong class");
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

			// Check the various boolean values for the states of the checkboxes
			String s = setup.get(noPriAlmStr);
			if (s != null) {
				if (s.equals("true")){
					noPriorityAlarms = true;
				} else if (s.equals("false")){
					noPriorityAlarms = false;
				}
			}

			s = setup.get(infoAlmStr);
			if (s != null) {
				if (s.equals("true")){
					informationAlarms = true;
				} else if (s.equals("false")){
					informationAlarms = false;
				}
			}

			s = setup.get(warnAlmStr);
			if (s != null) {
				if (s.equals("true")){
					warningAlarms = true;
				} else if (s.equals("false")){
					warningAlarms = false;
				}
			}

			s = setup.get(dangAlmStr);
			if (s != null) {
				if (s.equals("true")){
					dangerAlarms = true;
				} else if (s.equals("false")){
					dangerAlarms = false;
				}
			}

			s = setup.get(sevAlmStr);
			if (s != null) {
				if (s.equals("true")){
					severeAlarms = true;
				} else if (s.equals("false")){
					severeAlarms = false;
				}
			}

			if (noPriorityAlarms){
				noPriorityCb.setSelected(true);
			}
			if (informationAlarms){
				informationCb.setSelected(true);
			}
			if (warningAlarms){
				warningCb.setSelected(true);
			}
			if (dangerAlarms){
				dangerCb.setSelected(true);
			}
			if (severeAlarms){
				severeCb.setSelected(true);
			}
			if (noPriorityAlarms && informationAlarms && warningAlarms && dangerAlarms && severeAlarms){
				allCb .setSelected(true);
			}


		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source.equals(loginBtn)){

				JPanel inputs = new JPanel();
				inputs.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 0.5;
				gbc.gridx = 0;
				gbc.gridy = 0;
				JLabel usernameLabel = new JLabel("Username: ");
				JTextField usernameField = new JTextField(20);
				inputs.add(usernameLabel, gbc);
				gbc.gridx = 1;
				gbc.gridwidth = 3;
				inputs.add(usernameField, gbc);
				JLabel passwordLabel = new JLabel("Password: ");
				JPasswordField passwordField = new JPasswordField(20);
				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.gridwidth = 1;
				inputs.add(passwordLabel, gbc);
				gbc.gridwidth = 3;
				gbc.gridx = 1;
				inputs.add(passwordField, gbc);

				int result = JOptionPane.showConfirmDialog(this, inputs, "Authentication", JOptionPane.OK_CANCEL_OPTION);

				if (result == JOptionPane.OK_OPTION){
					try {
						username = usernameField.getText();
						password = new String(passwordField.getPassword());
						if (username.equals("") || password.equals("")){
							throw new IllegalArgumentException(); 
						}
					} catch (IllegalArgumentException dfe){
						username = "";
						password = "";
						JOptionPane.showMessageDialog(this, "Incorrect Username or Password", "ERROR LOGGING IN", JOptionPane.ERROR_MESSAGE);
					}
				}

				if (username.equals("")){
					loginState.setText("Not Logged In");
				} else {
					loginState.setText("Username set as " + username);
					loginState.setForeground(new Color(0x6E8B3D));
				}
			}

			if (username.equals("") || password.equals("")){
				JOptionPane.showMessageDialog(this, "You are required to log in with a valid username and password", "AUTHENTICATION ERROR", JOptionPane.ERROR_MESSAGE);
			}else{
				super.actionPerformed(e);	
			}
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getSource();
			boolean states[] = new boolean[5];
			if (source.equals(noPriorityCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					noPriorityAlarms = false;
					states[0] = false;
					states[1] = informationCb.isSelected();
					states[2] = warningCb.isSelected();
					states[3] = dangerCb.isSelected();
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);
					informationCb.setSelected(states[1]);
					warningCb.setSelected(states[2]);
					dangerCb.setSelected(states[3]);
					severeCb.setSelected(states[4]);
				} else if (e.getStateChange() == ItemEvent.SELECTED){
					noPriorityAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && dangerAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(informationCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					informationAlarms = false;

					states[0] = noPriorityCb.isSelected();
					states[1] = false;
					states[2] = warningCb.isSelected();
					states[3] = dangerCb.isSelected();
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					warningCb.setSelected(states[2]);
					dangerCb.setSelected(states[3]);
					severeCb.setSelected(states[4]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					informationAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && dangerAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(warningCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					warningAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = false;
					states[3] = dangerCb.isSelected();
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					dangerCb.setSelected(states[3]);
					severeCb.setSelected(states[4]);
				} else if (e.getStateChange() == ItemEvent.SELECTED){
					warningAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && dangerAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(dangerCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					severeAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = warningCb.isSelected();
					states[3] = false;
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					warningCb.setSelected(states[2]);
					severeCb.setSelected(states[4]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					dangerAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && dangerAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(severeCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					severeAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = warningCb.isSelected();
					states[3] = dangerCb.isSelected();
					states[4] = false;

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					warningCb.setSelected(states[2]);
					dangerCb.setSelected(states[3]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					severeAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && dangerAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(allCb)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					noPriorityAlarms = true;
					informationAlarms = true;
					warningAlarms = true;
					dangerAlarms = true;
					severeAlarms = true;

					noPriorityCb.setSelected(true);
					informationCb.setSelected(true);
					warningCb.setSelected(true);
					dangerCb.setSelected(true);
					severeCb.setSelected(true);

				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					noPriorityAlarms = false;
					informationAlarms = false;
					warningAlarms = false;
					dangerAlarms = false;
					severeAlarms = false;

					noPriorityCb.setSelected(false);
					informationCb.setSelected(false);
					warningCb.setSelected(false);
					dangerCb.setSelected(false);
					severeCb.setSelected(false);

				}
			}

		}

	}

	// ///////////////////// END NESTED CLASS /////////////////////////////

	// /////////////////////// NESTED CLASS ///////////////////////////////

	protected class AlarmDisplayPanel extends JPanel implements	ActionListener, ListSelectionListener, ItemListener{

		int type = -1;

		JButton notify = new JButton("NOTIFY");
		JToggleButton ack = new JToggleButton("ACK");
		JToggleButton shelve = new JToggleButton("SHELVE");
		JButton cancel = new JButton("Reset");
		JButton ok = new JButton("Confirm");

		boolean tempAck = false;
		boolean tempShlv = false;
		boolean ackChanged = false;
		boolean shlvChanged = false;

		Vector<Alarm> selectedAlarms = new Vector<Alarm>();
		Vector<String> localPoints = new Vector<String>();

		DefaultListModel localListModel = new DefaultListModel();
		JList plist;

		JScrollPane alarmDetailsScroller;

		/**
		 * Constructor for the AlarmDisplayPanel
		 */
		public AlarmDisplayPanel(String t){
			super();

			if (!t.equals("all")) this.type = this.getType(t);

			localListModel = itsListModel;

			//LAYOUT

			//Set internals of the panel to appear left to right - 
			// only three internal panes, at least.
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			// Let's start with initialising the three main panels - nothing
			// is in them yet, but let's start here anyway
			JPanel buttons = new JPanel();

			// Point List panel
			plist = new JList(localListModel);
			plist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			plist.addListSelectionListener(this);
			JScrollPane plistScroller = new JScrollPane(plist);
			plistScroller.setPreferredSize(new Dimension(170, 200));
			plistScroller.setMinimumSize(new Dimension(140, 100));
			plistScroller.setMaximumSize(new Dimension(170, Integer.MAX_VALUE));

			JPanel alarmPanels = new JPanel();
			// Alarm Details Panel
			alarmPanels.add(new AlarmPanel());


			alarmDetailsScroller = new JScrollPane(alarmPanels);

			// Button Panel
			JPanel confirmation = new JPanel();
			confirmation.setLayout(new GridLayout(1,2));


			// Make the buttons top to bottom
			buttons.setLayout(new GridLayout(4,1));	
			buttons.setPreferredSize(new Dimension(300, 0));
			buttons.setMinimumSize(new Dimension(300, 100));
			buttons.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));


			// Let's add some stuff to the button panel!
			notify.setToolTipText("Notify someone about these alarms through email.");
			notify.setFont(new Font("Sans Serif", Font.BOLD, 48));
			ack.setToolTipText("Acknowledge these alarms.");
			ack.setFont(new Font("Sans Serif", Font.BOLD, 48));
			shelve.setToolTipText("Shelve the selected alarms.");
			shelve.setFont(new Font("Sans Serif", Font.BOLD, 48));
			cancel.setToolTipText("Reset your selections");
			cancel.setFont(new Font("Sans Serif", Font.ITALIC, 28));
			ok.setToolTipText("Execute the selected actions on these alarms.");
			ok.setFont(new Font("Sans Serif", Font.ITALIC, 28));

			// set the action commands that are sent when these buttons are pressed
			notify.setActionCommand("notify");
			cancel.setActionCommand("cancel");
			ok.setActionCommand("ok");

			// now register the buttons with the actionlistener
			notify.addActionListener(this);
			ack.addItemListener(this);
			shelve.addItemListener(this);
			cancel.addActionListener(this);
			ok.addActionListener(this);

			//let's add the buttons to the button pane now!
			confirmation.add(cancel);
			confirmation.add(ok);

			buttons.add(notify);
			buttons.add(ack);
			buttons.add(shelve);
			buttons.add(confirmation);

			//Add the big panels to the tabbed pane now
			this.add(plistScroller);
			this.add(alarmDetailsScroller);
			this.add(buttons);

		}

		private int getType(String type) {
			int res;
			if (type.equals("nonAlarmed")){
				res = Alarm.NOT_ALARMED;
			} else if (type.equals("acknowledged")){
				res = Alarm.ACKNOWLEDGED;
			} else if (type.equals("shelved")){
				res = Alarm.SHELVED;
			} else if (type.equals("alarming")){
				res = Alarm.ALARMING;
			} else {
				System.err.println("Alarm State Unrecognised");
				throw (new InvalidParameterException("Alarm State unrecognised"));
			}
			return res;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();

			if (command.equals("notify")){
				//notify via email
				String[] args = new String[3];
				JPanel inputs = new JPanel();
				JPanel tFieldsp = new JPanel();
				tFieldsp.setLayout(new GridLayout(3,2,0,5));
				JLabel lemail = new JLabel("CSIRO Recipient: ");
				JLabel lsubject = new JLabel("Subject: ");
				JLabel lbody = new JLabel("Body Text: ");
				lemail.setHorizontalAlignment(JLabel.LEFT);
				lsubject.setHorizontalAlignment(JLabel.LEFT);
				JTextField email = new JTextField(20);
				JTextField subject = new JTextField(20);
				JTextArea body = new JTextArea(5,20);
				Border border = BorderFactory.createLineBorder(Color.BLACK);
				body.setBorder(border);
				email.setBorder(border);
				subject.setBorder(border);

				tFieldsp.add(lemail);
				tFieldsp.add(email);
				tFieldsp.add(lsubject);
				tFieldsp.add(subject);
				tFieldsp.add(lbody);

				inputs.setLayout(new BoxLayout(inputs,BoxLayout.Y_AXIS));
				inputs.add(tFieldsp);
				inputs.add(body);

				int result = JOptionPane.showConfirmDialog(this, inputs, "Please fill out the following fields: ", JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION){
					args[0] = email.getText();
					args[1] = subject.getText();
					args[2] = body.getText();
					args[2] += "\n\n\n\t -- Sent via MoniCA Java Client";
					try {		

						//if (!args[0].endsWith("@csiro.au")) throw new IllegalArgumentException("Non-CSIRO Email");//Checks for correct email address
						MailSender.sendMail(args[0], args[1], args[2]);
						JOptionPane.showMessageDialog(this, "Email successfully sent!", "Email Notification", JOptionPane.INFORMATION_MESSAGE);
					} catch (IllegalArgumentException e0){
						JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
								"You need to send this to a CSIRO email address!", "Email Notification", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e1){
						JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
								"You  may want to check your connection settings.", "Email Notification", JOptionPane.ERROR_MESSAGE);
					}
				}

			} else if (command.equals("cancel")){
				// cancel the options taken, unselect all buttons that may
				// have been selected etc.
				tempShlv = false;
				tempAck = false;
				ack.setSelected(false);
				shelve.setSelected(false);
				plist.clearSelection();

			} else if (command.equals("ok")){
				//send the commands along to the server
				int res = 0;
				if (ackChanged && ack.isSelected()){
					res = JOptionPane.showConfirmDialog(this, "This will set all selected points to Acknowledged. Is this OK?", "Confirmation" , JOptionPane.YES_NO_OPTION);
				} else if (shlvChanged && shelve.isSelected()){
					res = JOptionPane.showConfirmDialog(this, "This will set all selected points to Shelved. Is this OK?", "Confirmation", JOptionPane.YES_NO_OPTION);
				} else if (ackChanged && !ack.isSelected()){
					res = JOptionPane.showConfirmDialog(this, "This will de-Acknowledge all selected points. Is this OK?", "Confirmation" , JOptionPane.YES_NO_OPTION);
				} else if (shlvChanged && !shelve.isSelected()){
					res = JOptionPane.showConfirmDialog(this, "This will un-Shelve all selected points. Is this OK?", "Confirmation" , JOptionPane.YES_NO_OPTION);
				}
				if (res == JOptionPane.YES_OPTION){
					ackChanged = false;
					shlvChanged = false;
					for (Object s : plist.getSelectedValues()){
						//System.out.println("Username: " + username + "\nPassword: " + password);
						AlarmMaintainer.setAcknowledged(s.toString(), tempAck, username, password);
						AlarmMaintainer.setShelved(s.toString(), tempShlv, username, password);
					}
					updateListModels();
					this.updateAlarmPanels();
				}
			} 



		}

		private void updateAlarmPanels(){
			JPanel newPanel = new JPanel();
			newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));

			Object[] pointNames = plist.getSelectedValues();
			for (Object o : pointNames){
				AlarmPanel a = new AlarmPanel(o.toString());
				a.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));
				newPanel.add(a);
			}

			alarmDetailsScroller.add(newPanel);
			alarmDetailsScroller.setViewportView(newPanel);
			alarmDetailsScroller.revalidate();
			alarmDetailsScroller.repaint();

		}


		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() == false){
				try {
					if (AlarmMaintainer.getAlarm(localListModel.get(plist.getSelectedIndex()).toString()).getAlarmStatus() == Alarm.ACKNOWLEDGED){
						tempAck = true;
						ack.setSelected(true);
						tempShlv = false;
						shelve.setSelected(false);
					} else if (AlarmMaintainer.getAlarm(localListModel.get(plist.getSelectedIndex()).toString()).getAlarmStatus() == Alarm.SHELVED){
						tempShlv = true;
						shelve.setSelected(true);
						tempAck = false;
						ack.setSelected(false);
					} else {
						tempShlv = false;
						shelve.setSelected(false);
						tempAck = false;
						ack.setSelected(false);
					}
				} catch (ArrayIndexOutOfBoundsException aioobe){
					// Not sure why this keeps triggering, doesn't seem to have any negative effect... yet.
					// Probably something to do with removing the element of the list in the JButton thread
				}
				this.updateAlarmPanels();
			}
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			ackChanged = false;
			shlvChanged = false;
			if (e.getSource().equals(ack)){
				ackChanged = true;
				if (e.getStateChange() == ItemEvent.SELECTED){
					tempAck = true;
					tempShlv = false;
					shelve.setSelected(false);
				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					tempAck = false;
				}

			} else if (e.getSource().equals(shelve)){
				shlvChanged = true;
				if (e.getStateChange() == ItemEvent.SELECTED){
					tempShlv = true;
					tempAck = false;
					ack.setSelected(false);
				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					tempShlv = false;
				}
			}
		}

		public void updateListModel() {
			this.setListModel(itsListModel);
			Vector<String> newList = new Vector<String>();
			for (int i = 0; i < localListModel.getSize(); i ++){
				String s = (String) localListModel.get(i);
				if (AlarmMaintainer.getAlarm(s).getAlarmStatus() == this.type){
					newList.add(s);
				}
			}

			localListModel = new DefaultListModel();
			localListModel.setSize(newList.size());
			for (int i = 0; i < newList.size(); i++){
				localListModel.set(i, newList.get(i));
			}

			//plist.setListData(newList);
			plist.setModel(localListModel);
			plist.revalidate();
			plist.repaint();

		}

		private void setListModel(DefaultListModel lm){
			localListModel = lm;
		}

	}

	// ///////////////////// END NESTED CLASS /////////////////////////////

	private Vector<String> itsPoints = new Vector<String>();
	private DefaultListModel itsListModel = new DefaultListModel();
	
	private JTabbedPane stateTabs;
	private AlarmDisplayPanel all;
	private AlarmDisplayPanel nonAlarmed;
	private AlarmDisplayPanel acknowledged;
	private AlarmDisplayPanel shelved;
	private AlarmDisplayPanel alarming;

	private AudioWarning  klaxon = new AudioWarning();

	/**
	 * C'tor
	 */
	public AlarmManagerPanel() {
		// Set layout
		this.setLayout(new BorderLayout());

		stateTabs = new JTabbedPane(JTabbedPane.TOP);
		all = new AlarmDisplayPanel("all");
		nonAlarmed = new AlarmDisplayPanel("nonAlarmed");
		acknowledged = new AlarmDisplayPanel("acknowledged");
		shelved = new AlarmDisplayPanel("shelved");
		alarming = new AlarmDisplayPanel("alarming");

		// Insert the tabs into the tabbed pane
		stateTabs.insertTab("All", null, all, "List of all alarms", 0);
		stateTabs.insertTab("Non-Alarmed", null, nonAlarmed, "List of non-alarming Alarms", 1);
		stateTabs.insertTab("Acknowledged", null, acknowledged, "List of Acknowledged Alarms", 2);
		stateTabs.insertTab("Shelved", null, shelved, "List of Shelved Alarms", 3);
		stateTabs.insertTab("Alarming", null , alarming, "List of Currently Active alarms", 4);

		stateTabs.setForegroundAt(0, AlarmManagerPanel.ALL_COLOUR);
		stateTabs.setForegroundAt(1, AlarmManagerPanel.NOT_ALARMED_COLOUR);
		stateTabs.setForegroundAt(2, AlarmManagerPanel.ACKNOWLEDGED_COLOUR);
		stateTabs.setForegroundAt(3, AlarmManagerPanel.SHELVED_COLOUR);
		stateTabs.setForegroundAt(4, AlarmManagerPanel.ALARMING_COLOUR);

		this.add(stateTabs);
	}

	public void updateListModels(){
		nonAlarmed.updateListModel();
		acknowledged.updateListModel();
		shelved.updateListModel();
		alarming.updateListModel();
	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#export(java.io.PrintStream)
	 */
	@Override
	public void export(PrintStream p) {
		final String rcsid = "$Id: $";
		p.println("#Dump from AlarmManagerPanel " + rcsid);
		p.println("#Data dumped at " + (new AbsTime().toString(AbsTime.Format.UTC_STRING)));
		// itsListModel.export(p);
		p.println();
		p.println();
	}

	/**
	 * @see atnf.atoms.mon.gui.MonPanel#getControls()
	 */
	@Override
	public MonPanelSetupPanel getControls() {
		return new AlarmManagerSetupPanel(this, itsFrame);
	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#getLabel()
	 */
	@Override
	public String getLabel() {
		return "Alarm Manager Panel";
	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#getSetup()
	 */
	@Override
	public synchronized SavedSetup getSetup() {
		SavedSetup ss = new SavedSetup();
		ss.setClass("atnf.atoms.mon.gui.monpanel.AlarmManagerPanel");
		ss.setName("alarmSetup");

		DefaultListModel listPoints = itsListModel;
		String p = "";
		if (listPoints.size() > 0) {
			p += listPoints.get(0);
			// Then add rest of point names with a delimiter
			for (int i = 1; i < listPoints.size(); i++) {
				p += ":" + listPoints.get(i);
			}
		}
		ss.put("points", p);

		if (noPriorityAlarms){
			ss.put(noPriAlmStr, "true");
		} else {
			ss.put(noPriAlmStr, "false");
		}
		if (informationAlarms){
			ss.put(infoAlmStr, "true");
		} else {
			ss.put(infoAlmStr, "false");
		}
		if (warningAlarms){
			ss.put(warnAlmStr, "true");
		} else {
			ss.put(warnAlmStr, "false");
		}
		if (dangerAlarms){
			ss.put(dangAlmStr, "true");
		} else {
			ss.put(dangAlmStr, "false");
		}
		if (severeAlarms){
			ss.put(sevAlmStr, "true");
		} else {
			ss.put(sevAlmStr, "false");
		}

		return ss;
	}

	/** 
	 * String containing information pertaining to this subclass, such as
	 * point information and which categories of alarms are enabled
	 * @return <strong>true</strong> if the setup could be parsed, <strong>false</strong> otherwise. 
	 * @see atnf.atoms.mon.gui.MonPanel#loadSetup(atnf.atoms.mon.SavedSetup)
	 */
	@Override
	public synchronized boolean loadSetup(final SavedSetup setup) {
		try {
			// check if the setup is suitable for our class
			if (!setup.checkClass(this)) {
				System.err.println("AlarmManagerPanel:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}

			/*if (itsUser == null){
				itsUser = JOptionPane.showInputDialog("Please input your username: ");
			}*/

			// Get the list of points to be monitored
			String p = (String) setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			while (stp.hasMoreTokens()) {
				itsPoints.add(stp.nextToken());
			}

			DataMaintainer.subscribe(itsPoints, this);

			// Get which categories of alarms to monitor
			String str;
			str = (String) setup.get(noPriAlmStr);
			if (str != null){
				if (str.equals("true")){
					noPriorityAlarms = true;
				} else if (str.equals("false")){
					noPriorityAlarms = false;
				}
			}
			str = (String) setup.get(infoAlmStr);
			if (str != null){
				if (str.equals("true")){
					informationAlarms = true;
				} else if (str.equals("false")){
					informationAlarms = false;
				}
			}
			str = (String) setup.get(warnAlmStr);
			if (str != null){
				if (str.equals("true")){
					warningAlarms = true;
				} else if (str.equals("false")){
					warningAlarms = false;
				}
			}
			str = (String) setup.get(dangAlmStr);
			if (str != null){
				if (str.equals("true")){
					dangerAlarms = true;
				} else if (str.equals("false")){
					dangerAlarms = false;
				}
			}
			str = (String) setup.get(sevAlmStr);
			if (str != null){
				if (str.equals("true")){
					severeAlarms = true;
				} else if (str.equals("false")){
					severeAlarms = false;
				}
			}

			Vector<String> badPoints = new Vector<String>();
			for (String s : itsPoints){

				if (PointDescription.getPoint(s).getPriority() == -1 && !noPriorityAlarms){
					badPoints.add(s);
					DataMaintainer.unsubscribe(s, this);
				}
				if (PointDescription.getPoint(s).getPriority() == 0 && !informationAlarms){
					badPoints.remove(s);
					DataMaintainer.unsubscribe(s, this);
				}
				if (PointDescription.getPoint(s).getPriority() == 1 && !warningAlarms){
					badPoints.remove(s);
					DataMaintainer.unsubscribe(s, this);
				}
				if (PointDescription.getPoint(s).getPriority() == 2 && !dangerAlarms){
					badPoints.remove(s);
					DataMaintainer.unsubscribe(s, this);
				}
				if (PointDescription.getPoint(s).getPriority() == 3 && !severeAlarms){
					badPoints.remove(s);
					DataMaintainer.unsubscribe(s, this);
				}

			}

			for (String bStr : badPoints){
				itsPoints.remove(bStr);
			}

			itsListModel.setSize(itsPoints.size());
			for (int i = 0; i < itsPoints.size(); i++){
				itsListModel.setElementAt(itsPoints.get(i), i);
			}

			for (String pd : itsPoints){
				AlarmMaintainer.setAlarm(PointDescription.getPoint(pd));
			}

			nonAlarmed.updateListModel();
			acknowledged.updateListModel();
			shelved.updateListModel();
			alarming.updateListModel();

			AlarmMaintainer.addListener(this);

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
		for (String s : itsPoints){
			DataMaintainer.unsubscribe(s, this);
		}

		AlarmMaintainer.removeListener(this);

		itsPoints = new Vector<String>();
		itsListModel = new DefaultListModel();
		noPriorityAlarms = false;
		informationAlarms = false;
		warningAlarms = false;
		dangerAlarms = false;
		severeAlarms = false;

	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#vaporise()
	 */
	@Override
	public void vaporise() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPointEvent(Object source, PointEvent evt) {
		// TODO Auto-generated method stub
	}

	/** Play an audio sample on the sound card. */
	private boolean playAudio(String resname) {
		RelTime sleep = RelTime.factory(1000000);
		try {
			InputStream in = Watchdog.class.getClassLoader().getResourceAsStream(resname);
			AudioInputStream soundIn = AudioSystem.getAudioInputStream(in);
			DataLine.Info info = new DataLine.Info(Clip.class, soundIn.getFormat());
			Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(soundIn);
			sleep.sleep(); // Clips start of clip without this
			clip.start();
			// Wait until clip is finished then release the sound card
			while (clip.isActive()) {
				Thread.yield();
			}
			clip.drain();
			sleep.sleep(); // Clips end of clip without this
			clip.close();
		} catch (Exception e) {
			System.err.println("AlarmManagerPanel.playAudio: " + e.getClass());
			return false;
		}
		return true;
	}

	@Override
	public void onAlarmEvent(AlarmEvent event) {
		// TODO Auto-generated method stub
		this.updateListModels();
		Alarm thisAlarm = event.getAlarm();
		if (thisAlarm.isAlarming()){
			stateTabs.setSelectedIndex(4); //if alarming, automatically switch over to the "Alarming" tab
			if (!klaxon.isAlive()){
				klaxon.start();
			}
		}
	}

	// /////////////////////// NESTED CLASS ///////////////////////////////
	public class AudioWarning extends Thread {
		public void run() {
			RelTime sleep = RelTime.factory(10000000);
			while (!alarming.localListModel.isEmpty()) {
				playAudio("atnf/atoms/mon/gui/monpanel/watchdog.wav");
			}
			try {
				sleep.sleep();
			} catch (Exception e) {
			}
		}
	}
}

// ///////////////////// END NESTED CLASS /////////////////////////////

