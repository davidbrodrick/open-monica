//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * This class translates a String data value to a sub-class of Number. Float, Double, Integer and Long are supported. An optional
 * radix argument may be used for Integers. If radix is omitted, base 10 is assumed.
 * 
 * @author Simon Hoyle
 * @author David Brodrick
 * @version $Id: $
 */

public class TranslationStringToNumber extends Translation {
  /** Arguments. Only the first argument is essential. */
  protected static String itsArgs[] = new String[] { "Translation StringToNumber", "StringToNumber", "NumericTypeName", "java.lang.String", "Radix",
      "java.lang.String" };

  /** Logger */
  protected static Logger theirLogger = Logger.getLogger(TranslationStringToNumber.class.getName());

  /**  */
  protected String itsNumericTypeName = null;

  /**  */
  protected int itsRadix = 0;

  public TranslationStringToNumber(PointDescription parent, String[] init) {
    super(parent, init);
    if (init.length > 0) {
      itsNumericTypeName = init[0];
      if (init.length > 1)
        itsRadix = Integer.parseInt(init[1]);
    } else
      System.err.println("ERROR: TranslationStringToNumber (for " + itsParent.getName() + "): Expect 1 or 2 Arguments.. got " + init.length);
  }

  /** Perform the actual data translation. */
  public PointData translate(PointData data) {
    // Ensure there is raw data for us to translate!
    if (data == null || data.getData() == null || itsNumericTypeName == null)
      return null;

    Number num = null;
    PointData res = null;

    Object d = data.getData();

    if (d instanceof String) {
      d = ((String) d).trim();
    } else {
      d = d.toString().trim();
    }

    try {
      if (itsNumericTypeName.equalsIgnoreCase("Float")) {
        num = Float.valueOf((String) d);
      } else if (itsNumericTypeName.equalsIgnoreCase("Double")) {
        num = Double.valueOf((String) d);
      } else if (itsNumericTypeName.equalsIgnoreCase("Integer")) {
        if (itsRadix != 0)
          num = Integer.valueOf((String) d, itsRadix);
        else
          num = Integer.valueOf((String) d);
      } else if (itsNumericTypeName.equalsIgnoreCase("Long")) {
        if (itsRadix != 0)
          num = Long.valueOf((String) d, itsRadix);
        else
          num = Long.valueOf((String) d);
      }
    } catch (NumberFormatException e) {
      theirLogger.error(itsParent.getFullName() + ": " + e);
    }

    res = new PointData(data.getName(), data.getTimestamp(), num);

    return res;
  }

  public static String[] getArgs() {
    return itsArgs;
  }
}
