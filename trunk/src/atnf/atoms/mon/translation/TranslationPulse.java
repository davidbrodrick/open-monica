//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.translation.Translation;

/**
 * Outputs True for a fixed mark period when triggered and then outputs False
 * for at least the specified space period irrespective of input. This sequence
 * is triggered by looking at the input as a boolean (if the input is a Number
 * it will be cast to an integer and interpreted as value of zero means false,
 * with any other value meaning true).
 * 
 * It requires two arguments:
 * <ol>
 * <li> <b>Mark Period:</b> The minimum period (in seconds) to maintain the
 * mark output.
 * <li> <b>Space Period:</b> The minimum period (in seconds) to maintain the
 * space output.
 * </ol>
 * 
 * @author David Brodrick
 */
public class TranslationPulse extends Translation {
  /** The period to pulse for. */
  RelTime itsMarkPeriod;

  /** The minimum period between rising edge of consecutive pulses. */
  RelTime itsSpacePeriod;

  /** The time we last pulsed. */
  AbsTime itsLastPulse = null;

  /** Constructor. */
  public TranslationPulse(PointDescription parent, String[] init) {
    super(parent, init);
    try {
      itsMarkPeriod = RelTime.factory((long) (Double.parseDouble(init[0]) * 1000000));
      itsSpacePeriod = RelTime.factory((long) (Double.parseDouble(init[1]) * 1000000));
    } catch (Exception e) {
      MonitorMap.logger.error("TranslationPulse for " + itsParent.getFullName() + ": " + e);
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
      if (data != null && data.getData() != null) {
        Object rawinput = data.getData();
        if (rawinput instanceof Boolean) {
          output = (Boolean) rawinput;
        } else if (rawinput instanceof Number) {
          int intval = ((Number) rawinput).intValue();
          if (intval == 0) {
            output = new Boolean(false);
          } else {
            output = new Boolean(true);
          }
        } else {
          MonitorMap.logger.error("TranslationPulse for " + itsParent.getFullName() + ": Expect Boolean or Number input");
          output = null;
        }

        if (output != null && output.booleanValue()) {
          // Input is now high, triggering a new pulse
          itsLastPulse = now;
        }
      }
    }
    return new PointData(itsParent.getFullName(), now, output);
  }

}
