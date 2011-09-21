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
import org.apache.log4j.Logger;

/**
 * Return a substring of the argument using the given start index and, optionally, end index.
 * 
 * @author David Brodrick
 */
public class TranslationSubstring extends Translation
{
  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(TranslationSubstring.class);
  
  /** Start index. */
  private int itsStartIndex;
  
  /** End index. */
  private int itsEndIndex = -1;
  
  public TranslationSubstring(PointDescription parent, String[] init) throws Exception
  {
    super(parent, init);

    if (init.length<1) {
      theirLogger.error("(" + parent.getFullName() + "): Insuffient arguments for TranslationSubstring");
      throw new IllegalArgumentException("Insuffient arguments for TranslationSubstring");
    }
    
    try {
      itsStartIndex = Integer.parseInt(init[0]);
      if (itsStartIndex<0) {
        theirLogger.error("(" + parent.getFullName() + "): Invalid startIndex for TranslationSubstring");
        throw new IllegalArgumentException("Invalid startIndex for TranslationSubstring");
      }
      if (init.length>1) {
        itsEndIndex = Integer.parseInt(init[1]);
        if (itsEndIndex<itsStartIndex) {
          theirLogger.error("(" + parent.getFullName() + "): Invalid endIndex for TranslationSubstring");
          throw new IllegalArgumentException("Invalid endIndex for TranslationSubstring");
        }
      }
    } catch (Exception e) {
      theirLogger.error("(" + parent.getFullName() + "): Creating TranslationSubstring: " + e);
      throw e;
    }
  }

  public PointData translate(PointData data)
  {
    Object val = data.getData();

    // If we got null-data then throw a null-data result
    if (val == null) {
      return new PointData(itsParent.getFullName());
    }

    // Get value as a string
    String str = val.toString();
    
    // Check bounds
    int strlen = str.length();    
    if (strlen<itsStartIndex || (itsEndIndex!=-1 && strlen<itsEndIndex)) {
      // Halt the translation process for this point
      return null;
    }
    
    // Get the required substring
    String resstr;
    if (itsEndIndex!=-1) {
      resstr=str.substring(itsStartIndex, itsEndIndex);
    } else {
      resstr = str.substring(itsStartIndex);
    }

    // Create return structure with right details
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), resstr);

    return res;
  }
}
