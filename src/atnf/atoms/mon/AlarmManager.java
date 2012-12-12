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

import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

public class AlarmManager {
  /**
   * Class encapsulating the current alarm status and associated data for a particular point. There is some degeneracy in this as
   * the structure is essentially self contained but also has reference to the parent PointDescription.
   */
  public static class Alarm {
    public PointDescription point;
    public PointData data;
    public boolean alarm = false;
    public boolean shelved = false;
    public String shelvedBy = null;
    public AbsTime shelvedAt = null;
    public boolean acknowledged = false;
    public String acknowledgedBy = null;
    public AbsTime acknowledgedAt = null;
    public int priority = 0;
    public String guidance = null;
    
    public static final int NOT_ALARMED = 0;
    public static final int ACKNOWLEDGED = 1;
    public static final int SHELVED = 2;
    public static final int ALARMING = 3;

    public Alarm(PointDescription p) {
      point = p;
      priority = point.getPriority();
    }

    public Alarm(PointDescription p, PointData d) {
      point = p;
      priority = point.getPriority();
      data = d;
      alarm = d.getAlarm();
    }

    public void updateData(PointData d) {
      data = d;
      if (data != null) {
        alarm = data.getAlarm();
        guidance = getGuidanceText();
      }
    }

    private String getGuidanceText() {
      String text = point.getGuidance();
      if (text != null && !text.isEmpty() && data != null) {
        text = MonitorUtils.doSubstitutions(text, data, point);
      }
      return text;
    }

    public String toString() {
      String res = point.getFullName() + "\t" + priority + "\t" + alarm;
      res += "\t" + acknowledged + "\t" + acknowledgedBy;
      if (acknowledgedAt == null) {
        res += "\tnull";
      } else {
        res += "\t" + acknowledgedAt.toString(AbsTime.Format.HEX_BAT);
      }
      res += "\t" + shelved + "\t" + shelvedBy;
      if (shelvedAt == null) {
        res += "\tnull";
      } else {
        res += "\t" + shelvedAt.toString(AbsTime.Format.HEX_BAT);
      }
      res += "\t\"" + guidance + "\"";
      return res;
    }
    
	public int getAlarmStatus(){
		int status = Alarm.NOT_ALARMED;
		if (this.acknowledged){
			status = Alarm.ACKNOWLEDGED;
		} else if (this.shelved){
			status = Alarm.SHELVED;
		} else if (this.alarm){ // will only get here if it is not shelved OR acknowledged, but still alarming
			status = Alarm.ALARMING;
		}
		return status;
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
        thisalarm.updateData(data);
        // Acknowledgement gets cleared if no longer in alarm
        if (!thisalarm.alarm && thisalarm.acknowledged) {
          thisalarm.acknowledged = false;
          thisalarm.acknowledgedBy = null;
          thisalarm.acknowledgedAt = null;
        }
      } else {
        // Need to create new data structure
        thisalarm = new Alarm(point, data);
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
  
  /** Get the list of all alarms currently defined in the system. */
  public static Vector<Alarm> getAllAlarms() {
    Vector<Alarm> res;
    synchronized (theirAlarms) {
      res = new Vector<Alarm>(theirAlarms.size());
      Iterator<Alarm> i = theirAlarms.values().iterator();
      while (i.hasNext()) {
        Alarm thisalarm = i.next();
        res.add(thisalarm);
      }
    }
    return res;
  }
  
  /** Acknowledge an alarm. */
  public static void setAcknowledged(PointDescription point, boolean acked, String user, AbsTime time) {
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
  public static void setShelved(PointDescription point, boolean shelved, String user, AbsTime time) {
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
