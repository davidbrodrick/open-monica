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
 * Outputs True for a fixed mark period when triggered and then outputs False for at least the specified space period irrespective
 * of input. This sequence is triggered by looking at the input as a boolean.
 * 
 * It requires two arguments:
 * <ol>
 * <li><b>Mark Period:</b> The minimum period (in seconds) to maintain the mark output.
 * <li><b>Space Period:</b> The minimum period (in seconds) to maintain the space output (optional).
 * </ol>
 * 
 * @author David Brodrick
 */
public class TranslationPulse extends Translation {
  /** The period to pulse for. */
  RelTime itsMarkPeriod;

  /** The minimum period between rising edge of consecutive pulses. */
  RelTime itsSpacePeriod = RelTime.factory(0);

  /** The time we last pulsed. */
  AbsTime itsLastPulse = null;

  /** Constructor. */
  public TranslationPulse(PointDescription parent, String[] init) {
    super(parent, init);
    try {
      itsMarkPeriod = RelTime.factory((long) (Double.parseDouble(init[0]) * 1000000));
      if (init.length > 1) {
        itsSpacePeriod = RelTime.factory((long) (Double.parseDouble(init[1]) * 1000000));
      }
    } catch (Exception e) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): While parsing constructor string arguments: " + e);
    }
  }

  /** Determine output based on input and timing constraints. */
  public PointData translate(PointData data) {
    Boolean output = null;
    // If we are currently triggered then don't care about input
    AbsTime now;
    if (data == null) {
      now = AbsTime.factory();
    } else {
      now = data.getTimestamp();
    }
    if (itsLastPulse != null && itsLastPulse.add(itsMarkPeriod).add(itsSpacePeriod).compare(now) > 0) {
      if (itsLastPulse.add(itsMarkPeriod).compare(now) > 0) {
        // Currently generating the mark
        output = new Boolean(true);
      } else {
        // Currently generating the space
        output = new Boolean(false);
      }
    } else {
      // Currently idle (generating space), need to check input for new trigger
      try {
        output = new Boolean(MonitorUtils.parseAsBoolean(data.getData()));
      } catch (IllegalArgumentException e) {
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.error("(" + itsParent.getFullName() + "): " + e);
        output = null;
      }

      if (output != null && output.booleanValue()) {
        // Input is now high, triggering a new pulse
        itsLastPulse = now;
      }
    }
    return new PointData(itsParent.getFullName(), now, output);
  }

}
