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
  /** Device name. */
  private String itsDevice = "Logger";
  
  public Logger() {
  }

  public Logger(String devicename) {
    itsDevice=devicename;
  }

  public void checkpoint() {
    System.err.println(itsDevice + ": Checkpoint");
  }

  public void checkpoint(String message) {
    System.err.println(itsDevice + ": Checkpoint: " + message);
  }

  public void debug() {
    System.err.println(itsDevice + ": Debug");
  }

  public void debug(String message) {
    System.err.println(itsDevice + ": Debug: " + message);
  }

  public void error() {
    System.err.println(itsDevice + ": Error");    
  }

  public void error(String message) {
    System.err.println(itsDevice + ": Error: " + message);
  }

  public void fatal() {
    System.err.println(itsDevice + ": Fatal");    
  }

  public void fatal(String message) {
    System.err.println(itsDevice + ": Fatal: " + message);
  }

  public void information() {
    System.err.println(itsDevice + ": Information");
  }

  public void information(String message) {
    System.err.println(itsDevice + ": Information: " + message);    
  }

  public void warning() {
    System.err.println(itsDevice + ": Warning");
  }

  public void warning(String message) {
    System.err.println(itsDevice + ": Warning: " + message);
  }

  public String getDeviceName() {
    return itsDevice;
  }

  public void setDeviceName(String name) {
    itsDevice = name;
  }

  public String getVerboseMode() {
    return "";
  }

  public void setVerboseMode(String name) {
  }
}
