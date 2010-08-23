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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Generic base-class for Translations which need to listen to the values of one or more
 * other monitor points. It provides machinery for identifying the source of data events
 * and matching and storing the data from the different points.
 * 
 * <P>
 * This class supercedes TranslationDualListen. This class subscribes to updates from the
 * various points directly and does not need to be used with a TransactionListen.
 * <P>
 * The first constructor <i>init</i> argument needs to specify the number of monitor
 * points which are being listened to. The next arguments must be the names of the points
 * being listened to. Other, sub-class specific arguments can then follow. After the super
 * constructor has been called the index of the first sub-class argument can be obtained
 * by <i>itsNumPoints+1</i>.
 * <P>
 * Sub-classes should implement the abstract <i>doCalculations</i> method in order to
 * achieve the desired functionality. This takes an array of inputs which contain the same
 * order you declared them as arguments.
 * <P>
 * The <i>matchData</i> method is responsible for indicating when we have the appropriate
 * data to call the <i>doCalculations</i> method. This base class performs the
 * calculation whenever we have new values for all points since the last calculation was
 * performed. Sub-classes can implement a <i>matchData</i> method with more specialised
 * behavior, such as checking that both data have identical timestamps before allowing an
 * output value to be calculated.
 * 
 * @author David Brodrick
 */
public abstract class TranslationListener extends Translation implements PointListener, ActionListener
{
  /** The number of points we are listening to. */
  protected int itsNumPoints;

  /** Names of the points we are listening to. */
  protected String[] itsNames;

  /** Reference to the points we are listening to. */
  protected PointDescription[] itsPoints;

  /** Latest updates for the points we are listening to. */
  protected PointData[] itsValues;

  /** Timer used when listened-to points haven't been created yet. */
  protected Timer itsTimer = null;

  protected static String[] itsArgs = new String[] { "Listener", "Listens to two other points", "NumPoints", "Integer",
      "MonitorPoint 1", "String", "MonitorPoint N", "String" };

  /** Base-class constructor. */
  public TranslationListener(PointDescription parent, String[] init)
  {
    super(parent, init);
    if (init.length < 1) {
      throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - require at least one argument");
    }

    try {
      itsNumPoints = Integer.parseInt(init[0]);
      if (init.length < itsNumPoints + 1) {
        throw new IllegalArgumentException("(" + itsParent.getFullName() + ") - insufficient arguments provided");
      }
      itsNames = new String[itsNumPoints];
      itsPoints = new PointDescription[itsNumPoints];
      itsValues = new PointData[itsNumPoints];

      for (int i = 0; i < itsNumPoints; i++) {
        String thisname = init[i + 1];
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
    itsTimer = new Timer(500, this);
    itsTimer.start();
  }

  /** Just returns the input (which is created by us) */
  public PointData translate(PointData data)
  {
    return data;
  }

  /**
   * Check if now is an appropriate time to recalculate the output.
   * 
   * <P>
   * This method will be checked each time one of the input values updates.
   * 
   * <P>
   * The behaviour can be specialised by sub-classes. The default behaviour of this
   * super-class is to recalculate every time any of the inputs update, unless any of the
   * inputs has never been set yet.
   * 
   * @return <tt>True</tt> if we can now calculate an output value, <tt>False</tt> if
   * the current data don't enable us to perform the calculation.
   */
  protected boolean matchData()
  {
    for (int i = 0; i < itsNumPoints; i++) {
      if (itsValues[i] == null || itsValues[i].getData() == null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Abstract method which must be implemented by sub-classes. This performs the
   * manipulation of the input argument data required to produce the quantity of interest.
   * 
   * @return Arbitrary combination of the input values
   */
  protected abstract Object doCalculations();

  /** Called when a listened-to point updates. */
  public void onPointEvent(Object source, PointEvent evt)
  {
    PointData pd = evt.getPointData();
    // Check that there's data.. ?
    if (pd == null || pd.getData() == null) {
      return;
    }

    // Find the index of the point
    String fullname = pd.getName();
    int i = 0;
    for (; i < itsNumPoints; i++) {
      if (itsNames[i].equals(fullname)) {
        break;
      }
    }

    // Ensure we point the appropriate point
    if (i == itsNumPoints) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.warn("(" + itsParent.getFullName() + ") Received unsolicited data from: " + fullname);
      return;
    }

    // Everything looks good
    itsValues[i] = pd;

    // Check whether now is an appropriate time to recalculate output
    if (matchData()) {
      // Recalculate output and fire update event
      Object resval = doCalculations();
      PointData res = new PointData(itsParent.getFullName(), new AbsTime(), resval);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }

  /** Only used to subscribe to monitor point updates via timer. */
  public void actionPerformed(ActionEvent evt)
  {
    if (evt.getSource() == itsTimer) {
      boolean stillmissing = false;

      // Try to find any points that are still missing
      for (int i = 0; i < itsNumPoints; i++) {
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

  public static String[] getArgs()
  {
    return itsArgs;
  }
}
