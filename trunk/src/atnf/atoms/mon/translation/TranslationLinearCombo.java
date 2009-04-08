//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;

/**
 * Produces a linear combination of listened-to monitor points. This class
 * would normally be used together with a <i>TransactionListen</i> that is
 * configured to listen to the monitor points of interest. The arguments
 * for this class must be of the form <tt>"COEF""POINT"</tt> where COEF is
 * the coefficient to use with this monitor point (eg, 0.32) and POINT is
 * the name of the monitor point to use this coefficient with. You can
 * make the linear combination of however many monitor points you like.
 * Obviously the listened-to points must contain numerical data.
 *
 * <P>At the minute there is nothing very smart for synching up monitor
 * points.. so we need to be careful and think about the best way to handle
 * this.
 *
 * @author David Brodrick
 * @version $Id: TranslationLinearCombo.java,v 1.1 2005/08/30 05:41:10 bro764 Exp $
 */
public class
TranslationLinearCombo
extends Translation
{
  /** Names of the listened-to monitor points. */
  private Vector itsPoints = new Vector();

  /** Coefficients for each listened-to point. */
  private Vector itsCoeffs = new Vector();

  /** Contains the latest updates for each monitor point. */
  private HashMap itsValues = new HashMap();

  protected static String[] itsArgs = new String[]{"Linear Combo",
  "Listens to other points and numerically combines them"};


  /** Base-class constructor. */
  public
  TranslationLinearCombo(PointMonitor parent, String[] init)
  {
    super(parent, init);
    for (int i=0; i<init.length; i+=2) {
      //First should come the coefficient
      Float coef = new Float(init[i]);
      //Then the name of the point
      String name = init[i+1];
      //Substitute the name of our source if the $1 macro was used
      if (name.indexOf("$1") > -1) {
        name = MonitorUtils.replaceTok(name, parent.getSource());
      }
      itsPoints.add(name);
      itsCoeffs.add(coef);
    }
  }


  /**  */
  public
  PointData
  translate(PointData data)
  {
    //Precondition
    if (data==null) {
      return null;
    }

    //save the new value
    itsValues.put(data.getSource()+"."+data.getName(), data.getData());

    //check if we are ready to produce a new result
    boolean allfound = true;
    for (int i=0; i<itsPoints.size(); i++) {
      if (itsValues.get(itsPoints.get(i))==null) {
	allfound = false;
        break;
      }
    }

    if (!allfound) {
      //We don't have all the updated values yet
      return null;
    }

    //We can go ahead and calculate the linear combination
    double resval = 0.0;
    for (int i=0; i<itsPoints.size(); i++) {
      Number n = (Number)itsValues.get(itsPoints.get(i));
      Number c = (Number)itsCoeffs.get(i);
      resval += c.floatValue() * n.floatValue();
    }

    //Flag all current values as used
    itsValues.clear();

    //Save new data value and return
    PointData res = new PointData(itsParent.getName(), itsParent.getSource());
    res.setData(new Float(resval));
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
