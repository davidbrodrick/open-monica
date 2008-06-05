//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.apps;

import atnf.atoms.mon.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.time.*;
import java.util.Vector;

/** A class to illustrate how to integrate the new monitor system
 * into a Java application.
 * @author David Brodrick 20060213 */
public class TestApp
implements PointListener 
{

  /** Constructor. Firstly demonstrates how to do an archive query by
   * calling "printArchive" with the name of the first monitor point,
   * and then demonstrates real-time update mode by creating a
   * DataMaintainer. */
  public TestApp(Vector points) {
    //First print the archival data
    printArchive((String)points.get(0));

    //Then start in real-time mode
    System.out.println("SUBSCRIBING TO REAL-TIME UPDATES:");
    DataMaintainer d = new DataMaintainer();
    d.subscribe(points, this);
  }


  /** This is called by the DataMaintainer whenever one of the monitor
   * points that you are subscribed to has an updated value available. */
  public void onPointEvent(Object source, PointEvent evt)
  {
    PointData pd = evt.getPointData();
    //Just print the name, timestamp and latest value
    System.out.print(pd.getSource() + "." + pd.getName());
    System.out.print("\t(" + pd.getTimestamp().toString(AbsTime.Format.UTC_STRING) + ")");
    System.out.println("\t" + pd.getData());
  }


  /** Print the last 5 minutes worth of data from the archive for the
   * specified monitor point. */
  public void printArchive(String pointname) {
    //Get timestamps for both "now" and the request start time
    AbsTime now = new AbsTime();
    RelTime timespan = RelTime.factory(-300000000);
    AbsTime starttime = now.add(timespan);
    //Get a reference to the connection back to the server
    MonitorClientCustom server = MonClientUtil.getServer();
    //Ask for the archived data for the specified monitor point
    Vector data = server.getPointData(pointname, starttime, now);
    if (data!=null) {
      System.out.println("ARCHIVE QUERY RESULTS FOR " + pointname);
      //Print each returned data point
      for (int i=0; i<data.size(); i++) {
        PointData pd = (PointData)data.get(i);
        System.out.print(pd.getSource() + "." + pd.getName());
        System.out.print("\t(" + pd.getTimestamp().toString(AbsTime.Format.UTC_STRING) + ")");
        System.out.println("\t" + pd.getData());
      }
    }
  }


  /** Application start point - just creates a new "TestApp" with a set
   * of monitor points to subscribe to. */
  public static final void main(String[] args) {
    if (args.length<1) {
      System.err.println("USAGE: Requires a monitor point name argument!");
      System.exit(1);
    }
    //Specify the full names of as many points as you like here
    Vector points = new Vector();
    points.add(args[0]);

    TestApp t = new TestApp(points);
  }
}
