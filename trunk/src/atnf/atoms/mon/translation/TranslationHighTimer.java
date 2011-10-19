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
 * Measures the amount of time that the output has been in a high/mark state. The output will be zero if the input is in a low/space
 * state. This requires the input to be a Boolean or a Number. The output is a RelTime type.
 * 
 * <P>
 * This differs from a SinceLowTimer in that, if the input is high when MoniCA starts, the output will begin counting up from 0
 * rather than be assigned a null value until the first rising edge is found.
 * 
 * @author David Brodrick
 */
public class TranslationHighTimer extends Translation {
  /** The time the input last became high. */
  protected AbsTime itsLastHigh;

  /** Constructor. */
  public TranslationHighTimer(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Determine output based on input. */
  public PointData translate(PointData data) {
    RelTime output = null;
    AbsTime now = data.getTimestamp();

    // Get the input as a Boolean
    Object rawinput = data.getData();
    Boolean inputstate = null;
    if (rawinput instanceof Boolean) {
      inputstate = (Boolean) rawinput;
    } else if (rawinput instanceof Number) {
      int intval = ((Number) rawinput).intValue();
      if (intval == 0) {
        inputstate = new Boolean(false);
      } else {
        inputstate = new Boolean(true);
      }
    }

    if (inputstate != null) {
      if (!inputstate.booleanValue()) {
        // Input is currently low, so no time has elapsed since the high state began
        itsLastHigh = null;
        output = RelTime.factory(0);
      } else if (itsLastHigh == null) {
        // The input has just gone high
        itsLastHigh = now;
        output = RelTime.factory(0);
      } else {
        // Output continues to be high
        output = Time.diff(now, itsLastHigh);        
      }
    } else {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): Expect Boolean or Number input");
      output = null;
    }
    if (output != null) {
      return new PointData(itsParent.getFullName(), now, output);
    } else {
      return null;
    }
  }
}
