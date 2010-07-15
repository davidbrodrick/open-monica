//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;


/**
 * This class translates an instance of Angle to a Double. An optional format argument
 * may be used to specify whether the output should be in degrees (<i>"d"</i>) or radians 
 * (<i>"r"</i>). If no format is specified then the output will be in radians.
 *
 * @author David Brodrick */
public class TranslationAngleToNumber
extends Translation
{
  /** Arguments. Only the first argument is essential. */
  protected static String itsArgs[] = new String[]{
    "Translation AngleToNumber","AngleToNumber",
    "Format", "java.lang.String"};

  /** Is the input in degrees (true) or radians (false). */
  protected Angle.Format itsFormat = Angle.Format.RADIANS;

  public
  TranslationAngleToNumber(PointDescription parent, String[] init)
  {
    super(parent, init);
    if (init.length > 0) {
      if (init[0].equalsIgnoreCase("d")) {
        itsFormat = Angle.Format.DEGREES;
      } else if (init[0].equalsIgnoreCase("r")) {
        itsFormat = Angle.Format.RADIANS;
      } else {
        System.err.println("TranslationAngleToNumber (" + itsParent.getFullName() 
                           + "): Illegal argument expect \"d\" or \"r\"");
      }
    }
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    //Ensure there is raw data for us to translate!
    if (data==null || data.getData()==null) {
      return null;
    }

    if (! (data.getData() instanceof Angle)) {
      System.err.println("ERROR: TranslationAngleToNumber (" + itsParent.getFullName()
                         + "): Expect class \"Angle\" as input");
      return null;
    }

    double val;
    if (itsFormat==Angle.Format.DEGREES) {
      val = ((Angle)data.getData()).getValueDeg();
    } else {
      val = ((Angle)data.getData()).getValue();
    }
    
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), new Double(val));

    return res;
  }


  public static
  String[]
  getArgs()
  {
    return itsArgs;
  }
}
