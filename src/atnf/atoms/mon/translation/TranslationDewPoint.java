//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * Calculate dew point by translating a measurement of water vapour pressure.
 * <P>
 * This class must listen to a point which provides the water vapour
 * pressure in kiloPascals as an <i>init</i> argument. It uses that quantity
 * to produce a measurement of the dew point temperature in degrees Celcius.
 * <P>
 * The methods follow this site:<BR>
 * <TT>http://www.agsci.kvl.dk/~bek/relhum.htm</TT><BR>
 * Which follows the technique of <i>Jensen et al. (1990) ASCE Manual No.
 * 70 (pages 176 & 177)</i>.
 *
 * @author David Brodrick
 * @author David McConnell
 * @version $Id: TranslationDewPoint.java,v 1.4 2004/10/19 23:17:22 bro764 Exp $
 */
public class
TranslationDewPoint
extends Translation
{

  protected static String[] itsArgs = new String[]{"Dew Point",
  "what goes here"};

  public TranslationDewPoint(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Convert Vapour Pressure and return new value. */
  public
  PointData
  translate(PointData data)
  {
    //Precondition
    if (data==null) {
      return null;
    }

    //Create return structure with right details
    PointData res = new PointData(itsParent.getName(),
				  itsParent.getSource());
    //Call the calculation method and save result
    res.setData(doCalculations(data.getData()));

    return res;
  }



  /** Calculate the dew point from the water vapour pressure.
   *@param val Most recent water vapour pressure (in kPa)
   *@return Current dew point temperature (in degrees C) */
  protected
  Object
  doCalculations(Object val)
  {
    //Do some reality checks
    if (val == null) {
      return null;
    }
    if (! (val instanceof Number) ) {
      System.err.println("TranslationDewPoint: " + itsParent.getName()
			 + ": ERROR got non-numeric argument data!");
      return null;
    }

    //Extract the water vapour pressure
    double e  = ((Number)val).doubleValue();
    double td = (116.9 + 237.3*Math.log(e)) / (16.78 - Math.log(e));

    //Round off insignificant digits
    td = td - Math.IEEEremainder(td, 0.01);

    return new Float(td);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
