package atnf.atoms.mon;

import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Class encapsulating the current alarm status and associated data for a particular point. There is some degeneracy in this as
 * the structure is essentially self contained but also has reference to the parent PointDescription.
 */
public class Alarm {
	private PointDescription point;
	private PointData data;
	private boolean alarm = false;
	private boolean shelved = false;
	private String shelvedBy = null;
	private AbsTime shelvedAt = null;
	private boolean acknowledged = false;
	private String acknowledgedBy = null;
	private AbsTime acknowledgedAt = null;
	private int priority = 0;
	private String guidance = null;

	public static final int NOT_ALARMED = 0;
	public static final int ACKNOWLEDGED = 1;
	public static final int SHELVED = 2;
	public static final int ALARMING = 3;

	/**
	 * C'tor
	 * @param p - The PointDescription associated with this alarm
	 */
	public Alarm(PointDescription p) {
		point = p;
		priority = point.getPriority();
		data = null;
		alarm = false;
		acknowledged = false;
		shelved = false;
	}

	/**
	 * C'tor
	 * @param p - The PoinDescription associated with this alarm
	 * @param d - The PointData associated with this alarm
	 */
	public Alarm(PointDescription p, PointData d) {
		point = p;
		priority = point.getPriority();
		data = d;
		alarm = d.getAlarm();
	}

	public void updateData(PointData d) {
		data = d;
		if (data != null) {
			alarm = data.getAlarm();
			guidance = getGuidanceText();
		}
	}

	private String getGuidanceText() {
		String text = point.getGuidance();
		if (text != null && !text.isEmpty() && data != null) {
			text = MonitorUtils.doSubstitutions(text, data, point);
		}
		return text;
	}

	/**
	 * Formats this alarm into a human-readable String format
	 * @return Returns a string representation of this alarm
	 */
	public String toString() {
		String res = point.getFullName() + "\t" + priority + "\t" + alarm;
		res += "\t" + acknowledged + "\t" + acknowledgedBy;
		if (acknowledgedAt == null) {
			res += "\tnull";
		} else {
			res += "\t" + acknowledgedAt.toString(AbsTime.Format.HEX_BAT);
		}
		res += "\t" + shelved + "\t" + shelvedBy;
		if (shelvedAt == null) {
			res += "\tnull";
		} else {
			res += "\t" + shelvedAt.toString(AbsTime.Format.HEX_BAT);
		}
		res += "\t\"" + guidance + "\"";
		return res;
	}

	/**
	 * Gives a simple categorisation of what state this Alarm is in
	 * @return An <code><strong>int</strong></code> that corresponds to one of four
	 * states, NOT_ALARMED, ACKNOWLEDGED, SHELVED and ALARMING. The latter categories 
	 * take priority over the former ones. 
	 */
	public int getAlarmStatus(){
		int status = Alarm.NOT_ALARMED;
		if (this.acknowledged){
			status = Alarm.ACKNOWLEDGED;
		} else if (this.shelved){
			status = Alarm.SHELVED;
		} else if (this.alarm){ // will only get here if it is not shelved OR acknowledged, but still alarming
			status = Alarm.ALARMING;
		}
		return status;
	}

	/**
	 * The priority associated with this alarm point.
	 * @return An int holding the priority of the alarm, ranges between -1 and 3 inclusive
	 */
	public int getPriority(){
		return priority;
	}
	/**
	 * The name of the user who last acknowledged this alarm point, if applicable
	 * @return A String containing the name of the user
	 */
	public String getAckedBy(){
		return acknowledgedBy;
	}

	/**
	 * The time that this alarm point was last acknowledged
	 * @return An AbsTime value for when this point was acknowledged
	 */
	public AbsTime getAckedTime(){
		return acknowledgedAt;
	}

	/**
	 * Simple method that returns whether this alarm point is acknowledged or not
	 * @return A <code><strong>boolean</strong></code> holding the value.
	 */
	public boolean isAcknowledged(){
		return acknowledged;
	}
	
	/**
	 * Simple method to set the Acknowledged state of the Alarm
	 * @param bool The boolean state that the Acknowledgement state should be set to
	 */
	public void setAcknowledged(boolean state){
		this.acknowledged = state;
	}
	
	/**
	 * Simple method to set the Acknowledged state and other data of the Alarm
	 * @param state The state the Acknowledgement should take
	 * @param acknowledger The name of the person acknowledging this Alarm
	 * @param time The time of this acknowledgement
	 */
	public void setAcknowledged(boolean state, String acknowledger, AbsTime time){
		this.acknowledged = state;
		this.acknowledgedBy = acknowledger;
		this.acknowledgedAt = time;
	}
	/**
	 * Simple method to set the name of the person acknowedging the alarm
	 * @param acknowledger The name of the person acknowledging this Alarm
	 */
	public void setAcknowledgedBy(String acknowledger){
		this.acknowledgedBy = acknowledger;
	}
	/**
	 * Simple method to set the name of the person acknowedging the alarm
	 * @param time The time that the acknowledgedment is taking place
	 */
	public void setAcknowledgedAt(AbsTime time){
		this.acknowledgedAt = time;
	}
	
	/**
	 * The name of the user who last shelved this alarm point, if applicable
	 * @return A String containing the name of the user
	 */
	public String getShelvedBy(){
		return shelvedBy;
	}

	/**
	 * The time that this alarm point was last shelved
	 * @return An AbsTime value for when this point was shelved
	 */
	public AbsTime getShelvedAt(){
		return shelvedAt;
	}

	/**
	 * Simple method that returns whether this alarm point is shelved or not
	 * @return A <code><strong>boolean</strong></code> holding the value.
	 */
	public boolean isShelved(){
		return shelved;
	}
	
	/**
	 * Simple method to set the Shelving state of the Alarm
	 * @param bool The boolean state that the Shelving state should be set to
	 */
	public void setShelved(boolean state){
		this.shelved = state;
	}
	
	/**
	 * Simple method to set the Shelved state and other data of the Alarm
	 * @param state The state the Shelving should take
	 * @param shelver The name of the person shelving this Alarm
	 * @param time The time of this shelving
	 */
	public void setShelved(boolean state, String shelver, AbsTime time){
		this.shelved = state;
		this.shelvedBy = shelver;
		this.shelvedAt = time;
	}
	
	/**
	 * Simple method to set the name of the person shelving the alarm
	 * @param shelver The name of the person shelving this Alarm
	 */
	public void setShelvedBy(String shelver){
		this.shelvedBy = shelver;
	}
	/**
	 * Simple method to set the name of the person acknowedging the alarm
	 * @param time The time that the acknowledgedment is taking place
	 */
	public void setShelvedAt(AbsTime time){
		this.shelvedAt = time;
	}

	/**
	 * Returns a message conveying the actions that a user should take upon being
	 * alerted that this alarm has been activated.
	 * @return A String containing the message
	 */
	public String getGuidance(){
		return guidance;
	}
	/**
	 * Simple method that returns whether this alarm point is currently alarming or not
	 * @return A <code><strong>boolean</strong></code> holding the value.
	 */
	public boolean isAlarming(){
		return alarm;
	}
	/**
	 * Method to return the PointDescription (and associated metadata) related to this point
	 * @return the PointDescription
	 */
	public PointDescription getPointDesc(){
		return point;
	}
}
