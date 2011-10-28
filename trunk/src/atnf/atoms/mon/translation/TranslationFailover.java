//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Takes the value of the highest precedence point from a specified set. If the first ranking point becomes unavailable (ie. if it
 * its value becomes null) then we will start assuming the value of the next ranking valid point, etc.. If a higher ranking point
 * becomes available again then we will revert to using the data from it.
 * 
 * <P>
 * Requires at least one argument (or two to be useful) which is the names of the points to listen to, in order of priority. The $1
 * macro will be substituted for the source name if required.
 * 
 * <P>
 * The code or ExternalSystem generating values for the listened-to points may not necessarily publish an update with a null value
 * when the point/connection breaks, which may cause problems. You should verify the behaviour of the specific points before
 * employing this class.
 * 
 * @author David Brodrick
 */
public class TranslationFailover extends Translation implements PointListener {
  /** The number of points we are listening to. */
  protected int itsNumPoints;

  /** Names of the points we are listening to. */
  protected String[] itsNames;

  /** Reference to the points we are listening to. */
  protected PointDescription[] itsPoints;

  /** Latest updates for the points we are listening to. */
  protected PointData[] itsValues;

  /** Timer used to subscribe to listened-to points. */
  protected static Timer theirTimer = new Timer();

  /** Base-class constructor. */
  public TranslationFailover(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length < 1) {
      throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - require at least one argument");
    }

    try {
      itsNumPoints = init.length;
      itsNames = new String[itsNumPoints];
      itsPoints = new PointDescription[itsNumPoints];
      itsValues = new PointData[itsNumPoints];

      for (int i = 0; i < itsNumPoints; i++) {
        String thisname = init[i];
        // Substitute the name of our source if $1 macro was used
        if (thisname.indexOf("$1") > -1) {
          thisname = MonitorUtils.replaceTok(thisname, parent.getSource());
        }
        itsNames[i] = thisname;
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - error parsing arguments: " + e);
    }

    // Start the timer which subscribes us to updates from the points
    theirTimer.schedule(new SubscriptionTask(), 500, 500);
  }

  /** Just returns the input (which is created by us) */
  public PointData translate(PointData data) {
    return data;
  }

  /** Called when a listened-to point updates. */
  public synchronized void onPointEvent(Object source, PointEvent evt) {
    PointData pd = evt.getPointData();

    // Find the index of the point
    String fullname = pd.getName();
    int thisindex = 0;
    for (; thisindex < itsNumPoints; thisindex++) {
      if (itsNames[thisindex].equals(fullname)) {
        break;
      }
    }
    if (thisindex == itsNumPoints) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.warn("(" + itsParent.getFullName() + ") received unsolicited data from " + fullname);
      return;
    }

    // Record if last update of this point was valid
    boolean lastwasvalid = true;
    if (itsValues[thisindex] == null || itsValues[thisindex].getData() == null) {
      lastwasvalid = false;
    }

    // Save reference to new data
    itsValues[thisindex] = pd;

    // Find the index of the first point which has valid data
    int firstvalid = -1;
    for (int j = 0; j < itsNumPoints; j++) {
      if (itsValues[j] != null && itsValues[j].getData() != null) {
        firstvalid = j;
        break;
      }
    }

    if (firstvalid == thisindex) {
      // This point is the best source of valid data
      PointData res = new PointData(itsParent.getFullName(), pd.getTimestamp(), pd.getData());
      itsParent.firePointEvent(new PointEvent(this, res, true));
    } else if (firstvalid > thisindex && lastwasvalid) {
      // This point was being actively used but has just gone invalid
      // Fire an immediate update using data from the next backup point
      PointData res = new PointData(itsParent.getFullName(), pd.getTimestamp(), itsValues[firstvalid].getData());
      itsParent.firePointEvent(new PointEvent(this, res, true));
    } else if (firstvalid == -1) {
      // No points have valid data
      PointData res = new PointData(itsParent.getFullName(), pd.getTimestamp(), null);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }

  /** TimerTask used to subscribe to monitor point updates via timer. */
  private class SubscriptionTask extends TimerTask {
    public void run() {
      boolean stillmissing = false;

      // Try to find any points that are still missing
      for (int i = 0; i < itsNumPoints; i++) {
        if (itsPoints[i] == null) {
          itsPoints[i] = PointDescription.getPoint(itsNames[i]);
          if (itsPoints[i] == null) {
            stillmissing = true;
            if (PointDescription.getPointsCreated()) {
              // All points should have been created by now
              Logger logger = Logger.getLogger(this.getClass().getName());
              logger.warn("(" + itsParent.getFullName() + ") listened-to point " + itsNames[i] + " was not found");
            }
          } else {
            itsPoints[i].addPointListener(TranslationFailover.this);
          }
        }
      }

      if (!stillmissing) {
        // All points now found and all subscriptions complete
        cancel();
      }
    }
  }
}
