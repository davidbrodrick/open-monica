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
 * Measures the amount of time that the output has been in a low/space state. The output will be zero if the input is in a high/mark
 * state. This requires the input to be a Boolean or a Number. The output is a RelTime type.
 * 
 * <P>
 * This differs from SinceHighTimer in that, if the input is low when MoniCA starts, the output will begin counting up from 0
 * rather than be assigned a null value until the first falling edge is found.
 * 
 * @author David Brodrick
 */
public class TranslationLowTimer extends Translation {
  /** The time the input last became low. */
  protected AbsTime itsLastLow;

  /** Constructor. */
  public TranslationLowTimer(PointDescription parent, String[] init) {
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
        // Input is currently high, so no time has elapsed since the low state began
        itsLastLow = null;
        output = RelTime.factory(0);
      } else if (itsLastLow == null) {
        // The input has just gone low
        itsLastLow = now;
        output = RelTime.factory(0);
      } else {
        // Output continues to be low
        output = Time.diff(now, itsLastLow);        
      }
    }
    if (output != null) {
      return new PointData(itsParent.getFullName(), now, output);
    } else {
      return null;
    }
  }
}
