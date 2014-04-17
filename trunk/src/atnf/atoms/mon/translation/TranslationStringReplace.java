//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Replace any instances of the first argument string in the input with the second argument string. If no second string is given
 * then instances of the target will be replaced with the empty string.
 * 
 * @author David Brodrick
 */
public class TranslationStringReplace extends Translation {
  /** The string to be replaced. */
  private String itsTarget;

  /** The replacement string. */
  private String itsReplacement = "";

  public TranslationStringReplace(PointDescription parent, String[] init) throws Exception {
    super(parent, init);

    if (init.length == 0) {
      throw new IllegalArgumentException("Insuffient arguments for TranslationStringReplace");
    } else if (init.length == 2) {
      itsReplacement = init[1];
    }

    itsTarget = init[0];
  }

  public PointData translate(PointData data) {
    Object val = data.getData();

    // If we got null-data then throw a null-data result
    if (val == null) {
      return new PointData(itsParent.getFullName());
    }

    // Get value as a string
    String str = val.toString();

    // Do the replacement
    str = str.replace(itsTarget, itsReplacement);

    // Create return structure with right details
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), str, data.getAlarm());

    return res;
  }
}
