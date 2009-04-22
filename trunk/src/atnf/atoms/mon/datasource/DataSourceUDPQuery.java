// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import java.net.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.transaction.*;


/**
 * This class is for when you need to send an ASCII query string to a remote
 * UDP socket and expect an ASCII string in the response packet.
 *
 * <P>Monitor points using this should use a TransactionStrings, where the
 * first non-channel string contains the query string that needs to be sent
 * to the server.
 *
 * <P>If the response times out then <tt>null</tt> will be returned, otherwise
 * the full response packet will be returned as a String. If appropriate you
 * can use use appropriate Translations to dice and slice the response string.
 *
 * The monitor-sources.txt arguments for this class are:
 * <bl>
 * <li>Remote host name
 * <li>Remote port number
 * <li>Timeout (ms) - Optional
 * </bl>
 *
 * The name of the resulting channel will take the format
 * <tt>hostname-port</tt>, you will need to use this in the Transaction
 * for any points which will use this DataSource.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class DataSourceUDPQuery
extends DataSourceUDPSocket
{
  /** Address of the remote host. */
  protected InetAddress itsRemoteHost;
  
  /** Port on the remote host to send the query to. */
  protected int itsRemotePort;
  
  /** constructor. */
  public DataSourceUDPQuery(String args[])
  {
    super(new String[]{args[0]+"-"+args[1]});
    
    try {
      itsRemoteHost=InetAddress.getByName(args[0]);
      itsRemotePort=Integer.parseInt(args[1]);
    } catch (Exception e) {
      System.err.println("DataSourceUDPQuery: Error parsing arguments:");
      e.printStackTrace();
    }
    
    //Check for the optional timeout argument
    if (args.length>2) {
      try {
        itsTimeout=Integer.parseInt(args[2]);
      } catch (Exception e) {
        System.err.println("DataSourceUDPQuery (" + itsRemoteHost + ":" +
                           itsRemotePort + "): Error parsing timeout argument");
      }    
    }
  }


  /** Do the query and return the result. */
  public
  Object
  parseData(PointDescription requestor)
  throws Exception
  {
    try {
      String requeststr = ((TransactionStrings)getMyTransactions(requestor.getInputTransactions()).get(0)).getString(0);
    
      //Build datagrams for the request and response packet
      DatagramPacket request = new DatagramPacket(requeststr.getBytes(), requeststr.length(),
                                                itsRemoteHost, itsRemotePort);
      DatagramPacket response = new DatagramPacket(new byte[1500], 1500);

      //Send the query and obtain the response
      itsSocket.send(request);
      itsSocket.receive(response);

      //Get the response packet payload as a string    
      String res = new String(response.getData());
      return res;
    } catch (Exception e) {
      return null;
    }
  }
}
