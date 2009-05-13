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
 * Calculate the variance of a monitor point over a specified time range.
 *
 * <P>Expects one "init" argument, which is the period of time to buffer
 * data for in order to measure the variance.
 *
 * @author David Brodrick
 * @version $Id: TranslationVariance.java,v 1.4 2004/10/06 23:20:15 bro764 Exp $
 */
public class
TranslationVariance
extends Translation
{
  /** Buffer containing data. */
  protected Vector<PointData> itsBuffer = new Vector<PointData>();

  /** Period to measure the variance over. */
  protected RelTime itsPeriod = null;

  protected static String[] itsArgs = new String[]{"Translation Variance",
  "Variance", "Time Range", "java.lang.String"};

  public TranslationVariance(PointDescription parent, String[] init)
  {
    super(parent, init);

    //Find amount of time to buffer data for
    if (init.length<1) {
      System.err.println("TranslationVariance: " + itsParent.getLongName() +
			 ": No Buffer Period Argument!!");
      itsPeriod = RelTime.factory(-60000000l); //Default
    } else {
      try {
	long period = Long.parseLong(init[0]);
	period *= 1000000l; //To microseconds
	if (period>0) {
    period = -period; //Always want negative
  }
	itsPeriod = RelTime.factory(period);
      } catch (Exception e) {
	System.err.println("TranslationVariance: " + itsParent.getLongName() +
			   ": Error Parsing Period Argument!!");
	itsPeriod = RelTime.factory(-60000000l); //Default
      }
    }
  }


  /** Calculate the delta and return new value. */
  public
  PointData
  translate(PointData data)
  {
    //Add new data to buffer and remove any expired data
    updateBuffer(data);

    //If insufficient data then can't calculate result
    if (itsBuffer.size()<2) {
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    //Get the variance
    double v = getVariance();
    //Create result - set "raw" data field to null
    if (((PointData)itsBuffer.get(0)).getData() instanceof Angle) {
      return new PointData(itsParent.getName(), itsParent.getSource(),
                           data.getTimestamp(), Angle.factory(v, Angle.Format.RADIANS));
    } else {
      return new PointData(itsParent.getName(), itsParent.getSource(),
                           data.getTimestamp(), new Double(v));
    }
  }


  /** Add new data to buffer and purge old data. */
  protected
  void
  updateBuffer(PointData newdata)
  {
    //Add the new data
    if (newdata!=null && newdata.getData()!=null) {
      if (!(newdata.getData() instanceof Number) &&
	  !(newdata.getData() instanceof Angle)) {
	System.err.println("TranslationVariance: " + itsParent.getLongName() +
			   " Can't Use Non-Numeric Data!");
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


  /** Return the variance of the data in the buffer. */
  protected
  double
  getVariance()
  {
    int size = itsBuffer.size();
    double[] data = new double[size];

    //Translate the data to an array of doubles
    for (int i=0; i<size; i++) {
      Object thisdata = ((PointData)itsBuffer.get(i)).getData();
      if (thisdata instanceof Number) {
        data[i] = ((Number)thisdata).doubleValue();
      } else if (thisdata instanceof Angle) {
        data[i] = ((Angle)thisdata).getValue();
      }
    }

    //Calculate the mean of the data
    double sum = 0.0;
    for (int i=0; i<size; i++) {
      sum += data[i];
    }
    double mean = sum/size;

    //Get the variance
    double var = 0.0;
    for (int i=0; i<size; i++) {
      double diff = data[i] - mean;
      var += diff*diff;
    }
    var = var/size;
    return Math.sqrt(var);
  }


  public static
  String[]
  getArgs()
  {
     return itsArgs;
  }
}
