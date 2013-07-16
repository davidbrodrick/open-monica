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
 * Change a string to UPPER case if the argument is "upper" or lower case if it is "lower".
 * 
 * @author David Brodrick
 */
public class TranslationStringCase extends Translation {
  /** Records if we change to upper case (true) or lower (false). */
  private boolean itsUpper;

  public TranslationStringCase(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length < 1) {
      throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - require one argument");
    }

    if (init[0].equalsIgnoreCase("upper")) {
      itsUpper = true;
    } else if (init[0].equalsIgnoreCase("lower")) {
      itsUpper = false;
    } else {
      throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - argument must be \"upper\" or \"lower\"");
    }
  }

  public PointData translate(PointData data) {
    Object val = data.getData();

    // If we got null-data then throw a null-data result
    if (val == null) {
      return new PointData(itsParent.getFullName());
    }

    // Get value as a string
    String resstr = val.toString();
    if (itsUpper) {
      resstr = resstr.toUpperCase();
    } else {
      resstr = resstr.toLowerCase();
    }

    // Create return structure with right details
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), resstr);

    return res;
  }
}
