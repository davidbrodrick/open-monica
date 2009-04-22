// Copyright (C) Oz Forecast, NSW, Australia.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.io.*;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.*;

/**
 * WH1081 SinoMeter el-cheapo weather station driver.
 *
 * <P>Parses the output of wwsr, so wwsr should be installed SUID:
 * http://www.pendec.dk/weatherstation.htm
 *
 * <P>Uses the <i>timeout</i> program when invoking wwsr, so that is
 * also required.
 *
 * <P>Each data point associated with this source is given an array of the
 * latest values which contains:
 * <bl>
 * <li>Inside humidity (%)
 * <li>Outside humidity (%)
 * <li>Inside temp (degrees C)
 * <li>Outside temp (degrees C)
 * <li>Avg wind speed (km/h)
 * <li>Wind gust speed (km/h)
 * <li>Wind direction (degrees of azimuth)
 * <li>Pressure (hPa)
 * <li>Rain since last reading (tips)
 * </bl>
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class WH1081
extends ExternalSystem
{
  /** The number of elements in the data array we produce. */
  private static final int theirNumElements = 9;
  
  /** The entry in the data array which corresponds to rainfall. */
  private static final int theirRainElement = 8;
  
  /** The last 'interval' reading. */
  private String itsLastInterval = null;
  
  /** The last 'history pos' reading. */
  private String itsLastHistory = null;
  
  /** The last reading of total rainfall. */
  private Float itsLastRain = null;
  
  /** The last valid data we collected. */
  private Float[] itsLastData = null;
  
  public WH1081(String[] args)
  {
    super("wh1081");
  }

  /** Collect new data for requesting monitor points. */
  protected
  void
  getData(PointDescription[] points)
  throws Exception
  {
    //Get the actual data
    Float[] newdata=getNewWeather();
    
    //Fire new data to each point
    for (int i=0; i<points.length; i++) {
      PointDescription pm = points[i];
      PointData pd = new PointData(pm.getName(), pm.getSource(), newdata);
      PointEvent pe = new PointEvent(pm, pd, true);
      pm.firePointEvent(pe);
    }
  }


  /** Execute wwsr and return array of new data, or null if error. */
  protected
  Float[]
  getNewWeather()
  {
    Float[] res=new Float[theirNumElements];
    
    try {
      Process p = Runtime.getRuntime().exec("timeout 10 wwsr -a");
            
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      String err = stdError.readLine();
      if (err!=null) {
        System.err.println("WH1081: " + err);
        return null;
      }
        
      String line = stdInput.readLine();
      line = stdInput.readLine();
      String interval = stdInput.readLine();
      line = stdInput.readLine();
      res[0]=new Float(line.substring(20,line.length()).trim());
      line = stdInput.readLine();
      res[1]=new Float(line.substring(20,line.length()).trim());
      if (res[1].floatValue()<0 || res[1].floatValue()>100) {
        System.err.println("WH1081: Invalid data..");
        throw new Exception("Outside humidity out of range");
      }
      line = stdInput.readLine();
      res[2]=new Float(line.substring(20,line.length()).trim());
      //Check for invalid values
      if (res[2].floatValue()>80 || res[2].floatValue()<-20) {
        System.err.println("WH1081: Invalid data..");
        throw new Exception("Inside temperature out of range");
      }
      line = stdInput.readLine();
      res[3]=new Float(line.substring(20,line.length()).trim());
      //Check for invalid values
      if (res[3].floatValue()>80 || res[3].floatValue()<-20) {
        System.err.println("WH1081: Invalid data..");
        throw new Exception("Outside temperature out of range");
      }
      line = stdInput.readLine();
      res[4]=new Float(Float.parseFloat(line.substring(20,line.length()).trim())*3.6);
      line = stdInput.readLine();
      res[5]=new Float(Float.parseFloat(line.substring(20,line.length()).trim())*3.6);
      //If avg wind exceeds gust then the message is corrupted
      if (res[4].floatValue()>res[5].floatValue() ||
          res[4].floatValue()>162 || res[5].floatValue()>162) {
        System.err.println("WH1081: Invalid data..");
        throw new Exception("Wind data is invalid");        
      }
      line = stdInput.readLine();
      String wind_dir_temp=line.substring(20,line.length()).trim();
      double wdir;
      if (wind_dir_temp.equals("N")) {
        wdir=0;
      } else if (wind_dir_temp.equals("NNE")) {
        wdir=22.5;
      } else if (wind_dir_temp.equals("NE")) {
        wdir=45.0;
      } else if (wind_dir_temp.equals("ENE")) {
        wdir=67.5;
      } else if (wind_dir_temp.equals("E")) {
        wdir=90.0;
      } else if (wind_dir_temp.equals("ESE")) {
        wdir=112.5;
      } else if (wind_dir_temp.equals("SE")) {
        wdir=135.0;
      } else if (wind_dir_temp.equals("SSE")) {
        wdir=157.5;
      } else if (wind_dir_temp.equals("S")) {
        wdir=180.0;
      } else if (wind_dir_temp.equals("SSW")) {
        wdir=202.5;
      } else if (wind_dir_temp.equals("SW")) {
        wdir=225.0;
      } else if (wind_dir_temp.equals("WSW")) {
        wdir=247.5;
      } else if (wind_dir_temp.equals("W")) {
        wdir=270.0;
      } else if (wind_dir_temp.equals("WNW")) {
        wdir=295.2;
      } else if (wind_dir_temp.equals("NW")) {
        wdir=315.0;
      } else {
        wdir=337.5;
      }
      res[6]=new Float(wdir);
      line = stdInput.readLine();
      line = stdInput.readLine();
      Float thisrain=new Float(line.substring(20,line.length()).trim());
      line = stdInput.readLine();
      line = stdInput.readLine();
      line = stdInput.readLine();
      //Pressure
      res[7]=new Float(line.substring(20,line.length()).trim());

      String history = stdInput.readLine();
      //Check to make sure this is not the same data we have seen before
      //We need to compare data as well as the interval and history position
      //because sometimes they don't change even though the data does
      boolean datachanged=false;
      if (itsLastData==null) {
        datachanged=true;
      } else {
        //Compare all elements except rain
        for (int i=0; i<theirNumElements; i++) {
          if (i==theirRainElement) {
            continue;
          }
          if (itsLastData[i].floatValue()!=res[i].floatValue()) {
            datachanged=true;
            break;
          }
        }
      }
      if (datachanged || 
          itsLastInterval==null || !interval.equals(itsLastInterval) ||
          itsLastHistory==null || !history.equals(itsLastHistory))
      {
        //New data
        itsLastInterval=interval;
        itsLastHistory=history;   
      } else {
        //System.err.println("WH1081: repeated data.");
        return null;
      }

      if (itsLastRain==null || thisrain.floatValue()<itsLastRain.floatValue()) {
        //Impossible to tell how much rain since the last reading
        System.err.println("WH1081: Rainfall has reset.");
        res=null;
      } else {
        res[8]=new Float(10*(thisrain.floatValue()-itsLastRain.floatValue()));
      }
      itsLastRain=thisrain;
      itsLastData=res;
    } catch (Exception e) {          
      System.err.println("WH1081: " + e.getClass());
      return null;
    }
    
    return res;
  }
  
  /** Simple test program. */
  public static final void main(String[] args) {
    WH1081 ds=new WH1081(null);
    while (true) {
      Float[] newdata=ds.getNewWeather();
      if (newdata==null) {
        System.out.println("null");
      } else {
        for (int i=0; i<theirNumElements; i++) {
          System.out.print(newdata[i] + " ");
        }
        System.out.println();
      }
      //Sleep for a bit
      try {
        RelTime.factory(30000000l).sleep();
      } catch (Exception e) {}
    }
  }
}
