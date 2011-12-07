//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointDescription;

/**
 * Integrate every positive update of the input and reset once per day at the
 * specified time in the specified timezone.<BR>
 * 
 * For instance, this was initially implemented for the Parkes Telescope weather
 * station, which publishes the rainfall over the last 20 seconds. This
 * Translation is able to accumulate the rain tips over the course of the day
 * and reset the rain gauge at 9:01 local time.
 * 
 * <P>
 * The two init arguments for this are <tt>"09:01""Australia/Sydney"</tt>
 * 
 * @author David Brodrick
 */
public class TranslationDailyIntegratorPosOnly extends TranslationDailyIntegrator {
  public TranslationDailyIntegratorPosOnly(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /**
   * Check whether this input should be used in the integral (ie, if it is >=0).
   */
  protected boolean useThisData(double newval) {
    if (newval >= 0) {
      return true;
    } else {
      return false;
    }
  }
}
