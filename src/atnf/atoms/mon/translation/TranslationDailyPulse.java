//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.TimeZone;

import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;

/**
 * Generates a single "True" pulse at the specified time each day, the rest of
 * the time the output is "False". This is envisaged to be used with the Pulse
 * Translation to allow definition of time windows for certain operations to
 * happen.
 * 
 * <P>
 * The two init arguments for this are time in "HH:MM" format and the time zone,
 * eg: <tt>"09:01""Australia/Sydney"</tt>
 * 
 * @author David Brodrick
 */
public class TranslationDailyPulse extends Translation {
  /**
   * Timer used to trigger processing. TODO: Using a single static instance has
   * limited scaling potential. What would a better scheme be?
   */
  protected static Timer theirProcessTimer = new Timer();

  /** The hour to reset. */
  protected int itsHour = 0;

  /** The minute to reset. */
  protected int itsMinute = 0;

  /** The day of month when we last reset. */
  protected int itsLastPulseDay = -1;

  /** The timezone in which the reset time is to be calculated. */
  protected TimeZone itsTZ = TimeZone.getTimeZone("UTC");

  public TranslationDailyPulse(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length != 2) {
      System.err.println("TranslationDailyPulse: " + parent.getName() + ": NEED TWO INIT ARGUMENTS!");
    } else {
      // First argument is time
      int colon = init[0].indexOf(":");
      if (colon == -1 || init[0].length() > 5) {
        System.err.println("TranslationDailyPulse: " + parent.getName() + ": NEED TIME IN HH:MM 24-HOUR FORMAT!");
      } else {
        try {
          itsHour = Integer.parseInt(init[0].substring(0, colon));
          itsMinute = Integer.parseInt(init[0].substring(colon + 1, init[0].length()));
        } catch (Exception e) {
          System.err.println("TranslationDailyPulse: " + parent.getName() + ": NEED TIME IN HH:MM 24-HOUR FORMAT!");
        }
      }
      // TimeZone is second argument
      itsTZ = TimeZone.getTimeZone(init[1]);
      if (itsTZ == null) {
        System.err.println("TranslationDailyPulse: " + parent.getName() + ": UNKNOWN TIMEZONE \"" + init[1] + "\"");
      }
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
      if (c.get(Calendar.DAY_OF_YEAR) != itsLastPulseDay && (c.get(Calendar.HOUR_OF_DAY) == itsHour && c.get(Calendar.MINUTE) == itsMinute)) {
        // Yep, time for the pulse
        itsLastPulseDay = c.get(Calendar.DAY_OF_YEAR);
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
