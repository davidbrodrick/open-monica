//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import java.util.HashMap;

import atnf.atoms.mon.MonitorMap;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.time.AbsTime;

/**
 * This Translation checks if a set of points are all within their defined
 * <i>limits</i>. If the values are okay then the output of
 * this translation will be the first <i>init</i> argument (eg which might
 * be "okay"). If one or more of the values it outside it's nominated
 * limits then the second <i>init</i> argument is used as output (eg it
 * might say "alarm"). All the other init arguments are the names of the
 * points to listen to.
 *
 * @author David Brodrick
 * @version $Id: TranslationLimitCheck.java,v 1.3 2005/10/18 03:37:53 bro764 Exp bro764 $
 */
public class
TranslationLimitCheck
extends Translation
{
  protected static String itsArgs[] = new String[]{"Translation limit check", "foo",
  "okay string", "ok string", "fail string", "fail string", "point name 1",
  "point name 1"};

  /** String to use as output when all points are okay. */
  protected String itsOutput1 = null;

  /** String to use as output when one or more points are out of limits. */
  protected String itsOutput2 = null;

  /** In-limits indicator for each listened-to point. A <tt>True</tt>
   * value indicates that the last update from the point had an okay
   * value. A <tt>False</tt> indicates the last update had an abnormal
   * value. A <tt>null</tt> entry indicates we have no valid data for that
   * point. The order of this array is the same as the <i>itsPoints</i>
   * field. */
  //Boolean[] itsLastValues = null;
  HashMap itsLastValues = new HashMap();


  public
  TranslationLimitCheck(PointDescription parent, String[] init)
  {
    super(parent, init);

    if (init.length!=2) {
      System.err.println("TranslationLimitCheck (" + parent.getName() +
			 "): Need 2 arguments!");
    } else {
      itsOutput1 = init[0]; //String output for when data's okay
      itsOutput2 = init[1]; //String output for when data's not okay
    }
  }


  public
  PointData
  translate(PointData data)
  {
    //Not interested in null arguments..
    if (data==null) {
      return null;
    }

    //Get the name of the monitor point that this data comes from
    String dsrc = data.getSource() + "." + data.getName();
    //Get the monitor point object that this data comes from
    PointDescription pm = MonitorMap.getPointDescription(dsrc);
    if (pm==null) {
      System.err.println("TranslationLimitCheck: pm IS NULL!!");
      return null;
    }

    //If data is null then clear the entry in the hashtable
    if (data.getData()==null) {
      itsLastValues.put(dsrc, null);
    } else {
	//Check the value of this point and set hashtable
	if (pm.checkLimits(data)) {
    itsLastValues.put(dsrc, new Boolean(true));
  } else {
    itsLastValues.put(dsrc, new Boolean(false));
  }
    }

    //If it's not time to produce another output, then just return
    AbsTime now  = AbsTime.factory();
    AbsTime next = AbsTime.factory(itsParent.getNextEpoch());
    if (now.isBefore(next) && !next.isASAP()) {
      return null;
    }

    //Create return structure with right details
    PointData res = new PointData(itsParent.getName(), itsParent.getSource());

    //Get current values as an array
    Object[] values = itsLastValues.values().toArray();

    if (values==null || values.length==0) {
      res.setData(null);
    } else {
      //Check the status of each of our listened-to points
      boolean gotvalues = true;
      boolean notokay   = false;
      for (int i=0; i<values.length; i++) {
	if (values[i]==null) {
    gotvalues = false;
  } else if (!((Boolean)values[i]).booleanValue()) {
    notokay = true;
  }
      }

      if (notokay) {
        res.setData(itsOutput2); //Warning warning!
      } else if (!gotvalues) {
        res.setData(null); //Missing some data
      } else {
        res.setData(itsOutput1); //Everything's cool man!
      }
    }
    return res;
  }


  public static String[] getArgs()
  {
    return itsArgs;
  }
}
