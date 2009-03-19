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

/**
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class
TransactionListen
extends Transaction
implements PointListener, ActionListener
{
  /** Strings describing the expected arguments. */
  protected static String itsArgs[] = new String[]{"Transaction - Listen",
  "foo", "Arg1", "mp", "Arg2", "mp"};

  /** Reference to each of the monitor points we listen to. */
  protected PointInteraction[] itsPoints = null;

  /** The names of the monitor points we need to listen to. */
  protected String[] itsNames = null;

  /** Timer used when listened-to points haven't been created yet. */
  protected MonitorTimer itsTimer = null;

  public TransactionListen(PointInteraction parent, String specifics)
  {
    //Call parent-class constructor
    super(parent, specifics);

    setChannel("NONE"); //Set the channel type

    //Get the individual monitor point arguments from the argument
    String[] args = MonitorUtils.tokToStringArray(specifics);

    if (args==null || args.length<1) {
      System.err.println("ERROR: TransactionListen for " + parent.getName());
      System.err.println("EXPECT 1 OR MORE POINT-NAME ARGUMENTS!");
    } else {
      //We got some arguments, so try to make use of them
      itsNames = args;
      itsPoints = new PointInteraction[args.length];
      for (int i=0; i<args.length; i++) {
        //If the point has $1 source name macro, then expand it
        if (args[i].indexOf("$1") > -1) {
          args[i] = MonitorUtils.replaceTok(args[i], itsParent.getSource());
        }

        //Check that the point exists for the named source
        itsPoints[i] = MonitorMap.getPointMonitor(args[i]);
        if (itsPoints[i]==null) {
          //Either point name is wrong or point hasn't been created yet
          //Start timer which will try again shortly
          if (itsTimer==null) {
            itsTimer = new MonitorTimer(20, this);
            itsTimer.start();
          }
        } else {
          //Point already exists, we can subscribe to it now
          itsPoints[i].addPointListener(this);
        }
      }
    }
  }

  public static String[] getArgs()
  {
     return itsArgs;
  }


  /** Called when a listened-to point updates. */
  public
  void
  onPointEvent(Object source, PointEvent evt)
  {
    //System.err.println("TransactionListen: " + itsParent.getName() + " GOT " + evt);

    //Need to repack the data into a new event object
    PointData pd1 = evt.getPointData();

    //Check that there's data.. ?
    if (pd1==null) return;

    //PointData pd2 = new PointData(itsParent.getName(), itsParent.getSource(),
    PointData pd2 = new PointData(pd1.getName(), pd1.getSource(),
				  pd1.getTimestamp(),
				  pd1.getRaw(), pd1.getData());
    PointEvent evt2 = new PointEvent(this, pd2, true);

    itsParent.firePointEvent(evt2);
  }


  /** Only used to subscribe to main monitor point via timer. */
  public
  void
  actionPerformed(java.awt.event.ActionEvent evt)
  {
    boolean stillmissing = false;
    //Try to fill out any point names that are still missing
    for (int i=0; i<itsPoints.length; i++) {
      if (itsPoints[i]==null) {
        itsPoints[i] = MonitorMap.getPointMonitor(itsNames[i]);
        if (itsPoints[i]==null) {
          //Still couldn't find the point, perhaps it doesn't exist?!
          stillmissing = true;
          System.err.println("WARNING: TransactionListen for " + itsParent.getName());
          System.err.println("LISTENED-TO POINT " + itsNames[i] + " DOESN'T EXIST?!");
        } else {
          itsPoints[i].addPointListener(this);
        }
      }
    }

    if (!stillmissing) {
      //All points now found and all subscriptions complete
      itsTimer.stop();
      itsTimer = null;
    }
  }

}
