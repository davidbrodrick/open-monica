//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;


/**
 * Listens to a number and applies an arbitrary order polynomial to it.
 *
 * <P>The first argument is the number of terms in the polynomial. The
 * second argument is the 0th order term (output value for zero input).
 * All remaining coefficients must be given as additional arguments.
 *
 * <P>Note this class can only perform the translation when the raw data
 * is a <i>Number</i>.
 *
 * @author David Brodrick
 * @version $Id: TranslationPolynomial.java,v 1.1 2005/07/28 02:23:58 bro764 Exp $
 */
public class TranslationPolynomial
extends Translation
{
  /** Required arguments. */
  protected static String itsArgs[] = new String[]{
    "Polynomial","Polynomial",
    "Scale",  "java.lang.String",
    "Offset", "java.lang.String" };

  /** Order of polynomial. */
  int itsOrder = 1;

  /** Polynomial coefficients. */
  double[] itsCoeffs = null;

  public
  TranslationPolynomial(PointMonitor parent, String[] init)
  {
    super(parent, init);

    if (init.length<3) {
      System.err.println("TranslationPolynomial for " + parent.getName() +
			 ": Need at least two arguments");
    } else {
      itsOrder = Integer.parseInt(init[0]);
      if (itsOrder<1 || itsOrder>20) {
	System.err.println("TranslationPolynomial for " + parent.getName() +
			   ": Expect polynomial order 1-20");
      } else {
        itsCoeffs = new double[itsOrder];
	for (int i=0; i<itsOrder; i++) {
          itsCoeffs[i] = Double.parseDouble(init[i+1]);
	}
      }
    }
  }


  /** Perform the actual data translation. */
  public
  PointData
  translate(PointData data)
  {
    //Ensure there is raw data for us to translate!
    if (data==null || data.getData()==null) {
      return null;
    }

    Object d = data.getData();
    if (d instanceof Number) {
      double val  = ((Number)d).doubleValue();
      double term = val;
      double res = itsCoeffs[0];
      for (int i=1; i<itsOrder; i++) {
	res += itsCoeffs[i]*term;
	term*=val;
      }

      //Translation is now complete
      return new PointData(itsParent.getName(),
			   itsParent.getSource(),
			   data.getTimestamp(),
                           data.getRaw(),
			   new Double(res));
    } else {
      //We can only translate Numbers using this class
      System.err.println("TranslationPolynomial for " + itsParent.getName() +
			 ": GOT NON-NUMERIC ARGUMENT!");
      return null;
    }
  }


  public static
  String[]
  getArgs()
  {
    return itsArgs;
  }
}
