//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.limit;

import java.util.StringTokenizer;
import atnf.atoms.mon.*;
import atnf.atoms.util.Angle;

/**
 * <i>PointLimit</i> sub-class which checks if a String data-value
 * matches a predefined set. We can work in one of two ways, either
 * considering the new value to be <i>okay</i> if it matches one of
 * the strings in the set, or considering it to be <i>in error</i>
 * if it matches a string in the set.
 * <P>
 * The first argument determines which of these behaviors we use.
 * If the argument is <tt>true</tt> then we consider a string match
 * be be good. If the first argument is false then we consider a
 * string match to be bad.
 * <P>
 * All the remaining arguments must be the strings to check against.
 * <P>
 * NOTE: All string comparisons are performed in a case insensitive manner.
 *
 * @author David Brodrick
 * @version $Id: PointLimitStringMatch.java,v 1.1 2004/06/08 02:32:02 bro764 Exp $
 */
class PointLimitStringMatch
extends PointLimit
{
  /** Argument description strings. */
  protected static String itsArgs[] = new String[]{"Limits - None",""};

  /** Does a match indicate ok (true) or error (false). */
  protected boolean itsMatchGood = true;

  /** Strings to check the data values against. */
  protected String[] itsStringSet = null;

  public PointLimitStringMatch(String[] args)
  {
    if (args.length<2) {
      //This is a problem
      System.err.println("PointLimitStringMatch: " +
			 "Need at least two arguments!!!");
    } else {
      //First check which mode to run in
      if (args[0].equalsIgnoreCase("true") ||
	  args[0].equalsIgnoreCase("t") ||
	  args[0].equalsIgnoreCase("1")) {
	itsMatchGood = true;
      } else {
	itsMatchGood = false;
      }
      //The rest of the arguments are strings to check against
      itsStringSet = new String[args.length-1];
      for (int i=1; i<args.length; i++) {
	itsStringSet[i-1] = args[i].trim();
      }
    }
  }


  /**
   * Checks if the value is "normal" with respect to the specified limits.
   * If the value is okay then <tt>true</tt> will be returned, otherwise
   * <tt>false</tt> is returned.
   *
   * @param data The value to check against our limits.
   * @return <tt>True</tt> if the value is okay, <tt>false</tt> otherwise. **/
  public boolean checkLimits(PointData data)
  {
    //No strings means we shouldn't even bother
    if (itsStringSet==null) return true;
    //No news is good news, right?
    if (data==null || data.getData()==null) return true;

    //Get the data in string form
    String strData = data.getData().toString().trim();

    //Check the string against each string in our set
    boolean havematch = false;
    for (int i=0; i<itsStringSet.length; i++) {
      if (strData.equalsIgnoreCase(itsStringSet[i])) {
	havematch = true;
	break;
      }
    }

    //Return the appropriate result
    if (havematch && itsMatchGood) return true;
    else if (!havematch && !itsMatchGood) return true;
    else return false;
  }
}
