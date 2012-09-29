// Copyright 2012 (C) Inside Systems Pty Ltd
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.StringTokenizer;
import java.util.Date;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.externalsystem.*;

/**
 * MLTS = Multi Line Time Stamped
 * Read data from a TCP Socket. This Socket provides an entire file of format:
 * Name of point \t Unix timestamp \t data
 * 
 * @author Balthasar Indermuehle
 */
public class MLTS extends ASCIISocket
{
  private long WaitInterval = 0L;
  
  public MLTS(String[] args)
  {
    super(args);
    //System.out.println("MLTS: Constructing");  
    try {
      WaitInterval = Long.parseLong(args[2]) * 1000;
      DataReader worker = new DataReader();
      worker.start();    
    } catch (Exception e) {
      System.err.println("MLTS: Must specify a wait interval in monitor-sources.txt in milliseconds!, e.g. MLTS 127.0.0.1:7111:20000");
    }
  }
  
  // The data reader thread
  public class DataReader extends Thread 
  {
    public void run()
    {
      while (true) {
        try {
          itsWriter.write("MLTS Ready\r\n");
          itsWriter.flush();
          //System.out.println("MLTS: sent request");
        } catch (Exception e) {
          System.err.println("MLTS 52: Caught exception: " + e);
        }
        // Sleep for a jiffy to allow the server to respond
        try {
          RelTime sleep = RelTime.factory(1000000);
          sleep.sleep();
        } catch (Exception e) {
          System.err.println("MLTS 59: Caught exception: " + e);
        }
    
        // Read each line that the server returned
        String key = "";
        Long ts;
        AbsTime BAT;
        
        try {
          while (itsReader.ready()) {
            String line = itsReader.readLine();
            //System.out.println("MLTS: got response: " + line);
            StringTokenizer st = new StringTokenizer(line, "\t");
            if (st.countTokens() < 3)
              continue;
            key = st.nextToken();
            Object ststamp = st.nextToken().trim();
            Object value = Float.parseFloat(st.nextToken().trim());
            
            key = key.replace(' ', '_');
            
            BAT = AbsTime.factory(new Date(Long.parseLong((String) ststamp) * 1000));
            //System.out.println( BAT.toString() + " " + ststamp);
            PointData pd = PointBuffer.getPointData(key);
            PointDescription pm = PointDescription.getPoint(key);
            
            if ( pd==null || pd.getTimestamp().isBefore(BAT) ) {
                // new data, add it
                PointData npd = new PointData(key, BAT, value);
                //System.out.println(" " + npd.toString());
                PointEvent pe = new PointEvent(this, npd, true);
                //System.out.println(" " + pe.toString());
                pm.firePointEvent(pe); 
            }
          }
        } catch (Exception e) {
          System.err.println("MLTS 95: Caught exception: " + e + " key: " + key);
        }
        
        //System.out.println("itsReader was done");
        
        // Sleep for WaitInterval seconds
        try {
          RelTime sleep = RelTime.factory(WaitInterval);
          sleep.sleep();
        } catch (Exception e) {
          System.err.println("MLTS 105: Caught exception: " + e);
        }
      } // while true
    } // run
  } // DataReader
} // class MLTS

