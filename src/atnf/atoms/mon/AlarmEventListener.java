package atnf.atoms.mon;

import atnf.atoms.mon.AlarmManager.AlarmEvent;



public interface AlarmEventListener {

	public void onAlarmEvent(AlarmEvent event);
}
