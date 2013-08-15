// Copyright (C) Inside Systems Pty Ltd
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;
import com.eclipsesource.json.*;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.io.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.TransactionStrings;
import atnf.atoms.time.RelTime;

import org.apache.log4j.Logger;

/**
 * Class for ExternalSystems which need to read and/or write JSON data over TCP socket connection to a remote end-point. This class
 * derives from the TCPSocket class but also provides BufferedReader and BufferedWriter fields.
 * <P>
 * The constructor argument defined in <tt>monitor-sources.txt</tt> must include the remote machine, port and optionally timeout in
 * <tt>host:port:timeout_ms:your_other_args</tt> format.
 * <P>
 * You can override the <tt>parseData</tt> method to perform whatever response parsing and/or request formatting is required, and
 * return an appropriate data object. Alternately you may override the <i>getData</i> method in which case the <i>parseData</i>
 * method will not be called.
 * <P>
 * The default <tt>parseData</tt> implementation expects the points to have a <tt>TransactionStrings</tt> input transaction, which
 * has the query string to be sent over the socket. An optional additional argument can specify the number of lines expected in the
 * response, this is useful when the server has inter-line latency which can trick the standard logic into thinking that the entire
 * response has been consumed. All response lines will be concatenated and returned as the data object.
 * 
 * @author Balthasar Indermuehle (based on ASCIISocket by David Brodrick)
 */
public class JSONSocket extends TCPSocket {
  /** The output stream for writing text to the remote service. */
  protected BufferedWriter itsWriter = null;

  /** The input stream for reading responses from the remote service. */
  protected BufferedReader itsReader = null;

  /** Argument must include host:port and optionally :timeout_ms */
  public JSONSocket(String[] args) {
    super(args);
  }

  /** Make a new socket connection. */
  public boolean connect() throws Exception {
    try {
      super.connect();
      itsWriter = new BufferedWriter(new OutputStreamWriter(itsSocket.getOutputStream()));
      itsReader = new BufferedReader(new InputStreamReader(itsSocket.getInputStream()));
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.info("Connected to " + itsHostName + ":" + itsPort);
    } catch (Exception e) {
      itsReader = null;
      itsWriter = null;
      super.disconnect();
      throw e;
    }
    return itsConnected;
  }

  /** Close the socket, unless it is already closed. */
  public void disconnect() throws Exception {
    itsReader = null;
    itsWriter = null;
    super.disconnect();
  }

  /** */
  public synchronized void putData(PointDescription desc, PointData pd) throws Exception {
    if (!isConnected()) {
      connect();
    }

    Logger logger = Logger.getLogger(this.getClass().getName());

    //logger.info("JSONSocket: putData initiated");

    try {
      // Get the Transaction which associates the point with us
      TransactionStrings thistrans = (TransactionStrings) getMyTransactions(desc.getOutputTransactions()).get(0);

      // The Transaction should contain a query string to be issued to the
      // server
      if (thistrans.getNumStrings() < 1) {
        throw new Exception("JSONSocket: Not enough arguments in Transaction");
      }
      
      String fullname = pd.getName().toString();
      String name = fullname.substring(fullname.lastIndexOf('.') + 1);
      JsonObject thisdata = new JsonObject().add( name, pd.getData().toString() );
     
      String cmdstring = thisdata.toString() + "\n";
      
      //logger.info("JSONSocket: sending data: " + cmdstring);

      // Send the command to the server
      itsWriter.write(cmdstring);
      itsWriter.flush();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * You can override this method in your class to perform any response parsing and/or request formatting that is required. This
   * default implementation sends the request string to the server and then returns all response lines as a single string.
   */
  public Object parseData(PointDescription requestor) throws Exception {
    // Get the Transaction which associates the point with us
    HashMap<String,Object> res = new HashMap<String,Object>();

    Logger logger = Logger.getLogger(this.getClass().getName());
    //logger.info("This query:" + query);
    
    String key = "";
    while (itsReader.ready()) {
      try {
        String line = itsReader.readLine();
        //logger.info("JSONSocket: got data: " + line);
        
        JsonObject jsonObject = JsonObject.readFrom( line );

        for( String thiskey : jsonObject.names() ) {
          key = thiskey;        
          Float value = jsonObject.get( thiskey ).asFloat();
          thiskey = thiskey.replace(' ', '_');
          res.put(thiskey, value);
        }
        
        /*
        // assuming datatype string as default. Any other datatype needs defined as Integer or Float.
        if ( type.indexOf("Int") != -1 ) {
            value = new Integer((String) value);
        } else if ( type.indexOf("Flo") != -1 ) {
            value = new Float((String) value);
        } else if ( type.indexOf("Lon") != -1 ) {
            value = new Long((String) value);
        }
        */
        
        
      } catch (Exception e) {
        System.err.println("JSONSocket: Caught exception: " + e + " key: " + key);
      }
    }
    return res;
  }

  /** Collect data and fire events to queued monitor points. */
  protected synchronized void getData(PointDescription[] points) throws Exception {
    try {    
      for (int i = 0; i < points.length; i++) {
        Object o;
        PointDescription pm = points[i];      

        o = parseData(pm);
        
        // Count successful transactions
        if (o != null) {
          itsNumTransactions++;
        }
        // Fire the new data off for this point
        pm.firePointEvent(new PointEvent(this, new PointData(pm.getFullName(), o), true));
      }
    } catch (Exception e) {
      // Probably a comms error.
      disconnect();
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("JSONSocket (" + itsName + "): " + e);
    }
  }
}
