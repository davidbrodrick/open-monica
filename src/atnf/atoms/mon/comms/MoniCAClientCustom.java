//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.net.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

/**
 * Provides client software with convenient access to the network
 * data server. Communication with the server is achieved using a custom
 * encoding scheme based on Java Object Serialisation.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: MonitorClientCustom.java,v 1.9 2005/09/08 03:22:55 bro764 Exp bro764 $
 */
public class
MoniCAClientCustom extends MoniCAClient
{
  /** Name of the server this client should connect to. */
  protected String itsServer = null;
  /** Port we should use to connect to the server. */
  protected int itsPort = -1;
  /** Records if we are currently connected to the server. */
  protected boolean itsConnected  = false;
  /** Indicates if we are presently connecting tot he server. */
  protected boolean itsConnecting = false;
  /** Socket connection to the server. */
  protected Socket itsSocket = null;
  /** Socket timeout in milliseconds. */
  protected int itsTimeout = 15000;
  /** Input stream used for reading data from the server. */
  protected ObjectInputStream itsIn = null;
  /** Output stream used for sending requests/data to the server. */
  protected ObjectOutputStream itsOut = null;

  /** Create new instance connected to the given server on the default port.
   * @param server Hostname of the server to connect to. */
  public
  MoniCAClientCustom(String server)
  throws Exception
  {
    itsServer = server;
    itsPort   = 8050;
    connect();
  }


  /** Create new instance connected to the given server on the given port.
   * @param server Hostname of the server to connect to.
   * @param TCP/IP port number to connect to. */
  public
  MoniCAClientCustom(String server, int port)
  throws Exception
  {
    itsServer = server;
    itsPort   = port;
    connect();
  }


  /** Connect to the server.
   * @return True if connected, False if not connected. */
  public synchronized
  boolean
  connect()
  throws Exception
  {
    itsConnecting = true;
    try {
      itsSocket = new Socket();
      itsSocket.bind(null);
      itsSocket.connect(new InetSocketAddress(itsServer, itsPort), 5000);
      itsSocket.setSoTimeout(itsTimeout); //timeout, in milliseconds
      itsConnected = true;
      itsOut = new ObjectOutputStream(itsSocket.getOutputStream());
      itsOut.flush();
      itsIn = new ObjectInputStream(itsSocket.getInputStream());
      System.err.println("MonitorClientCustom: connected to "
                         + itsServer + ":" + itsPort);
    } catch (Exception e) {
      itsSocket = null;
      itsIn     = null;
      itsOut    = null;
      itsConnected  = false;
      itsConnecting = false;
      throw e;
    }
    itsConnecting = false;
    return itsConnected;
  }


  /** Close the connection to the server. */
  public synchronized
  void
  disconnect()
  throws Exception
  {
    itsConnecting = true;
    if (itsSocket!=null) {
      itsSocket.close();
    }
    itsSocket = null;
    itsIn     = null;
    itsOut    = null;
    itsConnected  = false;
    itsConnecting = false;
  }


  /** Set the socket timeout, in milliseconds. */
  public synchronized
  void
  setTimeout(int timeout)
  {
    itsTimeout=timeout;
    if (itsConnected) {
      try {
        itsSocket.setSoTimeout(itsTimeout);
      } catch (Exception e) {
        try {
          disconnect();
        } catch (Exception f) { }
      }
    }
  }


  /** Return the hostname of our server.
   * @return The server's hostname. */
  public
  String
  getServer()
  {
    return itsServer;
  }


  /** Return the port used to connect to our server.
   * @return The server's listening port. */
  public
  int
  getPort()
  {
    return itsPort;
  }


  /** Return the current connection status.
   * @return Connection status, True if connected, False if disconnected. */
  public synchronized
  boolean
  isConnected()
  {
    return itsConnected;
  }


  /** Return a short descriptive string.
   * @return A short descriptive string. */
  public
  String
  toString()
  {
    String res = itsServer + ":" + itsPort + "(";
    if (isConnected()) {
      res += "connected)";
    } else {
      res += "disconnected)";
    }
    return res;
  }


  /** Send the request to the server and return the servers response.
   * @param req The request to send to the network server.
   */
  public synchronized
  PointData
  makeRequest(MonCustomRequest req)
  {
    try {
      //If we're not connected, try to connect
      if (!itsConnected) {
        if (itsConnecting) {
          return null; //Really, we'd like to wait
        }
        try { connect(); } catch (Exception j) {return null;}
      }

      itsOut.writeObject(req);
      itsOut.flush();
      //Reset the stream to prevent memory leak
      itsOut.reset();
      Object data = itsIn.readObject();
      return (PointData)data;
    } catch (Exception e) {
      try { disconnect(); } catch (Exception j) {}
      System.err.println("MonitorClientCustom:makeRequest: " + e.getMessage());
      System.err.println("##### LOST CONNECTION TO MONITOR SERVER! #####");
      //e.printStackTrace();
    }
    return null;
  }


  /** Return the latest data for all the points named in the Vector. Each
   * String entry in the Vector is expected to be of the format
   * <tt>source.point.name</tt>. The Vector which is returned will contain
   * the latest data for each point, in the same order as the request
   * Vector. If no data was available for a point (eg, point doesn't
   * exist) then <tt>null</tt> will occupy that index in the Vector. */
  public 
  Vector<PointData>
  getData(Vector points)
  {
    MonCustomRequest req = new MonCustomRequest(MonCustomRequest.GETDATA, new Object[]{points});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) {
      return null;
    }
    return (Vector<PointData>)res.getData();
  }


  /** Return archived data for the given points.
   * @param pointnames Names of points to get data for.
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @param maxsamples Maximum number of records to be returned.
   * @return Data from the archive between the specified times, for each point. */
  public 
  Vector<Vector<PointData>>
  getArchiveData(Vector<String> pointnames, AbsTime start, AbsTime end, int maxsamples)
  {
    Vector<Vector<PointData>> res = new Vector<Vector<PointData>>(pointnames.size());
    //Need to request data for each point in turn
    for (int i = 0; i < pointnames.size(); i++) {
      Vector<PointData> thispointdata = new Vector<PointData>();      
      // Server may have limit on number of points returned at once,
      // therefore go into loop which makes consecutive requests.
      AbsTime thisstart = start;
      while (true) {
        MonCustomRequest req = new MonCustomRequest(MonCustomRequest.GETDATA, new Object[] {
            pointnames.get(i), thisstart, end, new Integer(maxsamples) });
        PointData data = makeRequest(req);
        if (data == null || data.getData() == null) {
          break;
        }
        Vector<PointData> thisres = (Vector<PointData>) (MonitorUtils
            .decompress((byte[]) (data.getData())));
        thispointdata.addAll(thisres);
        thisstart = ((PointData) (thisres.get(thisres.size() - 1)))
            .getTimestamp().add(RelTime.factory(1));
      }
      if (thispointdata.size() == 0) {
        thispointdata = null;
      }
      res.add(thispointdata);
    }
    return res;
  }


  /** Return the definition string for all the points named in the Vector.
   * Each entry in the argument Vector is expected to be a String in the
   * format <tt>source.point.name</tt>. The result will be a Vector of
   * Strings representing the definitions for the requested points, in the
   * same order as in the request Vector. If any of the requested points
   * don't exist then it will have <tt>null</tt> in it's index of the result.
   * @param points Vector of point names.
   * @return Vector containing the point initialisation strings. */
  public 
  Vector<PointDescription>
  getPoints(Vector<String> pointnames)
  {
    MonCustomRequest req = new MonCustomRequest(MonCustomRequest.GETPOINT,
				    new Object[]{pointnames});
    PointData response = makeRequest(req);
    if (response==null || response.getData()==null) {
      return null;
    }
    Vector<PointDescription> res = new Vector<PointDescription>(pointnames.size());
    for (int i=0; i<pointnames.size(); i++) {
      String defstring = (String)((Vector)response.getData()).get(i);
      if (defstring!=null) {
        PointDescription newpoint = PointDescription.parseLine(defstring).get(0);
        if (newpoint!=null) {
          newpoint.populateClientFields();
        }
        res.add(newpoint);
      }
    }
    return res;
  }

  /** Return an RSA encryptor that uses the servers public key and modulus.
   * this will allow us to encrypt information that can only be encrypted by
   * the monitor server. */
  public 
  RSA
  getEncryptor()
  {
    //Ask the server to send us its current public key and modulus
    MonCustomRequest req = new MonCustomRequest(MonCustomRequest.GETKEY, null);
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) {
      return null;
    }

    //Get the public key and modulus
    String[] a = (String[])res.getData();
    BigInteger e = new BigInteger(a[0]);
    BigInteger n = new BigInteger(a[1]);
    return new RSA(n, e);
  }

  /** TODO: Unimplemented method. */
  public 
  boolean
  addPoints(Vector<PointDescription> newpoints, String username, String passwd)
  {
    return false;
  }

  /** TODO: Unimplemented method. */
  public 
  boolean
  setData(Vector<String> pointnames, Vector<PointData> values, String username, String passwd)
  {
    return false;
  }
  
  
  /** TODO: This is broken. It should return current time on the server. */
  public 
  AbsTime
  getCurrentTime()
  {
    return AbsTime.factory();
  }


  /** Get the names of all points (including aliases) on the system. 
   * @return Names of all points on the system. */
  public 
  String[]
  getAllPointNames()
  {
    MonCustomRequest req;
    req = new MonCustomRequest(MonCustomRequest.GETPOINTNAMES, null);
    PointData res = makeRequest(req);
    return (String[])res.getData();
  }

  /** Get all of the points on the system.
   * @return Vector containing all point definitions. */
  public 
  Vector<PointDescription>
  getAllPoints()
  {
    MonCustomRequest req = new MonCustomRequest(MonCustomRequest.GETALLPOINTS, null);
    PointData raw = makeRequest(req);
    if (raw == null) {
      return null;
    }
    String[] pointdefs = (String[])MonitorUtils.decompress((byte[])raw.getData());
    Vector<PointDescription> res = new Vector<PointDescription>(pointdefs.length);
    for (int i=0; i<pointdefs.length; i++) {
      PointDescription newpoint = PointDescription.parseLine(pointdefs[i]).get(0);
      newpoint.populateClientFields();
      res.add(newpoint);
    }
    return res;
  }


  /** Return all SavedSetups for client Objects from the server.
   * <tt>null</tt> may be returned if the server has no SavedSetups. */
  public
  Vector<SavedSetup>
  getAllSetups()
  {
    MonCustomRequest req = new MonCustomRequest(MonCustomRequest.GETALLSETUPS, null);
    PointData response = makeRequest(req);
    if (response == null) {
      return null;
    }
    SavedSetup[] setups = (SavedSetup[])MonitorUtils.decompress((byte[])response.getData());
    Vector<SavedSetup> result = null;
    if (setups!=null) {
      result = new Vector<SavedSetup>(setups.length);
      for (int i=0; i<setups.length; i++) {
        result.add(setups[i]);
      }
    }
    return result;
  }


  /** Add a new SavedSetup to the server. This requires authentication to
   * prevent inappropriate modification of the server data. The username
   * and password are sent to the server using strong asymmetric key
   * encryption so that they cannot be read if intercepted.
   * @param setup The SavedSetup to add to the server.
   * @param username Username to authenticate.
   * @param password Password to authenticate the user.
   * @return True if the setup was added, False if it couldn't be added. */
  public 
  boolean
  addSetup(SavedSetup setup,
	  String username,
	  String password)
  {
    int retries = 2;
    while (retries > 0) {
      //Get an RSA encryptor with right public key and modulus
      RSA encryptor = getEncryptor();

      // Encrypt the username/password
      String enc_name = encryptor.encrypt(username);
      String enc_pass = encryptor.encrypt(password);
      System.err.println("ENCRYPTED USERNAME = " + enc_name);

      // Make the correct arguments
      Object[] args = new Object[]{enc_name, enc_pass, setup};
      MonCustomRequest req = new MonCustomRequest(MonCustomRequest.ADDSETUP, args);
      //Send request to the server
      PointData res = makeRequest(req);
      //Check if the Page was added OK
      if (res.getData() instanceof Boolean &&
          ((Boolean)res.getData()).booleanValue()) {
        return true;
      }
      retries--;
    }
    return false;
  }


  /** Test if the class works. */
  public static
  void
  main(String[] args)
  {
    if (args.length<1) {
      System.err.println("USAGE: Needs server host name argument");
      System.exit(1);
    }

    //Create instance and connect it to server
    MoniCAClientCustom MCC = null;
    try {
      MCC = new MoniCAClientCustom(args[0]);
      if (!MCC.connect()) {
        System.err.println("ERROR: Couldn't connect to " + args[0]);
        System.exit(1);
      }
    } catch (Exception e) {
      System.err.println("ERROR: Couldn't connect: " + e.getMessage());
      System.exit(1);
    }

    //Connected. Now can do something
  }
}
