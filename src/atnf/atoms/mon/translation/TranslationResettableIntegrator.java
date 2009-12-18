//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Integrates the input and resets the integral sum whenever the listened-to
 * point is "True". The listened-to point is expected to have boolean values (or
 * numeric values which are interpreted as booleans).
 * 
 * <P>
 * Constructor arguments is just the name of the listened-to point.
 * 
 * <P>
 * Remember it is the normal input from the Transaction which is integrated, the
 * listened-to points just control when to reset the integral.
 * 
 * @author David Brodrick
 */
public class TranslationResettableIntegrator extends Translation implements
    PointListener, ActionListener {
  /** The accumulated input. */
  protected double itsSum = 0.0;

  /** The last state of the reset-control point. */
  protected Boolean itsLastState = null;

  /** Timer used when listened-to point hasn't been created yet. */
  protected Timer itsTimer = null;

  /** Name of the reset-control listened-to point. */
  protected String itsPointName;

  public TranslationResettableIntegrator(PointDescription parent, String[] init) {
    super(parent, init);
    if (init == null || init.length < 1) {
      System.err.println("TranslationResettableIntegrator: NO ARGUMENTS: for "
          + parent.getFullName());
      return;
    }

    itsPointName = init[0];
    // Substitute the name of our source if $1 macro was used
    if (itsPointName.indexOf("$1") > -1) {
      itsPointName = MonitorUtils.replaceTok(itsPointName, parent.getSource());
    }

    // Start the timer which subscribes us to updates from the points
    itsTimer = new Timer(100, this);
    itsTimer.start();
  }

  /** Calculate the average and return an averaged value. */
  public PointData translate(PointData data) {
    if (itsLastState != null && itsLastState.booleanValue()) {
      // Need to reset the integral
      itsSum = 0.0;
      return new PointData(itsParent.getFullName(), new AbsTime(), new Double(
          0.0));
    }

    // Extract the numeric value from the new input
    if (data.getData() != null) {
      if (data.getData() instanceof Number) {
        itsSum += ((Number) (data.getData())).doubleValue();
      } else {
        System.err.println("TranslationResettableIntegrator: "
            + itsParent.getFullName() + ": REQUIRES NUMERIC INPUT!");
      }
    }

    // Return the integrated sum
    return new PointData(itsParent.getFullName(), new AbsTime(), new Double(
        itsSum));
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
      System.err.println("TranslationResettableIntegrator ("
          + itsParent.getFullName() + ": Listened-to point " + itsPointName
          + " MUST HAVE BOOLEAN OR NUMERIC VALUES");
    }

    if (newvalue != null) {
      itsLastState = newvalue;
    }
  }

  /** Only used to subscribe to monitor point updates via timer. */
  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == itsTimer) {
      PointDescription pd = PointDescription.getPoint(itsPointName);
      if (pd == null) {
        // Still couldn't find the point, perhaps it doesn't exist?!
        System.err.println("WARNING: TranslationResettableIntegrator ("
            + itsParent.getFullName() + "): LISTENED-TO POINT " + itsPointName
            + " DOESN'T EXIST?!");
      } else {
        pd.addPointListener(this);
        itsTimer.stop();
        itsTimer = null;
      }
    }
  }
}
