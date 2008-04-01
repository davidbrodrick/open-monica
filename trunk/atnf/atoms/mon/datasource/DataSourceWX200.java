// Copyright (C) Oz Forecast, NSW, Australia.
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
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class DataSourceWX200
extends DataSourceASCIISocket
{
  /** Name hierarchy base location for our output monitor points */
  private String itsTreeBase="weather";

  public DataSourceWX200(String nameOfSource)
  {
    super(nameOfSource.substring(0,nameOfSource.lastIndexOf(":")));

    itsTreeBase=nameOfSource.substring(nameOfSource.lastIndexOf(":")+1, nameOfSource.length());
    createMonitorPoints();
    
    //Set a longer socket timeout than the default
    setTimeout(60000);
    
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

  /** Create the monitor points which we will update. */
  protected
  void
  createMonitorPoints()
  {
 	  String[] nullarray={"-"};
    String[] archivepolicy={"COUNTER-1"};
    String[] archivepolicychange={"CHANGE-"};
    //Inside temperature
    String[] pointnames={itsTreeBase+".in_temp"};
    PointMonitor mp = PointMonitor.factory(pointnames,
                      "Inside temperature", "", "C",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Outside temperature
    String[] pointnames2={itsTreeBase+".out_temp"};
    mp = PointMonitor.factory(pointnames2,
                      "Outside temperature", "", "C",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Inside humidity
    String[] pointnames3={itsTreeBase+".in_humid"};
    mp = PointMonitor.factory(pointnames3,
                      "Inside humidity", "", "%",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Outside humidity
    String[] pointnames4={itsTreeBase+".out_humid"};
    mp = PointMonitor.factory(pointnames4,
                      "Outside humidity", "", "%",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Inside dew point temperature
    String[] pointnames5={itsTreeBase+".in_dewpoint"};
    mp = PointMonitor.factory(pointnames5,
                      "Inside dew point", "", "C",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Outside dew point temperature
    String[] pointnames6={itsTreeBase+".out_dewpoint"};
    mp = PointMonitor.factory(pointnames6,
                      "Outside dew point", "", "C",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Barometric pressure
    String[] pointnames7={itsTreeBase+".pressure"};
    mp = PointMonitor.factory(pointnames7,
                      "Seal-level pressure", "", "hPa",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Wind gust direction
    String[] pointnames8={itsTreeBase+".wind_gust_dir"};
    mp = PointMonitor.factory(pointnames8,
                      "Wind gust direction", "", "deg",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Wind gust speed
    String[] pointnames9={itsTreeBase+".wind_gust_speed"};
    mp = PointMonitor.factory(pointnames9,
                      "Wind gust speed", "", "km/h",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);

    //Average wind direction
    String[] pointnames10={itsTreeBase+".wind_avg_dir"};
    mp = PointMonitor.factory(pointnames10,
                      "Average wind direction", "", "deg",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Average wind speed
    String[] pointnames11={itsTreeBase+".wind_avg_speed"};
    mp = PointMonitor.factory(pointnames11,
                      "Average wind speed", "", "km/h",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Total rainfall
    String[] pointnames12={itsTreeBase+".rain"};
    mp = PointMonitor.factory(pointnames12,
                      "Rainfall (since 9am)", "", "mm",
                      itsHostName, "-", nullarray, "-",
                      archivepolicy, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Power source
    String[] pointnames13={itsTreeBase+".power"};
    mp = PointMonitor.factory(pointnames13,
                      "Mains power failure", "", "",
                      itsHostName, "-", nullarray, "-",
                      archivepolicychange, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Low battery warning
    String[] pointnames14={itsTreeBase+".battery"};
    mp = PointMonitor.factory(pointnames14,
                      "Low battery warning", "", "",
                      itsHostName, "-", nullarray, "-",
                      archivepolicychange, "10000000", true);
	  if (mp!=null) MonitorMap.addPointMonitor(mp);
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
          while (true) {
            String line = itsReader.readLine();
            String[] tokens=line.split("\t");
            
            //Don't log temp/humidity points if they are invalid
            Float in_temp=new Float(tokens[2]);
            Float in_humid=new Float(tokens[4]);
            Float in_dewpoint=new Float(tokens[6]);
            if (in_temp.floatValue()==0.0 && in_humid.floatValue()==0.0 && in_dewpoint.floatValue()==0.0) {
              in_temp=null;
              in_humid=null;
              in_dewpoint=null;
            }
            Float out_temp=new Float(tokens[3]);
            Float out_humid=new Float(tokens[5]);
            Float out_dewpoint=new Float(tokens[7]);
            if (out_temp.floatValue()==0.0 && out_humid.floatValue()==0.0 && out_dewpoint.floatValue()==0.0) {
              out_temp=null;
              out_humid=null;
              out_dewpoint=null;
            }
            
            PointMonitor pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".in_temp");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".in_temp", itsHostName, in_temp);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".out_temp");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".out_temp", itsHostName, out_temp);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }

            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".in_humid");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".in_humid", itsHostName, in_humid);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".out_humid");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".out_humid", itsHostName, out_humid);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
              pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".in_dewpoint");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".in_dewpoint", itsHostName, in_dewpoint);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
              pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".out_dewpoint");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".out_dewpoint", itsHostName, out_dewpoint);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".pressure");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".pressure", itsHostName, new Float(tokens[8]));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".wind_gust_dir");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".wind_gust_dir", itsHostName, new Float(tokens[9]));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".wind_gust_speed");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".wind_gust_speed", itsHostName, new Float(Float.parseFloat(tokens[10])*3600.0/1000.0));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".wind_avg_dir");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".wind_avg_dir", itsHostName, new Float(tokens[11]));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".wind_avg_speed");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".wind_avg_speed", itsHostName, new Float(Float.parseFloat(tokens[12])*3600.0/1000.0));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".rain");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".rain", itsHostName, new Float(tokens[13]));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".power");
            if (pm!=null) {
              PointData pd=new PointData(itsTreeBase+".power", itsHostName, new Boolean(tokens[14]));
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
            pm = MonitorMap.getPointMonitor(itsHostName+"."+itsTreeBase+".battery");
            if (pm!=null) {
              Boolean newval=new Boolean(true);
              if (tokens[15].trim().equals("0"))
                newval=new Boolean(false);
              PointData pd=new PointData(itsTreeBase+".battery", itsHostName, newval);
              pm.firePointEvent(new PointEvent(this, pd, true));
            }
 
            
          }
        } catch (Exception e) {
          System.err.println("DataSourceWX200:DataReader.run (" + itsHostName 
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
       System.err.println("Missing argument: Needs IP:port of the wx200d server");
       System.exit(1);
     }
     DataSourceWX200 ups = new DataSourceWX200("wx200://" + argv[0] + ":weather");

     try {
       ups.connect();
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
