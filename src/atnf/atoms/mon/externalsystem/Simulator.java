// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.*;
import atnf.atoms.mon.*;

/**
 * This creates a number of dummy monitor points and assigns them a random number data
 * value when they are scheduled for collection. It has been designed to test the
 * scalability of some aspects of MoniCA.
 * 
 * <P>
 * All updates are achived but this and some other behaviour aspects should be made into
 * options.
 * 
 * The ExternalSystem takes the following arguments:
 * <ul>
 * <li> Number of points to create.
 * <li> Minimum update interval (seconds).
 * <li> Maximum update interval (seconds).
 * </ul>
 * 
 * @author David Brodrick
 */
public class Simulator extends ExternalSystem {
  /** Random number generator. */
  private Random itsRandom = new Random();

  /** Total number of simulators running. */
  private static int theirPopulation = 0;

  public Simulator(String[] args) {
    super("simulator" + theirPopulation);

    int numpoints = Integer.parseInt(args[0]);
    int minseconds = Integer.parseInt(args[1]);
    int maxseconds = Integer.parseInt(args[2]);

    System.out.println("Simulator: Will create " + numpoints + " dummy points");

    for (int i = 0; i < numpoints; i++) {
      String[] names = { "dummy." + i };
      String[] empty = { "-" };
      String[] transaction = { "Generic-\"simulator" + theirPopulation + "\"" };
      String[] translation = { "NumDecimals-\"2\"" };
      String[] arch = { "-" }; // {"COUNTER-1"};

      int period = minseconds + itsRandom.nextInt(maxseconds - minseconds);
      PointDescription pd = PointDescription.factory(names, "Dummy " + i, "Dummy" + i, "", "sim" + theirPopulation, transaction,
              empty, translation, empty, arch, "" + period + "000000", "-1", true);
      pd.populateServerFields();
    }

    // Increase the record of the number of simulators created
    theirPopulation++;
  }

  /** Assign random values to queued monitor points. */
  protected void getData(PointDescription[] points) throws Exception {
    // Fire new data to each point
    for (int i = 0; i < points.length; i++) {
      PointDescription pm = points[i];
      Float newdata = new Float(itsRandom.nextFloat());
      PointData pd = new PointData(pm.getFullName(), newdata);
      PointEvent pe = new PointEvent(pm, pd, true);
      pm.firePointEvent(pe);
    }
  }
}
