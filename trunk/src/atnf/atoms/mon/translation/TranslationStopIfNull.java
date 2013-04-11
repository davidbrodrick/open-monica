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
import atnf.atoms.util.Angle;

/**
 * Stops the point update/translation process if the update has a null data value.
 * 
 * <P>
 * It is valid for Translations to return a PointData object with a null data value, however at times we would like to detect this
 * and arrest the point update process without attempting to perform any further translation. This can be accomplished by returning
 * a null PointData (as to opposed a valid PointData with a null value). An object of this class with return a null PointData if the
 * input has a null data value. If the input data is not null then the input PointData is returned without change.
 * 
 * @author David Brodrick
 */
public class TranslationStopIfNull extends Translation {
  public TranslationStopIfNull(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Calculate the delta and return new value. */
  public PointData translate(PointData data) {
    if (data.getData() == null) {
      // Return null data to terminate translation for this point
      return null;
    } else {
      // Just return the input
      return data;
    }
  }
}
