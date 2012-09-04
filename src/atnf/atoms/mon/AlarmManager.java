//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon;

import java.util.*;
import atnf.atoms.time.AbsTime;

public class AlarmManager {
  public static class Alarm {
    public PointDescription point;
    public boolean alarm = false;
    public boolean shelved = false;
    public String shelvedBy = null;
    public AbsTime shelvedAt = null;
    public boolean acknowledged = false;
    public String acknowledgedBy = null;
    public AbsTime acknowledgedAt = null;

    public Alarm(PointDescription p) {
      point = p;
    }

    public Alarm(PointDescription p, boolean a) {
      point = p;
      alarm = a;
    }
  }

  /** Record of points which are currently in a priority alarm state. */
  private static HashMap<PointDescription, Alarm> theirAlarms = new HashMap<PointDescription, Alarm>(500, 1000);

  /** Set the current alarm status for the given point. */
  public static void setAlarm(PointDescription point, PointData data) {
    synchronized (theirAlarms) {
      Alarm thisalarm = theirAlarms.get(point);
      if (thisalarm != null) {
        // Just update the extant data structure
        thisalarm.alarm = data.getAlarm();
        // Acknowledgement gets cleared if no longer in alarm
        if (!thisalarm.alarm && thisalarm.acknowledged) {
          thisalarm.acknowledged = false;
        }
      } else {
        // Need to create new data structure
        thisalarm = new Alarm(point, data.getAlarm());
        theirAlarms.put(point, thisalarm);
      }
    }
  }

  /** Get the list of priority alarms currently in an alarm state (acknowledged or not) or not in an alarm but shelved. */
  public static Vector<Alarm> getAlarms() {
    Vector<Alarm> res;
    synchronized (theirAlarms) {
      res = new Vector<Alarm>(theirAlarms.size());
      Iterator<Alarm> i = theirAlarms.values().iterator();
      while (i.hasNext()) {
        Alarm thisalarm = i.next();
        if (thisalarm.alarm || thisalarm.shelved) {
          res.add(thisalarm);
        }
      }
    }
    return res;
  }

  /** Acknowledge an alarm. */
  public void setAcknowledged(PointDescription point, boolean acked, String user, AbsTime time) {
    synchronized (theirAlarms) {
      Alarm thisalarm = theirAlarms.get(point);
      if (thisalarm == null) {
        // Need to create new data structure
        thisalarm = new Alarm(point);
        theirAlarms.put(point, thisalarm);
      }
      thisalarm.acknowledged = acked;
      thisalarm.acknowledgedBy = user;
      thisalarm.acknowledgedAt = time;
    }
  }

  /** Shelve an alarm. */
  public void setShelved(PointDescription point, boolean shelved, String user, AbsTime time) {
    synchronized (theirAlarms) {
      Alarm thisalarm = theirAlarms.get(point);
      if (thisalarm == null) {
        // Need to create new data structure
        thisalarm = new Alarm(point);
        theirAlarms.put(point, thisalarm);
      }
      thisalarm.shelved = shelved;
      thisalarm.shelvedBy = user;
      thisalarm.shelvedAt = time;
    }
  }
}
