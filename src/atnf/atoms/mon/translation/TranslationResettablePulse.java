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

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.mon.translation.Translation;

/**
 * Outputs True for a fixed mark period when triggered, however the output can
 * be reset by listening to a specified point. Input continues to be monitored
 * as normal but output will not transition high again once reset until trigger
 * goes low and pulse period expires. The input and reset are triggered by
 * looking at the input as a boolean (if the input is a Number it will be cast
 * to an integer and interpreted as value of zero means false, with any other
 * value meaning true).
 * 
 * It requires two arguments:
 * <ol>
 * <li><b>Mark Period:</b> The minimum period (in seconds) to maintain the mark
 * output.
 * <li><b>Reset Point:</b> The point to listen to for reset signals.
 * </ol>
 * 
 * @author David Brodrick
 */
public class TranslationResettablePulse extends Translation implements PointListener {
  /** The period to pulse for. */
  protected RelTime itsPulsePeriod;

  /** The time pulse was last retriggered. */
  protected AbsTime itsLastTrigger;

  /** Whether the current pulse has been reset. */
  protected boolean itsPulseCancelled = false;

  /** Name of the reset-control listened-to point. */
  protected String itsPointName;

  /** Timer used to subscribe to listened-to points. */
  protected static Timer theirTimer = new Timer();

  /** Logger. */
  protected Logger theirLogger = Logger.getLogger(TranslationResettablePulse.class.getName());

  /** Constructor. */
  public TranslationResettablePulse(PointDescription parent, String[] init) {
    super(parent, init);
    try {
      itsPulsePeriod = RelTime.factory((long) (Double.parseDouble(init[0]) * 1000000));

      itsPointName = init[1];
      // Substitute the name of our source if $1 macro was used
      if (itsPointName.indexOf("$1") > -1) {
        itsPointName = MonitorUtils.replaceTok(itsPointName, parent.getSource());
      }

      // Start the timer which subscribes us to updates from the point
      theirTimer.schedule(new SubscriptionTask(), 500, 500);
    } catch (Exception e) {
      theirLogger.error("(" + itsParent.getFullName() + "): While parsing constructor string arguments: " + e);
    }
  }

  /** Determine output based on input and timing constraints. */
  public PointData translate(PointData data) {
    if (data == null || data.getData() == null) {
      return null;
    }
    
    AbsTime now = new AbsTime();

    // Check new input for retrigger
    boolean triggered = false;
    Object rawinput = data.getData();
    if (rawinput instanceof Boolean) {
      triggered = ((Boolean)rawinput).booleanValue();
    } else if (rawinput instanceof Number) {
      int intval = ((Number) rawinput).intValue();
      if (intval == 0) {
        triggered = false;
      } else {
        triggered = true;
      }
    } else {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): Expect Boolean or Number input");
      return null;
    }    
    
    if (triggered) {
      // Record timestamp of retrigger
      itsLastTrigger = now;
    } else if (itsLastTrigger!=null && itsLastTrigger.add(itsPulsePeriod).isAfterOrEquals(now)) {
      // Still triggered from previous input
      triggered = true;
    }
    
    Boolean output;
    if (triggered) {
      if (!itsPulseCancelled) {
        // Uncancelled pulse
        output = new Boolean(true);
      } else {
        // Triggered, but pulse has been cancelled
        output = new Boolean(false);
      }
    } else {
      // Not triggered and old pulse has expired
      output = new Boolean(false);
      itsPulseCancelled = false;
    }

    return new PointData(itsParent.getFullName(), now, output);
  }

  /** Called when a listened-to point updates. */
  public void onPointEvent(Object source, PointEvent evt) {
    PointData pd = evt.getPointData();
    // Check that there's data.. ?
    if (pd == null || pd.getData() == null) {
      return;
    }

    boolean newvalue = false;    
    try {
      newvalue = MonitorUtils.parseAsBoolean(pd.getData());
    } catch (IllegalArgumentException e) {
      theirLogger.error("(" + itsParent.getFullName() + "): " + e);
      return;
    }

    if (newvalue) {
      // The peak has been reset
      itsPulseCancelled = true;
    }
  }

  /** TimerTask used to subscribe to monitor point updates via timer. */
  private class SubscriptionTask extends TimerTask {
    public void run() {
      PointDescription pd = PointDescription.getPoint(itsPointName);
      if (pd == null && PointDescription.getPointsCreated()) {
        // Still couldn't find the point, perhaps it doesn't exist?!
        theirLogger.warn("(" + itsParent.getFullName() + ") listened-to point " + itsPointName + " was not found");
      } else if (pd != null) {
        pd.addPointListener(TranslationResettablePulse.this);
        cancel();
      }
    }
  }
}
