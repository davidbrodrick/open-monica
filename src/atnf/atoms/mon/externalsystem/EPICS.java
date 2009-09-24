// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.*;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;

/**
 * Interface between MoniCA and EPICS Channel Access, allowing CA monitors
 * (publish/subscribe) updates and gets (polling).
 * 
 * <P>
 * Channel access monitors can be established by using a <tt>TransactionEPICSMonitor</tt>
 * as an input for the MoniCA point. The transaction requires one argument which is the
 * name of the EPICS Process Variable to be monitored.
 * 
 * <P>
 * Polling is implemented by using a <tt>TransactionEPICS</tt> as an input transaction.
 * This requires the name of the EPICS Process Variable to be polled as an argument and
 * also the DBRType if the Transaction is being used for control/write operations. Polling
 * will occur at the normal update period specified for the MoniCA point.
 * 
 * <P>
 * Both kinds of Transactions can take an optional argument if you need to collect the
 * data as a specific DBRType, eg "DBR_STS_STRING". Doing this at the STS level also
 * provides MoniCA with additional information such as the record's alarm severity and
 * allows UNDefined values to be recognised. If you do not specify a DBRType then
 * operations will be performed using the channel's native type, at the STS level.
 * 
 * @author David Brodrick
 */
public class EPICS extends ExternalSystem {
  /** JCA context. */
  Context itsContext = null;

  /** Mapping between PV names and Channels. */
  HashMap<String, Channel> itsChannelMap = new HashMap<String, Channel>();

  /** Mapping between 'pointname:PVname' strings and EPICSListeners. */
  HashMap<String, EPICSListener> itsListenerMap = new HashMap<String, EPICSListener>();

  /**
   * Lists of MoniCA points which require 'monitor' updates for each PV which hasn't been
   * connected yet.
   */
  HashMap<String, Vector<Vector<Object>>> itsRequiresMonitor = new HashMap<String, Vector<Vector<Object>>>();

  public EPICS(String[] args) {
    super("EPICS");

    // Get the JCALibrary instance.
    JCALibrary jca = JCALibrary.getInstance();
    try {
      // Create context with default configuration values.
      itsContext = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
    } catch (Exception e) {
      MonitorMap.logger.error("EPICS(constructor): Creating Context: " + e);
    }

    // Create thread to connect channels
    try {
      ChannelConnector connector = new ChannelConnector();
      connector.start();
    } catch (Exception e) {
      MonitorMap.logger.error("EPICS(constructor): Creating ChannelConnector: " + e);
    }
  }

  /**
   * Poll new values from EPICS. This first ensures the EPICS channel has been established
   * and subsequently performs an asynchronous 'get' on the channel, providing the data to
   * the MoniCA point when the 'get' callback is called.
   */
  protected void getData(PointDescription[] points) throws Exception {
    // Process each requesting point in turn
    for (int i = 0; i < points.length; i++) {
      // Get the appropriate Transaction(s) and process each PV
      Vector<Transaction> thesetrans = getMyTransactions(points[i].getInputTransactions());
      for (int j = 0; j < thesetrans.size(); j++) {
        // Get this Transaction which contains the PV name
        TransactionEPICS thistrans = (TransactionEPICS) thesetrans.get(j);
        String pvname = thistrans.getPVName();
        // Lookup the channel connected to this PV
        synchronized (itsChannelMap) {
          Channel thischan = itsChannelMap.get(pvname);
          if (thischan == null) {
            // We haven't connected to this channel yet so request its connection.
            itsChannelMap.put(pvname, null);
            // Fire a null-data update to indicate that data is not yet available.
            points[i].firePointEvent(new PointEvent(this, new PointData(points[i].getFullName()), true));
          } else {
            String listenername = points[i].getFullName() + ":" + pvname;
            EPICSListener listener = itsListenerMap.get(listenername);
            if (listener == null) {
              // This MoniCA point/PV combination hasn't been activated yet
              listener = new EPICSListener(thischan, points[i]);
              itsListenerMap.put(listenername, listener);
              thischan.addConnectionListener(listener);
            }

            if (thischan.getConnectionState() == Channel.ConnectionState.CONNECTED) {
              // Channel is connected, request data via a channel access 'get'
              try {
                points[i].isCollecting(true);
                DBRType type = thistrans.getType();

                // If the Transaction doesn't specify a particular DBRType, then
                // do an initial CA get to determine the native type of the record,
                // so that we can ensure all subsequent gets use an STS DBRType
                // which ensures alarm and validity information is available.
                if (type == null) {
                  DBR tempdbr = thischan.get();
                  itsContext.flushIO();
                  type = DBRType.forName(tempdbr.getType().getName().replaceAll("DBR_", "DBR_STS_").replaceAll("_ENUM", "_STRING"));
                  thistrans.setType(type);
                }

                // Do the actual CA get operation
                thischan.get(type, 1, listener);
              } catch (Exception e) {
                // Maybe the channel just became disconnected
                points[i].firePointEvent(new PointEvent(this, new PointData(points[i].getFullName()), true));
                points[i].isCollecting(false);
              }
            } else {
              // Channel exists but is currently disconnected. Fire null-data
              // event.
              points[i].firePointEvent(new PointEvent(this, new PointData(points[i].getFullName()), true));
            }
          }
        }
      }
    }
    try {
      // Flush all of the get requests
      itsContext.flushIO();
    } catch (Exception e) {
      MonitorMap.logger.error("EPICS.getData: Flushing IO: " + e);
    }
  }

  /** Send a value from MoniCA to EPICS. */
  public void putData(PointDescription desc, PointData pd) throws Exception {
    MonitorMap.logger.error("EPICS: Unsupported control request from " + desc.getFullName());

    try {
      itsContext.flushIO();
    } catch (Exception e) {
      MonitorMap.logger.error("EPICS.putData: Flushing IO: " + e);
    }
  }

  /** Register the specified point and PV name pair to receive monitor updates. */
  public void registerMonitor(PointDescription point, String pvname, DBRType type) {
    synchronized (itsChannelMap) {
      if (itsChannelMap.get(pvname) == null) {
        // Not connected to this channel yet
        itsChannelMap.put(pvname, null);
      }
    }
    synchronized (itsRequiresMonitor) {
      Vector<Vector<Object>> thesepoints = itsRequiresMonitor.get(pvname);
      if (thesepoints == null) {
        thesepoints = new Vector<Vector<Object>>(1);
        Vector<Object> newpoint = new Vector<Object>(2);
        newpoint.add(point);
        newpoint.add(type);
        thesepoints.add(newpoint);
        itsRequiresMonitor.put(pvname, thesepoints);
      } else {
        if (!thesepoints.contains(point)) {
          Vector<Object> newpoint = new Vector<Object>(2);
          newpoint.add(point);
          newpoint.add(type);
          thesepoints.add(newpoint);
        }
      }
    }
  }

  /**
   * Thread which connects to EPICS channels and configures 'monitor' updates for points
   * which request it.
   */
  protected class ChannelConnector extends Thread {
    public ChannelConnector() {
      super("EPICS ChannelConnector");
    }

    public void run() {
      while (true) {
        // Build a list of all channels which need connecting
        Vector<Channel> newchannels = new Vector<Channel>();
        synchronized (itsChannelMap) {
          Iterator allchans = itsChannelMap.keySet().iterator();
          while (allchans.hasNext()) {
            String thispv = (String) allchans.next();
            // Don't try to connect if it's already connected
            if (itsChannelMap.get(thispv) != null) {
              continue;
            }
            try {
              // Create the Channel to connect to the PV.
              Channel thischan = itsContext.createChannel(thispv);
              newchannels.add(thischan);
            } catch (Exception e) {
              MonitorMap.logger.warning("EPICS.ChannelConnector: Connecting Channel " + thispv + ": " + e);
            }
          }
        }

        // Try to connect to the channels
        try {
          itsContext.pendIO(5.0);
        } catch (Exception e) {
          // Failed to connect: IOC probably isn't running yet
        }

        // Check which channels connected successfully
        synchronized (itsChannelMap) {
          for (int i = 0; i < newchannels.size(); i++) {
            Channel thischan = newchannels.get(i);
            String thispv = thischan.getName();
            if (thischan.getConnectionState() == Channel.ConnectionState.CONNECTED) {
              // This channel connected okay
              itsChannelMap.put(thispv, thischan);
            } else {
              // This channel failed to connect
              try {
                thischan.destroy();
              } catch (Exception e) {
                MonitorMap.logger.error("EPICS: Destroying channel for " + thispv + ": " + e);
              }
            }
          }
        }

        // ////////////////////////////////////////////////////////
        // Connect any 'monitor' requests for established channels
        synchronized (itsRequiresMonitor) {
          Vector<String> allconnected = new Vector<String>();
          Iterator allreqs = itsRequiresMonitor.keySet().iterator();
          while (allreqs.hasNext()) {
            String thispv = (String) allreqs.next();
            Channel thischan = itsChannelMap.get(thispv);
            if (thischan == null) {
              // This channel is still not connected so cannot configure
              // monitors
              continue;
            }

            // Do an initial CA get to determine the native type of the record,
            // so that we can ensure all subsequent gets use an STS DBRType
            // which ensures alarm and validity information is available.
            DBRType nativetype = null;
            try {
              DBR tempdbr = thischan.get();
              itsContext.flushIO();
              nativetype = DBRType.forName(tempdbr.getType().getName().replaceAll("DBR_", "DBR_STS_")
                      .replaceAll("_ENUM", "_STRING"));
            } catch (Exception e) {
              MonitorMap.logger.error("EPICS.ChannelConnector: ERROR determining native DBRType for " + thispv);
              continue;
            }

            Vector<Vector<Object>> thesepoints = itsRequiresMonitor.get(thispv);
            if (thesepoints == null || thesepoints.size() == 0) {
              // Should never happen
              MonitorMap.logger.error("EPICS.ChannelConnector: PV " + thispv
                      + " is queued for monitor connection but no points are waiting!");
              continue;
            } else {
              for (int i = 0; i < thesepoints.size(); i++) {
                // Connect each point to 'monitor' updates from this channel
                Vector<Object> thisvector = thesepoints.get(i);
                PointDescription thispoint = (PointDescription) thisvector.get(0);
                DBRType thistype = (DBRType) thesepoints.get(i).get(1);
                // If no DBRType explicitly specified then set native type
                if (thistype == null) {
                  thistype = nativetype;
                }

                String listenername = thispoint.getFullName() + ":" + thispv;
                EPICSListener listener = new EPICSListener(thischan, thispoint);
                try {
                  thischan.addConnectionListener(listener);
                  if (thistype == null) {
                    thischan.addMonitor(Monitor.VALUE, listener);
                  } else {
                    // Needs to be monitored so data arrives as a specific type
                    thischan.addMonitor(thistype, 1, Monitor.VALUE, listener);
                  }
                  itsListenerMap.put(listenername, listener);
                  thesepoints.remove(thisvector);
                } catch (Exception f) {
                  MonitorMap.logger.error("EPICS: Establising Listener " + thispoint.getFullName() + "/" + thispv + ": " + f);
                }
              }
              if (thesepoints.size() == 0) {
                // We successfully connected all queued points
                allconnected.add(thispv);
              }
            }
          }
          // Now modify the map by removing any PV's which are fully connected
          for (int i = 0; i < allconnected.size(); i++) {
            itsRequiresMonitor.remove(allconnected.get(i));
          }
        }

        // Ensure all monitor requests have been flushed
        try {
          itsContext.flushIO();
        } catch (Exception e) {
          MonitorMap.logger.error("EPICS.ChannelConnector: Flushing IO: " + e);
        }
        try {
          // Sleep for a while before trying to connect remaining channels
          final RelTime sleeptime = RelTime.factory(5000000);
          sleeptime.sleep();
        } catch (Exception e) {
        }
      }
    }
  };

  /**
   * Class which handles asynchronous callbacks from EPICS for a specific MoniCA point.
   */
  protected class EPICSListener implements MonitorListener, GetListener, ConnectionListener {
    /** The name of the process variable we handle events for. */
    String itsPV = null;

    /** Channel which connects us to the PV. */
    Channel itsChannel = null;

    /** The point name. */
    String itsPointName = null;

    /** The MoniCA point instance. */
    PointDescription itsPoint = null;

    /** Create new instance to handle updates to the specified PV. */
    public EPICSListener(Channel chan, PointDescription point) {
      itsChannel = chan;
      itsPV = itsChannel.getName();
      itsPoint = point;
      itsPointName = point.getFullName();
    }

    /** Call back for 'monitor' updates. */
    public void monitorChanged(MonitorEvent ev) {
      PointData pd = new PointData(itsPointName);
      try {
        if (ev.getStatus() == CAStatus.NORMAL && ev.getDBR() != null) {
          pd = getPDforDBR(ev.getDBR(), itsPointName, itsPV);
        }
      } catch (Exception e) {
        MonitorMap.logger.warning("EPICS:EPICSListener.monitorChanged: " + itsPV + ": " + e);
      }
      itsPoint.firePointEvent(new PointEvent(this, pd, true));
    }

    /** Call back for 'get' updates. */
    public void getCompleted(GetEvent ev) {
      PointData pd = new PointData(itsPointName);
      try {
        if (ev.getStatus() == CAStatus.NORMAL && ev.getDBR() != null) {
          pd = getPDforDBR(ev.getDBR(), itsPointName, itsPV);
        }
      } catch (Exception e) {
        MonitorMap.logger.warning("EPICS:EPICSListener.getCompleted: " + itsPV + ": " + e);
      }
      itsPoint.firePointEvent(new PointEvent(this, pd, true));
      // Return the point for rescheduling
      asynchReturn(itsPoint);
    }

    /** Call back for channel state changes. */
    public void connectionChanged(ConnectionEvent ev) {
      if (!ev.isConnected()) {
        // Connection just dropped out so fire null-data update
        itsPoint.firePointEvent(new PointEvent(this, new PointData(itsPointName), true));
        if (itsPoint.isCollecting()) {
          // A get was in progress so need to return the point for rescheduling
          asynchReturn(itsPoint);
        }
      }
    }
  };

  /** Decode the value from an EPICS DBR. */
  public PointData getPDforDBR(DBR dbr, String pointname, String pvname) {
    PointData pd = new PointData(pointname);
    Object newval = null;
    boolean alarm = false;
    AbsTime timestamp = new AbsTime();

    try {
      int count = dbr.getCount();
      if (count > 1) {
        MonitorMap.logger.warning("EPICS.getPDforDBR: " + pvname + ": >1 value received");
      }
      Object rawval = dbr.getValue();
      // Have to switch on type, don't think there's any simpler way
      // to get the individual data as an object type.
      if (dbr instanceof INT) {
        newval = new Integer(((int[]) rawval)[0]);
      } else if (dbr instanceof BYTE) {
        newval = new Integer(((byte[]) rawval)[0]);
      } else if (dbr instanceof SHORT) {
        newval = new Integer(((short[]) rawval)[0]);
      } else if (dbr instanceof FLOAT) {
        newval = new Float(((float[]) rawval)[0]);
      } else if (dbr instanceof DOUBLE) {
        newval = new Double(((double[]) rawval)[0]);
      } else if (dbr instanceof STRING) {
        newval = ((String[]) rawval)[0];
      } else if (dbr instanceof ENUM) {
        newval = new Integer(((short[]) rawval)[0]);
      } else {
        MonitorMap.logger.warning("EPICS.getPDforDBR: " + pvname + ": Unhandled DBR type: " + dbr.getType());
      }

      // Check the alarm status, if information is available
      if (dbr instanceof STS) {
        STS thissts = (STS) dbr;
        if (thissts.getSeverity() == Severity.INVALID_ALARM) {
          // Point is undefined, so data value is invalid
          newval = null;
        } else if (thissts.getSeverity() != Severity.NO_ALARM) {
          // An alarm condition exists
          alarm = true;
        }
      }

      // Preserve the data time stamp, if available
      // / ts always seems to be null, so conversion below is untested
      /*
       * if (dbr instanceof TIME) { TIME time = (TIME)dbr; TimeStamp ts =
       * time.getTimeStamp(); System.err.println("TS = " + ts); if (ts!=null) {
       * System.err.println("NOW=" + timestamp.toString(AbsTime.Format.UTC_STRING) +
       * "\tTS=" + AbsTime.factory(new Date(ts.secPastEpoch()*1000l +
       * ts.nsec()/1000000l)).toString(AbsTime.Format.UTC_STRING)); timestamp =
       * AbsTime.factory(new Date(ts.secPastEpoch()*1000l + ts.nsec()/1000000l)); } }
       */

      pd.setData(newval);
      pd.setAlarm(alarm);
      pd.setTimestamp(timestamp);
    } catch (Exception e) {
      MonitorMap.logger.warning("EPICS.getPDforDBR: " + pvname + ": " + e);
      e.printStackTrace();
    }
    return pd;
  }
}
