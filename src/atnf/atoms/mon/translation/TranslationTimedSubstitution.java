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

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Periodically runs the MonitorUtils.doSubstitutions method over the template string given as an argument. This can be used to
 * combine the values of other point names together, etc..
 * 
 * @author David Brodrick
 */
public class TranslationTimedSubstitution extends Translation {
  /** Timer used to trigger processing. */
  protected static Timer theirProcessTimer = new Timer();

  /** The template string for substitutions. */
  protected String itsTemplate;

  /** The last data value produced. */
  protected PointData itsLastValue;

  public TranslationTimedSubstitution(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length != 1) {
      // We require the template string argument
      throw new IllegalArgumentException("Requires template string argument");
    }
    // Replace source name macro
    itsTemplate = init[0].replaceAll("\\$1", itsParent.getSource());

    // Parent's update interval in ms
    long period = (long) (parent.getPeriod() / 1000);
    theirProcessTimer.schedule(new UpdateTask(), period, period);
  }

  /** Just returns the input (which is created by us) */
  public PointData translate(PointData data) {
    return data;
  }

  /** Called when timer expires. */
  private class UpdateTask extends TimerTask {
    public void run() {
      // It's time to perform the calculation and fire an update of the point
      Object resval = MonitorUtils.doSubstitutions(itsTemplate, itsLastValue, itsParent);
      itsLastValue = new PointData(itsParent.getFullName(), new AbsTime(), resval);
      itsParent.firePointEvent(new PointEvent(this, itsLastValue, true));
    }
  }
}
