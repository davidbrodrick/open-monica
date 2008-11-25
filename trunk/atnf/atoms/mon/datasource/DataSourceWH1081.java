// Copyright (C) Oz Forecast, NSW, Australia.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.util.*;
import java.io.*;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.*;

/**
 * WH1081 el-cheapo weather station driver.
 *
 * <P>Parses the output of wwsr, so wwsr should be installed SUID:
 * http://www.pendec.dk/weatherstation.htm
 *
 * <P>Uses the <i>timeout</i> program when invoking wwsr.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class DataSourceWH1081
{
  /** Name hierarchy base location for our output monitor points */
  private String itsTreeBase="weather.wh1081";
  
  /** Source name to use for generated data. */
  private String itsSource=null;
  
  /** Wind direction offset. */
  private float itsWindRotation = 0.0f;
  
  /** Absolute barometer offset. */
  private float itsPresOffset = 0.0f;
  
  /** Scale factor for rain tipper. */
  private float itsRainScale = 1.0f;
  
  /** Maximum wind speed in last 15min. */
  protected float itsWindMax = 0.0f;
  /** Sum of all wind measurements over last 15min. */
  protected double itsWindSum = 0.0f;
  /** For calculating avg wind over last 15min. */
  protected double itsDirSum = 0.0f;
  /** Number of wind measurements over last 15min. */
  protected int itsWindCount = 0;
  /** The minute we last reset these measurements. */
  protected int itsMinute = -1;

  /** Maximum wind speed over previous 15min. */
  protected float itsWindMaxL = 0.0f;
  /** Average wind speed over previous 15min. */
  protected float itsWindAvgL = 0.0f;
  /** Average wind speed over previous 15min. */
  protected float itsDirAvgL = 0.0f;

  public DataSourceWH1081(String[] args)
  {
    itsSource=args[0];
    itsTreeBase=args[1];
    itsWindRotation=Float.parseFloat(args[2]);
    itsPresOffset=Float.parseFloat(args[3]);
    itsRainScale=Float.parseFloat(args[4]);
    System.err.println(itsSource + "\t" + itsTreeBase + "\t" + itsWindRotation + "\t" + itsPresOffset);
    createMonitorPoints();
    
    //Start the thread that reads the data as it comes in
    DataReader worker = new DataReader();
    worker.start();
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
    String[] fifteenmins={"Mean-\"900\"\"F\"", "NumDecimals-\"1\""};
    String[] archivepolicy={"COUNTER-1"};
    String[] archivepolicychange={"CHANGE-"};
    //Inside temperature
    String[] pointnames={itsTreeBase+".in_temp"};
    PointMonitor mp = PointMonitor.factory(pointnames,
                      "Inside temperature", "", "C",
                      itsSource, "-", fifteenmins, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Outside temperature
    String[] pointnames2={itsTreeBase+".out_temp"};
    mp = PointMonitor.factory(pointnames2,
                      "Outside temperature", "", "C",
                      itsSource, "-", fifteenmins, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Inside humidity
    String[] pointnames3={itsTreeBase+".in_humid"};
    mp = PointMonitor.factory(pointnames3,
                      "Inside humidity", "", "%",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Outside humidity
    String[] pointnames4={itsTreeBase+".out_humid"};
    mp = PointMonitor.factory(pointnames4,
                      "Outside humidity", "", "%",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Barometric pressure
    String[] pointnames7={itsTreeBase+".pressure"};
    mp = PointMonitor.factory(pointnames7,
                      "Seal-level pressure", "", "hPa",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Wind direction
    String[] pointnames8={itsTreeBase+".wind_dir"};
    mp = PointMonitor.factory(pointnames8,
                      "Wind direction", "", "deg",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Average wind direction
    String[] pointnames17={itsTreeBase+".wind_dir_15m"};
    mp = PointMonitor.factory(pointnames17,
                      "Average wind direction", "", "deg",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);      
    //Wind gust speed
    String[] pointnames9={itsTreeBase+".wind_gust_speed"};
    mp = PointMonitor.factory(pointnames9,
                      "Wind gust speed", "", "km/h",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Peak wind gust speed
    String[] pointnames14={itsTreeBase+".wind_gust_15m"};
    mp = PointMonitor.factory(pointnames14,
                      "15m max wind gust", "", "km/h",
                      itsSource, "-", nullarray, "-",
                      archivepolicychange, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Average wind speed
    String[] pointnames11={itsTreeBase+".wind_avg_speed"};
    mp = PointMonitor.factory(pointnames11,
                      "Average wind speed", "", "km/h",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Average wind speed
    String[] pointnames15={itsTreeBase+".wind_avg_15m"};
    mp = PointMonitor.factory(pointnames15,
                      "15m average wind speed", "", "km/h",
                      itsSource, "-", nullarray, "-",
                      archivepolicychange, "60000000", true);
    if (mp!=null) MonitorMap.addPointMonitor(mp);
    //Total rainfall
    String[] pointnames12={itsTreeBase+".rain"};
    mp = PointMonitor.factory(pointnames12,
                      "Rainfall (since 9am)", "", "mm",
                      itsSource, "-", nullarray, "-",
                      archivepolicy, "60000000", true);
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
      String s = null;
      int lastinterval=-1;
      Float lastrain=null;
      Float rain9am=null;
      int lastresetdate=-1;
      while (true) {
        try {
          Process p = Runtime.getRuntime().exec("timeout 10 wwsr -a");
            
          BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
          BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

          s = stdError.readLine();
          if (s!=null) {
            System.err.println("DataSourceWH1081: " + s);
            throw new Exception(s);
          }
        
          String line = stdInput.readLine();
          line = stdInput.readLine();
          line = stdInput.readLine();
          line = stdInput.readLine();
          Float in_humid=new Float(line.substring(20,line.length()).trim());
          line = stdInput.readLine();
          Float out_humid=new Float(line.substring(20,line.length()).trim());
          if (out_humid.floatValue()>100) {
            System.err.println("DataSourceWH1081: Invalid data..");
            throw new Exception("Outside humidity out of range");
          }
          line = stdInput.readLine();
          Float in_temp=new Float(line.substring(20,line.length()).trim());
          //Check for invalid values
          if (in_temp.floatValue()>80 || in_temp.floatValue()<-20) {
            System.err.println("DataSourceWH1081: Invalid data..");
            throw new Exception("Inside temperature out of range");
          }
          line = stdInput.readLine();
          Float out_temp=new Float(line.substring(20,line.length()).trim());
          //Check for invalid values
          if (out_temp.floatValue()>80 || out_temp.floatValue()<-20) {
            System.err.println("DataSourceWH1081: Invalid data..");
            throw new Exception("Outside temperature out of range");
          }
          line = stdInput.readLine();
          Float wind_avg=new Float(Float.parseFloat(line.substring(20,line.length()).trim())*3.6);
          line = stdInput.readLine();
          Float wind_gust=new Float(Float.parseFloat(line.substring(20,line.length()).trim())*3.6);
          line = stdInput.readLine();
          String wind_dir_temp=line.substring(20,line.length()).trim();
          double wdir;
          if (wind_dir_temp.equals("N")) wdir=0;
          else if (wind_dir_temp.equals("NNE")) wdir=22.5;
          else if (wind_dir_temp.equals("NE"))  wdir=45.0;
          else if (wind_dir_temp.equals("ENE")) wdir=67.5;
          else if (wind_dir_temp.equals("E"))   wdir=90.0;
          else if (wind_dir_temp.equals("ESE")) wdir=112.5;
          else if (wind_dir_temp.equals("SE"))  wdir=135.0;
          else if (wind_dir_temp.equals("SSE")) wdir=157.5;
          else if (wind_dir_temp.equals("S"))   wdir=180.0;
          else if (wind_dir_temp.equals("SSW")) wdir=202.5;
          else if (wind_dir_temp.equals("SW"))  wdir=225.0;
          else if (wind_dir_temp.equals("WSW")) wdir=247.5;
          else if (wind_dir_temp.equals("W"))   wdir=270.0;
          else if (wind_dir_temp.equals("WNW")) wdir=295.2;
          else if (wind_dir_temp.equals("NW"))  wdir=315.0;
          else wdir=337.5;
          //Apply any wind direction correction
          wdir=wdir+itsWindRotation;
          if (wdir<0) wdir=360+wdir;
          if (wdir>360) wdir=wdir-360;
          Float wind_dir=new Float(wdir);
          line = stdInput.readLine();
          line = stdInput.readLine();
          Float rain=new Float(line.substring(20,line.length()).trim());
          lastrain=rain;
          if (rain9am==null) {
            System.err.println("DataSourceWH1081: 9am rainfall not set yet");
            throw new Exception("DataSourceWH1081: 9am rainfall not set yet");
          }
          if (rain.floatValue()<rain9am.floatValue()) {
            System.err.println("DataSourceWH1081: rain reset since 9am");
            throw new Exception("DataSourceWH1081: rain reset since 9am");
          }
          rain=new Float(itsRainScale*(rain.floatValue()-rain9am.floatValue()));
          line = stdInput.readLine();
          line = stdInput.readLine();
          line = stdInput.readLine();
          float pres_temp=Float.parseFloat(line.substring(20,line.length()).trim());
          Float pres=new Float(pres_temp+itsPresOffset);
      
          if (wind_gust.floatValue()>itsWindMax) itsWindMax=wind_gust.floatValue();
          itsWindSum+=wind_avg.floatValue();
          itsDirSum+=wdir+360;
          itsWindCount++;    

          PointMonitor pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".in_temp");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".in_temp", itsSource, in_temp);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".out_temp");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".out_temp", itsSource, out_temp);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".in_humid");
          if (pm!=null) {
          PointData pd=new PointData(itsTreeBase+".in_humid", itsSource, in_humid);
          pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".out_humid");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".out_humid", itsSource, out_humid);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".pressure");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".pressure", itsSource, pres);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_dir");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_gust_dir", itsSource, wind_dir);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_gust_speed");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_gust_speed", itsSource, wind_gust);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_avg_speed");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_avg_speed", itsSource, wind_avg);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".rain");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".rain", itsSource, rain);
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_gust_15m");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_gust_15m", itsSource, new Float(itsWindMaxL));
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_avg_15m");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_avg_15m", itsSource, new Float(itsWindAvgL));
            pm.firePointEvent(new PointEvent(this, pd, true));
          }            
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_dir_15m");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_dir_15m", itsSource, new Float(itsDirAvgL));
            pm.firePointEvent(new PointEvent(this, pd, true));
          } 
        } catch (Exception e) {          
          System.err.println("DataSourceWH1081: " + e.getClass());
        }
        
        //Check for 15-min mark for peak/avg wind
        Calendar c = Calendar.getInstance();
        if ((c.get(Calendar.MINUTE)==0 ||
             c.get(Calendar.MINUTE)==15 ||
             c.get(Calendar.MINUTE)==30 ||
             c.get(Calendar.MINUTE)==45) &&
             c.get(Calendar.MINUTE)!=itsMinute) {
          //Time to process lastest max/avg wind speed
          itsWindMaxL = itsWindMax;
          itsWindAvgL = (float)(itsWindSum/itsWindCount);
          itsDirAvgL  = (float)(itsDirSum/itsWindCount)-360;
          itsWindCount = 0;
          itsDirSum   = 0.0f;
          itsWindSum   = 0.0f;
          itsWindMax   = 0.0f;
          itsMinute = c.get(Calendar.MINUTE);            
          PointMonitor pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_gust_15m");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_gust_15m", itsSource, new Float(itsWindMaxL));
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_avg_15m");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_avg_15m", itsSource, new Float(itsWindAvgL));
            pm.firePointEvent(new PointEvent(this, pd, true));
          }            
          pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".wind_dir_15m");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".wind_dir_15m", itsSource, new Float(itsDirAvgL));
            pm.firePointEvent(new PointEvent(this, pd, true));
          } 
        }
        if (lastrain!=null &&
            c.get(Calendar.DAY_OF_MONTH)!=lastresetdate &&
            c.get(Calendar.HOUR_OF_DAY)==9 &&
            (c.get(Calendar.MINUTE)==1 || c.get(Calendar.MINUTE)==2)) {
          //Time to do the 9am rainfall reset
          rain9am=lastrain;
          lastresetdate=c.get(Calendar.DAY_OF_MONTH);
          PointMonitor pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".rain");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".rain", itsSource, new Float(0.0));
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
        }
        if (lastrain!=null && rain9am==null) {
          //Program must have just started. All we can do is assume there
          //has been no rain today..
          rain9am=lastrain;
          PointMonitor pm = MonitorMap.getPointMonitor(itsSource+"."+itsTreeBase+".rain");
          if (pm!=null) {
            PointData pd=new PointData(itsTreeBase+".rain", itsSource, new Float(0.0));
            pm.firePointEvent(new PointEvent(this, pd, true));
          }
        }
        
        try {
          RelTime.factory(15000000).sleep();
        } catch (Exception f) {}
      }
    }
  }
}
