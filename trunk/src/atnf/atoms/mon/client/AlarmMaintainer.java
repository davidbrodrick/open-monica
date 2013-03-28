//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.gui.AlarmPopupFrame;
import atnf.atoms.time.RelTime;

/**
 * Class which periodically polls the server for the status of priority alarms, and presents a high level interface to client
 * objects which need to interact with alarms.
 * 
 * @author David Brodrick
 * @author Kalinga Hulugalle
 */
public class AlarmMaintainer implements Runnable {
	/** The polling interval. */
	private static final RelTime theirPollInterval = RelTime.factory(10000000);

	/** Record of points which are currently in a priority alarm state. */
	private static HashMap<PointDescription, Alarm> theirAlarms = new HashMap<PointDescription, Alarm>(500, 1000);

	/** Boolean to determine whether to allow automatic alarm popups or not */
	public static boolean autoAlarms = false;

	static {
		// Start the polling thread
		new Thread(new AlarmMaintainer(), "AlarmMaintainer Poller").start();
	}

	/** List of AlarmEventListeners currently registered to this source **/
	private static ArrayList<AlarmEventListener> theirListeners = new ArrayList<AlarmEventListener>();

	public static HashMap<String, AlarmPopupFrame> popupMap = new HashMap<String, AlarmPopupFrame>();
	private static String lastMapped = "";
	/** Centrally held list of ignored points*/
	public static HashSet<String> ignoreList = new HashSet<String>();
	private static HashSet<String> alarmingIgnores = new HashSet<String>();

	/**
	 * Registers the specified listener with the AlarmEvent source
	 * 
	 * @param listener
	 *          - the listener to be registered
	 */
	public static void addListener(AlarmEventListener listener) {
		theirListeners.add(listener);
	}

	/**
	 * Removes the specified listener from the list of currently registered listeners
	 * 
	 * @param listener
	 *          - the listener to be deregistered
	 */
	public static void removeListener(AlarmEventListener listener) {
		theirListeners.remove(listener);
	}

	/**
	 * For a each given Alarm, creates a new AlarmEvent and distributes it to all the registered listeners
	 * 
	 * @param a
	 *          The Collection of Alarm that the listeners are notified about
	 */
	private synchronized static void fireAlarmEvent(Collection<Alarm> a) {
		Vector<AlarmEvent> alarms = new Vector<AlarmEvent>();
		for (Alarm am : a){
			String name = am.getPointDesc().getFullName();
			if ((am.getAlarmStatus() != Alarm.ALARMING) && alarmingIgnores.contains(name)){
				removeIgnorePoint(name);
			}
			AlarmEvent ae = new AlarmEvent(am.getPointDesc(), am);
			alarms.add(ae);
		}
		for (AlarmEventListener ael : theirListeners) {
			ael.onAlarmEvent(alarms);
		}
	}

	/**
	 * Method to display automatically an Alarm notification popup frame if the alarm in question is actively alarming and is part of
	 * the highest bracket of alarm priorities.
	 * 
	 * @param a
	 *          The Alarm to display the notification about
	 */
	private synchronized static void displayAlarmNotification(final Alarm a) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				AlarmPopupFrame apf = new AlarmPopupFrame(a);
				apf.pack();
				apf.setVisible(true);
				if (popupMap.size() > 0) {
					apf.setLocationRelativeTo(popupMap.get(lastMapped));
				}
				popupMap.put(apf.getPointName(), apf);
				lastMapped = apf.getPointName();
			}
		});
	}


	@Override
	public void run() {
		while (true) {
			try {
				if (autoAlarms) { // if automatic alarm notifications are enabled
					// Fetch the list of all alarms from the server
					Vector<Alarm> newalarms = MonClientUtil.getServer().getAllAlarms();

					if (newalarms != null) {
						// Update the local alarm information
						synchronized (theirAlarms) {
							for (Alarm a : newalarms) {
								theirAlarms.put(a.getPointDesc(), a);
							}
						}
						// Notify any listeners about the updates
						if (newalarms.size() > 0){
							fireAlarmEvent(newalarms);
							for (Alarm a : newalarms) {
								if (a.getAlarmStatus() == Alarm.ALARMING && a.getPriority() >= 1 && !ignoreList.contains(a.getPointDesc().getFullName())){
									displayAlarmNotification(a);
								}
							}
						}
					}
				} else if (theirListeners.size() > 0) { // if automatic alarms are not enabled,
					// only update when there are registered listeners
					// Fetch the list of all alarms from the server
					Vector<Alarm> newalarms = MonClientUtil.getServer().getAllAlarms();

					if (newalarms != null) {
						// Update the local alarm information
						synchronized (theirAlarms) {
							for (Alarm a : newalarms) {
								theirAlarms.put(a.getPointDesc(), a);
							}
						}
						// Notify any listeners about the updates
						if (newalarms.size() > 0){
							fireAlarmEvent(newalarms);
						}
					}
				}
			} catch (Exception e) {
			}

			// Sleep awhile
			try {
				theirPollInterval.sleep();
			} catch (Exception e) {
			}
		}
	}

	/** Write the specified alarm acknowledgement change to the server. */
	public static boolean setAcknowledged(String point, boolean ack, String username, String password) {
		Vector<String> pointname = new Vector<String>(1);
		pointname.add(point);
		boolean res = setAcknowledged(pointname, ack, username, password);
		return res;
	}

	/** Write the specified alarm acknowledgement change to the server, and also temporarily to the local list until it gets updated */
	public static boolean setAcknowledged(Vector<String> pointnames, boolean ack, String username, String password) {
		boolean res = false;
		try {
			res = MonClientUtil.getServer().acknowledgeAlarms(pointnames, ack, username, password);
			if (res){
				for (String s : pointnames) {
					theirAlarms.get(PointDescription.getPoint(s)).setAcknowledged(ack);
				}
			}
		} catch (Exception e) {}
		return res;
	}

	/** Write the specified alarm shelving change to the server. */
	public static boolean setShelved(String point, boolean shelve, String username, String password) {
		Vector<String> pointname = new Vector<String>(1);
		pointname.add(point);
		boolean res = setShelved(pointname, shelve, username, password);
		return res;
	}

	/** Write the specified alarm shelving change to the server, and also temporarily to the local list until it gets updated */
	public static boolean setShelved(Vector<String> pointnames, boolean shelve, String username, String password) {
		boolean res = false;
		try {
			res = MonClientUtil.getServer().shelveAlarms(pointnames, shelve, username, password);
			if (res){
				for (String s : pointnames) {
					theirAlarms.get(PointDescription.getPoint(s)).setShelved(shelve);
				}
			}
		} catch (Exception e) {}
		return res;
	}

	/** Get the list of priority alarms currently in an alarm state (acknowledged or not) or not in an alarm but shelved. */
	public static Vector<Alarm> getAlarms() {
		Vector<Alarm> res;
		synchronized (theirAlarms) {
			res = new Vector<Alarm>(theirAlarms.size());
			Iterator<Alarm> i = theirAlarms.values().iterator();
			while (i.hasNext()) {
				Alarm thisalarm = i.next();
				if (thisalarm.isAlarming() || thisalarm.isShelved()) {
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

	/** Get the alarm for the given point, or null if not defined. */
	public static Alarm getAlarm(PointDescription point) {
		return theirAlarms.get(point);
	}

	/** Get the alarm for the given point, or null if not defined. */
	public static Alarm getAlarm(String pointname) {
		Alarm res = null;
		PointDescription point = PointDescription.getPoint(pointname);
		if (point != null) {
			res = getAlarm(point);
		}
		return res;
	}

	/**
	 * Initialises a simple Alarm locally if it is not already defined.
	 * 
	 * @param point
	 *          The PointDescription the new Alarm should be based on
	 */
	public static void setAlarm(PointDescription point) {
		Alarm res = getAlarm(point);
		if (res == null) {
			res = new Alarm(point);
			theirAlarms.put(point, res);
		}
	}

	public static void addIgnorePoint(String point){
		Alarm a = getAlarm(point);
		if (a.getAlarmStatus() == Alarm.ALARMING){
			alarmingIgnores.add(point);
		}
		ignoreList.add(point);
	}

	private static void removeIgnorePoint(String point){
		alarmingIgnores.remove(point);
		ignoreList.remove(point);
	}

	/** Simple test method to start the polling engine. */
	public static final void main(String[] args) {
		AlarmEventListener listener = new AlarmEventListener() {
			public void onAlarmEvent(AlarmEvent event) {
				System.err.println(event.getAlarm());
			}

			@Override
			public void onAlarmEvent(Collection<AlarmEvent> events) {
			}
		};
		AlarmMaintainer.addListener(listener);
		while (true) {
			try {
				theirPollInterval.sleep();
			} catch (Exception e) {
			}
		}
	}
}
