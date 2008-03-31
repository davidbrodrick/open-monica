//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.io.*;
import java.util.*;
import java.net.*;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.mon.transaction.*;

/**
 * DataSource to retrieve and process lighning strike data. The output
 * of the lightning detector is available on the ethernet via a
 * serial/ethernet converter.
 *
 * @author David Brodrick
 * @version $Id: DataSourceLightning.java,v 1.10 2008/02/08 01:03:14 bro764 Exp $
 **/
class DataSourceLightning
extends DataSource
{
  /** The socket connection to the serial/ethernet converter. */
  private Socket itsSocket = null;
  /** Network port to connect to. */
  private int itsPort = 6000;
  /** Name of remote host to connect to. */
  private String itsHost = null;

  /** InputStream for reading data. */
  protected BufferedReader itsReader = null;
  /** OutputStream for sending commands. */
  protected PrintStream itsWriter = null;

   /** Constructor.
    * @param args
    */
   public DataSourceLightning(String args)
   {
     super(args);

     itsHost = args.substring(args.lastIndexOf("/")+1);
   }


   /** Establish a new network connection to the lighning detector. */
   public
   boolean
   connect()
   throws Exception
   {
    try {
      itsSocket = new Socket(itsHost, itsPort);
      itsSocket.setSoTimeout(10000); ///Ten second timeout.
      itsConnected = true;
      itsReader = new BufferedReader(new InputStreamReader(
                                     itsSocket.getInputStream()));
      itsWriter = new PrintStream(itsSocket.getOutputStream());

      //Ensure it's set to poll-only mode
      itsWriter.print("H 0\r\n");
      itsReader.readLine();
      //Ensure it's set to 15-minute strike expiry age
      itsWriter.print("J 1\r\n");
      itsReader.readLine();

      System.err.println("DataSourceLightning: Connected to " + itsHost);
      MonitorMap.logger.information("DataSourceLightning: Connected to "
                                    + itsHost);
      //Reset the transaction counter
      itsNumTransactions = 0; 
    } catch (Exception e) {
      try {
	if (itsSocket!=null) itsSocket.close();
      } catch (Exception f) { }
      itsSocket = null;
      itsReader = null;
      itsConnected  = false;
      System.err.println("DataSourceLightning: Couldn't Connect: "
			 + e.getMessage());
    }
    return itsConnected;
   }


   /** Close the network connection to the lightning detector. */
   public
   void
   disconnect()
   throws Exception
   {
     System.err.println("DataSourceLightning: Lost Connection");
     itsConnected = false;
     if (itsSocket!=null) itsSocket.close();
     itsSocket = null;
     itsReader = null;
     itsWriter = null;
   }


   /** Do the actual network transactions and parse the output of the
    * lightning detector into a HashMap that can be used by other monitor
    * points. */
   private
   HashMap
   getNewData()
   throws Exception
   {
     if (!itsConnected) throw new Exception("Not connected to lightning detector");

     itsWriter.print("A\r\n");
     String line1 = itsReader.readLine();
     if (line1.startsWith("P")) {
       //It's an automatic status message, need to discard it
       line1 = itsReader.readLine();
     }
     String line2 = itsReader.readLine();
     String line3 = itsReader.readLine();

     HashMap res = new HashMap();
     int numnear = 0;
     int numfar = 0;

     StringTokenizer st = new StringTokenizer(line1);
     String tok = st.nextToken();
     if (!tok.equals("NEAR:")) return null;
     //Strikes, Near, North
     tok = st.nextToken();
     if (!tok.equals("N")) return null;
     Integer i = new Integer(st.nextToken());
     res.put("NEAR-N", i);
     numnear+=i.intValue();
     //Strikes, Near, North East
     tok = st.nextToken();
     if (!tok.equals("NE")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-NE", i);
     numnear+=i.intValue();
     //Strikes, Near, East
     tok = st.nextToken();
     if (!tok.equals("E")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-E", i);
     numnear+=i.intValue();
     //Strikes, Near, South East
     tok = st.nextToken();
     if (!tok.equals("SE")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-SE", i);
     numnear+=i.intValue();
     //Strikes, Near, South
     tok = st.nextToken();
     if (!tok.equals("S")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-S", i);
     numnear+=i.intValue();
     //Strikes, Near, South West
     tok = st.nextToken();
     if (!tok.equals("SW")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-SW", i);
     numnear+=i.intValue();
     //Strikes, Near, West
     tok = st.nextToken();
     if (!tok.equals("W")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-W", i);
     numnear+=i.intValue();
     //Strikes, Near, North West
     tok = st.nextToken();
     if (!tok.equals("NW")) return null;
     i = new Integer(st.nextToken());
     res.put("NEAR-NW", i);
     numnear+=i.intValue();
     res.put("NUM-NEAR", new Integer(numnear));

     st = new StringTokenizer(line2);
     tok = st.nextToken();
     if (!tok.equals("DIST:")) return null;
     //Strikes, Distant, North
     tok = st.nextToken();
     if (!tok.equals("N")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-N", i);
     numfar+=i.intValue();
     //Strikes, Distant, North East
     tok = st.nextToken();
     if (!tok.equals("NE")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-NE", i);
     numfar+=i.intValue();
     //Strikes, Distant, East
     tok = st.nextToken();
     if (!tok.equals("E")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-E", i);
     numfar+=i.intValue();
     //Strikes, Distant, South East
     tok = st.nextToken();
     if (!tok.equals("SE")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-SE", i);
     numfar+=i.intValue();
     //Strikes, Distant, South
     tok = st.nextToken();
     if (!tok.equals("S")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-S", i);
     numfar+=i.intValue();
     //Strikes, Distant, South West
     tok = st.nextToken();
     if (!tok.equals("SW")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-SW", i);
     numfar+=i.intValue();
     //Strikes, Distant, West
     tok = st.nextToken();
     if (!tok.equals("W")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-W", i);
     numfar+=i.intValue();
     //Strikes, Distant, North West
     tok = st.nextToken();
     if (!tok.equals("NW")) return null;
     i = new Integer(st.nextToken());
     res.put("DIST-NW", i);
     numfar+=i.intValue();
     res.put("NUM-DIST", new Integer(numfar));

     st = new StringTokenizer(line3);
     //Overhead strikes
     tok = st.nextToken();
     if (!tok.equals("OVHD")) return null;
     i = new Integer(st.nextToken());
     int ovhd = i.intValue();
     res.put("OVHD", i);
     //Cloud strikes
     tok = st.nextToken();
     if (!tok.equals("CLOUD")) return null;
     i = new Integer(st.nextToken());
     res.put("CLOUD", i);
     //Total flashes
     tok = st.nextToken();
     if (!tok.equals("TOTAL")) return null;
     i = new Integer(st.nextToken());
     res.put("TOTAL", i);
     //Result of self-test
     tok = st.nextToken();
     if (tok.equals("P")) res.put("SLFTST", "PASS");
     else if (tok.equals("F")) res.put("SLFTST", "FAIL");
     else return null;
     //Status code
     tok = st.nextToken();
     tok = (tok.replace('H', ' ')).trim();
     int status = Integer.parseInt(tok, 16);
     if (status==0) res.put("STATUS", "OK");
     else {
       //We can decode the error using the table on page 29 of the manual
       String error = "";
       if ((status&1)!=0)  error += "input voltage failure, ";
       if ((status&2)!=0)  error += "reference voltage failure, ";
       if ((status&4)!=0)  error += "optical 5V failure, ";
       if ((status&8)!=0)  error += "digital 5V failure, ";
       if ((status&16)!=0) error += "analog 5V failure, ";
       if ((status&32)!=0) error += "internal temperature failure, ";
       if ((status&64)!=0) error += "self-test B signal failure";
       res.put("STATUS", error);
     }
     //Internal temperature
     i = new Integer(st.nextToken());
     res.put("TEMP", i);
     tok = st.nextToken();
     if (!tok.equals("C")) return null;
     //total strikes since last self-test
     i = new Integer(st.nextToken());
     res.put("TOTSLFTST", i);
     //total rejected strikes since last self-test
     i = new Integer(st.nextToken());
     res.put("BADTOT", i);
     //rejected by minimum E/B
     i = new Integer(st.nextToken());
     res.put("BADMINEB", i);
     //rejected by maximum E/B
     i = new Integer(st.nextToken());
     res.put("BADMAXEB", i);
     //rejected by minimum B
     i = new Integer(st.nextToken());
     res.put("BADMINB", i);
     //Average E/B ratio since last self-test
     Float ebrat = new Float(st.nextToken());
     res.put("EBRATIO", ebrat);

     //Next we do a separate command to get the system run-time
     itsWriter.print("F\r\n");
     String line4 = itsReader.readLine();
     if (line4.startsWith("P")) {
       //It's an automatic status message, need to discard it
       line4 = itsReader.readLine();
     }
     st = new StringTokenizer(line4);
     if (!st.nextToken().equals("D")) return null;
     int d = Integer.parseInt(st.nextToken());
     if (!st.nextToken().equals("H")) return null;
     int h = Integer.parseInt(st.nextToken());
     if (!st.nextToken().equals("M")) return null;
     int m = Integer.parseInt(st.nextToken());
     if (!st.nextToken().equals("S")) return null;
     int s = Integer.parseInt(st.nextToken());
     //get uptime as seconds
     long uptime = s + 60*m + 3600*h + 86400*d;
     //uptime as microseconds
     uptime *= 1000000;
     res.put("UPTIME", RelTime.factory(uptime));

     int j = 1*numfar + 10*numnear + 45*ovhd;
     res.put("WEIGHTED_THREAT", new Integer(j));
     if (j<22) j=0;       //All OK
     else if (j<52)  j=1; //Warn of storms
     else if (j<102) j=2; //Suggest starting gensets
     else if (j<202) j=3; //Start gensets
     else j=4;            //Storm stow
     res.put("STORM_THREAT", new Integer(j));

     return res;
   }



   public 
   void
   getData(Object[] points)
   throws Exception
   {
     //Precondition
     if (points==null || points.length==0) return;
     //Increment transaction counter
     itsNumTransactions += points.length;

     //Try to get the new data and force a reconnect if the read times out
     HashMap newdata = null;
     try {
       if (itsConnected) newdata = getNewData();
     } catch (Exception e) {
       try {
	 System.err.println("DatasourceLightning: " + e.getMessage());
	 disconnect();
       } catch (Exception f) { }
     }
     //If the response was null then there must have been a parse error
     //this tends to happen after a power glitch when the detector gets
     //power cycled and spits out a heap of rubbish characters that then
     //get buffered by the media converter. Let's force a reconnect and
     //make sure the buffer has been flushed.
     if (newdata==null) {
       try {
	 System.err.println("DatasourceLightning: Parse error..");
	 disconnect();
       } catch (Exception e) { }
     }

     //Fire off the new data
     for (int i=0; i<points.length; i++) {
       PointInteraction pm = (PointInteraction)points[i];
       PointData pd = new PointData(pm.getName(), pm.getSource(), newdata);
       pm.firePointEvent(new atnf.atoms.mon.PointEvent(this, pd, true));
     }
   }

   public final static
   void
   main(String[] argv)
   {
     if (argv.length<1) {
       System.err.println("Missing argument: Needs IP of lightning detector");
       System.exit(1);
     }
     DataSourceLightning bang = new DataSourceLightning("lightning://" + argv[0]);

     try {
       bang.connect();
       HashMap res = bang.getNewData();
       System.err.println("HashMap=" + res);
     } catch (Exception e) {
       System.err.println(e.getMessage());
       System.exit(1);
     }
     System.exit(0);

     while (true);
   }
}
