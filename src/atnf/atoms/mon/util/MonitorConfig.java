// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Simple properties class for configuration settings.
 *
 * @author Le Cuong Nguyen
 */
public class MonitorConfig
{
   private static HashMap<String,String> itsData = new HashMap<String,String>();
   private static boolean itsInit = false;

   /** Read the config file and cache properties. */
   public static void init()
   {
     InputStream configfile = MonitorConfig.class.getClassLoader().getResourceAsStream("monitor-config.txt");
     if (configfile==null) {
       System.err.println("ERROR: Could not find monitor-config.txt configuration file");
       System.exit(1);
     }
     String[] lines = MonitorUtils.parseFile(new InputStreamReader(configfile));
     if (lines != null) {
      for (int i = 0; i < lines.length; i++) {
         int firstSpace = lines[i].indexOf(' ');
         itsData.put(lines[i].substring(0, firstSpace), lines[i].substring(firstSpace+1, lines[i].length()));
       }
    }
   }
   
   /** Return the string value of the named property. */
   public static String getProperty(String prop)
   {
      if (!itsInit) {
        init();
      }
      return (String)itsData.get(prop);
   }
   
   /** Return value of the named property, or <i>null</i> if it wasn't found. */
   public static String getProperty(String prop, String def)
   {
      if (!itsInit) {
        init();
      }
      String res=(String)itsData.get(prop);
      if (res==null) {
        res=def;
      }
      return res;
   }
}
