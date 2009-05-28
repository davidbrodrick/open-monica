// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.io.*;
import java.util.*;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;

/**
 * Expects file with space delimited PV/monitor point name mapping to be 
 * included as part of argument.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class EPICS
extends ExternalSystem
{
  /** JCA context. */
  Context itsContext = null;
  
  /** Mapping between PV names and MoniCA point names. */
  HashMap<String,String> itsPVPointMap = new HashMap<String,String>();
  
  /** Mapping between PV names and Channels. */
  HashMap<String,Channel> itsChannelMap = new HashMap<String,Channel>();

  public EPICS(String[] args)
  {
    super("EPICS");
    
    //Get the JCALibrary instance.
    JCALibrary jca=JCALibrary.getInstance();
    try {
      //Create context with default configuration values.
      itsContext=jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
    } catch (Exception e) {
      MonitorMap.logger.error("EPICS: Constructor: " + e.getClass() + " " + e.getMessage());
    }

    //Create thread to connect channels
    try {
      ChannelConnector connector = new ChannelConnector();
      connector.start();
    } catch (Exception e) {
      MonitorMap.logger.error("EPICS: Constructor: Connecting Channels: " + e.getClass() + " " + e.getMessage());
    }
  }

  /** Poll new values from EPICS. */
  protected
  void
  getData(PointDescription[] points)
  throws Exception
  {
    //Process each point in turn
    for (int i=0; i<points.length; i++) {
      //Get the appropriate Transaction(s) and process each PV
      Vector<Transaction> thesetrans = getMyTransactions(points[i].getInputTransactions());
      for (int j=0; j<thesetrans.size(); j++) {
        //Get this Transaction which contains the PV name
        TransactionEPICS thistrans = (TransactionEPICS)thesetrans.get(j);
        String pvname = thistrans.getPVName();
        //Lookup the channel connected to this PV
        Channel thischan = itsChannelMap.get(pvname);
        if (thischan==null) {
          //We haven't connected to this channel yet. Try to connect.
          thischan=itsContext.createChannel(pvname);
          try {
            itsContext.pendIO(5.0);
            itsChannelMap.put(pvname, thischan);
          } catch (Exception e) {
            //Failed to connect: IOC probably isn't running yet
            thischan.destroy();
            thischan=null;
          }
        }
        Object newval = null;
        if (thischan!=null) {
          try {
            DBR dbr = thischan.get();
            if (dbr != null) {
              newval = processDBR(dbr, pvname);
            }
          } catch (Exception e) {
            MonitorMap.logger.error("EPICS.getData: " + pvname + ": " + e.getClass() + ": " + e.getMessage());
          }
        }
        // Fire new data as an event on this monitor point
        PointData pd = new PointData(itsName, newval);
        points[i].firePointEvent(new PointEvent(this, pd, true));
      }
    }
  }

  /** Send a value from MoniCA to EPICS. */ 
  public
  void
  putData(PointDescription desc, PointData pd)
  throws Exception
  {
    
    System.err.println("EPICS: Unsupported control request from " + desc.getFullName());
  }
  
  /** Register the specified point and PV name pair to receive monitor updates. */
  public void registerMonitor(String pointname, String pvname)
  {
    itsPVPointMap.put(pvname, pointname);
    itsChannelMap.put(pvname, null);
  }

  /** Nested threaded class to connect Channels as the IOCs and monitor points become available. */
  protected class ChannelConnector
  extends Thread {
    public void run() {
      //Thread continues to run connecting any points which require it
      while (true) {
        Iterator allpvs = itsPVPointMap.keySet().iterator();
        while (allpvs.hasNext()) {
          //Get the name of the next PV
          String thispv = (String)allpvs.next();
          //Don't try to connect if it's already connected
          if (itsChannelMap.get(thispv)!=null) {
            continue;
          }
          //Don't try connect if point hasn't been instantiated yet
          if (PointDescription.getPoint((String)itsPVPointMap.get(thispv))==null) {
            continue;
          }

          try {
            //Create the Channel to connect to the PV.
            Channel thischan=itsContext.createChannel(thispv);
            try {
              itsContext.pendIO(5.0);
            } catch (Exception e) {
              //Failed to connect: IOC probably isn't running yet
              System.err.println("EPICS: Connecting Channel: " + thispv + e.getClass() + " " + e.getMessage());
              thischan.destroy();
              continue;
            }
        
            //Create MonUpdater instance to handle updates from this particular point
            MonUpdater updater = new MonUpdater(thischan);
            //Subscribe to updates from this PV
            thischan.addMonitor(Monitor.VALUE, updater);
        
            //Keep this association
            itsChannelMap.put(thispv, thischan);
          } catch (Exception e) {
            System.err.println("EPICS: Connecting Channel " + thispv + ": " + e.getClass() + " " + e.getMessage());
            MonitorMap.logger.error("EPICS: Connecting Channel " + thispv + ": " + e.getClass() + " " + e.getMessage());
          }
        }
        
        try {
          //Sleep for a while before trying to connect remaining channels
          RelTime sleepy=RelTime.factory(5000000);
          sleepy.sleep();
        } catch (Exception e) {}
      }
    }
  };
  

  /** Nested class which received Monitor events for a specific process variable. */
  protected class MonUpdater
  implements MonitorListener
  {
    /** The process variable we handle events for. */
    String itsPV = null;
    /** The point name. */
    String itsName = null;
    /** The monitor point instance itself. */
    PointDescription itsMonitorPoint = null;
    /** Channel which connects us to the point. */
    Channel itsChannel = null;
     
    /** Create new instance to handle updates to the specified PV. */
    public 
    MonUpdater(Channel chan)
    {
      itsChannel=chan;
      itsPV=itsChannel.getName();
      itsName=(String)itsPVPointMap.get(itsPV);
      itsMonitorPoint=PointDescription.getPoint(itsName);
      
      if (itsMonitorPoint != null) {
        // Try to perform an initial get on the channel
        try {
          DBR dbr = itsChannel.get();
          if (dbr != null) {
            Object newval = processDBR(dbr, itsPV);
            // Fire new data as an event on our monitor point
            PointData pd = new PointData(itsName, newval);
            itsMonitorPoint.firePointEvent(new PointEvent(this, pd, true));
          }
        } catch (Exception e) {
          MonitorMap.logger.error("EPICS:MonUpdater: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
        }
      }
    }
    
    /** Call back for PV updates. */
    public
    void
    monitorChanged(MonitorEvent ev)
    {
      try {
        if (ev.getStatus()==CAStatus.NORMAL && ev.getDBR()!=null) {
          Object newval = processDBR(ev.getDBR(), itsPV);
          //Fire new data as an event on our monitor point
          PointData pd=new PointData(itsName, newval);
          if (itsMonitorPoint==null) {
            itsMonitorPoint=PointDescription.getPoint(itsName);
          }
          if (itsMonitorPoint!=null) {
            itsMonitorPoint.firePointEvent(new PointEvent(this, pd, true));
          }        
        } else {
          //Fire null data to indicate the problem
          PointData pd=new PointData(itsName);
          if (itsMonitorPoint==null) {
            itsMonitorPoint=PointDescription.getPoint(itsName);
          }
          itsMonitorPoint.firePointEvent(new PointEvent(this, pd, true));
        }
      } catch (Exception e) {
        System.err.println("EPICS:monitorChanged: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
        MonitorMap.logger.warning("EPICS: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
      }
    }    
  };  
  
  /** Decode the value from an EPICS DBR. */
  public
  Object
  processDBR(DBR dbr, String pvname)
  {
    Object newval = null;
    try {
      int count = dbr.getCount();
      if (count > 1) {
        MonitorMap.logger.warning("EPICS.processDBR: " + pvname + ": >1 value received");
      }
      Object rawval = dbr.getValue();
      // Have to switch on type, don't think there's any simpler way
      // to get the individual data as an object type.
      if (dbr.getType() == DBRType.INT) {
        newval = new Integer(((int[]) rawval)[0]);
      } else if (dbr.getType() == DBRType.BYTE) {
        newval = new Integer(((byte[]) rawval)[0]);
      } else if (dbr.getType() == DBRType.SHORT) {
        newval = new Integer(((short[]) rawval)[0]);
      } else if (dbr.getType() == DBRType.FLOAT) {
        newval = new Float(((float[]) rawval)[0]);
      } else if (dbr.getType() == DBRType.DOUBLE) {
        newval = new Double(((double[]) rawval)[0]);
      } else if (dbr.getType() == DBRType.STRING) {
        newval = ((String[]) rawval)[0];
      } else if (dbr.getType() == DBRType.ENUM) {
        newval = new Integer(((short[]) rawval)[0]);
      } else {
        MonitorMap.logger.warning("EPICS.processDBR: " + pvname + ": Unhandled DBR type: " + dbr.getType());
      }

      System.out.println("EPICS.processDBR: " + pvname + "\t" + newval);
    } catch (Exception e) {
      MonitorMap.logger.warning("EPICS: " + pvname + ": " + e.getClass() + ": " + e.getMessage());
      e.printStackTrace();
    }
    return newval;
  }  
}
