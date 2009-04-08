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
 * and return the angle of the vector.
 *
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with X being the first argument. By default
 * the result is returned in radians but an optional third argument can be 
 * set to "d" and the angle will be returned as degrees.
 *
 * The output will occupy the domain:
 * <ul>
 * <li> 0<=x<=360 for degrees.
 * <li> 0<=x<=2*PI for radians.
 * </ul>
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class
TranslationXY2Angle
extends TranslationDualListen
{
  protected static String[] itsArgs = new String[]{};
  
  /** Set to true if the result must be degrees. */
  private boolean itsDegrees=false;  

  public
  TranslationXY2Angle(PointMonitor parent, String[] init)
  {
    super(parent, init);
    
    if (init.length==3 && init[2].toLowerCase().equals("d")) {
      //System.err.println("TranslationXY2Angle: Will produce degrees");
      itsDegrees=true;
    } else {
      //System.err.println("TranslationXY2Angle: Will produce radians");
    }
  }


  protected
  Object
  doCalculations(Object val1, Object val2)
  {
    if (! (val1 instanceof Number) || ! (val2 instanceof Number)) {
      System.err.println("TranslationXY2Angle: " + itsParent.getName()
			 + ": ERROR got invalid data!");
      return null;
    }

    double x = ((Number)val1).doubleValue();
    double y = ((Number)val2).doubleValue();

    Float res;
    if (itsDegrees) {
      res = new Float(180*Math.atan2(x, y)/Math.PI);
      if (res.floatValue()<0) {
        res = new Float(360 + res.floatValue());
      }
    } else {
      res = new Float(Math.atan2(x, y));
      if (res.floatValue()<0) {
        res = new Float(2*Math.PI + res.floatValue());
      }      
    }
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
