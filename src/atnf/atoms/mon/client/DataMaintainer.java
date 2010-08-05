package atnf.atoms.mon.client;

import java.util.*;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.*;

/**
 * The DataMaintainer provides a simple interface for listeners to subscribe to
 * updates for nominated points.
 * 
 * @author Le Cuong Nguyen, David Brodrick
 */
public class DataMaintainer implements Runnable
{
  // Create a thread to do the polling of the server
  static {
    new Thread(new DataMaintainer(), "DataMaintainer Collector").start();
  }

  /**
   * Comparator to compare PointDescriptions and/or AbsTimes. We need to use
   * this Comparator with the SortedLinkedList class.
   */
  private static class TimeComp implements Comparator
  {
    /** Return a timestamp for any known class type. */
    private long getTimeStamp(Object o)
    {
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
    public int compare(Object o1, Object o2)
    {
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
    public boolean equals(Object o)
    {
      if (o instanceof TimeComp) {
        return true;
      } else {
        return false;
      }
    }
  }

  /** Queue of points sorted by time of next collection. */
  protected static SortedLinkedList theirQueue = new SortedLinkedList(new TimeComp());

  /** All points currently being collected. */
  protected static HashMap<String, PointDescription> theirPoints = new HashMap<String, PointDescription>();

  /** Record of the latest data reported to subscribers for each point. */
  protected static HashMap<String, PointData> theirLastData = new HashMap<String, PointData>();

  public DataMaintainer()
  {
  }

  /** Check if the named point is already being collected. */
  public static boolean alreadyCollecting(String name)
  {
    boolean res;
    synchronized (theirPoints) {
      res = theirPoints.containsKey(name);
    }
    return res;
  }

  /** Schedules a point */
  protected static void addPoint(PointDescription pd)
  {
    synchronized (theirPoints) {
      theirPoints.put(pd.getFullName(), pd);
    }
    synchronized (theirQueue) {
      theirQueue.add(pd);
      theirQueue.notifyAll();
    }
  }

  /** Unschedules a point. */
  public static void removePoint(PointDescription pd)
  {
    String thisname = pd.getFullName();
    synchronized (theirPoints) {
      theirPoints.remove(thisname);
    }
    synchronized (theirQueue) {
      theirQueue.remove(pd);
    }
    synchronized (theirLastData) {
      theirLastData.remove(thisname);
    }
  }

  /** Subscribe the specified listener to updates from all of the given points. */
  public static void subscribe(final Vector<String> points, final PointListener pl)
  {
    // Identify any points we dont have full definitions for yet
    Vector<String> needdefs = new Vector<String>();
    for (int i = 0; i < points.size(); i++) {
      String pname = points.get(i);
      if (PointDescription.getPoint(pname) == null) {
        // We don't already have this point
        needdefs.add(pname);
      }
    }

    // Get the new definitions from the server. This is done by calling thread
    // because caller will assume point definitions are available when call
    // returns.
    if (needdefs.size() > 0) {
      Vector<PointDescription> queryres = null;
      try {
        // Points will get added to static fields when the downloaded
        // definitions are instanciated - so simply requesting the points
        // from
        // the server meets our goals.
        queryres = (MonClientUtil.getServer()).getPoints(needdefs);
      } catch (Exception e) {
        queryres = null;
      }
      if (queryres == null) {
        System.err.println("DataMaintainer.subscribe: COULD NOT SUBSCRIBE - GOT NULL RESULT!");
        return;
      }
    }

    // Do all further IO on new thread
    new Thread() {
      public void run()
      {
        // Fire last collected value or queue for immediate collection
        Vector<String> needvals = new Vector<String>();
        for (int i = 0; i < points.size(); i++) {
          String thisname = points.get(i);
          PointDescription thispoint = PointDescription.getPoint(thisname);
          // Add the listener to this point
          thispoint.addPointListener(pl);
          if (alreadyCollecting(thisname)) {
            PointData lastdata;
            synchronized (theirLastData) {
              lastdata = theirLastData.get(thisname);
            }
            if (lastdata != null) {
              pl.onPointEvent(thispoint, new PointEvent(thispoint, lastdata, false));
            }
          } else {
            needvals.add(thisname);
          }
        }

        if (needvals.size() > 0) {
          // Need to collect values for these points
          Vector<PointData> resdata = null;
          try {
            // Do the actual data collection
            resdata = MonClientUtil.getServer().getData(needvals);
          } catch (Exception e) {
          }
          if (resdata != null) {
            for (int i = 0; i < needvals.size(); i++) {
              String thisname = needvals.get(i);
              PointDescription thispoint = PointDescription.getPoint(thisname);
              PointData thisval = resdata.get(i);
              if (thisval != null) {
                // Got new data for this point okay
                pl.onPointEvent(thispoint, new PointEvent(thispoint, thisval, false));
                synchronized (theirLastData) {
                  // Record new data
                  theirLastData.put(thisname, resdata.get(i));
                }
              } else {
                // Got no data back for this point
                pl.onPointEvent(thispoint, new PointEvent(thispoint, null, false));
              }
            }
          } else {
            // Got no data back at all
            for (int i = 0; i < needvals.size(); i++) {
              PointDescription thispoint = PointDescription.getPoint(needvals.get(i));
              // Inform listeners about the failure to collect data
              pl.onPointEvent(thispoint, new PointEvent(thispoint, null, false));
            }
          }
        }

        // Ensure all of the specified points are being collected
        for (int i = 0; i < points.size(); i++) {
          PointDescription thispoint = PointDescription.getPoint(points.get(i));
          if (thispoint != null) {
            if (!alreadyCollecting(points.get(i))) {
              addPoint(thispoint);
            }
          }
        }
      }
    }.start();
  }

  /** Subscribe the specified listener to updates from the specified point. */
  public static void subscribe(String point, PointListener pl)
  {
    Vector<String> pointnames = new Vector<String>(1);
    pointnames.add(point);
    subscribe(pointnames, pl);
  }

  /** Unsubscribe the listener from all points contained in the vector. */
  public static void unsubscribe(Vector<String> points, PointListener pl)
  {
    for (int i = 0; i < points.size(); i++) {
      unsubscribe(points.get(i), pl);
    }
  }

  /** Unsubscribe the listener from the specified point. */
  public static void unsubscribe(String point, PointListener pl)
  {
    // Get the reference to the full point
    PointDescription thispoint = PointDescription.getPoint(point);
    if (thispoint != null) {
      // Point exists
      thispoint.removePointListener(pl);
      if (thispoint.getNumListeners() == 0) {
        // No listeners left so stop collecting and clear old data
        removePoint(thispoint);
      }
    }
  }

  /** Determine when to next collect the point. */
  public static void updateCollectionTime(PointDescription point)
  {
    // A short interval
    final long shortinterval = 1000000l;

    long nexttime = 0;
    if (point.getNextEpoch() == 0) {
      // Point has never been collected so collect now
      nexttime = AbsTime.factory().getValue();
    } else {
      // Last collection attempt failed so try again shortly
      nexttime = AbsTime.factory().getValue() + shortinterval;
    }

    point.setNextEpoch(nexttime);
  }

  /** Determine when to next collect the point. */
  public static void updateCollectionTime(PointDescription point, PointData data)
  {
    // A short interval
    final long shortinterval = 1000000l;

    long nexttime = 0;
    long lasttime = point.getNextEpoch();
    long datatime = data.getTimestamp().getValue();
    long now = AbsTime.factory().getValue();
    long period = point.getPeriod();

    if (lasttime == 0) {
      // Point has never been collected, so collect now
      nexttime = now;
    } else if (period <= 0) {
      // Point is aperiodic. For now all we can do is try again shortly to see
      // if it has updated yet. A proper publish/subscribe to replace this
      // polling
      // will be the ultimate solution.
      nexttime = now + shortinterval;
    } else if (datatime + period > now) {
      // Schedule for expected next update of the point
      nexttime = datatime + period;
    } else {
      // Try again shortly
      nexttime = now + shortinterval;
    }

    point.setNextEpoch(nexttime);
  }

  /** Main loop for collection thread. */
  public void run()
  {
    while (true) {
      Vector<PointDescription> getpoints = null;
      AbsTime nextTime = AbsTime.factory("ASAP");
      synchronized (theirQueue) {
        try {
          // Wait for notification if no points are waiting
          while (theirQueue.size() == 0) {
            theirQueue.wait();
          }
        } catch (Exception e) {
        }

        // Get the points which are ready for collection
        getpoints = theirQueue.headSet(AbsTime.factory());
      }

      if (getpoints.size() > 0) {
        Vector<String> getnames = new Vector<String>();
        for (int i = 0; i < getpoints.size(); i++) {
          getnames.add(getpoints.get(i).getFullName());
        }
        Vector<PointData> resdata = null;
        try {
          resdata = MonClientUtil.getServer().getData(getnames);
        } catch (Exception e) {
        }
        if (resdata != null) {
          for (int i = 0; i < getpoints.size(); i++) {
            PointDescription pm = getpoints.get(i);

            if (resdata.get(i) != null) {
              // Got data, but check if it is new or repeated
              PointData lastdata;
              synchronized (theirLastData) {
                lastdata = theirLastData.get(getnames.get(i));
              }
              if (lastdata == null || lastdata.getTimestamp().compare(resdata.get(i).getTimestamp()) != 0) {
                // Got data for this point
                synchronized (theirLastData) {
                  theirLastData.put(getnames.get(i), resdata.get(i));
                }
                pm.distributeData(new PointEvent(pm, resdata.get(i), false));
                // System.err.println("DataMaintainer.run: " + getnames.get(i) +
                // " got new data " + resdata.get(i).getTimestamp());
              } else {
                // System.err.println("DataMaintainer.run: " + getnames.get(i) +
                // " got repeated data");
              }
              updateCollectionTime(pm, resdata.get(i));
            } else {
              // Got no data back for this point
              pm.distributeData(new PointEvent(pm, new PointData(pm.getFullName()), false));
              updateCollectionTime(pm);
            }
          }
        } else {
          // Got no data back at all
          for (int i = 0; i < getpoints.size(); i++) {
            PointDescription pm = getpoints.get(i);
            pm.distributeData(new PointEvent(pm, new PointData(pm.getFullName()), false));
            updateCollectionTime(pm);
          }
        }

        // Need to reinsert the points to reschedule collection
        for (int i = 0; i < getpoints.size(); i++) {
          // Ensure point subscription hasn't been cancelled in meantime
          synchronized (theirQueue) {
            if (theirPoints.containsKey(getpoints.get(i).getFullName()) && !theirQueue.contains(getpoints.get(i))) {
              theirQueue.add(getpoints.get(i));
            }
          }
        }
      }

      try {
        synchronized (theirQueue) {
          if (theirQueue.size() > 0) {
            PointDescription pm = (PointDescription) theirQueue.first();
            nextTime = AbsTime.factory(pm.getNextEpoch());// -10000);
          }
        }
        AbsTime timenow = AbsTime.factory();
        if (nextTime.isAfter(timenow)) {
          final RelTime maxsleep = RelTime.factory(10000);
          // We need to wait before we collect the next point.
          // Work out how long we need to wait for
          RelTime waittime = Time.diff(nextTime, timenow);
          if (waittime.getValue() > maxsleep.getValue()) {
            // We don't want to sleep longer than this, in case points
            // have been inserted or removed in the mean time
            maxsleep.sleep();
          } else {
            // Sleep for the correct amount of time
            waittime.sleep();
          }
        }
      } catch (Exception e) {
        System.err.println("PointCollector::run(): " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
}
