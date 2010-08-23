//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import atnf.atoms.mon.*;

/**
 * <i>AlarmCheck</i> sub-class which checks if a String data-value matches a predefined
 * set. We can work in one of two ways, either considering the new value to be <i>okay</i>
 * if it matches one of the strings in the set, or considering it to be <i>in error</i>
 * if it matches a string in the set.
 * <P>
 * The first argument determines which of these behaviors we use. If the argument is
 * <tt>true</tt> then we consider a string match be be good. If the first argument is
 * false then we consider a string match to be bad.
 * <P>
 * All the remaining arguments must be the strings to check against.
 * <P>
 * NOTE: All string comparisons are performed in a case insensitive manner.
 * 
 * @author David Brodrick
 */
public class AlarmCheckStringMatch extends AlarmCheck
{
  /** Does a match indicate ok (true) or error (false). */
  protected boolean itsMatchGood = true;

  /** Strings to check the data values against. */
  protected String[] itsStringSet = null;

  public AlarmCheckStringMatch(PointDescription parent, String[] args)
  {
    super(parent, args);

    if (args.length < 2) {
      // This is a problem
      System.err.println("AlarmCheckStringMatch: Need at least two arguments!!!");
    } else {
      // First check which mode to run in
      if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("t") || args[0].equalsIgnoreCase("1")) {
        itsMatchGood = true;
      } else {
        itsMatchGood = false;
      }
      // The rest of the arguments are strings to check against
      itsStringSet = new String[args.length - 1];
      for (int i = 1; i < args.length; i++) {
        itsStringSet[i - 1] = args[i].trim();
      }
    }
  }

  public void checkAlarm(PointData data)
  {
    // No strings means we shouldn't even bother
    if (itsStringSet == null) {
      return;
    }
    // No news is good news, right?
    if (data == null || data.getData() == null) {
      return;
    }

    // Get the data in string form
    String strData = data.getData().toString().trim();

    // Check the string against each string in our set
    boolean havematch = false;
    for (int i = 0; i < itsStringSet.length; i++) {
      if (strData.equalsIgnoreCase(itsStringSet[i])) {
        havematch = true;
        break;
      }
    }

    if ((!havematch && itsMatchGood) || (havematch && !itsMatchGood)) {
      // Found an alarm condition
      data.setAlarm(true);
    }
  }
}
