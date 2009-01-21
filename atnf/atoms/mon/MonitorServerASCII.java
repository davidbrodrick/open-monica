//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.time.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.mon.limit.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

/**
 * Provides a simple ASCII interface for clients to obtain monitor data.
 *
 * @author David Brodrick
 * @version $Id: MonitorServerASCII.java,v 1.6 2007/11/01 01:09:16 bro764 Exp $
 */
public class
MonitorServerASCII
implements Runnable
{
  /** Keep track of how many clients are connected. */
  protected static int theirNumClients = 0;

  /** Indicates if the thread should keep running (true) or stop (false). */
  protected boolean itsRunning = true;

  /** The socket used to listen for requests from our client. */
  protected Socket itsSocket = null;

  /** For writing data to the client. */
  protected PrintWriter itsWriter = null;

  /** For reading instructions from the client. */
  protected BufferedReader itsReader = null;

  /** Starts up the main server thread which waits for client connections. */
  public MonitorServerASCII()
  {
    Thread t = new Thread(this, "MonitorServerASCII Main");
    t.setDaemon(true);
    t.start();
  }


  /** Starts up a server thread to handle requests from a new client.
   * @param socket The socket connection to the new client. */
  public MonitorServerASCII(Socket socket)
  throws IOException
  {
    theirNumClients++;
//    MonitorMap.logger.debug("New ASCII Client Connection from: "
//			    + socket.getInetAddress().getHostAddress());
    System.out.println("MonitorServerASCII: New Connection from "
		       + socket.getInetAddress().getHostAddress());
    itsSocket = socket;
    itsReader = new BufferedReader(new InputStreamReader(
                                   itsSocket.getInputStream()));
    itsWriter = new PrintWriter(itsSocket.getOutputStream());
    Thread t = new Thread(this, "MonitorServerASCII/"+socket.getInetAddress().getHostAddress());
    t.setDaemon(true);
    t.start();
  }


  /** Return the number of clients connected. */
  public static
  int
  getNumClients()
  {
    return theirNumClients;
  }


  /** Stop the running thread. */
  public
  void
  stopRunning()
  {
    itsRunning = false;
    Thread.currentThread().interrupt();
    try {
      Thread.currentThread().join();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII::stopRunning(): " +
			 e.getMessage());
    }
  }


  /** Main loop to parse and service client requests. */
  private
  void
  processConnection()
  {
    //Main loop for handling client requests
    while (itsRunning) {
      try {
        synchronized (itsReader) {
          String line = itsReader.readLine();
          if (line==null) {
            //Connection broke..
            itsRunning = false;
            return;
          }
          line=line.trim();
          if (line.equalsIgnoreCase("poll")) {
            state_poll();
          } else if (line.equalsIgnoreCase("poll2")) {
            state_poll2();
          } else if (line.equalsIgnoreCase("since")) {
            state_since();
          } else if (line.equalsIgnoreCase("between")) {
            state_between();
          } else if (line.equalsIgnoreCase("preceeding")) {
            state_preceeding();
          } else if (line.equalsIgnoreCase("following")) {
            state_following();          
          } else if (line.equalsIgnoreCase("names")) {
            state_names();
          } else if (line.equalsIgnoreCase("details")) {
            state_details();
          }
        }
      } catch (Exception f) {
        System.err.println("MonitorServerASCII: processConnection: " + f.getClass());
                           f.printStackTrace();
        itsRunning = false;
      }
    }
    System.out.println("MonitorServerASCII: Lost Connection to "
		       + itsSocket.getInetAddress().getHostAddress());
    try {
      itsReader.close();
      itsWriter.close();
      itsSocket.close();
    } catch (Exception e) {
      //Who gives a rats?
    }
  }


  /** Return the names of all monitor points. */
  protected
  void
  state_names()
  {
    try {
      //Get the list of all names
      String[] names = MonitorMap.getPointNames();

      //Tell the client how many names we will return
      itsWriter.println(names.length);

      //Send each name
      for (int i=0; i<names.length; i++) {
        itsWriter.println(names[i]);
      }

      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: names: " + e.getClass());
      itsRunning = false;
    }
  }


  protected
  void
  state_between()
  {
    try {
      String tempstr = itsReader.readLine().trim();
      //Line should say <TIMESTAMP> <TIMESTAMP> <POINTNAME>
      StringTokenizer st = new StringTokenizer(tempstr);
      if (st.countTokens()!=3) {
        itsWriter.println("? Need two BAT timestamps and a point name argument");
        itsWriter.flush();
        return;
      }

      //Get/check start timestamp
      AbsTime starttime = null;
      try {
        starttime = AbsTime.factory(st.nextToken());
      } catch (Exception e) {
        itsWriter.println("? First BAT timestamp couldn't be parsed");
        itsWriter.flush();
        return;
      }

      //Get/check end timestamp
      AbsTime endtime = null;
      try {
        endtime = AbsTime.factory(st.nextToken());
      } catch (Exception e) {
        itsWriter.println("? Second BAT timestamp couldn't be parsed");
        itsWriter.flush();
        return;
      }

      //Ensure start/end arguments are in the correct sequence
      if (endtime.isBefore(starttime)) {
        AbsTime temp = endtime;
        endtime = starttime;
        starttime = temp;
      }

      //Get/check monitor point name
      String mpname = st.nextToken();
      if (!MonitorMap.checkPointName(mpname)) {
        itsWriter.println("? Named point doesn't exist");
        itsWriter.flush();
        return;
      }

      //Get data between the specified times
      Vector data = PointBuffer.getPointData(mpname, starttime, endtime, 0);
      if (data==null) data = new Vector();

      //Tell the client how many samples we are going to send
      int numdata = data.size();
      itsWriter.println(numdata);
      //Send each sample
      for (int i=0; i<numdata; i++) {
        PointData pd = (PointData)data.get(i);
        itsWriter.println(pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
      }

      itsWriter.flush();
    } catch (Exception e) {
      e.printStackTrace();
      itsRunning = false;
    }
  }


  protected
  void
  state_since()
  {
    try {
      String tempstr = itsReader.readLine().trim();
      //Line should say <TIMESTAMP> <POINTNAME>
      StringTokenizer st = new StringTokenizer(tempstr);
      if (st.countTokens()!=2) {
        itsWriter.println("? Need BAT timestamp and point name arguments");
        itsWriter.flush();
        return;
      }

      //Get/check timestamp
      AbsTime sincetime = null;
      try {
        sincetime = AbsTime.factory(st.nextToken());
      } catch (Exception e) {
        itsWriter.println("? BAT timestamp couldn't be parsed");
        itsWriter.flush();
        return;
      }

      //Get/check monitor point name
      String mpname = st.nextToken();
      if (!MonitorMap.checkPointName(mpname)) {
        itsWriter.println("? Named point doesn't exist");
        itsWriter.flush();
        return;
      }

      //Get data between specified time and now
      AbsTime now = new AbsTime();
      Vector data = PointBuffer.getPointData(mpname, sincetime, now, 0);
      if (data==null) data = new Vector();

      //Tell the client how many samples we are going to send
      int numdata = data.size();
      itsWriter.println(numdata);
      //Send each sample
      for (int i=0; i<numdata; i++) {
        PointData pd = (PointData)data.get(i);
        itsWriter.println(pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
      }

      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: since: " + e.getClass());
      itsRunning = false;
    }
  }


  protected
  void
  state_details()
  {
    try {
      //First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i=0; i<numpoints; i++) {
        String pointname = itsReader.readLine().trim();
        PointMonitor pm = MonitorMap.getPointMonitor(pointname);
        if (pm==null) {
          itsWriter.println("?");
        } else {
          itsWriter.println(pointname + "\t" + pm.getPeriod()/1000000.0 + "\t\"" +
			    pm.getUnits() + "\"\t\"" + pm.getLongDesc() + "\"");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: details: " + e.getClass());
      itsRunning = false;
    }
  }


  /** Return latest values for specified monitor points. */
  protected
  void
  state_poll()
  {
    try {
      //First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i=0; i<numpoints; i++) {
        String pointname = itsReader.readLine().trim();
        if (MonitorMap.checkPointName(pointname)) {
          PointData pd = PointBuffer.getPointData(pointname);
          if (pd==null) {
            itsWriter.println(pointname + "\t?\t?");
          } else {
            itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT)
                              + "\t" + pd.getData());
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: poll: " + e.getClass());
      itsRunning = false;
    }
  }

  /** Return next update >= specified times, for specified monitor points. */
  protected
  void
  state_following()
  {
    try {
      //First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i=0; i<numpoints; i++) {
        //Tokenise request line to get pointname and BAT arguments
        StringTokenizer st = new StringTokenizer(itsReader.readLine());      
        if (st.countTokens()!=2) {
          itsWriter.println("? Need BAT timestamp and a point name argument");
          itsWriter.flush();
          continue;
        }
        
        //Read the timestamp argument for this point
        String argtimeascii = st.nextToken().trim();
        AbsTime argtime = null;
        try {
          argtime = AbsTime.factory(argtimeascii);
        } catch (Exception f) {
          itsWriter.println("? Error parsing BAT timestamp \"" + argtimeascii + "\"");
          itsWriter.flush();
          continue;
        }
        
        String pointname = st.nextToken().trim();
        if (MonitorMap.checkPointName(pointname)) {
          //All arguments look good so do the query
          PointData pd = PointBuffer.getFollowing(pointname, argtime);
          if (pd==null) {
            itsWriter.println(pointname + "\t?\t?");
          } else {
            itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT)
                              + "\t" + pd.getData());
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: following: " + e.getClass());
      itsRunning = false;
    }
  }


  /** Return last update <= specified times, for specified monitor points. */
  protected
  void
  state_preceeding()
  {
    try {
      //First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i=0; i<numpoints; i++) {
        //Tokenise request line to get pointname and BAT arguments
        StringTokenizer st = new StringTokenizer(itsReader.readLine());      
        if (st.countTokens()!=2) {
          itsWriter.println("? Need BAT timestamp and a point name argument");
          itsWriter.flush();
          continue;
        }
        
        //Read the timestamp argument for this point
        String argtimeascii = st.nextToken().trim();
        AbsTime argtime = null;
        try {
          argtime = AbsTime.factory(argtimeascii);
        } catch (Exception f) {
          itsWriter.println("? Error parsing BAT timestamp \"" + argtimeascii + "\"");
          itsWriter.flush();
          continue;
        }
        
        String pointname = st.nextToken().trim();
        if (MonitorMap.checkPointName(pointname)) {
          //All arguments look good so do the query
          PointData pd = PointBuffer.getPreceeding(pointname, argtime);
          if (pd==null) {
            itsWriter.println(pointname + "\t?\t?");
          } else {
            itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT)
                              + "\t" + pd.getData());
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: preceeding: " + e.getClass());
      itsRunning = false;
    }
  }


  /** Return latest values, units, and boolean for range check of specified
   * monitor points. */
  protected
  void
  state_poll2()
  {
    try {
      //First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i=0; i<numpoints; i++) {
        String pointname = itsReader.readLine().trim();
        //Make sure the monitor point name is valid
        if (MonitorMap.checkPointName(pointname)) {
          PointMonitor pm = MonitorMap.getPointMonitor(pointname);
          if (pm==null) {
            //Invalid monitor point requested
            itsWriter.println("? Named point doesn't exist");
          } else {
            PointData pd = PointBuffer.getPointData(pointname);
            if (pd==null) {
              //No current data for this monitor point
              itsWriter.println(pointname + "\t?\t?\t?\t?");
            } else {
              PointLimit pl = pm.getLimits();
              boolean limits = true;
              if (pl!=null) {
                //If this point has a limit checker then check latest value
                limits = pl.checkLimits(pd);
              }
              String units = pm.getUnits();
              if (units==null || units=="") {
                units="?";
              }
              itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT)
                                + "\t" + pd.getData() + "\t" + units + "\t" +
                                limits);
            }
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      System.err.println("MonitorServerASCII: poll2: " + e.getClass());
      itsRunning = false;
    }
  }

  /** Starting point for threads. If this object was created without a
   * specified socket then we start a server thread which awaits client
   * connections and spawns new instances to service new clients. If a
   * socket was specified at construction then we know that we are
   * supposed to service a particular client and we leap to the
   * <i>processConnection</i> method to do so. */
  public
  void
  run()
  {
    try {
      if (itsSocket == null) {
        //No socket specified - we are the main server
        //Get the port to listen on for new client connections
        int port = Integer.parseInt(MonitorConfig.getProperty("ASCIIPort"));
        //Create the server socket to listen with
        ServerSocket ss = new ServerSocket(port);

        //Keep looping until we need to stop
        while (itsRunning) {
        try {
          //Await a new client connection
          Socket soc = ss.accept();
          //Got a new client connection, spawn a server to service it
          if (soc!=null) new MonitorServerASCII(soc);
        }
          catch (IOException ie) {}
        }
      } else {
        //We aren't the main server: we need to service a particular client
        processConnection();
        //Keep track of how many servers/clients there are
        theirNumClients--;
      }
    } catch (IOException ie) {
      //Couldn't open the server port
      ///Should probably be a logger message here as well
      System.err.println("MonitorServerASCII::run(): Can't open port");
      System.err.println(ie.getMessage());
      MonitorMap.logger.error("MonitorServerASCII::run(): Can't open port - "
                              + ie.getMessage());
    }
  }
}
