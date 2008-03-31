// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.util.StringTokenizer;
import java.util.HashMap;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.*;

/**
 * Monitor Thytec brand UPSs. This currently supports two different varieties,
 * the appropriate one is choosen based on information returned by the UPS.
 * It can be easily extended to support other versions/varieties.
 *
 * <P>The argument string needs to contain:
 * <ul>
 * <li>name of the serial/ethernet interface.
 * <li>port number on the serial/ethernet box.
 * <li>name of monitor point to fire updates to.
 * </ul>
 *
 * <P>Here is an example string for the monitor-sources.txt:<BR>
 * <tt>DataSourceThytecUPS thytecups://ca06ups:7001:ca06.power.ups.antenna.alldata</tt>
 *
 * <P>The specified monitor point will receive updates with a HashMap which
 * contains all of the data extracted from the UPS. You can then define
 * monitor points which listen for updates to this point and extract
 * individual pieces of data by using TranslationNV's.
 *
 * @author David Brodrick
 * @version $Id: DataSourceThytecUPS.java,v 1.1 2008/03/02 22:58:47 bro764 Exp $
 **/
public class DataSourceThytecUPS
extends DataSourceASCIISocket
{
  /** Name of the monitor point we fire updates to */
  private String itsMonitorPointName=null;
  /** Monitor point we fire updates to */
  private PointMonitor itsMonitorPoint=null;

  public DataSourceThytecUPS(String nameOfSource)
  {
    super(nameOfSource.substring(0,nameOfSource.lastIndexOf(":")));

    itsMonitorPointName=nameOfSource.substring(nameOfSource.lastIndexOf(":")+1, nameOfSource.length());

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
  
  /** Data is pushed, so this method is redundant in this class. */
  protected
  void
  getData(Object[] points)
  throws Exception
  {
    return;
  }

  /** UPS version currently used in ATCA antennas. */
  public
  HashMap
  parse31V72(String line)
  {
    HashMap res=new HashMap();
    //Input voltages
    res.put("INPV1", new Integer(line.substring(102,105).trim())); //V
    res.put("INPV2", new Integer(line.substring(105,108).trim())); //V
    res.put("INPV3", new Integer(line.substring(108,111).trim())); //V
    res.put("INPI1", new Integer(line.substring(131,133).trim())); //I
    res.put("INPI2", new Integer(line.substring(134,136).trim())); //I
    res.put("INPI3", new Integer(line.substring(137,139).trim())); //I
    res.put("INPF", new Float(line.substring(145,149).trim())); //F

    //Inverter
    res.put("INVV", new Integer(line.substring(191,194).trim())); //V
    res.put("INVI", new Integer(line.substring(209,212).trim())); //I
    res.put("INVF", new Float(line.substring(218,222).trim())); //F

    //Bypass supply
    res.put("BYPV", new Integer(line.substring(255,258).trim())); //Voltage
    res.put("BYPF", new Float(line.substring(264,268).trim())); //Frequency
    res.put("BYPS", new Boolean(line.substring(270,291).equals("INVERTER SYNCHRONIZED")));

    //Battery charging
    res.put("CHAV", new Integer(line.substring(326,329).trim())); //Voltage
    res.put("CHAI", new Float(line.substring(335,339).trim())); //Current
    res.put("CHAT", new Integer(line.substring(358,364).trim())); //Runtime

    //Temperatures
    res.put("TEMR", new Integer(line.substring(400,402).trim())); //Recitifer
    res.put("TEMI", new Integer(line.substring(417,419).trim())); //Inverter

    //Alarm status
    String alarm=line.substring(462,line.length()).trim();
    if (alarm.equals("ALARM STATUS NORMAL")) {
      alarm="OK";
    }
    res.put("ALARM", alarm);

    return res;
  }

  /** UPS version currently used at Mopra. */
  public
  HashMap
  parse33V33(String line)
  {
    HashMap res=new HashMap();
    //Inputs
    res.put("INPV1", new Integer(line.substring(102,105).trim())); //V
    res.put("INPV2", new Integer(line.substring(105,108).trim())); //V
    res.put("INPV3", new Integer(line.substring(108,111).trim())); //V
    res.put("INPI1", new Integer(line.substring(131,133).trim())); //I
    res.put("INPI2", new Integer(line.substring(134,136).trim())); //I
    res.put("INPI3", new Integer(line.substring(137,139).trim())); //I
    res.put("INPF", new Float(line.substring(145,149).trim())); //F

    //Inverter output
    res.put("OUTV1", new Integer(line.substring(195,198).trim())); //V
    res.put("OUTV2", new Integer(line.substring(198,201).trim())); //V
    res.put("OUTV3", new Integer(line.substring(201,204).trim())); //V
    res.put("OUTI1", new Integer(line.substring(224,226).trim())); //I
    res.put("OUTI2", new Integer(line.substring(227,229).trim())); //I
    res.put("OUTI3", new Integer(line.substring(230,232).trim())); //I
    res.put("OUTF", new Float(line.substring(238,242).trim())); //F

    //Bypass supply
    res.put("BYPV1", new Integer(line.substring(288,291).trim()));
    res.put("BYPV2", new Integer(line.substring(291,294).trim()));
    res.put("BYPV3", new Integer(line.substring(294,297).trim()));
    res.put("BYPF", new Float(line.substring(303,307).trim())); //Frequency
    res.put("BYPS", new Boolean(line.substring(309,330).equals("INVERTER SYNCHRONIZED")));

    //Battery charging
    res.put("CHAV", new Integer(line.substring(365,368).trim())); //Voltage
    res.put("CHAI", new Float(line.substring(374,378).trim())); //Current
    res.put("CHAT", new Integer(line.substring(398,403).trim())); //Runtime

    //Temperatures
    res.put("TEMR", new Integer(line.substring(439,441).trim())); //Recitifer
    res.put("TEMI", new Integer(line.substring(456,458).trim())); //Inverter

    //Alarm status
    String alarm=line.substring(502,line.length()).trim();
    if (alarm.equals("ALARM STATUS NORMAL")) {
      alarm="OK";
    }
    res.put("ALARM", alarm);

    return res;

  }


  ////////////////////////////////////////////////////////////////////////
  /** Nested class to collect data and fire monitor point update events
   * as new data becomes available from the ASCII socket. */
  public class
  DataReader
  extends Thread
  {
    public void	run()
    {
      boolean firstread=true;
      while (true) {
        //If we're not connected then try to reconnect
        if (!itsConnected) {
          try {
            if (!connect()) {
              //Sleep for a while then try again
              try { RelTime.factory(5000000).sleep(); } catch (Exception f) {}
              continue;
	    }
            firstread=true;
          } catch (Exception e) {
            //Sleep for a while then try again
            try { RelTime.factory(5000000).sleep(); } catch (Exception f) {}
            continue;
          }
        }
        try {
	  //Read a line from the server
	  String thisline="";
	  while (true) {
	    int thischar = itsReader.read();
	    if (thischar==27) {
	      String escape="";
	      //System.out.println("escape");
	      //Ignore escape sequences
	      while (true) {
		thischar=itsReader.read();
		//System.out.println("{"+thischar + "\t"+(char)thischar+"}");
		if (thischar>=65 && thischar<=90 || thischar>=97 && thischar<=122) {
		  //thischar=itsReader.read();
		  break;
		}
                escape=escape+(char)thischar;
	      }
	      //System.out.println(escape);
	      //Detect when cursors goes back to home position
	      if (escape.indexOf("[1;2")!=-1) {
		//System.out.println("HOME");
		if (!firstread) {
		  thisline=thisline.trim();
		  //System.out.println(thisline);
		  String version=thisline.substring(0,3)+thisline.substring(32,36);
                  HashMap newdata=null;
		  if (version.equals("3/1V7.2")) {
		    newdata=parse31V72(thisline);
		  } else if (version.equals("3/3V3.3")) {
		    newdata=parse33V33(thisline);
		  } else {
		    System.err.println("ThytecUPS: " + itsHostName + ": Unsupported UPS version " + version);
		  }
		  //If we got new data then fire it off
		  if (newdata!=null) {
		    if (itsMonitorPoint==null) {
		      itsMonitorPoint=MonitorMap.getPointMonitor(itsMonitorPointName);
		      if (itsMonitorPoint==null) {
			MonitorMap.logger.error("MoniCA: ThytecUPS: No monitor point " + itsMonitorPointName);
			System.err.println("#############MoniCA: ThytecUPS: No monitor point " + itsMonitorPointName);
		      }
		    }
                    if (itsMonitorPoint!=null) {
		      itsMonitorPoint.firePointEvent(new PointEvent(this,
								    new PointData(itsMonitorPoint.getName(), itsMonitorPoint.getSource(), newdata), true));
		    }
		  }
		} else {
		  firstread=false;
		}
		break;
	      }
              continue;
	    }
	    if (thischar!=0) {
	      //System.out.println(thischar + "\t"+(char)thischar);
	      thisline=thisline+(char)thischar;
	    }
	  }
        } catch (Exception e) {
          e.printStackTrace();
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
       System.err.println("Missing argument: Needs IP of the UPS serial/ethernet converter");
       System.exit(1);
     }
     DataSourceThytecUPS ups = new DataSourceThytecUPS("thytecups://" + argv[0] + ":foo");

     try {
       ups.connect();
/*       while (true) {
	 ups.parseResponse(ups.sendRequest(OUTCUR), new HashMap());
	 RelTime sleep = RelTime.factory(5000000);
	 try {
           sleep.sleep();
	 } catch (Exception j) { }
       }*/
//       HashMap res = ups.getNewData();
//       System.err.println("HashMap=" + res);
     } catch (Exception e) {
       System.err.println(e.getMessage());
       System.exit(1);
     }
//     System.exit(0);

     while (true);
   }

}
