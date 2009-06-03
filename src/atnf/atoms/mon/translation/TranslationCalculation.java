//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import org.nfunk.jep.JEP;

import atnf.atoms.mon.PointDescription;

/**
 * Calculate an output value which is a function one of one or more listened-to
 * points. Expression parsing is performed using JEP, the Java Expression 
 * Parser, which makes this Translation highly flexible and very useful.
 * <P>
 * The first constructor <i>init</i> argument needs to specify the number, N,
 * of monitor points which will be listened to. The next N arguments must be
 * the names of the points being listened to. The final argument is the
 * expression to be evaluated where each input variable is named a, b, c, etc,
 * corresponding to the order they were named.
 * <P>
 * This implementation clears all inputs after a value is calculated, so that
 * a subsequent calculation will not happen until all inputs have updated again.
 *
 * @author David Brodrick
 */
public class
TranslationCalculation
extends TranslationListener
{
  protected static String[] itsArgs = new String[]{"Calculation",
  "Listens to two other points",
  "NumPoints", "Integer",
  "MonitorPoint 1", "String",
  "MonitorPoint N", "String",
  "Function", "String"};

  /** Used for parsing and evaluating the expression. */
  JEP itsParser = new JEP();

  public
  TranslationCalculation(PointDescription parent, String[] init)
  {
    super(parent, init);

    if (init.length<itsNumPoints+2) {
      System.err.println("TranslationCalculation: INSUFFICIENT ARGUMENTS: for " + parent.getFullName());
      return;
    }

    //Configure parser options
    itsParser.setAllowUndeclared(true);
    itsParser.addStandardConstants();
    itsParser.addStandardFunctions();
    itsParser.parseExpression(init[itsNumPoints+1]);
  }


  /** Provide the current input values to the expression parser, calculate
   * result and clear all input variables so that fresh values will be used
   * next time. */
  protected
  Object
  doCalculations()
  {
    for (int i=0; i<itsNumPoints; i++) {
      //Update the value for this variable
      String thisvar=""+((char)(('a')+i));
      if (!(itsValues[i].getData() instanceof Boolean)) {
        itsParser.addVariableAsObject(thisvar,itsValues[i].getData());
      } else {
        boolean boolval = ((Boolean)itsValues[i].getData()).booleanValue();
        if (boolval) {
          itsParser.addVariable(thisvar, 1.0);
        } else {
          itsParser.addVariable(thisvar, 0.0);
        }
      }
      //Clear the current value now that it has been used
      itsValues[i]=null;
    }

    //Parse the expression using new values
    Object res=itsParser.getValueAsObject();
    
    //Check for parse error
    if (itsParser.hasError()) {
      System.err.println("TranslationCalculator(" + itsParent.getFullName() + ": " + itsParser.getErrorInfo());
    }
    
    return res;
  }
}
