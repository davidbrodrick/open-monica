//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.Vector;

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
    
      Vector<String> upsnames=new Vector<String>();
      itsWriter.write("LIST UPS\n");
      itsWriter.flush();
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
        Vector<String> upsvars=new Vector<String>();
        String thisname=(String)upsnames.get(i);
        itsWriter.write("LIST VAR " + thisname + "\n");
        itsWriter.flush();
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
            
        //We got the list of this UPSs variables, now get info about each one
        for (int j=0; j<upsvars.size(); j++) {
          String thisvar=(String)upsvars.get(j);
          itsWriter.write("GET DESC " + thisname + " " + thisvar + "\n");
          itsWriter.flush();
          line=itsReader.readLine();
          if (!line.startsWith("DESC " + thisname + " " + thisvar)) {
            System.err.println("NUTUPS:discoverUPS: expected DESC: " + line);
            continue;
          }
          String thisdesc=line.split("\"")[1];
          if (thisdesc.toLowerCase().contains("unavailable")) {
            //No meaning full description. Substitute the point name
            //thisdesc=thisvar.replace(".", " ").replace(" ups ", " UPS ");
            thisdesc=thisvar.replace(".", " ").replace("ups ", "UPS ");
          }
          //Use '-' rather than '.' in point names because NUT has some points
          //on non-leaf nodes and those points get lost by MoniCA
          String[] pointnames={itsTreeBase+"."+thisvar.replace('.', '-')};
          String[] limits={"-"};
          String[] archivepolicy={"Change-"};
          //Don't auto-create the point if it already exists
          if (!PointDescription.checkPointName(thisname+"."+pointnames[0])) {
            final String[] nullarray = {"-"};
            String[] transaction = {"Strings-\"" + itsHostName + ":" + itsPort + "\"\"" + thisname + "\"\"" + thisvar + "\""};
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
               "2000000",
               "-1",
               true);
        	  if (mp!=null) {
              PointDescription.addPoint(mp);
              mp.populateServerFields();
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
    itsWriter.flush();
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
    new NUTUPS(fullargs);
  }
}
