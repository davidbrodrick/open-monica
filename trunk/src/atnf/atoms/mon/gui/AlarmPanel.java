package atnf.atoms.mon.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmManager;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.gui.monpanel.AlarmManagerPanel;

/**
 * AlarmPanel class for use by the AlarmManagerPanel class and the automated
 * alarm popup alert functionality (TBC). Basic JPanel extension that displays
 * some basic information about an alarm in a user-friendly manner so they can 
 * quickly and easily assess the alarm and make a decision on what to do.
 * @author Kalinga Hulugalle
 * @see AlarmManagerPanel
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

	//Empty alarm panel when no alarms are selected
	public AlarmPanel(){

		this.setLayout(new GridBagLayout());
		JLabel nope = new JLabel("No selected Alarm point");
		this.add(nope);

	}

	public AlarmPanel(String name) {

		// Setup on new AlarmPanel instance
		this.rankSetup();

		boolean hasPointDesc = false;

		itsName = name;
		itsPointDesc = PointDescription.getPoint(itsName);
		/*		System.out.println("DEBUG: Full Name: " + itsPointDesc.getFullName());
		System.out.println("DEBUG: Long Description: " + itsPointDesc.getLongDesc());
		System.out.println("DEBUG: Period: " + itsPointDesc.getPeriod());
		System.out.println("DEBUG: Enabled: " + itsPointDesc.getEnabled());
		System.out.println("DEBUG: Archive Policy: " + itsPointDesc.getArchivePolicyString());
		System.out.println("DEBUG: Guidance: " + itsPointDesc.getGuidance());
		System.out.println("DEBUG: Alarm Conditions: " + itsPointDesc.getAlarmCheckString());
		System.out.println("DEBUG: Notifications: " + itsPointDesc.getNotificationString());
		System.out.println("DEBUG: String Equivalent: " + itsPointDesc.getStringEquiv());
		System.out.println("DEBUG: Priority: " + itsPointDesc.getPriority());*/
		alarms = AlarmManager.getAllAlarms(); //update current alarms

		if (itsPointDesc != null) hasPointDesc = true;


		if (hasPointDesc){

			itsAlarm = AlarmManager.getAlarm(itsPointDesc);
			if (itsAlarm == null) System.err.println("No corresponding alarm for this PointDescription");

			itsAlarmStatus = itsAlarm.getAlarmStatus();

			if (itsAlarmStatus == Alarm.NOT_ALARMED || itsAlarmStatus == Alarm.ALARMING){
				this.setLayout(new GridLayout(6,1));
			} else {
				this.setLayout(new GridLayout(8,1));
			}

			JLabel alarmPriority = new JLabel(rankLookup.get(itsAlarm.getPriority()) + " Alarm".toUpperCase());
			alarmPriority.setForeground(Color.BLACK);
			alarmPriority.setFont(new Font("Serif", Font.BOLD, 32));

			if (itsAlarm.getPriority() == -1){
				alarmPriority.setBackground(Color.GRAY);
			} else if (itsAlarm.getPriority() == 0){
				alarmPriority.setBackground(new Color(0x63B8FF));
			} else if (itsAlarm.getPriority() == 1){
				alarmPriority.setBackground(Color.YELLOW);
			} else if (itsAlarm.getPriority() == 2){
				alarmPriority.setBackground(new Color(0xFF7F24));			
			} else if (itsAlarm.getPriority() == 3){
				alarmPriority.setBackground(new Color(0xEE0000));
			} else {
				alarmPriority.setBackground(Color.DARK_GRAY);
			}


			alarmPriority.setHorizontalAlignment(JLabel.CENTER);
			JLabel pointString = new JLabel("Point: " + itsName);
			JLabel pointDesc = new JLabel("Description: " + itsPointDesc.getLongDesc());
			JPanel alarmStatus = new JPanel();
			JLabel statusString = new JLabel("Status: ");
			JLabel status = new JLabel();

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

			JLabel ackedBy = new JLabel("Acknowledged by: " + itsAlarm.getAckedBy());
			JLabel ackedAt = new JLabel("Acknowledged at: " + itsAlarm.getAckedAt());
			JLabel shelvedBy = new JLabel("Shelved by: " + itsAlarm.getShelvedBy());
			JLabel shelvedAt = new JLabel("Shelved at " + itsAlarm.getShelvedAt());

			pointString.setFont(new Font("Sans Serif", Font.PLAIN, 24));
			pointDesc.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			statusString.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			status.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			ackedBy.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			ackedAt.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			shelvedBy.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			shelvedAt.setFont(new Font("Sans Serif", Font.ITALIC, 18));

			alarmStatus.setLayout(new BoxLayout(alarmStatus, BoxLayout.X_AXIS));
			alarmStatus.add(statusString);
			alarmStatus.add(status);

			JLabel guidanceString = new JLabel("Guidance:");
			JTextArea guidance = new JTextArea(itsAlarm.getGuidance(), 5, 20);
			guidanceString.setFont(new Font("Sans Serif", Font.ITALIC, 18));
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
			guidanceString.setBackground(Color.WHITE);

			alarmPriority.setOpaque(true);
			pointString.setOpaque(true);
			pointDesc.setOpaque(true);
			alarmStatus.setOpaque(true);
			ackedBy.setOpaque(true);
			ackedAt.setOpaque(true);
			shelvedBy.setOpaque(true);
			shelvedAt.setOpaque(true);
			guidanceString.setOpaque(true);
			guidance.setOpaque(true);


			this.add(alarmPriority);
			this.add(pointString);
			this.add(pointDesc);
			this.add(alarmStatus);
			if (itsAlarmStatus == Alarm.ACKNOWLEDGED){
				this.add(ackedBy);
				this.add(ackedAt);
			}
			if (itsAlarmStatus == Alarm.SHELVED){
				this.add(shelvedBy);
				this.add(shelvedAt);
			}
			this.add(guidanceString);
			this.add(guidance);

		} else {

			this.setLayout(new GridBagLayout());
			JLabel nope = new JLabel("No selected Alarm point");
			this.add(nope);

		}


	}

	private void rankSetup(){
		rankLookup.put(-1, "NO PRIORITY");
		rankLookup.put(0, "INFORMATION");
		rankLookup.put(1, "WARNING") ;
		rankLookup.put(2, "DANGER");
		rankLookup.put(3, "SEVERE");
	}

}

