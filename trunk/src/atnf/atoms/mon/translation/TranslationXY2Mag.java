//
// Copyright (C) Oz Forecast
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Listen to two points which represent the X and Y components of a vector
 * and return the magnitude of the vector.
 *
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with X being the first argument.
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class
TranslationXY2Mag
extends TranslationDualListen
{
  protected static String[] itsArgs = new String[]{};
  
  public
  TranslationXY2Mag(PointMonitor parent, String[] init)
  {
    super(parent, init);
  }


  protected
  Object
  doCalculations(Object val1, Object val2)
  {
    if (! (val1 instanceof Number) || ! (val2 instanceof Number)) {
      System.err.println("TranslationXY2Mag: " + itsParent.getName()
			 + ": ERROR got invalid data!");
      return null;
    }

    double v1 = ((Number)val1).doubleValue();
    double v2 = ((Number)val2).doubleValue();

    return new Float(Math.sqrt(v1*v1 + v2*v2));
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
