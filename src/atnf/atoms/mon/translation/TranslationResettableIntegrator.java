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

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Integrates the input and resets the integral sum whenever the listened-to
 * point is "true". The listened-to point is expected to have boolean values (or
 * numeric values, which are interpreted as booleans).
 * 
 * <P>
 * Constructor arguments are: <bl>
 * <li><b>Point:</b> Name of the listened-to point which controls when the
 * integral is reset.
 * <li><b>Reload:</b> Optional argument which determines whether the integral
 * will reload its last value from the archive when the system starts up. This
 * can be used to prevent resetting the integral if the MoniCA server is
 * restarted. Set this argument to "true" to enable this functionality. </bl>
 * 
 * <P>
 * Remember it is the normal input to the Translation which is integrated, while
 * the listened-to point is what control when to reset the integral.
 * 
 * @author David Brodrick
 */
public class TranslationResettableIntegrator extends Translation implements PointListener {
  /** The accumulated input. */
  protected double itsSum = 0.0;

  /** The last state of the reset-control point. */
  protected Boolean itsLastState = null;

  /** Whether or not the reset-control point requires an integral reset. */
  protected boolean itsNeedsReset = false;

  /** Timer used when listened-to point hasn't been created yet. */
  protected Timer itsTimer = null;

  /** Name of the reset-control listened-to point. */
  protected String itsPointName;

  /** Do we need to load our last value from the archive, on system startup. */
  protected boolean itsGetArchive = false;

  /** Timer used to subscribe to listened-to points. */
  protected static Timer theirTimer = new Timer();

  /** Logger. */
  protected Logger theirLogger = Logger.getLogger(TranslationResettableIntegrator.class.getName());

  public TranslationResettableIntegrator(PointDescription parent, String[] init) {
    super(parent, init);
    if (init.length < 1) {
      throw new IllegalArgumentException("TranslationResettableIntegrator (" + itsParent.getFullName() + ") - require at least one argument");
    }

    itsPointName = init[0];
    // Substitute the name of our source if $1 macro was used
    if (itsPointName.indexOf("$1") > -1) {
      itsPointName = MonitorUtils.replaceTok(itsPointName, parent.getSource());
    }

    // Check for the optional 'reload last value at startup' argument
    if (init.length == 2 && init[1].equalsIgnoreCase("true")) {
      itsGetArchive = true;
    }

    // Start the timer which subscribes us to updates from the point
    theirTimer.schedule(new SubscriptionTask(), 500, 500);
  }

  /** Calculate the current value of the integral. */
  public PointData translate(PointData data) {
    if (itsGetArchive) {
      // Need to reload last value from the archive, since system has just
      // restarted
      PointData archiveval = PointBuffer.getPreceding(itsParent.getFullName(), new AbsTime());
      if (archiveval != null && archiveval.getData() instanceof Number) {
        itsSum = ((Number) archiveval.getData()).doubleValue();
      }
      itsGetArchive = false;
    }

    if (itsLastState != null && itsLastState.booleanValue()) {
      // Need to reset the integral due to current state of control point
      itsSum = 0.0;
      return new PointData(itsParent.getFullName(), new AbsTime(), new Double(0.0));
    } else if (itsNeedsReset) {
      // Need to reset but control point has since changed state again
      itsSum = 0.0;
      itsNeedsReset = false;
    }

    // Extract the numeric value from the new input
    if (data.getData() != null) {
      if (data.getData() instanceof Number) {
        itsSum += ((Number) (data.getData())).doubleValue();
      } else {
        theirLogger.warn("(" + itsParent.getFullName() + ": Listened-to point " + itsPointName + " must have Boolean or Numeric values");
      }
    }

    // Return the integrated sum
    return new PointData(itsParent.getFullName(), new AbsTime(), new Double(itsSum));
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
      theirLogger.warn("(" + itsParent.getFullName() + ": Listened-to point " + itsPointName + " must have Boolean or Numeric values");
    }

    if (newvalue != null) {
      itsLastState = newvalue;
      if (itsLastState.booleanValue()) {
        // Flag this in case control point has changed state by next time
        // translate is called
        itsNeedsReset = true;
      }
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
        pd.addPointListener(TranslationResettableIntegrator.this);
        cancel();
      }
    }
  }
}
