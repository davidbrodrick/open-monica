//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.apache.commons.math3.analysis.interpolation.*;

import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Interpolator which uses the Apache Commons Math HermiteInterpolator class.
 * 
 * <P>
 * Instances expect the following arguments:
 * <ul>
 * <li><b>Period:</b> The buffer length for past values in seconds.
 * <li><b>Prediction Time:</b> The time into the future (or past if negative) to extrapolate/interpolate the value for.
 * <li><b>Preload:</b> Optional argument, if set to "true" the buffer will be pre-seeded from the archive at construction time.
 * </ul>
 * 
 * @author David Brodrick
 */
public class TranslationHermiteInterpolator extends TranslationInterpolator {
  public TranslationHermiteInterpolator(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Return the interpolated value for the specified time offset. */
  protected double interpolate(double reqtime) {
    // Use the HermiteInterpolator
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

    // Get the interpolated value for the requested time
    double res = interpolator.value(reqtime)[0];

    return res;
  }
}
