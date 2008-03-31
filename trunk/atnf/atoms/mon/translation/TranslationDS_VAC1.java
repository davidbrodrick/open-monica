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
 * Translate a vacuum pressure reading for the KW-band receivers on the
 * Compact Array. Really this translation could be realised by one of the
 * other translation classes like the DS_T16, however we have implemented
 * this class to enable some sensor-specific adaptations:
 * <P>
 * The vacuum sensors produce a positive DC voltage to indicate the
 * vacuum pressure. A zero volt reading indicates a perfect vacuum. In
 * practice, small DC offsets on the ADC can give a negative reading
 * when the vacuum is good. The code has a few special cases for dealing
 * with this fault, while still ensuring large negative voltages (indicating
 * a sensor or ADC failure) are not disguised.
 *
 * <P>Of course, this class can only perform the translation when the raw
 * data is a <i>Number</i>.
 *
 * @author David Brodrick
 * @author Mark Wieringa
 * @version $Id: TranslationDS_VAC1.java,v 1.3 2004/10/07 05:53:05 bro764 Exp $
 */
public class TranslationDS_VAC1
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "VAC1 Translation", "DS_VAC1"};

  public
  TranslationDS_VAC1(PointMonitor parent, String[] init)
  {
    super(parent, init);
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
      //Get raw dataset reading as unsigned integer
      double temp = ((Number)d).doubleValue();
      //Convert to two's complement if need be
      if (temp>32768) temp = temp - 65535.0;
      //Apply appropriate translation
      temp = temp/81920.0;

      //Map small numbers to a standard reading
      if (temp<0.001 && temp>=-0.001) temp = 0.001;
      //Finally remove insignificant figures
      temp = temp - Math.IEEEremainder(temp, 0.0001);

      //Translation complete
      return new PointData(data.getName(),
			   data.getSource(),
			   data.getTimestamp(),
			   data.getRaw(),
			   new Float(temp));
    } else {
      //We can only translate Numbers using this class
      if (MonitorMap.logger!=null) {
	MonitorMap.logger.error("TranslationDS_VAC1: Non-Numeric type for "
				+ data.getName() + "(" + data.getSource() + ")");
      } else {
	System.err.println("TranslationDS_VAC1: Non-Numeric type for "
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
