//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointDescription;

/**
 * Listens to two monitor points with synchronously updating values and
 * returns the ratio of the first listened-to point divided by the second
 * point.
 *
 * @author David Brodrick
 * @version $Id: TranslationRatio.java,v 1.1 2005/07/20 23:06:26 bro764 Exp $
 */
public class
TranslationRatio
extends TranslationSynch
{
  protected static String[] itsArgs = new String[]{
    "Ratio", "what goes here",
    "Numerator point", "java.lang.String",
    "Divisor point", "java.lang.String"};

  public
  TranslationRatio(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Calculate the ratio of the two arguments.
   *@param val1 Most recent numerator value
   *@param val2 Most recent divisor value
   *@return The ratio val1/val2 */
  protected
  Object
  doCalculations(Object val1, Object val2)
  {
    if (! (val1 instanceof Number) || ! (val2 instanceof Number)) {
      System.err.println("TranslationRatio: " + itsParent.getName()
			 + ": ERROR got data of NON-NUMERIC type!");
      return null;
    }
    float numerator = ((Number)(val1)).floatValue();
    float divisor   = ((Number)(val2)).floatValue();

    return new Float(numerator/divisor);
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
