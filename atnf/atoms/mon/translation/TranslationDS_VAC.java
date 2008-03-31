//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;


/**
 * This class can perform <i>VAC</i> translation for data from an
 * ATNF dataset.
 *
 * <P><TT><CENTER>RESULT = </CENTER></TT>
 *
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @version $Id: TranslationDS_VAC.java,v 1.2 2004/10/07 05:48:24 bro764 Exp $
 */
public class TranslationDS_VAC
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "VAC Translation", "DS_VAC",
    "Scale",  "java.lang.String",
    "Offset", "java.lang.String" };
  /** Gain scalar to apply to the data. */
  protected double itsScale = 1.0;
  /** Offset to add to the rescaled data. */
  protected double itsOffset = 0.0;

  public
  TranslationDS_VAC(PointMonitor parent, String[] init)
  {
    super(parent, init);
    //Parse scale and offset from the argument strings
    if (init.length == 2) {
      itsScale  = Double.parseDouble(init[0]);
      itsOffset = Double.parseDouble(init[1]);
    }
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    //Ensure there is raw data for us to translate!
    if (data==null || data.getData()==null) return null;

    Object d = data.getData();
    if (d instanceof Number) {
      double temp = ((((Number)d).doubleValue() + 0.5)/2048.0) - 1.0;
      //Convert pseudo-real by applying the specified scale and offset
      temp = itsScale*temp + itsOffset;
      if (temp<0.05) {
	temp = 0.001;
      } else if (temp<9.7) {
	double f = Math.pow(10.0, temp*0.465);
	temp = 0.00196*(f-1.0) + 0.0043;
	temp = temp - Math.IEEEremainder(temp, 0.0001);
      } else {
	temp = 1000.0;
      }

      //Translation is now complete
      return new PointData(data.getName(),
			   data.getSource(),
			   data.getTimestamp(),
                           data.getRaw(),
			   new Float(temp));
    } else {
      //We can only translate Numbers using this class
      if (MonitorMap.logger!=null) {
	MonitorMap.logger.error("TranslationDS_VAC: Non-Numeric type for "
				+ data.getName() + "(" + data.getSource() + ")");
      } else {
	System.err.println("TranslationDS_VAC: Non-Numeric type for "
			   + data.getName() + "(" + data.getSource() + ")");
      }
      return null;
    }
  }

  public static
  String[]
  getArgs()
  {
    return itsArgs;
  }
}
