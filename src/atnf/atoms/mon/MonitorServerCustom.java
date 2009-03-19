//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.util.*;
import atnf.atoms.time.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.mon.archiver.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

/**
 * Provides network access to the server using a custom encoding scheme based
 * on Java Object Serialisation. The constructor with no arguments creates a
 * main thread which listens for client connections. When a new client
 * connects we launch a new instance with the socket as a constructor
 * argument. The new instance will handle all requests from the client.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: MonitorServerCustom.java,v 1.15 2007/04/12 05:18:07 bro764 Exp bro764 $
 */
public class
MonitorServerCustom
implements Runnable
{
  /** Keep track of how many clients are connected. */
  protected static int theirNumClients = 0;

  /** Indicates if the thread should keep running (true) or stop (false). */
  protected boolean itsRunning = true;
  /** The socket used to listen for requests from our client. */
  protected Socket itsSocket = null;
  /** Input stream used to read data from the client. */
  protected ObjectInputStream itsInput = null;
  /** Output stream used for writing data to the client. */
  protected ObjectOutputStream itsOutput = null;
  /** Logger used to report messages to the site message logging system. */
  protected Logger itsLogger = new Logger("Monitor Server");

  /** Starts up the main server thread which waits for client connections. */
  public MonitorServerCustom()
  {
    Thread t = new Thread(this, "MonitorServer Main");
    t.setDaemon(true);
    t.start();
  }

  /** Starts up a server thread to handle requests from a new client.
   * @param socket The socket connection tot he new client. */
  public MonitorServerCustom(Socket socket)
  {
    theirNumClients++;
    itsLogger.debug("New Java Connection from: "
                    + socket.getInetAddress().getHostAddress());
    System.out.println("MonitorServerCustom: New Connection from "
                       + socket.getInetAddress().getHostAddress());

    itsSocket = socket;
    Thread t = new Thread(this, "MonitorServer to: "+socket.getInetAddress().getHostAddress());
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


  /** Return current or archival data about a specific monitor point. If
   * the argument only specifies the name of a monitor point the latest
   * value for that point will be returned. If the argument contains the
   * name and two timestamps then we will access the archive and return all
   * data for the point within that time range. If the argument additionally
   * specifies a sampling interval we return undersampled archival data. */
  private
  PointData
  getData(MonRequest req)
  {
    //Sanity check
    if (req==null || req.Args==null || req.Args.length<1) return null;

    if (req.Args.length==1) {
      if (req.Args[0] instanceof String) {
        //Latest data for a single point
        return PointBuffer.getPointData((String)req.Args[0]);
      } else if (req.Args[0] instanceof Vector) {
        //Latest data for a batch of points
        Vector arg = (Vector)req.Args[0];
        if (arg==null || arg.size()==0) return null;
        Vector res = new Vector(arg.size());
        //Get the latest value for each
        for (int i=0; i<arg.size(); i++) {
          try {
            String point = (String)arg.get(i);
            PointData val = PointBuffer.getPointData(point);
            res.add(val);
          } catch (Exception e) {
            e.printStackTrace();
            res.add(null);
          }
        } 
        return new PointData(res);
      } else {
        System.err.println("MonitorServerCustom: Unexpected Argument...");
        return null;
      }
    }

    if (req.Args.length<=4 && req.Args[1] instanceof AbsTime) {
      //Timestamps included: we need to return archival data
      AbsTime end_time = null;
      AbsTime start_time = (AbsTime)req.Args[1];

      if (req.Args.length==2) {
        //Client wants all data SINCE the given time
        end_time = AbsTime.factory();
      } else if (req.Args[2] instanceof RelTime) {
        //3rd arg is a RelTime
        if (!start_time.isASAP()) {
          end_time = start_time.add((RelTime)req.Args[2]);
        } else {
          end_time = AbsTime.factory();
        }
      } else {
        //3rd arg is another AbsTime
        end_time = (AbsTime)req.Args[2];
      }

      Vector res = null;
      if (req.Args.length==4 && req.Args[3] instanceof Integer) {
        //A resampling interval has been specified
        res = PointBuffer.getPointData((String)req.Args[0], start_time,
                          end_time, ((Integer)req.Args[3]).intValue());
      } else {
        res = PointBuffer.getPointData((String)req.Args[0], start_time,
                                       end_time, 0);
      }
      //We got the requested data, now return it
      return new PointData(MonitorUtils.compress(res));
    }

    //No valid request found, so no valid data can be returned!
    return null;
  }


  public
  PointData
  getPointNames(MonRequest req)
  {
    if (req == null) return null;
    // All names, no filtering
    if (req.Args == null || req.Args.length == 0) return new PointData(MonitorMap.getPointNames());
    // Get by source
    return new PointData(MonitorMap.getPointNames((String)req.Args[0]));
  }


  public
  PointData
  getPointNamesShort(MonRequest req)
  {
    if (req == null) return null;
    // All names, no filtering
    if (req.Args == null || req.Args.length == 0) return new PointData(MonitorMap.getPointNamesShort());
    // Get by source
    return new PointData(MonitorMap.getPointNamesShort((String)req.Args[0]));
  }


  /** Return source names for all, a single, or multiple, point names. */
  public
  PointData
  getSources(MonRequest req)
  {
    if (req == null) return null;
    
    if (req.Args == null || req.Args.length == 0) {
      //All sources
      return new PointData(MonitorMap.getSources());
    } else if (req.Args[0] instanceof String) {
      //Sources for a single point
      return new PointData(MonitorMap.getSources((String)req.Args[0]));
    } else if (req.Args[0] instanceof Vector) {
      //Sources for multiple points
      Vector points = (Vector)req.Args[0];
      if (points==null || points.size()==0) return null;
      Vector res = new Vector(points.size());
      for (int i=0; i<points.size(); i++) {
        if (points.get(i)!=null && points.get(i) instanceof String) {
          String[] sources = MonitorMap.getSources((String)points.get(i));
          if (sources==null || sources.length==0) res.add(null);
          else res.add(sources);
        } else res.add(null);
      }
      return new PointData(res);
    } else {
      //Argument was not understood
      return null;
    }
  }


  public
  PointData
  addPoint(MonRequest req)
  {
    //Preconditions
    if (req==null || req.Args==null || req.Args.length<3) return null;

    ArrayList points = PointInteraction.parseLine((String)req.Args[0]);

    //Decrypt and check the users credentials
    String username = MonitorMap.decrypt((String)req.Args[1]);
    String password = MonitorMap.decrypt((String)req.Args[2]);
    if (!atnf.atoms.mon.util.Authenticator.check(username, password))
      return null;

    //Log a message
    MonitorMap.logger.information("" + username + "@" +
				  itsSocket.getInetAddress().getHostAddress()
				  + " added " + points.size()
				  + " new points");

    System.err.println("MonitorServerCustom:addPoint: UNIMPLEMENTED!!!");

    return new PointData(new Boolean(true));
  }


  public
  PointData
  getPoint(MonRequest req)
  {
    if (req == null || req.Args == null || req.Args.length < 1) return null;

    if (req.Args[0] instanceof Vector) {
      //Batch of points were requested
      Vector arg = (Vector)req.Args[0];
      if (arg==null || arg.size()==0) return null;

      Vector res = new Vector(arg.size());
      for (int i=0; i< arg.size(); i++) {
        PointMonitor pm = MonitorMap.getPointMonitor((String)arg.get(i));
        if (pm==null) {
          //Wasn't found
          res.add(null);
        } else {
          res.add(pm.getStringEquiv());
        }
      }
      return new PointData(res);
    } else if (req.Args[0] instanceof String) {
      //Single point was requested
      PointMonitor pm = MonitorMap.getPointMonitor((String)req.Args[0]);
      if (pm==null) {
        //Wasn't found
        System.err.println("MonitorServerCustom:getPoint: NO POINT \""
			                     + (String)req.Args[0] + "\"");
        return null;
      }
      return new PointData(pm.getStringEquiv());
    } else {
      System.err.println("MonitorServerCustom:getPoint: Unexpected Argument...");
      return null;
    }
  }


  public
  PointData
  setPoint(MonRequest req)
  {
    //preconditions
    if (req==null || req.Args==null || req.Args.length<4) return null;

    //Decrypt and check the users credentials
    String username = MonitorMap.decrypt((String)req.Args[2]);
    String password = MonitorMap.decrypt((String)req.Args[3]);
    if (!atnf.atoms.mon.util.Authenticator.check(username, password))
      return null;

    System.err.println("MonitorClientCustom:setPoint: UNIMPLEMENTED!");
    return new PointData(new Boolean(true));
  }


  public
  PointData
  getAllPoints()
  {
    return new PointData(MonitorUtils.compress(MonitorMap.getAllPoints()));
  }


  /** Return the <i>Transaction</i> definition string used by the specified
   * point(s). The request can either consist of the string name of one point
   * (including source), or it may be a Vector of string names if you wish
   * to obtain the Transaction strings for many points at once. */
  public
  PointData
  getTransaction(MonRequest req)
  {
    if (req == null || req.Args == null || req.Args.length != 1) return null;

    if (req.Args[0] instanceof Vector) {
      //Batch of points were requested
      Vector arg = (Vector)req.Args[0];
      if (arg==null || arg.size()==0) return null;

      Vector res = new Vector(arg.size());
      for (int i=0; i< arg.size(); i++) {
        PointInteraction pm = MonitorMap.getPointInteraction((String)arg.get(i));
        if (pm==null) {
          //Wasn't found
          res.add(null);
        } else {
          res.add(pm.getTransactionString());
        }
      }
      return new PointData(res);
    } else if (req.Args[0] instanceof String) {
      //Single point was requested
      PointInteraction pm = MonitorMap.getPointInteraction((String)req.Args[0]);
      if (pm==null) {
        //Wasn't found
        return null;
      }
      System.err.println("Got request for Transaction:");
      System.err.println("pm=" + pm);
      System.err.println("trans=" + pm.getTransactionString());
      return new PointData(pm.getTransactionString());
    } else {
      System.err.println("MonitorServerCustom:getTransaction: Unexpected Argument...");
      return null;
    }
  }


  /** Return the <i>Translation</i> definition strings used by the specified
   * point(s). The request can either consist of the string name of one point
   * (including source), or it may be a Vector of string names if you wish
   * to obtain the Translation strings for many points at once. */
  public
  PointData
  getTranslations(MonRequest req)
  {
    if (req == null || req.Args == null || req.Args.length != 1) return null;

    if (req.Args[0] instanceof Vector) {
      //Batch of points were requested
      Vector arg = (Vector)req.Args[0];
      if (arg==null || arg.size()==0) return null;

      Vector res = new Vector(arg.size());
      for (int i=0; i< arg.size(); i++) {
        PointInteraction pm = MonitorMap.getPointInteraction((String)arg.get(i));
        if (pm==null) {
          //Wasn't found
          res.add(null);
        } else {
          res.add(pm.getTranslationString());
        }
      }
      return new PointData(res);
    } else if (req.Args[0] instanceof String) {
      //Single point was requested
      PointInteraction pm = MonitorMap.getPointInteraction((String)req.Args[0]);
      if (pm==null) {
        return null;
      }
      return new PointData(pm.getTranslationString());
    } else {
      System.err.println("MonitorServerCustom:getTranslations: Unexpected Argument...");
      return null;
    }
  }


  /** Return all SavedSetups known by the system. The SavedSetups are
   * used on the client side to simplify the process of navigating and
   * viewing the available information.
   * @see SavedSetup */
  public
  PointData
  getAllSetups()
  {
    return new PointData(MonitorUtils.compress(MonitorMap.getAllSetups()));
  }


  /** Add a new SavedSetup to the system. */
  public
  PointData
  addSetup(MonRequest req)
  {
    //preconditions
    if (req==null || req.Args==null || req.Args.length<3) return null;

    //Decrypt and check the users credentials
    String username = MonitorMap.decrypt((String)req.Args[0]);
    System.err.println("DECRYPTED USERNAME = " + username);
    String password = MonitorMap.decrypt((String)req.Args[1]);
    if (!atnf.atoms.mon.util.Authenticator.check(username, password))
      return null;

    //Retrieve the SavedSetup and add it to the system
    SavedSetup setup = (SavedSetup)req.Args[2];
    MonitorMap.addSetup(setup);
    saveSetupToFile(setup, username);

    return new PointData(new Boolean(true));
  }


  /** Save the given setup to the setup file. */
  protected static synchronized
  void
  saveSetupToFile(SavedSetup setup, String user)
  {
    try {
      URL setupfile = MonitorServerCustom.class.getClassLoader().getResource("monitor-setups.txt");
      if (setupfile==null || setupfile.equals("")) {
        MonitorMap.logger.error("I don't know which file to save SavedSetups to");
        System.err.println("ERROR: Couldn't determine file to save setup to");
      } else {
        //Open file in APPEND mode and write the setup out
        PrintStream file = new PrintStream(new FileOutputStream(setupfile.getFile(), true));
        AbsTime now = AbsTime.factory();
        file.println("#Saved by " + user + " at " + now.toString(AbsTime.Format.UTC_STRING));
        file.println(setup);
        file.close();
        System.out.println("SavedSetup \"" + setup.getName() + "\" uploaded by " + user);
      }
    } catch (Exception e) {
      System.err.println("Exception at MonitorServerCustom:saveSetupToFile");
      e.printStackTrace();
    }
  }

  /** Return the server's public encryption key and modulus. */
  public
  PointData
  getPublicKey()
  {
    String[] a = new String[2];
    a[0] = MonitorMap.getPublicKey();
    a[1] = MonitorMap.getModulus();
    return new PointData(a);
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
      System.err.println("MonitorServerCustom::stopRunning(): " +
			 e.getMessage());
    }
  }


  /** Main loop to parse and service client requests. */
  private
  void
  processConnection()
  {
    MonRequest req = null;
    try {

/*       // Failed GZIP technique, only use if compressing large chunks
         GZIPOutputStream gout = new GZIPOutputStream(socket.getOutputStream());
	 output = new ObjectOutputStream(gout);
	 output.flush();
	 gout.finish();
         GZIPInputStream gin = new GZIPInputStream(socket.getInputStream());
	 input = new ObjectInputStream(gin);
*/
/*       // Compression - good for large data, bad for small - CPU intensive
         CompressedOutputStream cos = new CompressedOutputStream(socket.getOutputStream());      
	 output = new ObjectOutputStream(cos);
	 output.flush();
         CompressedInputStream cis = new CompressedInputStream(socket.getInputStream());
	 input = new ObjectInputStream(cis);
*/
      // No compression but still fairly efficient
      itsOutput = new ObjectOutputStream(itsSocket.getOutputStream());
      itsOutput.flush();
      itsInput = new ObjectInputStream(itsSocket.getInputStream());
    }
    catch (IOException ie) {
      ie.printStackTrace();
      return;
    }
    while (true) {
      try {
	req = (MonRequest)itsInput.readObject();
      } catch (Exception e) {break;}

      PointData res = null;
      switch (req.Command) {
      case MonRequest.GETDATA:       res = getData(req);
      break;
      case MonRequest.GETPOINT:      res = getPoint(req);
      break;
      case MonRequest.SETPOINT:      res = setPoint(req);
      break;
      case MonRequest.GETPOINTNAMES: res = getPointNames(req);
      break;
      case MonRequest.GETSOURCES:    res = getSources(req);
      break;
      case MonRequest.ADDPOINT:      res = addPoint(req);
      break;
      case MonRequest.GETKEY:        res = getPublicKey();
      break;
      case MonRequest.GETPOINTNAMES_SHORT: res = getPointNamesShort(req);
      break;
      case MonRequest.GETALLPOINTS:  res = getAllPoints();
      break;
      case MonRequest.GETALLSETUPS:  res = getAllSetups();
      break;
      case MonRequest.ADDSETUP:      res = addSetup(req);
      break;
      case MonRequest.GETTRANSACTION: res = getTransaction(req);
      break;
      case MonRequest.GETTRANSLATION: res = getTranslations(req);
      break;
      default: System.err.println("MonitorServerCustom: Invalid request");
      }
      try {
	if (res != null && res instanceof PointData) itsOutput.writeObject(res);
	else itsOutput.writeObject(new PointData(null));
	itsOutput.flush();
	itsOutput.reset(); //Stops memory leak
      } catch (java.net.SocketException e) {
	System.err.println(e.getMessage() + ": Closing connection");
	try {
	  itsOutput.close();
	} catch (Exception f) {}
        return;
      } catch (Exception e) {e.printStackTrace();}
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
	int port = Integer.parseInt(MonitorConfig.getProperty("MonPort"));
	//Create the server socket to listen with
	ServerSocket ss = new ServerSocket(port);

	//Keep looping until we need to stop
	while (itsRunning) {
	  try {
	    //Await a new client connection
	    Socket soc = ss.accept();
	    //Got a new client connection, spawn a server to service it
	    if (soc!=null) new MonitorServerCustom(soc);
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
      System.err.println("MonitorServerCustom::run(): Can't open port");
      System.err.println(ie.getMessage());
    }
  }
}
