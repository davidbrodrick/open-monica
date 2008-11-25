//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archiver;

import java.io.*;
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
 * @version $Id: PointArchiver.java,v 1.6 2007/10/24 03:41:40 bro764 Exp $
 */
public abstract
class PointArchiver
extends Thread
{
   /** Data which has not yet been written out. */
   protected Hashtable itsBuffer = new Hashtable();

   /** Max number of records to be returned in response to a single archive 
    * request. The idea is to prevent the server bogging down if there is
    * a very large request.
    * Obtained from the property <tt>ArchiveMaxRecords</tt> */
   protected static final int MAXNUMRECORDS = Integer.parseInt(MonitorConfig.getProperty("ArchiveMaxRecords", "50000"));
   

   /** Abstract method to do the actual archiving.
    * @param pm The point whos data we wish to archive.
    * @param data Vector of data to be archived. */
   protected abstract
   void
   saveNow(PointMonitor pm, Vector data);


   /** Extract data from the archive.
    * @param pm Point to extract data for.
    * @param start Earliest time in the range of interest.
    * @param end Most recent time in the range of interest.
    * @return Vector containing all data for the point over the time range. */
   public abstract
   Vector
   extract(PointMonitor pm, AbsTime start, AbsTime end);


   /** Return the last update which preceeds the specified time.
    * We interpret 'preceeds' to mean data_time<=req_time.
    * @param pm Point to extract data for.
    * @param ts Find data preceeding this timestamp.
    * @return PointData for preceeding update or null if none found. */
   public abstract
   PointData
   getPreceeding(PointMonitor pm, AbsTime ts);


   /** Return the first update which follows the specified time.
    * We interpret 'follows' to mean data_time>=req_time.
    * @param pm Point to extract data for.
    * @param ts Find data following this timestamp.
    * @return PointData for following update or null if none found. */
   public abstract
   PointData
   getFollowing(PointMonitor pm, AbsTime ts);


   /** Main loop for the archiving thread. */
   public
   void
   run()
   {
     RelTime sleeptime = RelTime.factory(50000);
     while (true) {
       Enumeration keys = itsBuffer.keys();
       while (keys.hasMoreElements()) {
         Vector bData = null;
         PointMonitor pm = null;
         synchronized(itsBuffer) {
           pm = (PointMonitor)keys.nextElement();
           if (pm == null) continue;
           if (itsBuffer.get(pm) == null) continue;
           bData = new Vector((Vector)itsBuffer.remove(pm));
           if (bData.size() < 1) {
             //Sleep even if no data to archive, otherwise uses all CPU
             try {
               sleeptime.sleep();
             } catch (Exception e) {
               MonitorMap.logger.error("MoniCA Server: PointArchiver.run1: " + e.getMessage());
             }
             continue;
           }
         }
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
   archiveData(PointMonitor pm, PointData data)
   {
     synchronized(itsBuffer) {
       //Ensure this point has a storage buffer allocated
       if (!(itsBuffer.get(pm) instanceof Vector)) {
         itsBuffer.put(pm, new Vector());
       }
       //Add the new data to our storage buffer
       Vector myVec = (Vector)itsBuffer.get(pm);
       myVec.add(data);
       itsBuffer.notifyAll();
     }
   }


   /** Archive the Vector of data for the given point. Note this actually
    * places the data into a write-out buffer, the data may not be flushed
    * to disk immediately. The <tt>saveNow</tt> does the real archiving.
    * @param pm The point that the data belongs to
    * @param data The Vector of data to save to disk */
   public 
   void 
   archiveData(PointMonitor pm, Vector data)
   {
     synchronized(itsBuffer) {
       //Ensure this point has a storage buffer allocated
       if (!(itsBuffer.get(pm) instanceof Vector)) {
         itsBuffer.put(pm, new Vector());
       }
       //Add the new data to our storage buffer
       Vector myVec = (Vector)itsBuffer.get(pm);
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
}                                                          
