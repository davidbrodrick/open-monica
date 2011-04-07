//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.translation.Translation;

/**
 * Converts a RelTime input to a Double representing the time period in seconds.
 * 
 * @author David Brodrick
 */
public class TranslationRelTimeToSeconds extends Translation {
  /** Constructor. */
  public TranslationRelTimeToSeconds(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Determine output based on input. */
  public PointData translate(PointData data) {
    if (data.getData() == null) {
      return data;
    }

    Double output = null;
    if (!(data.getData() instanceof RelTime)) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): Expect RelTime input");
      output = null;
    } else {
      output = new Double(((RelTime) data.getData()).getAsSeconds());
    }
    return new PointData(itsParent.getFullName(), data.getTimestamp(), output);
  }
}
