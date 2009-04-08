//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

import java.lang.Float;
import java.lang.Double;
import java.lang.Integer;


/**
 * This class translates a String data value to a sub-class of Number.
 * Currently, only Float, Double and Integer are supported.  
 * An optional radix argument may be used for Integers. If radix 
 * is omitted, base 10 is assumed.
 *
 * @author Simon Hoyle
 * @author David Brodrick
 * @version $Id: $
 */

public class TranslationStringToNumber
extends Translation
{
  /** Arguments. Only the first argument is essential. */
  protected static String itsArgs[] = new String[]{
    "Translation StringToNumber","StringToNumber",
    "NumericTypeName", "java.lang.String",
    "Radix", "java.lang.String"};

  /**  */
  protected String itsNumericTypeName = null;
  /**  */
  protected int itsRadix = 0;


  public
  TranslationStringToNumber(PointMonitor parent, String[] init)
  {
    super(parent, init);
    if (init.length > 0) {
      itsNumericTypeName  = init[0];
      if (init.length > 1) {
        itsRadix = Integer.parseInt(init[1]);
      }
    } else {
      System.err.println("ERROR: TranslationStringToNumber (for " + itsParent.getName()
        + "): Expect 1 or 2 Arguments.. got " + init.length);
    }
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    //Ensure there is raw data for us to translate!
    if (data==null || data.getData()==null || itsNumericTypeName==null) {
      return null;
    }

    Number num = null;
    PointData res = null;

    Object d = data.getData();
    
    if (d instanceof String) {
      try {
      if (itsNumericTypeName.equalsIgnoreCase("Float")) {
        num = Float.valueOf((String)d);
      } else if (itsNumericTypeName.equalsIgnoreCase("Double")) {
        num = Double.valueOf((String)d);
      } else if (itsNumericTypeName.equalsIgnoreCase("Integer")) {
        if (itsRadix != 0) {
          num = Integer.valueOf((String)d, itsRadix);
        } else {
          num = Integer.valueOf((String)d);
        }
      }
      } catch (NumberFormatException e) {  
               //System.err.println("ERROR: TranslationStringToNumber: (for " 
               //           + data.getName() + "): " + e.toString());
      }
      
      res = new PointData(data.getName(), data.getSource(),
			data.getTimestamp(), data.getRaw(),
			num);
    } else {
      System.err.println("ERROR: TranslationStringToNumber: (for " 
                          + data.getName() + "): EXPECT String got " + data.getClass());
    }

    return res;
  }


  public static
  String[]
  getArgs()
  {
    return itsArgs;
  }
}
