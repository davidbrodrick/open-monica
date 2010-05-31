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
 * The first "init" argument is the averaging period in seconds. For instance if
 * you wished to average 1 minutes worth of samples then you would supply the
 * parameter with the value <i>60</i>.
 * 
 * <P>
 * The second argument determines how we timestamp the generated data. It is
 * controlled by a boolean which may have the value "T" or "F". If the value is
 * "T" (for True) then we try to avoid the group delay by timestamping the data
 * with an epoch that is in the middle of the current data window. If the
 * argument is false, "F", we timestamp the data with the current time.
 * 
 * <P>
 * If there is no third argument then we produce one new output value for every
 * new input. If the third argument is used it must specify a period (in
 * seconds) for how often to produce a new output value.
 * 
 * @author David Brodrick
 */
public class TranslationMean extends Translation {
  /** Buffer containing data. */
  protected Vector<PointData> itsBuffer = new Vector<PointData>();

  /** Period to measure the mean over. */
  protected RelTime itsPeriod = null;

  /** The minimum number of samples before output will be produced. */
  protected int itsMinSamples = 1;

  public TranslationMean(PointDescription parent, String[] init) {
    super(parent, init);

    // Find amount of time to buffer data for
    if (init.length < 1) {
      System.err.println("TranslationMean: " + itsParent.getFullName() + ": No Buffer Period Argument!!");
      itsPeriod = RelTime.factory(-60000000l); // Default
    } else {
      try {
        long period = Long.parseLong(init[0]);
        period *= 1000000l; // To microseconds
        if (period > 0) {
          period = -period; // Always want negative
        }
        itsPeriod = RelTime.factory(period);
      } catch (Exception e) {
        System.err.println("TranslationMean: " + itsParent.getFullName() + ": Error Parsing Period Argument!!");
        itsPeriod = RelTime.factory(-60000000l); // Default
      }

      if (init.length == 2) {
        // Minimum number of samples before output can be produced
        try {
          itsMinSamples = Integer.parseInt(init[1]);
        } catch (NumberFormatException e) {
          System.err.println("TranslationMean: " + itsParent.getFullName() + ": \"" + e + "\" for minimum samples "
              + init[1]);
        }
      }
    }
  }

  /** Calculate the average and return an averaged value. */
  public PointData translate(PointData data) {
    // Add new data to buffer and remove any expired data
    updateBuffer(data);

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

  /**
   * Add new data to buffer and purge old data.
   * 
   * @return True if new data was okay, False if a problem.
   */
  protected void updateBuffer(PointData newdata) {
    // Add the new data
    if (newdata != null && newdata.getData() != null) {
      if (!(newdata.getData() instanceof Number) && !(newdata.getData() instanceof Angle)) {
        System.err.println("TranslationMean: " + itsParent.getFullName() + " Can't Use Non-Numeric Data!");
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
