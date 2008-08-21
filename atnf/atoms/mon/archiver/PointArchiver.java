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
 * Superclass for all Archiver classes. The Archiver classes are able to
 * save and retrieve data to disk. This base-class defines basic
 * functionality relevant to all the archiving techniques. The sub-classes
 * define the methods which actually save and retrieve the data in some
 * format.
 *
 * <i>This class is currently geared towards Archivers which write data
 * to files. It should be made more abstract so that Archivers which write
 * to databases, etc, can all be neatly accomodated.</i>
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: PointArchiver.java,v 1.6 2007/10/24 03:41:40 bro764 Exp $
 */
public abstract
class PointArchiver
extends Thread
{
   Hashtable itsBuffer = new Hashtable();
   boolean itsRunning = true;
   String itsArg = null;

   /** Base directory for the data archive.
    * Derived from the property <tt>LogDir</tt>. */
   public static final String SAVEPATH = MonitorConfig.getProperty("LogDir");
   /** OS-dependant file separation character. */
   protected static final char FSEP = System.getProperty("file.separator").charAt(0);
   /** Maximum size for an archive file.
    * Derived from the property <tt>ArchiveSize</tt>. */
   protected static final int MAXLENGTH = Integer.parseInt(MonitorConfig.getProperty("ArchiveSize"));
   /** Max time-span for an archive data file.
    * Derived from the property <tt>ArchiveMaxAge</tt> */
   protected static final int MAXAGE = 1000 * Integer.parseInt(MonitorConfig.getProperty("ArchiveMaxAge"));
   /** Max number of records to be returned in response to a single archive request.
    * Derived from the property <tt>ArchiveMaxRecords</tt> */
   protected static final int MAXNUMRECORDS = Integer.parseInt(MonitorConfig.getProperty("ArchiveMaxRecords", "86400"));
   /** Enable extra debugging (<tt>true</tt>) or not (<tt>false</tt>).
    * The value for this is derived from the property <tt>Debug</tt>. */
   protected static boolean itsDebug = false;

   /** */
   public PointArchiver(String arg)
   {
      itsArg = arg;
      MonitorMap.addPointArchiver(this);
      Object db = MonitorConfig.getProperty("Debug");
      if (db != null && ((String)db).equalsIgnoreCase("true")) itsDebug = true;
   }


   /** Abstract method to do the actual archiving.
    * @param pm The point whos data we wish to archive.
    * @param data Vector of data to be archived. */
   protected abstract
   void
   saveNow(PointMonitor pm, Vector data);


   /** Abstract method to extract data from the archive.
    * @param pm Point to extract data for.
    * @param start Earliest time in the range of interest.
    * @param end Most recent time in the range of interest.
    * @param undersample Undersampling factor.
    * @return Vector containing all data for the point over the time range. */
   public abstract
   Vector
   extract(PointMonitor pm, AbsTime start, AbsTime end, int undersample);


   /** Extract data from the archive with no undersampling.
    * @param pm Point to extract data for.
    * @param start Earliest time in the range of interest.
    * @param end Most recent time in the range of interest.
    * @return Vector containing all data for the point over the time range. */
   public
   Vector
   extract(PointMonitor pm, AbsTime start, AbsTime end)
   {
     return extract(pm, start, end, 0);
   }


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


   /** */
   public
   String
   getArg()
   {
     return itsArg;
   }


   /** Archive the data for the given point. Note this actually places the
    * data into a write-out buffer, the data may not be flushed to disk
    * immediately. The <tt>saveNow</tt> does the real archiving.
    * @param pm The point that the data belongs to
    * @param data The data to save to disk */
   public
   void
   saveToFile(PointMonitor pm, PointData data)
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
   public void saveToFile(PointMonitor pm, Vector data)
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


   /** Get the appropriate filename representation of the current time.
    * @return Filename representation of the current time. */
   public static
   String
   getDateTimeNow()
   {
      return getDateTime(new Date());
   }


   /** Get a filename representation of the given epoch.
    * @param date Date to translate into a filename.
    * @return Filename corresponding to the given Date. */
   public static
   String
   getDateTime(Date date)
   {
      GregorianCalendar calendar = new GregorianCalendar();
      calendar.setTime(date);
      calendar.setTimeZone(SimpleTimeZone.getTimeZone("GMT"));
      StringBuffer buf = new StringBuffer("");
      //YYYYMMDD-HHMM format
      buf.append(calendar.get(Calendar.YEAR));
      if (calendar.get(Calendar.MONTH) < 9) buf.append("0");
      buf.append(calendar.get(Calendar.MONTH)+1);
      if (calendar.get(Calendar.DAY_OF_MONTH) < 10) buf.append("0");
      buf.append(calendar.get(Calendar.DAY_OF_MONTH));
      buf.append("-");
      if (calendar.get(Calendar.HOUR_OF_DAY) < 10) buf.append("0");
      buf.append(calendar.get(Calendar.HOUR_OF_DAY));
      if (calendar.get(Calendar.MINUTE) < 10) buf.append("0");
      buf.append(calendar.get(Calendar.MINUTE));
      return buf.toString();
   }


   /** Get the epoch represented by the given file name.
    * @param parseDate The filename to parse.
    * @return Date represented by the given filename. */
   public static
   Date
   getDateTime(String parseDate)
   {
      try {
        int i = 0;
        //YYYYMMDD-HHMM format
        int year  = Integer.parseInt(parseDate.substring(i,i+=4));
        int month = Integer.parseInt(parseDate.substring(i, i+=2)) - 1;
        int day   = Integer.parseInt(parseDate.substring(i, i+=2));
        i++;
        int hour  = Integer.parseInt(parseDate.substring(i, i+=2));
        int minute = Integer.parseInt(parseDate.substring(i, i+=2));
        GregorianCalendar pope = new GregorianCalendar(year, month, day,
                                                       hour, minute);
        pope.setTimeZone(SimpleTimeZone.getTimeZone("GMT"));
        return pope.getTime();
      } catch (Exception e) {return null;}
   }


   /** Get the save directory for the given point.
    * @param pm Point to get the archive directory for.
    * @return Name of appropriate archive directory. */
   public static
   String
   getDir(PointMonitor pm)
   {
     String tempname = pm.getName();
     tempname = tempname.replace('.', FSEP);
     return SAVEPATH + FSEP + tempname + FSEP + pm.getSource();
   }


   /** Get a 'name' for this class. */
   public static
   String
   name()
   {
      return "PointArchiver";
   }
}                                                          
