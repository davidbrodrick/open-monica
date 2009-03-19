// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.util;

/** Fake logger class that doesn't send messages anywhere. */
public class Logger
{
   public Logger() {}
   public Logger(String devicename) {}
   
   public void checkpoint() {}
   public void checkpoint(String message) {}

   public void debug() {}
   public void debug(String message) {}

   public void error() {}
   public void error(String message) {}

   public void fatal() {}
   public void fatal(String message) {}

   public void information() {}
   public void information(String message) {}

   public void warning() {}
   public void warning(String message) {}

   public String getDeviceName() {return "";}
   public void setDeviceName(String name) {}
   public String getVerboseMode() {return "";}
   public void setVerboseMode(String name) {}
}
