//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.client;

import java.net.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.mon.translation.*;
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
MonitorClientCustom
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

  /*
   protected static GZIPOutputStream itsGout = null;
   protected static GZIPInputStream itsGin = null;
   protected static CompressedOutputStream itsCos = null;
   protected static CompressedInputStream itsCis = null;
*/


  /** Create new instance connected to the given server on the default port.
   * @param server Hostname of the server to connect to. */
  public
  MonitorClientCustom(String server)
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
  MonitorClientCustom(String server, int port)
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
    if (itsSocket!=null) itsSocket.close();
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
    if (isConnected()) res += "connected)";
    else res += "disconnected)";
    return res;
  }


  /** Send the request to the server and return the servers response.
   * @param req The request to send to the network server.
   */
  public synchronized
  PointData
  makeRequest(MonRequest req)
  {
    try {
      //If we're not connected, try to connect
      if (!itsConnected) {
        if (itsConnecting) return null; //Really, we'd like to wait
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


  /** Return the latest data for the specified point from the specified
   * source. */
  public 
  PointData
  getPointData(String name, String source)
  {
    MonRequest req = new MonRequest(MonRequest.GETDATA,
       			    new Object[]{new String(source+"."+name)});
    return makeRequest(req);
  }


  /** Return the latest data for the specified point. The argument is
   * expected to be of the format <tt>source.point.name</tt>. */
  public 
  PointData
  getPointData(String point)
  {
    MonRequest req = new MonRequest(MonRequest.GETDATA, new Object[]{point});
    return makeRequest(req);
  }


  /** Return the latest data for all the points named in the Vector. Each
   * String entry in the Vector is expected to be of the format
   * <tt>source.point.name</tt>. The Vector which is returned will contain
   * the latest data for each point, in the same order as the request
   * Vector. If no data was available for a point (eg, point doesn't
   * exist) then <tt>null</tt> will occupy that index in the Vector. */
  public 
  Vector
  getPointData(Vector points)
  {
    MonRequest req = new MonRequest(MonRequest.GETDATA, new Object[]{points});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;
    return (Vector)res.getData();
  }


  /** Return archived data for the given point.
   * @param point Point to get data for. Expected format is
   *                                     <tt>source.point.name</tt>
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @return Data from the archive between the specified times. */
  public 
  Vector
  getPointData(String point, AbsTime start, AbsTime end)
  {
    //Server may have limit on number of points returned at once,
    //therefore go into loop which makes consecutive requests.
    Vector res=new Vector();
    AbsTime thisstart=start;
    while (true) {
      MonRequest req=new MonRequest(MonRequest.GETDATA, new Object[]{point, thisstart, end});
      PointData data=makeRequest(req);
      if (data==null || data.getData()==null) break;
      Vector thisres=(Vector)(MonitorUtils.decompress((byte[])(data.getData())));
      res.addAll(thisres);
      thisstart=((PointData)(thisres.get(thisres.size()-1))).getTimestamp().add(RelTime.factory(1));
    }
    if (res.size()==0) res=null;
    return res;
  }


  /** Return archived data for the given point. The sampling rate specifies
   * the minimum interval between data points. This should be useful when
   * we want to look at long time scale trends without wanting to process
   * millions of data points.
   * @param point Point to get data for. Expected format is
   *                                     <tt>source.point.name</tt>
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @param sample_rate Interval between returned data points.
   * @return Data from the archive between the specified times. */
  public 
  Vector
  getPointData(String point, AbsTime start, AbsTime end, int sample_rate)
  {
    //Server may have limit on number of points returned at once,
    //therefore go into loop which makes consecutive requests.
    Vector res=new Vector();
    AbsTime thisstart=start;
    while (true) {
      MonRequest req = new MonRequest(MonRequest.GETDATA, new Object[]{point, thisstart, end, new Integer(sample_rate)});
      PointData data=makeRequest(req);
      if (data==null || data.getData()==null) break;
      Vector thisres=(Vector)(MonitorUtils.decompress((byte[])(data.getData())));
      res.addAll(thisres);
      thisstart=((PointData)(thisres.get(thisres.size()-1))).getTimestamp().add(RelTime.factory(1));
    }
    if (res.size()==0) res=null;
    return res;
  }


  /** Return the definition string for the point from the given source.
   * This string contains all information required to gather the raw data
   * from the real world as well as process and display the point value.
   * This is an overloaded method provided for convenience.
   * @param name Name of point to download.
   * @param source The data source we're interested in.
   * @return Point initialisation string. */
  public 
  String
  getPointMonitor(String name, String source)
  {
    //Call overloaded method
    return getPointMonitor(source + "." + name);
  }


  /** Return the definition string for the given point.
   * This string contains all information required to gather the raw data
   * from the real world as well as process and display the point value.
   * @param point Point to get information for. Expected format is
   *                                     <tt>source.point.name</tt>
   * @param point
   * @return Point initialisation string. */
  public 
  String
  getPointMonitor(String point)
  {
    MonRequest req = new MonRequest(MonRequest.GETPOINT,
				    new Object[]{point});
    PointData res = makeRequest(req);
    if (res==null) return null;
    return (String)res.getData();
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
  Vector
  getPointMonitors(Vector points)
  {
    MonRequest req = new MonRequest(MonRequest.GETPOINT,
				    new Object[]{points});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;
    return (Vector)res.getData();
  }


  /** Return the <i>Transaction</i> object for the given point. Note the
   * name must be complete, including source name.
   * @param point Name of point.
   * @return Transaction object (or null if not found). */
  public
  Transaction
  getTransaction(String point)
  {
    MonRequest req = new MonRequest(MonRequest.GETTRANSACTION,
				    new Object[]{point});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;
    String transstr = (String)res.getData();
    return Transaction.factory(null, transstr);
  }

  /** Return the <i>Transaction</i> objects for the points whose names
   * are contained in the Vector. Note the names must be complete, including
   * source name.
   * @param points Vector of point names.
   * @return Vector of Transactions. */
  public
  Vector
  getTransactions(Vector points)
  {
    MonRequest req = new MonRequest(MonRequest.GETTRANSACTION,
				    new Object[]{points});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;
    Vector strvec = (Vector)res.getData();
    Vector transvec = new Vector(points.size());
    for (int i=0; i<points.size(); i++) {
      transvec.add(Transaction.factory(null, (String)strvec.get(i)));
    }
    return transvec;
  }

  /** Return the <i>Translation</i> objects for the given point. Note the
   * name must be complete, including source name.
   * @param point Name of point.
   * @return Array of Translation objects (or null if not found). */
  public
  String
  getTranslation(String point)
  {
    MonRequest req = new MonRequest(MonRequest.GETTRANSLATION,
				    new Object[]{point});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;
    return (String)res.getData();
  }

  /** Return the <i>Translation</i> objects for the points whose names
   * are contained in the Vector. Note the names must be complete, including
   * source name.
   * @param points Vector of point names.
   * @return Vector of Arrays of Translations. */
  public
  Vector
  getTranslations(Vector points)
  {
    MonRequest req = new MonRequest(MonRequest.GETTRANSLATION,
				    new Object[]{points});
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;
    return (Vector)res.getData();
  }

  /** Return an RSA encryptor that uses the servers public key and modulus.
   * this will allow us to encrypt information that can only be encrypted by
   * the monitor server. */
  public 
  RSA
  getEncryptor()
  {
    //Ask the server to send us its current public key and modulus
    MonRequest req = new MonRequest(MonRequest.GETKEY, null);
    PointData res = makeRequest(req);
    if (res==null || res.getData()==null) return null;

    //Get the public key and modulus
    String[] a = (String[])res.getData();
    BigInteger e = new BigInteger(a[0]);
    BigInteger n = new BigInteger(a[1]);
    return new RSA(n, e);
  }



  public 
  boolean
  setPoint(String point, String username, String password, String new_init)
  {
    int retries = 2;
    while (retries > 0) {
      //Get an RSA encryptor with right public key and modulus
      RSA encryptor = getEncryptor();

      // Encrypt the username/password
      String enc_name = encryptor.encrypt(username);
      String enc_pass = encryptor.encrypt(password);

      // Make the correct arguments
      Object[] args = new Object[]{point,new_init,enc_name,enc_pass};
      MonRequest req = new MonRequest(MonRequest.SETPOINT, args);
      PointData res = makeRequest(req);
      if (res.getData() instanceof Boolean && ((Boolean)res.getData()).booleanValue()) return true;
      retries--;
    }
    return false;
  }


  public 
  boolean
  setPoint(String name, String source, String username,
	   String password, String new_init)
  {
    return setPoint(source+"."+name, username, password, new_init);
  }


  /** Add a new point to the servers pool. This is a privileged
   * operation which requires the user to authenticate against the server.
   * The username and password are encrypted using strong asymmetric key
   * encryption prior to transmission over the network.
   * @param init The string required to create and configure the point.
   * @param username Username to authenticate against server.
   * @param password Password to authenticate against server.
   * @return True if point added, False if not added. */
  public 
  boolean
  addPoint(String init, String username, String password)
  {
    int retries = 2;
    while (retries > 0) {
      //Get an RSA encryptor with right public key and modulus
      RSA encryptor = getEncryptor();

      // Encrypt the username/password
      String enc_name = encryptor.encrypt(username);
      String enc_pass = encryptor.encrypt(password);

      // Make the correct arguments
      Object[] args = new Object[]{init,enc_name,enc_pass};
      MonRequest req = new MonRequest(MonRequest.ADDPOINT, args);
      PointData res = makeRequest(req);
      if (res.getData() instanceof Boolean && ((Boolean)res.getData()).booleanValue()) return true;
      retries--;
    }
    return false;
  }


  // For convenience only
  public 
  FakeMonitor
  makeFakeMonitor(String init)
  {
    // Use the PointInteraction to make the point
    ArrayList al = PointInteraction.parseLine(init, false);
    // Check
    if (al == null || al.size() < 1) return null;
    // Only want one point
    return (FakeMonitor)al.get(0);
  }


  /** Why is this here??? It returns the current time as a long. */
  public 
  long
  getCurrentTime()
  {
    return AbsTime.factory().getValue();
  }


  public 
  String[]
  getPointNames(String filter)
  {
    MonRequest req;
    if (filter == null) req = new MonRequest(MonRequest.GETPOINTNAMES, null);
    else req = new MonRequest(MonRequest.GETPOINTNAMES, new Object[]{filter});
    PointData res = makeRequest(req);
    return (String[])res.getData();
  }


  public 
  String[]
  getPointNamesShort(String filter)
  {
    MonRequest req;
    if (filter == null) req = new MonRequest(MonRequest.GETPOINTNAMES_SHORT, null);
    else req = new MonRequest(MonRequest.GETPOINTNAMES_SHORT, new Object[]{filter});
    PointData res = makeRequest(req);
    return (String[])res.getData();
  }


  /** Get all points */
  public 
  String[]
  getPointNames()
  {
    return getPointNames(null);
  }


  public 
  String[]
  getPointNamesShort()
  {
    return getPointNamesShort(null);
  }


  /** Get complete list of data sources operating on the server. */
  public 
  String[]
  getSources()
  {
    MonRequest req = new MonRequest(MonRequest.GETSOURCES, null);
    PointData res = makeRequest(req);
    return (String[])res.getData();
  }


  /** Return the names of all sources known by the system.
   * @return Array of all source names. */
  public 
  String[]
  getAllSources()
  {
    MonRequest req = new MonRequest(MonRequest.GETSOURCES, null);
    PointData res = makeRequest(req);
    if (res == null) return null;
    return (String[])res.getData();
  }


  /** Return the names of all sources for the given point.
   * @param name Name of the point of interest.
   * @return Array of all sources for the nominated point. */
  public 
  String[]
  getSources(String name)
  {
    MonRequest req = new MonRequest(MonRequest.GETSOURCES, new Object[]{name});
    PointData res = makeRequest(req);
    if (res == null) return null;
    return (String[])res.getData();
  }


  /** Return the names of all sources for each of the given points.
   * This is basically just a batched version of <i>getSources(String)</i>
   * to speed things up when we're interested in many points/sources. The
   * return Vector will be of the same length as the argument Vector. Each
   * entry will be an array of Strings or possibly <tt>null</tt>.
   * @param names Vector containing String names for the points.
   * @return Vector containing an array of names for each requested point. */
  public 
  Vector
  getSources(Vector names)
  {
    if (names==null || names.size()==0) return null;

    MonRequest req = new MonRequest(MonRequest.GETSOURCES, new Object[]{names});
    PointData res = makeRequest(req);
    if (res == null) return null;
    return (Vector)res.getData();
  }


  public 
  String[]
  getAllPoints()
  {
    MonRequest req = new MonRequest(MonRequest.GETALLPOINTS, null);
    PointData res = makeRequest(req);
    if (res == null) return null;
    return (String[])MonitorUtils.decompress((byte[])res.getData());
  }


  /** Return all SavedSetups for client Objects from the server.
   * <tt>null</tt> may be returned if the server has no SavedSetups. */
  public
  SavedSetup[]
  getAllSetups()
  {
    MonRequest req = new MonRequest(MonRequest.GETALLSETUPS, null);
    PointData res = makeRequest(req);
    if (res == null) return null;
    return (SavedSetup[])MonitorUtils.decompress((byte[])res.getData());
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
      MonRequest req = new MonRequest(MonRequest.ADDSETUP, args);
      //Send request to the server
      PointData res = makeRequest(req);
      //Check if the Page was added OK
      if (res.getData() instanceof Boolean &&
          ((Boolean)res.getData()).booleanValue()) return true;
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
    MonitorClientCustom MCC = null;
    try {
      MCC = new MonitorClientCustom(args[0]);
      if (!MCC.connect()) {
        System.err.println("ERROR: Couldn't connect to " + args[0]);
        System.exit(1);
      }
    } catch (Exception e) {
      System.err.println("ERROR: Couldn't connect: " + e.getMessage());
      System.exit(1);
    }

    //Print all pages to screen just to prove things work
    SavedSetup[] setups = MCC.getAllSetups();
    for (int i=0; i<setups.length; i++) {
      System.out.println(setups[i]);
    }
  }
}
