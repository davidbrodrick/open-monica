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
 * This class can perform <i>T12</i> translation for data from an
 * ATNF dataset.
 *
 * <P><TT><CENTER>RESULT = </CENTER></TT>
 *
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @version $Id: TranslationDS_T12.java,v 1.2 2004/10/18 02:30:27 bro764 Exp $
 */
public class TranslationDS_T12
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "T12 Translation", "DS_T12"};

  public
  TranslationDS_T12(PointMonitor parent, String[] init)
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
      //Get as a voltage
      double temp = 5.0*(((Number)d).doubleValue()/2048.0 - 1.0);

      if (temp>0.001) {
	temp = 5.03*Math.log(Math.abs(temp)) + 42.82 + temp*113.45;
	//Only keep one decimal place
	temp = temp - Math.IEEEremainder(temp, 0.1);
      } else {
	temp = 12.0;
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
	MonitorMap.logger.error("TranslationDS_T12: Non-Numeric type for "
				+ data.getName() + "(" + data.getSource() + ")");
      } else {
	System.err.println("TranslationDS_T12: Non-Numeric type for "
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
