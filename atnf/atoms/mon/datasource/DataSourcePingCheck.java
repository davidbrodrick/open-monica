//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.datasource;

import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;
import java.io.*;
import java.net.*;

/**
 * Ping a remote machine and provide true/false connectivity report.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
class DataSourcePingCheck
extends DataSource
{
  public DataSourcePingCheck(String nameOfSource)
  {
    super(nameOfSource);
  }


  /** Ping the relevant servers and return the status. */
  protected
  void
  getData(Object[] points)
  throws Exception
  {
    //Precondition
    if (points==null) return;

    for (int i=0; i<points.length; i++) {
      PointInteraction pm = (PointInteraction)points[i];
      String host = ((TransactionStrings)pm.getTransaction()).getString();
      boolean canping = false;
      try {
	InetAddress address = InetAddress.getByName(host);
	canping = address.isReachable(10000);
      }
      catch (UnknownHostException e) {
	System.err.println("DataSourcePingCheck: Unknown host \"" + host + "\"");
      }
      catch (IOException e) {
        System.err.println("DataSourcePingCheck: Timeout for host \"" + host + "\"");
      }

      //Increment the transaction counter for this DataSource
      itsNumTransactions++;

      //Fire off the updated value for this monitor point
      pm.firePointEvent(new PointEvent(this, new
         PointData(pm.getName(), pm.getSource(),
         new Boolean(canping)), true));
    }
  }
}
