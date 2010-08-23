// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.archivepolicy;

import java.lang.reflect.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Archives data when the value changes.
 * 
 * <P>
 * The default value is to archive when the value changes at all, however an optional
 * argument can be used to specify a delta threshold (either absolute or as a percentage)
 * and archiving will only take place once the data value has changed by more than the
 * threshold since the data was last archived.
 * 
 * <P>
 * Examples: <bl>
 * <li><b>Change-</b> This will archive every change of value.
 * <li><b>Change-"20"</b> Archive when the value changes by more then 20 (absolute).
 * <li><b>Change-"15%"</b> Archive when the value varies from the last archived value by
 * more than 15% (relative). </bl>
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public class ArchivePolicyChange extends ArchivePolicy
{
  /** The last data which was archived. */
  Object itsLastSaveData = null;

  /** Whether we are running in percentage mode. */
  boolean itsPercentage = false;

  /** Whether we are running in absolute delta mode. */
  boolean itsDelta = false;

  /** The absolute or relative change threshold. */
  float itsChangeThreshold;

  public ArchivePolicyChange(PointDescription parent, String[] args)
  {
    super(parent, args);

    if (args != null && args.length > 0) {
      if (args[0].indexOf("%") != -1) {
        args[0] = args[0].replace("%", "");
        itsPercentage = true;
      } else {
        itsDelta = true;
      }
      itsChangeThreshold = Float.parseFloat(args[0]);
    }
  }

  public boolean checkArchiveThis(PointData data)
  {
    boolean savenow = false;
    Object newData = data.getData();
    if (newData == null && itsLastSaveData == null) {
      savenow = false;
      return savenow;
    }
    if (itsLastSaveData == null) {
      itsLastSaveData = newData;
      savenow = true;
      return savenow;
    } else if (newData == null) {
      itsLastSaveData = null;
      savenow = true;
      return savenow;
    }

    if (newData instanceof Number && itsLastSaveData instanceof Number) {
      float delta = Math.abs(((Number) (newData)).floatValue() - ((Number) (itsLastSaveData)).floatValue());
      if (itsDelta) {
        if (delta >= itsChangeThreshold) {
          itsLastSaveData = newData;
          savenow = true;
        }
      } else if (itsPercentage) {
        float percent = delta / Math.abs(((Number) (itsLastSaveData)).floatValue());
        if (percent >= itsChangeThreshold) {
          itsLastSaveData = newData;
          savenow = true;
        }
      } else {
        if (delta != 0.0f) {
          itsLastSaveData = newData;
          savenow = true;
        }
      }
    } else {
      try {
        Method equalsMethod = newData.getClass().getMethod("equals", new Class[] { Object.class });
        Object res = equalsMethod.invoke(newData, new Object[] { itsLastSaveData });
        savenow = !((Boolean) res).booleanValue();
        itsLastSaveData = newData;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return savenow;
  }
}
