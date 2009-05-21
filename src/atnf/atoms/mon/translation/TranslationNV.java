// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import java.util.HashMap;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.NamedValueList;

/**
 * Returns a particular entry from a NamedValueList or HashMap.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 **/
public class
TranslationNV
extends Translation
{
   String itsName = null;
   
   protected static String[] itsArgs = new String[]{"Translation NamedValue",
   "NV", "Name in NamedValue", "java.lang.String"};
   
   public
   TranslationNV(PointDescription parent, String[] init)
   {
     super(parent, init);
     if (init.length!=1) {
       System.err.println("ERROR: TranslationNV (for " + itsParent.getName()
			  + "): Expect 1 Argument.. got " + init.length);
     } else {
       itsName = init[0];
     }
   }
   
   public
   PointData
   translate(PointData data)
   {
     //Precondition
     if (data==null) {
      return null;
    }

     //Get the actual data
     Object realdata = data.getData();

     //If the data is null we need to throw a null-data result
     if (realdata==null) {
       return new PointData(itsParent.getFullName());
     }

     //Ensure it is a valid type for this class
     if (!(realdata instanceof NamedValueList) &&
	 !(realdata instanceof HashMap)) {
       System.err.print("ERROR: TranslationNV (for " + itsParent.getName() + "): "
			  + "EXPECT NVL or HashMap got " + realdata.getClass());
       return null;
     }

     //Create the new data structure to be returned
     PointData res = new PointData(itsParent.getFullName());

     res.setData((realdata instanceof NamedValueList) ?
		 ((NamedValueList)realdata).get(itsName) :
                 ((HashMap)realdata).get(itsName));

     //Keep the time-stamp of the parent point rather than use "now"
     res.setTimestamp(data.getTimestamp());

     return res;
   }
   
   public static String[] getArgs()
   {
     return itsArgs;
   }
}
