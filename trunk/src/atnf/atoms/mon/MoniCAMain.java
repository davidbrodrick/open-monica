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
 * Top-level class with methods for starting and stopping MoniCA.
 * 
 * @author David Brodrick
 */
public class MoniCAMain {
  /** The ICE Adapter to be used for initialising the ICE server interface.
   * This is optional, if left as null then a new adapter will be created using
   * configuration parameters. */
  private static Ice.ObjectAdapter theirICEAdapter = null;
  
  /** Specify the ICE Communicator to be used by the ICE server interface. */
  public static void setICEAdapter(Ice.ObjectAdapter a)
  {
    theirICEAdapter = a;
  }
  
  /**
   * Start MoniCA.
   * @return True if successfully stopped, False if a problem was encountered.
   */
  public static boolean start() {
    // Create the archiver to store historical data
    PointArchiver pa = null;
    try {
      Class archiverClass = Class.forName("atnf.atoms.mon.archiver.PointArchiver" + MonitorConfig.getProperty("Archiver"));
      pa = (PointArchiver) (archiverClass.newInstance());
      MonitorMap.setPointArchiver(pa);
    } catch (Exception e) {
      System.err.println("ERROR: While creating PointArchiver:");
      return false;
    }

    // Initialise all the ExternalSystems
    InputStream exsystemsfile = MoniCAMain.class.getClassLoader().getResourceAsStream("monitor-sources.txt");
    if (exsystemsfile == null) {
      System.err.println("ERROR: Failed to find monitor-sources.txt configuration file");
      return false;
    }
    ExternalSystem.init(new InputStreamReader(exsystemsfile));

    // Create all the points
    InputStream pointsfile = MoniCAMain.class.getClassLoader().getResourceAsStream("monitor-points.txt");
    if (pointsfile == null) {
      System.err.println("ERROR: Failed to find monitor-points.txt configuration file");
      return false;
    }
    ArrayList<PointDescription> points = PointDescription.parseFile(new InputStreamReader(pointsfile));
    for (int i = 0; i < points.size(); i++) {
      // Populate the appropriate fields for server use
      points.get(i).populateServerFields();
    }

    // Create a thread to update the encryption key occasionally
    new KeyKeeper();

    // Recover all the SavedSetups from the file
    InputStream setupfile = MoniCAMain.class.getClassLoader().getResourceAsStream("monitor-setups.txt");
    if (setupfile == null) {
      System.err.println("WARNING: Failed to find monitor-setups.txt configuration file");
    } else {
      try {
        Vector setups = SavedSetup.parseFile(new InputStreamReader(setupfile));
        System.err.println("Recovered " + setups.size() + " SavedSetups from " + setupfile);
        for (int i = 0; i < setups.size(); i++) {
          MonitorMap.addSetup((SavedSetup) setups.get(i));
        }
      } catch (Exception e) {
        System.err.println("ERROR: Can't parse saved setup file: " + setupfile);
        return false;
      }
    }

    // Start the data collection
    ExternalSystem.startAll();

    // Start archive thread
    ((Thread) pa).start();

    return true;
  }

  /**
   * Open the network server interfaces.
   */
  public static void openInterfaces() {
    new MoniCAServerASCII();
    if (theirICEAdapter==null) {
      MoniCAIceI.startIceServer();
    } else {
      MoniCAIceI.startIceServer(theirICEAdapter);
    }    
  }
  
  /**
   * Close the network server interfaces.
   */
  public static void closeInterfaces() {
    MoniCAServerASCII.stopAll();
    MoniCAIceI.stopIceServer();
  }

  public static void main(String[] argv) {
    // Start the system
    if (MoniCAMain.start()) {
      // Open the network server interfaces
      MoniCAMain.openInterfaces();
      // We're now underway!
      System.err.println("CHECKPOINT: MoniCA System is GO!");
    } else {
      System.err.println("ERROR: Failed to Start MoniCA System!");
    }
  }
}
