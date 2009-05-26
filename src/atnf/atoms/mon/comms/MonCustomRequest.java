//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.comms;

import java.io.*;


/**
 * An object that has all the information required by the server.
 * @author Le Cuong Nguyen
 * @version $Id: MonRequest.java,v 1.2 2004/02/19 04:32:56 bro764 Exp bro764 $
 **/

public class
MonCustomRequest
implements Serializable
{
   public static final int GETDATA = 0;
   public static final int GETPOINT = 1;
   public static final int SETPOINT = 2;
   public static final int GETPOINTNAMES = 3;
   public static final int ADDPOINT = 6;
   public static final int GETKEY = 7;
   public static final int GETALLPOINTS = 9;
   public static final int GETALLSETUPS = 10;
   public static final int ADDSETUP = 11;

   public int Command;
   public Object[] Args = null;

   public MonCustomRequest(int cmd, Object[] args)
   {
      Command = cmd;
      Args = args;
   }
   
   public MonCustomRequest()
   {
   }
}
