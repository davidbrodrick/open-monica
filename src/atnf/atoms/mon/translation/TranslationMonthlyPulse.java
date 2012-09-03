//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.TimeZone;

import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;

/**
 * Generates a single "True" pulse at the specified time on the given day of each month, the rest of the time the output is "False".
 * It can be used, for instance, with the ResettableIntegrator to reset the integral at a given time each month.
 * 
 * <P>
 * The three init arguments for this are:
 * 
 * <ol>
 * <li>The day of month (first day is 1).
 * <li>The time of day in "HH:MM" 24 hour format.
 * <li>The time zone, eg: <tt>"09:01""Australia/Sydney"</tt>
 * </ol>
 * 
 * @author David Brodrick
 */
public class TranslationMonthlyPulse extends Translation {
  /**
   * Timer used to trigger processing. TODO: Using a single static instance has limited scaling potential. What would a better
   * scheme be?
   */
  protected static Timer theirProcessTimer = new Timer();

  /** The day of month to reset. */
  protected int itsDOM = 0;

  /** The hour to reset. */
  protected int itsHour = 0;

  /** The minute to reset. */
  protected int itsMinute = 0;

  /** The month when we last reset. */
  protected int itsLastPulseMonth = -1;

  /** The timezone in which the reset time is to be calculated. */
  protected TimeZone itsTZ = TimeZone.getTimeZone("UTC");

  /** Logger. */
  protected Logger theirLogger = Logger.getLogger(TranslationResettablePulse.class.getName());

  public TranslationMonthlyPulse(PointDescription parent, String[] init) throws Exception {
    super(parent, init);

    if (init.length != 3) {
      theirLogger.error("(" + parent.getName() + "): Requires three init arguments");
      throw new IllegalArgumentException("Requires three init arguments");
    }
    // First argument is day of month
    try {
      itsDOM = Integer.parseInt(init[0]);
    } catch (Exception e) {
      theirLogger.error("(" + parent.getName() + "): Could not parse first argument as day-of-month");
      throw e;
    }
    // Next is the time of day
    int colon = init[1].indexOf(":");
    if (colon == -1 || init[1].length() > 5) {
      theirLogger.error("(" + parent.getName() + "): Need second argument to be time in HH:MM 24-hour format");
      throw new IllegalArgumentException("Need second argument to be time in HH:MM 24-hour format");
    }
    try {
      itsHour = Integer.parseInt(init[1].substring(0, colon));
      itsMinute = Integer.parseInt(init[1].substring(colon + 1, init[1].length()));
    } catch (Exception e) {
      theirLogger.error("(" + parent.getName() + "): Could not parse time of day");
      throw e;
    }
    // TimeZone is third argument
    itsTZ = TimeZone.getTimeZone(init[2]);
    if (itsTZ == null) {
      theirLogger.error("(" + parent.getName() + "): Unknown timezone \"" + init[1] + "\"");
      throw new IllegalArgumentException("Unknown timezone \"" + init[1] + "\"");
    }

    // Start the timer
    // Parent's update interval in ms
    long period = (long) (parent.getPeriod() / 1000);
    theirProcessTimer.schedule(new PeriodicTickTask(), period, period);
  }

  /** Just returns the input (which is created by us) */
  public PointData translate(PointData data) {
    return data;
  }

  /** Called when timer expires. */
  private class PeriodicTickTask extends TimerTask {
    public void run() {
      Boolean val;

      // Check if it is time for the pulse
      Calendar c = Calendar.getInstance(itsTZ);
      if (c.get(Calendar.MONTH) != itsLastPulseMonth
          && (c.get(Calendar.DAY_OF_MONTH) == itsDOM && c.get(Calendar.HOUR_OF_DAY) == itsHour && c.get(Calendar.MINUTE) == itsMinute)) {
        // Yep, time for the pulse
        itsLastPulseMonth = c.get(Calendar.MONTH);
        val = new Boolean(true);
      } else {
        // Not time for the pulse
        val = new Boolean(false);
      }

      PointData res = new PointData(itsParent.getFullName(), new AbsTime(), val);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }
}
