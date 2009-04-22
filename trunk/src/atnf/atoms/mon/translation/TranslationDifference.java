//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointDescription;

/** Implementation of <i>TranslationDualListen</i> which calculates the
 * numerical difference between two different monitor points.
 *
 * @author David Brodrick
 * @version $Id: TranslationDifference.java,v 1.1 2005/01/27 22:49:48 bro764 Exp $
 */
public class
TranslationDifference
extends TranslationDualListen
{
  protected static String[] itsArgs = new String[]{"Difference",
  "Get difference between two other points",
  "MonitorPoint 1", "java.lang.String",
  "MonitorPoint 2", "java.lang.String"};

  public
  TranslationDifference(PointDescription parent, String[] init)
  {
    super(parent, init);
  }

  /** Get the actual difference.
   *@param val1 Most recent (non-null) data from monitor point 1
   *@param val2 Most recent (non-null) data from monitor point 2
   *@return Difference between the two values */
  protected
  Object
  doCalculations(Object val1, Object val2)
  {
    if (val1 instanceof Integer && val2 instanceof Integer) {
      return new Integer(((Integer)val1).intValue() -
			 ((Integer)val2).intValue());
    } else if (val1 instanceof Long && val2 instanceof Long) {
      return new Long(((Long)val1).longValue() -
		      ((Long)val2).longValue());
    } else if (val1 instanceof Number && val2 instanceof Number) {
      //return a double
      return new Double(((Number)val1).doubleValue() -
			((Number)val2).doubleValue());
    } else {
      System.err.println("TranslationDifference: " + itsParent.getName() +
			 ": Unsupported types");
      return null;
    }
}


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
