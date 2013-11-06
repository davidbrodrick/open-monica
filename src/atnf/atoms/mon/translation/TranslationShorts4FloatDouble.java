// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Merge four 16 bit integers to reassemble a IEEE754 64 bit double float.
 *
 * <p>
 * 1st field is Most Significant 16 bits, 2nd field is next Most Significant 16 bits, 3rd field is next Most Significant 16 bits, Least Significant 16 bits of 64 bit double float.
 *
 
@author Peter Mirtschin
 **/
public class TranslationShorts4FloatDouble extends Translation {  


	private int itsIndex3 = 0; //Hi
	private int itsIndex2 = 0;
	private int itsIndex1 = 0;
	private int itsIndex0 = 0; //Lo
	
	private int itsIntVal3 = 0; //High
	private int itsIntVal2 = 0;
	private int itsIntVal1 = 0;
	private int itsIntVal0 = 0; //Low
	
	private long itsLongValHi = 0;
	private long itsLongValLo = 0;

	protected static String[] itsArgs = new String[]{"Translation Shorts4FloatDouble", "Shorts4FloatDouble", "Array index of most significant", "Array index of next most significant", "Array index of most significant", "Array index of least significant" , "java.lang.Integer"};
   
	public TranslationShorts4FloatDouble(PointDescription parent, String[] init) {
		super(parent, init);
		
		if (init.length!=4) {
			System.err.println("ERROR: TranslationShorts4FloatDouble (for " + itsParent.getName()
				+ "): Expect 4 Arguments.. got " + init.length);
		} else {
			itsIndex3 = Integer.parseInt(init[0]);
			itsIndex2 = Integer.parseInt(init[1]);
			itsIndex1 = Integer.parseInt(init[2]);
			itsIndex0 = Integer.parseInt(init[3]);
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
		
		//Get the 4 words
		itsIntVal3 =  (Integer) array[itsIndex3];
		itsIntVal2 =  (Integer) array[itsIndex2];
		itsIntVal1 =  (Integer) array[itsIndex1];
		itsIntVal0 =  (Integer) array[itsIndex0];
		
		itsLongValHi = (itsIntVal3 << 16) + itsIntVal2;
		itsLongValLo = (itsIntVal1 << 16) + itsIntVal0;
		
		//Convert to double float and set point to return
		res.setData(Double.longBitsToDouble( (itsLongValHi << 32) + itsLongValLo ));

		//Keep the time-stamp of the parent point rather than use "now"
		res.setTimestamp(data.getTimestamp());	

		return res;		
	}

	public static String[] getArgs() {
		return itsArgs;
	}
}
