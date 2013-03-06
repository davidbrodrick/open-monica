// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.client.AlarmMaintainer;
import atnf.atoms.mon.gui.monpanel.AlarmManagerPanel;
import atnf.atoms.time.AbsTime.Format;

/**
 * AlarmPanel class for use by the AlarmManagerPanel class and the automated
 * AlarmPopupFrame class. Basic JPanel extension that displays
 * some basic information about an alarm in a user-friendly manner so they can 
 * quickly and easily assess the alarm and make a decision on what to do.
 * @author Kalinga Hulugalle
 * @see JPanel
 * @see AlarmManagerPanel
 * @see AlarmPopupFrame
 */
public class AlarmPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5673594608903644332L;
	
	String itsName;
	String itsUser;
	PointDescription itsPointDesc;
	Vector<Alarm> alarms;
	Alarm itsAlarm;
	HashMap<Integer, String> rankLookup = new HashMap<Integer, String>();
	int itsAlarmStatus;

	JLabel pointString;
	JLabel pointDesc;
	JPanel alarmStatus;
	JLabel statusString;
	JLabel status;
	JLabel ackedBy;
	JLabel ackedAt;
	JLabel shelvedBy;
	JLabel shelvedAt;
	JTextArea guidance;

	/**
	 * Constructor for an AlarmPanel
	 * @param name The name of the point in dotted-delimiter format
	 */
	public AlarmPanel(String name) {
		// Setup on new AlarmPanel instance
		this.rankSetup();
		this.setBackground(Color.WHITE);

		boolean hasPointDesc = false;

		itsName = name;
		itsPointDesc = PointDescription.getPoint(itsName);

		if (itsPointDesc != null) hasPointDesc = true;

		if (hasPointDesc){

			itsAlarm = AlarmMaintainer.getAlarm(itsPointDesc);
			if (itsAlarm == null) {
				System.err.println("No corresponding alarm for this PointDescription");
				AlarmMaintainer.setAlarm(itsPointDesc);
				itsAlarm = AlarmMaintainer.getAlarm(itsPointDesc);
			}

			itsAlarmStatus = itsAlarm.getAlarmStatus();

			this.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.insets = new Insets(0, 0, 3, 0);

			JPanel alarmTitle = new JPanel();
			alarmTitle.setLayout(new BoxLayout(alarmTitle, BoxLayout.X_AXIS));
			JLabel alarmPriority = new JLabel(rankLookup.get(itsAlarm.getPriority()) + " Alarm".toUpperCase());
			alarmPriority.setForeground(Color.BLACK);
			alarmPriority.setFont(new Font("Serif", Font.BOLD, 28));

			if (itsAlarm.getPriority() == -1 || !itsAlarm.isAlarming()){
				alarmTitle.setBackground(Color.GRAY);
			} else if (itsAlarm.getPriority() == 0){
				alarmTitle.setBackground(new Color(0x63B8FF));
			} else if (itsAlarm.getPriority() == 1){
				alarmTitle.setBackground(Color.YELLOW);
			} else if (itsAlarm.getPriority() == 2){
				alarmTitle.setBackground(new Color(0xFF7F24));			
			} else if (itsAlarm.getPriority() == 3){
				alarmTitle.setBackground(new Color(0xEE0000));
			} else {
				alarmTitle.setBackground(Color.DARK_GRAY);
			}

			alarmPriority.setHorizontalAlignment(JLabel.CENTER);
			pointString = new JLabel("Point: " + itsName);
			pointDesc = new JLabel("Description: " + itsPointDesc.getLongDesc());
			alarmStatus = new JPanel();
			statusString = new JLabel("Status: ");
			status = new JLabel();

			if (itsAlarm.getAlarmStatus() == Alarm.ACKNOWLEDGED){
				status.setText("Acknowledged");
				status.setForeground(AlarmManagerPanel.ACKNOWLEDGED_COLOUR);
			} else if (itsAlarm.getAlarmStatus() == Alarm.ALARMING){
				status.setText("Currently Alarming");
				status.setForeground(AlarmManagerPanel.ALARMING_COLOUR);
			} else if (itsAlarm.getAlarmStatus() == Alarm.NOT_ALARMED){
				status.setText("Not Alarmed");
				status.setForeground(AlarmManagerPanel.NOT_ALARMED_COLOUR);
			} else if (itsAlarm.getAlarmStatus() == Alarm.SHELVED){
				status.setText("Shelved");
				status.setForeground(AlarmManagerPanel.SHELVED_COLOUR);
			}
			ackedBy = new JLabel("Acknowledged by: " + itsAlarm.getAckedBy());
			ackedAt = new JLabel("Acknowledged at: " + itsAlarm.getAckedAt().toString(Format.UTC_STRING) + "(UTC)");
			//displayed time is always in UTC
			if (itsAlarm.getAckedBy().equals("null")){
				ackedBy.setText("Acknowledged by: ");
				ackedAt.setText("Acknowledged at: ");
			}
			shelvedBy = new JLabel("Shelved by: " + itsAlarm.getShelvedBy());
			shelvedAt = new JLabel("Shelved at " + itsAlarm.getShelvedAt().toString(Format.UTC_STRING) + "(UTC)");
			//displayed time is always in UTC
			if (itsAlarm.getShelvedBy().equals("null")){
				shelvedBy.setText("Shelved by: ");
				shelvedAt.setText("Shelved at: ");
			}
			pointString.setFont(new Font("Sans Serif", Font.BOLD, 14));
			pointDesc.setFont(new Font("Sans Serif", Font.BOLD, 14));
			statusString.setFont(new Font("Sans Serif", Font.BOLD, 14));
			status.setFont(new Font("Sans Serif", Font.BOLD, 14));
			ackedBy.setFont(new Font("Sans Serif", Font.BOLD, 14));
			ackedAt.setFont(new Font("Sans Serif", Font.BOLD, 14));
			shelvedBy.setFont(new Font("Sans Serif", Font.BOLD, 14));
			shelvedAt.setFont(new Font("Sans Serif", Font.BOLD, 14));

			alarmStatus.setLayout(new BoxLayout(alarmStatus, BoxLayout.X_AXIS));
			alarmStatus.add(statusString);
			alarmStatus.add(status);

			guidance = new JTextArea(itsAlarm.getGuidance(), 2, 10);
			guidance.setFont(new Font("Sans Serif", Font.PLAIN, 14));
			guidance.setEditable(false);
			guidance.setWrapStyleWord(true);
			guidance.setLineWrap(true);

			pointString.setBackground(Color.WHITE);
			pointDesc.setBackground(Color.WHITE);
			alarmStatus.setBackground(Color.WHITE);
			ackedBy.setBackground(Color.WHITE);
			ackedAt.setBackground(Color.WHITE);
			shelvedBy.setBackground(Color.WHITE);
			shelvedAt.setBackground(Color.WHITE);

			alarmTitle.setOpaque(true);
			pointString.setOpaque(true);
			pointDesc.setOpaque(true);
			alarmStatus.setOpaque(true);
			ackedBy.setOpaque(true);
			ackedAt.setOpaque(true);
			shelvedBy.setOpaque(true);
			shelvedAt.setOpaque(true);
			guidance.setOpaque(true);

			alarmTitle.add(Box.createHorizontalGlue());
			alarmTitle.add(alarmPriority);
			alarmTitle.add(Box.createHorizontalGlue());

			this.add(alarmTitle, gbc);
			gbc.gridy +=1;
			gbc.insets = new Insets(0, 10, 2, 10);
			this.add(pointString, gbc);
			gbc.gridy +=1;
			this.add(pointDesc, gbc);
			gbc.gridy +=1;
			this.add(alarmStatus, gbc);
			gbc.gridy +=1;
			if (itsAlarmStatus == Alarm.ACKNOWLEDGED){
				this.add(ackedBy, gbc);
				gbc.gridy +=1;
				this.add(ackedAt, gbc);
				gbc.gridy +=1;
			}
			if (itsAlarmStatus == Alarm.SHELVED){
				this.add(shelvedBy, gbc);
				gbc.gridy +=1;
				this.add(shelvedAt, gbc);
				gbc.gridy +=1;
			}
			if (this.getAlarm().isAlarming()){
				this.add(guidance, gbc);
				gbc.gridy +=1;
			}
		}
	}

	public String getPointName(){
		return itsName;
	}

	/**
	 * Method to map the alarm category ranks to their String counterparts.
	 */
	private void rankSetup(){
		rankLookup.put(-1, "NO PRIORITY");
		rankLookup.put(0, "INFORMATION");
		rankLookup.put(1, "MINOR") ;
		rankLookup.put(2, "MAJOR");
		rankLookup.put(3, "SEVERE");
	}

	/**
	 * Provides a means of highlighting the AlarmPanel in a different colour if necessary
	 * @param c The colour to highlight the alarm in
	 */
	public void highlight(Color c) {
		this.setBackground(c);
		pointString.setBackground(c);
		pointDesc.setBackground(c);
		alarmStatus.setBackground(c);
		statusString.setBackground(c);
		status.setBackground(c);
		ackedBy.setBackground(c);
		ackedAt.setBackground(c);
		shelvedBy.setBackground(c);
		shelvedAt.setBackground(c);
		guidance.setBackground(c);
	}

	/**
	 * Method to get a reference to the Alarm this AlarmPanel is describing
	 * @return The Alarm that dictates the contents of this AlarmPanel
	 */
	public Alarm getAlarm() {
		return itsAlarm;
	}

}

