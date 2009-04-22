//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.externalsystem;

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
class PingCheck
extends ExternalSystem
{
  public PingCheck(String[] args)
  {
    super("pingcheck");
  }


  /** Ping the relevant servers and return the status. */
  protected
  void
  getData(PointDescription[] points)
  throws Exception
  {
    for (int i=0; i<points.length; i++) {
      PointDescription pm = points[i];
      String host = ((TransactionStrings)getMyTransactions(pm.getInputTransactions()).get(0)).getString();
      boolean canping = false;
      try {
        InetAddress address = InetAddress.getByName(host);
        canping = address.isReachable(10000);
      }
      catch (UnknownHostException e) {
        System.err.println("PingCheck: Unknown host \"" + host + "\"");
      }
      catch (IOException e) {
        System.err.println("PingCheck: Timeout for host \"" + host + "\"");
      }

      //Increment the transaction counter for this ExternalSystem
      itsNumTransactions++;

      //Fire off the updated value for this monitor point
      pm.firePointEvent(new PointEvent(this, new
         PointData(pm.getName(), pm.getSource(),
         new Boolean(canping)), true));
    }
  }
}
