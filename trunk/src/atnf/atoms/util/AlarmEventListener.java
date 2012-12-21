package atnf.atoms.util;

import atnf.atoms.mon.AlarmManager.AlarmEvent;


public interface AlarmEventListener {

	public void onAlarmEvent(AlarmEvent event);
}
