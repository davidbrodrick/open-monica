//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Integrate every update of the input and reset once per day at the specified
 * time in the specified timezone.<BR>
 *
 * For instance, this was initially implemented for the Parkes Telescope
 * weather station, which publishes the rainfall over the last 20 seconds.
 * This Translation is able to accumulate the rain tips over the course of
 * the day and reset the rain gauge at 9:01 local time.
 *
 * <P>The two init arguments for this are <tt>"09:01""Australia/Sydney"</tt>
 *
 * @author David Brodrick
 */
public class
TranslationDailyIntegrator
extends Translation
{
  /** The hour to reset. */
  protected int itsHour = 0;
  /** The minute to reset. */
  protected int itsMinute = 0;
  /** The day of month when we last reset. */
  protected int itsLastReset = -1;
  
  /** The timezone in which the reset time is to be calculated. */
  protected TimeZone itsTZ = TimeZone.getTimeZone("UTC");

  /** The accumulated input for the day so far. */
  protected double itsSum = 0.0;
  
  protected static String[] itsArgs = new String[]{
      "Translation Daily Integrator", "DailyIntegrator",
      "Time Range", "java.lang.String"};

  public TranslationDailyIntegrator(PointDescription parent, String[] init)
  {
    super(parent, init);

    if (init.length!=2) {
      System.err.println("TranslationDailyIntegrator: " + parent.getName() +
                         ": NEED TWO INIT ARGUMENTS!");
    } else {
      //First argument is time
      int colon=init[0].indexOf(":");
      if (colon==-1 || init[0].length()>5) {
        System.err.println("TranslationDailyIntegrator: " + parent.getName() +
                           ": NEED TIME IN HH:MM 24-HOUR FORMAT!");
      } else {
        try {
          itsHour=Integer.parseInt(init[0].substring(0,colon));
          itsMinute=Integer.parseInt(init[0].substring(colon+1,init[0].length()));
        } catch (Exception e) {
          System.err.println("TranslationDailyIntegrator: " + parent.getName() +
                             ": NEED TIME IN HH:MM 24-HOUR FORMAT!");
        }
      }
      //TimeZone is second argument
      itsTZ=TimeZone.getTimeZone(init[1]);
      if (itsTZ==null) {
        System.err.println("TranslationDailyIntegrator: " + parent.getName() +
                           ": UNKNOWN TIMEZONE \"" + init[1] + "\"");
      }
    }
  }


  /** Calculate the average and return an averaged value. */
  public
  PointData
  translate(PointData data)
  {
    //Extract the numeric value from this new input
    double thisvalue=0.0;
    if (data!=null && data.getData()!=null) {
      if (data.getData() instanceof Number) {
        thisvalue=((Number)(data.getData())).doubleValue();
      } else {
        System.err.println("TranslationDailyIntegrator: " + itsParent.getName() +
                           ": REQUIRE NUMERIC INPUT!");
      }
    }
    
    //Check if it is time to reset the integrator
    Calendar c = Calendar.getInstance(itsTZ);
    if (c.get(Calendar.DAY_OF_YEAR)!=itsLastReset &&
        c.get(Calendar.HOUR_OF_DAY)>itsHour ||
        c.get(Calendar.HOUR_OF_DAY)==itsHour && c.get(Calendar.MINUTE)>=itsMinute)
    {
      //Yep, we need to reset. This lastest update counts towards new sum
      itsLastReset=c.get(Calendar.DAY_OF_YEAR);
      itsSum=thisvalue;
    } else {
      //Not time to reset, so accumulate this value
      itsSum+=thisvalue;
    }

    //Return the integrated sum
    return new PointData(itsParent.getName()+"."+itsParent.getSource(),
                         new AbsTime(), new Double(itsSum));
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
