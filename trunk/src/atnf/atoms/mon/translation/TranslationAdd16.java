//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.MonitorMap;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;


/**
 * This adds a constant offset to a 16 bit number. The number will be
 * folded at 0 and 65535.
 *
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @version $Id: TranslationAdd16.java,v 1.2 2008/02/25 00:44:27 bro764 Exp $
 */
public class TranslationAdd16
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "Add-16 Translation","Add16",
    "Offset", "java.lang.String" };

  /** Offset to add to the rescaled data. */
  protected short itsOffset = 0;

  public
  TranslationAdd16(PointDescription parent, String[] init)
  {
    super(parent, init);
    //Parse offset from the argument string
    if (init.length == 1) {
      itsOffset = Short.parseShort(init[0]);
    }
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    if (data==null) {
      return null;
    }

    //Ensure there is raw data for us to translate!
    //Might actually need to return pd with null data field here?
    if (data.getData()==null) {
      return null;
    }

    Object d = data.getData();
    if (d instanceof Number) {
      Short s = new Short((short)(((Number)d).shortValue() + itsOffset));
      //System.err.println("TranslationAdd16: " + s.shortValue() + " = " + ((Number)d).shortValue() + " + " + itsOffset);
      //Translation is now complete
      return new PointData(itsParent.getFullName(), data.getTimestamp(), s);
    } else {
      //We can only translate Numbers using this class
      if (MonitorMap.logger!=null) {
	MonitorMap.logger.error("TranslationAdd16: Non-Numeric type for " + data.getName());
      } else {
	System.err.println("TranslationAdd16: Non-Numeric type for " + data.getName());
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
