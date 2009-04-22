// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.net.*;
import atnf.atoms.mon.*;

/**
 * Abstract class which binds to a local UDP socket. The first argument must
 * be the full 'channel' string. An optional second argument can specify the
 * local port to bind to, if not specified then any system assigned port will
 * be used. The third argument, also optional, is the SO timeout to associate
 * with the socket, in milliseconds.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public abstract class UDPSocket
extends ExternalSystem
{
  /** The socket used for communicating with the remote service. */
  protected DatagramSocket itsSocket = null;

  /** The local port to bind to, -1 for system assigned local port. */
  protected int itsLocalPort = -1;
  
  /** Read timeout period, in ms. */
  protected int itsTimeout = 5000;

  public UDPSocket(String[] args)
  {
    super(args[0]);
    
    if (args.length>1) {
      itsLocalPort=Integer.parseInt(args[1]);
    }
    if (args.length>2) {
      itsTimeout=Integer.parseInt(args[2]);
    }
    
    try {
    connect();
    } catch (Exception e) {
      e.printStackTrace();
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
    try {
      if (itsLocalPort==-1) {
        itsSocket = new DatagramSocket();
      } else {
        itsSocket = new DatagramSocket(itsLocalPort);
      }
      itsSocket.setSoTimeout(itsTimeout);
      itsConnected = true;
      itsNumTransactions=0;
    } catch (Exception e) {
      e.printStackTrace();
      itsSocket = null;
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
    itsConnected  = false;
  }


  /** Implement this method in your class. The <tt>requestor</tt> argument
   * can be used to determine which data to request, if your implementation
   * provides different kinds of data to different monitor points. */
  public abstract
  Object
  parseData(PointDescription requestor)
  throws Exception;


  /** Collect data and fire events to queued monitor points. */
  protected
  void
  getData(PointDescription[] points)
  throws Exception
  {
    try {
      for (int i=0; i<points.length; i++) {
        Object o = null;
        PointDescription pm = points[i];
        o = parseData(pm);
        //Count successful transactions
        if (o!=null) {
          itsNumTransactions += points.length;
        }
        //Fire the new data off for this point
        pm.firePointEvent(new PointEvent(this,
           new PointData(pm.getName(), pm.getSource(), o), true));
      }
    } catch (Exception e) {
      //Probably a comms error.
      disconnect();
      System.err.println("UDPSocket: " + e.getClass() + ": " + e.getMessage());
      e.printStackTrace();
      MonitorMap.logger.error(e.getMessage());
    }
  }
}
