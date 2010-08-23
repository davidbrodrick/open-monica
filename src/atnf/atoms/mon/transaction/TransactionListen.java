// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import java.awt.event.*;
import javax.swing.Timer;

import org.apache.log4j.Logger;

/**
 * 
 * @author David Brodrick
 */
public class TransactionListen extends Transaction implements PointListener, ActionListener
{
  /** Reference to each of the monitor points we listen to. */
  protected PointDescription[] itsPoints = null;

  /** The names of the monitor points we need to listen to. */
  protected String[] itsNames = null;

  /** Timer used when listened-to points haven't been created yet. */
  protected Timer itsTimer = null;

  public TransactionListen(PointDescription parent, String[] args)
  {
    super(parent, args);

    setChannel("NONE"); // Set the channel type

    if (args == null || args.length < 1) {
      throw new IllegalArgumentException("Requires one or more point-name arguments");
    } else {
      // We got some arguments, so try to make use of them
      itsNames = args;
      itsPoints = new PointDescription[args.length];
      for (int i = 0; i < args.length; i++) {
        // If the point has $1 source name macro, then expand it
        if (args[i].indexOf("$1") > -1) {
          args[i] = MonitorUtils.replaceTok(args[i], itsParent.getSource());
        }

        // Check that the point exists for the named source
        itsPoints[i] = PointDescription.getPoint(args[i]);
        if (itsPoints[i] == null) {
          // Either point name is wrong or point hasn't been created yet
          // Start timer which will try again shortly
          if (itsTimer == null) {
            itsTimer = new Timer(500, this);
            itsTimer.start();
          }
        } else {
          // Point already exists, we can subscribe to it now
          itsPoints[i].addPointListener(this);
        }
      }
    }
  }

  /** Called when a listened-to point updates. */
  public void onPointEvent(Object source, PointEvent evt)
  {
    // Need to repack the data into a new event object
    PointData pd = evt.getPointData();
    // Check that there's data.. ?
    if (pd == null) {
      return;
    }

    PointEvent evt2 = new PointEvent(this, new PointData(pd), true);

    itsParent.firePointEvent(evt2);
  }

  /** Only used to subscribe to main monitor point via timer. */
  public void actionPerformed(java.awt.event.ActionEvent evt)
  {
    boolean stillmissing = false;
    // Try to fill out any point names that are still missing
    for (int i = 0; i < itsPoints.length; i++) {
      if (itsPoints[i] == null) {
        itsPoints[i] = PointDescription.getPoint(itsNames[i]);
        if (itsPoints[i] == null) {
          // Still couldn't find the point, perhaps it doesn't exist?!
          stillmissing = true;
          Logger logger = Logger.getLogger(this.getClass().getName());
          logger.warn("(" + itsParent.getFullName() + ") listened-to point " + itsNames[i] + " doesn't exist yet");
        } else {
          itsPoints[i].addPointListener(this);
        }
      }
    }

    if (!stillmissing) {
      // All points now found and all subscriptions complete
      itsTimer.stop();
      itsTimer = null;
    }
  }

}
