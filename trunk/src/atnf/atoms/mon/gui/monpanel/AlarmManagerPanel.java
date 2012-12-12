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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.util.StringTokenizer;
import java.util.Vector;

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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import atnf.atoms.mon.AlarmManager;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.AlarmManager.Alarm;
import atnf.atoms.mon.client.DataMaintainer;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.PointSourceSelector;
import atnf.atoms.mon.util.MailSender;

/**
 * MonPanel class intended to highlight an alarm should the point 
 * <br/>move to alarming state. It should give the user immediate
 * <br/>notification of an alarm, with options to Notify via email, 
 * <br/> Acknowledge or Shelve the alarm. 
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
@SuppressWarnings("serial")
public class AlarmManagerPanel extends MonPanel implements PointListener{

	//temporary registration to ensure that the panel appears manually, 
	//and looks correct at least
	static {
		MonPanel.registerMonPanel("Alarm Manager", AlarmManagerPanel.class);
	}

	private boolean noPriorityAlarms = false;
	private boolean informationAlarms = false;
	private boolean warningAlarms = false;
	private boolean severeAlarms = false;

	private String noPriAlmStr = "noPriority";
	private String infoAlmStr = "information";
	private String warnAlmStr = "warning";
	private String sevAlmStr = "severe";



	// /////////////////////// NESTED CLASS ///////////////////////////////
	/** Nested class to provide GUI controls for configuring the AlarmManagerPanel */
	private class AlarmManagerSetupPanel extends MonPanelSetupPanel implements ItemListener{

		private JCheckBox noPriorityCb = new JCheckBox("\"No Priority\" Alarms");
		private JCheckBox informationCb = new JCheckBox("Information Alarms");
		private JCheckBox warningCb = new JCheckBox("Warning Alarms");
		private JCheckBox severeCb = new JCheckBox("Severe Alarms");
		private JCheckBox allCb = new JCheckBox("All");

		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private PointSourceSelector itsPointSelector = new PointSourceSelector();

		/** Constructor for the AlarmManager setup pane **/
		protected AlarmManagerSetupPanel(AlarmManagerPanel panel, JFrame frame) {
			super(panel, frame);

			itsMainPanel.setLayout(new BorderLayout());
			JPanel selectCategory = new JPanel();
			selectCategory.setLayout(new BoxLayout(selectCategory, BoxLayout.X_AXIS));

			noPriorityCb.setActionCommand("noPriority");
			noPriorityCb.addItemListener(this);

			informationCb.setActionCommand("information");
			informationCb.addItemListener(this);

			warningCb.setActionCommand("warning");
			warningCb.addItemListener(this);

			severeCb.setActionCommand("severe");
			severeCb.addItemListener(this);

			allCb.setActionCommand("all");
			allCb.addItemListener(this);

			// lots of glue so that the layout doesn't look silly when resized horizontally
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(noPriorityCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(informationCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(warningCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(severeCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(allCb);
			selectCategory.add(Box.createHorizontalGlue());

			itsPointSelector.setPreferredSize(new Dimension(340, 150));
			itsMainPanel.add(selectCategory, BorderLayout.NORTH);
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

			Vector points = itsPointSelector.getSelections();
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
			String p = (String) setup.get("points");
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
			if (severeAlarms){
				severeCb.setSelected(true);
			}
			if (noPriorityAlarms && informationAlarms && warningAlarms && severeAlarms){
				allCb .setSelected(true);
			}


		}


		@Override
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getSource();
			boolean states[] = new boolean[4];
			if (source.equals(noPriorityCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					noPriorityAlarms = false;
					states[0] = false;
					states[1] = informationCb.isSelected();
					states[2] = warningCb.isSelected();
					states[3] = severeCb.isSelected();

					allCb.setSelected(false);
					informationCb.setSelected(states[1]);
					warningCb.setSelected(states[2]);
					severeCb.setSelected(states[3]);
				} else if (e.getStateChange() == ItemEvent.SELECTED){
					noPriorityAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(informationCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					informationAlarms = false;

					states[0] = noPriorityCb.isSelected();
					states[1] = false;
					states[2] = warningCb.isSelected();
					states[3] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					warningCb.setSelected(states[2]);
					severeCb.setSelected(states[3]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					informationAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(warningCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					warningAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = false;
					states[3] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					severeCb.setSelected(states[3]);
				} else if (e.getStateChange() == ItemEvent.SELECTED){
					warningAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(severeCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					severeAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = warningCb.isSelected();
					states[3] = false;

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					warningCb.setSelected(states[2]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					severeAlarms = true;
					if (noPriorityAlarms && informationAlarms && warningAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(allCb)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					noPriorityAlarms = true;
					informationAlarms = true;
					warningAlarms = true;
					severeAlarms = true;

					noPriorityCb.setSelected(true);
					informationCb.setSelected(true);
					warningCb.setSelected(true);
					severeCb.setSelected(true);

					//	System.out.println("DEBUG: All is Selected");
				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					noPriorityAlarms = false;
					informationAlarms = false;
					warningAlarms = false;
					severeAlarms = false;

					noPriorityCb.setSelected(false);
					informationCb.setSelected(false);
					warningCb.setSelected(false);
					severeCb.setSelected(false);

					//	System.out.println("DEBUG: All is deselected");
				}
			}
			/*System.out.println("DEBUG: noPriorityAlarms: " + noPriorityAlarms);
			System.out.println("DEBUG: informationAlarms: " + informationAlarms);
			System.out.println("DEBUG: warningAlarms: " + warningAlarms);
			System.out.println("DEBUG: severeAlarms: " + severeAlarms);*/

		}

	}

	// ///////////////////// END NESTED CLASS /////////////////////////////

	// /////////////////////// NESTED CLASS ///////////////////////////////

	protected class AlarmDisplayPanel extends JPanel implements PointListener,
	ActionListener, ListSelectionListener, ItemListener{

		JButton notify = new JButton("NOTIFY");
		JToggleButton ack = new JToggleButton("ACK");
		JToggleButton shelve = new JToggleButton("SHELVE");
		JButton cancel = new JButton("Reset");
		JButton ok = new JButton("Confirm");

		boolean tempAck = false;
		boolean tempShlv = false;

		/**
		 * Constructor for the AlarmDisplayPanel
		 */
		public AlarmDisplayPanel(String type){
			super();
			// LOGIC

			// Vector containing all the points
			Vector<Alarm> localAlarms = AlarmManager.getAllAlarms();
			for (Alarm a : localAlarms){
				System.out.println(a.data.getName());
			}
			// cull the list down to the ones we want for this pane
			// ignores if it's not one of these mentioned here. Either
			// way, the "all" pane should be right even if this is wrong
			if (type.equals("shelved")){
				for (Alarm al : localAlarms){
					if (al.getAlarmStatus() != Alarm.SHELVED){
						localAlarms.remove(al);
					}
				}
			} else if (type.equals("acknowledged")){
				for (Alarm al : localAlarms){
					if (al.getAlarmStatus() != Alarm.ACKNOWLEDGED){
						localAlarms.remove(al);
					}
				}
			} else if (type.equals("alarming")){
				for (Alarm al : localAlarms){
					if (al.getAlarmStatus() != Alarm.ALARMING){
						localAlarms.remove(al);
					}
				}
			} else if (type.equals("nonAlarmed")){
				for (Alarm al : localAlarms){
					if (al.getAlarmStatus() != Alarm.NOT_ALARMED){
						localAlarms.remove(al);
					}
				}
			}

			// Now we cull down to the list of points we actually want to monitor
			// NOTE: THIS ONLY WORKS ASSUMING getName() WILL MATCH THE STRING HELD IN itsPoints
			//Vector<String> localPoints = (Vector<String>) itsPoints.clone();

			System.out.println("Printing out point names:");
			for (String s : itsPoints){
				System.out.println(s);
			}
			/*for (Alarm al : localAlarms){
				boolean notFound = true;
				for (String s : localPoints){
					if (s.equals(al.data.getName())){
						notFound = false;
					}
				}
				if (notFound){
					localAlarms.remove(al);
				}
			}*/

			// At this point we should have only a list of Alarms that correspond to
			// the alarm state we're interested in, and also the actual points we
			// want to look at.





			//LAYOUT

			//Set internals of the panel to appear left to right - 
			// only three internal panes, at least.
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			// Let's start with initialising the three main panels - nothing
			// is in them yet, but let's start here anyway
			AlarmPanel alarmDetails;
			JScrollPane alarmDetailsScroller;
			JPanel buttons = new JPanel();

			// Point List panel
			DefaultListModel lm = new DefaultListModel();

			for (Alarm al : localAlarms){
				lm.addElement(al.data.getName());	
				System.out.println(al.data.getName());
				System.out.println(al.data.getNameOnly() + "\n");
			}

			JList plist = new JList(lm);
			plist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			plist.addListSelectionListener(this);
			JScrollPane plistScroller = new JScrollPane(plist);
			plistScroller.setPreferredSize(new Dimension(170, 200));
			plistScroller.setMinimumSize(new Dimension(140, 100));
			plistScroller.setMaximumSize(new Dimension(170, Integer.MAX_VALUE));


			// Alarm Details Panel
			// defaults to first alarm in list
			alarmDetails = new AlarmPanel();
			alarmDetailsScroller = new JScrollPane(alarmDetails);

			// Button Panel
			JPanel confirmation = new JPanel();
			confirmation.setLayout(new GridLayout(1,2));


			// Make the buttons top to bottom
			buttons.setLayout(new GridLayout(4,1));	
			buttons.setPreferredSize(new Dimension(300, 0));
			buttons.setMinimumSize(new Dimension(140, 100));
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

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
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

						if (!args[0].endsWith("@csiro.au")) throw new InvalidParameterException("Non-CSIRO Email");
						MailSender.sendMail(args[0], args[1], args[2]);
						JOptionPane.showMessageDialog(this, "Email successfully sent!", "Email Notification", JOptionPane.INFORMATION_MESSAGE);
					} catch (InvalidParameterException e0){
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

			} else if (command.equals("ok")){
				//send the commands along to the server

				// Pseudocode
				//for (Alarm a : this.getSelected()){
				//	a.setAcknowledged(tempAck);
				// a.setShelved(tempShlv);
				// }
			} 



		}

		@Override
		public void onPointEvent(Object source, PointEvent evt) {
			// TODO Auto-generated method stub
			for (Alarm al : alarms){
				if (evt.getPointData().getName().equals(al.data.getName())){
					al.updateData(evt.getPointData());
				}
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			// TODO Auto-generated method stub
			if (e.getSource().equals(ack)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					tempAck = true;
				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					tempAck = false;
				}

			} else if (e.getSource().equals(shelve)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					tempShlv = true;
				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					tempShlv = false;
				}
			}
			System.out.println(tempAck);
			System.out.println(tempShlv);
		}
	}

	// ///////////////////// END NESTED CLASS /////////////////////////////

	private Vector<String> itsPoints = new Vector<String>();
	private Vector<Alarm> alarms = new Vector<Alarm>();
	/**
	 * C'tor
	 */
	public AlarmManagerPanel() {
		alarms = AlarmManager.getAllAlarms();
		// Set layout
		this.setLayout(new BorderLayout());

		// Create the tabbed pane and the main panels inside it
		JTabbedPane stateTabs = new JTabbedPane(JTabbedPane.TOP);
		AlarmDisplayPanel all = new AlarmDisplayPanel("all");
		AlarmDisplayPanel nonAlarmed = new AlarmDisplayPanel("nonAlarmed");
		AlarmDisplayPanel acknowledged = new AlarmDisplayPanel("acknowledged");
		AlarmDisplayPanel shelved = new AlarmDisplayPanel("shelved");
		AlarmDisplayPanel alarming = new AlarmDisplayPanel("alarming");

		// Insert the tabs into the tabbed pane
		stateTabs.insertTab("All", null, all, "List of all alarms", 0);
		stateTabs.insertTab("Non-Alarmed", null, nonAlarmed, "List of non-alarming Alarms", 1);
		stateTabs.insertTab("Acknowledged", null, acknowledged, "List of Acknowledged Alarms", 2);
		stateTabs.insertTab("Shelved", null, shelved, "List of shelved Alarms", 3);
		stateTabs.insertTab("Alarming", null , alarming, "List of currently active alarms", 4);

		this.add(stateTabs);

	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#export(java.io.PrintStream)
	 */
	@Override
	public void export(PrintStream p) {
		// TODO Auto-generated method stub

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
		return "Alarm Manager Setup Panel";
	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#getSetup()
	 */
	@Override
	public SavedSetup getSetup() {
		SavedSetup ss = new SavedSetup();
		ss.setClass("atnf.atoms.mon.gui.monpanel.AlarmManagerPanel");
		ss.setName("alarmSetup");

		Vector<String> points = itsPoints;
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
			str = (String) setup.get(sevAlmStr);
			if (str != null){
				if (str.equals("true")){
					severeAlarms = true;
				} else if (str.equals("false")){
					severeAlarms = false;
				}
			}


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
		// TODO Auto-generated method stub
		for (String s : itsPoints){
			DataMaintainer.unsubscribe(s, this);
		}
		itsPoints = new Vector<String>();
		noPriorityAlarms = false;
		informationAlarms = false;
		warningAlarms = false;
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

	/** Basic test application. */
	public static void main(String[] argv) {
		JFrame frame = new JFrame("Alarm Test App");
		// frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

		/*
		 * SavedSetup seemon = new SavedSetup("temp", "atnf.atoms.mon.gui.monpanel.ATPointTable",
		 * "true:3:site.seemon.Lock1:site.seemon.Lock2:site.seemon.Lock3:1:seemon");
		 */
		AlarmManagerPanel ap = new AlarmManagerPanel();
		MonPanelSetupPanel asp = ap.getControls();
		// wd.loadSetup(seemon);
		// frame.getContentPane().add(pt);
		frame.setContentPane(asp);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		/*
		 * try { RelTime sleepy = RelTime.factory(15000000l); sleepy.sleep(); } catch (Exception e) { e.printStackTrace(); }
		 * 
		 * SavedSetup ss = pt.getSetup(); pt.loadSetup(ss);
		 */
	}



}
