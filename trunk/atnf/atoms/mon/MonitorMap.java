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
import atnf.atoms.mon.datasource.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.util.*;
import java.math.BigInteger;

public class MonitorMap
{
   /** Archiver used for archiving data to disk, database, etc. */
   private static PointArchiver theirArchiver;

   private static TreeMap itsPointMap = new TreeMap();
   
   /** Map of all data sources known to the system. */
   private static TreeMap itsDataSourceMap = new TreeMap();

   /** Holds all known <i>'saved setups</i> for the clients to use. */
   private static TreeMap itsSetupMap = new TreeMap();

   private static TreeMap itsPoints = new TreeMap();

   private static TreeMap itsSources = new TreeMap();

   /** Holds list of sources which we should normally ignore. */
   private static Vector itsIgnoreSources = new Vector();

   /** System logger. */
   public static final Logger logger = new Logger("MoniCA");

   /** Handles RSA encryption of user/password pairs. */
   private static RSA itsRSA = new RSA(1024);

   public static synchronized void addPointMonitor(PointMonitor pm)
   {
     String[] hashes = pm.getHashes();
     for (int i = 0; i < hashes.length; i++) itsPointMap.put(hashes[i], pm);
     if (itsSources.get(pm.getSource()) == null) itsSources.put(pm.getSource(), new TreeSet());
     ((TreeSet)itsSources.get(pm.getSource())).add(pm.getLongName());
     if (itsPoints.get(pm.getLongName()) == null) itsPoints.put(pm.getLongName(), new TreeSet());
     ((TreeSet)itsPoints.get(pm.getLongName())).add(pm.getSource());
     //If the Transaction is not null, assign to appropriate DataSource
     Transaction t = pm.getTransaction();
     if (t!=null && !t.getChannel().equals("NONE")) {
       DataSource ds = getDataSource(t.getChannel() + pm.getSource());
       if (ds!=null) {
         ds.addPoint(pm);
         //System.err.println("MonitorMap:addPointMonitor: OK for "
         //     	    + pm + " (" + t.getChannel() + pm.getSource() + ")");
       } else {
         System.err.println("MonitorMap:addPointMonitor: No DataSource for "
                            + pm + " (" + t.getChannel() + pm.getSource() + ")");
       }
     }
     if (pm.getArchive()!=null) {
       pm.setArchiver(theirArchiver);
     }
   }
   
   public static synchronized PointInteraction getPointInteraction(String hash)
   {
     return (PointInteraction)itsPointMap.get(hash);
   }

   public static synchronized PointMonitor getPointMonitor(String hash)
   {
      return (PointMonitor)itsPointMap.get(hash);
   }
   
   /** Careful, this method does not fix all the maps yet */
   public static synchronized void removePointMonitor(PointMonitor pm)
   {
      String[] hashes = pm.getHashes();
      for (int i = 0; i < hashes.length; i++) {
         itsPointMap.remove(hashes[i]);
      }
      if (itsSources.get(pm.getSource()) != null)
        ((TreeSet)itsSources.get(pm.getSource())).remove(pm.getLongName());
      if (itsPoints.get(pm.getLongName()) != null)
        ((TreeSet)itsPoints.get(pm.getLongName())).remove(pm.getSource());
   }
   
   public static synchronized void addDataSource(DataSource source)
   {
      itsDataSourceMap.put(source.getName(), source);
   }

   public static synchronized void addDataSource(String name, DataSource source)
   {
      itsDataSourceMap.put(name, source);
   }
   
   public static synchronized DataSource getDataSource(String name)
   {
      return (DataSource)itsDataSourceMap.get(name);
   }
   
   public static synchronized String[] getDataSources()
   {
      return MonitorUtils.toStringArray(itsDataSourceMap.keySet().toArray());
   }

   /** Specify the archiver to be used for archiving all data. */
   public static synchronized void setPointArchiver(PointArchiver archiver)
   {
     theirArchiver = archiver;
   }
   
   public static synchronized PointArchiver getPointArchiver()
   {
      return theirArchiver;
   }
   
   
   /** Returns all the points (including aliases) in the system */
   public static synchronized String[] getPointNames()
   {
      return MonitorUtils.toStringArray(itsPointMap.keySet().toArray());
   }

   /** Returns the names of all the points (including aliases) without prepending the sourcename */
   public static synchronized String[] getPointNamesShort()
   {
      return MonitorUtils.toStringArray(itsPoints.keySet().toArray());
   }

   /** Returns all the points on a source */
   public static synchronized String[] getPointNames(String source)
   {
      String[] res = MonitorUtils.toStringArray(((TreeSet)itsSources.get(source)).toArray());
      for (int i = 0; i < res.length; i++) res[i] = source + "." + res[i];
      return res;
   }

   /** Returns all the points on a source without the prepending sourcename*/
   public static synchronized String[] getPointNamesShort(String source)
   {
      return MonitorUtils.toStringArray(((TreeSet)itsSources.get(source)).toArray());
   }

   /** Check if the specified monitor point exists */
   public static
   boolean
   checkPointName(String name)
   {
     if (itsPointMap.containsKey(name)) return true;
     else return false;
   }

   /** Returns all sources on the system */
   public static synchronized String[] getSources()
   {
      return MonitorUtils.toStringArray(itsSources.keySet().toArray());
   }

   /** Returns all the sources for the specified point.
    * @param pointname Name of the point ot get the sources for.
    * @return Array containing all sources for the given point.
    *         <tt>null</tt> will be returned if there are no sources. */
   public static synchronized String[] getSources(String pointname)
   {
     String[] res = null;
     TreeSet ts = (TreeSet)itsPoints.get(pointname);
     if (ts!=null) {
       res = MonitorUtils.toStringArray(ts.toArray());
     }
     return res;
   }

   public static long getTotalMemory()
   {
      return Runtime.getRuntime().totalMemory();
   }
   
   public static long getFreeMemory()
   {
      return Runtime.getRuntime().freeMemory();
   }
      
   public static long getCPUTime()
   {
      return 0;
//      return MonitorNativeLinux.getCPUTime();
   }

   public static long getCPUUserTime()
   {
      return 0;
//      return MonitorNativeLinux.getCPUUserTime();
   }

   public static long getCPUSystemTime()
   {
      return 0;
//      return MonitorNativeLinux.getCPUSystemTime();
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
     if (setup!=null) itsSetupMap.remove(setup);
   }

   /** Return all SavedSetups on the system. */
   public static synchronized SavedSetup[] getAllSetups()
   {
     Object[] allkeys = itsSetupMap.keySet().toArray();
     if (allkeys==null || allkeys.length==0) return null;

     SavedSetup[] res = new SavedSetup[allkeys.length];
     for (int i=0; i<allkeys.length; i++) {
       res[i] = (SavedSetup)itsSetupMap.get(allkeys[i]);
     }
     return res;
   }

   public static synchronized String[] getAllPoints()
   {
      TreeSet uniquePoints = new TreeSet(itsPointMap.values());
      Iterator i = uniquePoints.iterator();
      TreeSet points = new TreeSet();
      while (i.hasNext()) points.add(((PointMonitor)i.next()).getStringEquiv());
      return MonitorUtils.toStringArray(points.toArray());
   }

}
