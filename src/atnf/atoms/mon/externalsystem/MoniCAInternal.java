//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import atnf.atoms.time.AbsTime;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;

/**
 * Used to return data about the MoniCA server.
 * 
 * Points using this should have a TransactionStrings with the channel set to
 * "system" and a second argument which determines the data to be retrieved.
 * This may be one of the following:
 * 
 * <ul>
 * <li><b>time</b> Return the current time on the server.
 * <li><b>points</b> Return the current number of points defined on the server.
 * <li><b>systems</b> Return the current number of external systems defined on
 * the system.
 * </ul>
 * 
 * @author David Brodrick
 **/
class MoniCAInternal extends ExternalSystem {
  public MoniCAInternal(String[] args) {
    super("system");
  }

  protected void getData(PointDescription[] points) throws Exception {
    try{
    for (int i = 0; i < points.length; i++) {
      PointDescription desc = points[i];
      // Get the Transactions which associates the point with us
      TransactionStrings thistrans = (TransactionStrings) getMyTransactions(desc.getInputTransactions()).get(0);
      PointData pd = new PointData(desc.getFullName(), AbsTime.factory(), null);

      if (thistrans.getString().equals("time")) {
        pd.setData(pd.getTimestamp());
      } else if (thistrans.getString().equals("points")) {
        pd.setData(new Integer(PointDescription.getAllPoints().size()));
      } else if (thistrans.getString().equals("systems")) {
        pd.setData(new Integer(ExternalSystem.getAllExternalSystems().size()));
      }

      desc.firePointEvent(new PointEvent(this, pd, true));
    }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
