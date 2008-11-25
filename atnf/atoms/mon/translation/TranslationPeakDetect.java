//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.*;

/**
 * Find the peak of a numeric input over a specified period of time.
 *
 * <P>The "init" argument specifies the time period in seconds.
 *
 * @author David Brodrick
 */
public class
TranslationPeakDetect
extends Translation
{
  /** Buffer containing data. */
  protected Vector itsBuffer = new Vector();

  /** Period to measure the peak over. */
  protected RelTime itsPeriod = RelTime.factory(60000000l);

  protected static String[] itsArgs = new String[]{
      "Translation Peak Detect", "PeakDetect",
      "Time Range", "java.lang.String"};

  public TranslationPeakDetect(PointMonitor parent, String[] init)
  {
    super(parent, init);

    //Find amount of time to buffer data for
    try {
      float period = Float.parseFloat(init[0])*1000000;
      if (period>0) period=-period;
      itsPeriod = RelTime.factory((long)period);
    } catch (Exception e) {
      System.err.println("TranslationPeakDetect: " + itsParent.getLongName() +
                         ": Error Parsing Period Argument!!");
    }
  }


  /** Calculate the average and return an averaged value. */
  public
  PointData
  translate(PointData data)
  {
    //Add new data to buffer and remove any expired data
    updateBuffer(data);

    //If insufficient data then can't calculate result
    if (itsBuffer.size()<1) {
      return null;
    }

    //Find the peak value
    Double peak=getPeak();

    //Return the peak value
    return new PointData(itsParent.getName()+"."+itsParent.getSource(),
                         new AbsTime(), peak);
  }


  /** Add new data to buffer and purge old data. */
  protected
  void
  updateBuffer(PointData newdata)
  {
    //Add the new data
    if (newdata!=null && newdata.getData()!=null) {
      if (!(newdata.getData() instanceof Number)) {
        System.err.println("TranslationPeakDetect: " + itsParent.getLongName()
                           + " Can't Use Non-Numeric Data!");
      } else {
        itsBuffer.add(newdata);
      }
    }

    //Purge any old data which has now expired
    AbsTime expiry = (new AbsTime()).add(itsPeriod);
    while (itsBuffer.size()>0 &&
           ((PointData)itsBuffer.get(0)).getTimestamp().isBefore(expiry)) {
      itsBuffer.remove(0);
    }
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

    if (first) return null;
    else return new Double(peak);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
