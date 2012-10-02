//
// Copyright (C) Inside Systems Pty Ltd
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointDescription;

/**
 * Calculate precipitable water in mm by listening to two other
 * points, one representing temperature in degrees Celcius and the other
 * representing the relative humidity as a percentage.
 * <P>
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with temperature being the first argument.
 * <P>
 * The method is derived in MMA Memo 238 by Bryan Butler :<BR>
 * <TT>http://legacy.nrao.edu/alma/memos/html-memos/alma238/memo238.pdf</TT><BR>
 *
 * @author Balt Indermuehle (adapted from David Brodrick's original version)
 */
public class
TranslationPrecipitableWaterMMA
extends TranslationDualListen
{

  protected static String[] itsArgs = new String[]{
    "Precipitable Water", "what goes here",
    "Temperature (C)", "java.lang.String",
    "Relative Humidity (%)", "java.lang.String"};

  public
  TranslationPrecipitableWaterMMA(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Calculate the vapour pressure from the temperature and relative
   * humidity.
   *@param val1 Most recent temperature (in degrees C)
   *@param val2 Most recent relative humidity (in percent)
   *@return Float containing the vapour pressure (in kPa) */
  protected
  Object
  doCalculations(Object val1, Object val2)
  {
    if (! (val1 instanceof Number) || ! (val2 instanceof Number)) {
      System.err.println("TranslationPrecipitableWater: " + itsParent.getName()
			 + ": ERROR got non-numeric data!");
      return null;
    }

    double temp = ((Number)val1).doubleValue() + 273.15;
    double rh = ((Number)val2).doubleValue();

    double res = 2.409e12 * rh * Math.pow(300.0/temp, 4) * Math.exp(-22.64 * (300.0 / temp)) / (3 * temp);

    //Round off insignificant digits
    res = res - Math.IEEEremainder(res, 0.01);

    return new Float(res);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
