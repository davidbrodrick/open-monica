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
 * Call the String.split() method to turn the input string into an array of output strings. Other points can then listen to this one
 * and extract a particular field using TranslationArray.
 * 
 * <P>
 * The first (optional) argument is the regexp for the delimiter to use to split the string. By default the string will be split
 * whereever there is one or more spaces.
 * 
 * <P>
 * An optional second argument specifies the minimum number of elements expected once the input has been split. If less than this
 * number of elements is found then the input will be discarded.
 * 
 * @author David Brodrick
 */
public class TranslationStringToArray extends Translation {
  /** The regexp used to split strings. */
  protected String itsRegexp = " +";
  
  /** The expected minimum number of elements (-1 to disable). */
  protected int itsMinElements = -1;

  public TranslationStringToArray(PointDescription parent, String[] init) {
    super(parent, init);
    if (init != null && init.length >= 1) {
      itsRegexp = init[0];
      if (init.length>=2) {
        itsMinElements = Integer.parseInt(init[1]);
      }
    }
  }

  /** Map the input data to an output string. */
  public PointData translate(PointData data) {
    // preconditions
    if (data == null)
      return null;
    Object val = data.getData();

    // If we got null-data then throw a null-data result
    if (val == null) {
      return new PointData(itsParent.getFullName());
    }

    // Get input value as a string
    String strval = val.toString().trim();
    // Split string
    String[] resstrings = strval.split(itsRegexp);
    // Validate input
    if (itsMinElements!=-1 && resstrings.length<itsMinElements) {
      // This input is no good. Halt translation process here.
      return null;
    }    
    // Trim each String
    for (int i = 0; i < resstrings.length; i++) {
      resstrings[i] = resstrings[i].trim();
    }
    // Generate output
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), resstrings);
    return res;
  }
}
