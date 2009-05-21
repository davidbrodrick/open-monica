// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Class: MonitorMap
 * Description: Static class that has all the mappings that the monitoring
 * system will use.
 * @author Le Cuong Nguyen
 **/

package atnf.atoms.mon;

import java.util.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.util.*;

public class MonitorMap
{
   /** Archiver used for archiving data to disk, database, etc. */
   private static PointArchiver theirArchiver;
   
   /** Holds all known <i>'saved setups</i> for the clients to use. */
   private static TreeMap itsSetupMap = new TreeMap();

   /** System logger. */
   public static final Logger logger = new Logger("MoniCA");

   /** Handles RSA encryption of user/password pairs. */
   private static RSA itsRSA = new RSA(1024);

   
   /** Specify the archiver to be used for archiving all data. */
   public static synchronized void setPointArchiver(PointArchiver archiver)
   {
     theirArchiver = archiver;
   }
   
   public static synchronized PointArchiver getPointArchiver()
   {
      return theirArchiver;
   }

   public static long getTotalMemory()
   {
      return Runtime.getRuntime().totalMemory();
   }
   
   public static long getFreeMemory()
   {
      return Runtime.getRuntime().freeMemory();
   }
      
   /** Return the public RSA key. */
   public static String getPublicKey()
   {
     return new String(itsRSA.getE().toString());
   }

   /** Return the RSA modulus. */
   public static String getModulus()
   {
     return new String(itsRSA.getN().toString());
   }
   
   /** Generate a new set of RSA encryption keys. */
   public static void generateNewKeys()
   {
     itsRSA.generateKeys();
   }

   /** Decrypt the given ciphertext. */
   public static String decrypt(String ciphertext)
   {
     return itsRSA.decrypt(ciphertext);
   }

   /** Add the new SavedSetup to the system. */
   public static synchronized void addSetup(SavedSetup setup)
   {
     if (itsSetupMap.get(setup.getLongName())!=null) {
       //Map already contains a setup with that name. Remove and reinsert.
       itsSetupMap.remove(setup.getLongName());
     }
     itsSetupMap.put(setup.getLongName(), setup);
   }

   /** Remove the setup with the given name from the system. */
   public static synchronized void removeSetup(String setupname)
   {
     SavedSetup setup = (SavedSetup)itsSetupMap.get(setupname);
     if (setup!=null) {
      itsSetupMap.remove(setup);
    }
   }

   /** Return all SavedSetups on the system. */
   public static synchronized SavedSetup[] getAllSetups()
   {
     Object[] allkeys = itsSetupMap.keySet().toArray();
     if (allkeys==null || allkeys.length==0) {
      return null;
    }

     SavedSetup[] res = new SavedSetup[allkeys.length];
     for (int i=0; i<allkeys.length; i++) {
       res[i] = (SavedSetup)itsSetupMap.get(allkeys[i]);
     }
     return res;
   }
}
