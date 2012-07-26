//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import org.apache.log4j.Logger;

import atnf.atoms.mon.*;
import atnf.atoms.util.Angle;

/**
 * <i>AlarmCheck</i> sub-class which checks if a numeric data value is within a
 * nominated range. The first two <i>init</i> arguments should be the lower and
 * upper limits of the range. An optional third argument defines whether inside
 * the range is considered nominal (<tt>True</tt> - the default) or outside the
 * range is nominal (<tt>False</tt>). An optional fourth argument specifies the
 * number of update cycles that the value must be outside of the target range
 * before the alarm state is actually flagged (default is 1 update).
 * 
 * @author David Brodrick
 * @author Le Cuong Nguyen
 * @author Camille Nicodemus
 */
public class AlarmCheckRange extends AlarmCheck {
	/** The lower limit of the range. */
	private double itsLower = 0.0;

	/** The upper limit of the range. */
	private double itsUpper = 0.0;

	/**
	 * Is it normal to be inside the range (<tt>true</tt>) or is a normal value
	 * outside the range (<tt>false</tt>).
	 */
	private boolean itsInsideNormal = true;

	/**
	 * Minimum amount of times a value must appear in a row before raising an
	 * alarm
	 */
	private int itsUpdateAmt = 0;

	/**
	 * The amount of times an alarm value has appeared in a row
	 */
	private int itsAlarmCount = 0;

	public AlarmCheckRange(PointDescription parent, String[] args)
			throws IllegalArgumentException {
		super(parent, args);

		try {
			// Read the upper and lower limits of the numeric range
			itsLower = Double.parseDouble(args[0]);
			itsUpper = Double.parseDouble(args[1]);

			if (itsLower > itsUpper) {
				// Need to swap!
				double temp = itsUpper;
				itsUpper = itsLower;
				itsLower = temp;
			}
			// Check if the optional third argument was specified
			// Store the boolean argument for use in the alarm checking.
			if (args.length > 2) {
				itsInsideNormal = Boolean.parseBoolean(args[2]);
			}

			// This is the number of times a value should appear before setting
			// the alarm to True.
			// If no argument has been specified this defaults to an update
			// amount of 1.
			if (args.length > 3) {
				itsUpdateAmt = Integer.parseInt(args[3]);

			} else {
				itsUpdateAmt = 1;
			}

		} catch (Exception e) {
			Logger logger = Logger.getLogger(Factory.class.getName());
			logger.error("AlarmCheckRange: Need at least two arguments!");

			itsUpper = itsLower = 0.0;
		}
	}

	/**
	 * Checks if the value is "normal" with respect to the specified limits.
	 * 
	 * @param data
	 *            The value to check against our limits.
	 */
	public void checkAlarm(PointData data) {
		// All zeroes means that we shouldn't even check
		if (itsUpper == itsLower && itsLower == 0.0) {
			return;
		}
		// No news is good news, right?
		if (data == null || data.getData() == null) {
			return;
		}

		Object myData = data.getData();
		double doubleData = 0.0;

		// Retrieve the data as a double
		if (myData instanceof Number) {
			doubleData = ((Number) myData).doubleValue();
		} else if (myData instanceof Angle) {
			doubleData = ((Angle) myData).getValue();
		} else {
			Logger logger = Logger.getLogger(Factory.class.getName());
			logger.error("AlarmCheckRange: ERROR: " + data.getName()
					+ " has NON-NUMERIC data!");
			return;
		}

		// Check if the number is within the specified range
		boolean withinRange = false;
		if (doubleData >= itsLower && doubleData <= itsUpper) {
			withinRange = true;
		}

		// If it's inside the range and it's supposed to be, then all is well
		if ((!itsInsideNormal && withinRange)
				|| (itsInsideNormal && !withinRange)) {
			itsAlarmCount++;
		} else {
			itsAlarmCount = 0;
		}

		// If the alarm count is the same as the amount specified in the config
		// file, set
		// the alarm to True.
		if (itsAlarmCount >= itsUpdateAmt) {
			data.setAlarm(true);
		}
	}
}