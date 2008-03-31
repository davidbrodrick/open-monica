// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.*;
import java.awt.event.ActionListener;
import java.lang.reflect.*;
import java.net.URL;
import atnf.atoms.mon.datasource.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.archiver.*;

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
      Constructor con = archiverClass.getConstructor(new Class[]{String.class});
      pa = (PointArchiver)(con.newInstance(new Object[]{MonitorConfig.getProperty("ArchiverArg")}));
    } catch (Exception e) {e.printStackTrace();}

    //Initialise all the DataSources
    URL datasourcefile = MonitorMain.class.getClassLoader().getResource("monitor-sources.txt");
    if (datasourcefile==null) {
      System.err.println("ERROR: Failed to find monitor-sources.txt configuration file");
      System.exit(1);
    }
    DataSource.init(datasourcefile.getFile());

    //Create all the points
    URL pointsfile = MonitorMain.class.getClassLoader().getResource("monitor-points.txt");
    if (pointsfile==null) {
      System.err.println("ERROR: Failed to find monitor-points.txt configuration file");
      System.exit(1);
    }
    ArrayList points = PointInteraction.parseFile(pointsfile.getFile());
    for (int i=0; i<points.size(); i++) {
      if (points.get(i) instanceof PointMonitor) {
	//Tell the point which archiver to use - for now there's only one
	((PointMonitor)points.get(i)).setArchiver(pa);
      }
    }

    //Create a thread to update the encryption key occasionally
    new KeyKeeper();

    //Recover all the SavedSetups from the file
    URL setupfile = MonitorMain.class.getClassLoader().getResource("monitor-setups.txt");
    if (setupfile==null) {
      System.err.println("WARNING: Failed to find monitor-setups.txt configuration file");
    } else {
      try {
	Vector setups = SavedSetup.parseFile(setupfile.getFile());
	System.err.println("Recovered " + setups.size() + " SavedSetups from " + setupfile);
	for (int i=0; i<setups.size(); i++) MonitorMap.addSetup((SavedSetup)setups.get(i));
      } catch (Exception e) {
	System.err.println("ERROR: Can't parse saved setup file: " + setupfile);
      }
    }

    //Create the network server interfaces
    new MonitorServerCustom();
    new MonitorServerASCII();

    //Start the data collection
    DataSource.startAll();

    //Start archive thread
    ((Thread)pa).start();

    //We're now underway!
    System.err.println("CHECKPOINT: System is GO!");
  }
   
  public static void main(String[] argv)
  {
    MonitorMain monitorsystem = new MonitorMain();
  }
}
