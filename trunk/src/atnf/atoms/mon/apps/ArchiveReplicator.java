//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.apps;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.time.*;

/**
 * Connects to a MoniCA server and requests bulk data from it's archive,
 * which is then used to populate a new archive. If the new archive already
 * contains some data then only more recent data will be requested from the 
 * server.
 *
 * <P>This can be used to migrate a server from one kind of archiver to
 * another or to backup/mirror an archive.
 * 
 * @author David Brodrick
 * @version $Id: ArchiveReplicator.java,v 1.1 2009/01/09 03:36:06 bro764 Exp bro764 $
 */
public
class ArchiveReplicator
{
  /** The new archive which will be populated. */
  private static PointArchiver itsNewArchive = null;
  
  /** Connection to the server. */
  private static MonitorClientCustom itsServer = null;
  
  public static final void main(String[] args) {
    //CHECK USER ARGUMENTS
    if (args.length<2) {
      System.err.println("USAGE: ArchiveReplicator remote_server archive_type [point1] [pointN]");
      System.err.println("       Copies a remote monitor point archive to a local archive.");
      System.err.println("       If no points are specified then all points are copied.");
      System.err.println("       eg.. ArchiveReplicator myserver MySQL");
      System.err.println("       ..would copy the data from host 'myserver' to a local MySQL archive.");
      System.exit(1);
    }
    
    //CONNECT TO SERVER
    System.out.println("#Connecting to \"" + args[0] + "\"");
    try {
      itsServer = new MonitorClientCustom(args[0]);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
    
    //CREATE NEW ARCHIVER
    System.out.println("#Instanciating new PointArchiver" + args[1]);
    try {
      Class archiverClass = Class.forName("atnf.atoms.mon.archiver.PointArchiver"+args[1]);
      itsNewArchive = (PointArchiver)(archiverClass.newInstance());
      itsNewArchive.start();
    } catch (Exception e) {
      System.err.println("ERROR: Could not instanciate local 'PointArchiver" + args[1] + "'");
      System.exit(1);
    }

    //DETERMINE WHICH POINTS TO MIGRATE
    String[] serverpoints = itsServer.getPointNames();
    Vector pointnames = null;
    if (args.length>2) {
      //USER SPECIFIED SUBSET OF POINTS
      pointnames=new Vector(args.length-2);
      for (int i=2; i<args.length; i++) {
        //Ensure the user-specified points exist on the server
        boolean found=false;
        for (int j=0; j<serverpoints.length; j++) {
          if (serverpoints[j].equals(args[i])) {
            found=true;
            pointnames.add(args[i]);
            break;
          }
        }
        if (!found) {
          System.err.println("#ERROR: Point \"" + args[i] + "\" does not exist");
          System.exit(1);
        }
      }
    } else {
      //ALL POINTS AVAILABLE FROM SERVER
      pointnames=new Vector(serverpoints.length);
      for (int i=0; i<serverpoints.length; i++) {
        pointnames.add(serverpoints[i]);
      }
    }
    System.out.println("#Will replicate " + pointnames.size() + " points to new archive");
    
    //GET THE FULL DATA FOR EACH POINT FROM THE SERVER
    Vector defstrings = itsServer.getPointMonitors(pointnames);
    if (defstrings==null || defstrings.size()!=pointnames.size()) {
      System.err.println("#ERROR: Obtaining point definitions from server");
      System.exit(1);
    }
    
    //CREATE MONITOR POINT OBJECTS FOR EACH POINT
    Vector points = new Vector(defstrings.size());
    for (int i=0; i<defstrings.size(); i++) {
      PointDescription newpoint=PointDescription.parseLine((String)defstrings.get(i)).get(0);
      //PointDescription newpoint=(PointDescription)(PointInteraction.parsePoints((String)defstrings.get(i))[0]);
      if (newpoint==null) {
        System.err.println("#ERROR: Creating \"" + pointnames.get(i) + "\"");
        System.exit(1);
      }
      points.add(newpoint);
    }
    
    //PROCESS EACH POINT IN TURN
    long totalrecords=0;
    AbsTime downloadend=new AbsTime();
    for (int pointnum=0; pointnum<points.size(); pointnum++) {
      PointDescription thispoint=(PointDescription)points.get(pointnum);
      String thisname=(String)pointnames.get(pointnum);
      //IF POINT IS NOT TO BE ARCHIVED THEN DON't ARCHIVE IT
      String archivepols=thispoint.getArchivePolicyString();
      if (archivepols==null || archivepols.equals("-") || archivepols.equals("NONE")) {
        System.out.println("#Skipping non-archived point \"" + thisname + "\"");
        continue;
      }
      System.err.println("#Replicating \"" + thisname + "\"");
      //DETERMINE HOW FAR BACK TO COLLECT DATA FROM REMOTE SERVER
      AbsTime downloadstart=null;
      PointData ourlast=itsNewArchive.getPreceding(thispoint, downloadend);
      if (ourlast==null) {
        downloadstart=AbsTime.factory(0l);
      } else {
        downloadstart=ourlast.getTimestamp().add(RelTime.factory(1l));
      }
      long numcollected=0;
      while (true) {
        //COLLECT SOME MORE DATA FROM THE SERVER
        Vector newdata=itsServer.getPointData(thisname, downloadstart, downloadend);
        if (newdata==null || newdata.size()==0) {
          System.out.println("#Replicated " + numcollected + " data points");
          totalrecords+=numcollected;
          break;
        }
        //System.err.println("Got " + newdata.size() + " elements");
        numcollected+=newdata.size();
        
        //INSERT THIS DATA INTO LOCAL ARCHIVE
        itsNewArchive.archiveData(thispoint, newdata);          
        
        //WAIT UNTIL ARCHIVE HAS FINISHED FLUSHING
        while (itsNewArchive.checkBuffer()) {
          RelTime sleeptime=RelTime.factory(1000000l);
          try {
            sleeptime.sleep();
          } catch (Exception e) { }
          //System.out.println("#Waiting for local archive to finish flushing..");
        }
//        if (numcollected>700000 || totalrecords>1000000) {
//          System.exit(0);
//        }
        
        //UPDATE QUERY TIME DELIMITERS
        downloadstart=((PointData)newdata.get(newdata.size()-1)).getTimestamp();
        downloadstart=downloadstart.add(RelTime.factory(1l));
      }
    }
    
    //DONT EXIT UNTIL LOCAL ARCHIVE HAS FINISHED FLUSHING
    while (itsNewArchive.checkBuffer()) {
      RelTime sleeptime=RelTime.factory(1000000l);
      try {
        sleeptime.sleep();
      } catch (Exception e) { }
      System.out.println("#Waiting for local archive to finish flushing..");
    }
    
    System.out.println("#Replicated " + totalrecords + " records total. Cya!");
    System.exit(0);
  }
}
