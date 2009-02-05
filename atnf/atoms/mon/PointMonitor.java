// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import atnf.atoms.time.AbsTime;
import atnf.atoms.time.RelTime;
import atnf.atoms.mon.datasource.*;
import atnf.atoms.mon.translation.*;
import atnf.atoms.mon.transaction.*;
import atnf.atoms.mon.archivepolicy.*;
import atnf.atoms.mon.archiver.*;
import atnf.atoms.mon.limit.*;
import atnf.atoms.mon.util.*;
import java.awt.event.ActionEvent;

/**
 * Subclass of PointTransaction which encapsulates all aspects of a monitor
 * point.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public class
PointMonitor
extends PointInteraction
{
   /** The interval between updates. If the period is set to 0, then this
    * point has no scheduled update frequency. */
   long itsPeriod = 0;

   /** Indicates if the point is currently in the process of being updated. */
   boolean itsCollecting = false;

   /** The limit/alarm criteria for this point.
    * TODO: Should make this an array for different criteria to be used. */
   PointLimit itsLimits = null;
   
   /** The policies that define when to archive this point. */
   ArchivePolicy[] itsArchive = null;
   
   /** The archiver used to store data for this point. */
   PointArchiver itsArchiver = null;

   /** The description of what this monitor point represents. */   
   String itsLongDesc = "";
   
   /** The "units" of the monitor point value. For instance this might be
    * "Volts" or "Amps" or "dBm". Leave as null if units are not appropriate
    * for a given point. */
   String itsUnits = null;

   /** The time the point should next be updated. */
   protected transient long itsNextEpoch = 0;


   /**
    * Return the period between updates for this monitor point. A period
    * of zero has the special meaning that the update frequency is being
    * handled through some other mechanism.
    */
   public
   long
   getPeriod()
   {
     return itsPeriod;
   }


   /** Set the update interval. */
   public
   void
   setPeriod(RelTime newperiod)
   {
     itsPeriod = newperiod.getValue();
   }


   /** Set the update interval. */
   public
   void
   setPeriod(long newperiod)
   {
     itsPeriod = newperiod;
   }


   /** Set the update interval. */
   public
   void
   setPeriod(String newperiod)
   {
     if (newperiod.equalsIgnoreCase("null") || newperiod.trim().equals("-")) {
       itsPeriod = -1;
     } else {
       try {
         itsPeriod = Long.parseLong(newperiod);
       } catch (Exception e) {
         MonitorMap.logger.error("PointMonitor: (" + getName()
                                 + "): setPeriod: " + e.getMessage());
         itsPeriod = -1; //Better than doing nothing..
       }
     }
   }


   /** Overriding the PointInteraction method of the same name. This method
       ensures that the data is appropriately marked in the buffer */
   public
   void
   setEnabled(boolean enabled)
   {
      itsEnabled = enabled;
   }


   /** Get the limit/alarm checking criteria used by this point. */
   public
   PointLimit
   getLimits()
   {
     return itsLimits;
   }


   /** Set the limit/alarm checking criteria to be used by this point. */
   public
   void
   setLimits(PointLimit limits)
   {
     itsLimits = limits;
   }


   /** Get the archive policies used by this point. */
   public
   ArchivePolicy[]
   getArchive()
   {
     return itsArchive;
   }

   
   /** Specify the archive policies to be used by this point. */
   public
   void
   setArchive(ArchivePolicy[] archive)
   {
     itsArchive = archive;
   }


   /**
    * Return the time when this monitor point was last sampled.
    * If the monitor point hasn't yet been sampled "NEVER" is returned.
    */
   public
   long
   getLastEpoch()
   {
     PointData data = PointBuffer.getPointData(this);
     if (data==null) return -1;
     else return data.getTimestamp().getValue();
   }


   /**
    * Return the time when this monitor point will next be sampled.
    * If the monitor point hasn't yet been sampled, "ASAP" will be
    * returned.
    */
   public
   long
   getNextEpoch()
   {
     return itsNextEpoch;
   }
   
   
   /** Allows the manual setting of the next epoch */
   public
   void
   setNextEpoch(long nextEpoch)
   {
     itsNextEpoch = nextEpoch;
   }


   /** Allows the manual setting of the next epoch */
   public
   void
   setNextEpoch(AbsTime nextEpoch)
   {
     itsNextEpoch = nextEpoch.getValue();
   }

   
   /** Get the number of data updates to keep buffered in memory.
    * TODO: This currently just returns a fixed number.
    * TODO: Do we want to buffer updates at all? */
   public
   int
   getMaxBufferSize()
   {
     return 50;
   }


   /** Indicates if the point is in the process of actively being updated. */
   public
   boolean
   isCollecting()
   {
     return itsCollecting;
   }


   /** Set the description of this point. */
   public
   void
   setLongDesc(String desc)
   {
     itsLongDesc = desc.replace('\"', '\0');
   }

   
   /** Get the description. */
   public
   String
   getLongDesc()
   {
      return itsLongDesc;
   }


   /** Return the units of the monitor point's value. This string may be
    * null if the point has no units. */
   public
   String
   getUnits()
   {
     return itsUnits;
   }


   /** Specify the units of the monitor point's value. */
   public
   void
   setUnits(String units)
   {
     itsUnits = units;
   }


   /** Construct a new monitor point from the given fields. */
   public static PointMonitor factory(String[] names,
                                      String longDesc,
                                      String shortDesc,
                                      String units,
                                      String source,
                                      String channel,
                                      String[] translate,
                                      String limits,
                                      String[] archive,
                                      String period,
                                      boolean enabled) {
      PointMonitor result = new PointMonitor();
      result.setNames(names);
      result.setLongDesc(longDesc);
      result.setUnits(units);
      result.setSource(source);

      //Monitor point may use multiple translations
      Translation[] translations = new Translation[translate.length];
      for (int i=0; i<translate.length; i++) {
        translations[i] = Translation.factory(result, translate[i]);
      }
      result.setTranslations(translations);
      result.setTranslationString(translate);
      result.setTransaction(Transaction.factory(result, channel));
      result.setTransactionString(channel);
      result.setLimits(PointLimit.factory(limits));
      ArchivePolicy[] archives = new ArchivePolicy[archive.length];
      for (int i = 0; i < archives.length; i++) archives[i] = ArchivePolicy.factory(archive[i]);
      result.setArchive(archives);
      result.setPeriod(period);
      result.setEnabled(enabled);
      MonitorMap.addPointMonitor(result);
      PointBuffer.updateData(result, new PointData(names[0], source, AbsTime.factory()));
      return result;
   }


   /** OK, maybe data has been collected */
   public
   synchronized
   void
   firePointEvent(PointEvent pe)
   {
     MonitorWatchDog.ignore(this);

     if (pe.isRaw()) {
       //This is a raw event, we need to translate the data
       PointData data = pe.getPointData();
       //Don't translate if there was nothing to translate
       if (data!=null) {
         for (int i=0; i<itsTranslations.length; i++) {
           try {
             //Apply the next translation
             data = itsTranslations[i].translate(data);
           } catch (Throwable e) {
             System.err.println("PointMonitor:firePointevent: Translation Error:"
                                + e.getMessage());
             System.err.println("\tPOINT       = " + getSource() + "." + getName());
             System.err.println("\tTRANSLATION = " + itsTranslations[i].getClass());
           }
           //If null was returned then stop translation process
           if (data==null) break;
         }
       }
       //Translation has been completed so prepare new event and fire
       pe = new PointEvent(this, data, false);
     }

     PointData data = pe.getPointData();
     if (data!=null) {
       //Add the updated value to the archive + data buffer
       if (data!=null) PointBuffer.updateData(this, data);
       if (data.getData()!=null && itsArchiver!=null && itsEnabled) {
         //Archive data?
         for (int i=0; i<itsArchive.length; i++) {
           if (itsArchive[i].newData(data)) {
             itsArchiver.archiveData(this, data);
             break;
           }
         }
       }

       //Schedule the next collection
       if (itsPeriod>0) {
         if (itsFirstEpoch<1) {
           itsFirstEpoch = data.getTimestamp().getValue();
         }
         itsNextEpoch = data.getTimestamp().getValue() + itsPeriod;
       }

       // Pass the event on to all listeners
       Object[] listeners = itsPLList.getListenerList();
       for (int i = 0; i < listeners.length; i+=2) {
         if (listeners[i] == PointListener.class) {
           ((PointListener)listeners[i+1]).onPointEvent(this, pe);
         }
       }
     } else {
       //Schedule the next collection
       if (itsPeriod>0) {
         itsFirstEpoch = 0;
         itsNextEpoch = (new AbsTime()).getValue() + itsPeriod;
       }
     }
     itsCollecting = false;
   }


   /** Specify the PointArchiver to archive data for this point. 
    * @param archiver The PointArchiver to be used. */
   public
   void
   setArchiver(PointArchiver archiver)
   {
      itsArchiver = archiver;
   }


   /** Converts this point into a string which can be used to re-create 
    * an identical point */
   public
   String
   getStringEquiv()
   {
      StringBuffer res = new StringBuffer();
      res.append('M');
      res.append(' ');
      if (itsNames.length > 1) {
         res.append('{');
         for (int i = 0; i < itsNames.length - 1; i++) {
           res.append(itsNames[i]+",");
        }
         res.append(itsNames[itsNames.length-1]+"}");
      } else {
        res.append(itsNames[0]);
      }
      res.append(' ');
      res.append('"');
      res.append(itsLongDesc);
      res.append('"');
      res.append(' ');
      res.append('"'); //Empty place where short description used to go..
      res.append('"'); //..for backwards compat only.
      res.append(' ');
      res.append('"');
      res.append(itsUnits);
      res.append('"');
      res.append(' ');
      res.append(itsSource);
      res.append(' ');
      res.append(itsEnabled ? 'T' : 'F');
      res.append(' ');
      res.append(itsTransaction.getStringEquiv().trim());
      res.append(' ');
      if (itsTranslations.length>1) {
        //Need to write out the entire chain of Translations
        res.append('{');
        for (int i=0; i<itsTranslations.length-1; i++) {
          res.append(itsTranslations[i].getStringEquiv().trim() + ",");
        }
        res.append(itsTranslations[itsTranslations.length-1].getStringEquiv().trim() + "}");
      } else {
        res.append(itsTranslations[0].getStringEquiv().trim());
      }
      res.append(' ');
      res.append(itsLimits.getStringEquiv().trim());
      res.append(' ');
      if (itsArchive.length > 1) {
        //Write out a set of ArchivePolicies
        res.append('{');
        for (int i = 0; i < itsArchive.length - 1; i++) {
          res.append(itsArchive[i].getStringEquiv()+",");
        }
        res.append(itsArchive[itsArchive.length-1].getStringEquiv().trim()+"}");
      } else {
        res.append(itsArchive[0].getStringEquiv());
      }
      res.append(' ');
      res.append(itsPeriod);
      return res.toString();
   }
   

   /** Get a basic string representation. */
   public
   String
   toString()
   {
     return "{" + itsSource + "." + itsNames[0] + " " +
       getNextEpoch_AbsTime().toString(AbsTime.Format.UTC_STRING) + "}";
   }
}
