// Copyright (C) Oz Forecast, NSW, Australia.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.io.*;
import atnf.atoms.time.*;
import atnf.atoms.mon.*;

/**
 * WH1081 SinoMeter el-cheapo weather station driver.
 * 
 * <P>
 * Parses the output of wwsr, so wwsr should be installed SUID:
 * http://www.pendec.dk/weatherstation.htm
 * 
 * <P>
 * Uses the <i>timeout</i> program when invoking wwsr, so that is also
 * required.
 * 
 * <P>
 * Each data point associated with this source is given an array of the latest
 * values which contains: <bl>
 * <li>Inside humidity (%)
 * <li>Outside humidity (%)
 * <li>Inside temp (degrees C)
 * <li>Outside temp (degrees C)
 * <li>Avg wind speed (km/h)
 * <li>Wind gust speed (km/h)
 * <li>Wind direction (degrees of azimuth)
 * <li>Pressure (hPa)
 * <li>Rain since last reading (tips) </bl>
 * 
 * @author David Brodrick
 * @version $Id: $
 */
public class WH1081 extends ExternalSystem {
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

  /** Whether the next data collected should be ignored. */
  private boolean itsIgnoreNextData = false;

  public WH1081(String[] args) {
    super("wh1081");
  }

  /** Collect new data for requesting monitor points. */
  protected void getData(PointDescription[] points) throws Exception {
    // Get the actual data
    Float[] newdata = getNewWeather();
    if (newdata == null)
      return;

    // Fire new data to each point
    for (int i = 0; i < points.length; i++) {
      PointDescription pm = points[i];
      PointData pd = new PointData(pm.getFullName(), newdata);
      PointEvent pe = new PointEvent(pm, pd, true);
      pm.firePointEvent(pe);
    }
  }

  /** Execute wwsr and return array of new data, or null if error. */
  protected Float[] getNewWeather() {
    Float[] res = new Float[theirNumElements];

    try {
      Process p = Runtime.getRuntime().exec("timeout 10 wwsr -a");

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
          .getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(p
          .getErrorStream()));

      p.waitFor();

      String err = stdError.readLine();
      if (err != null || p.exitValue() != 0) {
        System.err.println("WH1081: Error running \"wwsr\"");
        RelTime.factory(1000000).sleep();
        itsIgnoreNextData = true;
        return null;
      }

      String line = stdInput.readLine();
      line = stdInput.readLine();
      String interval = stdInput.readLine();
      line = stdInput.readLine();
      res[0] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      res[1] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      res[2] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      res[3] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      res[4] = new Float(Float.parseFloat(line.substring(20, line.length())
          .trim()) * 3.6);
      line = stdInput.readLine();
      res[5] = new Float(Float.parseFloat(line.substring(20, line.length())
          .trim()) * 3.6);
      line = stdInput.readLine();
      String wind_dir_temp = line.substring(20, line.length()).trim();
      line = stdInput.readLine();
      line = stdInput.readLine();
      Float thisrain = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      int other1 = Integer.parseInt(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      int other2 = Integer.parseInt(line.substring(20, line.length()).trim());
      line = stdInput.readLine();
      res[7] = new Float(line.substring(20, line.length()).trim());

      // Check for invalid values
      if (other1 != 0 || other2 != 0) {
        itsIgnoreNextData = true;
        throw new Exception("No data from remote sensors");
      }
      if (res[1].floatValue() < 0 || res[1].floatValue() > 100) {
        itsIgnoreNextData = true;
        throw new Exception("Outside humidity out of range");
      }
      if (res[2].floatValue() > 80 || res[2].floatValue() < -20) {
        itsIgnoreNextData = true;
        throw new Exception("Inside temperature out of range");
      }
      if (res[3].floatValue() > 80 || res[3].floatValue() < -20) {
        itsIgnoreNextData = true;
        throw new Exception("Outside temperature out of range");
      }
      // If avg wind exceeds gust then the message is corrupted
      if (res[4].floatValue() > res[5].floatValue()
          || res[4].floatValue() > 162 || res[5].floatValue() > 162) {
        itsIgnoreNextData = true;
        throw new Exception("Wind data is invalid");
      }
      double wdir;
      if (wind_dir_temp.equals("N")) {
        wdir = 0;
      } else if (wind_dir_temp.equals("NNE")) {
        wdir = 22.5;
      } else if (wind_dir_temp.equals("NE")) {
        wdir = 45.0;
      } else if (wind_dir_temp.equals("ENE")) {
        wdir = 67.5;
      } else if (wind_dir_temp.equals("E")) {
        wdir = 90.0;
      } else if (wind_dir_temp.equals("ESE")) {
        wdir = 112.5;
      } else if (wind_dir_temp.equals("SE")) {
        wdir = 135.0;
      } else if (wind_dir_temp.equals("SSE")) {
        wdir = 157.5;
      } else if (wind_dir_temp.equals("S")) {
        wdir = 180.0;
      } else if (wind_dir_temp.equals("SSW")) {
        wdir = 202.5;
      } else if (wind_dir_temp.equals("SW")) {
        wdir = 225.0;
      } else if (wind_dir_temp.equals("WSW")) {
        wdir = 247.5;
      } else if (wind_dir_temp.equals("W")) {
        wdir = 270.0;
      } else if (wind_dir_temp.equals("WNW")) {
        wdir = 295.2;
      } else if (wind_dir_temp.equals("NW")) {
        wdir = 315.0;
      } else {
        wdir = 337.5;
      }
      res[6] = new Float(wdir);
      if (res[7].floatValue() == 0.0f) {
        itsIgnoreNextData = true;
        throw new Exception("Pressure is out of range.");
      }

      String history = stdInput.readLine();
      // Check to make sure this is not the same data we have seen before
      // We need to compare data as well as the interval and history position
      // because sometimes they don't change even though the data does
      boolean datachanged = false;
      if (itsLastData == null) {
        datachanged = true;
      } else {
        // Compare all elements except rain
        for (int i = 0; i < theirNumElements; i++) {
          if (i == theirRainElement) {
            continue;
          }
          if (itsLastData[i].floatValue() != res[i].floatValue()) {
            datachanged = true;
            break;
          }
        }
      }
      if (datachanged || itsLastInterval == null
          || !interval.equals(itsLastInterval) || itsLastHistory == null
          || !history.equals(itsLastHistory)) {
        // New data
        itsLastInterval = interval;
        itsLastHistory = history;
        // System.err.println("WH1081: New data " + datachanged + " " + interval
        // + " " + history);
      } else {
        // System.err.println("WH1081: repeated data.");
        return null;
      }

      // Keep a record to this data
      itsLastData = res;

      if (itsIgnoreNextData) {
        System.err.println("WH1081: Was told to ignore this data");
        itsIgnoreNextData = false;
        return null;
      }

      if (itsLastRain == null
          || thisrain.floatValue() < itsLastRain.floatValue()) {
        // Impossible to tell how much rain since the last reading
        System.err.println("WH1081: Rainfall has reset..");
        res = null;
      } else {
        res[8] = new Float(10 * (thisrain.doubleValue() - itsLastRain
            .doubleValue()));
        System.err.println("WH1081: Rainfall "
            + (new AbsTime()).toString(AbsTime.Format.UTC_STRING) + " "
            + res[8] + " " + thisrain.floatValue());
      }
      itsLastRain = thisrain;
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("WH1081: " + e);
      return null;
    }

    if (res == null) {
      System.err.println("WH1081: res=null");
    } else {
      System.err.print("WH1081: ");
      for (int i = 0; i < res.length; i++) {
        System.err.print(res[i] + " ");
      }
      System.err.println();
    }

    return res;
  }

  /** Simple test program. */
  public static final void main(String[] args) {
    WH1081 ds = new WH1081(null);
    while (true) {
      Float[] newdata = ds.getNewWeather();
      if (newdata == null) {
        System.out.println("No Data");
      } else {
        for (int i = 0; i < theirNumElements; i++) {
          System.out.print(newdata[i] + " ");
        }
        System.out.println();
      }
      // Sleep for a bit
      try {
        RelTime.factory(30000000l).sleep();
      } catch (Exception e) {
      }
    }
  }
}
