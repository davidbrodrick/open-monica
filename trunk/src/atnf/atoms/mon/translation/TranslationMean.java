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

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.time.AbsTime;
import atnf.atoms.time.RelTime;
import atnf.atoms.util.Angle;

/**
 * Calculate the mean value of a monitor point over time.
 * 
 * <P>
 * The first "init" argument is the averaging period in seconds. For instance if you wished to average 1 minutes worth of samples
 * then you would supply the parameter with the value <i>60</i>.
 * 
 * <P>
 * The second argument determines how we timestamp the generated data. It is controlled by a boolean which may have the value "T" or
 * "F". If the value is "T" (for True) then we try to avoid the group delay by timestamping the data with an epoch that is in the
 * middle of the current data window. If the argument is false, "F", we timestamp the data with the current time.
 * 
 * <P>
 * If there is no third argument then we produce one new output value for every new input. If the third argument is used it must
 * specify a period (in seconds) for how often to produce a new output value.
 * 
 * @author David Brodrick
 */
public class TranslationMean extends TranslationDataBuffer {
  /** The minimum number of samples before output will be produced. */
  protected int itsMinSamples = 1;

  public TranslationMean(PointDescription parent, String[] init) {
    super(parent, init);
    if (init.length == 2) {
      // Minimum number of samples before output can be produced
      try {
        itsMinSamples = Integer.parseInt(init[1]);
      } catch (NumberFormatException e) {
        System.err.println("TranslationMean: " + itsParent.getFullName() + ": \"" + e + "\" for minimum samples " + init[1]);
      }
    }
  }

  /** Calculate the average and return an averaged value. */
  public PointData translate(PointData data) {
    // Check data type and update buffer
    if (data.getData() == null || !(data.getData() instanceof Number)) {
      theirLogger.warn(getClass().getCanonicalName() + ": " + itsParent.getFullName() + ": Can't use non-numeric data");
      updateBuffer(null);
    } else {
      updateBuffer(data);
    }

    // If insufficient data then can't calculate result
    if (itsBuffer.size() < itsMinSamples) {
      return null;
    }

    // Get a timestamp for this data
    AbsTime tstamp = data.getTimestamp();

    // Get the mean
    double m = getMean();

    // Return result
    return new PointData(itsParent.getFullName(), tstamp, new Double(m));
  }

  /** Return the mean of the data in the buffer. */
  protected double getMean() {
    int size = itsBuffer.size();
    double[] data = new double[size];

    // Translate the data to an array of doubles
    for (int i = 0; i < size; i++) {
      Object thisdata = ((PointData) itsBuffer.get(i)).getData();
      if (thisdata instanceof Number) {
        data[i] = ((Number) thisdata).doubleValue();
      } else if (thisdata instanceof Angle) {
        data[i] = ((Angle) thisdata).getValue();
      }
    }

    // Calculate the mean of the data
    double sum = 0.0;
    for (int i = 0; i < size; i++) {
      sum += data[i];
    }
    return sum / size;
  }
}
