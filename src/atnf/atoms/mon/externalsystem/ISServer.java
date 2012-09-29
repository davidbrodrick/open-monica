// Copyright (C) Inside Systems Pty Ltd
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.StringTokenizer;
import java.util.HashMap;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.externalsystem.*;

/**
 * Provides a generic interface to a perl server which sends requested data over a TCP socket
 */
public class ISServer extends ASCIISocket {
  /** Timestamp of the last message. */
  private String itsTstamp = null;

  public ISServer(String[] args) {
    super(args);
    //System.err.println("ISServer: Constructing");    
  }

  /** Query all of the latest values and return a HashMap containing them. */
  public Object parseData(PointDescription requestor) throws Exception {
    HashMap<String,Object> res = new HashMap<String,Object>();
    itsWriter.write("all\r\n");
    itsWriter.flush();
    //System.err.println("ISServer: sent request");

    // Sleep for a jiffy to allow the server to respond
    try {
      RelTime sleep = RelTime.factory(1000000);
      sleep.sleep();
    } catch (Exception e) {
      System.err.println("ISServer 41: Caught exception: " + e);
    }

    // Read each line that the server returned
    String key = "";
    while (itsReader.ready()) {
      try {
        String line = itsReader.readLine();
        //System.err.println("ISServer: got response: " + line);
        StringTokenizer st = new StringTokenizer(line, "\t");
        if (st.countTokens() < 2)
          continue;
        key = st.nextToken();
        Object value = st.nextToken().trim();
        String type = st.nextToken();
        
        key = key.replace(' ', '_');
        
        // assuming datatype string as default. Any other datatype needs defined as Integer or Float.
        if ( type.indexOf("Int") != -1 ) {
            value = new Integer((String) value);
        } else if ( type.indexOf("Flo") != -1 ) {
            value = new Float((String) value);
        } else if ( type.indexOf("Lon") != -1 ) {
            value = new Long((String) value);
        }

        res.put(key, value);
      } catch (Exception e) {
        System.err.println("ISServer 67: Caught exception: " + e + " key: " + key);
      }
    }
    //disconnect();
    return res;
  }
}
