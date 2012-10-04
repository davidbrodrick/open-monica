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
 * Replace the input value with the last value of a nominated other point whose timestamp precedes that of the input.
 * 
 * <P>
 * For instance say you have a point X which updates once per minute, and a point Y which updates every 10 minutes but is delayed by
 * 10 minutes. You could use this translation to find what the value of X was when the delayed value of Y was actually generated.
 * 
 * @author David Brodrick
 */
public class TranslationPreceding extends Translation {
  /** Name of the point whose value we will adopt. */
  private String itsOtherPoint;

  public TranslationPreceding(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length != 1) {
      throw new IllegalArgumentException("Requires 1 argument");
    }

    itsOtherPoint = init[0];
    itsOtherPoint.replaceAll("$1", itsParent.getSource());
  }

  public PointData translate(PointData data) {
    PointData lastval = PointBuffer.getPreceding(itsOtherPoint, data.getTimestamp());
    PointData res = null;
    if (lastval != null) {
      res = new PointData(lastval);
      res.setName(itsParent.getFullName());
    }
    return res;
  }
}
