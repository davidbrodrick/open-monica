// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import java.util.Timer;
import java.util.TimerTask;

import atnf.atoms.mon.*;
import atnf.atoms.time.*;

/**
 * Fires an update to the point when the MoniCA server first starts, using the last value out of the archive.
 * 
 * Translations will not be applied to this value (since they were already applied when the value was last updated and archived).
 * 
 * @author David Brodrick
 */
public class TransactionPersist extends Transaction {
  /** Timer used to wait until the server has finished starting up. */
  protected static Timer theirProcessTimer = new Timer();

  /** Period to check if the server has started up or not (ms). */
  protected static final int theirDelay = 1000;

  public TransactionPersist(PointDescription parent, String[] args) {
    super(parent, args);

    // Start timer to wait until server is fully running
    theirProcessTimer.schedule(new WaitingTask(), theirDelay);
  }

  /** Fire an update to our parent point once the server is fully started. */
  private class WaitingTask extends TimerTask {
    public void run() {
      if (MoniCAMain.serverFullyStarted()) {
        // Get the last value from the archive
        PointData archiveval = PointBuffer.getPreceding(itsParent.getFullName(), new AbsTime());
        if (archiveval != null) {
          // Fire the point update
          PointEvent evt = new PointEvent(this, archiveval, false);
          itsParent.firePointEvent(evt);
        }
      } else {
        // Server not yet up and running, reschedule
        theirProcessTimer.schedule(this, theirDelay);
      }
    }
  }
}
