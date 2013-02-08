// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Class encapsulating the current alarm status and associated data for a particular point. There is some duplication in this as the
 * structure is essentially self contained but also has reference to the parent PointDescription.
 * @author David Brodrick
 * @author Kalinga Hulugalle
 */
public class Alarm {
  private PointDescription point;
  private PointData data;
  private boolean alarm = false;
  private boolean shelved = false;
  private String shelvedBy = null;
  private AbsTime shelvedAt = null;
  private boolean acknowledged = false;
  private String acknowledgedBy = null;
  private AbsTime acknowledgedAt = null;
  private int priority = -1;
  private String guidance = null;

  public static final int NOT_ALARMED = 0;
  public static final int ACKNOWLEDGED = 1;
  public static final int SHELVED = 2;
  public static final int ALARMING = 3;

  /**
   * C'tor
   */
  public Alarm() {
  }

  /**
   * C'tor
   * 
   * @param p
   *          - The PointDescription associated with this alarm
   */
  public Alarm(PointDescription p) {
    point = p;
    priority = point.getPriority();
    data = null;
    alarm = false;
    acknowledged = false;
    shelved = false;
  }

  /**
   * C'tor
   * 
   * @param p
   *          - The PoinDescription associated with this alarm
   * @param d
   *          - The PointData associated with this alarm
   */
  public Alarm(PointDescription p, PointData d) {
    point = p;
    priority = point.getPriority();
    data = d;
    alarm = d.getAlarm();
  }

  /**
   * Server side method for updating data and updating the guidance text.
   * 
   * @param d
   *          The new data value.
   */
  public void updateData(PointData d) {
    data = d;
    if (data != null) {
      alarm = data.getAlarm();
      guidance = populateGuidanceText();
    }
  }

  /**
   * Get the current value of the guidance text by applying any required substitutions.
   * 
   * @return New guidance text.
   */
  private String populateGuidanceText() {
    String text = point.getGuidance();
    if (text != null && !text.isEmpty() && data != null) {
      text = MonitorUtils.doSubstitutions(text, data, point);
    }
    return text;
  }

  /**
   * Set a new data value.
   * 
   * @param d
   *          The new data value.
   */
  public void setData(PointData d) {
    data = d;
  }

  /**
   * Get the data value.
   * 
   * @return The data value associated with this alarm.
   */
  public PointData getData() {
    return data;
  }

  /**
   * Formats this alarm into a human-readable String format
   * 
   * @return Returns a string representation of this alarm
   */
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

  /**
   * Gives a simple categorisation of what state this Alarm is in
   * 
   * @return An <code><strong>int</strong></code> that corresponds to one of four states, NOT_ALARMED, ACKNOWLEDGED, SHELVED and
   *         ALARMING. The former categories take priority over the latter ones.
   */
  public int getAlarmStatus() {
    int status = Alarm.NOT_ALARMED;
    if (this.isAcknowledged()) {
      status = Alarm.ACKNOWLEDGED;
    } else if (this.isShelved()) {
      status = Alarm.SHELVED;
    } else if (this.isAlarming()) { // will only get here if it is not shelved OR acknowledged, but still alarming
      status = Alarm.ALARMING;
    }
    return status;
  }

  /**
   * The priority associated with this alarm point.
   * 
   * @return An int holding the priority of the alarm, ranges between -1 and 3 inclusive
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Set the priority associated with this alarm point.
   * 
   * @param p
   *          An int holding the priority of the alarm, ranges between -1 and 3 inclusive
   */
  public void setPriority(int p) {
    priority = p;
  }

  /**
   * The name of the user who last acknowledged this alarm point, if applicable
   * 
   * @return A String containing the name of the user
   */
  public String getAckedBy() {
    return acknowledgedBy;
  }

  /**
   * The time that this alarm point was last acknowledged
   * 
   * @return An AbsTime value for when this point was acknowledged
   */
  public AbsTime getAckedAt() {
    return acknowledgedAt;
  }

  /**
   * Simple method that returns whether this alarm point is acknowledged or not
   * 
   * @return A <code><strong>boolean</strong></code> holding the value.
   */
  public boolean isAcknowledged() {
    return acknowledged;
  }

  /**
   * Simple method to set the Acknowledged state of the Alarm
   * 
   * @param bool
   *          The boolean state that the Acknowledgement state should be set to
   */
  public void setAcknowledged(boolean state) {
    this.acknowledged = state;
  }

  /**
   * Simple method to set the Acknowledged state and other data of the Alarm
   * 
   * @param state
   *          The state the Acknowledgement should take
   * @param acknowledger
   *          The name of the person acknowledging this Alarm
   */
  public void setAcknowledged(boolean state, String acknowledger) {
    this.acknowledged = state;
    this.acknowledgedBy = acknowledger;
    this.acknowledgedAt = new AbsTime();
  }

  /**
   * Simple method to set the Acknowledged state and other data of the Alarm
   * 
   * @param state
   *          The state the Acknowledgement should take
   * @param acknowledger
   *          The name of the person acknowledging this Alarm
   * @param time
   *          The time of this acknowledgement
   */
  public void setAcknowledged(boolean state, String acknowledger, AbsTime time) {
    this.acknowledged = state;
    this.acknowledgedBy = acknowledger;
    this.acknowledgedAt = time;
  }

  /**
   * Simple method to set the name of the person acknowledging the alarm
   * 
   * @param acknowledger
   *          The name of the person acknowledging this Alarm
   */
  public void setAcknowledgedBy(String acknowledger) {
    this.acknowledgedBy = acknowledger;
  }

  /**
   * Simple method to set the name of the person acknowledging the alarm
   * 
   * @param time
   *          The time that the acknowledgement is taking place
   */
  public void setAcknowledgedAt(AbsTime time) {
    this.acknowledgedAt = time;
  }

  /**
   * The name of the user who last shelved this alarm point, if applicable
   * 
   * @return A String containing the name of the user
   */
  public String getShelvedBy() {
    return shelvedBy;
  }

  /**
   * The time that this alarm point was last shelved
   * 
   * @return An AbsTime value for when this point was shelved
   */
  public AbsTime getShelvedAt() {
    return shelvedAt;
  }

  /**
   * Simple method that returns whether this alarm point is shelved or not
   * 
   * @return A <code><strong>boolean</strong></code> holding the value.
   */
  public boolean isShelved() {
    return shelved;
  }

  /**
   * Simple method to set the Shelving state of the Alarm
   * 
   * @param bool
   *          The boolean state that the Shelving state should be set to
   */
  public void setShelved(boolean state) {
    this.shelved = state;
  }

  /**
   * Simple method to set the Shelved state and other data of the Alarm
   * 
   * @param state
   *          The state the Shelving should take
   * @param shelver
   *          The name of the person shelving this Alarm
   * @param time
   *          The time of this shelving
   */
  public void setShelved(boolean state, String shelver, AbsTime time) {
    this.shelved = state;
    this.shelvedBy = shelver;
    this.shelvedAt = time;
  }

  /**
   * Simple method to set the name of the person shelving the alarm
   * 
   * @param shelver
   *          The name of the person shelving this Alarm
   */
  public void setShelvedBy(String shelver) {
    this.shelvedBy = shelver;
  }

  /**
   * Simple method to set the name of the person acknowedging the alarm
   * 
   * @param time
   *          The time that the acknowledgedment is taking place
   */
  public void setShelvedAt(AbsTime time) {
    this.shelvedAt = time;
  }

  /**
   * Returns a message conveying the actions that a user should take upon being alerted that this alarm has been activated.
   * 
   * @return A String containing the message
   */
  public String getGuidance() {
    return guidance;
  }

  /**
   * Set the message conveying the actions that a user should take upon being alerted that this alarm has been activated.
   * 
   * @param g
   *          A String containing the message
   */
  public void setGuidance(String g) {
    guidance = g;
  }

  /**
   * Simple method that returns whether this alarm point is currently alarming or not
   * 
   * @return A <code><strong>boolean</strong></code> holding the value.
   */
  public boolean isAlarming() {
    return alarm;
  }

  /**
   * Specify whether this alarm point is currently alarming or not
   * 
   * @param a
   *          Boolean holding the new alarm value.
   */
  public void setAlarming(boolean a) {
    alarm = a;
  }

  /**
   * Method to return the PointDescription (and associated metadata) related to this point
   * 
   * @return the PointDescription
   */
  public PointDescription getPointDesc() {
    return point;
  }

  /**
   * Set the PointDescription reference.
   * 
   * @param p
   *          The PointDescription.
   */
  public void setPointDesc(PointDescription p) {
    point = p;
  }
}
