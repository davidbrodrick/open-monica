// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.util;

import java.io.*;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Simple properties class for configuration settings.
 * 
 * @author Le Cuong Nguyen
 */
public class MonitorConfig {
  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(MonitorConfig.class);

  /** Dictionary of property/value pairs. */
  private static HashMap<String, String> itsData = new HashMap<String, String>();

  /** Records if the confile file has already been parsed. */
  private static boolean theirIsInitialised = false;

  /** Read the config file and cache properties. */
  public static void init() {
    InputStream configfile = null;

    String confname = System.getProperty("MoniCA.ConfFile");
    if (confname != null) {
      // Config file name was specified via a property, so use that one
      theirLogger.info("Configuration file \"" + confname + "\" specified via property");
      try {
        configfile = new FileInputStream(confname);
      } catch (Exception e) {
        theirLogger.fatal("While trying to open configuration file: " + e);
        System.exit(1);
      }
    } else {
      // Use config file from the jar
      final String CONFRESNAME = "monitor-config.txt";
      configfile = MonitorConfig.class.getClassLoader().getResourceAsStream(CONFRESNAME);
      if (configfile == null) {
        theirLogger.fatal("Could not open configuration resource \"" + CONFRESNAME + "\"");
        System.exit(1);
      }
    }

    // Now load the file contents
    String[] lines = MonitorUtils.parseFile(new InputStreamReader(configfile));
    if (lines != null) {
      for (int i = 0; i < lines.length; i++) {
        int firstSpace = lines[i].indexOf(' ');
        itsData.put(lines[i].substring(0, firstSpace), lines[i].substring(firstSpace + 1, lines[i].length()));
      }
    }
    
    theirIsInitialised = true;
  }

  /** Return the string value of the named property. */
  public static String getProperty(String prop) {
    if (!theirIsInitialised) {
      init();
    }
    return itsData.get(prop);
  }

  /** Return value of the named property, or <i>null</i> if it wasn't found. */
  public static String getProperty(String prop, String def) {
    if (!theirIsInitialised) {
      init();
    }
    String res = itsData.get(prop);
    if (res == null) {
      res = def;
    }
    return res;
  }
}
