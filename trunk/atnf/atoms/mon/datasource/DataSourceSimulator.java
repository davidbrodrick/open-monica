// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.util.*;
import java.io.*;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.*;

/**
 * This creates a number of dummy monitor points and assigns them a random
 * number data value when they are scheduled for collection. It has been 
 * designed to test the scalability of some aspects of MoniCA.
 *
 * <P>All updates are achived but this and some other behaviour aspects should
 * be made into options.
 *
 * The DataSource takes the following arguments:
 * <ul>
 * <li> Number of points to create.
 * <li> Minimum update interval (seconds).
 * <li> Maximum update interval (seconds).
 * </ul>
 *
 * @author David Brodrick
 **/
public class DataSourceSimulator
extends DataSource
{
  /** Random number generator. */
  private Random itsRandom = new Random();
  
  /** Total number of simulators running. */
  private static int theirPopulation = 0;
  
  public DataSourceSimulator(String[] args)
  {
    super("simulator"+theirPopulation);
    
    int numpoints = Integer.parseInt(args[0]);
    int minseconds = Integer.parseInt(args[1]);
    int maxseconds = Integer.parseInt(args[2]);

    System.out.println("DataSourceSimulator: Will create " + numpoints + " dummy points");
    
    for (int i=0; i<numpoints; i++) {
      String[] names = {"dummy."+i};
      String[] empty = {"-"};
      String[] trans = {"NumDecimals-\"2\""};
      String[] arch = {"-"}; //{"COUNTER-1"};
      
      int period = minseconds + itsRandom.nextInt(maxseconds-minseconds);
      PointMonitor newpoint=PointMonitor.factory(names, "Dummy " + i,
                   "", "", "sim"+theirPopulation, "Generic-\"simulator" + theirPopulation+"\"",
                   trans, "-", arch, "" + period + "000000", true);
    }
    
    //Increase the record of the number of simulators created
    theirPopulation++;
  }

  /** Assign random values to queued monitor points. */
  protected
  void
  getData(Object[] points)
  throws Exception
  {
    if (points==null || points.length==0) {
      //No data to collect
      return;
    }
    
    //Fire new data to each point
    for (int i=0; i<points.length; i++) {
      PointMonitor pm = (PointMonitor)(points[i]);
      Float newdata = new Float(itsRandom.nextFloat());
      PointData pd = new PointData(pm.getName(), pm.getSource(), newdata);
      PointEvent pe = new PointEvent(pm, pd, true);
      pm.firePointEvent(pe);
    }
  }
}
