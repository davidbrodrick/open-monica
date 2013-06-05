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

/**
 * Find the peak of a numeric input over a specified period of time.
 *
 * <P>The "init" argument specifies the time period in seconds.
 *
 * @author David Brodrick
 */
public class
TranslationPeakDetect
extends TranslationDataBuffer
{
  public TranslationPeakDetect(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Calculate the average and return an averaged value. */
  public
  PointData
  translate(PointData data)
  {  
    // Check data type and update buffer
    if (data.getData() == null || !(data.getData() instanceof Number)) {
      theirLogger.warn(getClass().getCanonicalName() + ": " + itsParent.getFullName() + ": Can't use non-numeric data");
      updateBuffer(null);
    } else {
      updateBuffer(data);
    }

    //If insufficient data then can't calculate result
    if (itsBuffer.size()<1) {
      return null;
    }

    //Find the peak value
    Double peak=getPeak();

    //Return the peak value
    return new PointData(itsParent.getFullName(), data.getTimestamp(), peak);
  }

  /** Return the mean of the peak in the buffer. */
  protected
  Double
  getPeak()
  {
    double peak=0.0;
    boolean first=true;
    
    int size=itsBuffer.size();
    for (int i=0; i<size; i++) {
      double thisval=((Number)((PointData)itsBuffer.get(i)).getData()).doubleValue();
      if (first || thisval>peak) {
        peak=thisval;
        first=false;
      }
    }

    if (first) {
      return null;
    } else {
      return new Double(peak);
    }
  }
}
