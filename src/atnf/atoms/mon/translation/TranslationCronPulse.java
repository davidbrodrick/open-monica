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

import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.TimeZone;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.AbsTime;

/**
 * Generates a single "True" pulse at the specified time given in cron format, the rest of the time the output is "False".
 * It can be used, for instance, with the ResettableIntegrator to reset the integral at a given time each month.
 * 
 * <P>
 * The two init arguments for this are:
 * 
 * <ol>*/
// <li>The cron string, i.e. "*/30 */6 * * Mon,Fri" will return true at 0:30, 6:30, 12:30, 18:30 every Mon and Fri.
/* <li>The time zone, eg: <tt>"09:01""Australia/Sydney"</tt>
 * </ol>
 * 
 * @author Balt Indermuehle
 */
public class TranslationCronPulse extends Translation {
  /**
   * Timer used to trigger processing. TODO: Using a single static instance has limited scaling potential. What would a better
   * scheme be?
   */
  protected static Timer theirProcessTimer = new Timer();

  /** The date to reset. */
  protected Crontab itsCrontab = null;

  /** The date we last reset. */
  protected Calendar itsLastPulse = null;

  /** The timezone in which the reset time is to be calculated. */
  protected TimeZone itsTZ = TimeZone.getTimeZone("UTC");

  /** Logger. */
  protected Logger theirLogger = Logger.getLogger(TranslationResettablePulse.class.getName());

  public TranslationCronPulse(PointDescription parent, String[] init) throws Exception {
    super(parent, init);

    if (init.length != 2) {
      theirLogger.error("(" + parent.getName() + "): Requires two init arguments");
      throw new IllegalArgumentException("Requires two init arguments");
    }
    // First argument is cron string
    try {
      itsCrontab = new Crontab(init[0]);
    } catch (Exception e) {
      theirLogger.error("(" + parent.getName() + "): Could not parse first argument as valid crontab string");
      throw e;
    }
    // TimeZone is second argument
    itsTZ = TimeZone.getTimeZone(init[1]);
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
      Calendar rightNow = Calendar.getInstance(itsTZ);
      rightNow.set(GregorianCalendar.SECOND, 0);
      rightNow.set(GregorianCalendar.MILLISECOND, 0);
      
      if (itsCrontab.RunNow(itsTZ)) {
        if (itsLastPulse == null) {
          val = new Boolean(true);
          itsLastPulse = rightNow;
         } else if ( ! rightNow.equals(itsLastPulse)) {
          val = new Boolean(true);
          //theirLogger.debug("RunNow");
           itsLastPulse = rightNow;
        } else {
          // Not time for the pulse
          val = new Boolean(false);
          //theirLogger.debug("Do not RunNow - time blocking");        
        }
        
      } else {
        // Not time for the pulse
        val = new Boolean(false);
        //theirLogger.debug("Do not RunNow - negative Cron match");
      }
  
      PointData res = new PointData(itsParent.getFullName(), new AbsTime(), val);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }
}
