//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.time.AbsTime;

/**
 * Base-class for Translations which need synchronous values from two other
 * monitor points.
 * <P>
 * The constructor <i>init</i> arguments need to start with the names of the
 * two points to listen to.
 * <P>
 * The <i>matchData</i> method will only allow <i>doCalculations</i> to be
 * called if the data from both points have exactly the same timestamp.
 * The actual time-stamp is made available to sub-classes via the instance
 * variable <i>itsEpoch</i>.
 * <P>
 * Sub-classes should implement the abstract <i>doCalculations<i> method in
 * order to achieve the desired functionality.
 *
 * @author David Brodrick
 * @version $Id: TranslationSynch.java,v 1.3 2005/07/07 23:52:57 bro764 Exp $
 */
public abstract class
TranslationSynch
extends TranslationDualListen
{
  protected static String[] itsArgs = new String[]{"Sync.",
  "Listens for synchronous data from two other points",
  "MonitorPoint 1", "java.lang.String",
  "MonitorPoint 2", "java.lang.String"};

  /** Time-stamp of the latest synchronized data. */
  protected AbsTime itsEpoch = null;

  public
  TranslationSynch(PointMonitor parent, String[] init)
  {
    super(parent, init);
  }


  /** Save the new data and indicate if we can now perform the calculation.
   * <P>
   * In this class, the calculation will only be allowed if both data now
   * have identical timestamps.
   *
   * @param data Latest data for one of the points we listen to.
   * @return <tt>True</tt> if we can now calculate an output value,
   *   <tt>False</tt> if the current data don't enable us to perform the
   *   calculation. */
  protected
  boolean
  matchData(PointData data)
  {
    if (!super.matchData(data)) {
      //Mustn't have data for both points yet
      return false;
    }

    //We need to be the timestamps to be the same
    if (!itsVal1.getTimestamp().equiv(itsVal2.getTimestamp())) {
      //They aren't the same so don't calculate a new value
      return false;
    }

    itsEpoch = itsVal1.getTimestamp();

    //They are the same - so go for it!
    return true;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
