//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.Vector;

import atnf.atoms.mon.MonitorMap;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.transaction.TransactionStrings;

/**
 * Discover and monitor UPSs from a NUT UPS monitor daemon.
 *
 * <BR>Constructor requires <i>hostname:port</i> and optionally <i>:timeout_ms</i>
 * arguments.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
class NUTUPS
extends ASCIISocket
{
  /** The place in the tree where we create our monitor points. */
  private String itsTreeBase = "ups";
  
  /** Constructor.
   * @param args
   */
  public NUTUPS(String[] args)
  {
    super(args);
    
    try {
      connect();
      discoverUPS();
    } catch (Exception e) { 
      System.err.println("NUTUPS:constructor: " + e.getClass());
    }
  }

  /** Discover what UPSs and monitor points the NUTS server offers. */
  private
  void
  discoverUPS()
  {
    try {
      if (!itsConnected) {
        System.err.println("NUTUPS: Could not connect to discover UPSs");
        return;
      }
    
      Vector upsnames=new Vector();
      itsWriter.write("LIST UPS\n");
      String line=itsReader.readLine();
      if (!line.equals("BEGIN LIST UPS")) {
        System.err.println("NUTUPS:discoverUPS: parse error listing UPSs");
        return;
      }
      line=itsReader.readLine();
      while (!line.equals("END LIST UPS")) {
        //Make sure this line is a new UPS description
        if (!line.startsWith("UPS ")) {
          System.err.println("NUTUPS:discoverUPS: expected UPS: " + line);
          continue;
        }
        //Get the name of this UPS
        upsnames.add(line.split(" ")[1]);
        line=itsReader.readLine();
         
      }
      
      if (upsnames.size()==0) {
        System.err.println("NUTUPS:discoverUPS: No UPSs defined on " + itsHostName);
        return;
      }

      //Request a list of variables supported by each UPS
      for (int i=0; i<upsnames.size(); i++) {
        Vector upsvars=new Vector();
        String thisname=(String)upsnames.get(i);
        itsWriter.write("LIST VAR " + thisname + "\n");
        line=itsReader.readLine();
        if (!line.equals("BEGIN LIST VAR " + thisname)) {
          System.err.println("NUTUPS:discoverUPS: parse error listing VARs for " + thisname);
          continue;
        } 
        line=itsReader.readLine();
        while (!line.equals("END LIST VAR " + thisname)) {
          //Make sure this line is a new variable description
          if (!line.startsWith("VAR " + thisname + " ")) {
            System.err.println("NUTUPS:discoverUPS: expected VAR: " + line);
            continue;
          }
          //Get the name of this VAR
          upsvars.add(line.split(" ")[2]);
          line=itsReader.readLine();
        }
        if (upsvars.size()==0) {
          System.err.println("NUTUPS:discoverUPS: No Variables defined for " + thisname);
          continue;
        }
        
        //Since we have data, add ourselves as another ExternalSystem
        if (getExternalSystem("NUTUPS"+itsHostName+thisname)==null) {
          System.err.println("Adding ExternalSystem: NUTUPS"+itsHostName+thisname);
          addExternalSystem("NUTUPS"+itsHostName+thisname, this);
        }
        
        //We got the list of this UPSs variables, now get info about each one
        for (int j=0; j<upsvars.size(); j++) {
          String thisvar=(String)upsvars.get(j);
          itsWriter.write("GET DESC " + thisname + " " + thisvar + "\n");
          line=itsReader.readLine();
          if (!line.startsWith("DESC " + thisname + " " + thisvar)) {
            System.err.println("NUTUPS:discoverUPS: expected DESC: " + line);
            continue;
          }
          String thisdesc=line.split("\"")[1];
          if (thisdesc.toLowerCase().equals("unavailable")) {
            //No meaning full description. Substitute the point name
            thisdesc=thisvar.replace(".", " ").replace(" ups ", " UPS ");
            thisdesc=thisvar.replace(".", " ").replace("ups ", "UPS ");
          }
          String[] pointnames={itsTreeBase+"."+thisvar.replace(".", "-")};
          String[] limits={"-"};
          //Use a special limit for UPS status
          if (pointnames[0].endsWith("ups-status")) {
            limits[0]="StringMatch-\"true\"\"mains\"";
          }
          String[] archivepolicy={"CHANGE-"};
          //Don't auto-create the point if it already exists
          if (!MonitorMap.checkPointName(thisname+"."+pointnames[0])) {
            final String[] nullarray = {"-"};
            String[] transaction = {"Strings-\"NUTUPS" + itsHostName + "\"\"" + thisname + "\"\"" + thisvar + "\""};
            PointDescription mp = PointDescription.factory(pointnames,
               thisdesc,
               thisdesc,
               "", //Units
               thisname,
               transaction,
               nullarray,
               nullarray,
               limits,
               archivepolicy,
               "5000000",
               true);
        	  if (mp!=null) {
              MonitorMap.addPointMonitor(mp);
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("NUTUPS:discoverUPS: " + e.getClass());
      e.printStackTrace();
    }  
  }

  /** Implement this method in your class. The <tt>requestor</tt> argument
   * can be used to determine which data to request, if your implementation
   * provides different kinds of data to different monitor points. */
  public synchronized
  Object
  parseData(PointDescription requestor)
  throws Exception
  {
    TransactionStrings ts = (TransactionStrings)getMyTransactions(requestor.getInputTransactions()).get(0);
    while (itsReader.ready()) {
      itsReader.readLine();
    }
    itsWriter.write("GET VAR " + ts.getString(0) + " " + ts.getString(1) + "\n");
    String line = itsReader.readLine();
    if (!line.startsWith("VAR " + ts.getString(0) + " ")) {
      System.err.println("NUTUPS:discoverUPS: expected value");//, got: " + line);
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
      if (((String)val).equals("OL")) {
        val = "mains";
      } else if (((String)val).equals("")) {
        val=null;
      } else {
        val = "DISCHARGING";
      }
      
        
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
    NUTUPS ups = new NUTUPS(fullargs);
  }
}
