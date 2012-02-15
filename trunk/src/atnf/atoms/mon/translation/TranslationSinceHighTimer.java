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
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Measures the amount of time since the input was last in a high/mark state.
 * This requires the input to be a Boolean or a Number. The output is a RelTime
 * type. The output will not be able to be produced until the input has gone
 * high for the first time.
 * 
 * @author David Brodrick
 */
public class TranslationSinceHighTimer extends Translation {
  /** The time the input was last high. */
  protected AbsTime itsLastHigh;

  /** Constructor. */
  public TranslationSinceHighTimer(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Determine output based on input. */
  public PointData translate(PointData data) {
    RelTime output = null;
    AbsTime now = data.getTimestamp();

    // Get the input as a Boolean
    Boolean inputstate;
    try {
      inputstate = new Boolean(MonitorUtils.parseAsBoolean(data.getData()));
    } catch (IllegalArgumentException e) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): " + e);
      inputstate = null;
      output = null;
    }

    if (inputstate != null) {
      if (inputstate.booleanValue()) {
        // Input is currently high, so no time has elapsed
        itsLastHigh = now;
        output = RelTime.factory(0);
      } else if (itsLastHigh != null) {
        // Calculate the time since the input was last high
        output = Time.diff(now, itsLastHigh);
      } else {
        // Unable to calculate output value as output has always been low
        output = null;
      }
    }
    if (output != null) {
      return new PointData(itsParent.getFullName(), now, output);
    } else {
      return null;
    }
  }
}
