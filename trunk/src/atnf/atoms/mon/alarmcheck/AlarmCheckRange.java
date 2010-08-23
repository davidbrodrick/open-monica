//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import atnf.atoms.mon.*;
import atnf.atoms.util.Angle;

/**
 * <i>AlarmCheck</i> sub-class which checks if a numeric data value is within a nominated
 * range. The first two <i>init</i> arguments should be the lower and upper limits of the
 * range. An optional third argument defines whether inside the range is considered
 * nominal (<tt>True</tt> - the default) or outside the range is nominal (<tt>False</tt>).
 * 
 * @author David Brodrick
 * @author Le Cuong Nguyen
 */
public class AlarmCheckRange extends AlarmCheck
{
  /** The lower limit of the range. */
  private double itsLower = 0.0;

  /** The upper limit of the range. */
  private double itsUpper = 0.0;

  /**
   * Is it normal to be inside the range (<tt>true</tt>) or is a normal value outide
   * the range (<tt>false</tt>).
   */
  private boolean itsInsideNormal = true;

  public AlarmCheckRange(PointDescription parent, String[] args)
  {
    super(parent, args);

    if (args.length < 2) {
      // This is a problem
      System.err.println("AlarmCheckRange: Need at least two arguments!!!");
      itsUpper = itsLower = 0.0;
    } else {
      // Read the upper and lower limits of the numeric range
      itsLower = Double.parseDouble(args[0]);
      itsUpper = Double.parseDouble(args[1]);
      if (itsLower > itsUpper) {
        // Need to swap!
        double temp = itsUpper;
        itsUpper = itsLower;
        itsLower = temp;
      }
      // Check if the optional third argument was specified
      if (args.length == 3) {
        if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("t") || args[2].equalsIgnoreCase("1")) {
          itsInsideNormal = true;
        } else {
          itsInsideNormal = false;
        }
      }
    }
  }

  /**
   * Checks if the value is "normal" with respect to the specified limits.
   * @param data The value to check against our limits.
   */
  public void checkAlarm(PointData data)
  {
    // All zeroes means that we shouldn't even check
    if (itsUpper == itsLower && itsLower == 0.0) {
      return;
    }
    // No news is good news, right?
    if (data == null || data.getData() == null) {
      return;
    }

    Object myData = data.getData();
    double doubleData = 0.0;
    // Get the data as a double
    if (myData instanceof Number) {
      doubleData = ((Number) myData).doubleValue();
    } else if (myData instanceof Angle) {
      doubleData = ((Angle) myData).getValue();
    } else {
      System.err.println("AlarmCheckRange: ERROR: " + data.getName() + " has NON-NUMERIC data!");
      return;
    }

    // Check if the number is within the specified range
    boolean withinRange = false;
    if (doubleData >= itsLower && doubleData <= itsUpper) {
      withinRange = true;
    }

    // If it's inside the range and it's supposed to be, then all is well
    if ((!itsInsideNormal && withinRange) || (itsInsideNormal && !withinRange)) {
      data.setAlarm(true);
    }
  }

}
