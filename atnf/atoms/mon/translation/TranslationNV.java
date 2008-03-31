// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;

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
   TranslationNV(PointMonitor parent, String[] init)
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
     if (data==null) return null;

     //Get the actual data
     Object realdata = data.getData();

     //If the data is null we need to throw a null-data result
     if (realdata==null) {
       return new PointData(itsParent.getName(), itsParent.getSource());
     }

     //Ensure it is a valid type for this class
     if (!(realdata instanceof NamedValueList) &&
	 !(realdata instanceof HashMap)) {
       System.err.print("ERROR: TranslationNV (for " + itsParent.getName() + "): "
			  + "EXPECT NVL or HashMap got " + realdata.getClass());
       return null;
     }

     //Create the new data structure to be returned
     PointData res = new PointData(itsParent.getName(), itsParent.getSource());

     res.setData((realdata instanceof NamedValueList) ?
		 ((NamedValueList)realdata).get(itsName) :
                 ((HashMap)realdata).get(itsName));

     //Keep the time-stamp of the parent point rather than use "now"
     res.setTimestamp(data.getTimestamp());

     //Remove the reference to the raw data. Most translations won't do this
     //but it is appropriate here because:
     //*NVL data is likely to be high-level to start with, so we don't need
     // to store raw, unprocessed values anyway.
     //*There will be huge transport and archive savings by doing this here
     res.setRaw(null);

     return res;
   }
   
   public static String[] getArgs()
   {
     return itsArgs;
   }
}
