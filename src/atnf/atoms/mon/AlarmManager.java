//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;
import atnf.atoms.util.AlarmEventListener;

/**
 * Class that encapsulates most data about alarms, and facilitates the retrieval and modification
 * of those alarms
 * @author David Brodrick
 * @author Kalinga Hulugalle
 *
 */
public class AlarmManager {
	/**
	 * Class encapsulating the current alarm status and associated data for a particular point. There is some degeneracy in this as
	 * the structure is essentially self contained but also has reference to the parent PointDescription.
	 */
	public static class Alarm {
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

		private void updateData(PointData d) {
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

	/** Record of points which are currently in a priority alarm state. */
	private static HashMap<PointDescription, Alarm> theirAlarms = new HashMap<PointDescription, Alarm>(500, 1000);

	/** List of AlarmEventListeners currently registered to this source **/
	private static ArrayList<AlarmEventListener> listeners = new ArrayList<AlarmEventListener>();

	/**
	 * Registers the specified listener with the AlarmEvent source
	 * @param listener - the listener to be registered
	 */
	public static void addListener(AlarmEventListener listener){
		listeners.add(listener);
	}
	/**
	 * Removes the specified listener from the list of currently registered listeners
	 * @param listener - the listener to be deregistered
	 */
	public static void removeListener(AlarmEventListener listener){
		listeners.remove(listener);
	}
	/** Set the current alarm status for the given point. */
	public static void setAlarm(PointDescription point, PointData data) {
		synchronized (theirAlarms) {
			Alarm thisalarm = theirAlarms.get(point);
			if (thisalarm != null) {
				// Just update the extant data structure
				thisalarm.updateData(data);
				// Acknowledgement gets cleared if no longer in alarm
				if (!thisalarm.alarm && thisalarm.acknowledged) {
					thisalarm.acknowledged = false;
					thisalarm.acknowledgedBy = null;
					thisalarm.acknowledgedAt = null;
				}
			} else {
				// Need to create new data structure
				thisalarm = new Alarm(point, data);
				theirAlarms.put(point, thisalarm);
			}
			fireAlarmEvent(thisalarm);
		}
	}
	/**
	 * Returns the corresponding alarm for this point
	 * @param point - The String-formatted name of the point
	 * @return The Alarm that corresponds to this point name
	 */
	public static Alarm getAlarm(String point){
		Alarm res = null;
		synchronized (theirAlarms){
			Iterator<Alarm> i = theirAlarms.values().iterator();
			while (i.hasNext()){
				Alarm thisAlarm = i.next();
				if (thisAlarm.point.getFullName().equals(point)){
					res = thisAlarm;
					break;
				}
			}
		}
		return res;
	}

	/**
	 * Returns the corresponding alarm for this point
	 * @param point - The PointDescription for the point
	 * @return The Alarm that corresponds to this PointDescription
	 */
	public static Alarm getAlarm(PointDescription point){
		Alarm res = null;
		synchronized (theirAlarms){
			Iterator<Alarm> i = theirAlarms.values().iterator();
			while (i.hasNext()){
				Alarm thisAlarm = i.next();
				if (thisAlarm.point.equals(point)){
					res = thisAlarm;
					break;
				}
			}
		}
		return res;
	}

	/**
	 * Sets an alarm for this PointDescription, or creates a new one if none exists
	 * @param point - The PointDescription for the point
	 */
	public static void setAlarm(PointDescription point) {
		synchronized (theirAlarms) {
			Alarm thisalarm = theirAlarms.get(point);
			if (thisalarm != null) {
				// Acknowledgement gets cleared if no longer in alarm
				if (!thisalarm.alarm && thisalarm.acknowledged) {
					thisalarm.acknowledged = false;
					thisalarm.acknowledgedBy = null;
					thisalarm.acknowledgedAt = null;
				}
			} else {
				// Need to create new data structure
				thisalarm = new Alarm(point);
				theirAlarms.put(point, thisalarm);
			}
			fireAlarmEvent(thisalarm);
		}
	}

	/** Get the list of priority alarms currently in an alarm state (acknowledged or not) or not in an alarm but shelved. */
	public static Vector<Alarm> getAlarms() {
		Vector<Alarm> res;
		synchronized (theirAlarms) {
			res = new Vector<Alarm>(theirAlarms.size());
			Iterator<Alarm> i = theirAlarms.values().iterator();
			while (i.hasNext()) {
				Alarm thisalarm = i.next();
				if (thisalarm.alarm || thisalarm.shelved) {
					res.add(thisalarm);
				}
			}
		}
		return res;
	}

	/** Get the list of all alarms currently defined in the system. */
	public static Vector<Alarm> getAllAlarms() {
		Vector<Alarm> res;
		synchronized (theirAlarms) {
			res = new Vector<Alarm>(theirAlarms.size());
			Iterator<Alarm> i = theirAlarms.values().iterator();
			while (i.hasNext()) {
				Alarm thisalarm = i.next();
				res.add(thisalarm);
			}
		}
		return res;
	}

	/** Acknowledge an alarm. */
	public static void setAcknowledged(PointDescription point, boolean acked, String user, AbsTime time) {
		synchronized (theirAlarms) {
			Alarm thisalarm = theirAlarms.get(point);
			if (thisalarm == null) {
				// Need to create new data structure
				thisalarm = new Alarm(point);
				theirAlarms.put(point, thisalarm);
			}
			thisalarm.acknowledged = acked;
			thisalarm.acknowledgedBy = user;
			thisalarm.acknowledgedAt = time;
			fireAlarmEvent(thisalarm);
		}
	}

	/** Shelve an alarm. */
	public static void setShelved(PointDescription point, boolean shelved, String user, AbsTime time) {
		synchronized (theirAlarms) {
			Alarm thisalarm = theirAlarms.get(point);
			if (thisalarm == null) {
				// Need to create new data structure
				thisalarm = new Alarm(point);
				theirAlarms.put(point, thisalarm);
			}
			thisalarm.shelved = shelved;
			thisalarm.shelvedBy = user;
			thisalarm.shelvedAt = time;
			fireAlarmEvent(thisalarm);
		}
	}

	private synchronized static void fireAlarmEvent(Alarm a){
		AlarmEvent ae = new AlarmEvent(a.point, a);
		for (AlarmEventListener ael : listeners){
			ael.onAlarmEvent(ae);
		}
	}

	/**
	 * Class created for utilisation with alarm notifications to Listeners
	 * @author Kalinga Hulugalle
	 *
	 */
	public static class AlarmEvent extends EventObject{
		/**
		 * 
		 */
		private static final long serialVersionUID = 6237646518729914716L;
		private Alarm alarm;
		private Object source;
		/**
		 * C'tor for a new AlarmEvent
		 * @param source - the source of the Alarm, typically the String-formatted name of the point
		 * @param a - the Alarm that is associated with this event
		 */
		public AlarmEvent(Object source, Alarm a){
			super(source);
			this.source = source;
			this.alarm = a;
		}

		/**
		 * Method to return the alarm associated with this event
		 * @return The Alarm
		 */
		public Alarm getAlarm(){
			return this.alarm;
		}

		/**
		 * Method to return the source of this event
		 * @return The source Object
		 */
		public Object getSource(){
			return this.source;
		}

	}

}
