//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.util.Vector;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.RSA;
import atnf.atoms.time.AbsTime;

/** Abstract class representing the client-side interface to a MoniCA server.
 * 
 * <P>All communications methods should attempt to reestablish connectivity to the server
 * if the client is not currently connected.
 * 
 * @author David Brodrick */
public abstract class MoniCAClient {
  /** Return the current connection status.
   * @return Connection status, True if connected, False if disconnected. */
  public abstract boolean isConnected();

  /** Get the names of all points (including aliases) on the system. 
   * @return Names of all points on the system. */
  public abstract String[] getAllPointNames() throws Exception;

  /** Get the point with the specified name.
   * @param pointname Name of the point to be retrieved.
   * @return Point definition. */
  public PointDescription getPoint(String pointname)
  throws Exception
  {
    Vector<String> pointnames = new Vector<String>(1);
    pointnames.add(pointname);
    Vector<PointDescription> points = getPoints(pointnames);
    PointDescription res = null;
    if (points!=null && points.size()>0) {
      res = points.get(0);
    }
    return res;
  }

  /** Get the points with the specified names. The populateClientFields method should
   * be invoked on each point prior to returning the result.
   * @param pointnames Vector containing point names to be retrieved.
   * @return Vector containing all point definitions. */
  public abstract Vector<PointDescription> getPoints(Vector<String> pointnames) throws Exception;
  
  /** Get all of the points on the system.
   * @return Vector containing all point definitions. */
  public abstract Vector<PointDescription> getAllPoints() throws Exception;
  
  /** Add a new point to the server. This is a privileged
   * operation which requires the user to authenticate against the server.
   * The username and password are encrypted prior to transmission over the network.
   * @param newpoint Definition for the new point.
   * @param username Username to authenticate against server.
   * @param password Password to authenticate against server.
   * @return True if point added, False if not added. */
  public boolean addPoint(PointDescription newpoint, String username, String passwd)
  throws Exception
  {
    Vector<PointDescription> newpoints = new Vector<PointDescription>(1);
    newpoints.add(newpoint);
    return addPoints(newpoints, username, passwd);
  }

  /** Add multiple points to the servers. This is a privileged
   * operation which requires the user to authenticate against the server.
   * The username and password are encrypted prior to transmission over the network.
   * @param newpoints Definitions for the new points.
   * @param username Username to authenticate against server.
   * @param passwd Password to authenticate against server.
   * @return True if points added, False if not added. */
  public abstract boolean addPoints(Vector<PointDescription> newpoints, String username, String passwd) throws Exception;
  
  /** Return the latest data for the specified point.
   * @param pointname Name of the point to obtain data for.
   * @return The latest data available on the server.  */
  public PointData getData(String pointname)
  throws Exception
  {
    Vector<String> pointnames = new Vector<String>(1);
    pointnames.add(pointname);
    Vector<PointData> data = getData(pointnames);
    PointData res=null;
    if (data!=null&&data.size()>0) {
      res=data.get(0);
    }
    return res;
  }

  /** Return the latest data for all of the named points.
   * @param pointnames Points to obtain data for.
   * @return Vector of latest values in same order as argument. */
  public abstract Vector<PointData> getData(Vector<String> pointnames) throws Exception;

  /** Return archived data for the given point.
   * @param pointname Point to get data for.
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @return Data from the archive between the specified times. */
  public Vector<PointData> getArchiveData(String pointname, AbsTime start, AbsTime end)
  throws Exception
  {
    return getArchiveData(pointname, start, end, 0);
  }

  /** Return archived data for the given point.
   * @param pointname Point to get data for.
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @param maxsamples Maximum number of records to be returned. 
   * @return Data from the archive between the specified times. */
  public Vector<PointData> getArchiveData(String pointname, AbsTime start, AbsTime end, int maxsamples)
  throws Exception
  {
    Vector<String> pointnames = new Vector<String>(1);
    pointnames.add(pointname);
    Vector<Vector<PointData>> data = getArchiveData(pointnames, start, end, maxsamples);
    Vector<PointData> res = null;
    if (data!=null && data.size()>0) {
      res = data.get(0);
    }
    return res;    
  }

  /** Return archived data for the given points.
   * @param pointnames Names of points to get data for.
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @return Data from the archive between the specified times, for each point. */
  public Vector<Vector<PointData>> getArchiveData(Vector<String> pointnames, AbsTime start, AbsTime end)
  throws Exception
  {
    return getArchiveData(pointnames, start, end, 0);
  }

  /** Return archived data for the given points.
   * @param pointnames Names of points to get data for.
   * @param start The oldest data to be retrieved.
   * @param end The most recent data to be retrieved.
   * @param maxsamples Maximum number of records to be returned.
   * @return Data from the archive between the specified times, for each point. */
  public abstract Vector<Vector<PointData>> getArchiveData(Vector<String> pointnames, AbsTime start, AbsTime end, int maxsamples) throws Exception;

  /** Set a new value for the specified point. This requires authentication. The 
   * username and password are encrypted prior to transmission over the network.
   * @param pointname Name of the point to set value for.
   * @param value The value to be assigned.
   * @param username Username to authenticate.
   * @param passwd Password to authenticate the user.
   * @return The latest data available on the server.  */
  public boolean setData(String pointname, PointData value, String username, String passwd)
  throws Exception
  {
    Vector<String> pointnames = new Vector<String>(1);
    pointnames.add(pointname);
    Vector<PointData> values = new Vector<PointData>(1);
    values.add(value);
    return setData(pointnames, values, username, passwd);
  }

  /** Set new values for the specified points. This requires authentication. The 
   * username and password are encrypted prior to transmission over the network.
   * @param pointnames Names of the points to set values for.
   * @param values New values to be assigned to the points.
   * @param username Username to authenticate.
   * @param passwd Password to authenticate the user. 
   * @return The latest data available on the server.  */
  public abstract boolean setData(Vector<String> pointnames, Vector<PointData> values, String username, String passwd) throws Exception;

  /** Return all SavedSetups for client Objects from the server.
   * <tt>null</tt> may be returned if the server has no SavedSetups. */
  public abstract Vector<SavedSetup> getAllSetups() throws Exception;

  /** Add a new SavedSetup to the server. This requires authentication to
   * prevent inappropriate modification of the server data. The username
   * and password are encrypted prior to transmission over the network.
   * @param setup The SavedSetup to add to the server.
   * @param username Username to authenticate.
   * @param passwd Password to authenticate the user.
   * @return True if the setup was added, False if it couldn't be added. */
  public abstract boolean addSetup(SavedSetup setup, String username, String passwd) throws Exception;

  /** Return an RSA encryptor that uses the servers public key and modulus.
   * this will allow us to encrypt information that can only be encrypted by
   * the monitor server. */
  public abstract RSA getEncryptor() throws Exception;

  /** Get the current time on the server. */
  public abstract AbsTime getCurrentTime() throws Exception;  
}