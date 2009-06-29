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

/**
 * Class which can block the point from updating if other points are
 * unavailable or in an alarm state. This can be used when the point has
 * some kind of external flag to indicate if the reading is valid.
 *
 * <P>The first <tt>init</tt> argument must be the number of mask points
 * which are listened to, and subsequent arguments are the actual names of
 * the points.
 *
 * @author David Brodrick
 */
public class
TranslationAvailabilityMask
extends TranslationListener
{
  public
  TranslationAvailabilityMask(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Check if any listened-to points are in an alarm state and either
   * return the input data if they are okay or else return null data. */
  public
  PointData
  translate(PointData data)
  {
    //Check each listened-to point to see if it is in an alarm state
    boolean unavailable = false;
    for (int i=0; i<itsNumPoints; i++) {
      if (itsValues[i]==null || itsValues[i].getData()==null) {
        //No valid data is available
        unavailable=true;
        break;
      }
      
      if (itsValues[i].getAlarm()) {
        //This point is in an alarm state
        unavailable=true;
        break;        
      }
    }

    if (unavailable) return null;
    else return data;
  }


  /** Always returns false because this TranslationListener sub-class
   * doesn't use the doCalculations method. */
  protected
  boolean
  matchData()
  {
    return false;
  }


  /** Do nothing because listened-to points are checked in the
   * <tt>translate</tt> method. */
  protected
  Object
  doCalculations()
  {
    return null;
  }
}
