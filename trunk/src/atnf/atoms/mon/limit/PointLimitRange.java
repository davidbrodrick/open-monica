//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.limit;

import atnf.atoms.mon.*;
import atnf.atoms.util.Angle;

/**
 * <i>PointLimit</i> sub-class which checks if a numeric data value is
 * within a nominated range. The first two <i>init</i> arguments should
 * be the lower and upper limits of the range. An optional third argument
 * defines whether inside the range is considered nominal
 * (<tt>True</tt> - the default) or outside the range is nominal
 * (<tt>False</tt>).
 *
 * @author David Brodrick
 * @author Le Cuong Nguyen
 * @version $Id: PointLimitRange.java,v 1.1 2004/06/08 01:21:28 bro764 Exp $
 */
class PointLimitRange
extends PointLimit
{
  /** The lower limit of the range. */
  private double itsLower = 0.0;

  /** The upper limit of the range. */
  private double itsUpper = 0.0;

  /** Is it normal to be inside the range (<tt>true</tt>) or is a normal
   * value outide the range (<tt>false</tt>). */
  private boolean itsInsideNormal = true;

  /** Argument description strings. */
  protected static String itsArgs[] = new String[]{"Limits - None",""};


  public PointLimitRange(String[] args)
  {
    if (args.length<2) {
      //This is a problem
      System.err.println("PointLimitRange: Need at least two arguments!!!");
      itsUpper = itsLower = 0.0;
    } else {
      //Read the upper and lower limits of the numeric range
      itsLower = Double.parseDouble(args[0]);
      itsUpper = Double.parseDouble(args[1]);
      if (itsLower>itsUpper) {
	//Need to swap!
	double temp = itsUpper;
	itsUpper = itsLower;
	itsLower = temp;
      }
      //Check if the optional third argument was specified
      if (args.length==3) {
	if (args[2].equalsIgnoreCase("true") ||
	    args[2].equalsIgnoreCase("t") ||
	    args[2].equalsIgnoreCase("1")) {
	  itsInsideNormal = true;
	} else {
          itsInsideNormal = false;
	}
      }
    }
  }


  /**
   * Checks if the value is "normal" with respect to the specified limits.
   * If the value is okay then <tt>true</tt> will be returned, otherwise
   * <tt>false</tt> is returned.
   *
   * @param data The value to check against our limits.
   * @return <tt>True</tt> if the value is okay, <tt>false</tt> otherwise. **/
  public boolean checkLimits(PointData data)
  {
    //All zeroes means that we shouldn't even check
    if (itsUpper==itsLower && itsLower==0.0) {
      return true;
    }
    //No news is good news, right?
    if (data==null || data.getData()==null) {
      return true;
    }

    Object myData = data.getData();
    double doubleData = 0.0;
    //Get the data as a double
    if (myData instanceof Number) {
      doubleData = ((Number)myData).doubleValue();
    } else if (myData instanceof Angle) {
      doubleData = ((Angle)myData).getValue();
    } else {
      System.err.println("PointLimitRange: ERROR: " + data.getName() + " has NON-NUMERIC data!");
      return false;
    }

    //Check if the number is within the specified range
    boolean withinRange = false;
    if (doubleData>=itsLower && doubleData<=itsUpper) {
      withinRange = true;
    }

    //If it's inside the range and it's supposed to be, then all is well
    if (itsInsideNormal && withinRange) {
      return true;
    } else if (!itsInsideNormal && !withinRange) {
      return true;
    } else {
      return false;
    }
  }
   
}