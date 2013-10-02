//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * <i>AlarmCheck</i> sub-class which monitors the alarm status of a specified mask point. Depending on the alarm status of that
 * point this can prevent any additional alarm evaluations being performed on our parent point for this update cycle. This can be
 * used as a mechanism to make the alarm status of this point conditional on the alarm status of other points. For instance, a low
 * power on a photodetector might only be an alarm if the transmit LASER is turned on, but is not an alarm if the LASER is currently
 * turned off.
 * 
 * <P>
 * Requires one or two arguments:
 * <ol>
 * <li><b>Mask Point:</b> The name of the point whose alarm status we should use to mask our own.
 * <li><b>Mask Logic:</b> If set to <i>true</i> we mask alarm evaluation when the mask point is alarming (this is the default). If
 * <i>false</i> then the alarm evaluation will be masked when the mask point is NOT in an alarm state.
 * </ol>
 * 
 * @author David Brodrick
 */
public class AlarmCheckExternalMask extends AlarmCheck {
  /** The name of the alarm masking point to monitor. */
  protected String itsMaskPointName;

  /** Whether to abort our alarm evaluation when the mask point is alarming (true) or not (false). */
  protected boolean itsMaskWhenAlarming;

  public AlarmCheckExternalMask(PointDescription parent, String[] args) throws IllegalArgumentException {
    super(parent, args);

    if (args.length < 1) {
      throw new IllegalArgumentException("Requires a point name argument");
    }

    // Get the name of the point to monitor
    itsMaskPointName = args[0];
    // Substitute $1 macro, if used
    if (itsMaskPointName.indexOf("$1") > -1) {
      itsMaskPointName = MonitorUtils.replaceTok(itsMaskPointName, parent.getSource());
    }

    // Get the mask state
    if (args.length > 1) {
      itsMaskWhenAlarming = Boolean.parseBoolean(args[1]);
    } else {
      // By default, mask the alarm if the mask point is alarming
      itsMaskWhenAlarming = true;
    }
  }

  /**
   * Checks the alarm status and aborts subsequent alarm evaluation steps for our parent point if required.
   * 
   * @param data
   *          The value to check against our limits.
   * @return Always false if the mask point requires us to abort further alarm evaluation.
   */
  public boolean checkAlarm(PointData data) {
    // Get the latest data value for the mask point
    PointData maskdata = PointBuffer.getPointData(itsMaskPointName);

    if (maskdata != null && maskdata.getData() != null) {
      if (maskdata.getAlarm() == itsMaskWhenAlarming) {
        // Mask the alarm for our parent point by aborting evaluation of any subsequent alarms
        return false;
      }
    }

    // Alarm evaluation is not masked
    return true;
  }
}
