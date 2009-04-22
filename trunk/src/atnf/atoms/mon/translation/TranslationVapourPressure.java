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

/**
 * Calculate water vapour pressure in kiloPascals by listening to two other
 * points, one representing temperature in degrees Celcius and the other
 * representing the relative humidity as a percentage.
 * <P>
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with temperature being the first argument.
 * <P>
 * The methods follow this site:<BR>
 * <TT>http://www.agsci.kvl.dk/~bek/relhum.htm</TT><BR>
 * Which follows the technique of <i>Jensen et al. (1990) ASCE Manual No.
 * 70 (pages 176 & 177)</i>.
 *
 * @author David Brodrick
 * @author David McConnell
 * @version $Id: TranslationVapourPressure.java,v 1.3 2004/10/20 00:57:27 bro764 Exp $
 */
public class
TranslationVapourPressure
extends TranslationDualListen
{

  protected static String[] itsArgs = new String[]{
    "Vapour Pressure", "what goes here",
    "Temperature (C)", "java.lang.String",
    "Relative Humidity (%)", "java.lang.String"};

  public
  TranslationVapourPressure(PointDescription parent, String[] init)
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
      System.err.println("TranslationVapourPressure: " + itsParent.getName()
			 + ": ERROR got non-numeric data!");
      return null;
    }

    double temp = ((Number)val1).doubleValue();
    double relhumid = ((Number)val2).doubleValue();

    double es = 0.611*Math.exp(17.27*temp/(temp + 237.3));
    double e  = es * (relhumid/100.0);

    //Round off insignificant digits
    e = e - Math.IEEEremainder(e, 0.001);

    return new Float(e);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
