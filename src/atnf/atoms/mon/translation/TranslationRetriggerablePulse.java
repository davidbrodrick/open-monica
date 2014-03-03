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
 * This works as a basic pulse extender. This input is interpreted as boolean and a True state will trigger the output to remain
 * high for the specified period. If the input remains high or goes high again then the timer will be reset, retriggering the
 * extended pulse. This is different to the Pulse translation in that Pulse does not retrigger.
 * 
 * It requires a single argument:
 * <ol>
 * <li><b>Pulse Period:</b> The minimum period (in seconds) to maintain the mark output after the latest trigger.
 * </ol>
 * 
 * @author David Brodrick
 */
public class TranslationRetriggerablePulse extends Translation {
  /** The period to pulse for. */
  RelTime itsPulsePeriod;

  /** The time we last triggered. */
  AbsTime itsLastTrigger = null;

  /** Constructor. */
  public TranslationRetriggerablePulse(PointDescription parent, String[] init) {
    super(parent, init);
    try {
      itsPulsePeriod = RelTime.factory((long) (Double.parseDouble(init[0]) * 1000000));
    } catch (Exception e) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): While parsing constructor string arguments: " + e);
    }
  }

  /** Determine output based on input and timing constraints. */
  public PointData translate(PointData data) {
    try {
      Boolean output = false;
      AbsTime now = data.getTimestamp();
      // Check for new trigger
      if (MonitorUtils.parseAsBoolean(data.getData())) {
        itsLastTrigger = now;
      }
      // Check if need to output a high value
      if (itsLastTrigger != null && itsLastTrigger.add(itsPulsePeriod).compare(now) > 0) {
        output = true;
      }
      return new PointData(itsParent.getFullName(), now, output);
    } catch (Exception e) {
      return null;
    }
  }

}
