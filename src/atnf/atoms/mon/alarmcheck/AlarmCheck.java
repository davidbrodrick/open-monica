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
 * Sub-classes must implement an appropriate <i>checkAlarm</i> method. <i>checkAlarm</i>
 * should set the PointData's alarm field to True when an alarm condition is detected or
 * leave the field unchanged otherwise.
 * 
 * @author David Brodrick
 */
public abstract class AlarmCheck
{
  /** The point that we are checking. */
  protected PointDescription itsParent;

  /** Constructor. */
  protected AlarmCheck(PointDescription parent, String[] args)
  {
    itsParent = parent;
  }

  /**
   * Check the value against the alarm criteria.
   * @param data New data value to check.
   */
  public abstract void checkAlarm(PointData data);
}
