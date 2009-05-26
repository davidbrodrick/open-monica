//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.HashMap;
import atnf.atoms.mon.*;
import atnf.atoms.mon.comms.MoniCAServerASCII;
import atnf.atoms.mon.comms.MoniCAServerCustom;
import atnf.atoms.time.*;

/**
 * Used to make information about the monitor system itself available
 * for monitoring. Run-time information is all stored in a Named Value
 * List so that it can be extracted by listening points. The keys in the
 * HashMap include:
 *
 * <bl>
 * <li><b>TotalMemory</b> Memory used by monitor system (MB)
 * <li><b>NumClntsJ</b> Number of Java clients connected
 * <li><b>NumClntsA</b> Number of ASCII clients connected
 * <li><b>TimeUTC</b> Current UTC time
 * </bl>
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
class MonitorSystem
extends ExternalSystem
{
  /** Constructor */
  public
  MonitorSystem(String[] tokens)
  {
    super("MonitorSystem");
  }

  /** Get the latest information. */
  protected
  void
  getData(PointDescription[] points)
  throws Exception
  {
    Object[] buf = points;
   
    try {
      //Increment transaction counter
      itsNumTransactions += buf.length;

      for (int i=0; i<buf.length; i++) {
        PointDescription pm = (PointDescription)buf[i];
        HashMap<String,Object> res = new HashMap<String,Object>();

        //Get the actual data
        res.put("TotalMemory", new Float((float)(MonitorMap.getTotalMemory())/(1024*1024)));
        res.put("NumClntsJ", new Integer(MoniCAServerCustom.getNumClients()));
        res.put("NumClntsA", new Integer(MoniCAServerASCII.getNumClients()));
        res.put("TimeUTC", new AbsTime());

        pm.firePointEvent(new PointEvent(this, new
                          PointData(pm.getName(), AbsTime.factory(),
                                    res), true));
      }
    } catch (Exception e) {
      System.err.println("MonitorSystem: " + e);     
    }
  }
}
