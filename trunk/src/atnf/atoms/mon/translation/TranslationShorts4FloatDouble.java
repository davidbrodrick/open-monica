// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Merge four 16 bit integers to reassemble a IEEE754 64 bit double precision float.
 *
 * <p>
 * 1st field is array index of first 16 bits, 2nd field is Endian ( B[ig] - 1st array element is most significant or L[ittle] - 1st array element is least significant).
 *
 
@author Peter Mirtschin
 **/
public class TranslationShorts4FloatDouble extends Translation {  

	private int itsIndex;
	private char itsEndian;
	
	public TranslationShorts4FloatDouble(PointDescription parent, String[] init) {
		super(parent, init);
		
		if (init.length!=2) {
			System.err.println("ERROR: TranslationShorts4FloatDouble (for " + itsParent.getName()
				+ "): Expect 2 Arguments.. got " + init.length);
		} else {
			itsIndex = Integer.parseInt(init[0]); // get index of first element
			itsEndian = init[1].toUpperCase().charAt(0); // get Endianness
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
		
		//Get the 4 words into a 64 bit long
		long LongVal=0;
		switch (itsEndian) {
			case 'B': //Big Endian (MSB first)
				for (int i=0; i<4; i++) LongVal += (((Integer) array[itsIndex+i]).longValue()) << (16*(3-i));
				break;
			case 'L': //Little Endian (LSB first)
				for (int i=0; i<4; i++) LongVal += (((Integer) array[itsIndex+i]).longValue()) << (16*i);
				break;
			default: // invalid, so leave result as zero
		}
		
		//Convert to double float and set point to return
		res.setData(Double.longBitsToDouble( LongVal ));

		//Keep the time-stamp of the parent point rather than use "now"
		res.setTimestamp(data.getTimestamp());	

		return res;		
	}
}
