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
 * Outputs a single True boolean value when the input changes value. The output will be False if the input value hasn't changed.
 * 
 * @author David Brodrick
 */
public class TranslationDetectChange extends Translation {
  /** The previous data value. */
  protected Object itsLastValue;

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(TranslationDetectChange.class);

  public TranslationDetectChange(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Output True if value changed. */
  public PointData translate(PointData data) {
    boolean boolres;
    Object newvalue = data.getData();
    if (newvalue == null || itsLastValue == null) {
      // Don't trigger on null values
      boolres = false;
    } else if (newvalue instanceof Number && itsLastValue instanceof Number) {
      // Compare numbers
      if (((Number) newvalue).doubleValue() == ((Number) itsLastValue).doubleValue()) {
        boolres = false;
      } else {
        boolres = true;
      }
    } else {
      // Try to compare values using reflection
      try {
        Method equalsMethod = newvalue.getClass().getMethod("equals", new Class[] { Object.class });
        Object eq = equalsMethod.invoke(newvalue, new Object[] { itsLastValue });
        boolres = !((Boolean) eq).booleanValue();
      } catch (Exception e) {
        theirLogger.warn("(" + itsParent.getFullName() + "): [" + data.getName() + "]: " + e);
        boolres = false;
      }
    }

    // Record value and output result
    itsLastValue = data.getData();
    return new PointData(itsParent.getFullName(), data.getTimestamp(), new Boolean(boolres));
  }
}
