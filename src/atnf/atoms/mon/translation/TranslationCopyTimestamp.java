//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointBuffer;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * Change the timestamp on the new datum to be the same as the timestamp of the latest update of a nominated point.
 * 
 * @author David Brodrick
 */
public class TranslationCopyTimestamp extends Translation {
  /** Name of the point whose timestamp to borrow. */
  private String itsTimePoint;

  public TranslationCopyTimestamp(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length != 1) {
      throw new IllegalArgumentException("Requires 1 argument");
    }

    itsTimePoint = init[0];
    itsTimePoint.replaceAll("$1", itsParent.getSource());
  }

  public PointData translate(PointData data) {
    PointData res = new PointData(data);

    PointData latest = PointBuffer.getPointData(itsTimePoint);
    if (latest == null) {
      res = null;
    } else {
      res = new PointData(data);
      res.setTimestamp(latest.getTimestamp());
    }

    return res;
  }
}
