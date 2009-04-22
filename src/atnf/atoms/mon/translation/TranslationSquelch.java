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
 * Translation which sets the numeric value to a specified level if the 
 * input value is below a specified threshold. If the input is above the
 * squelch level then it is passed through unchanged.
 *
 * <P>Two <i>init</i> arguments are supported. The first (mandatory) sets 
 * the threshold value, below which the input should be squelched. The second
 * argument (optional) defines the output value to be used when the squelch
 * is in effect. If the second argument is not specified then 0.0 is used.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class
TranslationSquelch
extends Translation
{
  protected static String itsArgs[] = new String[]{
    "Translation - Squelch", "Apply a threshold function",
    "Threshold", "java.lang.Double",
    "Output", "java.lang.Double"};

  /** The threshold value. */
  private double itsThreshold;
  
   /** The output value when squelched. */
  private double itsSquelchOutput;

  public
  TranslationSquelch(PointDescription parent, String[] init)
  {
    super(parent, init);

    if (init.length<1) {
      System.err.println("TranslationSquelch for \"" + parent.getName()
                         + "\": Expect ONE or TWO Arguments!");
      itsThreshold=1.0; //Make it up..
      itsSquelchOutput=0.0;
    } else if (init.length==1) {
      try {
        itsThreshold = Double.parseDouble(init[0]);
      } catch (NumberFormatException e) {
        System.err.println("TranslationSquelch for \"" + parent.getName()
                           + "\": " + e.getMessage());
        itsThreshold = 1.0; //Make it up..
      }
      itsSquelchOutput=0.0;
    } else {
      try {
        itsThreshold = Double.parseDouble(init[0]);
        itsSquelchOutput = Double.parseDouble(init[1]);
      } catch (NumberFormatException e) {
        System.err.println("TranslationSquelch for \"" + parent.getName()
                           + "\": " + e.getMessage());
        itsThreshold = 1.0; //Make it up..
        itsSquelchOutput=0.0;
      }
    }
  }


  /** Do the translation. */
  public
  PointData
  translate(PointData data)
  {
    if (data==null) {
      return null;
    }

    Object rawval = data.getData();

    //Need to make new object with our parent as source/name
    PointData res = new PointData(itsParent.getName(),
				  itsParent.getSource(),
				  data.getTimestamp(),
				  data.getRaw(), null);

    Object newval = null;

    if (rawval==null) {
      newval = null;
    } else if (rawval instanceof Number) {
      double temp = ((Number)rawval).doubleValue();
      if (temp<itsThreshold) {
        newval=new Double(itsSquelchOutput);
      } else {
        newval=new Double(temp);
      }
    } else {
      //It's an unknown data type
      System.err.println("TranslationSquelch for \"" + itsParent.getName()
                         + "\": UNEXPECTED CLASS: " + rawval.getClass());
      return null;
    }

    //Save the result
    res.setData(newval);
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
