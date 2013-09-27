//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import atnf.atoms.mon.*;

/**
 * Base-class for classes which check if a point is in an alarm state.
 * <P>
 * Sub-classes must implement an appropriate <i>checkAlarm</i> method. <i>checkAlarm</i> should set the PointData's alarm field to
 * True when an alarm condition is detected or leave the field unchanged otherwise.
 * 
 * <P>
 * If the <i>checkAlarm</i> method returns false then no further alarms tests for the point will be evaluated for this update - this
 * provides a mechanism so that classes can be implemented which make subsequent alarm checking conditional on an external
 * condition, such as the status of another point.
 * 
 * @author David Brodrick
 */
public abstract class AlarmCheck {
  /** The point that we are checking. */
  protected PointDescription itsParent;

  /** Constructor. */
  protected AlarmCheck(PointDescription parent, String[] args) {
    itsParent = parent;
  }

  /**
   * Check the value against the alarm criteria.
   * 
   * @param data
   *          New data value to check.
   * @return True normally, or False to abort evaluation of subsequent AlarmCheck objects for this update.
   */
  public abstract boolean checkAlarm(PointData data);
}
