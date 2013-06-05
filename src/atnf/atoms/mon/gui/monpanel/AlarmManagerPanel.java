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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.AuthenticationException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.AlarmMaintainer;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.gui.AlarmPanel;
import atnf.atoms.mon.gui.MonFrame;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.PointSourceSelector;
import atnf.atoms.mon.util.MonitorUtils;
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

	public static HashMap<String, Alarm> localAlarms = new HashMap<String, Alarm>();

	private JCheckBox allowAutoAlarms = new JCheckBox("Allow Automatic Notifications");

	private boolean muteOn = false;
	private boolean noPriorityAlarms = false;
	private boolean informationAlarms = false;
	private boolean minorAlarms = false;
	private boolean majorAlarms = false;
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
	private String minorAlmStr = "minor";
	private String majorAlmStr = "major";
	private String sevAlmStr = "severe";

	// /////////////////////// NESTED CLASS ///////////////////////////////
	/** Nested class to provide GUI controls for configuring the AlarmManagerPanel */
	private class AlarmManagerSetupPanel extends MonPanelSetupPanel implements ItemListener, ActionListener{

		private JCheckBox selectAllPointCb = new JCheckBox("Select All Points");
		private JLabel catLabel = new JLabel("Select Alarm Categories: ");


		private JCheckBox noPriorityCb = new JCheckBox("\"No Priority\"");
		private JCheckBox informationCb = new JCheckBox("Information");
		private JCheckBox minorCb = new JCheckBox("Minor");
		private JCheckBox majorCb = new JCheckBox("Major");
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
			minorCb.addItemListener(this);
			majorCb.addItemListener(this);
			severeCb.addItemListener(this);
			allCb.addItemListener(this);

			// lots of glue so that the layout doesn't look silly when resized horizontally
			//selectCategory.add(Box.createHorizontalGlue());
			//selectCategory.add(noPriorityCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(informationCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(minorCb);
			selectCategory.add(Box.createHorizontalGlue());
			selectCategory.add(majorCb);
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

			long startTime = System.currentTimeMillis();
			while (AlarmMaintainer.getAllAlarms().size() == 0){
				//loop forever
				if ((System.currentTimeMillis() - startTime) > 10000){
					JOptionPane.showMessageDialog(AlarmManagerPanel.this, "Error retrieving alarms from the server", "Alarm Retrieval Error", JOptionPane.ERROR_MESSAGE);
					return null;
				}
				continue;
			}

			if (selectAllPointCb.isSelected()){
				selectAllPointCb.doClick();
				selectAllPointCb.doClick();
				selectAllPointCb.setSelected(true);
				//weird hack to get the default settings to work

				String[] points = MonClientUtil.getAllPointNames();
				if (points.length > 0) {
					HashSet<String> selections = new HashSet<String>();
					HashSet<String> allPoints = new HashSet<String>();
					for (int i = 0; i < points.length; i++){
						allPoints.add(points[i]);
						try { //only add points that have valid alarms to the setup
							int priority = AlarmMaintainer.getAlarm(points[i]).getPriority();
							if (priority >= 0){
								if ((informationAlarms && priority == 0) || (minorAlarms && priority == 1) || (majorAlarms && priority == 2) || (severeAlarms && priority == 3)){
									selections.add(points[i]);
								}
							}
						} catch (NullPointerException n){}
					}

					selections = MonitorUtils.prunePointTree(selections, allPoints);

					int n = 0;
					for (String s : selections){
						if (n == 0){
							p += s;
						} else {
							p += ":" + s;
						}
						n++;
					}
				}
			} else {
				Vector<?> points = itsPointSelector.getSelections();
				if (points.size() > 0) {
					HashSet<String> selections = new HashSet<String>();
					HashSet<String> allPoints = new HashSet<String>();
					String[] names = PointDescription.getAllPointNames();
					for (int i = 0; i < names.length; i++){
						allPoints.add(names[i]);
					}
					for (int i = 0; i < points.size(); i++) {
						selections.add(points.get(i).toString());
					}
					selections = MonitorUtils.prunePointTree(selections, allPoints);

					int n = 0;
					for (String s : selections){
						if (n == 0){
							p += s;
						} else {
							p += ":" + s;
						}
						n++;
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
			if (minorAlarms){
				ss.put(minorAlmStr, "true");
			} else {
				ss.put(minorAlmStr, "false");
			}
			if (majorAlarms){
				ss.put(majorAlmStr, "true");
			} else {
				ss.put(majorAlmStr, "false");
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

			s = setup.get(minorAlmStr);
			if (s != null) {
				if (s.equals("true")){
					minorAlarms = true;
				} else if (s.equals("false")){
					minorAlarms = false;
				}
			}

			s = setup.get(majorAlmStr);
			if (s != null) {
				if (s.equals("true")){
					majorAlarms = true;
				} else if (s.equals("false")){
					majorAlarms = false;
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
			if (minorAlarms){
				minorCb.setSelected(true);
			}
			if (majorAlarms){
				majorCb.setSelected(true);
			}
			if (severeAlarms){
				severeCb.setSelected(true);
			}
			if (noPriorityAlarms && informationAlarms && minorAlarms && majorAlarms && severeAlarms){
				allCb .setSelected(true);
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
					states[2] = minorCb.isSelected();
					states[3] = majorCb.isSelected();
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);
					informationCb.setSelected(states[1]);
					minorCb.setSelected(states[2]);
					majorCb.setSelected(states[3]);
					severeCb.setSelected(states[4]);
				} else if (e.getStateChange() == ItemEvent.SELECTED){
					noPriorityAlarms = true;
					if (noPriorityAlarms && informationAlarms && minorAlarms && majorAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(informationCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					informationAlarms = false;

					states[0] = noPriorityCb.isSelected();
					states[1] = false;
					states[2] = minorCb.isSelected();
					states[3] = majorCb.isSelected();
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					minorCb.setSelected(states[2]);
					majorCb.setSelected(states[3]);
					severeCb.setSelected(states[4]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					informationAlarms = true;
					if (noPriorityAlarms && informationAlarms && minorAlarms && majorAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(minorCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					minorAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = false;
					states[3] = majorCb.isSelected();
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					majorCb.setSelected(states[3]);
					severeCb.setSelected(states[4]);
				} else if (e.getStateChange() == ItemEvent.SELECTED){
					minorAlarms = true;
					if (noPriorityAlarms && informationAlarms && minorAlarms && majorAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(majorCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					severeAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = minorCb.isSelected();
					states[3] = false;
					states[4] = severeCb.isSelected();

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					minorCb.setSelected(states[2]);
					severeCb.setSelected(states[4]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					majorAlarms = true;
					if (noPriorityAlarms && informationAlarms && minorAlarms && majorAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(severeCb)){
				if (e.getStateChange() == ItemEvent.DESELECTED){
					severeAlarms = false;
					states[0] = noPriorityCb.isSelected();
					states[1] = informationCb.isSelected();
					states[2] = minorCb.isSelected();
					states[3] = majorCb.isSelected();
					states[4] = false;

					allCb.setSelected(false);

					noPriorityCb.setSelected(states[0]);
					informationCb.setSelected(states[1]);
					minorCb.setSelected(states[2]);
					majorCb.setSelected(states[3]);

				} else if (e.getStateChange() == ItemEvent.SELECTED){
					severeAlarms = true;
					if (noPriorityAlarms && informationAlarms && minorAlarms && majorAlarms && severeAlarms){
						allCb.setSelected(true);
					}
				}
			} else if (source.equals(allCb)){
				if (e.getStateChange() == ItemEvent.SELECTED){
					noPriorityAlarms = true;
					informationAlarms = true;
					minorAlarms = true;
					majorAlarms = true;
					severeAlarms = true;

					noPriorityCb.setSelected(true);
					informationCb.setSelected(true);
					minorCb.setSelected(true);
					majorCb.setSelected(true);
					severeCb.setSelected(true);

				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					noPriorityAlarms = false;
					informationAlarms = false;
					minorAlarms = false;
					majorAlarms = false;
					severeAlarms = false;

					noPriorityCb.setSelected(false);
					informationCb.setSelected(false);
					minorCb.setSelected(false);
					majorCb.setSelected(false);
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

		@Override
		public void actionPerformed(ActionEvent e){
			if (e.getActionCommand().equals("OK")){
				long startTime = System.currentTimeMillis();
				while (AlarmMaintainer.getAllAlarms().size() == 0){
					//loop for 5 seconds
					if ((System.currentTimeMillis() - startTime) > 5000){
						JOptionPane.showMessageDialog(AlarmManagerPanel.this, "Error retrieving alarms from the server", "Alarm Retrieval Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					continue;
				}
			}
			super.actionPerformed(e);
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
	protected class AlarmDisplayPanel extends JPanel implements	ActionListener, ListSelectionListener, ItemListener, MouseListener, KeyListener{

		private static final int ALL = 98;
		private static final int IGNORED = 99;
		private boolean selectionIsShelved = false;
		private boolean selectionIsIgnored = false;
		private int type = -1;
		private ArrayList<AlarmPanel> panelSelections = new ArrayList<AlarmPanel>();
		private boolean flashing = false;
		private boolean flashOn = false;
		private Timer timer = new Timer(500, new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				if (flashing){
					if (flashOn){
						stateTabs.setBackgroundAt(4, null);
						flashOn = false;
					} else {
						stateTabs.setBackgroundAt(4, Color.YELLOW);
						flashOn = true;
					}
				} else {
					stateTabs.setBackgroundAt(4, null);
					flashOn = false;
				}
			}
		});

		Object[] selections; 
		int scrollBarPos = 0;

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

			timer.start();
			typeString = t;
			this.type = this.convertType(t);

			localListModel = itsListModel;

			//LAYOUT

			//Set internals of the panel to appear left to right - 
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
			listPanel.setPreferredSize(new Dimension(200, 200));
			listPanel.setMinimumSize(new Dimension(200, 200));
			listPanel.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));

			reset.setFont(new Font("Sans Serif", Font.ITALIC, reset.getFont().getSize()));
			mute.setFont(new Font("Sans Serif", Font.ITALIC, reset.getFont().getSize()));

			listButtons.add(mute);
			listButtons.add(reset);
			listPanel.add(listButtons, BorderLayout.NORTH);
			listPanel.add(plistScroller, BorderLayout.CENTER);

			// Alarm Details Panel
			alarmPanels.add(new JPanel());
			alarmPanels.setBackground(Color.WHITE);
			alarmPanels.setOpaque(true);
			alarmDetailsScroller = new JScrollPane(alarmPanels);
			alarmDetailsScroller.getVerticalScrollBar().setUnitIncrement(24);
			alarmDetailsScroller.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);

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
			plist.addMouseListener(this);
			plist.addKeyListener(new KeyListener(){
				@Override
				public void keyPressed(KeyEvent arg0) {
					if(arg0.getKeyCode() == KeyEvent.VK_CONTROL || arg0.getKeyCode() == KeyEvent.VK_SHIFT){
						controlShiftIsDown =  true;
					}
				}
				@Override
				public void keyReleased(KeyEvent arg0) {
					if(arg0.getKeyCode() == KeyEvent.VK_CONTROL || arg0.getKeyCode() == KeyEvent.VK_SHIFT){
						AlarmManagerPanel.this.multiSelectLabel.setText("Multi-Select: OFF");
						AlarmManagerPanel.this.multiSelectLabel.setBackground(null);
						controlShiftIsDown = false;
					}
				}
				@Override
				public void keyTyped(KeyEvent arg0) {}
			});

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
			this.addKeyListener(this);
		}

		/**
		 * Returns the int mask for a given type formatted as a String.
		 * These types are masks for the different states an alarm can be in.
		 * @param type The String equivalent of a given alarm state
		 * @return An int mask of the alarm state this display panel focusses on
		 */
		private int convertType(String type) throws InvalidParameterException{
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

		/**
		 * Method that returns the int mask type of this AlarmDisplayPanel
		 * @return The int mask for this AlarmDisplayPanel's type
		 */
		private int getType(){
			return this.type;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();

			if (command.equals("notify")){
				MonClientUtil.showEmailPrompt(this);
			} else if (command.equals("reset")){
				// cancel the options taken
				plist.clearSelection();
				try {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							AlarmDisplayPanel.this.showDefaultAlarmPanels();
						}
					});
				} catch (Exception e1) {}
			} else if (command.equals("ack")){
				String[] creds = LoginManager.getLoginCredentials(AlarmManagerPanel.this);
				try {
					if (creds[0].isEmpty() || creds[1].isEmpty()){
						LoginManager.setCredentials(new String[]{creds[0], ""});
						return;
					}
					//send the commands along to the server

					if (e.getSource() instanceof JButton){
						this.listAcknowledge(e);
					} else {
						if (e.getSource() instanceof JMenuItem){
							if (plist.getSelectedIndices().length > 0){
								this.listAcknowledge(e);
							} else {
								this.singlePanelAcknowledge(e);
							}
						}
					}
					updateListModels();
					this.updateAlarmPanels();
				} catch (Exception ex){
					ex.printStackTrace();
					LoginManager.setCredentials(new String[]{creds[0], ""});
					JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
							"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
							"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (command.equals("shelve")){
				String[] creds = LoginManager.getLoginCredentials(AlarmManagerPanel.this);
				try {
					if (creds[0].isEmpty() || creds[1].isEmpty()){
						LoginManager.setCredentials(new String[]{creds[0], ""}); 
						return;
					}
					LoginManager.setCredentials(new String[]{creds[0], creds[1]});
					if (e.getSource() instanceof JButton){
						this.listShelve(e);
					} else if (e.getSource() instanceof JMenuItem){
						if (plist.getSelectedIndices().length > 0){
							this.listShelve(e);
						} else {
							this.singlePanelShelve(e);
						}
					}
					updateListModels();
					this.updateAlarmPanels();
					if (selectionIsIgnored){
						ignore.setText("Unignore");
					} else {
						ignore.setText("Ignore");
					}
					if (selectionIsShelved){
						shelve.setText("UNSHELVE");
					} else {
						shelve.setText("SHELVE");
					}
				} catch (Exception ex){
					LoginManager.setCredentials(new String[]{creds[0], ""});
					JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
							"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
							"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else if (command.equals("ignore")){
				if (e.getSource() instanceof JButton){
					this.listIgnore(e);
				} else if (e.getSource() instanceof JMenuItem){
					if (plist.getSelectedIndices().length > 0){
						this.listIgnore(e);
					} else {
						this.singlePanelIgnore(e);
					}
				}
			}
			this.updateAlarmPanels();
			AlarmManagerPanel.updateListModels();
			AlarmManagerPanel.this.updateInfoBar();
		}

		/**
		 * Ignores a number of Alarms that have been selected from the JList
		 * @param e The ActionEvent that is received that triggers this behaviour
		 */
		private void listIgnore(ActionEvent e){
			if (this.getType() != AlarmDisplayPanel.ALL && this.getType() != AlarmDisplayPanel.IGNORED){ //regular tabs
				Object[] listValues = plist.getSelectedValues();
				for (int i = 0; i < listValues.length; i++){
					AlarmMaintainer.ignoreList.add(listValues[i].toString());
					localListModel.removeElement(listValues[i]);
				}
			} else if (this.getType() == AlarmDisplayPanel.ALL){ //All tab
				Object[] listValues = plist.getSelectedValues();
				if (AlarmMaintainer.ignoreList.contains(listValues[0].toString())){ //unignore
					for (int i = 0; i < listValues.length; i++){
						AlarmMaintainer.ignoreList.remove(listValues[i].toString());
					}
				} else {
					for (int i = 0; i < listValues.length; i++){ //ignore
						AlarmMaintainer.ignoreList.add(listValues[i].toString());
					}
				}
			} else { //ignore tab
				Object[] listValues = plist.getSelectedValues();
				for (int i = 0; i < listValues.length; i++){
					AlarmMaintainer.ignoreList.remove(listValues[i].toString());
				}
			}
		}

		/**
		 * Shelves a number of Alarms that have been selected from the JList
		 * @param e The ActionEvent that is received that triggers this behaviour
		 */
		private void listShelve(ActionEvent e){
			selectionIsShelved = !selectionIsShelved;
			for (Object s : plist.getSelectedValues()){
				new DataSender(s.toString(), "shelve", selectionIsShelved).start();
			}
		}

		/**
		 * Acknowledges a number of Alarms that have been selected from the JList
		 * @param e The ActionEvent that is received that triggers this behaviour
		 */
		private void listAcknowledge(ActionEvent e){
			for (Object s : plist.getSelectedValues()){
				try {
					if (localAlarms.get(s.toString()).isAlarming()){ //Only able to acknowledge if the alarm is actually alarming
						new DataSender(s.toString(), "ack", true).start();
					}
				} catch (NullPointerException n){
					if (AlarmMaintainer.getAlarm(s.toString()).isAlarming()){ //Only able to acknowledge if the alarm is actually alarming
						new DataSender(s.toString(), "ack", true).start();
					}
				}
			}
		}

		/**
		 * Ignores an alarm where only a single panel is the focus, with no selections in the JList
		 * @param e The ActionEvent that is received that triggers this behaviour
		 */
		private void singlePanelIgnore(ActionEvent e){
			JMenuItem source = (JMenuItem) e.getSource();
			JPopupMenu parent = (JPopupMenu) source.getParent();
			AlarmPanel pan = (AlarmPanel) parent.getInvoker();
			final String point = pan.getPointName();
			if (this.getType() != AlarmDisplayPanel.ALL && this.getType() != AlarmDisplayPanel.IGNORED){ //regular tabs
				AlarmMaintainer.ignoreList.add(point);
				localListModel.removeElement(point);
			} else if (this.getType() == AlarmDisplayPanel.ALL){ //All tab
				if (AlarmMaintainer.ignoreList.contains(point)){ //unignore
					AlarmMaintainer.ignoreList.remove(point);
				} else {
					AlarmMaintainer.ignoreList.add(point);
				}
			} else { //ignore tab
				AlarmMaintainer.ignoreList.remove(point);
			}
		}

		/**
		 * Shelves an alarm where only a single panel is the focus, with no selections in the JList
		 * @param e The ActionEvent that is received that triggers this behaviour
		 */
		private void singlePanelShelve(ActionEvent e){
			JMenuItem source = (JMenuItem) e.getSource();
			JPopupMenu parent = (JPopupMenu) source.getParent();
			AlarmPanel pan = (AlarmPanel) parent.getInvoker();
			final String point = pan.getPointName();
			selectionIsShelved = !pan.getAlarm().isShelved();
			new DataSender(point, "shelve", selectionIsShelved).start();
			new Thread(){
				public void run(){
					try {
						SwingUtilities.invokeAndWait(new Runnable(){
							public void run(){
								plist.setSelectedValue(point, false);
							}
						});
					} catch (InterruptedException e1) {
					} catch (InvocationTargetException e1) {}
					shelve.doClick();
				}
			}.start();
		}

		/**
		 * Acknowledges an alarm where only a single panel is the focus, with no selections in the JList
		 * @param e The ActionEvent that is received that triggers this behaviour
		 */
		private void singlePanelAcknowledge(ActionEvent e){
			JMenuItem source = (JMenuItem) e.getSource();
			JPopupMenu parent = (JPopupMenu) source.getParent();
			AlarmPanel pan = (AlarmPanel) parent.getInvoker();
			final String point = pan.getPointName();
			new DataSender(point, "ack", true).start();
			new Thread(){
				public void run(){
					try {
						SwingUtilities.invokeAndWait(new Runnable(){
							public void run(){
								plist.setSelectedValue(point, false);
							}
						});
					} catch (InterruptedException e1) {
					} catch (InvocationTargetException e1) {}
					ack.doClick();
				}
			}.start();
		}

		/**
		 * Updates and repaints the AlarmPanels to match the selections in the JList
		 */
		private void updateAlarmPanels(){
			JPanel newPanel = new JPanel();
			newPanel.setOpaque(true);
			newPanel.setBackground(Color.WHITE);
			newPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weighty = 0.000000001;
			gbc.weightx = 0.5;
			gbc.gridx = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;

			Object[] pointNames = plist.getSelectedValues();
			int i = 0;
			for (Object o : pointNames){
				gbc.gridy = i;
				AlarmPanel a;
				if (i == pointNames.length-1){
					gbc.weighty = 1.0;
					a = new AlarmPanel(o.toString());
				} else {
					a = new AlarmPanel(o.toString());
					a.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));
				}
				a.addMouseListener(this);
				newPanel.add(a, gbc);
				i++;
			}
			alarmDetailsScroller.setViewportView(newPanel);
			alarmDetailsScroller.repaint();
		}

		/**
		 * Shows all alarm points that are currently in an alarming or shelved state
		 */
		private void showDefaultAlarmPanels(){		  
			scrollBarPos = alarmDetailsScroller.getVerticalScrollBar().getValue();
			if (all.plist.getModel().getSize() == 0){
				updateLists();
			}
			this.updateListModel();
			if (this.plist.getModel().getSize() == 1){
				plist.setSelectedIndex(0);
				return;
			}
			JPanel newPanel = new JPanel();
			newPanel.setOpaque(true);
			newPanel.setBackground(Color.WHITE);
			newPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weighty = 0.000000001;
			gbc.weightx = 0.5;
			gbc.gridx = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;

			Collection<Alarm> alarms;
			if (this.getType() == AlarmDisplayPanel.ALL || this.getType() == AlarmDisplayPanel.IGNORED){
				alarms = AlarmMaintainer.getAllAlarms();
			} else {
				alarms = AlarmMaintainer.getAlarms();
			}

			Collection<String> alarmingPoints = new ArrayList<String>();
			for (Alarm a : alarms){ //put all the alarms in a locally maintained lookup table
				String thisname = a.getPointDesc().getFullName();
				if (itsPoints.contains(thisname)){
					if (AlarmMaintainer.ignoreList.contains(thisname) && this.getType() == AlarmDisplayPanel.IGNORED){
						//case for ignored tab
						alarmingPoints.add(thisname);
					} else if (!AlarmMaintainer.ignoreList.contains(thisname) && this.getType() == AlarmDisplayPanel.ALL){ 
						//case for all tab
						alarmingPoints.add(thisname);
					} else if (!AlarmMaintainer.ignoreList.contains(thisname) && this.getType() == a.getAlarmStatus()){ 
						//case for other tabs
						alarmingPoints.add(thisname);
					}
				}
			}
			if (!alarmingPoints.isEmpty()){
				alarmingPoints = alphaSortByPriority(alarmingPoints.toArray(new String[0]));
				int i = 0;
				for (String o : alarmingPoints){
					gbc.gridy = i;
					AlarmPanel a;
					if (i == alarmingPoints.size()-1){
						gbc.weighty = 1.0;
						a = new AlarmPanel(o);
					} else {
						a = new AlarmPanel(o);
						a.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));
					}
					a.addMouseListener(this);
					newPanel.add(a, gbc);
					i++;
				}
			}
			plist.revalidate();
			plist.repaint();
			alarmDetailsScroller.setViewportView(newPanel);
			alarmDetailsScroller.revalidate();
			alarmDetailsScroller.repaint();
			alarmDetailsScroller.getVerticalScrollBar().setValue(scrollBarPos);
			this.requestFocusInWindow();
		}

		/**
		 * Simple reverse-quicksort implementation on ArrayLists.<br/>
		 * Adapted from {@link http://en.wikipedia.org/wiki/Quicksort#Simple_version}
		 * @deprecated Uses the {@link AlarmManagerPanel#basicQuickSort} method instead.
		 * @param array The ArrayList to be reverse-sorted
		 * @return The reverse-sorted ArrayList
		 * @see <a href=http://en.wikipedia.org/wiki/Quicksort#Simple_version">http://en.wikipedia.org/wiki/Quicksort#Simple_version</a>
		 */
		public synchronized Vector<String> reverseQuickSort(Vector<String> array, HashMap<String, Alarm> lookup) {
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
					try {
						if (source.getSelectedIndices().length == 1 && 
								!localAlarms.get(source.getSelectedValue().toString()).isAlarming()){
							/* Only allow the ack button to be enabled if the selected point is alarming
							 * or there are multiple points selected, in which case only valid points
							 * will be acknowledged @see DataSender
							 */
							ack.setEnabled(false);
						} else {
							ack.setEnabled(true);
						}
					} catch (NullPointerException n) {
						if (source.getSelectedIndices().length == 1 && 
								!AlarmMaintainer.getAlarm(source.getSelectedValue().toString()).isAlarming()){
							/* Only allow the ack button to be enabled if the selected point is alarming
							 * or there are multiple points selected, in which case only valid points
							 * will be acknowledged @see DataSender
							 */
							ack.setEnabled(false);
						} else {
							ack.setEnabled(true);
						}
					}
					shelve.setEnabled(true);
					notify.setEnabled(true);
					try {
						String src = source.getSelectedValue().toString();
						try {
							selectionIsShelved = localAlarms.get(src).isShelved();
						} catch (NullPointerException e1) {
							selectionIsShelved = AlarmMaintainer.getAlarm(src).isShelved();
						}
						selectionIsIgnored = (AlarmMaintainer.ignoreList.contains(src));
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
					} catch (NullPointerException npe){
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								reset.doClick();
							}
						});
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
					acknowledged.mute.setSelected(true);
					shelved.mute.setSelected(true);
					alarming.mute.setSelected(true);

				} else if (e.getStateChange() == ItemEvent.DESELECTED){
					muteOn = false;
					all.mute.setSelected(false);
					ignored.mute.setSelected(false);
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
			if (this.getType() == AlarmDisplayPanel.IGNORED){
				ArrayList<String> al = new ArrayList<String>();
				HashSet<String> newList = new HashSet<String>();
				localListModel = new DefaultListModel();
				for (String s : AlarmMaintainer.ignoreList){
					if (!s.equals("") && s != null && itsPoints.contains(s)){
						al.add(s);
						newList.add(s);
					}
				}
				AlarmMaintainer.ignoreList = newList;
				al = alphaSortByPriority(al.toArray(new String[0]));
				for (String s : al){
					localListModel.addElement(s);
				}
			} else {
				this.setListModel(itsListModel);
				ArrayList<String> newList = new ArrayList<String>();
				for (int i = 0; i < localListModel.getSize(); i ++){
					String s = (String) localListModel.get(i);
					if (AlarmMaintainer.getAlarm(s).getAlarmStatus() == this.getType()){
						if (!AlarmMaintainer.ignoreList.contains(s)) newList.add(s);
					} else if (this.getType() == AlarmDisplayPanel.ALL){
						newList.add(s);
					}
				}
				if (this.getType() == AlarmDisplayPanel.ALL){
					Collections.sort(newList);
				} else {
					newList = alphaSortByPriority(newList.toArray(new String[0]));
				}
				localListModel = new DefaultListModel();
				localListModel.setSize(newList.size());
				for (int i = 0; i < newList.size(); i++){
					localListModel.set(i, newList.get(i));
				}
			}
			plist.setModel(localListModel);
			//plist.revalidate();
			plist.repaint();
		}

		/**
		 * Sets the list model for the current AlarmDisplayPanel to the specified list model
		 * @param lm The DefaultListModel to set the JList to
		 */
		private void setListModel(DefaultListModel lm){
			localListModel = lm;
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {}
		@Override
		public void mouseEntered(MouseEvent arg0) {}
		@Override
		public void mouseExited(MouseEvent arg0) {}
		@Override
		public void mouseReleased(MouseEvent arg0) {
			if ((SwingUtilities.isRightMouseButton(arg0) && !(arg0.isControlDown() || arg0.isShiftDown()))){
				JPopupMenu jpop = new JPopupMenu();
				JMenuItem ignMen = new JMenuItem("Ignore/Unignore");
				ignMen.setActionCommand("ignore");
				ignMen.addActionListener(AlarmDisplayPanel.this);
				JMenuItem ackMen = new JMenuItem("Acknowledge");
				ackMen.setActionCommand("ack");
				ackMen.addActionListener(AlarmDisplayPanel.this);
				JMenuItem shvMen = new JMenuItem("Shelve/Unshelve");
				shvMen.setActionCommand("shelve");
				shvMen.addActionListener(AlarmDisplayPanel.this);
				if (arg0.getComponent() instanceof AlarmPanel){
					AlarmPanel ap = (AlarmPanel) arg0.getComponent();
					if (plist.getSelectedIndices().length <= 1 && !ap.getAlarm().isAlarming()){
						ackMen.setEnabled(false); //disable acknowledgement through right-click if selected alarm point is a single panel that isn't alarming
					}
				} else if (arg0.getComponent() instanceof JList){
					if (plist.getSelectedIndices().length <= 1){
						plist.setSelectedIndex(plist.locationToIndex(arg0.getPoint())); //select the item
						try {
							if (!localAlarms.get(plist.getSelectedValue().toString()).isAlarming()){
								ackMen.setEnabled(false); //disable acknowledgement through right-click if selected alarm point is a point that isn't alarming
							}
						} catch (NullPointerException n){
							if (!AlarmMaintainer.getAlarm(plist.getSelectedValue().toString()).isAlarming()){
								ackMen.setEnabled(false); //disable acknowledgement through right-click if selected alarm point is a point that isn't alarming
							}
						}
					}
				}
				jpop.add(ignMen);
				jpop.add(ackMen);
				jpop.add(shvMen);
				jpop.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				this.requestFocusInWindow();
			}

		}
		@Override
		public void mousePressed(MouseEvent arg0) {
			if (arg0.getComponent() instanceof AlarmPanel){
				AlarmPanel clicked = (AlarmPanel)arg0.getComponent();
				String point = clicked.getPointName();
				if (SwingUtilities.isLeftMouseButton(arg0) && (arg0.isControlDown() || arg0.isShiftDown())){
					controlShiftIsDown = true;
					ignore.setEnabled(false);
					ack.setEnabled(false);
					shelve.setEnabled(false);
					notify.setEnabled(false);
					if (panelSelections.contains(clicked)){
						panelSelections.remove(clicked);
						clicked.highlight(Color.WHITE);
					} else {
						panelSelections.add(clicked);
						clicked.highlight(Color.YELLOW);
					}
				} else {
					if (SwingUtilities.isLeftMouseButton(arg0)){
						plist.clearSelection();
						try {
							plist.setSelectedValue(point, true);
						} catch (NullPointerException n){
							this.showDefaultAlarmPanels();
						}
					}
					controlShiftIsDown = false;
				}
				this.requestFocusInWindow();
			}

		}

		@Override
		public void keyPressed(KeyEvent arg0) {
			if (arg0.getKeyCode() == KeyEvent.VK_CONTROL || arg0.getKeyCode() == KeyEvent.VK_SHIFT){
				panelSelections.clear();
				ignore.setEnabled(false);
				ack.setEnabled(false);
				shelve.setEnabled(false);
				notify.setEnabled(false);
				AlarmManagerPanel.this.multiSelectLabel.setText("Multi-Select: ON");
				AlarmManagerPanel.this.multiSelectLabel.setBackground(Color.YELLOW);
				controlShiftIsDown = true;
			}
		}

		@Override
		public void keyReleased(KeyEvent arg0) {
			if (arg0.getKeyCode() == KeyEvent.VK_CONTROL || arg0.getKeyCode() == KeyEvent.VK_SHIFT){
				controlShiftIsDown = false;
				if (panelSelections.size() > 0){
					int[] indices = new int[panelSelections.size()];
					for (int i = 0,j = 0; i < localListModel.size(); i++){
						for (AlarmPanel s : panelSelections){
							if (s.getPointName().equals(localListModel.get(i))){
								indices[j] = i;
								j++;
								break;
							}
						}
					}
					plist.clearSelection();
					for (AlarmPanel ap : panelSelections){
						ap.highlight(Color.WHITE);
					}
					plist.setSelectedIndices(indices);
				}
				panelSelections.clear();
				AlarmManagerPanel.this.multiSelectLabel.setText("Multi-Select: OFF");
				AlarmManagerPanel.this.multiSelectLabel.setBackground(null);
			}
		}

		@Override
		public void keyTyped(KeyEvent arg0) {}

		/**
		 * Sets the boolean value for whether this panel's tab should start flashing.
		 * @param flash A boolean signifying whether this should start flashing or not.
		 */
		public void setFlashing(boolean flash){
			flashing = flash;
		}

		/**
		 * Method that stores the current list selections.
		 * @return Returns true if there were selections stored, otherwise false
		 */
		public synchronized boolean storeSelections(){
			selections = new String[plist.getSelectedValues().length];
			selections = plist.getSelectedValues();
			scrollBarPos = alarmDetailsScroller.getVerticalScrollBar().getValue();
			if (selections.length > 0){
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Method to reset the selections in the JList after they have been stored, if applicable
		 */
		public synchronized void setSelections(){
			if (selections.length > 0){
				try {
					int[] res = new int[selections.length];
					int i = 0;
					int j = 0;
					for (i = 0; i < plist.getModel().getSize(); i++){
						for (j = 0; j < selections.length; j++){
							if (plist.getModel().getElementAt(i).toString().equals(selections[j].toString())){
								res[j] = i;
								break;
							}
						}
					}
					this.plist.setSelectedIndices(res);
					alarmDetailsScroller.getVerticalScrollBar().setValue(scrollBarPos);
				} catch (Exception e){
					this.showDefaultAlarmPanels();
				}
			} else {
				this.showDefaultAlarmPanels();
			}
		}
	}

	// ///////////////////// END NESTED CLASS /////////////////////////////

	private Vector<String> itsPoints = new Vector<String>();
	private DefaultListModel itsListModel = new DefaultListModel();

	private JTabbedPane stateTabs;
	private static AlarmDisplayPanel all;
	private static AlarmDisplayPanel ignored;
	private static AlarmDisplayPanel acknowledged;
	private static AlarmDisplayPanel shelved;
	private static AlarmDisplayPanel alarming;

	private JPanel statusPanel = new JPanel();
	private JLabel allLabel = new JLabel("ALL: 0");
	private JLabel ignLabel = new JLabel("IGN: 0");
	private JLabel ackLabel = new JLabel("ACK: 0");
	private JLabel shvLabel = new JLabel("SHV: 0");
	private JLabel almLabel = new JLabel("ALM: 0");
	private JLabel multiSelectLabel = new JLabel("Multi-Select: OFF");

	private boolean alive = true;
	private AudioWarning  klaxon = new AudioWarning();

	/**
	 * C'tor
	 */
	public AlarmManagerPanel() {
		AlarmMaintainer.addListener(this);
		// Set layout
		this.setLayout(new BorderLayout());

		stateTabs = new JTabbedPane(JTabbedPane.TOP);
		all = new AlarmDisplayPanel("all");
		ignored = new AlarmDisplayPanel("ignored");
		acknowledged = new AlarmDisplayPanel("acknowledged");
		shelved = new AlarmDisplayPanel("shelved");
		alarming = new AlarmDisplayPanel("alarming");

		// Insert the tabs into the tabbed pane
		stateTabs.insertTab("All", null, all, "List of all alarms", 0);
		stateTabs.insertTab("Ignored", null, ignored, "List of ignored alarms", 1);
		stateTabs.insertTab("Acknowledged", null, acknowledged, "List of Acknowledged Alarms", 2);
		stateTabs.insertTab("Shelved", null, shelved, "List of Shelved Alarms", 3);
		stateTabs.insertTab("Alarming", null , alarming, "List of Currently Active alarms", 4);

		stateTabs.setForegroundAt(0, AlarmManagerPanel.ALL_COLOUR);
		stateTabs.setForegroundAt(1, AlarmManagerPanel.IGNORE_COLOUR);
		stateTabs.setForegroundAt(2, AlarmManagerPanel.ACKNOWLEDGED_COLOUR);
		stateTabs.setForegroundAt(3, AlarmManagerPanel.SHELVED_COLOUR);
		stateTabs.setForegroundAt(4, AlarmManagerPanel.ALARMING_COLOUR);

		stateTabs.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				final JTabbedPane source = (JTabbedPane) e.getSource();
				try{
					SwingUtilities.invokeLater(new Runnable(){
						@Override
						public void run(){
							AlarmDisplayPanel selectedTab = ((AlarmDisplayPanel) source.getSelectedComponent());
							JPanel replace = new JPanel();
							replace.setBackground(Color.WHITE);
							replace.setOpaque(true);
							selectedTab.alarmDetailsScroller.setViewportView(replace);
							selectedTab.showDefaultAlarmPanels();
							selectedTab.requestFocusInWindow();
							if (selectedTab.equals(alarming)){
								alarming.setFlashing(false);
							}
						}
					});
				} catch (NullPointerException n){
					System.err.println("Null Pointer Exception in selecting tabs");
				}
			}
		});

		multiSelectLabel.setOpaque(true);
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusPanel.add(allLabel);
		statusPanel.add(Box.createHorizontalGlue());
		statusPanel.add(ignLabel);
		statusPanel.add(Box.createHorizontalGlue());
		statusPanel.add(ackLabel);
		statusPanel.add(Box.createHorizontalGlue());
		statusPanel.add(shvLabel);
		statusPanel.add(Box.createHorizontalGlue());
		statusPanel.add(almLabel);
		statusPanel.add(Box.createHorizontalGlue());
		statusPanel.add(multiSelectLabel);

		this.add(stateTabs, BorderLayout.CENTER);
		this.add(statusPanel, BorderLayout.SOUTH);
	}

	/**
	 * Shorthand macro for calling the update methods for each of the tabs
	 */
	public static void updateListModels(){
		ignored.updateListModel();
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
			HashSet<String> selections = new HashSet<String>();
			HashSet<String> allPoints = new HashSet<String>();
			String[] names = PointDescription.getAllPointNames();
			for (int i = 0; i < names.length; i++){
				allPoints.add(names[i]);
			}
			for (int i = 0; i < listPoints.size(); i++) {
				selections.add(listPoints.get(i).toString());
			}
			selections = MonitorUtils.prunePointTree(selections, allPoints);

			int n = 0;
			for (String s : selections){
				if (n == 0){
					p += s;
				} else {
					p += ":" + s;
				}
				n++;
			}

			/*p += listPoints.get(0);
			// Then add rest of point names with a delimiter
			for (int i = 1; i < listPoints.size(); i++) {
				p += ":" + listPoints.get(i);
			}*/
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
		if (minorAlarms){
			ss.put(minorAlmStr, "true");
		} else {
			ss.put(minorAlmStr, "false");
		}
		if (majorAlarms){
			ss.put(majorAlmStr, "true");
		} else {
			ss.put(majorAlmStr, "false");
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

			long startTime = System.currentTimeMillis();
			while (AlarmMaintainer.getAllAlarms().size() == 0){
				//loop for 5 seconds
				if ((System.currentTimeMillis() - startTime) > 5000){
					JOptionPane.showMessageDialog(AlarmManagerPanel.this, "Error connecting to server", "Alarm Retrieval Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				continue;
			}
			/* Decoding the pruned values - this should be backwards compatible
			 * with old Setups. This should be relatively quick compared to parsing
			 * the Strings of (potentially) thousands of points using the StringTokenizer
			 * Even if the actual pruning operation is slow, loading a saved setup 
			 * should be relatively efficient compared to generating one. 
			 */
			HashSet<String> depruned = new HashSet<String>();
			HashSet<String> allPoints = new HashSet<String>();
			itsPoints = new Vector<String>();

			// Get the list of points to be monitored
			String p = (String) setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			while (stp.hasMoreTokens()) {
				depruned.add(stp.nextToken());
			}
			String[] names = MonClientUtil.getAllPointNames();
			for (int i = 0; i < names.length; i++){
				allPoints.add(names[i]);
			}

			depruned = MonitorUtils.sprout(depruned, allPoints);
			for (String s : depruned){
				itsPoints.add(s);
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
			str = (String) setup.get(minorAlmStr);
			if (str != null){
				if (str.equals("true")){
					minorAlarms = true;
				} else if (str.equals("false")){
					minorAlarms = false;
				}
			}
			str = (String) setup.get(majorAlmStr);
			if (str != null){
				if (str.equals("true")){
					majorAlarms = true;
				} else if (str.equals("false")){
					majorAlarms = false;
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
				try {
					if (AlarmMaintainer.getAlarm(s).getPriority() == -1 && !noPriorityAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 0 && !informationAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 1 && !minorAlarms){
						badPoints.add(s);
					}
					if (AlarmMaintainer.getAlarm(s).getPriority() == 2 && !majorAlarms){
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

			Collections.sort(itsPoints);

			this.updateLists();

			all.showDefaultAlarmPanels();
			ignored.showDefaultAlarmPanels();
			acknowledged.showDefaultAlarmPanels();
			shelved.showDefaultAlarmPanels();
			alarming.showDefaultAlarmPanels();

			stateTabs.setSelectedComponent(alarming);
			alarming.requestFocusInWindow();

			setPreferredSize(MonFrame.getDefaultSize());

			this.updateInfoBar();
		} catch (final Exception e) {
			e.printStackTrace();
			if (itsFrame != null) {
				JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
						+ "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
						JOptionPane.WARNING_MESSAGE);
			} else {
				System.err.println("AlarmManagerPanel:loadSetup: " + e.getClass().getName());
			}
			blankSetup();
			return false;
		}
		return true;
	}

	private void updateLists(){
		@SuppressWarnings ("unchecked")
		Vector<String> newPoints = (Vector<String>) itsPoints.clone();
		itsListModel.setSize(itsPoints.size());

		int i = 0;
		for (String s : newPoints){
			itsListModel.setElementAt(s, i);
			i++;
		}
		itsPoints = newPoints;

		ignored.updateListModel();
		acknowledged.updateListModel();
		shelved.updateListModel();
		alarming.updateListModel();
	}

	/**
	 * Shorthand method to update the information bar at the bottom of the AlarmManagerPanel
	 */
	private void updateInfoBar(){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				allLabel.setText("ALL: " + all.plist.getModel().getSize());
				ignLabel.setText("IGN: " + AlarmMaintainer.ignoreList.size());
				ackLabel.setText("ACK: " + acknowledged.plist.getModel().getSize());
				shvLabel.setText("SHV: " + shelved.plist.getModel().getSize());
				almLabel.setText("ALM: " + alarming.plist.getModel().getSize());
			}
		});
	}

	/**
	 * Method to create a new blank setup pane
	 */
	private void blankSetup() {
		itsPoints = new Vector<String>();
		itsListModel = new DefaultListModel();
		noPriorityAlarms = false;
		informationAlarms = false;
		minorAlarms = false;
		majorAlarms = false;
		severeAlarms = false;
	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#vaporise()
	 */
	@Override
	public void vaporise() {
		alive = false;
		AlarmMaintainer.removeListener(this);
	}

	@Override
	public void onAlarmEvent(AlarmEvent event) {
		// Don't do anything, we don't handle single alarm events
	}

	//boolean used to check if this is down - manually set after a KeyEvent or a MouseEvent
	// Not particularly elegant, but it fits the bill for the moment.
	private boolean controlShiftIsDown = false;

	@Override
	public void onAlarmEvent(Collection<AlarmEvent> events) {
		final AlarmDisplayPanel select = (AlarmDisplayPanel) stateTabs.getSelectedComponent();
		boolean selectionsStored = select.storeSelections();
		for (AlarmEvent event : events){
			if (!event.getAlarm().isSameAs(localAlarms.get(event.getAlarm().getPointDesc().getFullName()))){
				Alarm thisAlarm = event.getAlarm();
				if (!thisAlarm.isShelved() && !thisAlarm.isAcknowledged() && thisAlarm.isAlarming() && !AlarmMaintainer.ignoreList.contains(thisAlarm.getPointDesc().getFullName())){
					if (select.getType() != Alarm.ALARMING){
						alarming.setFlashing(true);
					} else {
						alarming.setFlashing(false);
					}
					if (!klaxon.isAlive()){
						klaxon.start();
					}
				}
				localAlarms.put(thisAlarm.getPointDesc().getFullName(), thisAlarm);
			}
		}
		updateListModels();
		if (!controlShiftIsDown){
			if (selectionsStored){
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run(){
						select.setSelections();
					}
				});
			} else {
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run(){
						select.showDefaultAlarmPanels();
					}
				});
			}
		}
		if (alarming.plist.getModel().getSize() == 0){
			alarming.setFlashing(false);
		}
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				if (AlarmMaintainer.autoAlarms){
					allowAutoAlarms.setSelected(true);
				} else {
					allowAutoAlarms.setSelected(false);
				}
			}
		});
		this.updateInfoBar();
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
			boolean highPriority = false;
			RelTime sleep = RelTime.factory(10000000);
			while (alive){
				try {
					if (!alarming.localListModel.isEmpty()) {
						if (muteOn){
							continue;
						} else {
							highPriority = false;
							for (int i = 0; i < alarming.localListModel.size(); i++){
								try {
									if (localAlarms.get(((String) alarming.localListModel.get(i))).getPriority() >= 1){
										highPriority = true;
										break;
									}
								} catch (NullPointerException n){
									if (AlarmMaintainer.getAlarm(((String) alarming.localListModel.get(i))).getPriority() >= 1){
										highPriority = true;
										break;
									}
								}
							}
						}
						if (highPriority){
							boolean success = MonClientUtil.playAudio("atnf/atoms/mon/gui/monpanel/watchdog.wav");
							if (success == false) throw (new Exception());
						}
					}
				} catch (Exception e) {
					System.err.println("Audio Playing failed");
				} finally {
					try {
						sleep.sleep();
					}catch (Exception ex){}
				}
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
			boolean res = false;
			String[] creds = LoginManager.getLoginCredentials(AlarmManagerPanel.this);
			try{
				if (action.equals("shelve")){
					res = AlarmMaintainer.setShelved(point, state, creds[0], creds[1]);
					updateListModels();
				} else if (action.equals("ack")){
					res = AlarmMaintainer.setAcknowledged(point, state, creds[0], creds[1]);
					updateListModels();
				} else {
					throw (new IllegalArgumentException());
				}
				if (!res) throw (new AuthenticationException());
			} catch (IllegalArgumentException i){
				LoginManager.setCredentials(new String[]{creds[0], ""});
				JOptionPane.showMessageDialog(AlarmManagerPanel.this, "You somehow sent an invalid command to the server - check the source code!", 
						"Invalid command Error", JOptionPane.ERROR_MESSAGE);

			} catch (AuthenticationException ae){
				LoginManager.setCredentials(new String[]{creds[0], ""});
				JOptionPane.showMessageDialog(AlarmManagerPanel.this, "Data transmission failed.\nPlease check your username and password.", 
						"Authentication Failure", JOptionPane.ERROR_MESSAGE);

			} catch (Exception e){
				LoginManager.setCredentials(new String[]{creds[0], ""});
				JOptionPane.showMessageDialog(AlarmManagerPanel.this, "Something went wrong with the sending of data. " +
						"\nPlease ensure that you're properly connected to the network.", 
						"Data Sending Error", JOptionPane.ERROR_MESSAGE);
			}
		}

	}
	// ///// END NESTED CLASS ///////

	/**
	 * Basic reverse quicksort implementation adapted from {@link http://www.javacodegeeks.com/2012/06/all-you-need-to-know-about-quicksort.html}
	 * @deprecated Uses {@link AlarmManagerPanel#alphaSortByPriority(String[])} instead.
	 * @param arr The array to be sorted
	 * @param beginIdx The index to begin sorting from
	 * @param len The length within the array to be sorted
	 * @return The sorted array (or subsection thereof)
	 */
	public static String[] basicReverseQuickSort(String arr[], int beginIdx, int len, HashMap<String, Alarm> lookup) {
		if ( len <= 1 )
			return arr;

		final int endIdx = beginIdx+len-1;

		// Pivot selection
		final int pivotPos = beginIdx+len/2;
		arr = swap(arr, pivotPos, endIdx);

		// partitioning
		int p = beginIdx;
		for(int i = beginIdx; i < endIdx; i++) {
			if (lookup.get(arr[i]).getPriority() >= lookup.get(arr[pivotPos]).getPriority()) {
				arr = swap(arr, i, p++);
			}
		}
		arr = swap(arr, p, endIdx);

		// recursive call
		arr = basicReverseQuickSort(arr, p+1,  endIdx-p, lookup);
		arr = basicReverseQuickSort(arr, beginIdx, p-beginIdx, lookup);
		return arr;
	}
	/**
	 * Simple utility method to swap two elements in a String[]
	 * @param arr The array that needs elements swapped
	 * @param i The index of the first element
	 * @param e The index of the second element
	 * @return The array with the elements swapped around
	 */
	private static String[] swap(String[] arr, int i, int e){
		String temp = arr[i];
		arr[i] = arr[e];
		arr[e] = temp;
		return arr;
	}

	/**
	 * Utility method to sort an array of Alarm names alphabetically, and by their priorities
	 * @param arr An array containing the names of the strings
	 * @return The sorted array
	 */
	private static ArrayList<String> alphaSortByPriority(String arr[]){
		ArrayList<String> sev = new ArrayList<String>();
		ArrayList<String> maj = new ArrayList<String>();
		ArrayList<String> min = new ArrayList<String>();
		ArrayList<String> inf = new ArrayList<String>();

		for (int i = 0; i < arr.length; i++){
			int pr = AlarmMaintainer.getAlarm(arr[i]).getPriority();
			if (pr == 3){
				sev.add(arr[i]);
			} else if (pr == 2){
				maj.add(arr[i]);
			} else if (pr == 1){
				min.add(arr[i]);
			} else if (pr == 0){
				inf.add(arr[i]);
			}
		}

		Collections.sort(sev);
		Collections.sort(maj);
		Collections.sort(min);
		Collections.sort(inf);

		sev.addAll(maj);
		sev.addAll(min);
		sev.addAll(inf);

		return sev;
	}
}