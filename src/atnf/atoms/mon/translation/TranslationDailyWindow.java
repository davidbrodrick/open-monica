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
 * Generates "True" output between (inclusive) of the specified times each day,
 * the rest of the time the output is "False". This will do the right thing if
 * the output spans midnight (eg. if you specify a window from 11pm to 1am).
 * 
 * <P>
 * The three init arguments for this are the start time in "HH:MM" format, the
 * end time, and the time zone, eg: <tt>"09:00""09:30""Australia/Sydney"</tt>
 * 
 * @author David Brodrick
 */
public class TranslationDailyWindow extends Translation {
  /**
   * Timer used to trigger processing. TODO: Using a single static instance has
   * limited scaling potential. What would a better scheme be?
   */
  protected static Timer theirProcessTimer = new Timer();

  /** The hour to start. */
  protected int itsStartHour = 0;

  /** The minute to start. */
  protected int itsStartMinute = 0;

  /** The hour to end. */
  protected int itsEndHour = 0;

  /** The minute to end. */
  protected int itsEndMinute = 0;

  /** Does the time window span midnight? */
  protected boolean itsSpansMidnight = false;

  /** The timezone in which the reset time is to be calculated. */
  protected TimeZone itsTZ = TimeZone.getTimeZone("UTC");

  public TranslationDailyWindow(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length != 3) {
      throw new IllegalArgumentException("Insufficient arguments");
    } else {
      // First argument is start time
      int colon = init[0].indexOf(":");
      if (colon == -1 || init[0].length() > 5) {
        throw new IllegalArgumentException("Need start time in HH:MM 24-hour format");
      } else {
        try {
          itsStartHour = Integer.parseInt(init[0].substring(0, colon));
          itsStartMinute = Integer.parseInt(init[0].substring(colon + 1, init[0].length()));
        } catch (Exception e) {
          throw new IllegalArgumentException("Need start time in HH:MM 24-hour format");
        }
      }

      // Next argument is end time
      colon = init[1].indexOf(":");
      if (colon == -1 || init[1].length() > 5) {
        throw new IllegalArgumentException("Need end time in HH:MM 24-hour format");
      } else {
        try {
          itsEndHour = Integer.parseInt(init[1].substring(0, colon));
          itsEndMinute = Integer.parseInt(init[1].substring(colon + 1, init[1].length()));
        } catch (Exception e) {
          throw new IllegalArgumentException("Need end time in HH:MM 24-hour format");
        }
      }

      // Check if the end time is earlier than the start time (ie, window spans
      // midnight).
      if (itsEndHour < itsStartHour || itsEndHour == itsStartHour && itsEndMinute < itsStartMinute) {
        itsSpansMidnight = true;
      }

      // TimeZone is last argument
      itsTZ = TimeZone.getTimeZone(init[2]);
      if (itsTZ == null) {
        throw new IllegalArgumentException("Unknown timezone \"" + init[2] + "\"");
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
      Boolean val = new Boolean(false);

      // Check if the time is currently in the window
      Calendar c = Calendar.getInstance(itsTZ);
      if (!itsSpansMidnight) {
        if ((c.get(Calendar.HOUR_OF_DAY) == itsStartHour && c.get(Calendar.MINUTE) >= itsStartMinute || c
            .get(Calendar.HOUR_OF_DAY) > itsStartHour)
            && (c.get(Calendar.HOUR_OF_DAY) == itsEndHour && c.get(Calendar.MINUTE) <= itsEndMinute || c
                .get(Calendar.HOUR_OF_DAY) < itsEndHour)) {
          val = new Boolean(true);
        }
      } else {
        if (c.get(Calendar.HOUR_OF_DAY) == itsStartHour && c.get(Calendar.MINUTE) >= itsStartMinute
            || c.get(Calendar.HOUR_OF_DAY) > itsStartHour || c.get(Calendar.HOUR_OF_DAY) == itsEndHour
            && c.get(Calendar.MINUTE) <= itsEndMinute || c.get(Calendar.HOUR_OF_DAY) <= itsEndHour) {
          val = new Boolean(true);
        }
      }

      PointData res = new PointData(itsParent.getFullName(), new AbsTime(), val);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }
}
