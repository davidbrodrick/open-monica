// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import atnf.atoms.util.*;

/**
 * Simple properties class for configuration settings.
 *
 * @author Le Cuong Nguyen
 */
public class MonitorConfig
{
   private static HashMap itsData = new HashMap();
   private static boolean itsInit = false;

   public static void init()
   {
     URL configfilename = MonitorConfig.class.getClassLoader().getResource("monitor-config.txt");
     if (configfilename==null) {
       System.err.println("ERROR: Could not find monitor-config.txt configuration file");
       System.exit(1);
     }
     String[] lines = MonitorUtils.parseFile(configfilename.getFile());
     if (lines != null)
     for (int i = 0; i < lines.length; i++) {
       int firstSpace = lines[i].indexOf(' ');
       itsData.put(lines[i].substring(0, firstSpace), lines[i].substring(firstSpace+1, lines[i].length()));
     }
   }
   
   public static String getProperty(String prop)
   {
      if (!itsInit) init();
      return (String)itsData.get(prop);
   }
}
