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

/**
 * <i>AlarmCheck</i> sub-class which checks if a String data-value matches a
 * predefined set. We can work in one of two ways, either considering the new
 * value to be <i>okay</i> if it matches one of the strings in the set, or
 * considering it to be <i>in error</i> if it matches a string in the set.
 * <P>
 * The first argument determines which of these behaviours we use. If the
 * argument is <tt>true</tt> then we consider a string match be be good. If the
 * first argument is false then we consider a string match to be bad.
 * <P>
 * All the remaining arguments must be the strings to check against.
 * <P>
 * NOTE: All string comparisons are performed in a case insensitive manner.
 * 
 * @author David Brodrick
 */
public class AlarmCheckStringMatch extends AlarmCheck {
	/** Does a match indicate OK (true) or error (false). */
	protected boolean itsMatchGood = true;

	/** Strings to check the data values against. */
	protected String[] itsStringSet = null;
	
	/**
	 * Minimum amount of times a value must appear in a row before raising an
	 * alarm
	 */
	private int itsUpdateAmt = 0;
	
	/**
	 * The amount of times an alarm value has appeared in a row
	 */
	private int itsBoolCount = 0;

	public AlarmCheckStringMatch(PointDescription parent, String[] args)
			throws IllegalArgumentException, NumberFormatException {
		super(parent, args);
		
		int argval = 1;
		
		try {
			boolean firstArgInteger = true;
			// If the first argument provided is an Integer value then user has specified minimum number of updates before raising an alarm.
			try {
				itsUpdateAmt = Integer.parseInt(args[0]);
			} catch (NumberFormatException n) {
				// First argument is not an integer hence the update amount has not been specified.
				firstArgInteger = false;
				itsUpdateAmt = 1; // Set default update amount to 1.
			}
			if (!firstArgInteger) { // If boolean is first argument
				// First check which mode to run in
				itsMatchGood = Boolean.parseBoolean(args[0]);
				argval = 1;
			} else { // If Integer value is first argument
				itsMatchGood = Boolean.parseBoolean(args[1]);
				argval = 2;
			}
			
			// The next arguments are strings to check against
			itsStringSet = new String[args.length - argval];
			for (int i=argval; i < args.length; i++) {
				itsStringSet[i - argval] = args[i].trim();
			}
		} catch (Exception e) {
			Logger logger = Logger.getLogger(Factory.class.getName());
			logger.error("AlarmCheckStringMatch: Not enough arguments given!");
		}
	}

	public void checkAlarm(PointData data) {
		// No strings means we shouldn't even bother
		if (itsStringSet == null) {
			return;
		}
		// No news is good news, right?
		if (data == null || data.getData() == null) {
			return;
		}

		// Get the data in string form
		String strData = data.getData().toString().trim();

		// Check the string against each string in our set
		boolean havematch = false;
		for (int i = 0; i < itsStringSet.length; i++) {
			if (strData.equalsIgnoreCase(itsStringSet[i])) {
				havematch = true;
				break;
			}
		}

		if ((!havematch && itsMatchGood) || (havematch && !itsMatchGood)) {
			itsBoolCount++;
		} else {
			itsBoolCount = 0;
		}
		
		if (itsBoolCount >= itsUpdateAmt) {
			data.setAlarm(true);
		}
	}
}
