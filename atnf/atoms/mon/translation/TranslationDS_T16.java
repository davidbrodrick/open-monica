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
 * This class can perform <i>T16</i> translation for data from an
 * ATNF dataset.
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @author Mark Wieringa
 * @version $Id: TranslationDS_T16.java,v 1.2 2005/09/27 05:15:44 bro764 Exp $
 */
public class TranslationDS_T16
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "Twos-Complement-16 Translation","DS_T16",
    "Scale",  "java.lang.String",
    "Offset", "java.lang.String" };

  /** Gain scalar to apply to the translation of the data. */
  protected double itsScale = 1.0;

  /** Offset to add to the rescaled data. */
  protected double itsOffset = 0.0;

  /** Precision required in the output. */
  private double itsPrec = 0.0;

  public
  TranslationDS_T16(PointMonitor parent, String[] init)
  {
    super(parent, init);
    //Parse scale and offset from the argument strings
    if (init.length == 2) {
      if (init[0].indexOf('/') > -1)
	itsScale = Double.parseDouble(init[0].substring(0, init[0].indexOf('/'))) / Double.parseDouble(init[0].substring(init[0].indexOf('/')+1, init[0].length()));
      else itsScale = Double.parseDouble(init[0]);
      itsOffset = Double.parseDouble(init[1]);
    }
    itsPrec = getPrecision();
    if (itsPrec<=0.0) itsPrec = 0.0;
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    if (data==null) return null;

    //Ensure there is raw data for us to translate!
    //Might actually need to return pd with null data field here?
    if (data.getData()==null) return null;

    Object d = data.getData();
    if (d instanceof Number) {
      double temp = ((Number)d).doubleValue();
      //Get sign
      if (temp>=32768.0) temp=temp-65535.0;
      //Account for +/-4V 16 bit ADC
      temp = temp*0.000122072;
      //Applying the specified scale and offset
      temp = itsScale*temp + itsOffset;

      //Limit output to significant digits only
      if (itsPrec!=0.0) {
	temp = temp - Math.IEEEremainder(temp, itsPrec);
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
	MonitorMap.logger.error("TranslationDS_T16: Non-Numeric type for "
				+ data.getName() + "(" + data.getSource() + ")");
      } else {
	System.err.println("TranslationDS_T16: Non-Numeric type for "
			   + data.getName() + "(" + data.getSource() + ")");
      }
      return null;
    }
  }


  /** Calculate the number of significant decimals for this translation. */
  private
  double
  getPrecision()
  {
    //Get the quantisation step after value translation
    double step = 2*itsScale/32767.0;
    //Work out the number of decimals required to fully express it
    double res;
    if (step<1.0 && step>0.0) {
      final double log10 = Math.log(10.0);
      double l = Math.log(step) / log10;
      l = Math.ceil(Math.abs(l));
      res = 1.0/Math.pow(10.0, l);
    } else {
      res = 0.0;
    }
    return res;
  }


  public static
  String[]
  getArgs()
  {
    return itsArgs;
  }
}
