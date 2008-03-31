//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.limit;

import java.lang.reflect.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;


/**
 * Base-class for classes which check the value of a monitor point. The
 * basic theory is that monitor points normally have some nominal value,
 * or set of values, and the user may need to be alerted when the value
 * strays outside of this nominal set. The approach taken in this
 * implementation is that a <i>PointLimit</i> sub-class will be able to
 * check a point's current value and indicate whether the value is within
 * the set of nominal values, or if it has strayed from the nominal set.
 * <P>
 * This is realised by implementing an appropriate <i>checkLimits</i>
 * method for the particular sub-class. <i>checkLimits</i> should return
 * <tt>True</tt> when the point's value is okay and <tt>False</tt> when
 * an alert should be raised.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: PointLimit.java,v 1.3 2004/09/14 22:57:10 bro764 Exp $
 **/
public abstract class
PointLimit
extends MonitorPolicy
{
   /** Override this. Use format fullname, shortname, argname, argtype, argname, argtype, ...
   */
   protected static String itsArgs[] = new String[]{"PointLimit",""};
   
   public static PointLimit factory(String arg)
   {
      if (arg.equalsIgnoreCase("null")) arg= "-";

      PointLimit result = null;

      // Find the specific informations
      String specifics = arg.substring(arg.indexOf("-") + 1);
      String[] limitArgs = MonitorUtils.tokToStringArray(specifics);
      // Find the type of translation
      String type = arg.substring(0, arg.indexOf("-"));
      if (type == "" || type == null || type.length()<1) type = "SIMPLE";

      try {
	 Constructor Limit_con = Class.forName("atnf.atoms.mon.limit.PointLimit"+type).getConstructor(new Class[]{String[].class});
	 result = (PointLimit)(Limit_con.newInstance(new Object[]{limitArgs}));
      } catch (Exception e) {e.printStackTrace();result = new PointLimitSIMPLE(new String[]{});}

      result.setStringEquiv(arg);
      
      return result;
   }


   /** Check the value against the limits. This is the sub-class specific
    * algorithm.
    * @param data New data value to check.
    * @return <tt>True</tt> if the value is okay, <tt>False</tt> if the
    *  value is not okay.
    */
   public abstract
   boolean
   checkLimits(PointData data);


   public static String[] getArgs()
   {
      return itsArgs;
   }
}
