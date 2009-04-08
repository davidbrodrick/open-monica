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

/**
 * Calculate precipitable water in mm by listening to two other
 * points, one representing temperature in degrees Celcius and the other
 * representing the relative humidity as a percentage.
 * <P>
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with temperature being the first argument.
 * <P>
 * The method follow this site:<BR>
 * <TT>http://www.all-science-fair-projects.com/science_fair_projects/21/821/90f14e4ad9bfe9ee9c53175dac83f954.html</TT><BR>
 *
 * @author David Brodrick
 * @version $Id: TranslationPrecipitableWater.java,v 1.1 2005/07/25 05:04:27 bro764 Exp bro764 $
 */
public class
TranslationPrecipitableWater
extends TranslationDualListen
{

  protected static String[] itsArgs = new String[]{
    "Precipitable Water", "what goes here",
    "Temperature (C)", "java.lang.String",
    "Relative Humidity (%)", "java.lang.String"};

  public
  TranslationPrecipitableWater(PointMonitor parent, String[] init)
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
    double relhumid = ((Number)val2).doubleValue()/100.0;

    double res = 0.439*relhumid*Math.exp(26.23 - 5416.0/temp)/temp;

    //As mm
    res = 10.0*res;

    //Round off insignificant digits
    res = res - Math.IEEEremainder(res, 0.01);

    return new Float(res);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
