// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.3.0

package atnf.atoms.mon.comms;

import java.util.Vector;
import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;

public final class MoniCAIceI extends _MoniCAIceDisp
{
    public
    MoniCAIceI()
    {
    }

    public boolean
    addPoints(PointDescriptionIce[] newpoints, String username, String passwd, Ice.Current __current)
    {
      return false;
    }

    public boolean
    addSetup(String setup, String username, String passwd, Ice.Current __current)
    {
      return false;
    }

    /** Get the names of all points on the system (including aliases). */
    public String[]
    getAllPointNames(Ice.Current __current)
    {
      return PointDescription.getAllPointNames();
    }

    /** Get all unique points on the system. */
    public PointDescriptionIce[]
    getAllPoints(Ice.Current __current)
    {
      //Get all unique points
      PointDescription[] points = PointDescription.getAllUniquePoints();
      return MoniCAIceUtil.getPointDescriptionsAsIce(points);
    }
    
    
    public PointDescriptionIce[] 
    getPoints(String[] names, Ice.Current __current) 
    {
      PointDescriptionIce[] res = new PointDescriptionIce[names.length];
      for (int i=0; i<names.length; i++) {
        PointDescription thispoint = PointDescription.getPoint(names[i]);
        if (thispoint!=null) {
          res[i]=MoniCAIceUtil.getPointDescriptionAsIce(thispoint);
        } else {
          res[i]=null;
        }
      }
      return res;
    }
    
    
    public String[]
    getAllSetups(Ice.Current __current)
    {
      SavedSetup[] allsetups = MonitorMap.getAllSetups();
      String[] stringsetups = new String[allsetups.length];
      for (int i=0; i<allsetups.length; i++) {
        stringsetups[i]=allsetups[i].toString();
      }
      return stringsetups;
    }

    /** Return historical data for the specified points. */
    public PointDataIce[][]
    getArchiveData(String[] names, long start, long end, long maxsamples, Ice.Current __current)
    {
      AbsTime absstart=AbsTime.factory(start);
      AbsTime absend=AbsTime.factory(end);
      PointDataIce[][] res = new PointDataIce[names.length][];
      for (int i=0; i<names.length; i++) {
        Vector<PointData> thisdata = PointBuffer.getPointData(names[i], absstart, absend, (int)maxsamples);
        res[i]=MoniCAIceUtil.getPointDataAsIce(thisdata);
      }
      return res;
    }

    /** Return the current time on the server. */
    public long
    getCurrentTime(Ice.Current __current)
    {
      return (new AbsTime()).getValue();
    }

    /** Return the latest values for the given points. */
    public PointDataIce[]
    getData(String[] names, Ice.Current __current)
    {
      PointDataIce[] res = new PointDataIce[names.length];
      for (int i=0; i<names.length; i++) {
        PointData pd = PointBuffer.getPointData(names[i]);
        PointDataIce pdi;
        if (pd!=null) {
          pdi=MoniCAIceUtil.getPointDataAsIce(PointBuffer.getPointData(names[i]));
        } else {
          //No data available so create dummy data with null value
          pdi=MoniCAIceUtil.getPointDataAsIce(new PointData(names[i]));
        }
        res[i]=pdi;
      }
      return res;
    }

    public String[]
    getEncryptionInfo(Ice.Current __current)
    {
        return null;
    }

    public boolean
    setData(String[] names, PointDataIce[] rawvalues, String username, String passwd, Ice.Current __current)
    {
      return false;
    }

  /** Start the server on the default port. */
  public static void startIceServer() {
    startIceServer(MoniCAIceUtil.getDefaultPort());
  }
    
  /** Start the server. */
  public static void startIceServer(int port) {
    Ice.Communicator ic = null;
    try {
      ic = Ice.Util.initialize();
      Ice.ObjectAdapter adapter = ic.createObjectAdapterWithEndpoints("MoniCAIceAdapter", "tcp -p " + port);
      Ice.Object object = new MoniCAIceI();
      adapter.add(object, ic.stringToIdentity("MoniCAIce"));
      adapter.activate();
      ic.waitForShutdown();
    } catch (Ice.LocalException e) {
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    if (ic != null) {
      try {
        ic.destroy();
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
    System.err.println("MoniCAIceI.startIceServer(): ERROR Ice Server Exited!");
  }
}
