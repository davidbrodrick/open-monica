//
// Copyright (C) Oz Forecast
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;

/**
 * Listen to two points which represent the magnitude and angle of a vector
 * in polar form and return the Y cartesian component of the vector.
 *
 * The names of the two points to listen to must be given as constructor
 * <i>init</i> arguments, with magnitude being the first argument. By default
 * the data from the angle point is considered to represent an angle in radians
 * but an optional third argument can be set to "d" and the angle will be 
 * interpreted as degrees.
 *
 * <P>The angle can either be a Number of an Angle object.
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class
TranslationPolar2Y
extends TranslationDualListen
{
  protected static String[] itsArgs = new String[]{};
  
  /** Set to true if the angle represents degrees. */
  private boolean itsDegrees=false;  

  public
  TranslationPolar2Y(PointMonitor parent, String[] init)
  {
    super(parent, init);
    
    if (init.length==3 && init[2].toLowerCase().equals("d")) {
      System.err.println("TranslationPolar2Y: Will interpret as degrees");
      itsDegrees=true;
    } else {
      System.err.println("TranslationPolar2Y: Will interpret as radians");
    }
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
    if (! (val1 instanceof Number) || 
        ! ((val2 instanceof Number) || (val2 instanceof Angle))) {
      System.err.println("TranslationPolar2Y: " + itsParent.getName()
			 + ": ERROR got invalid data!");
      return null;
    }

    double mag = ((Number)val1).doubleValue();
    double angle;
    if (val2 instanceof Number) {
      angle=((Number)val2).doubleValue();
      if (itsDegrees) {
        angle=Math.PI*angle/180.0;
      }
    } else {
      angle=((Angle)val2).getValue();
    }

    return new Float(mag*Math.cos(angle));
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
