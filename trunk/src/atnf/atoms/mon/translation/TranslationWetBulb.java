//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointDescription;
import java.math.*;

/**
 * Iteratively determine the current wet bulb temperature given the following inputs:
 *
 * <ol>
 * <li><b>Pressure:</b> The actual pressure at the site in hPa.
 * <li><b>Temperature:</b> The outside temperature in degrees Celcius.
 * <li><b>Mixing Ratio:</b> The observed mixing ratio in g/g.
 * </ol>
 * 
 * @author David Brodrick
 */
public class TranslationWetBulb extends TranslationListener {
  /** Index of the pressure point. */
  private final static int PRES = 0;
  /** Index of the temperature point. */
  private final static int TEMP = 1;
  /** Index of the mixing ratio point. */
  private final static int R = 2;
  /** Don't get stuck in loop if there is bad input. */
  private final static int MAXGUESSES = 1000;
  
  public TranslationWetBulb(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Get the mixing ratio for the given parameters. */  
  private static double getR(double td, double tw, double pres) {
    double rw=0.622/(1.631*pres*Math.exp((-17.67*tw)/(tw+243.5))-1);
    double r=rw-4.0224e-4*(td-tw);
    return r;
  }

  /**
   * Iteratively try to determine the wet bulb temperature.
   */
  protected Object doCalculations() {
    double pres=((Number)itsValues[PRES].getData()).doubleValue();
    double temp=((Number)itsValues[TEMP].getData()).doubleValue();
    double r=((Number)itsValues[R].getData()).doubleValue();
    
    //Use the current dry bulb temperature as a starting guess.
    //The wet bulb temperature cannot exceed this.
    double twguess=temp;
    double lastguess=twguess;
    double lasterr=-999;
    for (int i=0; i<MAXGUESSES; i++) {
      //Get the mixing ratio for this estimate of the wet bulb
      double rguess = getR(temp,twguess,pres/10.0);
      //See how good this guess compares to the observed mixing ratio
      double thiserr=Math.abs(rguess-r);      
      if (lasterr!=-999 && thiserr>lasterr) {
        //Our guesses are getting worse
        break;
      }
      //This is the best guess so far
      lasterr=thiserr;
      lastguess=twguess;
      //Try a smaller wet bulb temperature next time
      twguess=twguess-0.05;
    }
    return new Double(lastguess);
  }
}
