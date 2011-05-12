//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.time.AbsTime;
import atnf.atoms.util.Angle;

/**
 * Subclass of TranslationCalculation which performs processing based on the
 * period of the parent point rather than triggering when listen-to points
 * update.
 * 
 * @author David Brodrick
 */
public class TranslationCalculationTimed extends TranslationCalculation {
  /**
   * Timer used to trigger processing. TODO: Using a single static instance has
   * limited scaling potential. What would a better scheme be?
   */
  protected static Timer theirProcessTimer = new Timer();

  public TranslationCalculationTimed(PointDescription parent, String[] init) {
    super(parent, init);

    // Parent's update interval in ms
    long period = (long) (parent.getPeriod() / 1000);
    theirProcessTimer.schedule(new CalcTask(), period, period);
  }

  /**
   * Always returns false because we base trigger off timer, not off listened-to
   * points.
   */
  protected boolean matchData() {
    return false;
  }

  /** Provide the current input values to the expression parser. */
  protected Object doCalculations() {
    Object res = null;
    boolean missingdata = false;

    for (int i = 0; i < itsNumPoints; i++) {
      if (itsValues[i] == null || itsValues[i].getData() == null) {
        missingdata = true;
        break;
      }
      // Update the value for this variable
      String thisvar = "" + ((char) (('a') + i));
      Object thisval = itsValues[i].getData();
      if (thisval instanceof Boolean) {
        if (((Boolean) thisval).booleanValue()) {
          itsParser.addVariable(thisvar, 1.0);
        } else {
          itsParser.addVariable(thisvar, 0.0);
        }
      } else if (thisval instanceof Angle) {
        itsParser.addVariable(thisvar, ((Angle) thisval).getValue());
      } else {
        itsParser.addVariable(thisvar, thisval);
      }
    }

    if (!missingdata) {
      // Parse the expression using new values
      res = itsParser.getValueAsObject();

      // Check for parse error
      if (itsParser.hasError()) {
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.debug("TranslationCalculationTimed (" + itsParent.getFullName() + ") " + itsParser.getErrorInfo());
      }
    }
    return res;
  }

  /** Called when timer expires. */
  private class CalcTask extends TimerTask {
    public void run() {
      // It's time to perform the calculation and fire an update of the point
      Object resval = doCalculations();
      PointData res = new PointData(itsParent.getFullName(), new AbsTime(), resval);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }
}
