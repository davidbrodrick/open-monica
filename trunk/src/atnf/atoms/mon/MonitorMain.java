// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.*;
import java.io.*;

import atnf.atoms.mon.externalsystem.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.mon.comms.*;


/**
 * Main class which gets everything running.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: MonitorMain.java,v 1.1 2004/02/19 05:59:37 bro764 Exp bro764 $
 */
public class
MonitorMain
{
  public MonitorMain()
  {
    //Create the archiver to store historical data
    PointArchiver pa = null;
    try {
      Class archiverClass = Class.forName("atnf.atoms.mon.archiver.PointArchiver"+MonitorConfig.getProperty("Archiver"));
      pa = (PointArchiver)(archiverClass.newInstance());
      MonitorMap.setPointArchiver(pa);
    } catch (Exception e) {
      System.err.println("ERROR: While creating PointArchiver:");
      e.printStackTrace();
    }

    //Initialise all the DataSources
    InputStream datasourcefile = MonitorMain.class.getClassLoader().getResourceAsStream("monitor-sources.txt");
    if (datasourcefile==null) {
      System.err.println("ERROR: Failed to find monitor-sources.txt configuration file");
      System.exit(1);
    }
    ExternalSystem.init(new InputStreamReader(datasourcefile));

    //Create all the points
    InputStream pointsfile = MonitorMain.class.getClassLoader().getResourceAsStream("monitor-points.txt");
    if (pointsfile==null) {
      System.err.println("ERROR: Failed to find monitor-points.txt configuration file");
      System.exit(1);
    }
    ArrayList<PointDescription> points = PointDescription.parseFile(new InputStreamReader(pointsfile));
    for (int i=0; i<points.size(); i++) {
      //Populate the appropriate fields for server use
      points.get(i).populateServerFields();
    }

    //Create a thread to update the encryption key occasionally
    new KeyKeeper();

    //Recover all the SavedSetups from the file
    InputStream setupfile = MonitorMain.class.getClassLoader().getResourceAsStream("monitor-setups.txt");
    if (setupfile==null) {
      System.err.println("WARNING: Failed to find monitor-setups.txt configuration file");
    } else {
      try {
        Vector setups = SavedSetup.parseFile(new InputStreamReader(setupfile));
        System.err.println("Recovered " + setups.size() + " SavedSetups from " + setupfile);
        for (int i=0; i<setups.size(); i++) {
          MonitorMap.addSetup((SavedSetup)setups.get(i));
        }
      } catch (Exception e) {
        System.err.println("ERROR: Can't parse saved setup file: " + setupfile);
      }
    }

    //Create the network server interfaces
    new MoniCAServerCustom();
    new MoniCAServerASCII();
    MoniCAIceI.startIceServer();

    //Start the data collection
    ExternalSystem.startAll();

    //Start archive thread
    ((Thread)pa).start();

    //We're now underway!
    System.err.println("CHECKPOINT: System is GO!");
  }
   
  public static void main(String[] argv)
  {
    new MonitorMain();
  }
}
