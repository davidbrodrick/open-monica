// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;

/**
 * 
 * @author David Brodrick
 */
public class TransactionListen extends Transaction implements PointListener {
  /** The number of points we are listening to. */
  protected int itsNumPoints;

  /** Reference to each of the monitor points we listen to. */
  protected PointDescription[] itsPoints = null;

  /** The names of the monitor points we need to listen to. */
  protected String[] itsNames = null;

  /** Timer used to subscribe to listened-to points. */
  protected static Timer theirTimer = new Timer();

  public TransactionListen(PointDescription parent, String[] args) {
    super(parent, args);

    setChannel("NONE"); // Set the channel type

    if (args == null || args.length < 1) {
      throw new IllegalArgumentException("Requires one or more point-name arguments");
    }

    // We got some arguments, so try to make use of them
    itsNumPoints = args.length;
    itsNames = args;
    itsPoints = new PointDescription[itsNumPoints];

    for (int i = 0; i < itsNumPoints; i++) {
      // If the point has $1 source name macro, then expand it
      if (itsNames[i].indexOf("$1") > -1) {
        itsNames[i] = MonitorUtils.replaceTok(itsNames[i], itsParent.getSource());
      }
    }

    // Start the timer which subscribes us to updates from the points
    theirTimer.schedule(new SubscriptionTask(), 500, 500);
  }

  /** Called when a listened-to point updates. */
  public void onPointEvent(Object source, PointEvent evt) {
    // Need to repack the data into a new event object
    PointData pd = evt.getPointData();
    // Check that there's data.. ?
    if (pd == null) {
      return;
    }

    PointEvent evt2 = new PointEvent(this, new PointData(pd), true);

    itsParent.firePointEvent(evt2);
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
            itsPoints[i].addPointListener(TransactionListen.this);
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
