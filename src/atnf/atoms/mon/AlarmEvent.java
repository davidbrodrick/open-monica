// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.EventObject;

/**
 * Class created for utilisation with alarm notifications to Listeners
 * @author Kalinga Hulugalle
 * @see Alarm
 * @see AlarmEventListener
 *
 */
public class AlarmEvent extends EventObject{
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
