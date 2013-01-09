package atnf.atoms.mon.client;

import java.util.ArrayList;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.time.AbsTime;

public class AlarmMaintainer implements Runnable{
	
	static {
	    new Thread(new AlarmMaintainer(), "AlarmMaintainer Poller").start();
	  }
	
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
	
	private synchronized static void fireAlarmEvent(Alarm a){
		AlarmEvent ae = new AlarmEvent(a.getPointDesc(), a);
		for (AlarmEventListener ael : listeners){
			ael.onAlarmEvent(ae);
		}
	}
	@Override
	public void run() {
		ArrayList<Alarm> newAlarms = new ArrayList<Alarm>();
		//TODO Periodically poll the server for a list of updated alarms
		for (Alarm a : newAlarms){
			fireAlarmEvent(a);
		}
	}
	public static void setAcknowledged(PointDescription point, boolean tempAck,
			String itsUser, AbsTime factory) {
		// TODO Auto-generated method stub
		
		updateServer();
		//fireAlarmEvent(thisalarm);
	}
	public static void setShelved(PointDescription point, boolean tempShlv,
			String itsUser, AbsTime factory) {
		// TODO Auto-generated method stub
		updateServer();
		//fireAlarmEvent(thisalarm);
	}
	public static Alarm getAlarm(PointDescription itsPointDesc) {
		// TODO Auto-generated method stub
		return null;
	}
	public static Alarm getAlarm(String string) {
		// TODO Auto-generated method stub
		return null;
	}
	public static void setAlarm(PointDescription point) {
		// TODO Auto-generated method stub
		updateServer();
		//fireAlarmEvent(thisalarm);
	}
	
	private static void updateServer(){
		// TODO Auto-generated method stub
	}
}
