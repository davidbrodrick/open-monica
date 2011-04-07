//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * any argument with out a : is considered to define the default string
 * 
 * @author David Brodrick
 * @version $Id: TranslationEnumMap.java,v 1.3 2004/10/28 23:44:58 bro764 Exp
 *          bro764 $
 */
public class TranslationEnumMap extends Translation {
  /** Array of integer enumeration values. */
  protected int[] itsValues = null;

  /** Array of String to map the values to. */
  protected String[] itsMappings = null;

  /** String to use if no enum values match. */
  protected String itsDefault = "INVALID";

  protected static String[] itsArgs = new String[] { "Translation Enum Map", "Enum Map" };

  public TranslationEnumMap(PointDescription parent, String[] init) {
    super(parent, init);

    // Count how many mapping arguments there are
    int count = 0;
    for (int i = 0; i < init.length; i++) {
      if (init[i].indexOf(":") != -1) {
        count++;
      }
    }

    // Allocate containers of the correct size
    itsValues = new int[count];
    itsMappings = new String[count];

    // Parse and record values
    count = 0;
    for (int i = 0; i < init.length; i++) {
      int sep = init[i].indexOf(":");
      if (sep == -1) {
        // Must be the "default" string value
        itsDefault = init[i];
      } else {
        // Break string into value:mapping parts
        String val = init[i].substring(0, sep).trim();
        String map = init[i].substring(sep + 1).trim();
        itsValues[count] = Integer.parseInt(val);
        itsMappings[count] = map;
        count++;
      }
    }
  }

  /** Calculate the delta and return new value. */
  public PointData translate(PointData data) {
    // preconditions
    if (data == null) {
      return null;
    }
    Object val = data.getData();

    // If we got null-data then throw a null-data result
    if (val == null) {
      return new PointData(itsParent.getFullName());
    }

    if (!(val instanceof Number)) {
      // Probably an error somewhere so print a message
      System.err.println("TranslationEnumMap for " + itsParent.getSource() + "." + itsParent.getName() + ": Got NON-NUMERIC" + " data..");
      return null;
    }

    // Get the value
    int d = ((Number) val).intValue();
    // See if it corresponds to a value in our mapping set
    String m = null;
    for (int i = 0; i < itsValues.length; i++) {
      if (itsValues[i] == d) {
        m = itsMappings[i];
      }
    }
    if (m == null) {
      // Didn't find a match, so use our default value
      m = itsDefault;
    }

    // Create return structure with right details
    PointData res = new PointData(itsParent.getFullName(), m);

    return res;
  }

  public static String[] getArgs() {
    return itsArgs;
  }
}
