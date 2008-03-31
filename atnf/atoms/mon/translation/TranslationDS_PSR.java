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
 * This class can perform <i>pseudo-real</i> translation for data from an
 * ATNF dataset. The pseudo-real translation remaps the dataset readout
 * to the range <i>-1.0<=d<=1.0</i> and then applies a scale and an offset
 * to perform linear remapping to the desired range. The scale and offset
 * are required arguments for this class.
 *
 * <P><TT><CENTER>RESULT = SCALE*((RAW/2048)-1) + OFFSET</CENTER></TT>
 *
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @author Le Cuong Nguyen
 * @version $Id: TranslationDS_PSR.java,v 1.1 2004/06/02 06:48:55 bro764 Exp bro764 $
 */
public class TranslationDS_PSR
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "Pseudo-Real Translation","DS_PSR",
    "Scale",  "java.lang.String",
    "Offset", "java.lang.String" };
  /** Gain scalar to apply to the pseudo-real translation of the data. */
  protected double itsScale = 1.0;
  /** Offset to add to the rescaled data. */
  protected double itsOffset = 0.0;
  /** Precision required in the output. */
  private double itsPrec = 0.0;

  public
  TranslationDS_PSR(PointMonitor parent, String[] init)
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
    //Ensure there is raw data for us to translate!
    if (data==null) return null;
    if (data.getData()==null) {
      //Return a null result
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    Object d = data.getData();
    if (d instanceof Number) {
      //Convert dataset readout to pseudo-real form
      double temp = ((Number)d).doubleValue()/2048.0 - 1;
      //Convert pseudo-real by applying the specified scale and offset
      temp = itsScale*temp + itsOffset;
      //Limit output to significant digits only
      if (itsPrec!=0.0) {
	temp = temp - Math.IEEEremainder(temp, itsPrec);
      }
      //Translation is now complete
      return new PointData(itsParent.getName(),
			   itsParent.getSource(),
			   data.getTimestamp(),
                           data.getRaw(),
			   new Float(temp));
    } else {
      //We can only translate Numbers using this class
      if (MonitorMap.logger!=null) {
	MonitorMap.logger.error("TranslationDS_PSR: Non-Numeric type for "
				+ itsParent.getName() + "("
				+ itsParent.getSource() + ")");
      } else {
	System.err.println("TranslationDS_PSR: Non-Numeric type for "
			   + itsParent.getName() + "("
			   + itsParent.getSource() + ")");
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
    double step = 2*itsScale/4096.0;
    //Work out the number of decimals required to fully express it
    double res;
    if (step<1.0 && step>0.0) {
      final double log10 = Math.log(10.0);
      double l = Math.log(step) / log10;
      l = Math.ceil(Math.abs(l));
      res = 1.0/Math.pow(10.0, l+1);
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
