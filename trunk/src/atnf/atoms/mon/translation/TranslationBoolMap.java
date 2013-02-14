//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * This takes two string arguments. The first string is what should be
 * displayed when the Boolean data is <i>True</i>. The second argument
 * indicates what to output if the Boolean is <i>False</i>.
 *
 * In fact, we also support <i>Number</i> types, where a value of <i>zero</i>
 * is considered equivalent to the boolean value of <i>False</i>, as
 * with C.
 *
 * @author David Brodrick
 * @version $Id: TranslationBoolMap.java,v 1.1 2005/02/02 00:54:19 bro764 Exp $
 */
public class
TranslationBoolMap
extends Translation
{
  /** String to use when Boolean is True */
  protected String itsTrueVal = "True";

  /** String to use when Boolean is False */
  protected String itsFalseVal = "False";

  protected static String[] itsArgs = new String[]{
    "Translation Bool Map", "Bool Map",
    "String", "Value to show when bool is True",
    "String", "Value to show when bool is False"};

  public TranslationBoolMap(PointDescription parent, String[] init)
  {
    super(parent, init);

    if (init.length<2) {
      System.err.println("TranslationBoolMap (for " +
			 parent.getSource() + "." + parent.getName() +
			 "): REQUIRE TWO ARGUMENTS!");
    } else {
      itsTrueVal = init[0];
      itsFalseVal = init[1];
    }
  }


  /** Map the Boolean or Number to the appropriate String. */
  public
  PointData
  translate(PointData data)
  {
    String resstr = null;
    // Get the input as a Boolean
    try {
      boolean inputstate = MonitorUtils.parseAsBoolean(data.getData());
      if (inputstate) {
        resstr = itsTrueVal;
      } else {
        resstr = itsFalseVal;
      }      
    } catch (IllegalArgumentException e) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): " + e);
      return null;
    }

    //Create return structure with right details
    PointData res = new PointData(itsParent.getFullName(), data.getTimestamp(), resstr);
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
