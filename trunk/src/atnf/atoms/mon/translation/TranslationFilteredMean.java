//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * Keeps a buffer of recent values, discards any values more than X standard deviations from the median, and returns the mean value
 * of the remaining points. This is a good filter to use on slowly changing values which have an occasional bad value.
 * 
 * <P>
 * The first argument specifies the length of the buffer of past values, in seconds. The second value is the threshold used to
 * identify outliers which should be discarded, it is specified in standard deviations.
 * 
 * @author David Brodrick
 */
public class TranslationFilteredMean extends TranslationDataBuffer {
  /** The outlier threshold in standard deviations. */
  protected double itsThreshold = 0.0;

  public TranslationFilteredMean(PointDescription parent, String[] init) {
    super(parent, init);

    // Get the threshold to be used
    try {
      itsThreshold = Double.parseDouble(init[1]);
    } catch (Exception e) {
      throw new IllegalArgumentException("TranslationFilteredMean: " + itsParent.getFullName() + ": Error parsing threshold argument");
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
    if (itsBuffer.size() < 1) {
      return null;
    }

    // Get the filtered mean value
    Double newval = doFiltering();

    // Return the peak value
    return new PointData(itsParent.getFullName(), data.getTimestamp(), newval);
  }

  /** Do the processing and return the mean. */
  protected Double doFiltering() {
    DescriptiveStatistics stats = new DescriptiveStatistics();

    int size = itsBuffer.size();
    for (int i = 0; i < size; i++) {
      stats.addValue(((Number) itsBuffer.get(i).getData()).doubleValue());
    }

    // Compute the statistics
    double median = stats.getPercentile(50);
    double stddev = stats.getStandardDeviation();

    // theirLogger.debug("median=" + median + ", stddev=" + stddev);

    // Calculate the mean of the remaining values
    double sum = 0.0;
    int counter = 0;
    for (int i = 0; i < size; i++) {
      double thisval = ((Number) itsBuffer.get(i).getData()).doubleValue();
      if (thisval <= median + itsThreshold * stddev && thisval >= median - itsThreshold * stddev) {
        sum += thisval;
        counter++;
      }
    }
    if (counter == 0) {
      theirLogger.warn("TranslationFilteredMean: " + itsParent.getFullName() + ": No points to calculate mean");
      return null;
    }
    return sum / counter;
  }
}
