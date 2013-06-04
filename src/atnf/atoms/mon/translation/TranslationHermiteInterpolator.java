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

import org.apache.commons.math3.analysis.interpolation.*;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.time.AbsTime;
import atnf.atoms.time.RelTime;
import atnf.atoms.time.Time;

/**
 * Fits a polynomial to a buffer of past values and can interpolate/extrapolate.
 * 
 * <P>
 * The first argument specifies the length of the buffer of past values, in seconds. The second value is how far into the future to
 * extrapolate the value for, in seconds.
 * 
 * @author David Brodrick
 */
public class TranslationHermiteInterpolator extends Translation {
  /** Buffer containing data. */
  protected Vector<PointData> itsBuffer = new Vector<PointData>();

  /** Period to measure the peak over. */
  protected RelTime itsPeriod = RelTime.factory(-60000000l);

  /** How many seconds into the future to make the prediction for. */
  protected RelTime itsPredictionTime = RelTime.factory(-60000000l);

  public TranslationHermiteInterpolator(PointDescription parent, String[] init) {
    super(parent, init);

    // Find amount of time to buffer data for
    try {
      float period = Float.parseFloat(init[0]) * 1000000;
      if (period > 0) {
        period = -period;
      }
      itsPeriod = RelTime.factory((long) period);

      // Find when to make the prediction for
      float pred = Float.parseFloat(init[1]) * 1000000;
      itsPredictionTime = RelTime.factory((long) pred);
    } catch (Exception e) {
      System.err.println("TranslationExtrapolator: " + itsParent.getFullName() + ": Error Arguments!!");
    }
  }

  /** Calculate the average and return an averaged value. */
  public PointData translate(PointData data) {
    // Add new data to buffer and remove any expired data
    updateBuffer(data);

    // If insufficient data then can't calculate result
    if (itsBuffer.size() < 1) {
      return null;
    }

    // Build the interpolator based on data in the buffer
    HermiteInterpolator interpolator = buildInterpolator();

    // Get the value for the requested time offset
    double reqtime = Time.diff(data.getTimestamp().add(itsPredictionTime), itsBuffer.firstElement().getTimestamp()).getAsSeconds();
    double predval = interpolator.value(reqtime)[0];

    // Return the peak value
    return new PointData(itsParent.getFullName(), data.getTimestamp(), predval);
  }

  /** Add new data to buffer and purge old data. */
  protected void updateBuffer(PointData newdata) {
    // Add the new data
    if (newdata != null && newdata.getData() != null) {
      if (!(newdata.getData() instanceof Number)) {
        System.err.println("TranslationExtrapolator: " + itsParent.getFullName() + " Can't Use Non-Numeric Data!");
      } else {
        itsBuffer.add(newdata);
      }
    }

    // Purge any old data which has now expired
    AbsTime expiry = (new AbsTime()).add(itsPeriod);
    while (itsBuffer.size() > 0 && ((PointData) itsBuffer.get(0)).getTimestamp().isBefore(expiry)) {
      itsBuffer.remove(0);
    }
  }

  /** Build the interpolator using the data in the buffer. */
  protected HermiteInterpolator buildInterpolator() {
    HermiteInterpolator interpolator = new HermiteInterpolator();

    // Get the timestamp of the first point
    AbsTime first = itsBuffer.firstElement().getTimestamp();

    int size = itsBuffer.size();
    for (int i = 0; i < size; i++) {
      // Get the time offset from the first point
      double timeoffset = Time.diff(itsBuffer.get(i).getTimestamp(), first).getAsSeconds();
      // Get the value for this point
      double thisval = ((Number) ((PointData) itsBuffer.get(i)).getData()).doubleValue();
      // Add it to the interpolator
      interpolator.addSamplePoint(timeoffset, new double[] { thisval });
    }

    System.err.println("Fit is: " + interpolator.getPolynomials()[0]);

    return interpolator;
  }
}
