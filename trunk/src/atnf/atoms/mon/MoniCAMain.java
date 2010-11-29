// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import org.apache.log4j.Logger;

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
  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(MoniCAMain.class.getName());
  
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
   * Stop MoniCA and free resources.
   * @return True if successfully stopped, False if a problem was encountered.
   */
  public static boolean stop() {
    theirLogger.info("Stopping MoniCA Server..");
    System.exit(0);
    return true;
  }
  
  /**
   * Start MoniCA.
   * @return True if successfully stopped, False if a problem was encountered.
   */
  public static boolean start() {
    theirLogger.info("Starting MoniCA Server..");
    
    // Create the archiver to store historical data
    PointArchiver pa = null;
    try {
      Class archiverClass = Class.forName("atnf.atoms.mon.archiver.PointArchiver" + MonitorConfig.getProperty("Archiver"));
      pa = (PointArchiver) (archiverClass.newInstance());
      PointArchiver.setPointArchiver(pa);
    } catch (Exception e) {
      theirLogger.fatal("While creating PointArchiver:" + e);
      return false;
    }

    // Initialise all the ExternalSystems
    InputStream exsystemsfile = MoniCAMain.class.getClassLoader().getResourceAsStream("monitor-sources.txt");
    if (exsystemsfile == null) {
      theirLogger.fatal("Failed to find monitor-sources.txt configuration resource");
      return false;
    }
    ExternalSystem.init(new InputStreamReader(exsystemsfile));

    // Create all the points
    InputStream pointsfile = MoniCAMain.class.getClassLoader().getResourceAsStream("monitor-points.txt");
    if (pointsfile == null) {
      theirLogger.fatal("Failed to find monitor-points.txt configuration resource");
      return false;
    }
    ArrayList<PointDescription> points = PointDescription.parseFile(new InputStreamReader(pointsfile));
    for (int i = 0; i < points.size(); i++) {
      // Populate the appropriate fields for server use
      points.get(i).populateServerFields();
    }
    PointDescription.setPointsCreated();

    // Create a thread to update the encryption key occasionally
    new KeyKeeper();

    // Recover all the SavedSetups from the file
    InputStream setupfile = MoniCAMain.class.getClassLoader().getResourceAsStream("monitor-setups.txt");
    if (setupfile == null) {
      theirLogger.warn("Failed to find monitor-setups.txt configuration resource");
    } else {
      try {
        Vector setups = SavedSetup.parseFile(new InputStreamReader(setupfile));
        theirLogger.debug("Recovered " + setups.size() + " SavedSetups from " + setupfile);
        for (int i = 0; i < setups.size(); i++) {
          SavedSetup.addSetup((SavedSetup) setups.get(i));
        }
      } catch (Exception e) {
        theirLogger.fatal("Can't parse saved setup file: " + setupfile);
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
      theirLogger.info("MoniCA System is GO!");
    } else {
      theirLogger.fatal("Failed to Start MoniCA System!");
    }
  }
}