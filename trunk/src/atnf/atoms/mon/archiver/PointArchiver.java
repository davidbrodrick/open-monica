//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;

import java.util.*;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.AbsTime;

/**
 * Superclass for all Archiver classes, which save and retrieve data from
 * disk, databases, etc.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public abstract
class PointArchiver
extends Thread
{
   /** Data which has not yet been written out. */
   protected Hashtable<PointDescription,Vector<PointData>> itsBuffer = new Hashtable<PointDescription,Vector<PointData>>();

   /** Max number of records to be returned in response to a single archive 
    * request. The idea is to prevent the server bogging down if there is
    * a very large request.
    * Obtained from the property <tt>ArchiveMaxRecords</tt> */
   protected static final int MAXNUMRECORDS = Integer.parseInt(MonitorConfig.getProperty("ArchiveMaxRecords", "50000"));

   /** Constructor. */
   protected PointArchiver()
   {
     OldDataPurger purger = new OldDataPurger(this);
     purger.start();
   }
   
   
   /** Purge all data for the given point that is older than the specified age in days.
    * @param point The point whos data we wish to purge. */
   protected abstract
   void
   purgeOldData(PointDescription point);

   
   /** Abstract method to do the actual archiving.
    * @param pm The point whos data we wish to archive.
    * @param data Vector of data to be archived. */
   protected abstract
   void
   saveNow(PointDescription pm, Vector data);


   /** Extract data from the archive with no undersampling.
    * @param pm Point to extract data for.
    * @param start Earliest time in the range of interest.
    * @param end Most recent time in the range of interest.
    * @return Vector containing all data for the point over the time range. */
   public abstract
   Vector<PointData>
   extract(PointDescription pm, AbsTime start, AbsTime end);


   /** Return the last update which precedes the specified time.
    * We interpret 'precedes' to mean data_time<=req_time.
    * @param pm Point to extract data for.
    * @param ts Find data preceding this timestamp.
    * @return PointData for preceding update or null if none found. */
   public abstract
   PointData
   getPreceding(PointDescription pm, AbsTime ts);


   /** Return the first update which follows the specified time.
    * We interpret 'follows' to mean data_time>=req_time.
    * @param pm Point to extract data for.
    * @param ts Find data following this timestamp.
    * @return PointData for following update or null if none found. */
   public abstract
   PointData
   getFollowing(PointDescription pm, AbsTime ts);


   /** Main loop for the archiving thread. */
   public
   void
   run()
   {
     RelTime sleeptime = RelTime.factory(10000);
     while (true) {
       Enumeration keys = itsBuffer.keys();
       while (keys.hasMoreElements()) {
         Vector<PointData> bData = null;
         PointDescription pm = null;
         synchronized(itsBuffer) {
           pm = (PointDescription)keys.nextElement();
           if (pm == null) {
            continue;
          }
           if (itsBuffer.get(pm) == null) {
            continue;
          }
           bData = new Vector<PointData>(itsBuffer.remove(pm));
           if (bData.size() < 1) {
             //Sleep if no data to archive, otherwise may fast loop
             try {
               sleeptime.sleep();
             } catch (Exception e) {
               MonitorMap.logger.error("MoniCA Server: PointArchiver.run1: " + e.getMessage());
             }
             //System.err.println("Skipping " + pm.getSource() + "." + pm.getName());
             continue;
           }
         }
         //System.err.println("Archiving " + pm.getSource() + "." + pm.getName() + "\t" + bData.size());
         saveNow(pm, bData);
         try {
           sleeptime.sleep();
         } catch (Exception e) {
           MonitorMap.logger.error("MoniCA Server: PointArchiver.run2: " + e.getMessage());
         }
       }
       try {
         sleeptime.sleep();
       } catch (Exception e) {
         MonitorMap.logger.error("MoniCA Server: PointArchiver.run3: " + e.getMessage());
       }
     }
   }


   /** Archive the data for the given point. Note this actually places the
    * data into a write-out buffer, the data may not be flushed to disk
    * immediately. The <tt>saveNow</tt> does the real archiving.
    * @param pm The point that the data belongs to
    * @param data The data to save to disk */
   public
   void
   archiveData(PointDescription pm, PointData data)
   {
     synchronized(itsBuffer) {
       //Ensure this point has a storage buffer allocated
       if (!(itsBuffer.get(pm) instanceof Vector)) {
         itsBuffer.put(pm, new Vector<PointData>());
       }
       //Add the new data to our storage buffer
       Vector<PointData> myVec = itsBuffer.get(pm);
       myVec.add(data);
       itsBuffer.notifyAll();
     }
   }


   /** Check if data is still waiting to be flushed.
   * @return True if data is waiting, False if not. */
   public
   boolean
   checkBuffer()
   {
     boolean res=false;
     synchronized(itsBuffer) {
       Enumeration keys = itsBuffer.keys();
       while (keys.hasMoreElements()) {
         PointDescription pm = (PointDescription)keys.nextElement();
         if (pm == null) {
          continue;
        }
         if (itsBuffer.get(pm)!=null) {
           res=true;
           break;
         }
       }
     }
     return res;
   }
   

   /** Archive the Vector of data for the given point. Note this actually
    * places the data into a write-out buffer, the data may not be flushed
    * to disk immediately. The <tt>saveNow</tt> does the real archiving.
    * @param pm The point that the data belongs to
    * @param data The Vector of data to save to disk */
   public 
   void 
   archiveData(PointDescription pm, Vector<PointData> data)
   {
     synchronized(itsBuffer) {
       //Ensure this point has a storage buffer allocated
       if (!(itsBuffer.get(pm) instanceof Vector)) {
         itsBuffer.put(pm, new Vector<PointData>());
       }
       //Add the new data to our storage buffer
       Vector<PointData> myVec = itsBuffer.get(pm);
       myVec.addAll(data);
       itsBuffer.notifyAll();
     }
   }   
   
   /** Get a 'name' for this class. */
   public static
   String
   name()
   {
      return "PointArchiver";
   }
   
   /** Class to purge old data from the archive periodically. */
   private class OldDataPurger extends Thread {
     /** The archiver we are responsible for purging. */
     private PointArchiver itsOwner;
     
     OldDataPurger(PointArchiver owner)
     {
       super("OldDataPurger");
       itsOwner = owner;
     }
     
     /** Loops forever purging data then sleeping. */
     public
     void
     run()
     {           
       try {
         //Sleep before first purge cycle to allow system to initialise
         RelTime.factory(120000000l).sleep();
       } catch (Exception e) {}
       
       while (true) {
         AbsTime start = new AbsTime();
         
         //Loop through each point in the system, purging it if appropriate
         String[] allnames = PointDescription.getAllUniqueNames();
         for (int i=0; i<allnames.length; i++) {
           PointDescription point = PointDescription.getPoint(allnames[i]);   
           if (point==null) {
             System.err.println("PointArchiver:OldDataPurger: Point " + allnames[i] + " Doesn't Exist!");
           } else if (point.getArchiver()==itsOwner && point.getArchiveLongevity()>0) {
             itsOwner.purgeOldData(point);
           }
           try {
             //Short sleep so as not to hog resources
             RelTime.factory(250000l).sleep();
           } catch (Exception e) {}
         }
         //Sleep until next time we need to purge
         try {
           AbsTime.factory(start.getValue()+86400000000l).sleep();
         } catch (Exception e) {}
       }
     }
   }
}                                                          
