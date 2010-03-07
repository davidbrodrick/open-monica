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
  private static final int theirNumElements = 11;

  /** The entry in the data array which corresponds to rainfall. */
  private static final int theirRainElement = 8;

  /** The last reading of total rainfall. */
  private Integer itsLastRain = null;

  /** The last valid data we collected. */
  private Number[] itsLastData = null;

  /** Whether the next data collected should be ignored. */
  private boolean itsIgnoreNextData = false;

  public WH1081(String[] args) {
    super("wh1081");
  }

  /** Collect new data for requesting monitor points. */
  protected void getData(PointDescription[] points) throws Exception {
    // Get the actual data
    Number[] newdata = getNewWeather();
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
  protected Number[] getSensorImage() {
    Number[] res = new Number[theirNumElements];

    try {
      // Run wwsr, using 'timeout', and read output and status
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

      String line = stdInput.readLine(); // Interval
      line = stdInput.readLine();
      line = stdInput.readLine();
      res[9] = new Integer(Integer.parseInt(line.substring(20, line.length())
          .trim(), 16));
      line = stdInput.readLine(); // Inside humidity
      res[0] = new Integer(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // Outside humidity
      res[1] = new Integer(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // Inside temp
      res[2] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // Outside temp
      res[3] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // Wind speed
      res[4] = new Float(Float.parseFloat(line.substring(20, line.length())
          .trim()) * 3.6);
      line = stdInput.readLine(); // Wind gust
      res[5] = new Float(Float.parseFloat(line.substring(20, line.length())
          .trim()) * 3.6);
      line = stdInput.readLine(); // Wind direction
      String wind_dir_temp = line.substring(20, line.length()).trim();
      line = stdInput.readLine(); // Rain - unused
      line = stdInput.readLine(); // Rain 2
      res[8] = new Integer(Math.round((new Float(line.substring(20,
          line.length()).trim())).floatValue() * 10));
      line = stdInput.readLine(); // Other 1
      int other1 = Integer.parseInt(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // Other 2
      int other2 = Integer.parseInt(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // Pressure
      res[7] = new Float(line.substring(20, line.length()).trim());
      line = stdInput.readLine(); // History position
      res[10] = new Integer(Integer.parseInt(line.substring(20, line.length())
          .trim(), 16));

      // Check for invalid values
      if (other1 != 0 || other2 != 0) {
        System.err.println("WH1081: No data from remote sensors");
        return null;
      }
      if (res[0].floatValue() < 0 || res[0].floatValue() > 100) {
        System.err.println("WH1081: Inside humidity out of range");
        return null;
      }
      if (res[1].floatValue() < 0 || res[1].floatValue() > 100) {
        System.err.println("WH1081: Outside humidity out of range");
        return null;
      }
      if (res[2].floatValue() > 80 || res[2].floatValue() < -20) {
        System.err.println("WH1081: Inside temperature out of range");
        return null;
      }
      if (res[3].floatValue() > 80 || res[3].floatValue() < -40) {
        System.err.println("WH1081: Outside temperature out of range");
        return null;
      }
      // If avg wind exceeds gust then the message is corrupted
      if (res[4].floatValue() > res[5].floatValue()
          || res[4].floatValue() > 162 || res[5].floatValue() > 162) {
        System.err.println("WH1081: Wind data is invalid");
        return null;
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
        System.err.println("WH1081: Pressure is out of range.");
        return null;
      }
    } catch (Exception e) {
      System.err.println("WH1081.getSensorImage: " + e);
      return null;
    }
    return res;
  }

  /** Execute wwsr and return array of new data, or null if error. */
  protected Number[] getNewWeather() {
    // Ensure we get consistent set of readings twice in a row
    Number[] newdata1 = getSensorImage();
    Number[] newdata2 = getSensorImage();
    if (newdata1 == null || newdata2 == null) {
      System.err.println("WH1081: Invalid data returned by wwsr");
      itsIgnoreNextData = true;
      return null;
    }
    for (int i = 0; i < theirNumElements; i++) {
      if (newdata1[i].floatValue() != newdata2[i].floatValue()) {
        System.err.println("WH1081: Inconsistent data returned by wwsr");
        // itsIgnoreNextData = true;
        return null;
      }
    }

    // Ensure this isn't the same data we have already processed
    if (itsLastData != null) {
      boolean fieldchanged = false;
      for (int i = 0; i < theirNumElements; i++) {
        if (i!=theirRainElement && newdata1[i].floatValue() != itsLastData[i].floatValue()) {
          fieldchanged = true;
          break;
        }
      }
      if (!fieldchanged) {
        //System.err.println("WH1081: Repeated data");
        return null;
      }
    }

    // New data, keep a reference for comparison next time
    itsLastData = newdata1;

    System.err.print("WH1081: pre: " + (new AbsTime()).toString(AbsTime.Format.UTC_STRING) + " ");
    for (int i = 0; i < newdata1.length; i++) {
      System.err.print(newdata1[i] + " ");
    }
    System.err.println();
    
    
    // Check if we have reason to think this data is suspicious
    if (itsIgnoreNextData) {
      System.err.println("WH1081: Was told to ignore this data");
      itsIgnoreNextData = false;
      return null;
    }

    int temprain = newdata1[theirRainElement].intValue();
    if (itsLastRain == null
        || newdata1[theirRainElement].intValue() < itsLastRain.intValue()) {
      // Impossible to tell how much rain since the last reading
      System.err.println("WH1081: Rainfall has reset..");
      newdata1 = null;
    } else {
      newdata1[theirRainElement] = newdata1[theirRainElement].intValue()
          - itsLastRain.intValue();
    }
    itsLastRain = temprain;

    if (newdata1 != null) {
      System.err.print("WH1081: ");
      for (int i = 0; i < newdata1.length; i++) {
        System.err.print(newdata1[i] + " ");
      }
      System.err.println();
    }

    return newdata1;
  }

  /** Simple test program. */
  public static final void main(String[] args) {
    WH1081 ds = new WH1081(null);
    while (true) {
      Number[] newdata = ds.getNewWeather();
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
