//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import org.apache.log4j.Logger;
import atnf.atoms.mon.*;

/**
 * This can be used to switch to listen to different points by reading the required source name from another monitor point. It will
 * listen for changes to the required source and subscribe to updates from the point with the designated source name. When that
 * point has a value update this will fire an update to itself with a copy of the new value.
 * 
 * <P>
 * The required arguments are:
 * <ul>
 * <li><b>Discriminator:</b> The full name of the point to read the required source field from. The values must be strings.
 * <li><b>Point Name:</b> The basic name pattern for the points to listen to.
 * </ul>
 * 
 * <P>
 * As an example, say you have points called input.PowerLevel for ten different sources (in1, in2, .., in10), and another point
 * called site.input.CurrentlySelected which has the source name of which point is currently selected (eg. in1) as its value. You
 * could use this translation to define a new point which will take the PowerLevel value of the currently selected input by using
 * arguments like this: <br>
 * <tt>SourceSelector-"site.input.CurrentlySelected""input.PowerLevel"</tt>
 * 
 * @author David Brodrick
 */
public class TranslationSourceSelector extends Translation implements PointListener {
  /** The currently listened to point. */
  private PointDescription itsCurrentPoint;

  /** The name of the discriminator point. */
  private String itsDiscriminatorName;

  /** The pattern/base name for the points to listen to. */
  private String itsPointName;

  /** Timer used to subscribe to listened-to points. */
  private static Timer theirTimer = new Timer();

  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(TranslationSourceSelector.class.getName());

  public TranslationSourceSelector(PointDescription parent, String[] init) {
    super(parent, init);

    if (init.length < 2) {
      throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - require two arguments");
    }

    itsDiscriminatorName = init[0];
    itsPointName = init[1];

    // Start the timer which subscribes us to updates from the points
    theirTimer.schedule(new SubscriptionTask(), 500, 500);
  }

  public PointData translate(PointData pd) {
    // We fire this update to ourself, so just return the input
    return pd;
  }

  /** TimerTask used to subscribe to the discriminator point via timer. */
  private class SubscriptionTask extends TimerTask {
    public void run() {
      PointDescription pd = PointDescription.getPoint(itsDiscriminatorName);
      if (pd == null) {
        if (PointDescription.getPointsCreated()) {
          // All points should have been created by now, but we still didn't find it
          theirLogger.warn("(" + itsParent.getFullName() + ") listened-to point " + itsDiscriminatorName + " was not found");
          cancel();
        }
      } else {
        pd.addPointListener(TranslationSourceSelector.this);
        cancel();
      }
    }
  }

  public void onPointEvent(Object source, PointEvent evt) {
    if (evt.getPointData().getName().equals(itsDiscriminatorName)) {
      String reqsource = (String) evt.getPointData().getData();
      System.err.println("SourceSelector: Update from discriminator");
      if (reqsource != null) {
        if (itsCurrentPoint == null || !itsCurrentPoint.getSource().equals(reqsource)) {
          // We need to change to listen to a new point
          if (itsCurrentPoint != null) {
            // Need to unsubscribe from the old point first
            itsCurrentPoint.removePointListener(this);
          }

          // Subscribe to the new point
          String newpointname = reqsource + "." + itsPointName;
          itsCurrentPoint = PointDescription.getPoint(newpointname);
          if (itsCurrentPoint == null) {
            theirLogger.warn("(" + itsParent.getFullName() + ") Required point " + newpointname + " doesn't exist");
          } else {
            System.err.println("SourceSelector: Subscribed to " + newpointname);
            itsCurrentPoint.addPointListener(this);
          }
        }
      }
    } else if (itsCurrentPoint != null && evt.getPointData().getName().equals(itsCurrentPoint.getFullName())) {
      // This is a new data value for us to use
      PointData newdata = new PointData(evt.getPointData());
      newdata.setName(itsParent.getFullName());
      itsParent.firePointEvent(new PointEvent(this, newdata, true));
    } else {
      theirLogger.warn("(" + itsParent.getFullName() + ") Unexpected update from " + evt.getPointData().getName());
    }
  }
}
