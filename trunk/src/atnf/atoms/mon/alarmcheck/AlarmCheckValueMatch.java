//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;
import atnf.atoms.util.Angle;

/**
 * <i>AlarmCheck</i> sub-class which checks if a numeric data value is an exact match for the specified argument.
 * 
 * <P>
 * The default is to interpret any value <i>except</i> the one specified to be an alarm condition, but if the optional second
 * argument is <tt>false</tt> then we interpret any other value to be okay but an exact match to be an alarm condition.
 * 
 * <P>
 * An optional third argument specifies the number of update cycles that the value must be outside of the target range before the
 * alarm state is actually flagged (default is 1 update).
 * 
 * @author David Brodrick
 */
public class AlarmCheckValueMatch extends AlarmCheck {
  /** The number to match. */
  private double itsMatch = 0.0;

  /**
   * Is it normal to match the value (<tt>true</tt>) or is it normal to not match (<tt>false</tt>).
   */
  private boolean itsMatchNormal = true;

  /**
   * Minimum amount of times a value must appear in a row before raising an alarm
   */
  private int itsUpdateAmt = 0;

  /**
   * The amount of times an alarm value has appeared in a row
   */
  private int itsAlarmCount = 0;

  public AlarmCheckValueMatch(PointDescription parent, String[] args) throws IllegalArgumentException {
    super(parent, args);

    try {
      // Read the value
      itsMatch = Double.parseDouble(args[0]);

      // Check if the optional second argument was specified
      // Store the boolean argument for use in the alarm checking.
      if (args.length > 2) {
        itsMatchNormal = Boolean.parseBoolean(args[1]);
      }

      // This is the number of times a value should appear before setting
      // the alarm to True.
      // If no argument has been specified this defaults to an update
      // amount of 1.
      if (args.length > 2) {
        itsUpdateAmt = Integer.parseInt(args[2]);

      } else {
        itsUpdateAmt = 1;
      }

    } catch (Exception e) {
      Logger logger = Logger.getLogger(Factory.class.getName());
      logger.error("AlarmCheckValueMatch: Need at least one argument!");
    }
  }

  /**
   * Checks if the value is "normal" with respect to the specified limits.
   * 
   * @param data
   *          The value to check against our limits.
   * @return Always True.
   */
  public boolean checkAlarm(PointData data) {
    // No news is good news, right?
    if (data == null || data.getData() == null) {
      return true;
    }

    Object myData = data.getData();
    double doubleData = 0.0;

    // Retrieve the data as a double
    if (myData instanceof Number) {
      doubleData = ((Number) myData).doubleValue();
    } else if (myData instanceof Angle) {
      doubleData = ((Angle) myData).getValue();
    } else {
      Logger logger = Logger.getLogger(Factory.class.getName());
      logger.error("AlarmCheckValueMatch: ERROR: " + data.getName() + " has NON-NUMERIC data!");
      return true;
    }

    // If it's inside the range and it's supposed to be, then all is well
    if ((doubleData == itsMatch && !itsMatchNormal) || (doubleData != itsMatch && itsMatchNormal)) {
      itsAlarmCount++;
    } else {
      itsAlarmCount = 0;
    }

    // If the alarm count is the same as the amount specified in the config
    // file, set the alarm to True.
    if (itsAlarmCount >= itsUpdateAmt) {
      data.setAlarm(true);
    }
    return true;
  }
}