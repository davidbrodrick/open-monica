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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.AlarmMaintainer;
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
public class AlarmManagerPanel extends MonPanel implements AlarmEventListener{

	static {
		MonPanel.registerMonPanel("Alarm Manager", AlarmManagerPanel.class);
	}

	public static HashSet<String> ignoreList = new HashSet<String>();

	private boolean muteOn = false;
	private boolean noPriorityAlarms = false;
	private boolean informationAlarms = false;
	private boolean warningAlarms = false;
	private boolean dangerAlarms = false;
	private boolean severeAlarms = false;

	/** Colour for the "All" tab. Black. */
	public final static Color ALL_COLOUR = Color.BLACK;
	/** Colour for the "Not Alarming" tab. Grey. */
	public final static Color NOT_ALARMED_COLOUR = Color.GRAY;
	/** Colour for the "Acknowledged" tab. Yellow. */
	public final static Color ACKNOWLEDGED_COLOUR = new Color(0xCDAD00);
	/** Colour for the "Shelved" tab. Green. */
	public final static Color SHELVED_COLOUR = new Color(0x6E8B3D);
	/** Colour for the "Alarming" tab. Reddish-Orange. */
	public final static Color ALARMING_COLOUR = new Color(0xEE4000);
	/** Colour for the "Ignore" tab. Blue. */
	private static final Color IGNORE_COLOUR = new Color(0x0066FF);

	private String noPriAlmStr = "noPriority";
	private String infoAlmStr = "information";
	private String warnAlmStr = "minor";
	private String dangAlmStr = "major";
	private String sevAlmStr = "severe";

	private String username = "";
	private String password = "";

	// /////////////////////// NESTED CLASS ///////////////////////////////
	/** Nested class to provide GUI controls for configuring the AlarmManagerPanel */
	private class AlarmManagerSetupPanel extends MonPanelSetupPanel implements ItemListener, ActionListener{

		private JCheckBox selectAllPointCb = new JCheckBox("Select All Points");
		private JCheckBox allowAutoAlarms = new JCheckBox("Allow Automatic Notifications");
		private JLabel catLabel = new JLabel("Select Alarm Categories: ");


		private JCheckBox noPriorityCb = new JCheckBox("\"No Priority\"");
		private JCheckBox informationCb = new JCheckBox("Information");
		private JCheckBox warningCb = new JCheckBox("Minor");
		private JCheckBox dangerCb = new JCheckBox("Major");
		private JCheckBox severeCb = new JCheckBox("Severe");
		private JCheckBox allCb = new JCheckBox("All");

		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private AMPPointSourceSelector itsPointSelector = new AMPPointSourceSelector();

		/** Constructor for the AlarmManager setup pane **/
		protected AlarmManagerSetupPanel(AlarmManagerPanel panel, JFrame frame) {
			super(panel, frame);

			itsMainPanel.setLayout(new BorderLayout());
			JPanel selectCategory = new JPanel();
			JPanel catInfo = new JPanel();
			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
			selectCategory.setLayout(new BoxLayout(selectCategory, BoxLayout.X_AXIS));
			GridBagConstraints gbc = new GridBagConstraints();
			catInfo.setLayout(new GridBagLayout());
			gbc.insets = new Insets(0,10,0,10);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.weightx = 0.5;
			gbc.gridx = 0;
			gbc.gridy = 0;

			Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
			fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
			catLabel.setFont(new Font("Sans Serif", Font.BOLD, catLabel.getFont().getSize()).deriveFont(fontAttributes));
			catInfo.add(catLabel, gbc);
			gbc.gridx = 1;
			if (AlarmMaintainer.autoAlarms) allowAutoAlarms.setSelected(true);
			allowAutoAlarms.addItemListener(this);
			catInfo.add(allowAutoAlarms, gbc);

			gbc.gridx = 2;
			gbc.anchor = GridBagConstraints.EAST;
			catInfo.add(selectAllPointCb, gbc);
			selectAllPointCb.addItemListener(this);
			selectAllPointCb.setHorizontalAlignment(SwingConstants.RIGHT);

			noPriorityCb.addItemListener(this);
			informationCb.addItemListener(this);
			warningCb.addItemListener(this);
			dangerCb.addItemListener(this);
			severeCb.addItemListener(this);
			allCb.addItemListener(this);

			// lots of glue so that the layout doesn't look silly when resized horizontally
			//selectCategory.add(Box.createHorizontalGlue());
			//selectCategory.add(noPriorityCb);
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
			selectCategory.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

			allCb.setSelected(true);
			selectAllPointCb.setSelected(true);

			itsPointSelector.setPreferredSize(new Dimension(340, 150));

			topPanel.add(catInfo);
			topPanel.add(selectCategory);
			itsMainPanel.add(topPanel, BorderLayout.NORTH);
			itsMainPanel.add(itsPointSelector, BorderLayout.CENTER);

			this.add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);

			// Display the current setup on the GUI
			if (itsInitialSetup != null) {
				showSetup(itsInitialSetup);
			}

		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.AlarmManagerPanel");
			ss.setName("alarmSetup");

			String p = "";

			if (allCb.isSelected()){
				allCb.doClick();
				allCb.doClick();
				allCb.setSelected(true);
			}

			if (selectAllPointCb.isSelected()){
				selectAllPointCb.doClick();
				selectAllPointCb.doClick();
				selectAllPointCb.setSelected(true);
				//weird hack to get the default settings to work

				String[] points = itsPointSelector.getAllPointNames();
				if (points.length > 0) {
					p += points[0];
					// Then add rest of point names with a delimiter
					for (int i = 1; i < points.length; i++) {
						p += ":" + points[i];
					}
				}
			} else {
				Vector<?> points = itsPointSelector.getSelections();
				if (points.size() > 0) {
					p += points.get(0);
					// Then add rest of point names with a delimiter
					for (int i = 1; i < points.size(); i++) {
						p += ":" + points.get(i);
					}
				}

			}

			ss.put("points", p);

			/*if (noPriorityAlarms){
				ss.put(noPriAlmStr, "true");
			} else {
				ss.put(noPriAlmStr, "false");
			}*/
			ss.put(noPriAlmStr, "false");
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

			if (allowAutoAlarms.isSelected()){
				ss.put("autoAlarms", "true");
			} else {
				ss.put("autoAlarms", "false");
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

			/*// Turns on automatic notifications 
			if (setup.get("autoAlarms").equals("true")){
				if (!allowAutoAlarms.isSelected()){
					allowAutoAlarms.doClick();
				}
			} else {
				if (allowAutoAlarms.isSelected()){
					allowAutoAlarms.doClick();
				}
			}*/
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
			} else if (source.equals(allowAutoAlarms)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					AlarmMaintainer.autoAlarms = true;
				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					AlarmMaintainer.autoAlarms = false;
				}
			}
		}

		// /////////////////////// NESTED NESTED CLASS ///////////////////////////////

		/**
		 * Class to support the changing of the checkbox state when a selection in the Tree
		 * is made. Small, but fixes an incredibly annoying behaviour without it.
		 */
		public class AMPPointSourceSelector extends PointSourceSelector{

			/**
			 * Constructor, nothing fancy.
			 */
			public AMPPointSourceSelector(){
				super();
			}

			/**
			 * Overrides the superclass's actionPerformed() method, though it uses
			 * the same behaviour, just adds a little to it.
			 * @param e The ActionEvent object that encapsulates information about the action
			 */
			@Override
			public void actionPerformed(ActionEvent e){
				super.actionPerformed(e);
				if (e.getActionCommand().equals("Add")){
					selectAllPointCb.setSelected(false);
				}
			}

		}
		// ///////////////////// END NESTED NESTED CLASS /////////////////////////////
	}
	// ///////////////////// END NESTED CLASS /////////////////////////////

	// /////////////////////// NESTED CLASS ///////////////////////////////

	/**
	 * Class that encapsulates all the details with displaying the contents of the display,
	 * including the JList of the points, and the buttons. The actual alarm details are handled
	 * by the AlarmPanel class
	 * @see AlarmPanel
	 */
	protected class AlarmDisplayPanel extends JPanel implements	ActionListener, ListSelectionListener, ItemListener{

		private static final int ALL = 98;
		private static final int IGNORED = 99;
		boolean selectionIsShelved = false;
		boolean selectionIsIgnored = false;
		int type = -1;

		String typeString;

		JButton notify = new JButton("Notify");
		JButton ack = new JButton("ACKNOWLEDGE");
		JButton shelve = new JButton("SHELVE");
		JButton reset = new JButton("Display All Alarms");
		JButton ignore = new JButton("Ignore");
		JToggleButton mute = new JToggleButton("Mute Audio Warning");

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

			typeString = t;
			this.type = this.getType(t);

			localListModel = itsListModel;

			//LAYOUT

			//Set internals of the panel to appear left to right - 
			// only two internal panes, at least.
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			// Let's start with initialising some JPanels
			JPanel listPanel = new JPanel(new BorderLayout());
			JPanel listButtons = new JPanel(new GridLayout(2,1));
			JPanel alarmPanel = new JPanel(new BorderLayout());
			JPanel buttons = new JPanel(new GridLayout(2,2));
			JPanel alarmPanels = new JPanel();


			// Point List panel
			plist = new JList(localListModel);
			plist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			plist.addListSelectionListener(this);
			JScrollPane plistScroller = new JScrollPane(plist);
			listPanel.setPreferredSize(new Dimension(170, 200));
			listPanel.setMinimumSize(new Dimension(140, 100));
			listPanel.setMaximumSize(new Dimension(170, Integer.MAX_VALUE));

			reset.setFont(new Font("Sans Serif", Font.ITALIC, reset.getFont().getSize()));
			mute.setFont(new Font("Sans Serif", Font.ITALIC, reset.getFont().getSize()));

			listButtons.add(mute);
			listButtons.add(reset);
			listPanel.add(listButtons, BorderLayout.NORTH);
			listPanel.add(plistScroller, BorderLayout.CENTER);

			// Alarm Details Panel
			alarmPanels.add(new AlarmPanel(t));
			alarmDetailsScroller = new JScrollPane(alarmPanels);
			alarmDetailsScroller.getVerticalScrollBar().setUnitIncrement(24);

			// Let's add some stuff to the button panel!
			notify.setToolTipText("Notify someone about these alarms through email.");
			notify.setEnabled(false);
			ack.setToolTipText("Acknowledge these alarms.");
			ack.setEnabled(false);
			shelve.setToolTipText("Shelve the selected alarms.");
			shelve.setEnabled(false);
			reset.setToolTipText("Reset your selections");
			mute.setToolTipText("Mute the Alarm Audio Warning");
			ignore.setToolTipText("Set the selected alarms to ignore alarms");
			ignore.setEnabled(false);

			// set the action commands that are sent when these buttons are pressed
			notify.setActionCommand("notify");
			reset.setActionCommand("reset");
			ack.setActionCommand("ack");
			shelve.setActionCommand("shelve");
			ignore.setActionCommand("ignore");

			// now register the buttons with the actionlistener
			ignore.addActionListener(this);
			notify.addActionListener(this);
			ack.addActionListener(this);
			shelve.addActionListener(this);
			reset.addActionListener(this);
			mute.addItemListener(this);


			//let's add the buttons to the button pane now!
			buttons.add(ignore);
			buttons.add(notify);
			buttons.add(ack);
			buttons.add(shelve);

			alarmPanel.add(alarmDetailsScroller, BorderLayout.CENTER);
			alarmPanel.add(buttons, BorderLayout.SOUTH);

			//Add the big panels to the tabbed pane now
			this.add(listPanel);
			this.add(alarmPanel);
		}

		/**
		 * Returns the int mask for a given type formatted as a String.
		 * These types are masks for the different states an alarm can be in.
		 * @param type The String equivalent of a given alarm state
		 * @return An int mask of the alarm state this display panel focusses on
		 */
		private int getType(String type) throws InvalidParameterException{
			int res;
			if (type.equals("nonAlarmed")){
				res = Alarm.NOT_ALARMED;
			} else if (type.equals("acknowledged")){
				res = Alarm.ACKNOWLEDGED;
			} else if (type.equals("shelved")){
				res = Alarm.SHELVED;
			} else if (type.equals("alarming")){
				res = Alarm.ALARMING;
			} else if (type.equals("ignored")){
				res = AlarmDisplayPanel.IGNORED;
			} else if (type.equals("all")){
				res = AlarmDisplayPanel.ALL;
			}else {
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
						MailSender.sendMail(args[0], args[1], args[2]);
						JOptionPane.showMessageDialog(this, "Email successfully sent!", "Email Notification", JOptionPane.INFORMATION_MESSAGE);
					} catch (IllegalArgumentException e0){
						JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
								"You need to send this to a CSIRO email address!", "Email Notification", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e1){
						JOptionPane.showMessageDialog(this, "Email sending failed!\n" +
								"You  may want to check your connection settings.", "Email Notification", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					return;
				}

			} else if (command.equals("reset")){
				// cancel the options taken
				plist.clearSelection();
				new Thread(){
					public void run(){
						AlarmDisplayPanel.this.showDefaultAlarmPanels();
					}
				}.start();
			} else if (command.equals("ack")){
				if (username.equals("") || password.equals("")){
					JPanel inputs = new JPanel();
					inputs.setLayout(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 0.5;
					gbc.gridx = 0;
					gbc.gridy = 0;
					JLabel usernameLabel = new JLabel("Username: ");
					JTextField usernameField = new JTextField(20);
					usernameField.setText(username);
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
						username = usernameField.getText();
						password = new String(passwordField.getPassword());
						if (username.isEmpty() || password.isEmpty()){
							JOptionPane.showMessageDialog(this, "Invalid Username/Password!", "Authentication Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
					} else {
						return;
					}
				}
				//send the commands along to the server
				try {
					for (Object s : plist.getSelectedValues()){
						new DataSender(s.toString(), "ack", true).start();
					}			
					updateListModels();
					this.updateAlarmPanels();
				} catch (Exception ex){
					password = "";
					JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
							"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
							"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (command.equals("shelve")){
				if (username.equals("") || password.equals("")){
					JPanel inputs = new JPanel();
					inputs.setLayout(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 0.5;
					gbc.gridx = 0;
					gbc.gridy = 0;
					JLabel usernameLabel = new JLabel("Username: ");
					JTextField usernameField = new JTextField(20);
					usernameField.setText(username);
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
						username = usernameField.getText();
						password = new String(passwordField.getPassword());
						if (username.isEmpty() || password.isEmpty()){
							JOptionPane.showMessageDialog(this, "Invalid Username/Password!", "Authentication Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
					} else {
						return;
					}
				}
				try {
					selectionIsShelved = !selectionIsShelved;
					for (Object s : plist.getSelectedValues()){
						new DataSender(s.toString(), "shelve", selectionIsShelved).start();
					}
					updateListModels();
					this.updateAlarmPanels();
					if (selectionIsIgnored){
						shelve.setText("Unignore");
					} else {
						shelve.setText("Ignore");
					}
					if (selectionIsShelved){
						shelve.setText("UNSHELVE");
					} else {
						shelve.setText("SHELVE");
					}
				} catch (Exception ex){
					password = "";
					JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
							"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
							"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (command.equals("ignore")){
				if (this.type != AlarmDisplayPanel.ALL && this.type != AlarmDisplayPanel.IGNORED){ //regular tabs
					Object[] listValues = plist.getSelectedValues();
					for (int i = 0; i < listValues.length; i++){
						ignoreList.add(listValues[i].toString());
						localListModel.removeElement(listValues[i]);
					}
				} else if (this.type == AlarmDisplayPanel.ALL){ //All tab
					Object[] listValues = plist.getSelectedValues();
					if (ignoreList.contains(listValues[0].toString())){ //unignore
						for (int i = 0; i < listValues.length; i++){
							ignoreList.remove(listValues[i].toString());
						}
					} else {
						for (int i = 0; i < listValues.length; i++){ //ignore
							ignoreList.add(listValues[i].toString());
						}
					}
				} else { //ignore tab
					Object[] listValues = plist.getSelectedValues();
					for (int i = 0; i < listValues.length; i++){
						ignoreList.remove(listValues[i].toString());
					}
				}
			}
			this.updateAlarmPanels();
			AlarmManagerPanel.updateListModels();
		}

		/**
		 * Updates, revalidates and repaints the AlarmPanels to match the selections in the JList
		 */
		private void updateAlarmPanels(){
			JPanel newPanel = new JPanel();
			newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));

			Object[] pointNames = plist.getSelectedValues();
			for (Object o : pointNames){
				AlarmPanel a = new AlarmPanel(o.toString());
				a.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));
				newPanel.add(a);
			}
			alarmDetailsScroller.setViewportView(newPanel);
			alarmDetailsScroller.revalidate();
			alarmDetailsScroller.repaint();
		}

		/**
		 * Shows all alarm points that are currently in an alarming or shelved state
		 */
		private void showDefaultAlarmPanels(){
			JPanel newPanel = new JPanel();
			newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));

			Vector<String> alarmingPoints = new Vector<String>();
			HashMap<String, Alarm> lookup = new HashMap<String, Alarm>();
			Vector<Alarm> alarms = AlarmMaintainer.getAlarms();

			for (Alarm a : alarms){ //put all the alarms in a locally maintained lookup table
				if (!ignoreList.contains(a.getPointDesc().getFullName())) lookup.put(a.getPointDesc().getFullName(), a); 
			}
			if (lookup.size() > 0){
				for (String s : lookup.keySet()){
					alarmingPoints.add(s);
				}
				alarmingPoints = reverseQuickSort(alarmingPoints, lookup);
				for (String o : alarmingPoints){
					AlarmPanel a = new AlarmPanel(o);
					a.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));
					newPanel.add(a);
				}
			}
			alarmDetailsScroller.setViewportView(newPanel);
			alarmDetailsScroller.revalidate();
			alarmDetailsScroller.repaint();
		}


		/**
		 * Simple reverse-quicksort implementation on ArrayLists.<br/>
		 * Adapted from {@link http://en.wikipedia.org/wiki/Quicksort#Simple_version}
		 * @param array The ArrayList to be reverse-sorted
		 * @return The reverse-sorted ArrayList
		 * @see <a href=http://en.wikipedia.org/wiki/Quicksort#Simple_version">http://en.wikipedia.org/wiki/Quicksort#Simple_version</a>
		 */
		private synchronized Vector<String> reverseQuickSort(Vector<String> array, HashMap<String, Alarm> lookup) {
			Vector<String> res = array;
			Vector<String> less = new Vector<String>();
			Vector<String> greater = new Vector<String>();
			String removed = "";
			if (res.size() <= 1){
				return res;
			} else {
				int pivot = res.size()/2; //choose midpoint for pivot, no real reason
				removed = res.remove(pivot);

				for (int i = 0; i < res.size(); i++){
					if (lookup.get(res.get(i)).getPriority() > lookup.get(removed).getPriority()){
						greater.add(res.get(i));
					} else {
						less.add(res.get(i));
					}
				}
			}
			try {
				greater = reverseQuickSort(greater, lookup);
				less = reverseQuickSort(less, lookup);

				greater.add(removed);
				greater.addAll(less);
			} catch (Exception e){
				e.printStackTrace();
			}
			return greater;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() == false){
				JList source = (JList) e.getSource();
				if (source.getSelectedIndices().length > 0){
					this.updateAlarmPanels();
					ignore.setEnabled(true);
					ack.setEnabled(true);
					shelve.setEnabled(true);
					notify.setEnabled(true);
					selectionIsShelved = AlarmMaintainer.getAlarm(source.getSelectedValue().toString()).isShelved();
					selectionIsIgnored = (ignoreList.contains(source.getSelectedValue().toString()));
					if (selectionIsShelved){
						shelve.setText("UNSHELVE");
					} else {
						shelve.setText("SHELVE");
					}
					if (selectionIsIgnored){
						ignore.setText("Unignore");
					} else {
						ignore.setText("Ignore");
					}
				} else {
					ignore.setEnabled(false);
					ack.setEnabled(false);
					shelve.setEnabled(false);
					notify.setEnabled(false);
				}
			}
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource().equals(mute)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					muteOn = true;
					all.mute.setSelected(true);
					ignored.mute.setSelected(true);
					nonAlarmed.mute.setSelected(true);
					acknowledged.mute.setSelected(true);
					shelved.mute.setSelected(true);
					alarming.mute.setSelected(true);

				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					muteOn = false;
					all.mute.setSelected(false);
					ignored.mute.setSelected(false);
					nonAlarmed.mute.setSelected(false);
					acknowledged.mute.setSelected(false);
					shelved.mute.setSelected(false);
					alarming.mute.setSelected(false);
				} 
			}
		}

		/**
		 * Updates the list model for the specified type, and revalidates and repaints the panel
		 */
		public void updateListModel() {
			this.setListModel(itsListModel);
			Vector<String> newList = new Vector<String>();
			for (int i = 0; i < localListModel.getSize(); i ++){
				String s = (String) localListModel.get(i);
				if (AlarmMaintainer.getAlarm(s).getAlarmStatus() == this.type){
					if (!ignoreList.contains(s)) newList.add(s);
				}
			}

			localListModel = new DefaultListModel();
			localListModel.setSize(newList.size());
			for (int i = 0; i < newList.size(); i++){
				localListModel.set(i, newList.get(i));
			}
			plist.setModel(localListModel);
			plist.revalidate();
			plist.repaint();
		}

		/**
		 * Updates the list model for the "ignore" list tab
		 */
		public void updateIgnoreListModel(){
			localListModel = new DefaultListModel();
			localListModel.setSize(ignoreList.size());
			for (String s : ignoreList){
				localListModel.addElement(s);
			}
			plist.setModel(localListModel);
			plist.revalidate();
			plist.repaint();
		}

		/**
		 * Sets the list model for the current AlarmDisplayPanel to the specified list model
		 * @param lm The DefaultListModel to set the JList to
		 */
		private void setListModel(DefaultListModel lm){
			localListModel = lm;
		}

	}

	// ///////////////////// END NESTED CLASS /////////////////////////////

	private Vector<String> itsPoints = new Vector<String>();
	private DefaultListModel itsListModel = new DefaultListModel();

	private JTabbedPane stateTabs;
	private static AlarmDisplayPanel all;
	private static AlarmDisplayPanel ignored;
	private static AlarmDisplayPanel nonAlarmed;
	private static AlarmDisplayPanel acknowledged;
	private static AlarmDisplayPanel shelved;
	private static AlarmDisplayPanel alarming;

	private AudioWarning  klaxon = new AudioWarning();

	/**
	 * C'tor
	 */
	public AlarmManagerPanel() {
		// Set layout
		this.setLayout(new BorderLayout());
		AlarmMaintainer.addListener(this);

		stateTabs = new JTabbedPane(JTabbedPane.TOP);
		all = new AlarmDisplayPanel("all");
		ignored = new AlarmDisplayPanel("ignored");
		nonAlarmed = new AlarmDisplayPanel("nonAlarmed");
		acknowledged = new AlarmDisplayPanel("acknowledged");
		shelved = new AlarmDisplayPanel("shelved");
		alarming = new AlarmDisplayPanel("alarming");

		// Insert the tabs into the tabbed pane
		stateTabs.insertTab("All", null, all, "List of all alarms", 0);
		stateTabs.insertTab("Ignored", null, ignored, "List of ignored alarms", 1);
		stateTabs.insertTab("Non-Alarmed", null, nonAlarmed, "List of non-alarming Alarms", 2);
		stateTabs.insertTab("Acknowledged", null, acknowledged, "List of Acknowledged Alarms", 3);
		stateTabs.insertTab("Shelved", null, shelved, "List of Shelved Alarms", 4);
		stateTabs.insertTab("Alarming", null , alarming, "List of Currently Active alarms", 5);

		stateTabs.setForegroundAt(0, AlarmManagerPanel.ALL_COLOUR);
		stateTabs.setForegroundAt(1, AlarmManagerPanel.IGNORE_COLOUR);
		stateTabs.setForegroundAt(2, AlarmManagerPanel.NOT_ALARMED_COLOUR);
		stateTabs.setForegroundAt(3, AlarmManagerPanel.ACKNOWLEDGED_COLOUR);
		stateTabs.setForegroundAt(4, AlarmManagerPanel.SHELVED_COLOUR);
		stateTabs.setForegroundAt(5, AlarmManagerPanel.ALARMING_COLOUR);

		stateTabs.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				JTabbedPane source = (JTabbedPane) e.getSource();
				try{
					((AlarmDisplayPanel) source.getSelectedComponent()).showDefaultAlarmPanels();
				} catch (NullPointerException n){
					System.err.println("Null Pointer Exception in selecting tabs");
				}
			}
		});

		this.add(stateTabs);
	}

	/**
	 * Shorthand macro for calling the update methods for each of the tabs
	 */
	public static void updateListModels(){
		ignored.updateIgnoreListModel();
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

			itsPoints = new Vector<String>();

			// Get the list of points to be monitored
			String p = (String) setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			while (stp.hasMoreTokens()) {
				itsPoints.add(stp.nextToken());
			}

			// Get which categories of alarms to monitor
			String str;
			str = (String) setup.get(noPriAlmStr);
			if (str != null){
				/*if (str.equals("true")){
					noPriorityAlarms = true;
				} else if (str.equals("false")){
					noPriorityAlarms = false;
				}*/
				noPriorityAlarms = false;
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

			/*for (String pd : itsPoints){
				AlarmMaintainer.setAlarm(PointDescription.getPoint(pd));
			}*/
			Vector<String> badPoints = new Vector<String>();
			for (String s : itsPoints){
				try {
					if (AlarmMaintainer.getAlarm(s).getPriority() == -1 && !noPriorityAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 0 && !informationAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 1 && !warningAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 2 && !dangerAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 3 && !severeAlarms){
						badPoints.add(s);
					}
				} catch (NullPointerException n){
					badPoints.add(s);
				}
			}
			if (badPoints.size() > 0){
				for (String bStr : badPoints){
					itsPoints.remove(bStr);
				}
			}

			@SuppressWarnings ("unchecked")
			Vector<String> newPoints = (Vector<String>) itsPoints.clone();

			itsListModel.setSize(itsPoints.size());

			int i = 0;
			for (String s : newPoints){
				itsListModel.setElementAt(s, i);
				i++;
			}
			itsPoints = newPoints;

			nonAlarmed.updateListModel();
			ignored.updateIgnoreListModel();
			acknowledged.updateListModel();
			shelved.updateListModel();
			alarming.updateListModel();

			all.showDefaultAlarmPanels();
			ignored.showDefaultAlarmPanels();
			nonAlarmed.showDefaultAlarmPanels();
			acknowledged.showDefaultAlarmPanels();
			shelved.showDefaultAlarmPanels();
			alarming.showDefaultAlarmPanels();

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

	/**
	 * Method to create a new blank setup pane
	 */
	private void blankSetup() {
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
		AlarmMaintainer.removeListener(this);
	}

	/** Play an audio sample on the sound card. */
	private boolean playAudio(String resname) {
		RelTime sleep = RelTime.factory(1000000);
		try {
			InputStream in = AlarmManagerPanel.class.getClassLoader().getResourceAsStream(resname);
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
		updateListModels();
		Alarm thisAlarm = event.getAlarm();
		if (!thisAlarm.isShelved() && !thisAlarm.isAcknowledged() && thisAlarm.isAlarming() && !ignoreList.contains(thisAlarm.getPointDesc().getFullName())){
			stateTabs.setSelectedIndex(5); //if alarming, automatically switch over to the "Alarming" tab
			if (!klaxon.isAlive()){
				klaxon.start();
			}
		}
	}

	// /////////////////////// NESTED CLASS ///////////////////////////////

	/**
	 * Class that extends Thread, to create an audio sound effect without tying up any other
	 * threads, such as the UI or logic threads.
	 */
	public class AudioWarning extends Thread {
		/**
		 * AudioWarning implementation of the Thread's run() method. Periodically activates
		 * and sounds an audio warning if there are high priority (priority >=2 ) alarms currently
		 * alarming.
		 */
		@Override
		public void run() {
			try {
				RelTime sleep = RelTime.factory(10000000);
				while (!alarming.localListModel.isEmpty()) {
					if (muteOn){
						continue;
					} else {
						boolean highPriority = false;
						for (int i = 0; i < alarming.localListModel.size(); i++){
							if (AlarmMaintainer.getAlarm(((String) alarming.localListModel.get(i))).getPriority() >= 2){ //should only siren on Major or Severe alarms
								highPriority = true;
								break;
							}
						}
						if (highPriority){
							boolean success = playAudio("atnf/atoms/mon/gui/monpanel/watchdog.wav");
							if (success == false) throw (new Exception());
						}
					}
					sleep.sleep();
				}
			} catch (Exception e) {
				System.err.println("Audio Playing failed");
			}
		}
	}
	// ///////////////////// END NESTED CLASS /////////////////////////////

	// ///// NESTED CLASS: DataSender ///////
	/**
	 * Data Sending class that extends Thread. Implemented so that data sending wouldn't hold up the UI
	 * @see Thread
	 * @see Runnable
	 */
	public class DataSender extends Thread implements Runnable{

		String point;
		String action;
		boolean state;


		/**
		 * Constructor for this DataSender object
		 * @param string The name of the point that is being written to
		 * @param act The "action" that needs to be taken - only "shelve" and "ack" commands are valid
		 * @param selection Boolean value indicating whether this point should be set to be "true" or "false" depending on the action taken
		 */
		public DataSender(String string, String act,
				boolean selection) {
			point = string;
			action = act;
			state = selection;
		}

		/**
		 * Overrides the default {@code run()} method. Using the constructor parameters, sends
		 * data to the server using the AlarmMaintainer client utility class, and also updates
		 * the ListModel for each JList, so that the UI doesn't hang while this is performed.
		 */
		@Override
		public void run(){
			try{
				if (action.equals("shelve")){
					AlarmMaintainer.setShelved(point, state, username, password);
					updateListModels();
				} else if (action.equals("ack")){
					AlarmMaintainer.setAcknowledged(point, state, username, password);
					updateListModels();
				} else {
					throw (new IllegalArgumentException());
				}
			} catch (IllegalArgumentException i){
				password = "";
				JOptionPane.showMessageDialog(AlarmManagerPanel.this, "You somehow sent an invalid command to the server - check the source code!", 
						"Invalid command Error", JOptionPane.ERROR_MESSAGE);

			} catch (Exception e){
				password = "";
				JOptionPane.showMessageDialog(AlarmManagerPanel.this, "Something went wrong with the sending of data. " +
						"\nPlease ensure that you're properly connected to the network.", 
						"Data Sending Error", JOptionPane.ERROR_MESSAGE);
			}
		}

	}
	// ///// END NESTED CLASS ///////
}