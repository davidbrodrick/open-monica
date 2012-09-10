// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Merge two 16 bit integers to reassemble a IEEE754 32 bit float.
 *
 * <p>
 * First field is Most Significant 16 bits, Second field is Least Significant 16 bits of 32 bit float.

@author Ben McKay
 **/
public class TranslationShorts2Float extends Translation {  


	private int itsHighIndex = 0;
	private int itsLowIndex = 0;
	
	private int itsHigh = 0;
	private int itsLow = 0;

	protected static String[] itsArgs = new String[]{"Translation Shorts2Float", "Shorts2Float", "Array index of most significant", "Array index of least significant" , "java.lang.Integer"};
   
	public TranslationShorts2Float(PointDescription parent, String[] init) {
		super(parent, init);
		
		if (init.length!=2) {
			System.err.println("ERROR: TranslationShorts2Float (for " + itsParent.getName()
				+ "): Expect 2 Arguments.. got " + init.length);
		} else {
			itsHighIndex = Integer.parseInt(init[0]);
			itsLowIndex = Integer.parseInt(init[1]);
		}
		
	}
   
	public PointData translate(PointData data) {
	
		//Precondition
		if (data==null) {
		  return null;
		}

		//Get the full array (of shorts)
		Object[] array = (Object[])data.getData();

		//Create the new data structure to be returned
		PointData res = new PointData(itsParent.getFullName());

		//If the data is null we need to throw a null-data result
		if (array==null) return res;
		
		//Get high and low word
		itsHigh =  (Integer) array[itsHighIndex];
		itsLow =  (Integer) array[itsLowIndex];
		
		//Convert to float and set point to return
		res.setData(Float.intBitsToFloat((itsHigh << 16) + itsLow));

		//Keep the time-stamp of the parent point rather than use "now"
		res.setTimestamp(data.getTimestamp());	

		return res;		
	}

	public static String[] getArgs() {
		return itsArgs;
	}
}
