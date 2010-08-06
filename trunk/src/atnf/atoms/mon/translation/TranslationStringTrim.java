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
 * Remove any leading/trailing whitespace from a string.
 * 
 * @author David Brodrick
 */
public class TranslationStringTrim extends Translation
{
  public TranslationStringTrim(PointDescription parent, String[] init)
  {
    super(parent, init);
  }

  public PointData translate(PointData data)
  {
    Object val = data.getData();

    // If we got null-data then throw a null-data result
    if (val == null) {
      return new PointData(itsParent.getFullName());
    }

    // Get value as a string
    String resstr = val.toString().trim();

    // Create return structure with right details
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), resstr);

    return res;
  }
}
