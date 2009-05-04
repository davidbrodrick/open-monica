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
    //preconditions
    if (data==null) {
      return null;
    }
    Object val = data.getData();

    //If we got null-data then throw a null-data result
    if (val==null) {
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    String resd = null;
    if (val instanceof Boolean) {
      if (((Boolean)val).booleanValue()) {
        resd = itsTrueVal;
      } else {
        resd = itsFalseVal;
      }
    } else if (val instanceof Number) {
      if (((Number)val).intValue()!=0) {
        resd = itsTrueVal;
      } else {
        resd = itsFalseVal;
      }
    } else {
      System.err.println("TranslationBoolMap (for " + itsParent.getSource()
			 + "." + itsParent.getName() +
			 "): UNHANDLED DATA TYPE!");
      return null;
    }

    //Create return structure with right details
    PointData res = new PointData(itsParent.getName(), itsParent.getSource());
    res.setData(resd);

    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
