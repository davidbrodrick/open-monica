//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.*;
import atnf.atoms.mon.transaction.*;
import org.apache.log4j.Logger;

/**
 * ExternalSystem is the base class for objects which bring new information into the system and allow control operations to be
 * written out. They are essentially 'drivers' designed to interact with a specific system external to MoniCA. Specific behavior can
 * be realised by implementing the <i>getData</i> and <i>putData</i> methods.
 * 
 * @author David Brodrick
 * @author Le Cuong Nguyen
 */
public class ExternalSystem implements Runnable {
  /**
   * Comparator to compare PointDescriptions and/or AbsTimes. We need to use this Comparator with the SortedLinkedList class.
   */
  private class TimeComp implements Comparator {
    /** Return a timestamp for any known class type. */
    private long getTimeStamp(Object o) {
      if (o instanceof PointDescription) {
        return ((PointDescription) o).getNextEpoch();
      } else if (o instanceof AbsTime) {
        return ((AbsTime) o).getValue();
      } else if (o == null) {
        return 0;
      } else {
        System.err.println("ExternalSystem: TimeComp: compare: UNKNOWN TYPE (" + o.getClass() + ")");
        return 0;
      }
    }

    /** Compare PointDescriptions and/or AbsTimes. */
    public int compare(Object o1, Object o2) {
      long val1 = getTimeStamp(o1);
      long val2 = getTimeStamp(o2);

      if (val1 > val2) {
        return 1;
      }
      if (val1 < val2) {
        return -1;
      }
      return 0;
    }

    /** Test if equivalent to the given Comparator. */
    public boolean equals(Object o) {
      if (o instanceof TimeComp) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * List of all the points which need to be collected. A SortedLinkedList is used to keep the list sorted in order of time of next
   * collection.
   */
  protected SortedLinkedList itsPoints = new SortedLinkedList(new TimeComp());

  /**
   * Points which are currently being collected asynchronously. We need to keep track of them so we do not schedule them for
   * recollection until the asynchronous callback has been called.
   */
  private Set<PointDescription> itsAsyncPoints = Collections.newSetFromMap(new ConcurrentHashMap<PointDescription, Boolean>());

  /** Allows access to the thread running this collector */
  protected Thread itsThread = null;

  /** A unique "name" used for finding a specific ExternalSystem. */
  protected String itsName = null;

  /** Records if we're currently connected to the remote source. */
  protected boolean itsConnected = false;

  /**
   * Keep track of how many transactions we've done. Each ExternalSystem sub-class should probably zero this field whenever we
   * reconnect to the remote source.
   */
  protected long itsNumTransactions = 0;

  /** Flag to indicate if thread should continue running. */
  protected boolean itsKeepRunning = true;

  /** Static map of all ExternalSystems. */
  protected static HashMap<String, ExternalSystem> theirExternalSystems = new HashMap<String, ExternalSystem>();

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(ExternalSystem.class.getName());

  /** Add a ExternalSystem with the given unique channel description. */
  public static void addExternalSystem(String name, ExternalSystem source) {
    theirExternalSystems.put(name, source);
  }

  /** Get the ExternalSystem with the specified channel description. */
  public static ExternalSystem getExternalSystem(String name) {
    return theirExternalSystems.get(name);
  }

  /** Get a structure containing all ExternalSystem instances. */
  public static Collection<ExternalSystem> getAllExternalSystems() {
    return theirExternalSystems.values();
  }

  public ExternalSystem(String name) {
    itsName = name;
    addExternalSystem(name, this);
  }

  public ExternalSystem() {
  }

  /** Start all ExternalSystem collection threads. */
  public static void startAll() {
    Object[] ds = theirExternalSystems.values().toArray();
    for (int i = 0; i < ds.length; i++) {
      ((ExternalSystem) ds[i]).startCollection();
    }
  }

  /** Stop all ExternalSystem collection threads. */
  public static void stopAll() {
    theirLogger.info("Stopping all ExternalSystems");
    Object[] ds = theirExternalSystems.values().toArray();
    for (int i = 0; i < ds.length; i++) {
      theirLogger.info("Issuing stop request to " + ((ExternalSystem) ds[i]).itsName);
      ((ExternalSystem) ds[i]).stopCollection();
    }
    theirLogger.info("Stop request to all ExternalSystems now complete");
  }

  /** Start the data collection thread. */
  public synchronized void startCollection() {
    itsKeepRunning = true;
    itsThread = new Thread(this, "ExternalSystem " + itsName);
    itsThread.setDaemon(true);
    itsThread.start();
  }

  /**
   * Stop the data collection thread. This method actually just sets a flag to stop the collection and doesn't actually wait until
   * collection has been stopped.
   */
  public void stopCollection() {
    itsKeepRunning = false;
  }

  /**
   * Reconnect to the remote data source. This method should be overridden to achieve the required functionality.
   */
  public synchronized boolean connect() throws Exception {
    itsConnected = true;
    return itsConnected;
  }

  /**
   * Disconnect from the remote data source. This method should be overridden to achieve the required functionality.
   */
  public synchronized void disconnect() throws Exception {
    itsConnected = false;
  }

  /** Return the current connection status. */
  public boolean isConnected() {
    return itsConnected;
  }

  /** Return the unique "name" for this system. */
  public String getName() {
    return itsName;
  }

  /** Get the number of Transactions performed by this ExternalSystem. */
  public long getNumTransactions() {
    synchronized (itsPoints) {
      return itsNumTransactions;
    }
  }

  /** Get the number of points allocated to this ExternalSystem. */
  public int getNumPoints() {
    synchronized (itsPoints) {
      return itsPoints.size();
    }
  }

  /**
   * Add a point to the list of points to collect.
   * 
   * @param p
   *          The point to start monitoring.
   */
  public void addPoint(PointDescription p) {
    synchronized (itsPoints) {
      itsPoints.add(p);
      itsPoints.notifyAll();
    }
  }

  /**
   * Add an array of points to the list of points to collect.
   * 
   * @param v
   *          The points to start monitoring.
   */
  public void addPoints(Object[] v) {
    synchronized (itsPoints) {
      for (int i = 0; i < v.length; i++) {
        itsPoints.add(v[i]);
      }
      itsPoints.notifyAll();
    }
  }

  /**
   * Add a collection of points to the list of points to collect.
   * 
   * @param v
   *          The points to start monitoring.
   */
  public void addPoints(Collection v) {
    addPoints(v.toArray());
  }

  /**
   * Remove the point to the list of points to collect.
   * 
   * @param p
   *          The point to stop monitoring.
   */
  public void removePoint(PointDescription p) {
    synchronized (itsPoints) {
      itsPoints.remove(p);
      itsPoints.notifyAll();
    }
  }

  /**
   * Flag that a polled point is being collected asynchronously and we must await the callback before rescheduling.
   * 
   * @param point
   *          The point to be flagged.
   */
  protected void asynchCollecting(PointDescription point) {
    point.isCollecting(true);
    itsAsyncPoints.add(point);
  }

  /**
   * Reschedule a point which was being collected asynchronously but has now updated.
   * 
   * @param point
   *          The point to be rescheduled.
   */
  protected void asynchReturn(PointDescription point) {
    point.isCollecting(false);
    addPoint(point);
    itsThread.interrupt();
  }

  /** Return any Transactions which are associated with this ExternalSystem. */
  protected Vector<Transaction> getMyTransactions(Transaction[] transactions) {
    Vector<Transaction> match = new Vector<Transaction>(transactions.length);
    for (int i = 0; i < transactions.length; i++) {
      if (transactions[i].getChannel() != null && transactions[i].getChannel().equals(itsName)) {
        match.add(transactions[i]);
      }
    }
    return match;
  }

  /**
   * This method does the real work. Sub-classes should implement this method. It needs to fire PointEvent's for each monitor point
   * once the new data has been collected.
   * 
   * @param points
   *          The points that need collecting right now.
   */
  protected void getData(PointDescription[] points) throws Exception {
    theirLogger.warn("(" + itsName + "): Received unsupported monitor requests: stopping collection");
    stopCollection();
  }

  /**
   * Write a value to the device.
   * 
   * @param desc
   *          The point that requires the write operation.
   * @param pd
   *          The value that needs to be written.
   * @throws Exception
   */
  public void putData(PointDescription desc, PointData pd) throws Exception {
    theirLogger.warn("(" + itsName + "): Received unsupported control request from " + desc.getFullName());
  }

  /**
   * Initialise all the ExternalSystems declared in a file.
   * 
   * @param fileName
   *          The file to parse for ExternalSystem declarations.
   */
  public static void init(Reader sourcefile) {
    try {
      String[] lines = MonitorUtils.parseFile(sourcefile);
      if (lines != null) {
        for (int i = 0; i < lines.length; i++) {
          try {
            theirLogger.debug("Creating ExternalSystem from definition \"" + lines[i] + "\"");
            StringTokenizer tok = new StringTokenizer(lines[i]);
            String className = tok.nextToken();
            String[] classArgs = null;
            if (tok.countTokens() > 0) {
              // Split the arguments into an array at each colon
              classArgs = tok.nextToken().split(":");
            }
            Class<?> newes;
            try {
              // Might be fully qualified name
              newes = Class.forName(className);
            } catch (Exception e) {
              // Not fully qualified - so try default package
              newes = Class.forName("atnf.atoms.mon.externalsystem." + className);
            }
            Constructor<?> con = newes.getConstructor(new Class[] { String[].class });
            try {
              con.newInstance(new Object[] { classArgs });
            } catch (InvocationTargetException ite) {
              ite.getCause().printStackTrace();
            }
          } catch (Exception f) {
            theirLogger.error("Cannot Initialise \"" + lines[i] + "\" defined on line " + (i + 1) + ": " + f);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      theirLogger.error("Cannot Initialise External Systems!");
    }
  }

  /** Main loop for the point scheduling/collection thread. */
  public void run() {
    while (itsKeepRunning) {
      // /If we're not connected, try to reconnect
      if (!itsConnected) {
        try {
          connect();
        } catch (Exception e) {
          itsConnected = false;
        }
      }

      // We're connected, need to determine which points need collecting
      Vector<PointDescription> thesepoints = null;
      synchronized (itsPoints) {
        try {
          // Wait for notification if there are no points
          while (itsPoints.isEmpty()) {
            itsPoints.wait();
          }
        } catch (Exception e) {
          continue;
        }

        // Calculate the latest epoch we're prepared to collect now
        AbsTime cutoff = AbsTime.factory();
        cutoff.add(50000); // Fudge factor for better efficiency

        // Get the set of all points within the time bracket
        thesepoints = itsPoints.headSet(cutoff);
      }

      if (!thesepoints.isEmpty()) {
        PointDescription[] parray = new PointDescription[thesepoints.size()];
        for (int i = 0; i < thesepoints.size(); i++) {
          parray[i] = thesepoints.get(i);
        }
        if (itsConnected) {
          try {
            // Call the sub-class specific method to do the real work
            getData(parray);
          } catch (Exception e) {
            // e.printStackTrace();
            theirLogger.error("(" + itsName + ") " + e);
            itsConnected = false;
          }
        } else {
          // Points are scheduled for collection but we're not connected.
          // Fire null-data events for those points since old data is stale
          for (int i = 0; i < parray.length; i++) {
            PointDescription pm = (PointDescription) parray[i];
            pm.firePointEvent(new PointEvent(this, new PointData(pm.getFullName()), true));
          }
          // Throw in a brief sleep to stop fast reconnection loops
          try {
            final RelTime connectdelay = RelTime.factory(1000000);
            connectdelay.sleep();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        // Insert the points back into our list
        synchronized (itsPoints) {
          for (int i = 0; i < parray.length; i++) {
            // We can only reschedule points which aren't being collected asynchronously
            if (!itsAsyncPoints.remove(parray[i])) {
              addPoint(parray[i]);
            }
          }
        }
      }

      // We may need to wait before we collect the next point.
      try {
        PointDescription headpoint = (PointDescription) itsPoints.first();
        if (headpoint != null) {
          AbsTime nextTime = headpoint.getNextEpoch_AbsTime();
          AbsTime timenow = AbsTime.factory();
          if (nextTime.isAfter(timenow)) {
            RelTime waittime = Time.diff(nextTime, timenow);
            waittime.sleep();
          }
        }
      } catch (Exception e) {
        // Sleep was interrupted because asych point needs rescheduling
      }
    }
  }
}
