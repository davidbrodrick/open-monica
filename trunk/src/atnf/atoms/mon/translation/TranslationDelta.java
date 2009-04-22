//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.util.*;

/**
 * Calculate the difference between successive monitor point values.
 * This class can handle <i>Number</i> or <i>Angle</i> data types.
 * Angles are treated specially in that we try to handle phase wraps
 * correctly, ie, all jumps are mapped to the range -180<=d<=180.
 * <P>
 * At the moment the output is a Double, but perhaps we should make
 * the output type is the same as the input type, eg, the difference 
 * between two integers will also be an integer..
 * <P>
 * This class discards any reference to the original raw data value
 * because it is not a simple mapping translation.
 *
 * @author David Brodrick
 * @version $Id: TranslationDelta.java,v 1.5 2004/10/22 01:31:35 bro764 Exp $
 */
public class
TranslationDelta
extends Translation
{
  /** Previous value of the monitor point. */
  Double itsPreviousValue = null;

  protected static String[] itsArgs = new String[]{"Translation Delta",
  "Delta"};
   
  public TranslationDelta(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Calculate the delta and return new value. */
  public
  PointData
  translate(PointData data)
  {
    Object val = data.getData();
    if (val==null) {
      //There is no data, can't calculate a delta for this or next point
      itsPreviousValue = null;
      //Return point with a null data field
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    //Get the new value as a double
    double dval;
    if (val instanceof Number) {
      dval = ((Number)val).doubleValue();
    } else if (val instanceof Angle) {
      dval = ((Angle)val).getValue();
    } else {
      System.err.println("TranslationDelta: " + itsParent.getLongName() +
			 ": Got NON-NUMERIC data!");
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    PointData res = null;

    //If we had a previous value we can calculate the change
    if (itsPreviousValue!=null) {
      //Calculate the change in the value of the point
      double delta = itsPreviousValue.doubleValue() - dval;
      if (val instanceof Angle) {
        //Watch for phase wrap
	if (delta>Math.PI) {
          delta = 2*Math.PI - delta;
	} else if (delta<-Math.PI) {
	  delta = 2*Math.PI + delta;
	}
	res = new PointData(itsParent.getName(), itsParent.getSource(),
			    Angle.factory(delta, Angle.Format.RADIANS));
      } else {
	res = new PointData(itsParent.getName(), itsParent.getSource(),
			    new Double(delta));
      }
    } else {
      //No previous value so can't calculate delta, so return null data
      res = new PointData(itsParent.getName(), itsParent.getSource());
    }

    //Record the current value for use next time
    itsPreviousValue = new Double(dval);

    //Clear the "raw data" field since it doesn't apply to us
    res.setRaw(null);

    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
