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
 * Call the String.split() method to turn the input string into an array of 
 * output strings whereever the regexp given as an argument appears. Other
 * points can then listen to this one and extract a particular field using
 * TranslationArray.
 *
 * @author David Brodrick
 */
public class
TranslationStringToArray
extends Translation
{
  /** The regexp used to split strings. */
  protected String itsRegexp = " ";

  protected static String[] itsArgs = new String[]{"Translation String to Array",
  "String2Array"};

  public TranslationStringToArray(PointDescription parent, String[] init)
  {
    super(parent, init);
    if (init!=null && init.length>=1) {
      itsRegexp=init[0]; 
    }
  }


  /** Map the input data to an output string. */
  public
  PointData
  translate(PointData data)
  {
    //preconditions
    if (data==null) return null;
    Object val = data.getData();

    //If we got null-data then throw a null-data result
    if (val==null) {
      return new PointData(itsParent.getFullName());
    }

    //Get input value as a string
    String strval = val.toString().trim();
    //Split string
    String[] resstrings = strval.split(itsRegexp);
    //Trim each String
    for (int i=0; i<resstrings.length; i++) {
      resstrings[i]=resstrings[i].trim();
    }
    //Generate output
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), resstrings);
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
