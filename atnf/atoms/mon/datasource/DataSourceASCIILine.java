//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.datasource;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;

/**
 * Reads lines from an ASCII socket and fires updates to a specified
 * monitor point when a new line is read.
 *
 * Subclasses can implement the <i>preformat</i>method to perform any
 * filtering/manipulation of the line before it is used. If preformat
 * returns null than the line will be ignored and a new line read.<BR>
 *
 * A TranslationString2Array can then be used to tokenise the elements
 * of the line, and other monitor points can listen to a particular field.
 *
 * <P>The constructor requires <i>hostname:port:timeout_ms:point_name</i> 
 * arguments.
 * For instance your monitor-sources.txt file might contain:<BR>
 * <tt>ASCIILine localhost:10000:60000:site.monitor.point</tt>
 *
 * @author David Brodrick
 **/
public class DataSourceASCIILine
extends DataSourceASCIISocket
{
  /** Name of the monitor point to fire updates for. */
  protected String itsMonitorPoint;
  
  
  /** Constructor, expects host:port:timeout_ms:point_name arguments. */  
  public DataSourceASCIILine(String[] args)
  {
    super(args);

    itsMonitorPoint=args[4];

    //Start the thread that reads the data as it comes in
    DataReader worker = new DataReader();
    worker.start();
  }

  /** Data is pushed, so this method is redundant in this class. */
  public
  Object
  parseData(PointMonitor requestor)
  throws Exception
  {
    return null;
  }

  
  /** Peform any filtering/manipulation of the line here. Return null if you
   * want the line to be ignored. */
  protected
  String
  preformat(String line)
  {
    return line;
  }


  ////////////////////////////////////////////////////////////////////////
  /** Nested class to collect data. */
  public class
  DataReader
  extends Thread
  {
    public void	run()
    {
      while (true) {
        //If we're not connected then try to reconnect
        if (!itsConnected) {
          try {
            if (!connect()) {
              //Sleep for a while then try again
              try { RelTime.factory(5000000).sleep(); } catch (Exception f) {}
              continue;
            }
          } catch (Exception e) {
            //Sleep for a while then try again
            try { RelTime.factory(5000000).sleep(); } catch (Exception f) {}
            continue;
          }
        }
        
        try {
          while (true) {
            //Read a line from the server
            String line = itsReader.readLine();
            //System.err.println(line);
            
            //Do any required pre-formatting
            line=preformat(line);
            if (line==null) continue;
            
            //Present this new line to the monitor point
            PointData pd=new PointData(itsMonitorPoint, new AbsTime(), line);
            PointMonitor pm=MonitorMap.getPointMonitor(itsMonitorPoint);
            if (pm!=null) {
              pm.firePointEvent(new PointEvent(this, pd, true));
            } else {
              System.err.println("DataSourceASCIILine: " + itsMonitorPoint + 
                                 " WAS NOT FOUND!");
            }  
          }
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("DataSourceASCIILine:DataReader.run (" + itsHostName 
                             + ":" + itsPort + "): " + e.getClass());
          try { disconnect(); } catch (Exception f) { }
        }
      }
    }
  }

  public final static
  void
  main(String[] argv)
  {
     if (argv.length<1) {
       System.err.println("Missing argument: Needs IP:port of the server");
       System.exit(1);
     }
     DataSourceASCIILine al = new DataSourceASCIILine(argv[0].split(":"));

     try {
       al.connect();
     } catch (Exception e) {
       System.err.println(e.getMessage());
       System.exit(1);
     }
     while (true) {
       try {
         RelTime.factory(5000000).sleep();
       } catch (Exception e) {}
     }
  }

}
