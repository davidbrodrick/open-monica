//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.math.BigInteger;
import java.util.Vector;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.RSA;
import atnf.atoms.time.AbsTime;

/** Ice implementation of a MoniCAClient.
 * 
 * @author David Brodrick */
public class MoniCAClientIce extends MoniCAClient {
  /** Host name that we are connected to. */
  protected String itsHost;
  /** The port to connect to on the server. */
  protected int itsPort = getDefaultPort();
  /** The underlaying Ice client. */
  protected MoniCAIcePrx itsIceClient;
  /** The Ice communicator used to talk with the server. */
  protected Ice.Communicator itsCommunicator;
  
  /** Connect to the specified host. */
  public MoniCAClientIce(String host)
  throws Exception
  {
    itsHost = host;
    connect();
  }
  
  /** Connect to the specified host and port. */
  public MoniCAClientIce(String host, int port)
  throws Exception
  {
    itsHost = host;
    itsPort = port;
    connect();
  }
  
  /** Return the current connection status.
   * @return Connection status, True if connected, False if disconnected. */
  public boolean isConnected()
  {
    if (itsIceClient==null) {
      return false;
    } else {
      return true;
    }
  }

  /** Get the names of all points (including aliases) on the system. 
   * @return Names of all points on the system. */
  public String[] getAllPointNames()
  throws Exception
  {
    try {
      if (!isConnected()) {
        connect();
      }
      return itsIceClient.getAllPointNames();
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getAllPointNames:" + e.getClass());
      disconnect();
      throw e;
    }
  }

  /** Get the points with the specified names. The populateClientFields method should
   * be invoked on each point prior to returning the result.
   * @param pointnames Vector containing point names to be retrieved.
   * @return Vector containing all point definitions. */
  public Vector<PointDescription> getPoints(Vector<String> pointnames)
  throws Exception
  {
    Vector<PointDescription> res = null;
    try {
      if (!isConnected()) {
        connect();
      }
      String[] namesarray = new String[pointnames.size()];
      for (int i=0; i<pointnames.size(); i++) {
        namesarray[i] = pointnames.get(i);
      }
      PointDescriptionIce[] icepoints = itsIceClient.getPoints(namesarray);
      res = MoniCAIceUtil.getPointDescriptionsFromIce(icepoints);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getPoints:" + e.getClass());
      disconnect();
      throw e;
    }   
    return res;
  }
  
  /** Get all of the points on the system.
   * @return Vector containing all point definitions. */
  public Vector<PointDescription> getAllPoints()
  throws Exception
  {
    Vector<PointDescription> res = null;
    try {
      if (!isConnected()) {
        connect();
      }
      PointDescriptionIce[] icepoints = itsIceClient.getAllPoints();
      res = MoniCAIceUtil.getPointDescriptionsFromIce(icepoints);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getAllPoints:" + e.getClass());
      disconnect();
      throw e;
    }    
    return res;
  }
  
  /** Add multiple points to the servers. This is a privileged
   * operation which requires the user to authenticate against the server.
   * The username and password are encrypted prior to transmission over the network.
   * @param newpoints Definitions for the new points.
   * @param username Username to authenticate against server.
   * @param passwd Password to authenticate against server.
   * @return True if points added, False if not added. */
  public boolean addPoints(Vector<PointDescription> newpoints, String username, String passwd)
  throws Exception
  {
    boolean res = false;
    try {
      if (!isConnected()) {
        connect();
      }
      PointDescriptionIce[] icepoints = MoniCAIceUtil.getPointDescriptionsAsIce(newpoints);
      // Encrypt the username/password
      RSA encryptor = getEncryptor();      
      String encname = encryptor.encrypt(username);
      String encpass = encryptor.encrypt(passwd);      
      res = itsIceClient.addPoints(icepoints, encname, encpass);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.addPoints:" + e.getClass());
      disconnect();
      throw e;
    } 
    return res;
  }
  
  /** Return the latest data for all of the named points.
   * @param pointnames Points to obtain data for.
   * @return Vector of latest values in same order as argument. */
  public Vector<PointData> getData(Vector<String> pointnames)
  throws Exception
  {
    Vector<PointData> res = null;
    try {
      if (!isConnected()) {
        connect();
      }
      String[] namesarray = new String[pointnames.size()];
      for (int i=0; i<pointnames.size(); i++) {
        namesarray[i] = pointnames.get(i);
      }
      PointDataIce[] icedata = itsIceClient.getData(namesarray);
      res = MoniCAIceUtil.getPointDataFromIce(icedata);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getData:" + e.getClass());
      disconnect();
      throw e;
    } 
    return res;
  }
  
  /** Return archived data for the given points.
   * @param pointnames Names of points to get data for.
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @param maxsamples Maximum number of records to be returned.
   * @return Data from the archive between the specified times, for each point. */
  public Vector<Vector<PointData>> getArchiveData(Vector<String> pointnames, AbsTime start, AbsTime end, int maxsamples)
  throws Exception
  {
    Vector<Vector<PointData>> res = null;
    try {
      if (!isConnected()) {
        connect();
      }
      String[] namesarray = new String[pointnames.size()];
      for (int i=0; i<pointnames.size(); i++) {
        namesarray[i] = pointnames.get(i);
      }
      PointDataIce[][] icedata = itsIceClient.getArchiveData(namesarray, start.getValue(), end.getValue(), maxsamples);
      if (icedata!=null && icedata.length>0) {
        //Convert data to native representation
        res = new Vector<Vector<PointData>>(pointnames.size());
        for (int i=0; i<pointnames.size(); i++) {
          res.add(MoniCAIceUtil.getPointDataFromIce(icedata[i]));
        }
      }
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getArchiveData:" + e.getClass());
      disconnect();
      throw e;
    } 
    return res;
  }
  
  /** Set new values for the specified points. This requires authentication. The 
   * username and password are encrypted prior to transmission over the network.
   * @param pointnames Names of the points to set values for.
   * @param values New values to be assigned to the points.
   * @param username Username to authenticate.
   * @param passwd Password to authenticate the user. 
   * @return The latest data available on the server.  */
  public boolean setData(Vector<String> pointnames, Vector<PointData> values, String username, String passwd)
  throws Exception  
  {
    boolean res = false;
    try {
      if (!isConnected()) {
        connect();
      }
      String[] namesarray = new String[pointnames.size()];
      for (int i=0; i<pointnames.size(); i++) {
        namesarray[i] = pointnames.get(i);
      }
      PointDataIce[] icevalues = MoniCAIceUtil.getPointDataAsIce(values);
      // Encrypt the username/password
      RSA encryptor = getEncryptor();      
      String encname = encryptor.encrypt(username);
      String encpass = encryptor.encrypt(passwd);
      res = itsIceClient.setData(namesarray, icevalues, encname, encpass);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.setData:" + e.getClass());
      disconnect();
      throw e;
    } 
    return res;
  }

  /** Return all SavedSetups for client Objects from the server.
   * <tt>null</tt> may be returned if the server has no SavedSetups. */
  public Vector<SavedSetup> getAllSetups()
  throws Exception
  {
    Vector<SavedSetup> res = null;
    try {
      if (!isConnected()) {
        connect();
      }
      //Get the setup strings from server and construct SavedSetup objects from them
      String[] rawsetups = itsIceClient.getAllSetups();
      if (rawsetups != null && rawsetups.length > 0) {
        res = new Vector<SavedSetup>(rawsetups.length);
        for (int i = 0; i < rawsetups.length; i++) {
          SavedSetup thissetup = SavedSetup.fromString(rawsetups[i]);
          if (thissetup != null) {
            res.add(thissetup);
          }
        }
      }
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getAllSetups:" + e.getClass());
      disconnect();
      throw e;
    }
    return res;
  }

  /** Add a new SavedSetup to the server. This requires authentication to
   * prevent inappropriate modification of the server data. The username
   * and password are encrypted prior to transmission over the network.
   * @param setup The SavedSetup to add to the server.
   * @param username Username to authenticate.
   * @param passwd Password to authenticate the user.
   * @return True if the setup was added, False if it couldn't be added. */
  public boolean addSetup(SavedSetup setup, String username, String passwd)
  throws Exception
  {
    boolean res = false;
    try {
      if (!isConnected()) {
        connect();
      }
      String stringsetup = setup.toString();
      // Encrypt the username/password
      RSA encryptor = getEncryptor();      
      String encname = encryptor.encrypt(username);
      String encpass = encryptor.encrypt(passwd);
      res = itsIceClient.addSetup(stringsetup, encname, encpass);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.addSetup:" + e.getClass());
      disconnect();
      throw e;
    }
    return res;
  }

  /** Return an RSA encryptor that uses the servers public key and modulus.
   * This will allow us to encrypt information that can only be encrypted by
   * the server. */
  public RSA getEncryptor()
  throws Exception
  {
    RSA res = null;
    try {
      if (!isConnected()) {
        connect();
      }
      //Get the public key and modulus
      String[] a = itsIceClient.getEncryptionInfo();
      BigInteger e = new BigInteger(a[0]);
      BigInteger n = new BigInteger(a[1]);
      res = new RSA(n, e);
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getEncryptor:" + e.getClass());
      disconnect();
      throw e;
    }
    return res;
  }

  /** Get the current time on the server. */
  public AbsTime getCurrentTime()
  throws Exception
  {
    try {
      if (!isConnected()) {
        connect();
      }

      long servertime = itsIceClient.getCurrentTime();
      return AbsTime.factory(servertime);
      
    } catch (Exception e) {
      System.err.println("MoniCAClientIce.getCurrentTime:" + e.getClass());
      disconnect();
      throw e;
    }
  }
  
  protected boolean
  connect()
  throws Exception
  {
    itsCommunicator = null;
    itsCommunicator = Ice.Util.initialize();
    Ice.ObjectPrx base = itsCommunicator.stringToProxy("MoniCAIce: tcp -h " + itsHost + " -p " + itsPort);
    itsIceClient = MoniCAIcePrxHelper.checkedCast(base);
    if (itsIceClient == null) {
      throw new Error("MoniCAClientIce.connect: Invalid proxy");
    }
    return true;
  }  
  
  /** Disconnect from the server. */
  protected void
  disconnect()
  {
    if (itsCommunicator != null) {
      try {
        itsCommunicator.destroy();
      } catch (Exception e) {
        System.err.println("MoniCAClientIce.disconnect: " + e.getClass());
      }
    }
    itsCommunicator = null;
    itsIceClient = null;
  }
  
  /** Get the default port for client server communication. */
  public static
  int
  getDefaultPort()
  {
    return MoniCAIceUtil.getDefaultPort();
  }
}
