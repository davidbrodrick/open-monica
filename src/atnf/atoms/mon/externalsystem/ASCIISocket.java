// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.net.*;
import java.io.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.TransactionStrings;

/**
 * Class for DataSources which need to read and/or write ASCII data over TCP socket
 * connection to a remote end-point.
 * <P>
 * The constructor argument defined in <tt>monitor-sources.txt</tt> must include the
 * remote machine, port and optionally timeout in
 * <tt>host:port:timeout_ms:your_other_args</tt> format.
 * <P>
 * You can override the <tt>parseData</tt> method to perform whatever response parsing
 * and/or request formatting is required, and return an appropriate data object.
 * <P>
 * The default <tt>parseData</tt> implementation expects the points to have a
 * <tt>TransactionStrings</tt> input transaction, which has the query string to be sent
 * over the socket. All response lines will be concatenated and returned as the data
 * object.
 * 
 * @author David Brodrick
 */
public class ASCIISocket extends ExternalSystem {
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
  public ASCIISocket(String[] args) {
    super(args[0] + ":" + args[1]);
    itsHostName = args[0];
    itsPort = Integer.parseInt(args[1]);
    if (args.length > 2) {
      itsTimeout = Integer.parseInt(args[2]);
    }
  }

  /** Set the socket timeout to use (ms). */
  protected void setTimeout(int ms) {
    itsTimeout = ms;
    if (itsConnected) {
      try {
        itsSocket.setSoTimeout(itsTimeout);
      } catch (Exception e) {
        try {
          disconnect();
        } catch (Exception f) {
        }
      }
    }
  }

  /** Make a new socket connection. */
  public boolean connect() throws Exception {
    // Don't connect if already connected
    if (itsConnected) {
      return true;
    }

    try {
      itsSocket = new Socket(itsHostName, itsPort);
      itsSocket.setSoTimeout(itsTimeout);
      itsConnected = true;
      itsWriter = new BufferedWriter(new OutputStreamWriter(itsSocket.getOutputStream()));
      itsReader = new BufferedReader(new InputStreamReader(itsSocket.getInputStream()));

      System.err.println("ASCIISocket (" + getClass() + "): Connected to " + itsHostName + ":" + itsPort);
      itsNumTransactions = 0;
    } catch (Exception e) {
      itsSocket = null;
      itsReader = null;
      itsWriter = null;
      itsConnected = false;
      throw e;
    }
    return itsConnected;
  }

  /** Close the socket, unless it is already closed. */
  public void disconnect() throws Exception {
    if (itsSocket != null) {
      itsSocket.close();
    }
    itsSocket = null;
    itsReader = null;
    itsWriter = null;
    itsConnected = false;
  }

  /**
   * You can override this method in your class to perform any response parsing and/or
   * request formatting that is required. This default implementation sends the request
   * string to the server and then returns all response lines as a single string.
   */
  public Object parseData(PointDescription requestor) throws Exception
  {
    // Get the Transaction which associates the point with us
    TransactionStrings thistrans=(TransactionStrings)getMyTransactions(requestor.getInputTransactions()).get(0);

    // The Transaction should contain a query string to be issued to the server
    if (thistrans.getNumStrings()<1) {
      throw new Exception("ASCIISocket: Not enough arguments in Transaction");
    }
    
    String query = thistrans.getString();
    // Substitute EOL characters
    query=query.replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r");

    // Clear input buffer
    while (itsReader.ready()) {
      itsReader.readLine();
    }   
    
    // Send the query to the server
    itsWriter.write(query);
    itsWriter.flush();
    
    // Read response
    String result = itsReader.readLine() + "\n";
    while (itsReader.ready()) {
      String line = itsReader.readLine() + "\n";
      result = result + line;
    }
    
    return result;
  }

  /** Collect data and fire events to queued monitor points. */
  protected void getData(PointDescription[] points) throws Exception {
    Object[] buf = points;
    try {
      for (int i = 0; i < buf.length; i++) {
        Object o = null;
        PointDescription pm = (PointDescription) buf[i];
        o = parseData(pm);
        // Count successful transactions
        if (o != null) {
          itsNumTransactions += buf.length;
        }
        // Fire the new data off for this point
        pm.firePointEvent(new PointEvent(this, new PointData(pm.getFullName(), o), true));
      }
    } catch (Exception e) {
      // Probably a comms error.
      disconnect();
      System.err.println("ASCIISocket: " + e.getClass() + ": " + e.getMessage());
      MonitorMap.logger.error(e.getMessage());
    }
  }
}
