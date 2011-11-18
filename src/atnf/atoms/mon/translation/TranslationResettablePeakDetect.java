//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.Vector;

import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.util.MonitorUtils;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 * Report the peak detected value from the input but forget old data when a
 * reset control point is high.
 * 
 * <P>
 * The "init" argument specifies the name of the point which controls the reset.
 * 
 * @author David Brodrick
 */
public class TranslationResettablePeakDetect extends Translation implements PointListener {
  /** The peak value seen so far. */
  protected Double itsPeakValue;

  /** Whether or not the reset-control point requires an integral reset. */
  protected boolean itsNeedsReset = false;

  /** Name of the reset-control listened-to point. */
  protected String itsPointName;

  /** Timer used to subscribe to listened-to points. */
  protected static Timer theirTimer = new Timer();

  /** Logger. */
  protected Logger theirLogger = Logger.getLogger(TranslationResettablePeakDetect.class.getName());

  public TranslationResettablePeakDetect(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length < 1) {
      throw new IllegalArgumentException("TranslationResettablePeakDetect (" + itsParent.getFullName()
          + ") - require at least one argument");
    }

    itsPointName = init[0];
    // Substitute the name of our source if $1 macro was used
    if (itsPointName.indexOf("$1") > -1) {
      itsPointName = MonitorUtils.replaceTok(itsPointName, parent.getSource());
    }

    // Start the timer which subscribes us to updates from the point
    theirTimer.schedule(new SubscriptionTask(), 500, 500);
  }

  /** Calculate the average and return an averaged value. */
  public PointData translate(PointData data) {
    // Check if reset control point has gone high
    if (itsNeedsReset) {
      itsPeakValue = null;
    }

    // Incorporate new value
    if (data.getData() != null) {
      Double newdata = null;
      if (data.getData() instanceof Boolean) {
        if (((Boolean) data.getData()).booleanValue()) {
          newdata = new Double(1);
        } else {
          newdata = new Double(0);
        }
      } else if (data.getData() instanceof Number) {
        newdata = new Double(((Number) data.getData()).doubleValue());
      } else {
        theirLogger.warn("(" + itsParent.getFullName() + ": Input data must have Boolean or Numeric values");
      }
      if (newdata != null) {
        if (itsPeakValue == null || newdata.doubleValue() > itsPeakValue.doubleValue()) {
          itsPeakValue = newdata;
        }
      }
    }

    // Return the peak value
    return new PointData(itsParent.getFullName(), new AbsTime(), itsPeakValue);
  }

  /** Called when a listened-to point updates. */
  public void onPointEvent(Object source, PointEvent evt) {
    PointData pd = evt.getPointData();
    // Check that there's data.. ?
    if (pd == null || pd.getData() == null) {
      return;
    }

    Boolean newvalue = null;
    if (pd.getData() instanceof Boolean) {
      newvalue = new Boolean(((Boolean) pd.getData()).booleanValue());
    } else if (pd.getData() instanceof Number) {
      if (((Number) pd.getData()).intValue() == 0) {
        newvalue = new Boolean(false);
      } else {
        newvalue = new Boolean(true);
      }
    } else {
      theirLogger.warn("(" + itsParent.getFullName() + ": Listened-to point " + itsPointName
          + " must have Boolean or Numeric values");
    }

    if (newvalue != null && newvalue.booleanValue()) {
      // The peak has been reset
      itsNeedsReset = true;
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
        pd.addPointListener(TranslationResettablePeakDetect.this);
        cancel();
      }
    }
  }
}
