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

import atnf.atoms.time.AbsTime;
import atnf.atoms.mon.Alarm;

/**
 * Class that encapsulates most data about alarms, and facilitates the retrieval and modification
 * of those alarms
 * @author David Brodrick
 * @author Kalinga Hulugalle
 *
 */
public class AlarmManager {
	

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
				if (!thisalarm.isAlarming() && thisalarm.isAcknowledged()) {
					thisalarm.setAcknowledged(false, null, null);
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
				if (thisAlarm.getPointDesc().getFullName().equals(point)){
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
				if (thisAlarm.getPointDesc().equals(point)){
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
				if (!thisalarm.isAlarming() && thisalarm.isAcknowledged()) {
					thisalarm.setAcknowledged(false, null, null);
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
				if (thisalarm.isAlarming()|| thisalarm.isShelved()) {
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
			thisalarm.setAcknowledged(acked, user, time);
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
			thisalarm.setShelved(shelved, user, time);
			fireAlarmEvent(thisalarm);
		}
	}

	private synchronized static void fireAlarmEvent(Alarm a){
		AlarmEvent ae = new AlarmEvent(a.getPointDesc(), a);
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