//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * A simple Crontab implementation which can be used with CronPulse
 * adapted, solidified and improved from public domain code originally by pwalther
 * 
 * @author Balt Indermuehle
 */
public class Crontab {

    static final protected int MINUTESPERHOUR = 60;
    static final protected int HOURESPERDAY = 24;
    static final protected int DAYSPERWEEK = 7;
    static final protected int MONTHSPERYEAR = 12;
    static final protected int DAYSPERMONTH = 31;
    
    private Crontab() {  }
    
    private HashMap Minutes = new HashMap();
    private HashMap Hours = new HashMap();
    private HashMap DaysInMonth = new HashMap();
    private HashMap Month = new HashMap();
    private HashMap DaysInWeek = new HashMap();
    private String configLine = "";

/*
*  Constructor
*/
    public Crontab(String line) {
      try {
        configLine = line;
        // substitute day numbers rather than day names:        
        configLine = configLine.toUpperCase();
        configLine = configLine.replaceAll("SUN", "0");
        configLine = configLine.replaceAll("MON", "1");
        configLine = configLine.replaceAll("TUE", "2");
        configLine = configLine.replaceAll("WED", "3");
        configLine = configLine.replaceAll("THU", "4");
        configLine = configLine.replaceAll("FRI", "5");
        configLine = configLine.replaceAll("SAT", "6");
        
        String [] params = configLine.split(" ");
        Minutes = parseRangeParam(params[0], MINUTESPERHOUR, 0);
        Hours = parseRangeParam(params[1], HOURESPERDAY, 0);
        DaysInMonth = parseRangeParam(params[2], DAYSPERMONTH, 1);
        Month = parseRangeParam(params[3], MONTHSPERYEAR, 1); // 1 = january
        DaysInWeek = parseRangeParam(params[4], DAYSPERWEEK, 0);
      } catch (Exception e) {
        throw new IllegalArgumentException("Something bad happened in the Crontab constructor. Here's what the JVM thinks: " + e);
      }
        
    }

/*
* Parse the parameters and determine whether there are any repeats or ranges in them, and map them to a timeline
*/
    private static HashMap<String,String> parseRangeParam(String param, int timelength, int minlength) {
        //System.out.println(param);
        
        String [] paramarray;
        if(param.indexOf(",") != -1) {
            paramarray = param.split(",");
        } else {
            paramarray = new String [] {param};
        }
        StringBuffer rangeitems = new StringBuffer();
        for(int i = 0; i < paramarray.length; i++) {
            // you may mix */# syntax with other range syntax
            if(paramarray[i].indexOf("/") != -1) {
                // handle */# syntax
                for(int a = 1; a <= timelength; a++) {
                    if(a % Integer.parseInt(paramarray[i].substring(paramarray[i].indexOf("/")+1)) == 0) {
                        if(a == timelength) {
                            rangeitems.append(minlength + ",");
                        } else {
                            rangeitems.append(a + ",");
                        }
                    }
                }
            } else {
                if(paramarray[i].equals("X")) {
                    rangeitems.append(fillRange(minlength + "-" + timelength));
                } else {
                    rangeitems.append(fillRange(paramarray[i]));
                }
            }
        }
        String [] values = rangeitems.toString().split(",");
        HashMap<String,String> result = new HashMap<String, String>();
        for(int i = 0; i < values.length; i++) {
            result.put(values[i], values[i]);
        }
        
        return result;
        
    }
    
    private static String fillRange(String range) {
        // split by "-"
        
        if(range.indexOf("-") == -1) {
            return range + ",";
        }
        
        String [] rangearray = range.split("-");
        StringBuffer result = new StringBuffer();
        for(int i = Integer.parseInt(rangearray[0]); i <= Integer.parseInt(rangearray[1]); i++) {
            result.append(i + ",");
        }
        return result.toString();
    }
    
    public boolean RunAt(Calendar cal) {
        int month = cal.get(Calendar.MONTH) + 1;           // 1=Jan, 2=Feb etc
        int day = cal.get(Calendar.DAY_OF_MONTH);          // Starts from 1
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) -1 ; // 0=Sunday, 1=Monday
        int hour = cal.get(Calendar.HOUR_OF_DAY);	         // 0-23
        int minute = cal.get(Calendar.MINUTE);             // 0-59
        
        if(Minutes.get(Integer.toString(minute))!= null) {
        	if(Hours.get(Integer.toString(hour))!= null) {
        		if(DaysInMonth.get(Integer.toString(day))!= null) {
        			if(Month.get(Integer.toString(month))!= null) {
        				if(DaysInWeek.get(Integer.toString(dayOfWeek))!= null) {
        		        	return true;
        		        }
        	        }	
                }	
            }	
        }
        return false;
    }
    
    /*
    * Helper function to determine whether NOW is a time to run
    */
    public boolean RunNow(TimeZone TZ) {
    	return RunAt(new GregorianCalendar(TZ)); 
    }
    
    
    
}