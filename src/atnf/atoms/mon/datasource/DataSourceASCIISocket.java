// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.net.*;
import java.io.*;
import atnf.atoms.mon.*;

/**
 * Abstract class for DataSources which need to read and/or write ASCII data
 * over TCP socket connection to a remote end-point.
 * <P>
 * The constructor argument defined in <tt>monitor-sources.txt</tt> must
 * include the remote machine, port and optionally timeout in
 * <tt>host:port:timeout_ms:your_other_args</tt> format.
 * <P>
 * For devices where we send a command which trigger the remote end-point
 * to send the latest data, all you need to do is implement
 * the <tt>parseData(.)</tt> method. In that method you should request the
 * data from the remote machine, and return the appropriate object.
 * <P>
 * If a single query returns many pieces of data you might like to return
 * a HashMap here and then have all the monitor points for the individual
 * pieces of data listen for updates to the amalgamated point,
 * and use a <tt>TranslationNV</tt> to extract a particular value from the
 * HashMap value of the amalgamated point whenever a new value is available.
 * This saves needing to do the full query every time a single point needs
 * to be updated.
 * <P>
 * For classes where data is pushed to us without being triggered by a
 * request from our end, you can override the <tt>getData</tt> and
 * <tt>parseData</tt> methods with empty implementations, and then spawn
 * a thread which parses the incoming data and fires update events for
 * the appropriate monitor points directly. A trivial example of this is:
 *
 * <P><tt>
 * if (my_data_key.equals("temperature")) {<BR>
 * &nbsp PointMonitor pm=MonitorMap.getPointMonitor("gizmo.temperature");<BR>
 * &nbsp pm.firePointEvent(new PointEvent(this, new PointData(pm.getName(), pm.getSource(), my_data_value), true));<BR>
 * }</tt>
 *
 * @author David Brodrick
 * @version $Id: DataSourceASCIISocket.java,v 1.4 2008/03/12 01:36:14 bro764 Exp $
 **/
public abstract class DataSourceASCIISocket
extends DataSource
{
  /** The socket used for communicating with the remote service. */
  protected Socket itsSocket = null;
  /** The output stream for writing text to the remote service. */
  protected BufferedWriter itsWriter = null;
  /** The input stream for reading responses from the remote service. */
  protected BufferedReader itsReader = null;

  /** The port to connect to the remote end-point. */
  protected int itsPort = -1;
  /** The hostname or IP of the remote end-point. */
  protected String itsHostName = null;
  /** Socket timeout period, in ms. */
  protected int itsTimeout = 5000;

  /** Argument must include host:port and optionally :timeout_ms */
  public DataSourceASCIISocket(String[] args)
  {
    super(args[0]+":"+args[1]);
    itsHostName=args[0];
    itsPort=Integer.parseInt(args[1]);
    if (args.length>2) {
      itsTimeout=Integer.parseInt(args[2]);
    }
  }

  /** Set the socket timeout to use (ms). */
  protected
  void
  setTimeout(int ms) {
    itsTimeout=ms;
    if (itsConnected) {
      try {
        itsSocket.setSoTimeout(itsTimeout);
      } catch (Exception e) {
        try {
          disconnect();
        } catch (Exception f) {}
      }
    }
  }

  /** Make a new socket connection. */
  public
  boolean
  connect()
  throws Exception
  {
    //Don't connect if already connected
    if (itsConnected) {
      return true;
    }
    
    try {
      itsSocket = new Socket(itsHostName, itsPort);
      itsSocket.setSoTimeout(itsTimeout);
      itsConnected = true;
      itsWriter = new BufferedWriter(new OutputStreamWriter(
                                         itsSocket.getOutputStream()));
      itsReader = new BufferedReader(new InputStreamReader(
                                         itsSocket.getInputStream()));
      System.err.println("Connected to " + itsHostName + ":" + itsPort);
      //MonitorMap.logger.information("Connected to " + itsHostName + ":" + itsPort);
      itsNumTransactions=0;
    } catch (Exception e) {
      itsSocket = null;
      itsReader = null;
      itsWriter = null;
      itsConnected  = false;
      throw e;
    }
    return itsConnected;
  }


  /** Close the socket, unless it is already closed. */
  public
  void
  disconnect()
  throws Exception
  {
    if (itsSocket!=null) {
      itsSocket.close();
    }
    itsSocket = null;
    itsReader = null;
    itsWriter = null;
    itsConnected  = false;
  }


  /** Implement this method in your class. The <tt>requestor</tt> argument
   * can be used to determine which data to request, if your implementation
   * provides different kinds of data to different monitor points. */
  public abstract
  Object
  parseData(PointMonitor requestor)
  throws Exception;


  /** Collect data and fire events to queued monitor points. */
  protected
  void
  getData(Object[] points)
  throws Exception
  {
    //Precondition
    if (points==null || points.length==0 || !itsConnected) {
      return;
    }

    Object[] buf = points;
    try {
      for (int i=0; i<buf.length; i++) {
        Object o = null;
        PointMonitor pm = (PointMonitor)buf[i];
        o = parseData(pm);
        //Count successful transactions
        if (o!=null) {
          itsNumTransactions += buf.length;
        }
        //Fire the new data off for this point
        pm.firePointEvent(new PointEvent(this,
           new PointData(pm.getName(), pm.getSource(), o), true));
      }
    } catch (Exception e) {
      //Probably a comms error.
      disconnect();
      System.err.println("DataSourceASCIISocket: " + e.getClass() + ": " + e.getMessage());
      e.printStackTrace();
      MonitorMap.logger.error(e.getMessage());
    }
  }
}
