//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;

/**
 * Calculate the difference between successive monitor point values, calculated as <tt>prev_val - new_val</tt>. This class can
 * handle <i>Number</i> or <i>Angle</i> data types. Angles are treated specially in that we try to handle phase wraps correctly, ie,
 * all jumps are mapped to the range -180<=d<=180.
 * <P>
 * The output is always a Double data type.
 * 
 * @author David Brodrick
 */
public class TranslationDelta extends Translation {
  /** Previous value of the monitor point. */
  Double itsPreviousValue = null;

  protected static String[] itsArgs = new String[] { "Translation Delta", "Delta" };

  public TranslationDelta(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Calculate the delta and return new value. */
  public PointData translate(PointData data) {
    Object val = data.getData();
    if (val == null) {
      // There is no data, can't calculate a delta for this or next point
      itsPreviousValue = null;
      // Return null data to terminate translation for this point
      return null;
    }

    // Get the new value as a double
    double dval;
    if (val instanceof Number) {
      dval = ((Number) val).doubleValue();
    } else if (val instanceof Angle) {
      dval = ((Angle) val).getValue();
    } else {
      System.err.println("TranslationDelta: " + itsParent.getFullName() + ": Got NON-NUMERIC data!");
      return new PointData(itsParent.getFullName());
    }

    PointData res = null;

    // If we had a previous value we can calculate the change
    if (itsPreviousValue != null) {
      // Calculate the change in the value of the point
      double delta = itsPreviousValue.doubleValue() - dval;
      if (val instanceof Angle) {
        // Watch for phase wrap
        if (delta > Math.PI) {
          delta = 2 * Math.PI - delta;
        } else if (delta < -Math.PI) {
          delta = 2 * Math.PI + delta;
        }
        res = new PointData(itsParent.getFullName(), data.getTimestamp(), Angle.factory(delta, Angle.Format.RADIANS), data.getAlarm());
      } else {
        res = new PointData(itsParent.getFullName(), data.getTimestamp(), new Double(delta), data.getAlarm());
      }
    } else {
      // No previous value so can't calculate delta, so return null data
      res = null;
    }

    // Record the current value for use next time
    itsPreviousValue = new Double(dval);

    return res;
  }

  public static String[] getArgs() {
    return itsArgs;
  }
}
