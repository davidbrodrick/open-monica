// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.Collection;
import java.util.EventListener;

/**
 * Interface to enable the notification of AlarmEvents. Any class that wishes to subscribe
 * to AlarmEvents should implement this Interface.
 * @author Kalinga Hulugalle
 *
 */
public interface AlarmEventListener extends EventListener{

	/**
	 * Method that is called by the AlarmEvent source on each object that implements the
	 * AlarmEvent interface to notify that AlarmEventListener of a change in an Alarm object.
	 * @param event The AlarmEvent that encapsulates the information about this Alarm change
	 */
	public void onAlarmEvent(AlarmEvent event);
	
	/**
	 * Method that is called by the AlarmEvent source on each object that implements the
	 * AlarmEvent interface to notify that AlarmEventListener of a change in a Collection
	 * of Alarm objects.
	 * @param events The Collection of AlarmEvents that encapsulate the information about the Alarm changes
	 */
	public void onAlarmEvent(Collection<AlarmEvent> events);
}
