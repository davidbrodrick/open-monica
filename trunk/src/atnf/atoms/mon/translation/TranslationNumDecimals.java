// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;

/**
 * Translation to limit the number of decimal places in a floating point
 * number. This is useful for "cleaning up" the output of other Translations
 * which may generate output with too many insignificant figures.
 *
 * <P>Only one <i>init</i> argument is required, it must be an integer which
 * represents the maximum number of allowed decimal places. e.g. If the init
 * argument specified 3 decimal places and the input number was 3.14159265
 * then the output number would be 3.141.
 *
 * <P>Normally the output will be of type Double but if the specified number
 * of decimals is zero then Integers will be generated. This conversion
 * might prove useful in some circumstances. If the input is an Angle then the
 * output will also be an Angle.
 *
 * @author David Brodrick
 * @version $Id: TranslationNumDecimals.java,v 1.2 2005/08/15 01:50:43 bro764 Exp $
 **/
public class
TranslationNumDecimals
extends Translation
{
  protected static String itsArgs[] = new String[]{
    "Translation - NumDecimals", "Number of Decimals",
    "Max. Number of Decimals", "java.lang.Integer"};

  /** The required precision. */
  private double itsNumPlaces;

  public
  TranslationNumDecimals(PointDescription parent, String[] init)
  {
    super(parent, init);

    if (init.length!=1) {
      System.err.println("TranslationNumDecimals for \"" + parent.getName()
			 + "\": Expect ONE Argument!");
      itsNumPlaces = 3; //Make it up..
    } else {
      try {
	itsNumPlaces = Integer.parseInt(init[0]);
      } catch (NumberFormatException e) {
	System.err.println("TranslationNumDecimals for \"" + parent.getName()
			   + "\": " + e.getMessage());
	itsNumPlaces = 3; //Make it up..
      }
    }
    //Convert from number of places to power of ten
    itsNumPlaces = Math.pow(10.0, itsNumPlaces);
  }


  /** Do the translation. */
  public
  PointData
  translate(PointData data)
  {
    if (data==null) {
      return null;
    }

    Object rawval = data.getData();

    //Need to make new object with our parent as source/name
    PointData res = new PointData(itsParent.getName(),
				  itsParent.getSource(),
				  data.getTimestamp(),
				  null);

    Object newval = null;

    if (rawval==null) {
      newval = null;
    } else if (rawval instanceof Number) {
      double temp = ((Number)rawval).doubleValue();
      //Had problems with remainder technique, so multiply the number
      //up, then convert it to an integer and then scale it back..
      temp = Math.round(temp*itsNumPlaces)/itsNumPlaces;

      if (itsNumPlaces==1) {
	//Need to output an Integer
	newval = new Integer(((Number)newval).intValue());
      } else {
	newval = new Double(temp);
      }
    } else if (rawval instanceof Angle) {
      //Maybe we should process this in degrees instead?
      double temp = ((Angle)rawval).getValue();
      ///This remainder scheme doesn't seem to work properly..
      temp = temp - Math.IEEEremainder(temp, itsNumPlaces);
      newval = Angle.factory(temp);
    } else {
      //It's an unknown data type
      System.err.println("TranslationNumDecimals for \"" + itsParent.getName()
			 + "\": UNEXPECTED CLASS: " + rawval.getClass());
      return null;
    }

    //Save the result
    res.setData(newval);
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
