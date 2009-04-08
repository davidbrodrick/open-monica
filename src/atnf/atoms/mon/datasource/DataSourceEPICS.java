// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.io.*;
import java.util.*;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;

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
public class DataSourceEPICS
extends DataSource
{
  /** JCA context. */
  Context itsContext = null;
  
  /** Mapping between PV's and monitor points. */
  HashMap itsPVPointMap = new HashMap();
  
  /** Mapping between PV names and Channels. */
  HashMap itsChannelMap = new HashMap();

  public DataSourceEPICS(String[] args)
  {
    //Try to read the list of points to monitor
    InputStream mapfile = DataSourceEPICS.class.getClassLoader().getResourceAsStream(args[0]);
    System.err.println("DataSourceEPICS: Point map is " + mapfile);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(mapfile));
      while (reader.ready()) {
        String thisline=reader.readLine();
        if (thisline.length()==0 || thisline.startsWith("#")) {
          continue; //Comment line or blank line
        }
        StringTokenizer st=new StringTokenizer(thisline);
        if (st.countTokens()!=2) {
          MonitorMap.logger.warning("DataSourceEPICS: Parse error with line \"" + thisline + "\" of " + mapfile);
          continue;
        }
        itsPVPointMap.put(st.nextToken(), st.nextToken());
      }
      reader.close();
    } catch (Exception e) {
      MonitorMap.logger.error("DataSourceEPICS: Could not read PV/point map " + mapfile);
    }

    //Get the JCALibrary instance.
    JCALibrary jca=JCALibrary.getInstance();
    try {
      //Create context with default configuration values.
      itsContext=jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
    } catch (Exception e) {
      MonitorMap.logger.error("DataSourceEPICS: Constructor: " + e.getClass() + " " + e.getMessage());
    }

    //Create thread to connect channels
    try {
      ChannelConnector connector = new ChannelConnector();
      connector.start();
    } catch (Exception e) {
      MonitorMap.logger.error("DataSourceEPICS: Constructor: Connecting Channels: " + e.getClass() + " " + e.getMessage());
    }
    MonitorMap.logger.checkpoint("DataSourceEPICS: Initialised with \"" + itsPVPointMap.size() + "\" monitor points");
  }


  /** Dummy connect method. */
  public
  boolean
  connect()
  throws Exception
  {
    itsConnected=true;
    return itsConnected;
  }


  /** Dummy disconnect method. */
  public
  void
  disconnect()
  throws Exception
  {
    itsConnected  = false;
  }


  /** Dummy getData method. */
  protected
  void
  getData(Object[] points)
  throws Exception
  {
  }
  
  
  /** Nested threaded class to connect Channels as the IOCs and monitor points become available. */
  protected class ChannelConnector
  extends Thread {
    public void run() {
      //Thread continues to run until all channels connected
      while (itsPVPointMap.size()>itsChannelMap.size()) {
        Iterator allpvs = itsPVPointMap.keySet().iterator();
        while (allpvs.hasNext()) {
          //Get the name of the next PV
          String thispv = (String)allpvs.next();
          //Don't try to connect if it's already connected
          if (itsChannelMap.get(thispv)!=null) {
            continue;
          }
          //Don't try connect if monitor point hasn't been instantiated yet
          if (MonitorMap.getPointMonitor((String)itsPVPointMap.get(thispv))==null) {
            continue;
          }

          try {
            //Create the Channel to connect to the PV.
            Channel thischan=itsContext.createChannel(thispv);
            try {
              itsContext.pendIO(5.0);
            } catch (Exception e) {
              //Failed to connect: IOC probably isn't running yet
              System.err.println("DataSourceEPICS: Connecting Channel: " + thispv + e.getClass() + " " + e.getMessage());
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
            System.err.println("DataSourceEPICS: Connecting Channel " + thispv + ": " + e.getClass() + " " + e.getMessage());
            MonitorMap.logger.error("DataSourceEPICS: Connecting Channel " + thispv + ": " + e.getClass() + " " + e.getMessage());
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
    /** The source part of the monitor point name. */
    String itsSource = null;
    /** The name part of the monitor point name. */
    String itsName = null;
    /** The monitor point instance itself. */
    PointMonitor itsMonitorPoint = null;
    /** Channel which connects us to the point. */
    Channel itsChannel = null;
     
    /** Create new instance to handle updates to the specified PV. */
    public 
    MonUpdater(Channel chan)
    {
      itsChannel=chan;
      itsPV=itsChannel.getName();
      String mpname=(String)itsPVPointMap.get(itsPV);
      int dot=mpname.indexOf(".");
      itsSource=mpname.substring(0,dot);
      itsName=mpname.substring(dot+1, mpname.length());
      itsMonitorPoint=MonitorMap.getPointMonitor(itsSource+"."+itsName);
      
      //Try to perform a get on the channel
      try {
        DBR dbr=itsChannel.get();
        if (dbr!=null) {
          processDBR(dbr);
        }
      } catch (Exception e) {
        System.err.println("DataSourceEPICS:MonUpdater: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
      }
    }
    
    /** Call back for PV updates. */
    public
    void
    monitorChanged(MonitorEvent ev)
    {
      try {
        if (ev.getStatus()==CAStatus.NORMAL && ev.getDBR()!=null) {
          processDBR(ev.getDBR());
        } else {
          //Fire null data to indicate the problem
          PointData pd=new PointData(itsName, itsSource, null);
          if (itsMonitorPoint==null) {
            itsMonitorPoint=MonitorMap.getPointMonitor(itsSource+"."+itsName);
          }
          itsMonitorPoint.firePointEvent(new PointEvent(this, pd, true));
        }
      } catch (Exception e) {
        System.err.println("DataSourceEPICS:monitorChanged: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
        MonitorMap.logger.warning("DataSourceEPICS: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
      }
    }    
    
    /** Process a new DBR. */
    public
    void
    processDBR(DBR dbr)
    {
      try {
        int count=dbr.getCount();
        Object rawval=dbr.getValue();
        
        for (int i=0; i<count; i++) {
          //Process each new value
          Object newval=null;        
          //Have to switch on type, don't think there's any simpler way
          //to get the individual data as an object type.
          if (dbr.getType()==DBRType.INT) {
            newval=new Integer(((int[])rawval)[i]);
          } else if (dbr.getType()==DBRType.BYTE) {
            newval=new Integer(((byte[])rawval)[i]);
          } else if (dbr.getType()==DBRType.SHORT) {
            newval=new Integer(((short[])rawval)[i]);
          } else if (dbr.getType()==DBRType.FLOAT) {
            newval=new Float(((float[])rawval)[i]);
          } else if (dbr.getType()==DBRType.DOUBLE) {
            newval=new Double(((double[])rawval)[i]);
          } else if (dbr.getType()==DBRType.STRING) {
            newval=((String[])rawval)[i];
          } else if (dbr.getType()==DBRType.ENUM) {
            newval=new Integer(((short[])rawval)[i]);
          } else {
            System.err.println("DataSourceEPICS:processDBR: " + itsPV + ": Unhandled DBR type: " + dbr.getType());
            MonitorMap.logger.warning("DataSourceEPICS: " + itsPV + ": Unhandled DBR type: " + dbr.getType());
          }

          //System.out.println(itsPV + "\t" + newval);

          //Fire new data as an event on our monitor point
          PointData pd=new PointData(itsName, itsSource, newval);
          if (itsMonitorPoint==null) {
            itsMonitorPoint=MonitorMap.getPointMonitor(itsSource+"."+itsName);
          }
          if (itsMonitorPoint!=null) {
            itsMonitorPoint.firePointEvent(new PointEvent(this, pd, true));
          }
        }
      } catch (Exception e) {
        System.err.println("DataSourceEPICS:processDBR: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
        MonitorMap.logger.warning("DataSourceEPICS: " + itsPV + ": " + e.getClass() + ": " + e.getMessage());
        e.printStackTrace();
      }
    }
  };  
  
  /** Collect data and display to STDOUT. */
  public final static
  void
  main(String[] argv)
  {
    DataSourceEPICS epics = new DataSourceEPICS(null);
  }
}
