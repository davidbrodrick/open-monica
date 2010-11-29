//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.Calendar;
import java.util.TimeZone;

import atnf.atoms.mon.PointBuffer;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.time.AbsTime;

/**
 * Integrate every positive update of the input and reset once per day at the
 * specified time in the specified timezone.<BR>
 * 
 * For instance, this was initially implemented for the Parkes Telescope weather
 * station, which publishes the rainfall over the last 20 seconds. This
 * Translation is able to accumulate the rain tips over the course of the day
 * and reset the rain gauge at 9:01 local time.
 * 
 * <P>
 * The two init arguments for this are <tt>"09:01""Australia/Sydney"</tt>
 * 
 * @author David Brodrick
 */
public class TranslationDailyIntegratorPosOnly extends Translation {
  /** The hour to reset. */
  protected int itsHour = 0;

  /** The minute to reset. */
  protected int itsMinute = 0;

  /** The day of month when we last reset. */
  protected int itsLastReset = -1;

  /** The timezone in which the reset time is to be calculated. */
  protected TimeZone itsTZ = TimeZone.getTimeZone("UTC");

  /** The accumulated input for the day so far. */
  protected double itsSum = 0.0;

  /** Do we need to load our last value from the archive, on system startup. */
  protected boolean itsGetArchive = false;

  public TranslationDailyIntegratorPosOnly(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length < 2) {
      System.err.println("TranslationDailyIntegratorPosOnly: " + parent.getFullName() + ": NEED AT LEAST TWO INIT ARGUMENTS!");
    } else {
      // First argument is time
      int colon = init[0].indexOf(":");
      if (colon == -1 || init[0].length() > 5) {
        System.err.println("TranslationDailyIntegratorPosOnly: " + parent.getFullName() + ": NEED TIME IN HH:MM 24-HOUR FORMAT!");
      } else {
        try {
          itsHour = Integer.parseInt(init[0].substring(0, colon));
          itsMinute = Integer.parseInt(init[0].substring(colon + 1, init[0].length()));
        } catch (Exception e) {
          System.err.println("TranslationDailyIntegratorPosOnly: " + parent.getFullName() + ": NEED TIME IN HH:MM 24-HOUR FORMAT!");
        }
      }
      // TimeZone is second argument
      itsTZ = TimeZone.getTimeZone(init[1]);
      if (itsTZ == null) {
        System.err.println("TranslationDailyIntegratorPosOnly: " + parent.getFullName() + ": UNKNOWN TIMEZONE \"" + init[1] + "\"");
      }
      if (init.length == 3 && init[2].equalsIgnoreCase("true")) {
        itsGetArchive = true;
      }
    }
  }

  /** Calculate the average and return the integrated value. */
  public PointData translate(PointData data) {
    if (itsGetArchive) {
      // Need to reload last value from the archive
      PointData archiveval = PointBuffer.getPreceding(itsParent.getFullName(), new AbsTime());
      if (archiveval != null && archiveval.getData() instanceof Number) {
        // Calculate when the integral would have last reset
        Calendar c = Calendar.getInstance(itsTZ);
        c.set(Calendar.HOUR_OF_DAY, itsHour);
        c.set(Calendar.MINUTE, itsMinute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        AbsTime lastreset = AbsTime.factory(c.getTime());
        if (lastreset.isAfter(new AbsTime())) {
          // Calculated reset time is in the future, so subtract one day
          lastreset = lastreset.add(-86400000000l);
        }
        c.setTime(lastreset.getAsDate());
        if (lastreset.isBeforeOrEquals(archiveval.getTimestamp())) {
          // Archive value is after last reset, so use it as starting value
          itsSum = ((Number) archiveval.getData()).doubleValue();
          itsLastReset = c.get(Calendar.DAY_OF_YEAR);
        }
      }
      itsGetArchive = false;
    }

    // Extract the numeric value from this new input
    double thisvalue = 0.0;
    if (data != null && data.getData() != null) {
      if (data.getData() instanceof Number) {
        thisvalue = ((Number) (data.getData())).doubleValue();
      } else {
        System.err.println("TranslationDailyIntegratorPosOnly: " + itsParent.getFullName() + ": REQUIRES NUMERIC INPUT!");
      }
    }

    // Only integrate positive increments, and don't allow -0.0
    if (!(thisvalue > 0.0)) {
      thisvalue = 0.0;
    }

    // Check if it is time to reset the integrator
    Calendar c = Calendar.getInstance(itsTZ);
    if (c.get(Calendar.DAY_OF_YEAR) != itsLastReset
        && (c.get(Calendar.HOUR_OF_DAY) > itsHour || c.get(Calendar.HOUR_OF_DAY) == itsHour && c.get(Calendar.MINUTE) >= itsMinute)) {
      // Yep, we need to reset. This lastest update counts towards new sum
      itsLastReset = c.get(Calendar.DAY_OF_YEAR);
      itsSum = thisvalue;
    } else {
      // Not time to reset, so accumulate this value
      itsSum += thisvalue;
    }

    // Return the integrated sum
    return new PointData(itsParent.getFullName(), new AbsTime(), new Double(itsSum));
  }
}
