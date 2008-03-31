//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.mon.util.*;


/**
 * Encapsulates a thread which periodically changes the encryption keys.
 * The period at which the encryption keys are changed is determined by
 * the property <tt>KeyLife</tt>. The keys are updated by calling the
 * <tt>MonitorMap::generateNewKeys</tt> method.
 *
 * @author Le Cuong Nguyen
 * @version $Id: KeyKeeper.java,v 1.2 2004/02/13 05:14:01 bro764 Exp $
 */
class KeyKeeper
implements Runnable
{
  /** Should the thread keep running <tt>True</tt> or stop <tt>False</tt>. */
  protected boolean itsRunning = true;

  /** Create a new KeyKeeper thread. */
  public
  KeyKeeper()
  {
    Thread t = new Thread(this, "Encryption Key Keeper");
    t.start();
  }


  /** Main loop for KeyKeeper thread. */
  public
  void
  run()
  {
    while (itsRunning) {
      MonitorMap.generateNewKeys();
      try {
	synchronized(this) {
	  this.wait(Long.parseLong(MonitorConfig.getProperty("KeyLife")));
	}
      } catch (Exception e) { e.printStackTrace(); }
    }
  }
}
