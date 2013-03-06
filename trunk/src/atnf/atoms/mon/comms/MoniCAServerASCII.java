//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmManager;
import atnf.atoms.mon.PointBuffer;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.util.MonitorConfig;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.AbsTime;

/**
 * Provides a simple ASCII interface for clients to obtain monitor data.
 * 
 * @author David Brodrick
 */
public class MoniCAServerASCII extends Thread {
  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(MoniCAServerASCII.class.getName());

  /** Keep track of how many clients are connected. */
  protected static int theirNumClients = 0;

  /** Indicates if the thread should keep running (true) or stop (false). */
  protected boolean itsRunning = true;

  /** The socket used to listen for requests from our client. */
  protected Socket itsSocket = null;

  /** The name of the client. */
  protected String itsClientName = null;

  /** For writing data to the client. */
  protected PrintWriter itsWriter = null;

  /** For reading instructions from the client. */
  protected BufferedReader itsReader = null;

  /** List of all currently running servers. */
  protected static Vector<MoniCAServerASCII> theirServers = new Vector<MoniCAServerASCII>();

  /** Server socket SO timeout (ms). */
  protected static int theirServerSocketTimeout = 100;

  /** Starts up the main server thread which waits for client connections. */
  public MoniCAServerASCII() {
    super("MonitorServerASCII Main");
    synchronized (theirServers) {
      theirServers.add(this);
    }
    start();
  }

  /**
   * Starts up a server thread to handle requests from a new client.
   * 
   * @param socket
   *          The socket connection to the new client.
   */
  public MoniCAServerASCII(Socket socket) throws IOException {
    super("MonitorServerASCII/" + socket.getInetAddress().getHostAddress());
    synchronized (theirServers) {
      theirServers.add(this);
    }
    theirNumClients++;
    itsClientName = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    // System.out.println("MonitorServerASCII: New Connection from " +
    // itsClientName);
    itsSocket = socket;
    itsReader = new BufferedReader(new InputStreamReader(itsSocket.getInputStream()));
    itsWriter = new PrintWriter(itsSocket.getOutputStream());
    start();
  }

  /** Return the number of clients connected. */
  public static int getNumClients() {
    return theirNumClients;
  }

  /** Make all servers exit. */
  public static void stopAll() {
    synchronized (theirServers) {
      Iterator<MoniCAServerASCII> i = theirServers.iterator();
      while (i.hasNext()) {
        i.next().stopRunning();
      }
    }
    try {
      Thread.sleep(2 * theirServerSocketTimeout);
    } catch (InterruptedException e) {
    }
  }

  /** Stop the running thread. */
  public void stopRunning() {
    itsRunning = false;
    interrupt();
  }

  /** Main loop to parse and service client requests. */
  private void processConnection() {
    // Main loop for handling client requests
    while (itsRunning) {
      try {
        synchronized (itsReader) {
          String line = itsReader.readLine();
          if (line == null) {
            // Connection broke..
            itsRunning = false;
            return;
          }
          line = line.trim();
          if (line.equalsIgnoreCase("poll")) {
            poll();
          } else if (line.equalsIgnoreCase("poll2")) {
            poll2();
          } else if (line.equalsIgnoreCase("since")) {
            since();
          } else if (line.equalsIgnoreCase("between")) {
            between();
          } else if (line.equalsIgnoreCase("preceding") || line.equalsIgnoreCase("preceeding")) {
            // Original interface had spelling error
            preceding();
          } else if (line.equalsIgnoreCase("following")) {
            following();
          } else if (line.equalsIgnoreCase("names")) {
            names();
          } else if (line.equalsIgnoreCase("details")) {
            details();
          } else if (line.equalsIgnoreCase("set")) {
            set();
          } else if (line.equalsIgnoreCase("ack")) {
            ack();
          } else if (line.equalsIgnoreCase("shelve")) {
            shelve();
          } else if (line.equalsIgnoreCase("alarms")) {
            alarms();
          } else if (line.equalsIgnoreCase("allalarms")) {
            allalarms();
          } else if (line.equalsIgnoreCase("exit")) {
            itsRunning = false;
          }
        }
      } catch (Exception f) {
        System.err.println("MonitorServerASCII: processConnection: " + f.getClass());
        f.printStackTrace();
        itsRunning = false;
      }
    }
    // System.out.println("MonitorServerASCII: Lost Connection to " + itsSocket.getInetAddress().getHostAddress());
    try {
      itsReader.close();
      itsWriter.close();
      if (!itsSocket.isClosed()) {
        itsSocket.close();
      }
    } catch (Exception e) {
      theirLogger.warn("When closing socket: " + e);
    }
  }

  /** Return the names of all monitor points. */
  protected void names() {
    try {
      // Get the list of all names
      String[] names = PointDescription.getAllPointNames();

      // Tell the client how many names we will return
      itsWriter.println(names.length);

      // Send each name
      for (int i = 0; i < names.length; i++) {
        itsWriter.println(names[i]);
      }

      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in names request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  protected void between() {
    try {
      String tempstr = itsReader.readLine().trim();
      // Line should say <TIMESTAMP> <TIMESTAMP> <POINTNAME>
      StringTokenizer st = new StringTokenizer(tempstr);
      if (st.countTokens() != 3) {
        itsWriter.println("? Need two BAT timestamps and a point name argument");
        itsWriter.flush();
        return;
      }

      // Get/check start timestamp
      AbsTime starttime = null;
      try {
        starttime = AbsTime.factory(st.nextToken());
      } catch (Exception e) {
        itsWriter.println("? First BAT timestamp couldn't be parsed");
        itsWriter.flush();
        return;
      }

      // Get/check end timestamp
      AbsTime endtime = null;
      try {
        endtime = AbsTime.factory(st.nextToken());
      } catch (Exception e) {
        itsWriter.println("? Second BAT timestamp couldn't be parsed");
        itsWriter.flush();
        return;
      }

      // Ensure start/end arguments are in the correct sequence
      if (endtime.isBefore(starttime)) {
        AbsTime temp = endtime;
        endtime = starttime;
        starttime = temp;
      }

      // Get/check monitor point name
      String mpname = st.nextToken();
      checkPoint(mpname);
      if (!PointDescription.checkPointName(mpname)) {
        itsWriter.println("? Named point doesn't exist");
        itsWriter.flush();
        return;
      }

      // Get data between the specified times
      Vector data = PointBuffer.getPointData(mpname, starttime, endtime, 0);
      if (data == null) {
        data = new Vector();
      }

      // Tell the client how many samples we are going to send
      int numdata = data.size();
      itsWriter.println(numdata);
      // Send each sample
      for (int i = 0; i < numdata; i++) {
        PointData pd = (PointData) data.get(i);
        itsWriter.println(pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
      }

      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in between request from " + itsClientName + ": " + e);
      e.printStackTrace();
      itsRunning = false;
    }
  }

  protected void since() {
    try {
      String tempstr = itsReader.readLine().trim();
      // Line should say <TIMESTAMP> <POINTNAME>
      StringTokenizer st = new StringTokenizer(tempstr);
      if (st.countTokens() != 2) {
        itsWriter.println("? Need BAT timestamp and point name arguments");
        itsWriter.flush();
        return;
      }

      // Get/check timestamp
      AbsTime sincetime = null;
      try {
        sincetime = AbsTime.factory(st.nextToken());
      } catch (Exception e) {
        itsWriter.println("? BAT timestamp couldn't be parsed");
        itsWriter.flush();
        return;
      }

      // Get/check monitor point name
      String mpname = st.nextToken();
      checkPoint(mpname);
      if (!PointDescription.checkPointName(mpname)) {
        itsWriter.println("? Named point doesn't exist");
        itsWriter.flush();
        return;
      }

      // Get data between specified time and now
      AbsTime now = new AbsTime();
      Vector data = PointBuffer.getPointData(mpname, sincetime, now, 0);
      if (data == null) {
        data = new Vector();
      }

      // Tell the client how many samples we are going to send
      int numdata = data.size();
      itsWriter.println(numdata);
      // Send each sample
      for (int i = 0; i < numdata; i++) {
        PointData pd = (PointData) data.get(i);
        itsWriter.println(pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
      }

      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in since request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  protected void details() {
    try {
      // First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        String pointname = itsReader.readLine().trim();
        checkPoint(pointname);
        PointDescription pm = PointDescription.getPoint(pointname);
        if (pm == null) {
          itsWriter.println("?");
        } else {
          itsWriter.println(pointname + "\t" + pm.getPeriod() / 1000000.0 + "\t\"" + pm.getUnits() + "\"\t\"" + pm.getLongDesc() + "\"");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in details request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Return latest values for specified monitor points. */
  protected void poll() {
    try {
      // First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        String pointname = itsReader.readLine().trim();
        checkPoint(pointname);
        if (PointDescription.checkPointName(pointname)) {
          PointData pd = PointBuffer.getPointData(pointname);
          if (pd == null) {
            itsWriter.println(pointname + "\t?\t?");
          } else {
            itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in poll request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Specify new values for the given points. */
  protected void set() {
    try {
      // Read users credentials. These should be encrypted. TODO: Currently not
      // used.
      String username = itsReader.readLine().trim();
      String password = itsReader.readLine().trim();

      // First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        String[] tokens = itsReader.readLine().trim().split("\t");
        if (tokens.length < 3) {
          itsWriter.println("? Expect name, type code and value. Tab delimited.");
          continue;
        }
        
        checkPoint(tokens[0]);
        PointDescription thispoint = PointDescription.getPoint(tokens[0]);
        if (thispoint != null) {
          PointData newval = new PointData(thispoint.getFullName());
          String type = tokens[1];
          String strval = tokens[2];
          try {
            newval.setData(MonitorUtils.parseFixedValue(type, strval));
          } catch (Exception f) {
            // Parse error
            itsWriter.println("? Parse error reading type/value: " + f);
            continue;
          }

          theirLogger.trace("Assigning value " + newval + " to point " + thispoint.getFullName() + " as requested by " + username + " from " + itsClientName);
          thispoint.firePointEvent(new PointEvent(this, newval, true));
          itsWriter.println(thispoint.getFullName() + "\tOK");
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in set request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Return the current priority alarm states. */
  protected void alarms() {
    try {
      Vector<Alarm> thesealarms = AlarmManager.getAlarms();

      // Tell the client how many alarms we will return
      itsWriter.println(thesealarms.size());

      // Send each alarm
      for (int i = 0; i < thesealarms.size(); i++) {
        itsWriter.println(thesealarms.get(i));
      }

      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in alarms request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Return a list of all alarms. */
  protected void allalarms() {
    try {
      Vector<Alarm> thesealarms = AlarmManager.getAllAlarms();

      // Tell the client how many alarms we will return
      itsWriter.println(thesealarms.size());

      // Send each alarm
      for (int i = 0; i < thesealarms.size(); i++) {
        itsWriter.println(thesealarms.get(i));
      }

      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in allalarms request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Set the acknowledge state for an alarm. */
  protected void ack() {
    try {
      // Read users credentials. These should be encrypted. TODO: Currently not
      // used.
      String username = itsReader.readLine().trim();
      String password = itsReader.readLine().trim();

      AbsTime now = AbsTime.factory();

      // First line tells us how many alarms are going to be acknowledged
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        String[] tokens = itsReader.readLine().trim().split("\t");
        if (tokens.length < 2) {
          itsWriter.println("? Expect name, and acknowledgement value. Tab delimited.");
          continue;
        }
        
        checkPoint(tokens[0]);
        PointDescription thispoint = PointDescription.getPoint(tokens[0]);
        if (thispoint != null) {
          boolean ackval = Boolean.parseBoolean(tokens[1]);
          AlarmManager.setAcknowledged(thispoint, ackval, username, now);
          theirLogger.debug("Point \"" + tokens[0] + "\" acknowledged=" + ackval + " by \"" + username + "@" + itsClientName + "\"");
          itsWriter.println(thispoint.getFullName() + "\tOK");
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in ack request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Set the shelf state for an alarm. */
  protected void shelve() {
    try {
      // Read users credentials. These should be encrypted. TODO: Currently not
      // used.
      String username = itsReader.readLine().trim();
      String password = itsReader.readLine().trim();

      AbsTime now = AbsTime.factory();

      // First line tells us how many alarms are going to be acknowledged
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        String[] tokens = itsReader.readLine().trim().split("\t");
        if (tokens.length < 2) {
          itsWriter.println("? Expect name, and acknowledgement value. Tab delimited.");
          continue;
        }
        
        checkPoint(tokens[0]);
        PointDescription thispoint = PointDescription.getPoint(tokens[0]);
        if (thispoint != null) {
          boolean ackval = Boolean.parseBoolean(tokens[1]);
          AlarmManager.setShelved(thispoint, ackval, username, now);
          theirLogger.debug("Point \"" + tokens[0] + "\" shelved=" + ackval + " by \"" + username + "@" + itsClientName + "\"");
          itsWriter.println(thispoint.getFullName() + "\tOK");
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in shelve request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Return next update >= specified times, for specified monitor points. */
  protected void following() {
    try {
      // First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        // Tokenise request line to get pointname and BAT arguments
        StringTokenizer st = new StringTokenizer(itsReader.readLine());
        if (st.countTokens() != 2) {
          itsWriter.println("? Need BAT timestamp and a point name argument");
          itsWriter.flush();
          continue;
        }

        // Read the timestamp argument for this point
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
        checkPoint(pointname);
        if (PointDescription.checkPointName(pointname)) {
          // All arguments look good so do the query
          PointData pd = PointBuffer.getFollowing(pointname, argtime);
          if (pd == null) {
            itsWriter.println(pointname + "\t?\t?");
          } else {
            itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in following request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Return last update <= specified times, for specified monitor points. */
  protected void preceding() {
    try {
      // First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        // Tokenise request line to get pointname and BAT arguments
        StringTokenizer st = new StringTokenizer(itsReader.readLine());
        if (st.countTokens() != 2) {
          itsWriter.println("? Need BAT timestamp and a point name argument");
          itsWriter.flush();
          continue;
        }

        // Read the timestamp argument for this point
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
        checkPoint(pointname);
        if (PointDescription.checkPointName(pointname)) {
          // All arguments look good so do the query
          PointData pd = PointBuffer.getPreceding(pointname, argtime);
          if (pd == null) {
            itsWriter.println(pointname + "\t?\t?");
          } else {
            itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData());
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in preceding request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /**
   * Return latest values, units, and boolean for range check of specified monitor points.
   */
  protected void poll2() {
    try {
      // First line tells us how many points are going to be specified
      String tempstr = itsReader.readLine().trim();
      int numpoints = Integer.parseInt(tempstr);
      for (int i = 0; i < numpoints; i++) {
        String pointname = itsReader.readLine().trim();
        // Make sure the monitor point name is valid
        checkPoint(pointname);
        if (PointDescription.checkPointName(pointname)) {
          PointDescription pm = PointDescription.getPoint(pointname);
          if (pm == null) {
            // Invalid monitor point requested
            itsWriter.println("? Named point doesn't exist");
          } else {
            PointData pd = PointBuffer.getPointData(pointname);
            if (pd == null) {
              // No current data for this monitor point
              itsWriter.println(pointname + "\t?\t?\t?\t?");
            } else {
              String units = pm.getUnits();
              if (units == null || units == "") {
                units = "?";
              }
              itsWriter.println(pointname + "\t" + pd.getTimestamp().toString(AbsTime.Format.HEX_BAT) + "\t" + pd.getData() + "\t" + units + "\t"
                  + !pd.getAlarm());
            }
          }
        } else {
          itsWriter.println("? Named point doesn't exist");
        }
      }
      itsWriter.flush();
    } catch (Exception e) {
      theirLogger.error("Problem in poll2 request from " + itsClientName + ": " + e);
      itsRunning = false;
    }
  }

  /** Check if the point is valid and log appropriate messages if it is not. */
  private void checkPoint(String name) {
    int type = PointDescription.checkPointNameType(name);
    if (type < 0) {
      theirLogger.debug("Request for non-existent point \"" + name + "\" from \"" + itsClientName + "\"");
    } else if (type > 0) {
      theirLogger.debug("Request for alias name \"" + name + "\" from \"" + itsClientName + "\"");
    }
  }
  
  /**
   * Starting point for threads. If this object was created without a specified socket then we start a server thread which awaits
   * client connections and spawns new instances to service new clients. If a socket was specified at construction then we know that
   * we are supposed to service a particular client and we leap to the <i>processConnection</i> method to do so.
   */
  public void run() {
    try {
      if (itsSocket == null) {
        // No socket specified - we are the main server
        // Get the port to listen on for new client connections
        int port = Integer.parseInt(MonitorConfig.getProperty("ASCIIPort"));
        // Create the server socket to listen with
        ServerSocket ss = new ServerSocket(port);
        ss.setSoTimeout(theirServerSocketTimeout);

        // Keep looping until we need to stop
        while (itsRunning) {
          try {
            // Await a new client connection
            Socket soc = ss.accept();
            // Got a new client connection, spawn a server to service it
            if (soc != null) {
              new MoniCAServerASCII(soc);
            }
          } catch (IOException ie) {
          }
        }
        ss.close();

      } else {
        // We aren't the main server: we need to service a particular client
        processConnection();
        // Keep track of how many servers/clients there are
        theirNumClients--;
      }
    } catch (IOException ie) {
      // Couldn't open the server port
      theirLogger.error("Can't open server port: " + ie);
    }
    theirServers.remove(this);
  }
}
