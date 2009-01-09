// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
/**
 * Class: FakeMonitor
 * Description: Just like a PointMonitor, only not.
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon;

import atnf.atoms.mon.client.*;
import atnf.atoms.mon.limit.*;
import atnf.atoms.time.*;
import atnf.atoms.mon.transaction.*;

public class FakeMonitor extends PointMonitor
{
   String[] itsArchiveString = null;
   String itsLimitsString = "";
   protected static final int MAXTIMES = 255;
   protected long[] itsLastTimestamps = new long[MAXTIMES];
   protected int itsTimestampPos = 0;
   protected long itsLast = -1;
   protected long itsLastTimestamp = 0;
   protected int itsSequence = -1;
   protected transient long itsDelay = 500000;
   protected transient int itsDelayNum = 0;
   protected transient long itsTimeOffset = 0;

   public String
   getLimitsString()
   {
     return itsLimitsString;
   }

   public void
   setLimitsString(String limits)
   {
      itsLimitsString = limits;
   }
   
   public void setTimeOffset(long offset)
   {
      itsTimeOffset = offset;
   }
   
   public long getTimeOffset()
   {
      return itsTimeOffset;
   }

   public void setDelay(long delay)
   {
      itsDelay = 500000;
      itsDelayNum = 0;
   }

   public long getDelay()
   {
      itsDelayNum++;
      if (itsDelayNum > 10) {
         itsDelay *= 2;
	 itsDelayNum = 0;
      }
      return itsDelay;
   }
      
   public void
   setArchiveString(String[] archive)
   {
      itsArchiveString = archive;
   }
   
   public String[]
   getArchiveString()
   {
      return itsArchiveString;
   }

   public void setEnabled(boolean enabled)
   {
      itsEnabled = enabled;
   }
      
   public static
   FakeMonitor
   Fakefactory(String[] names,
	       String longDesc,
	       String shortDesc,
	       String units,
	       String source,
	       String channel,
	       String[] translate,
	       String limits,
	       String[] archive,
	       String period,
	       boolean enabled)
   {
      FakeMonitor result = new FakeMonitor();
      result.setNames(names);
      result.setLongDesc(longDesc);
      result.setShortDesc(shortDesc);
      result.setUnits(units);
      result.setSource(source);
      result.setTranslationString(translate);
      result.setTransactionString(channel);
      //result.setTransaction(Transaction.factory(result, channel));
      result.setTransaction(null);
      result.setLimitsString(limits);
      result.setLimits(PointLimit.factory(limits));
      result.setArchiveString(archive);
      result.setPeriod(period);
      result.setEnabled(enabled);
      MonitorMap.addPointMonitor(result);
      return result;
   }

   public void collectData()
   {
      itsCollecting = true;
      Object[] listeners = itsPLList.getListenerList();
      // No-one is listening except for the DataMaintainer
      if (listeners.length < 3) {
        firePointEvent(new PointEvent(this, null, false));
        return;
      }
      PointData data = (MonClientUtil.getServer()).getPointData(getHash());
      if (data==null) return; //Maybe no connection to server?
      System.err.println("FakeMonitor:CollectData: " + data.getName() + " " + data.getSource() + " " + data.getData());
      firePointEvent(new PointEvent(this, data, false));
   }

   public void collecting()
   {
     itsCollecting = true;
   }


   public void collecting(boolean state)
   {
     itsCollecting = state;
   }


   public void firePointEvent(PointEvent pe)
   {
     if (pe.getPointData() == null) {
       //System.err.println("FakeMonitor:firePointEvent: Event Data was Null");
       if (itsNextEpoch!=0 && itsPeriod>0)
	 itsNextEpoch = AbsTime.factory().getValue() + itsPeriod;
       else
	 itsNextEpoch = AbsTime.factory().getValue() + 1000000;
       itsNextEpoch += 50000;
       fireEvents(pe);
       itsCollecting = false;
       return;
     }
     PointData data = pe.getPointData();

     //Check to see if we collected the same data twice. If we did this
     //might be because: the point has stopped being monitored (eg because
     //the DataSource has lost it's connection to the source), we were just
     //unlucky and our timing was slightly off, or because the period
     //of the point has changed.
      boolean duplicateCollection = false;
      long lastTimestamp = data.getTimestamp().getValue();
      if (lastTimestamp == itsLastTimestamp) duplicateCollection = true;
      itsLastTimestamp = lastTimestamp;

      if (data.getSequence() - itsSequence > 1 && itsSequence > -1) {
	System.err.println(data.getSource() + "." + data.getName() +
			   " was out of sequence");
      }
      itsSequence = data.getSequence();


      if (itsPeriod > 0) {
        if (!data.isValid()) {
          //No valid data found, the DataSource is probably disconnected
          //from the source, so just wait for a while and try again
          itsNextEpoch = new AbsTime().getValue() + itsPeriod;
        } else if (duplicateCollection) {
          //Valid data - but it's the same data we've seen before!
          long diff = AbsTime.factory().getValue() - lastTimestamp;
          double d = diff/1000000.0;
          String n = getSource() + "." + getName();

          System.out.println(n + " should update every " + itsPeriod/1000000 +
                             " seconds but last good value is " + d +
                             " seconds old - I will try again in " +
                             itsPeriod/2000000 + " seconds.");

          //Try again in half a sample period
          itsNextEpoch = new AbsTime().getValue() + itsPeriod/2; 
          pe = null; //null event to ensure DataMaintainer reschedules us
        } else {
          //We got valid data, and know when to collect the next data
          itsNextEpoch = lastTimestamp + itsPeriod;
        }
      } else {
         // Non-predefined period
         // Check the sequences - invalidate data if sequence not met
         boolean seq = true;
         if (data.getSequence() - itsSequence > 1 && itsSequence > -1) {
             seq = false;
         }
         itsSequence = data.getSequence();

         // Data is invalid
         //if (duplicateCollection || seq == false) {
         if (seq == false) {
           System.err.println("Sequence was invalid");
           itsNextEpoch += getDelay() + 50000;
           fireEvents(pe);
           itsCollecting = false;
           return;
         }
	 
         // Data is valid
         setDelay(500000);
         itsLastTimestamps[itsTimestampPos] = lastTimestamp;
         itsTimestampPos++;

         // Expand array if you have to
         if (itsTimestampPos >= MAXTIMES) {
            long[] temp  = new long[MAXTIMES];
            for (int i = 0; i < MAXTIMES/2; i++) temp[i] = itsLastTimestamps[MAXTIMES/2+i];
            itsLastTimestamps = temp;
            itsTimestampPos = MAXTIMES/2;
         }
	 
         long avg = getAvgPeriod();
         long last = getLastPeriod();
         long delay = getDelay();

         if (avg == 0 || last == 0) {
           itsNextEpoch = lastTimestamp + delay;
         } else if (Math.abs(avg - last) > delay) {
            if (itsLast < 0) itsLast = last;
            else if (Math.abs(itsLast - last) < last/10) itsTimestampPos = 0;
            itsNextEpoch = lastTimestamp + delay;
         } else itsNextEpoch =  avg + lastTimestamp;
      }
      itsCollecting = false;
      if (itsNextEpoch < AbsTime.factory().getValue() + itsTimeOffset) itsNextEpoch = AbsTime.factory().getValue();
      itsNextEpoch += 100000;
      itsNextEpoch += itsTimeOffset;
      fireEvents(pe);
   }

   public long getAvgPeriod()
   {
      if (itsTimestampPos < 2) return 0;
      long sum = 0;
      for (int i = 1; i < itsTimestampPos; i++) sum += itsLastTimestamps[i] - itsLastTimestamps[i-1];
      return (long)(sum/(itsTimestampPos-1));
   }
   
   public long getLastPeriod()
   {
      if (itsTimestampPos < 2) return 0;
      return (long)((itsLastTimestamps[itsTimestampPos-1]-itsLastTimestamps[itsTimestampPos-2]));
   }
   
   private void fireEvents(PointEvent pe)
   {
     Object[] listeners = itsPLList.getListenerList();
     for (int i = 0; i < listeners.length; i +=2) {
       if (listeners[i] == PointListener.class) {
	 if ((listeners[i+1] instanceof DataMaintainer && pe == null)
	     || pe != null) {
	   ((PointListener)listeners[i+1]).onPointEvent(this, pe);
	 }
       }
     }
   }

   public String getStringEquiv()
   {
      StringBuffer res = new StringBuffer();
      res.append('M');
      res.append(' ');
      if (itsNames.length > 1) {
         res.append('{');
	 for (int i = 0; i < itsNames.length - 1; i++) res.append(itsNames[i]+",");
	 res.append(itsNames[itsNames.length-1]+"}");
      } else res.append(itsNames[0]);
      res.append(' ');
      res.append('"');
      res.append(itsLongDesc);
      res.append('"');
      res.append(' ');
      res.append('"');
      res.append(itsShortDesc);
      res.append('"');
      res.append(' ');
      res.append(itsSource);
      res.append(' ');
      res.append(itsEnabled ? 'T' : 'F');
      res.append(' ');
      res.append(itsTransactionString);
      res.append(' ');
      res.append(itsTranslationString);
      res.append(' ');
      res.append(itsLimitsString);
      res.append(' ');
      if (itsArchiveString.length > 1) {
        res.append('{');
        for (int i = 0; i < itsArchiveString.length - 1; i++) res.append(itsArchiveString[i]+",");
          res.append(itsArchiveString[itsArchiveString.length-1]+"}");
      } else res.append(itsArchiveString[0]);
      res.append(' ');
      res.append(itsPeriod);
      return res.toString();
   }
}
