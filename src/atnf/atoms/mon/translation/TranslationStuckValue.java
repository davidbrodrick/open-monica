//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.Vector;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 *
 * @author David Brodrick
 */
public class
TranslationStuckValue
extends Translation
{
  /** Buffer containing historical data. */
  protected Object[] itsBuffer;
  
  /** The number of identical updates before we declare a value is 'stuck'. */
  protected int itsNumUpdates;

  public TranslationStuckValue(PointDescription parent, String[] init)
  {
    super(parent, init);

    itsNumUpdates = Integer.parseInt(init[0]);
    itsBuffer = new Object[itsNumUpdates];
  }

  /** Check for stuck value, flag data if it still hasn't changed. */
  public
  PointData
  translate(PointData data)
  {
    // Rotate the data in the buffer
    for (int i=0; i<itsNumUpdates-1; i++) {
      itsBuffer[i] = itsBuffer[i+1];
    }
    itsBuffer[itsNumUpdates-1] = data.getData();
    
    boolean somenull = false;
    boolean somenotnull = false;
    Vector<String> dataclasses = new Vector<String>();
    for (int i=0; i<itsNumUpdates; i++) {
      if (itsBuffer[i]==null) {
        somenull = true;
      } else {
        somenotnull = true;
        String thisclass = itsBuffer[i].getClass().getName();
        if (!dataclasses.contains(thisclass)) {
          dataclasses.add(thisclass);
        }
      }
    }
    
    if (somenull && somenotnull) {
      // Value has obviously changed to/from null
      return data;
    } else if (somenull && !somenotnull) {
      // Value is stuck at null
      return new PointData(itsParent.getFullName(), data.getTimestamp(), null, true);
    } else if (dataclasses.size()>1) {
      // Different classes must mean data is changing
      return data;
    } else {
      // Need to check actual values, we know they are all of same class
      if (itsBuffer[0] instanceof Number) {
        double firstval = ((Number)itsBuffer[0]).doubleValue();
        for (int i=1; i<itsNumUpdates; i++) {
          if (((Number)itsBuffer[i]).doubleValue() != firstval) {
            // Value changed
            return data;
          }
        }
      } else if (itsBuffer[0] instanceof String) {
        String firstval = ((String)itsBuffer[0]);
        for (int i=1; i<itsNumUpdates; i++) {
          if (!((String)itsBuffer[i]).equals(firstval)) {
            // Value changed
            return data;
          }
        }
      } else {
        System.err.println("TranslationStuckValue(" + itsParent.getFullName() + "): Unsupported data class " + itsBuffer[0].getClass().getName());
        return data;
      }
    }
    
    // Value is stuck so mask to null and set alarm condition 
    return new PointData(itsParent.getFullName(), data.getTimestamp(), null, true);
  }
}
