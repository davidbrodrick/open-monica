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
import atnf.atoms.time.*;

/**
 * Calculate the mean value of a monitor point over time.
 *
 * <P>The first "init" argument is the averaging period in seconds.
 * For instance if you wished to average 1 minutes worth of samples
 * then you would supply the parameter with the value <i>60</i>.
 *
 * <P>The second argument determines how we timestamp the generated data.
 * It is controlled by a boolean which may have the value "T" or "F". If the
 * value is "T" (for True) then we try to avoid the group delay by
 * timestamping the data with an epoch that is in the middle of the current
 * data window. If the argument is false, "F", we timestamp the data with
 * the current time.
 *
 * <P>If there is no third argument then we produce one new output value
 * for every new input. If the third argument is used it must specify a
 * period (in seconds) for how often to produce a new output value.
 *
 * @author David Brodrick
 * @version $Id: TranslationMean.java,v 1.3 2004/10/06 04:17:46 bro764 Exp $
 */
public class
TranslationMean
extends Translation
{
  /** Buffer containing data. */
  protected Vector itsBuffer = new Vector();

  /** Period to measure the mean over. */
  protected RelTime itsPeriod = null;

  /** Retrofit timestamps to prevent group delay */
  protected boolean itsAvoidDelay = false;

  /** Produce one output number at this period. */
  protected RelTime itsOutputPeriod = null;

  /** Time we last produced an output. */
  protected AbsTime itsLastOutput = null;

  protected static String[] itsArgs = new String[]{
      "Translation Mean", "Mean",
      "Time Range", "java.lang.String",
      "Avoid Group Delay", "java.lang.Boolean",
      "Output Interval", "java.lang.Long:"};

  public TranslationMean(PointMonitor parent, String[] init)
  {
    super(parent, init);

    //Find amount of time to buffer data for
    if (init.length<1) {
      System.err.println("TranslationMean: " + itsParent.getLongName() +
			 ": No Buffer Period Argument!!");
      itsPeriod = RelTime.factory(-60000000l); //Default
    } else {
      try {
	long period = Long.parseLong(init[0]); ///Could parse as double instead
	period *= 1000000l; //To microseconds
	if (period>0) {
    period = -period; //Always want negative
  }
	itsPeriod = RelTime.factory(period);
      } catch (Exception e) {
	System.err.println("TranslationMean: " + itsParent.getLongName() +
			   ": Error Parsing Period Argument!!");
	itsPeriod = RelTime.factory(-60000000l); //Default
      }

      if (init.length<2) {
	//Assume that we don't compensate for the group delay
        itsAvoidDelay = false;
      } else {
	if (init[1].equals("T") || init[1].equals("true") ||
	    init[1].equals("yes") || init[1].equals("1")) {
	  itsAvoidDelay = true;
	} else {
	  itsAvoidDelay = false;
	}

	if (init.length<3) {
	  //Assume that every data point is wanted, no undersampling
	  itsOutputPeriod = null;
	} else {
	  long operiod = Long.parseLong(init[2]);
	  operiod *= 1000000l; //To microseconds
	  if (operiod<0) {
      operiod = -operiod; //Always want positive
    }
	  itsOutputPeriod = RelTime.factory(operiod);
	}
      }
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
      //Clear timestamp so buffer will be refilled before making output
      itsLastOutput = null;
      return null;
    }

    //Get a timestamp for this data
    AbsTime tstamp = null;
    if (itsAvoidDelay) {
      //Use a timestamp in the middle of the data time range
      long start = ((PointData)itsBuffer.get(0)).getTimestamp().getValue();
      long end = ((PointData)itsBuffer.get(itsBuffer.size()-1)).getTimestamp().getValue();
      tstamp = AbsTime.factory(start + (end-start)/2);
    } else {
      //Use the current time as the timestamp
      tstamp = new AbsTime();
    }

    //Determine if it's time to produce another measurement
    if (itsOutputPeriod!=null) {
      if (itsLastOutput!=null) {
	AbsTime nextoutput = itsLastOutput.add(itsOutputPeriod);
	if (tstamp.isBefore(nextoutput)) {
	  //It's not yet time to produce a new measurement
	  return null;
	}
      } else if (itsAvoidDelay) {
	//Record timestamp but don't write data - allows buffer to fill up.
	itsLastOutput = tstamp;
        return null;
      }
      //Record the time stamp of this measurement
      itsLastOutput = tstamp;
    }

    //Get the mean
    double m = getMean();

    //Produce the result. Set raw field to null.
    if (((PointData)itsBuffer.get(0)).getData() instanceof Angle) {
      return new PointData(itsParent.getName(), itsParent.getSource(),
			   tstamp, null,
			   Angle.factory(m, Angle.Format.RADIANS));
    } else {
      return new PointData(itsParent.getName(), itsParent.getSource(),
			   tstamp, null, new Double(m));
    }
  }


  /** Add new data to buffer and purge old data. 
   * @return True if new data was okay, False if a problem. */
  protected
  void
  updateBuffer(PointData newdata)
  {
    //Add the new data
    if (newdata!=null && newdata.getData()!=null) {
      if (!(newdata.getData() instanceof Number) &&
	  !(newdata.getData() instanceof Angle)) {
	System.err.println("TranslationMean: " + itsParent.getLongName() +
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


  /** Return the mean of the data in the buffer. */
  protected
  double
  getMean()
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
    return sum/size;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
