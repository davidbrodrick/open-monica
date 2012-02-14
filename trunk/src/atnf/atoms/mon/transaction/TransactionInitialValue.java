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
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.time.*;

/**
 * Provides an initial value input to the parent point. This is probably most useful for setting the startup value for control
 * points which will then subsequently have values assigned to them by outside agents.
 * 
 * Three arguments must be supplied:
 * <ol>
 * <li><b>Type code:</b> The type code of the data to be provided as input, one of <tt>int</tt>, <tt>flt</tt>, <tt>dbl</tt>,
 * <tt>str</tt>, <tt>bool</tt>.
 * <li><b>Value:</b> String representation of the value. Will be parsed as the appropriate data type.
 * </ol>
 * 
 * Here is an example definition for a point which uses a 'false' Boolean as the initial value: <BR>
 * <tt>InitialValue-"bool""false"</tt>
 * 
 * @author David Brodrick
 */
public class TransactionInitialValue extends Transaction {
  /** Timer used to wait until the server has finished starting up. */
  protected static Timer theirProcessTimer = new Timer();

  /** Period to check if the server has started up or not (ms). */
  protected static final int theirDelay = 1000;

  /** The data value to be fired. */
  protected Object itsValue;

  public TransactionInitialValue(PointDescription parent, String[] args) {
    super(parent, args);

    setChannel("NONE"); // Set the channel type

    if (args.length < 2) {
      throw new IllegalArgumentException("Requires two arguments");
    }

    // Parse the fixed value to be used as input
    itsValue = MonitorUtils.parseFixedValue(args[0], args[1]);

    // Start timer to wait until server is fully running
    theirProcessTimer.schedule(new WaitingTask(), theirDelay);
  }

  /** Fire an update to our parent point once the server is fully started. */
  private class WaitingTask extends TimerTask {
    public void run() {
      if (MoniCAMain.serverFullyStarted()) {
        // Server is running so fire initial value
        PointEvent evt = new PointEvent(this, new PointData(itsParent.getFullName(), itsValue), true);
        itsParent.firePointEvent(evt);
      } else {
        // Reschedule
        theirProcessTimer.schedule(this, theirDelay);
      }
    }
  }
}
