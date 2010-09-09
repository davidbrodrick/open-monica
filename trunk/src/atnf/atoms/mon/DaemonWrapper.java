// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import org.apache.commons.daemon.*;

import org.apache.log4j.Logger;

/**
 * Wrapper to support starting and stopping MoniCA as an Apache Commons Daemon.
 * 
 * @author David Brodrick
 */
public class DaemonWrapper implements Daemon
{
  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(DaemonWrapper.class.getName());

  /** Start the operation of this Daemon instance. */
  public void start()
  {
    // Start the system
    if (MoniCAMain.start()) {
      // Open the network server interfaces
      MoniCAMain.openInterfaces();
    }
  }

  /** Stop the operation of this Daemon instance. */
  public void stop()
  {
    MoniCAMain.stop();
  }

  public void destroy()
  {
  }

  public void init(DaemonContext context)
  {
  }  

  /** Stop the operation of this Daemon instance. */
  public static void staticStop(String[] args)
  {
    DaemonWrapper d = new DaemonWrapper();
    d.stop();    
  }  
  
  /** Start the operation of this Daemon instance. */
  public static final void main(String[] args)
  {
    DaemonWrapper d = new DaemonWrapper();
    d.start();
  }
}
