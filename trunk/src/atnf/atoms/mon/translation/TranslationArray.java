// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * Returns a particular entry from an array.
 * <P>
 * A monitor point that uses this translation would normally have a <i>Listen</i> transaction that listens to a monitor point whose
 * data is an array. This translation would then extract a single entry from the array and output just that entry.
 * 
 * @author David Brodrick
 * @version $Id: $
 **/
public class TranslationArray extends Translation {
  /** The array index we should return. */
  private int itsIndex = 0;

  protected static String[] itsArgs = new String[] { "Translation Array", "Array", "Array index to return", "java.lang.Integer" };

  public TranslationArray(PointDescription parent, String[] init) {
    super(parent, init);
    if (init.length != 1) {
      System.err.println("ERROR: TranslationArray (for " + itsParent.getName() + "): Expect 1 Argument.. got " + init.length);
    } else {
      if (init[0].startsWith("0x")) {
        // Allow a hex index
        itsIndex = Integer.parseInt(init[0].substring(2), 16);
      } else {
        itsIndex = Integer.parseInt(init[0]);
      }
    }
  }

  public PointData translate(PointData data) {
    // Precondition
    if (data == null) {
      return null;
    }

    // Get the full array
    Object[] array = (Object[]) data.getData();

    // Create the new data structure to be returned
    PointData res = new PointData(itsParent.getFullName());

    // If the data is null we need to throw a null-data result
    if (array == null || array.length < itsIndex) {
      return res;
    }

    // Copy just the particular index that we've been asked to
    res.setData(array[itsIndex]);

    // Keep the time-stamp of the parent point rather than use "now"
    res.setTimestamp(data.getTimestamp());

    return res;
  }

  public static String[] getArgs() {
    return itsArgs;
  }
}
