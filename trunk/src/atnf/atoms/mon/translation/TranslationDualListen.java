//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Generic base-class for Translations which need to listen to the values
 * of two other monitor points. You don't actually need to sub-class this to
 * listen to multiple points but it provides some useful methods for
 * identifying the source of data events and matching and storing the
 * data from the different points.
 * <P>
 * The constructor <i>init</i> arguments need to start with the names of the
 * two other points to listen to. Other, sub-class specific arguments can
 * follow these.
 * <P>
 * Sub-classes should implement the abstract <i>doCalculations</i> method in
 * order to achieve the desired functionality.
 * <P>
 * The <i>matchData</i> method is responsible for indicating when we have
 * the appropriate data to call the <i>doCalculations</i> method. This base
 * class performs the calculation whenever we have new values for both
 * points. Sub-classes can implement a <i>matchData</i> method with more
 * specialised behavior, such as checking that both data have identical
 * timestamps before allowing an output value to be calculated.
 *
 * @author David Brodrick
 * @version $Id: TranslationDualListen.java,v 1.1 2004/11/08 02:51:52 bro764 Exp $
 */
public abstract class
TranslationDualListen
extends Translation
{
  /** Name of argument point 1. */
  String itsMP1 = null;

  /** Name of argument point 2. */
  String itsMP2 = null;

  /** Latest data for argument point 1. */
  PointData itsVal1 = null;

  /** Latest data for argument point 2. */
  PointData itsVal2 = null;

  protected static String[] itsArgs = new String[]{"Dual-Listen",
  "Listens to two other points",
  "MonitorPoint 1", "java.lang.String",
  "MonitorPoint 2", "java.lang.String"};


  /** Base-class constructor. */
  public
  TranslationDualListen(PointDescription parent, String[] init)
  {
    super(parent, init);
    if (init==null||init.length<2) {
      //What to do?
      System.err.println("TranslationDualListen: NO ARGUMENTS: for " +
			 parent.getSource() + "." + parent.getName());
    } else {
      //Record the names of the points we need to listen to
      itsMP1 = init[0];
      itsMP2 = init[1];

      //Substittude the name of our source if the $1 macro was used
      if (itsMP1.indexOf("$1") > -1) {
        itsMP1 = MonitorUtils.replaceTok(itsMP1, parent.getSource());
      }
      if (itsMP2.indexOf("$1") > -1) {
        itsMP2 = MonitorUtils.replaceTok(itsMP2, parent.getSource());
      }
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

    //Check that the data value is not null
    if (data.getData()==null) {
      //There is no data, can't calculate a new value
      //This may cause probs for some sub-classes??
      return new PointData(itsParent.getFullName());
    }

    //Check if we now have the right data to produce a new output
    if (!matchData(data)) {
      //Can't produce a new output yet
      return null;
    }

    //Perform the sub-class specific calculations on the data
    Object newval = doCalculations(itsVal1.getData(), itsVal2.getData());

    //Clear current data so it is not reused
    //This may also cause probs for some sub-classes?
    itsVal1 = null;
    itsVal2 = null;

    PointData res = null;
    if (newval!=null) {
      //Save new data value and return
      res = new PointData(itsParent.getFullName(), newval);
    }
    return res;
  }


  /** Save the new data and indicate if we can now perform the calculation.
   * <P>
   * This base-class indicates that a calculation is possible whenever we
   * have a value for both points. Sub-classes may specialise this behavior,
   * eg, by ensuring the data have identical timestamps before allowing
   * a new output value to be calculated.
   * <P>
   * If the new data is useful, the <i>itsVal1</i> or <i>itsVal2</i>
   * references should be updated to point to the new data. Those same
   * fields can be checked to determine if a data calculation is now
   * appropriate.
   *
   * @param data Latest data for one of the points we listen to.
   * @return <tt>True</tt> if we can now calculate an output value,
   *   <tt>False</tt> if the current data don't enable us to perform the
   *   calculation. */
  protected
  boolean
  matchData(PointData data)
  {
    //Identify the source and save the new data value
    if (data.getName().equals(itsMP1)) {
      itsVal1 = data;
    } else if (data.getName().equals(itsMP2)) {
      itsVal2 = data;
    } else {
      System.err.println("TranslationDualListen: " + itsParent.getFullName() +
			 ": Unexpected data from " + data.getName());
      itsVal1 = null;
      itsVal2 = null;
      return false;
    }

    //All we need values for both points to calculate new value
    if (itsVal1==null || itsVal2==null) {
      //We've only got a new value for one of the points.
      //Return nothing until we have data for both points
      return false;
    }

    return true;
  }


  /** Abstract method which must be implemented by sub-classes. This
   * performs the manipulation of the two argument data required to
   * produce the quantity of interest.
   *@param val1 Most recent (non-null) data from monitor point 1
   *@param val2 Most recent (non-null) data from monitor point 2
   *@return Arbitrary combination of the two values */
  protected abstract
  Object
  doCalculations(Object val1, Object val2);


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
