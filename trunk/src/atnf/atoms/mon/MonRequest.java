//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.io.*;


/**
 * An object that has all the information required by the server.
 * @author Le Cuong Nguyen
 * @version $Id: MonRequest.java,v 1.2 2004/02/19 04:32:56 bro764 Exp bro764 $
 **/

public class
MonRequest
implements Serializable
{
   public static final int GETDATA = 0;
   public static final int GETPOINT = 1;
   public static final int SETPOINT = 2;
   public static final int GETPOINTNAMES = 3;
   //public static final int GETSOURCES = 4;
   /** Get list of <i>debugging</i> sources which should be ignored. */
   //public static final int GETIGNORESOURCES = 5;
   public static final int ADDPOINT = 6;
   public static final int GETKEY = 7;
   //public static final int GETPOINTNAMES_SHORT = 8;
   public static final int GETALLPOINTS = 9;
   /** Get all saved setups for client-side classes. */
   public static final int GETALLSETUPS = 10;
   /** Add a new saved setup to the system. */
   public static final int ADDSETUP = 11;
   /** Get names of classes to be preloaded. */
   public static final int PRELOADCLASSES = 12;
   public static final int GETTRANSACTION = 13;
   public static final int GETTRANSLATION = 14;

   public int Command;
   public Object[] Args = null;

   public MonRequest(int cmd, Object[] args)
   {
      Command = cmd;
      Args = args;
   }
   
   public MonRequest()
   {
   }
}
