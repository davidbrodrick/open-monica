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
 * This class translates an instance of a Number sub-class to an Angle.
 * An optional format argument may be used to specify whether the input is
 * in degrees (<i>"d"</i>) or radians (<i>"r"</i>). If no format is specified
 * then the input is assumed to be in radians.
 *
 * @author David Brodrick
 * @version $Id: $
 */

public class TranslationNumberToAngle
extends Translation
{
  /** Arguments. Only the first argument is essential. */
  protected static String itsArgs[] = new String[]{
    "Translation NumberToAngle","NumberToAngle",
    "Format", "java.lang.String"};

  /** Is the input in degrees (true) or radians (false). */
  protected Angle.Format itsFormat = Angle.Format.RADIANS;

  public
  TranslationNumberToAngle(PointDescription parent, String[] init)
  {
    super(parent, init);
    if (init.length > 0) {
      if (init[0].equalsIgnoreCase("d")) {
        itsFormat = Angle.Format.DEGREES;
      } else if (init[0].equalsIgnoreCase("r")) {
        itsFormat = Angle.Format.RADIANS;
      } else {
        System.err.println("TranslationNumberToAngle (" 
                           + itsParent.getSource() + "." + itsParent.getName()
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

    if (! (data.getData() instanceof Number)) {
      System.err.println("ERROR: TranslationNumberToAngle (" 
                         + itsParent.getSource() + "." + itsParent.getName()
                         + "): Expect \"Number\" as input");
      return null;
    }

    double arg = ((Number)data.getData()).doubleValue();
    Angle newangle = Angle.factory(arg, itsFormat);
    
    PointData res = new PointData(data.getName(), data.getSource(),
                                  data.getTimestamp(), newangle);

    return res;
  }


  public static
  String[]
  getArgs()
  {
    return itsArgs;
  }
}
