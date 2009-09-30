// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
// Copyright (C) 2009 CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.util.Vector;
import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;

/** Concrete implementation of the Ice server for MoniCA. 
 * 
 * @author David Brodrick */
public final class MoniCAIceI extends _MoniCAIceDisp
{
  public MoniCAIceI() {
  }

  /** Add the new points to the system. */
  public boolean addPoints(PointDescriptionIce[] newpoints, String encname, String encpass, Ice.Current __current) {
    //Decrypt the user's credentials
    String username = KeyKeeper.decrypt(encname);
    String password = KeyKeeper.decrypt(encpass);
    //TODO: Currently does nothing    
    return false;
  }

  /** Get the names of all points on the system (including aliases). */
  public String[] getAllPointNames(Ice.Current __current) {
    return PointDescription.getAllPointNames();
  }

  /** Get all unique points on the system. */
  public PointDescriptionIce[] getAllPoints(Ice.Current __current) {
    //Get all unique points
    PointDescription[] points = PointDescription.getAllUniquePoints();
    return MoniCAIceUtil.getPointDescriptionsAsIce(points);
  }

  /** Return the definitions for the specified points. */
  public PointDescriptionIce[] getPoints(String[] names, Ice.Current __current) {
    PointDescriptionIce[] res = new PointDescriptionIce[names.length];
    for (int i = 0; i < names.length; i++) {
      PointDescription thispoint = PointDescription.getPoint(names[i]);
      if (thispoint != null) {
        res[i] = MoniCAIceUtil.getPointDescriptionAsIce(thispoint);
      } else {
        res[i] = null;
      }
    }
    return res;
  }

  /** Add the new setup to the system. */
  public boolean addSetup(String setup, String encname, String encpass, Ice.Current __current) {
    //Decrypt the user's credentials
    String username = KeyKeeper.decrypt(encname);
    String password = KeyKeeper.decrypt(encpass);
    //TODO: Currently does nothing
    return false;
  }

  /** Return the string representation of all saved setups stored on the server. */
  public String[] getAllSetups(Ice.Current __current) {
    SavedSetup[] allsetups = MonitorMap.getAllSetups();
    String[] stringsetups = new String[allsetups.length];
    for (int i = 0; i < allsetups.length; i++) {
      stringsetups[i] = allsetups[i].toString();
    }
    return stringsetups;
  }

  /** Return historical data for the specified points. */
  public PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples, Ice.Current __current) {
    AbsTime absstart = AbsTime.factory(start);
    AbsTime absend = AbsTime.factory(end);
    PointDataIce[][] res = new PointDataIce[names.length][];
    for (int i = 0; i < names.length; i++) {
      Vector<PointData> thisdata = PointBuffer.getPointData(names[i], absstart, absend, (int) maxsamples);
      res[i] = MoniCAIceUtil.getPointDataAsIce(thisdata);
    }
    return res;
  }

  /** Return the latest values for the given points. */
  public PointDataIce[] getData(String[] names, Ice.Current __current) {
    PointDataIce[] res = new PointDataIce[names.length];
    for (int i = 0; i < names.length; i++) {
      PointData pd = PointBuffer.getPointData(names[i]);
      PointDataIce pdi;
      if (pd != null) {
        pdi = MoniCAIceUtil.getPointDataAsIce(PointBuffer.getPointData(names[i]));
      } else {
        //No data available so create dummy data with null value
        pdi = MoniCAIceUtil.getPointDataAsIce(new PointData(names[i]));
      }
      res[i] = pdi;
    }
    return res;
  }

  /** Set new values for the specified points. */
  public boolean setData(String[] names, PointDataIce[] rawvalues, String encname, String encpass, Ice.Current __current) {
    if (names.length!=rawvalues.length) {
      return false;
    }
    int numpoints=names.length;
    
    // Decrypt the user's credentials
    /// Doesn't presently do anything with user's credentials
    String username = KeyKeeper.decrypt(encname);
    String password = KeyKeeper.decrypt(encpass);
    
    // Process each of the control requests consecutively
    boolean result = true;    
    Vector<PointData> values = MoniCAIceUtil.getPointDataFromIce(rawvalues);
    for (int i=0; i<numpoints; i++) {
      try {
        // Get the specified point
        PointDescription thispoint = PointDescription.getPoint(names[i]);
        if (thispoint==null) {
          MonitorMap.logger.warning("MoniCAIceI.setData: Point " + names[i] + " does not exist");
          result = false;
          continue;
        }
        // Act on the new data value
        thispoint.firePointEvent(new PointEvent(this, values.get(i), true));
      } catch (Exception e) {
        MonitorMap.logger.warning("MoniCAIceI.setData: Processing " + names[i] + ": " + e);
        result = false;
      }
    }
    
    return result;
  }

  /** Return the key and modulus required to send encrypted data to the server. */
  public String[] getEncryptionInfo(Ice.Current __current) {
    String[] a = new String[2];
    a[0] = KeyKeeper.getPublicKey();
    a[1] = KeyKeeper.getModulus();
    return a;
  }

  /** Return the current time on the server. */
  public long getCurrentTime(Ice.Current __current) {
    return (new AbsTime()).getValue();
  }  
  
  /** Start the server on the default port. */
  public static void startIceServer() {
    startIceServer(MoniCAIceUtil.getDefaultPort());
  }

  /** Start the server. */
  public static void startIceServer(int port) {
    MoniCAIceServerThread server = new MoniCAIceServerThread(port);
    server.start();
  }

  /** Start a new thread to run the server. */
  public static class MoniCAIceServerThread extends Thread {
    protected int itsPort;

    public MoniCAIceServerThread(int port) {
      super("MoniCAIceServer");
      itsPort = port;
    }

    public void run() {
      Ice.Communicator ic = null;
      try {
        ic = Ice.Util.initialize();
        Ice.ObjectAdapter adapter = ic.createObjectAdapterWithEndpoints("MoniCAIceAdapter", "tcp -p " + itsPort);
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
  };
}
