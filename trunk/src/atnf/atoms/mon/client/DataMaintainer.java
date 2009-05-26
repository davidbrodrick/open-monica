package atnf.atoms.mon.client;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Class: DataMaintainer Description: handles client-side collection and
 * buffering of Points
 * 
 * @author Le Cuong Nguyen
 */

public class DataMaintainer implements Runnable, PointListener {
  protected ArrayList<PointDescription> itsPoints = new ArrayList<PointDescription>();

  protected static Hashtable<String,PointDescription> itsNames = new Hashtable<String,PointDescription>();

  protected static Hashtable<String,PointData> itsBuffer = new Hashtable<String,PointData>();

  protected static Thread itsCollector;

  protected static DataMaintainer itsMain;

  protected static boolean itsRunning = true;
  static {
    itsMain = new DataMaintainer();
    itsCollector = new Thread(itsMain);
    itsCollector.start();
  }

  public DataMaintainer() {
  }

  // Data is collected
  public void onPointEvent(Object source, PointEvent evt) {
    if (evt != null && !evt.isRaw()) {
      if (evt.getPointData() != null) {
        itsBuffer.put(evt.getPointData().getName(), evt.getPointData());
      }
      // Schedule again
      addPoint((PointDescription) source);
    } else if (evt == null) {
      // Schedule again
      addPoint((PointDescription) source);
    }
  }

  /** Schedules a point */
  protected void addPoint(PointDescription pm) {
    long nextExec = pm.getNextEpoch();
    synchronized (itsPoints) {
      for (int i = 0; i < itsPoints.size(); i++) {
        if (((PointDescription) itsPoints.get(i)).getNextEpoch() >= nextExec) {
          itsPoints.add(i, pm);
          itsPoints.notifyAll();
          return;
        }
      }
      itsPoints.add(pm);
      itsPoints.notifyAll();
    }
  }

  public void addPoint(PointDescription pm, boolean init) {
    if (init) {
      pm.addPointListener(this);
    }
    addPoint(pm);
  }

  /** Unschedules a point. */
  public void removePointMonitor(PointDescription pm) {
    synchronized (itsPoints) {
      itsPoints.remove(pm);
      itsPoints.notifyAll();
    }
  }

  public static void subscribe(Vector<String> points, PointListener pl) {
    Vector<String> realarg = new Vector<String>();
    for (int i = 0; i < points.size(); i++) {
      String pname = (String) points.get(i);
      if (itsNames.get(pname) == null) {
        // We don't already have this point
        realarg.add(pname);
      }
    }

    if (realarg.size() > 0) {
      // We need to go ask the server for these points
      Vector<PointDescription> res = null;
      try {
        res = (MonClientUtil.getServer()).getPoints(realarg);
      } catch (Exception e) {
      }
      if (res == null) {
        System.err.println("DataMaintainer.subscribe1: GOT NULL RESULT!");
      } else {
        // Add the details for all the new points
        for (int i = 0; i < res.size(); i++) {
          PointDescription pd = res.get(i);
          if (pd==null) {
            continue;
          }
          String[] names = pd.getFullNames();
          for (int j = 0; j < names.length; j++) {
            itsNames.put(names[j], pd);
          }
          itsMain.addPoint(pd, true);
        }
      }
    }

    // We've now loaded all the required points, need to add the listener
    for (int i = 0; i < points.size(); i++) {
      PointDescription fm = itsNames.get(points.get(i));
      if (fm == null) {
        continue;
      }
      fm.addPointListener(pl);
    }
  }

  public static void subscribe(String point, PointListener pl) {
    if (itsNames.get(point) == null) {
      //Get the info to build the structure from the server
      PointDescription pm = null;
      try {
        pm = (MonClientUtil.getServer()).getPoint(point);
      } catch (Exception e) {
      }
      if (pm == null) {        
        System.err.println("DataMaintainer.subscribe2: GOT NULL RESULT!");
        return;
      }
      itsMain.addPoint(pm, true);
    }
    PointDescription fm = itsNames.get(point);
    fm.addPointListener(pl);
  }

  public static void subscribe(String pointname, String source, PointListener pl) {
    subscribe(source + "." + pointname, pl);
  }

  public static void unsubscribe(String pointname, String source,
      PointListener pl) {
    unsubscribe(source + "." + pointname, pl);
  }

  /** Unsubscribe from all points contained in the vector. */
  public static void unsubscribe(Vector points, PointListener pl) {
    for (int i = 0; i < points.size(); i++) {
      unsubscribe((String) points.get(i), pl);
    }
  }

  public static void unsubscribe(String point, PointListener pl) {
    // TODO: Currently broken - doesn't unsubscribe.
    // Either fix or replace with ICE
    /*
     * if (itsNames.get(point) == null) { return; } FakeMonitor fm =
     * (FakeMonitor)MonitorMap.getPointMonitor(point);
     * fm.removePointListener(pl); itsBuffer.remove(point); if
     * (fm.getNumListeners() < 2) { itsMain.removePointMonitor(fm); //Remove
     * each name for the point String[] names = fm.getAllNames();
     * itsNames.remove(point); //for (int i=0; i<names.length; i++)
     * //itsNames.remove(source+"."+names[i]); //Next, let the collection thread
     * know our points have changed //synchronized (itsMain.itsPoints) { //
     * itsMain.itsPoints.notifyAll(); //itsMain.interrupt(); //} }
     */
  }

  /** For convenience, but it isn't the proper way of making a FakePoint */
  public static PointDescription getPointFromMap(String pointname, String source) {
    return itsNames.get(source + "." + pointname);
  }

  /** For convenience, but it isn't the proper way of making a FakePoint */
  public static PointDescription getPointFromMap(String fullname) {
    return itsNames.get(fullname);
  }

  public static PointData getBuffer(String point) {
    return (PointData) itsBuffer.get(point);
  }

  public static PointData getBuffer(String pname, String source) {
    return (PointData) itsBuffer.get(source + "." + pname);
  }

  public void run() {
    while (itsRunning) {
      Vector<PointDescription> getpoints = new Vector<PointDescription>();
      Vector<String> getnames = new Vector<String>();

      AbsTime nextTime = AbsTime.factory("ASAP");
      synchronized (itsPoints) {
        try {
          // Wait for notification if no points are waiting
          while (itsPoints.size() == 0) {
            itsPoints.wait();
          }
        } catch (Exception e) {
        }

        // Get the point from the head of the queue
        PointDescription pm = itsPoints.remove(0);
        long nextRun = pm.getNextEpoch();
        long now = AbsTime.factory().getValue();
        now += 50000; // Fudge factor

        while (pm != null && (nextRun <= now)) {
          // This point needs to be collected right now
          getpoints.add(pm);
          getnames.add("" + pm.getSource() + "." + pm.getLongName());
          pm = null;
          if (itsPoints.size() > 0) {
            // Get next point
            pm = itsPoints.remove(0);
            nextRun = pm.getNextEpoch();
          } else {
            break; // No more points awaiting collection
          }
        }
        if (pm != null) {
          itsPoints.add(0, pm); // Reinsert point
        }
      }

      if (getpoints.size() > 0) {
        // System.err.println("DataMaintainer: Requesting " + getpoints.size() +
        // " Updates");
        Vector<PointData> resdata = null;
        try {
          resdata = MonClientUtil.getServer().getData(getnames);
        } catch (Exception e) {
        }
        if (resdata != null) {
          for (int i = 0; i < getpoints.size(); i++) {
            PointDescription pm = getpoints.get(i);
            if (resdata.get(i) != null) {
              pm.firePointEvent(new PointEvent(pm, resdata.get(i), false));
            } else {
              pm.firePointEvent(new PointEvent(pm, new PointData(pm.getFullName()), false));
            }
          }
        } else {
          // Got no data back, flag points as no longer collecting
          for (int i = 0; i < getpoints.size(); i++) {
            PointDescription pm = getpoints.get(i);
            PointData pd = new PointData(pm.getFullName());
            pm.firePointEvent(new PointEvent(pm, pd, false));
          }
        }
      }

      try {
        synchronized (itsPoints) {
          if (itsPoints.size() > 0) {
            PointDescription pm = (PointDescription) itsPoints.get(0);
            nextTime = AbsTime.factory(pm.getNextEpoch());// -10000);
          }
        }
        AbsTime timenow = AbsTime.factory();
        if (nextTime.isAfter(timenow)) {
          final RelTime maxsleep = RelTime.factory(300000);
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
