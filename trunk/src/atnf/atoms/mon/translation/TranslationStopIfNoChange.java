//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MailSender;
import atnf.atoms.time.AbsTime;

import java.lang.reflect.*;
import org.apache.log4j.Logger;

/**
 * Arrests the point update/translation process if the data input value hasn't changed. This could be used for instance for control
 * points where we do not wish to write the data value to the hardware unless it has changed. The point update process is halted by
 * returning a null value instead of a valid PointData object.
 * 
 * @author David Brodrick
 */
public class TranslationStopIfNoChange extends Translation {
  /** The previous data value. */
  protected Object itsLastValue;

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(TranslationStopIfNoChange.class);

  public TranslationStopIfNoChange(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Output null if value hasn't changed. */
  public PointData translate(PointData data) {
    boolean haschanged;
    Object newvalue = data.getData();
    if (newvalue == null && itsLastValue == null) {
      haschanged = false;
    } else if (newvalue == null || itsLastValue == null) {
      haschanged = true;
    } else if (newvalue instanceof Number && itsLastValue instanceof Number) {
      // Compare numbers
      if (((Number) newvalue).doubleValue() == ((Number) itsLastValue).doubleValue()) {
        haschanged = false;
      } else {
        haschanged = true;
      }
    } else {
      // Try to compare values using reflection
      try {
        Method equalsMethod = newvalue.getClass().getMethod("equals", new Class[] { Object.class });
        Object eq = equalsMethod.invoke(newvalue, new Object[] { itsLastValue });
        haschanged = !((Boolean) eq).booleanValue();
      } catch (Exception e) {
        theirLogger.warn("(" + itsParent.getFullName() + "): [" + data.getName() + "]: " + e);
        haschanged = false;
      }
    }
    
    // Return a null value unless the input changed
    PointData res = null;
    if (haschanged) {
      res = data;
    }

    // Record value and output result
    itsLastValue = data.getData();
    return res;
  }
}
