// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.*;
import java.awt.event.*;
import javax.swing.Timer;

import org.apache.log4j.Logger;

/**
 * Provides a periodic fixed-value input to the parent point.
 * 
 * Three arguments must be supplied:
 * <ol>
 * <li><b>Period:</b> The cadence at which the timer should trigger the point,
 * in floating point seconds.
 * <li><b>Type code:</b> The type code of the data to be provided as input, one
 * of <tt>int</tt>, <tt>flt</tt>, <tt>dbl</tt>, <tt>str</tt>, <tt>bool</tt>.
 * <li><b>Value:</b> String representation of the value. Will be parsed as the
 * appropriate data type.
 * </ol>
 * 
 * Here is an example definition for a point which updates every 3.5 seconds and
 * uses a 'false' Boolean as the input: <BR>
 * <tt>Timer-"3.5""bool""false"</tt>
 * 
 * @author David Brodrick
 */
public class TransactionTimer extends Transaction implements ActionListener {
  /** Timer. */
  protected Timer itsTimer;

  /** The data value to be fired. */
  protected Object itsValue;

  public TransactionTimer(PointDescription parent, String[] args) {
    super(parent, args);

    setChannel("NONE"); // Set the channel type

    if (args.length < 3) {
      throw new IllegalArgumentException("Requires three arguments");
    }

    int period;
    // Parent point claims to be aperiodic, so we require a period argument
    try {
      period = (int) (Double.parseDouble(args[0]) * 1000);
    } catch (NumberFormatException e) {
      throw new NumberFormatException("Error parsing time interval argument");
    }

    // Parse the fixed value to be used as input
    String type = args[1];
    String strval = args[2];
    try {
      if (type.equals("dbl")) {
        itsValue = new Double(strval);
      } else if (type.equals("flt")) {
        itsValue = new Float(strval);
      } else if (type.equals("int")) {
        itsValue = new Integer(strval);
      } else if (type.equals("str")) {
        itsValue = strval;
      } else if (type.equals("bool")) {
        itsValue = new Boolean(strval);
      } else if (type.equals("abst")) {
        long foo = Long.parseLong(strval, 16); // Hex
        itsValue = AbsTime.factory(foo);
      } else if (type.equals("relt")) {
        long foo = Long.parseLong(strval); // Decimal
        itsValue = RelTime.factory(foo);
      } else {
        throw new IllegalArgumentException("Unknown type code for value data type");
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to parse data value");
    }

    // Start timer
    if (itsTimer == null) {
      itsTimer = new Timer(period, this);
      itsTimer.start();
    }
  }

  /** Fire an update to our parent point when the timer expires. */
  public void actionPerformed(java.awt.event.ActionEvent e) {
    PointEvent evt = new PointEvent(this, new PointData(itsParent.getFullName(), itsValue), true);
    System.err.println(evt.getPointData());
    itsParent.firePointEvent(evt);
  }
}
