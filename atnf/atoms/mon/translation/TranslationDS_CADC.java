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
 * Convert a reading from the ADC in the C32 or C42 into a real voltage.
 * Really we could probably use one of the other DS translations for this.
 *
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class TranslationDS_CADC
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
      "C32/42 ADC Translation","DS_CADC"};

  public
  TranslationDS_CADC(PointMonitor parent, String[] init)
  {
    super(parent, init);
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    if (data==null) return null;

    //Ensure there is raw data for us to translate!
    if (data.getData()==null) {
      //Return a null result
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    Object d = data.getData();
    if (d instanceof Number) {
      double temp = ((Number)d).doubleValue();
      //Make two's complement
      if (temp>32767.0) temp = temp - 65536.0;
      //Apply the ADC, voltage divider scaling factor to get volts
      temp = temp*0.000615;
      //Truncate to two decimals
      temp = Math.round(temp*100.0)/100.0;
      //Translation is now complete
      return new PointData(data.getName(),
			   data.getSource(),
			   data.getTimestamp(),
			   data.getRaw(),
			   new Float(temp));
    } else {
      //We can only translate Numbers using this class
      if (MonitorMap.logger!=null) {
	MonitorMap.logger.error("TranslationDS_DADC: Non-Numeric type for "
				+ data.getName() + "(" + data.getSource() + ")");
      } else {
	System.err.println("TranslationDS_DADC: Non-Numeric type for "
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
