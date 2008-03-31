//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.util.HashMap;
import java.net.*;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
//import atnf.atoms.mon.rpc.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.time.*;

/**
 * Used to make information about the monitor system itself available
 * for monitoring. Run-time information is all stored in a Named Value
 * List so that it can be easily extracted by listening points.
 *
 * An additional feature is to dynamically create new monitor point
 * objects which encapsulate the connection status, number of assigned points
 * and number of transactions for the different DataSources
 * used by the system. The string argument will be prefixed to the
 * names of each DataSource.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: DataSourceMonitorSystem.java,v 1.8 2007/06/05 01:25:42 bro764 Exp $
 */
class DataSourceMonitorSystem extends DataSource
{
  /** Name to prefix to DataSource monitor points. */
  private String itsPrefix = null;

  /** Name of monitor point which contains the DataSource status NVL */
  private String itsParentDS = null;
  /** Name of monitor point which contains the DataSource Number of Points
   * NVL */
  private String itsParentNP = null;
  /** Name of monitor point which contains the DataSource Number of
   * Transactions NVL */
  private String itsParentNT = null;

  /** Records if <i>getRealData</i> has been called before. We only
   * create the DataSource monitor points during the first call. */
  private boolean itsFirstCall = true;

  /** Hash table to map between channel names and decsriptions. This is
   * only used when creating the DataSource status monitor points during
   * initialisation. */
  private HashMap itsMappings = new HashMap();

  /** Constructor */
  public
  DataSourceMonitorSystem(String info)
  {
    super("MONITORMonitor");

    String[] tokens = MonitorUtils.getTokens(info);

    if (tokens==null) {
      System.err.println("DataSourceMonitorSystem: Got null Arguments: "
			 + "Can't Monitor DataSources");
    } else if (tokens.length<4) {
      System.err.println("DataSourceMonitorSystem: Got " + tokens.length +
			 " Arguments: Can't Monitor DataSources");
    } else {
      //Get the two mandatory arguments
      itsPrefix = tokens[0];
      itsParentDS = tokens[1];
      itsParentNP = tokens[2];
      itsParentNT = tokens[3];
      //Read the mapping between channels and descriptions
      for (int i=4; i<tokens.length; i++) {
	//Check for the :
	int colonindex = tokens[i].indexOf(":");
	if (colonindex==-1) {
	  System.err.println("DataSourceMonitorSystem: Expected " +
			     "<channel>:<description>");
	  continue;
	}
	String channel = tokens[i].substring(0, colonindex);
	String descrip = tokens[i].substring(colonindex+1);
	//Save the mapping for later use
	itsMappings.put(channel, descrip);
	//System.err.println("###" + channel + " => " + descrip);
      }
    }
    MonitorMap.addDataSource(this);
  }

   /** Get the latest information from our internal data structures. */
   protected
   void
   getData(Object[] points)
   throws Exception
   {
     //Precondition
     if (points==null || points.length==0 || !itsConnected) return;

     if (itsFirstCall && itsPrefix!=null
	 && itsParentDS!=null  && itsParentNP!=null  && itsParentNT!=null) {
       //Create monitor points to represent the status of each DataSource
       itsFirstCall = false;
       makeDataSourcePoints();
     }

     Object[] buf = points;
     Object data = null;
   
     try {
       //Increment transaction counter
       itsNumTransactions += buf.length;

       for (int i=0; i<buf.length; i++) {
	 PointMonitor pm = (PointMonitor)buf[i];
	 //This point wants the the Misc. information we've got
	 HashMap res = new HashMap();

         HashMap dataSources   = new HashMap(); //connection status
         HashMap dataSource_NP = new HashMap(); //Number of points
         HashMap dataSource_NT = new HashMap(); //Number of transactions
         String[] dsa = MonitorMap.getDataSources();
         for (int j=0; j<dsa.length; j++) {
	   DataSource ds = MonitorMap.getDataSource(dsa[j]);
	   //Bit of an issue here because the Datasource methods are
           //synchronized so we will hang if one of them is blocked.
	   dataSources.put(dsa[j], new Boolean(ds.isConnected()));
	   dataSource_NP.put(dsa[j]+"-NP", new Integer(ds.getNumPoints()));
	   dataSource_NT.put(dsa[j]+"-NT", new Long(ds.getNumTransactions()));
	 }
	 res.put("DataSourceNames", dsa);
	 res.put("DataSources", dataSources);
	 res.put("DataSources-NP", dataSource_NP);
	 res.put("DataSources-NT", dataSource_NT);

	 res.put("TotalMemory", new Float((float)(MonitorMap.getTotalMemory())/(1024*1024)));

         res.put("NumClntsJ", new Integer(MonitorServerCustom.getNumClients()));
         res.put("NumClntsA", new Integer(MonitorServerASCII.getNumClients()));

         res.put("TimeUTC", new AbsTime());

	 /*       OK, this doesn't work very well anyway. So lets not use native code.
	  res.put("CPUTime", new Long(MonitorMap.getCPUTime()));
	  res.put("CPUUserTime", new Long(MonitorMap.getCPUUserTime()));
	  res.put("CPUSystemTime", new Long(MonitorMap.getCPUSystemTime()));
	  */
	 //res.put("PointNames", MonitorMap.getPointNames());
	 //res.put("NumDSCThreads", new Integer(MonitorMap.getNumDSCThreads()));
	 pm.firePointEvent(new PointEvent(this, new
					  PointData(pm.getName(), AbsTime.factory(),
						    res), true));
       }
     } catch (Exception e) {e.printStackTrace();}
   }


  /** Create monitor points to represent each DataSource. */
  private
  void
  makeDataSourcePoints()
  {
    //Get the names of all the DataSources which exist
    String[] dsa = MonitorMap.getDataSources();

    for (int i=0; i<dsa.length; i++) {
      try {
	if (!dsa[i].equals("DSMonitorSystem") && !dsa[i].equals("MONITORmonitor")) {
	  //We need to come up with a source name and a point name
	  String source = dsa[i].substring(dsa[i].lastIndexOf("//")+2);
	  String name = dsa[i].substring(0, dsa[i].indexOf(":"));
          //Do some string-wise niddly-fiddling..
	  String[] pointname = {itsPrefix + name};
	  final String[] nullarray = {"-"};
	  String[] translations = {"NV-" + dsa[i],
	  "StringMap-\"true:OK\"\"false:DISCONNECTED\""};

	  //Translate to more meaningful description if possible
	  String newname = (String)itsMappings.get(name);
	  if (newname!=null) name = newname;

	  String description = name + " connection status";
	  PointMonitor mp = PointMonitor.factory(pointname,
						 description,
						 name, //Short description
						 "", //Units
						 source,
                                                 "Listen-" + itsParentDS,
						 translations,
						 "StringMatch-\"true\"\"OK\"",
						 nullarray,
						 "5000000",
						 true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);


	  description = name + " num. points queued";
	  String[] translationsNP = {"NV-" + dsa[i] + "-NP"};
	  String[] nameNP = {pointname[0] + "-numPoints"};
          mp = PointMonitor.factory(nameNP,
                                    description,
                                    name, //Short description
                                    "", //Units
                                    source,
                                    "Listen-" + itsParentNP,
                                    translationsNP,
                                    "-",
                                    nullarray,
                                    "5000000",
                                    true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);

	  description = name + " num. transactions";
	  String[] translationsNT = {"NV-" + dsa[i] + "-NT"};
	  String[] nameNT = {pointname[0] + "-numTrans"};
          mp = PointMonitor.factory(nameNT,
                                    description,
                                    name, //Short description
                                    "", //Units
                                    source,
                                    "Listen-" + itsParentNT,
                                    translationsNT,
                                    "-",
                                    nullarray,
                                    "5000000",
                                    true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
        }
      } catch (Exception e) {
	System.err.println("DataSourceMonitorSystem: \"" + dsa[i] + "\"" +
			   e.getMessage());
      }
    }

    //Finished with this now
    itsMappings = null;
  }
}
