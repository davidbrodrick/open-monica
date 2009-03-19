//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;

/**
 * Map between input strings and an output strings. Init arguments must be in
 * form "input1:output1""input2:output2", eg "true:ok""false:DISCONNECTED".
 *
 * <P>Any unrecognised input are passed through without alteration.
 *
 * @author David Brodrick
 * @version $Id: TranslationStringMap.java,v 1.1 2004/11/01 04:46:46 bro764 Exp $
 */
public class
TranslationStringMap
extends Translation
{
  /** Contains the mapping from inputs to outputs. */
  protected HashMap itsMappings = new HashMap();

  protected static String[] itsArgs = new String[]{"Translation String Map",
  "String Map"};

  public TranslationStringMap(PointMonitor parent, String[] init)
  {
    super(parent, init);

    for (int i=0; i<init.length; i++) {
      //Check for the :
      int colonindex = init[i].indexOf(":");
      if (colonindex==-1) {
	System.err.println("TranslationStringMap: \"" + parent.getName() +
			   "\" Expected \"input_string:output_string\"");
	continue;
      }
      String inp = init[i].substring(0, colonindex);
      String out = init[i].substring(colonindex+1);
      //Save the mapping for later use
      itsMappings.put(inp, out);
    }
  }


  /** Map the input data to an output string. */
  public
  PointData
  translate(PointData data)
  {
    //preconditions
    if (data==null) return null;
    Object val = data.getData();

    //If we got null-data then throw a null-data result
    if (val==null) {
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    //Get value as a string
    String strval = val.toString();
    //Do a lookup on the string value
    String newstr = (String)itsMappings.get(strval);

    if (newstr==null) newstr = strval;

    //Create return structure with right details
    PointData res = new PointData(itsParent.getName(), itsParent.getSource());
    //Call the sub-class method with possibly null value and save result
    res.setData(newstr);
    //Preserve the original data value
    res.setRaw(val);

    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
