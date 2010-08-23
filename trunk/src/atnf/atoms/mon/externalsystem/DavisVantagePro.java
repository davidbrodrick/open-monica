// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.externalsystem;

import java.util.HashMap;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;

/**
 * Pulls data from a Davis Vantage Pro or Vantage Pro 2 weather station. Data is published
 * as a HashMap with the following keys defined:
 * 
 * <P>
 * <bl>
 * <li><b>OUTTEMP</b> Outside temperature in degrees Celcius.
 * <li><b>INTEMP</b> Inside temperature in degrees Celcius.
 * <li><b>EXTEMPx</b> Extra temperature sensor in degrees Celcius, x=1-7.
 * <li><b>SOILTEMPx</b> Soil temperature sensor in degrees Celcius, x=1-4.
 * <li><b>LEAFTEMPx</b> Leaf temperature sensor in degrees Celcius, x=1-4.
 * <li><b>OUTHUMID</b> Outside relative humidity as a percentage.
 * <li><b>INHUMID</b> Inside relative humidity as a percentage.
 * <li><b>EXHUMIDx</b> Extra relative humidity sensor as a percentage, x=1-7.
 * <li><b>SOILMOISTx</b> Soil moisture in centibar, x=1-4.
 * <li><b>LEAFWETx</b> Leaf wetness, 0=very dry, 15=very wet, x=1-4.
 * <li><b>RAIN</b> Total number of rainfall tips.
 * <li><b>PRES</b> Atmospheric pressure.
 * <li><b>WINDSPD</b> Wind speed in km/h.
 * <li><b>WINDDIR</b> Wind direction in degrees of azimuth.
 * <li><b>SOLRAD</b> Solar radiation, in Watts per square metre.
 * <li><b>UV</b> Ultravoilet radiation, in UV Index units.
 * <li><b>BATTVOLTS</b> Battery voltage, in Volts. </bl>
 * 
 * <P>
 * The <tt>monitor-sources.txt</tt> file needs the hostname and port number of the TCP
 * server which is connected to the weather station's serial line, eg:
 * 
 * <P>
 * <tt>
 * DavisVantagePro 130.155.177.158:10000
 * </tt>
 * 
 * An basic set of monitor point definitions is shown below, these can be expanded for
 * additional sensors using the definitions above:
 * 
 * <P>
 * <tt>
 * #Full set of weather data read from the weather station
 * hidden.davis.data           "Raw weather data"            ""            ""     site T Generic-"130.155.177.158:10000"  -  -  -  - 1500000  -
 * ##Weather data direct from weather station
 * weather.OutTemp             "Outside Temperature"         "Out Temp"    "C"    site T Listen-"$1.hidden.davis.data"     -  {NV-"OUTTEMP", Mean-"60", NumDecimals-"1"}  -             {TIMER-60}           1500000  -
 * weather.InTemp              "Inside Temperature"          "In Temp"     "C"    site T Listen-"$1.hidden.davis.data"     -  {NV-"INTEMP", Mean-"60", NumDecimals-"1"}   -             {TIMER-60}           1500000  -
 * weather.ExtraTemp1          "Extra Temperature 1"         "Ex Temp 1"   "C"    site T Listen-"$1.hidden.davis.data"     -  {NV-"EXTEMP1", Mean-"60", NumDecimals-"1"}  -             {TIMER-60}           1500000  -
 * weather.SoilTemp1           "Soil Temperature 1"          "Soil Temp1"  "C"    site T Listen-"$1.hidden.davis.data"     -  {NV-"SOILTEMP1", Mean-"60", NumDecimals-"1"}  -           {TIMER-60}           1500000  -
 * weather.LeafTemp1           "Leaf Temperature 1"          "Leaf Temp1"  "C"    site T Listen-"$1.hidden.davis.data"     -  {NV-"LEAFTEMP1", Mean-"60", NumDecimals-"1"}  -           {TIMER-60}           1500000  -
 * weather.SoilMoisture1       "Soil Moisture 1"             "Soil Mst 1"  "cBa"  site T Listen-"$1.hidden.davis.data"     -  {NV-"SOILMOIST1", Mean-"60", NumDecimals-"1"} -           {TIMER-60}           1500000  -
 * weather.LeafWetness1        "Leaf Wetness 1"              "Leaf Wet 1"  ""     site T Listen-"$1.hidden.davis.data"     -  {NV-"LEAFWET1", Mean-"60", NumDecimals-"1"} -             {TIMER-60}           1500000  -
 * weather.OutHumid            "Outside Relative Humidity"   "Out Humid"   "%"    site T Listen-"$1.hidden.davis.data"     -  {NV-"OUTHUMID", Mean-"60", NumDecimals-"1"} -             {TIMER-60}           1500000  -
 * weather.InHumid             "Inside Relative Humidity"    "In Humid"    "%"    site T Listen-"$1.hidden.davis.data"     -  {NV-"INHUMID", Mean-"60", NumDecimals-"1"}  -             {TIMER-60}           1500000  -
 * weather.ExtraHumid1         "Extra Relative Humidity 1"   "Ex Humid 1"  "%"    site T Listen-"$1.hidden.davis.data"     -  {NV-"EXHUMID1", Mean-"60", NumDecimals-"1"} -             {TIMER-60}           1500000  -
 * weather.Pressure            "Atmospheric Pressure"        "Pressure"    "hPa"  site T Listen-"$1.hidden.davis.data"     -  {NV-"PRES", Mean-"60", NumDecimals-"1"}     -             {TIMER-180}          1500000  -
 * weather.SolarRad            "Solar Radiation"             "Solar Rad"  "W/m^2" site T Listen-"$1.hidden.davis.data"     -  {NV-"SOLRAD", NumDecimals-"1"}              -             {TIMER-180}          1500000  -
 * weather.UV                  "Ultraviolet Radiation"       "UV Rad"      ""     site T Listen-"$1.hidden.davis.data"     -  {NV-"UV", NumDecimals-"1"}                  -             {TIMER-180}          1500000  -
 * weather.Battery             "Battery Voltage"             "Batt Volts"  "V"    site T Listen-"$1.hidden.davis.data"     -  {NV-"BATTVOLTS"}                            -             {TIMER-60}           1500000  -
 * weather.TxBattery           "Transmitter Battery Status"  "Tx Batt"     ""     site T Listen-"$1.hidden.davis.data"     -  {NV-"TXBATT"}                               -             {CHANGE-, TIMER-180} 1500000  - 
 * weather.Wind                "Wind Speed"                  "Wind Spd"    "km/h" site T Listen-"$1.hidden.davis.data"     -  {NV-"WINDSPD"}                              Range-"0""45" {COUNTER-1}          1500000  2 
 * #NB: Need to set the angle correction for your wind vane here, 0 by default
 * weather.WindDir             "Wind Direction"              "Wind Dir"    "deg"  site T Listen-"$1.hidden.davis.data"     -  {NV-"WINDDIR", EQ-"(x+0)%360"}              -             {COUNTER-1}          1500000  2
 * weather.RainTotalTips       "Total Ever Rainfall Tips"    "Total Tips"  "tips" site T Listen-"$1.hidden.davis.data"     -  {NV-"RAIN"}                                 -             {CHANGE-, TIMER-300} 1500000  -
 * 
 * ##Derived weather data
 * #NB: Need to set the appropriate pressure offset for your alitude here, 0 by default
 * weather.PressureMSL         "Mean Sea Level Pressure"     "Pressure"    "hPa"  site T Listen-"$1.weather.Pressure"      -  {EQ-"x+0", NumDecimals-"1"}                 -             {TIMER-180}          1500000  -
 * #NB: Need to set your timezone here
 * weather.RainTips            "Rainfall Tips Since 9am"     "Rain Tips"   "tips" site T Listen-"$1.weather.RainTotalTips" -  {Delta-, EQ-"-x", DailyIntegratorPosOnly-"09:01""Australia/Sydney"} - -        1500000  -
 * #NB: Need to set the rain per tip scale for your rain sensor here, 0.254mm/tip by default
 * weather.Rain                "Rainfall Since 9am"          "Rain"        "mm"   site T Listen-"$1.weather.RainTips"      -  {EQ-"x*0.254"}                             -              {CHANGE-, TIMER-180} 1500000  -
 * weather.WindMax15m          "Wind Max Gust (15m)"         "Max Gust"    "km/h" site T Listen-"$1.weather.Wind"          -  {PeakDetect-"900", NumDecimals-"1"}        Range-"0""45"  {TIMER-300}          1500000  -
 * weather.WindAvg15m          "Wind Average (15m)"          "Avg Wind"    "km/h" site T Listen-"$1.weather.Wind"          -  {Mean-"900", NumDecimals-"1"}              Range-"0""35"  {TIMER-300}          1500000  -
 * hidden.davis.Wind_x         "Wind EW Component"           "Wind EW"     "km/h" site T Listen-"$1.weather.Wind""$1.weather.WindDir"             -  {Polar2X-"$1.weather.Wind""$1.weather.WindDir""d", Mean-"900"} - - 1500000  -
 * hidden.davis.Wind_y         "Wind NS Component"           "Wind NS"     "km/h" site T Listen-"$1.weather.Wind""$1.weather.WindDir              -  {Polar2Y-"$1.weather.Wind""$1.weather.WindDir""d", Mean-"900"} - - 1500000  -
 * weather.WindDir15m          "Wind Direction (15m)"        "Avg Dir"     "deg"  site T Listen-"$1.hidden.davis.Wind_x""$1.hidden.davis.Wind_y"  -  {XY2Angle-"$1.hidden.davis.Wind_x""$1.hidden.davis.Wind_y""d", NumDecimals-"1"} - {TIMER-300} 1500000  -
 * weather.VapourPressure      "Water Vapour Pressure"       "Vap Pres"    "hPa"  site T Listen-"$1.weather.OutTemp""$1.weather.OutHumid"         -  {VapourPressure-"$1.weather.OutTemp""$1.weather.OutHumid"}                      - {TIMER-300} 1500000  -
 * weather.DewPoint            "Dew Point Temperature"       "Dew Point"   "C"    site T Listen-"$1.weather.VapourPressure"                       -  {DewPoint-}                                                                     - {TIMER-300} 1500000  -
 * weather.SpecificHumidity    "Specific Humidity"           "Spec Humid"  "g/kg" site T Listen-"$1.weather.VapourPressure""$1.weather.Pressure"  -  {SpecificHumidity-"$1.weather.VapourPressure""$1.weather.Pressure"}             - {TIMER-300} 1500000  -
 * weather.PrecipitableWater   "Precipitable Water"          "Precip H2O"  "mm"   site T Listen-"$1.weather.OutTemp""$1.weather.OutHumid"         -  {PrecipitableWater-"$1.weather.OutTemp""$1.weather.OutHumid"}                   - {TIMER-300} 1500000  -
 * </tt>
 * 
 * @author David Brodrick
 */
public class DavisVantagePro extends DataSocket
{
    /** Argument must include host:port and optionally :timeout_ms */
    public DavisVantagePro(String[] args)
    {
        super(args);
    }

    /** Decode weather from the raw bytes and populate a HashMap with the data values. */
    protected HashMap parseSensorImage(int[] rawbytes)
    {
        HashMap<String, Number> res = new HashMap<String, Number>();

        // Check for valid Start Of Block
        if (rawbytes[0] != 1)
            return null;

        int temp;
        // Temperatures
        temp = rawbytes[10] + 256 * rawbytes[11];
        res.put("INTEMP", new Float(5 * ((temp / 10.0) - 32) / 9.0));
        temp = rawbytes[13] + 256 * rawbytes[14];
        res.put("OUTTEMP", new Float(5 * ((temp / 10.0) - 32) / 9.0));
        res.put("EXTEMP1", new Float(5 * ((rawbytes[19] - 90) - 32) / 9.0));
        res.put("EXTEMP2", new Float(5 * ((rawbytes[20] - 90) - 32) / 9.0));
        res.put("EXTEMP3", new Float(5 * ((rawbytes[21] - 90) - 32) / 9.0));
        res.put("EXTEMP4", new Float(5 * ((rawbytes[22] - 90) - 32) / 9.0));
        res.put("EXTEMP5", new Float(5 * ((rawbytes[23] - 90) - 32) / 9.0));
        res.put("EXTEMP6", new Float(5 * ((rawbytes[24] - 90) - 32) / 9.0));
        res.put("EXTEMP7", new Float(5 * ((rawbytes[25] - 90) - 32) / 9.0));
        res.put("SOILTEMP1", new Float(5 * ((rawbytes[26] - 90) - 32) / 9.0));
        res.put("SOILTEMP2", new Float(5 * ((rawbytes[27] - 90) - 32) / 9.0));
        res.put("SOILTEMP3", new Float(5 * ((rawbytes[28] - 90) - 32) / 9.0));
        res.put("SOILTEMP4", new Float(5 * ((rawbytes[29] - 90) - 32) / 9.0));
        res.put("LEAFTEMP1", new Float(5 * ((rawbytes[30] - 90) - 32) / 9.0));
        res.put("LEAFTEMP2", new Float(5 * ((rawbytes[31] - 90) - 32) / 9.0));
        res.put("LEAFTEMP3", new Float(5 * ((rawbytes[32] - 90) - 32) / 9.0));
        res.put("LEAFTEMP4", new Float(5 * ((rawbytes[33] - 90) - 32) / 9.0));

        // Soil moistures, in centibar
        res.put("SOILMOIST1", new Integer(rawbytes[63]));
        res.put("SOILMOIST2", new Integer(rawbytes[64]));
        res.put("SOILMOIST3", new Integer(rawbytes[65]));
        res.put("SOILMOIST4", new Integer(rawbytes[66]));

        // Leaf wetness, 0=very dry, 15=very wet
        res.put("LEAFWET1", new Integer(rawbytes[67]));
        res.put("LEAFWET2", new Integer(rawbytes[68]));
        res.put("LEAFWET3", new Integer(rawbytes[69]));
        res.put("LEAFWET4", new Integer(rawbytes[70]));

        // Transmitter battery status
        res.put("TXBATT", new Integer(rawbytes[87]));

        // Battery voltage
        temp = rawbytes[88] + 256 * rawbytes[89];
        res.put("BATTVOLTS", new Float(((temp * 300) / 512) / 100));

        // Wind readings
        res.put("WINDSPD", new Float(rawbytes[15] * 1.61));
        temp = rawbytes[17] + 256 * rawbytes[18];
        res.put("WINDDIR", new Float(temp));

        // Barometric pressure
        temp = rawbytes[8] + 256 * rawbytes[9];
        res.put("PRES", new Float(temp / 29.5287));

        // Humidity
        res.put("INHUMID", new Integer(rawbytes[12]));
        res.put("OUTHUMID", new Integer(rawbytes[34]));
        res.put("EXHUMID1", new Integer(rawbytes[35]));
        res.put("EXHUMID2", new Integer(rawbytes[36]));
        res.put("EXHUMID3", new Integer(rawbytes[37]));
        res.put("EXHUMID4", new Integer(rawbytes[38]));
        res.put("EXHUMID5", new Integer(rawbytes[39]));
        res.put("EXHUMID6", new Integer(rawbytes[40]));
        res.put("EXHUMID7", new Integer(rawbytes[41]));

        // Rain
        temp = rawbytes[55] + 256 * rawbytes[56];
        res.put("RAIN", new Integer(temp)); // In tips

        // Solar radiation
        temp = rawbytes[45] + 256 * rawbytes[46];
        res.put("SOLRAD", new Integer(temp)); // In Watts per square metre

        // UV
        temp = rawbytes[44];
        res.put("UV", new Integer(temp)); // In UV Index

        return res;
    }

    /** Collect data and fire events to queued monitor points. */
    protected void getData(PointDescription[] points) throws Exception
    {
        try {
            for (int i = 0; i < points.length; i++) {
                PointDescription pm = points[i];

                // Purge the read buffer
                int numpurge = itsReader.available();
                for (int j = 0; j < numpurge; j++) {
                    itsReader.read();
                }

                // Request the image
                itsWriter.write("LOOP".getBytes());
                itsWriter.write((byte) 0xFF);
                itsWriter.write((byte) 0xFF);
                itsWriter.write('\r');
                itsWriter.flush();
                if (itsReader.read() != 6) {
                    // Did not receive ACK for LOOP command
                    pm.firePointEvent(new PointEvent(this, new PointData(pm.getFullName()), true));
                    continue;
                }

                // Read the sensor image bytes
                int[] rawbytes = new int[91];
                for (int j = 0; j < rawbytes.length; j++) {
                    rawbytes[j] = itsReader.read();
                    if (rawbytes[j] == -1) {
                        throw new Exception("Reached EOF while reading from socket");
                    }
                }

                // Parse the weather data
                HashMap wxdata = parseSensorImage(rawbytes);

                // Count successful transactions
                if (wxdata != null) {
                    itsNumTransactions++;
                }

                // Fire the new data off for this point
                pm.firePointEvent(new PointEvent(this, new PointData(pm.getFullName(), wxdata), true));
            }
        } catch (Exception e) {
            disconnect();
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.error("In getData method: " + e);
        }
    }
}
