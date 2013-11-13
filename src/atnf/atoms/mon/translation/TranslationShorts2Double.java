// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Merge two 16 bit integers to reassemble a 32 bit integer.
 *
 * <p>
 * First field is Most Significant 16 bits, Second field is Least Significant 16 bits of 32 bit integer.
 *
 
@author Ben McKay
 **/
public class TranslationShorts2Double extends Translation {  

	private int itsHighIndex;
	private int itsLowIndex;
	
	public TranslationShorts2Double(PointDescription parent, String[] init) {
		super(parent, init);
		
		if (init.length!=2) {
			System.err.println("ERROR: TranslationShorts2Double (for " + itsParent.getName()
				+ "): Expect 2 Arguments.. got " + init.length);
		} else {
			itsHighIndex = Integer.parseInt(init[0]);
			itsLowIndex = Integer.parseInt(init[1]);
		}
	}
   
	public PointData translate(PointData data) {
	
		//Precondition
		if (data==null) return null;

		//Get the full array (of shorts)
		Object[] array = (Object[])data.getData();

		//Create the new data structure to be returned
		PointData res = new PointData(itsParent.getFullName());

		//If the data is null we need to throw a null-data result
		if (array==null) return res;
		
		//Convert to double and set point to return
		res.setData( ((Integer) array[itsHighIndex] << 16) + (Integer) array[itsLowIndex]);

		//Keep the time-stamp of the parent point rather than use "now"
		res.setTimestamp(data.getTimestamp());	

		return res;		
	}
}
