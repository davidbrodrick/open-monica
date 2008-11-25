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
 * Discover and monitor UPSs from a NUT UPS monitor daemon.
 *
 * <BR>Constructor requires channel:hostname:port:timeout_ms arguments.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
class DataSourceNUTUPS
extends DataSourceASCIISocket
{
  /** Have we discovered what UPSs the server provides yet? */
  private boolean itsDiscovery = false;
  /** The place in the tree where we create our monitor points. */
  private String itsTreeBase = "ups";
  
  /** Constructor.
   * @param args
   */
  public DataSourceNUTUPS(String[] args)
  {
    super(args);
    
    try {
      connect();
      discoverUPS();
    } catch (Exception e) { 
      System.err.println("DataSourceNUTUPS:constructor: " + e.getClass());
    }
  }

  /** Discover what UPSs and monitor points the NUTS server offers. */
  private
  void
  discoverUPS()
  {
    try {
      if (!itsConnected) {
        System.err.println("DataSourceNUTUPS: Could not connect to discover UPSs");
        return;
      }
    
      Vector upsnames=new Vector();
      itsWriter.write("LIST UPS\n");
      String line=itsReader.readLine();
      if (!line.equals("BEGIN LIST UPS")) {
        System.err.println("DataSourceNUTUPS:discoverUPS: parse error listing UPSs");
        return;
      }
      line=itsReader.readLine();
      while (!line.equals("END LIST UPS")) {
        //Make sure this line is a new UPS description
        if (!line.startsWith("UPS ")) {
          System.err.println("DataSourceNUTUPS:discoverUPS: expected UPS: " + line);
          continue;
        }
        //Get the name of this UPS
        upsnames.add(line.split(" ")[1]);
        line=itsReader.readLine();
         
      }
      
      if (upsnames.size()==0) {
        System.err.println("DataSourceNUTUPS:discoverUPS: No UPSs defined on " + itsHostName);
        return;
      }

      //Request a list of variables supported by each UPS
      for (int i=0; i<upsnames.size(); i++) {
        Vector upsvars=new Vector();
        String thisname=(String)upsnames.get(i);
        itsWriter.write("LIST VAR " + thisname + "\n");
        line=itsReader.readLine();
        if (!line.equals("BEGIN LIST VAR " + thisname)) {
          System.err.println("DataSourceNUTUPS:discoverUPS: parse error listing VARs for " + thisname);
          continue;
        } 
        line=itsReader.readLine();
        while (!line.equals("END LIST VAR " + thisname)) {
          //Make sure this line is a new variable description
          if (!line.startsWith("VAR " + thisname + " ")) {
            System.err.println("DataSourceNUTUPS:discoverUPS: expected VAR: " + line);
            continue;
          }
          //Get the name of this VAR
          upsvars.add(line.split(" ")[2]);
          line=itsReader.readLine();
        }
        if (upsvars.size()==0) {
          System.err.println("DataSourceNUTUPS:discoverUPS: No Variables defined for " + thisname);
          continue;
        }
        
        //Since we have data, add ourselves as another DataSource
        if (MonitorMap.getDataSource("NUTUPS"+itsHostName+thisname)==null) {
          System.err.println("Adding DataSource: NUTUPS"+itsHostName+thisname);
          addDataSource("NUTUPS"+itsHostName+thisname, this);
          MonitorMap.addDataSource("NUTUPS"+itsHostName+thisname, this);
        }
        
        //We got the list of this UPSs variables, now get info about each one
        for (int j=0; j<upsvars.size(); j++) {
          String thisvar=(String)upsvars.get(j);
          itsWriter.write("GET DESC " + thisname + " " + thisvar + "\n");
          line=itsReader.readLine();
          if (!line.startsWith("DESC " + thisname + " " + thisvar)) {
            System.err.println("DataSourceNUTUPS:discoverUPS: expected DESC: " + line);
            continue;
          }
          String thisdesc=line.split("\"")[1];
          if (thisdesc.toLowerCase().equals("unavailable")) {
            //No meaning full description. Substitute the point name
            thisdesc=thisvar.replace(".", " ").replace(" ups ", " UPS ");
            thisdesc=thisvar.replace(".", " ").replace("ups ", "UPS ");
          }
          String[] pointnames={itsTreeBase+"."+thisvar.replace(".", "-")};
          String limit="-";
          //Use a special limit for UPS status
          if (pointnames[0].endsWith("ups-status"))
            limit="StringMatch-\"true\"\"mains\"";
          String[] archivepolicy={"CHANGE-"};
          //Don't auto-create the point if it already exists
          if (!MonitorMap.checkPointName(thisname+"."+pointnames[0])) {
            final String[] nullarray = {"-"};
            PointMonitor mp = PointMonitor.factory(pointnames,
               thisdesc,
               "", //Short description
               "", //Units
               thisname,
               "Strings-\"NUTUPS" + itsHostName + "\"\"" + thisname + "\"\"" + thisvar + "\"",
               nullarray,
               limit,
               archivepolicy,
               "5000000",
               true);
        	  if (mp!=null) MonitorMap.addPointMonitor(mp);
          }
        }
      }
    } catch (Exception e) {
      System.err.println("DataSourceNUTUPS:discoverUPS: " + e.getClass());
      e.printStackTrace();
    }  
  }

  /** Implement this method in your class. The <tt>requestor</tt> argument
   * can be used to determine which data to request, if your implementation
   * provides different kinds of data to different monitor points. */
  public synchronized
  Object
  parseData(PointMonitor requestor)
  throws Exception
  {
    TransactionStrings ts = (TransactionStrings)requestor.getTransaction();
    while (itsReader.ready()) {
      itsReader.readLine();
    }
    itsWriter.write("GET VAR " + ts.getString(0) + " " + ts.getString(1) + "\n");
    String line = itsReader.readLine();
    if (!line.startsWith("VAR " + ts.getString(0) + " ")) {
      System.err.println("DataSourceNUTUPS:discoverUPS: expected value");//, got: " + line);
      return null;
    }
    //System.out.println(line);
    Object val=null;
    try {
      val=line.split("\"")[1];
    } catch (Exception e) {
      val=null;
    }
    if (val==null) {
      return null;
    }
    //See if it is a numeric point
    try {
      val=new Float((String)val);
    } catch (Exception e) {}
    //If it is the UPS Status then remap it
    if (requestor.getName().endsWith("ups-status")) {
      if (((String)val).equals("OL"))
        val = "mains";
      else if (((String)val).equals("")) 
        val=null;
      else
        val = "DISCHARGING";
      
        
    }
    return val;
  }
  
  /** Main program for testing in isolation. */
  static final public
  void
  main(String[] argv)
  {
    if (argv.length<1) {
      System.err.println("USAGE: Needs hostname:port argument!");
      System.exit(1);
    }
    String[] args=argv[0].split(":");
    String[] fullargs=new String[3];
    fullargs[0]="nutups";
    fullargs[1]=args[0];
    fullargs[2]=args[1];
    DataSourceNUTUPS ups = new DataSourceNUTUPS(fullargs);
  }
}
