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
 * This requires the name of the EPICS Process Variable to be polled as an argument. 
 * Polling will occur at the normal update period specified for the MoniCA point.
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
  HashMap<String, Vector<PointDescription>> itsRequiresMonitor = new HashMap<String, Vector<PointDescription>>();

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
                thischan.get(listener);
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
  public void registerMonitor(PointDescription point, String pvname) {
    synchronized (itsChannelMap) {
      if (itsChannelMap.get(pvname) == null) {
        // Not connected to this channel yet
        itsChannelMap.put(pvname, null);
      }
    }
    synchronized (itsRequiresMonitor) {
      Vector<PointDescription> thesepoints = itsRequiresMonitor.get(pvname);
      if (thesepoints == null) {
        thesepoints = new Vector<PointDescription>(1);
        thesepoints.add(point);
        itsRequiresMonitor.put(pvname, thesepoints);
      } else {
        if (!thesepoints.contains(point)) {
          thesepoints.add(point);
        }
      }
    }
  }

  /**
   * Thread which connects to EPICS channels and configures 'monitor' updates for points
   * which request it.
   */
  protected class ChannelConnector extends Thread {
    public void run() {
      while (true) {
        // Build a list of all challs which need connecting
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
            Vector<PointDescription> thesepoints = itsRequiresMonitor.get(thispv);
            if (thesepoints == null || thesepoints.size() == 0) {
              // Should never happen
              MonitorMap.logger.error("EPICS.ChannelConnector: PV " + thispv
                      + " is queued for monitor connection but no points are waiting!");
              continue;
            } else {
              for (int i = 0; i < thesepoints.size(); i++) {
                // Connect each point to 'monitor' updates from this channel
                PointDescription thispoint = thesepoints.get(i);
                String listenername = thispoint.getFullName() + ":" + thispv;
                EPICSListener listener = new EPICSListener(thischan, thispoint);
                try {
                  thischan.addConnectionListener(listener);
                  thischan.addMonitor(Monitor.VALUE, listener);
                  itsListenerMap.put(listenername, listener);
                  thesepoints.remove(thispoint);
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
          // Now modify the map by removing any pv's which are fully connected
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
          pd.setData(processDBR(ev.getDBR(), itsPV));
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
          pd.setData(processDBR(ev.getDBR(), itsPV));
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
  public Object processDBR(DBR dbr, String pvname) {
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

      // Print new value (for debugging)
      MonitorMap.logger.debug("EPICS.processDBR: " + pvname + "\t" + newval);
    } catch (Exception e) {
      MonitorMap.logger.warning("EPICS.processDBR: " + pvname + ": " + e);
      e.printStackTrace();
    }
    return newval;
  }
}
