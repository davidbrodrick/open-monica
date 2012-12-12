package atnf.atoms.mon.gui.monpanel;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import atnf.atoms.mon.AlarmManager.Alarm;

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

	Alarm alarm;
	HashMap<Integer, String> rankLookup = new HashMap<Integer, String>();

	//Empty alarm panel when no alarms are selected
	public AlarmPanel(){

		this.setLayout(new GridBagLayout());
		JLabel nope = new JLabel("No selected Alarm point");
		this.add(nope);

	}

	public AlarmPanel(Alarm a) {
		// TODO Auto-generated constructor stub

		this.rankSetup();

		alarm = a;

		if (alarm == null) {
			this.setLayout(new GridBagLayout());
			JLabel nope = new JLabel("No selected Alarm point");
			this.add(nope);

		} else {

			this.setLayout(new GridLayout(5,1));

			JLabel alarmPriority = new JLabel(rankLookup.get(alarm.priority) + " Alarm".toUpperCase());
			alarmPriority.setForeground(Color.BLACK);
			alarmPriority.setFont(new Font("Serif", Font.BOLD, 48));

			// If the alarm is currently not alarmed, leave the colour as grey
			// so as to not be distracting
			if (alarm.getAlarmStatus() == Alarm.NOT_ALARMED){
				alarmPriority.setBackground(Color.GRAY);
			} else {

				if (alarm.priority == -1){
					alarmPriority.setBackground(Color.GRAY);
				} else if (alarm.priority == 1){
					alarmPriority.setBackground(Color.BLUE);
				} else if (alarm.priority == 2){
					alarmPriority.setBackground(Color.YELLOW);			
				} else if (alarm.priority == 3){
					alarmPriority.setBackground(Color.RED);
				} else {
					alarmPriority.setBackground(Color.DARK_GRAY);
				}
			}

			JLabel pointString = new JLabel("Point: " + alarm.data.getName());

			JPanel alarmStatus = new JPanel();
			JLabel statusString = new JLabel("Status: ");
			JLabel status = new JLabel();

			if (alarm.getAlarmStatus() == Alarm.ACKNOWLEDGED){
				status.setText("Acknowledged");
				status.setForeground(Color.YELLOW);
			} else if (alarm.getAlarmStatus() == Alarm.ALARMING){
				status.setText("Currently Alarming");
				status.setForeground(Color.RED);
			} else if (alarm.getAlarmStatus() == Alarm.NOT_ALARMED){
				status.setText("Not Alarmed");
				status.setForeground(Color.BLACK);
			} else if (alarm.getAlarmStatus() == Alarm.SHELVED){
				status.setText("Shelved");
				status.setForeground(Color.GREEN);
			}
			
			JLabel ackedBy = new JLabel("Acknowledged by: " + alarm.acknowledgedBy);
			JLabel ackedAt = new JLabel("Acknowledged at: " + alarm.acknowledgedAt);
			JLabel shelvedBy = new JLabel("Shelved by: " + alarm.shelvedBy);
			JLabel shelvedAt = new JLabel("Shelved at " + alarm.shelvedAt);
			
			statusString.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			status.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			ackedBy.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			ackedAt.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			shelvedBy.setFont(new Font("Sans Serif", Font.ITALIC, 24));
			shelvedAt.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			
			alarmStatus.setLayout(new BoxLayout(alarmStatus, BoxLayout.X_AXIS));
			alarmStatus.add(statusString);
			alarmStatus.add(status);
			if (alarm.getAlarmStatus() == Alarm.ALARMING){
				status.setForeground(Color.ORANGE);
			} else if (alarm.getAlarmStatus() == Alarm.ACKNOWLEDGED){
				status.setForeground(Color.YELLOW);
			} else if (alarm.getAlarmStatus() == Alarm.SHELVED){
				status.setForeground(Color.GREEN);
			}
			JLabel guidanceString = new JLabel("Guidance");
			JTextArea guidance = new JTextArea(alarm.guidance, 5, 20);
			guidance.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			guidance.setEditable(false);
			guidance.setWrapStyleWord(true);
			guidance.setLineWrap(true);


			this.add(alarmPriority);
			this.add(pointString);
			this.add(alarmStatus);
			if (alarm.getAlarmStatus() == Alarm.ACKNOWLEDGED){
				this.add(ackedBy);
				this.add(ackedAt);
			}
			if (alarm.getAlarmStatus() == Alarm.SHELVED){
				this.add(shelvedBy);
				this.add(shelvedAt);
			}
			this.add(guidanceString);
			this.add(guidance);
			
			int almStat = alarm.getAlarmStatus();
			while (true){ //loop forever
				// keep checking this alarm status, 
				// and remove/add components as necessary
				// NB: would be better to do this via a listener
				
				if (almStat != alarm.getAlarmStatus()){
					almStat = alarm.getAlarmStatus();
				this.removeAll();
				
				if (almStat == Alarm.ACKNOWLEDGED){
					status.setText("Acknowledged");
					status.setForeground(Color.YELLOW);
				} else if (almStat == Alarm.ALARMING){
					status.setText("Currently Alarming");
					status.setForeground(Color.RED);
				} else if (almStat == Alarm.NOT_ALARMED){
					status.setText("Not Alarmed");
					status.setForeground(Color.BLACK);
				} else if (almStat == Alarm.SHELVED){
					status.setText("Shelved");
					status.setForeground(Color.GREEN);
				}
				this.add(alarmPriority);
				this.add(pointString);
				this.add(alarmStatus);
				if (almStat == Alarm.ACKNOWLEDGED){
					this.add(ackedBy);
					this.add(ackedAt);
				}
				if (almStat == Alarm.SHELVED){
					this.add(shelvedBy);
					this.add(shelvedAt);
				}
				this.add(guidanceString);
				this.add(guidance);
				}
			}
		}

	}

	private void rankSetup(){
		rankLookup.put(-1, "No Priority");
		rankLookup.put(1, "Information") ;
		rankLookup.put(2, "Warning");
		rankLookup.put(3, "Severe");
	}

}

