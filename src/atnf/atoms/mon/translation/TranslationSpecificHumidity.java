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
 * Calculate the specific humidity (in grams of water vapour per kilogram
 * of air) by listening to the values of two other points: one representing
 * the water vapour pressure in kiloPascals and the other being the
 * atmospheric pressure in HectoPascals.
 * <P>
 * <I>Should really modify this for consistancy in the units..</I>
 * <P>
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with vapour pressure being the first argument.
 * <P>
 * The methods follow this site:<BR>
 * <TT>http://www.agsci.kvl.dk/~bek/relhum.htm</TT><BR>
 * Which follows the technique of <i>Jensen et al. (1990) ASCE Manual No.
 * 70 (pages 176 & 177)</i>.
 *
 * @author David Brodrick
 * @author David McConnell
 * @version $Id: TranslationSpecificHumidity.java,v 1.2 2004/10/20 01:08:45 bro764 Exp $
 */
public class
TranslationSpecificHumidity
extends TranslationDualListen
{

  protected static String[] itsArgs = new String[]{
    "Specific Humidity", "what goes here",
    "Water Vapour Pressure Point", "java.lang.String",
    "Atmospheric Pressure Point", "java.lang.String",
  };
   
  public
  TranslationSpecificHumidity(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Calculate the specific humidity from the water vapour pressure and
   * atmospheric pressure.
   *@param val1 Most recent vapour pressure (in kPa)
   *@param val2 Most recent atmospheric pressure (in HPa)
   *@return Float containing the specific humidity in grams of water per
   * kilogram of air. */
  protected
  Object
  doCalculations(Object val1, Object val2)
  {
    if (! (val1 instanceof Number) || ! (val2 instanceof Number)) {
      System.err.println("TranslationSpecificHumidity: " + itsParent.getName()
                         + ": ERROR got non-numeric data!");
      return null;
    }

    final double Rl_Rv = 0.622;
    double e = ((Number)val1).floatValue(); //Get vapour pressure
    double p = ((Number)val2).floatValue()/10.0; //Get atmospheric pressure

    //Calculate the specific humidity
    double sh = 1000.0 * ( Rl_Rv*e / ( p + e*(Rl_Rv - 1.0)) );

    //Round off insignificant digits
    sh = sh - Math.IEEEremainder(sh, 0.01);

    return new Float(sh);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
