//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import java.lang.reflect.*;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;
import atnf.atoms.mon.archivepolicy.ArchivePolicy;
import atnf.atoms.mon.util.*;

/**
 * Base-class for classes which check if a point is in an alarm state.
 * <P>
 * Sub-classes must implement an appropriate <i>checkAlarm</i> method.
 * <i>checkAlarm</i> should set the PointData's alarm field to True when an
 * alarm condition is detected or leave the field unchanged otherwise.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public abstract class AlarmCheck extends MonitorPolicy
{  
  public static AlarmCheck factory(PointDescription parent, String strdef)
  {
    if (strdef.equalsIgnoreCase("null")) {
      strdef = "-";
    }

    AlarmCheck result = null;

    // Find the specific information
    String specifics = strdef.substring(strdef.indexOf("-") + 1);
    String[] limitArgs = MonitorUtils.tokToStringArray(specifics);
    // Find the type of translation
    String type = strdef.substring(0, strdef.indexOf("-"));
    if (type == "" || type == null || type.length() < 1) {
      type = "NONE";
    }

    try {
      Constructor con;
      try {
        // Try to find class by assuming argument is full class name
        con = Class.forName(type).getConstructor(new Class[] { String[].class });
      } catch (Exception f) {
        // Supplied name was not a full path
        // Look in atnf.atoms.mon.alarmcheck package
        con = Class.forName("atnf.atoms.mon.alarmcheck.AlarmCheck" + type).getConstructor(new Class[] { String[].class });
        result = (AlarmCheck) (con.newInstance(new Object[] { limitArgs }));
      }
    } catch (Exception e) {
      Logger logger = Logger.getLogger(AlarmCheck.class.getName());
      logger.error("Error creating AlarmCheck \'" + strdef + "\' for point " + parent.getFullName() + ": " + e);
    }

    return result;
  }

  /**
   * Check the value against the alarm criteria.
   * @param data New data value to check.
   */
  public abstract void checkAlarm(PointData data);
}
