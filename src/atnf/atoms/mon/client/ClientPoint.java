/**
 * Class: ClientPoint
 * Description: Data useful for the management of points on the client side
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.client;

import atnf.atoms.time.*;

public class ClientPoint
{
   protected static final int MAXTIMES = 255;
   protected static final long DEFAULTPERIOD = 1000000;
   protected String itsPointName = "";
   protected String itsSource = "";
   protected long[] itsLastTimestamps = new long[MAXTIMES];
   protected int itsTimestampPos = 0;
   protected long itsLast = -1;
      
   public ClientPoint(String name, String source)
   {
      itsPointName = name;
      itsSource = source;
   }
   
   public String getName()
   {
      return itsSource+"."+itsPointName;
   }
   
   public String getPointName()
   {
      return itsPointName;
   }
   
   public String getSource()
   {
      return itsSource;
   }

   public void setPointName(String pointname)
   {
      itsPointName = pointname;
   }
   
   public void setSource(String source)
   {
      itsSource = source;
   }
   
   public void addTimestamp(long timestamp)
   {
      itsLastTimestamps[itsTimestampPos] = timestamp;
      itsTimestampPos++;
      if (itsTimestampPos >= MAXTIMES) {
         long[] temp  = new long[MAXTIMES];
	 for (int i = 0; i < MAXTIMES/2; i++) {
    temp[i] = itsLastTimestamps[MAXTIMES/2+i];
  }
	 itsLastTimestamps = temp;
	 itsTimestampPos = MAXTIMES/2;
      }
   }
   
   public long getAvgPeriod()
   {
      if (itsTimestampPos < 2) {
        return 0;
      }
      long sum = 0;
      for (int i = 0; i < itsTimestampPos; i++) {
        sum += itsLastTimestamps[itsTimestampPos];
      }      
      return (sum/(itsTimestampPos+1));
   }
   
   public long getLastPeriod()
   {
      if (itsTimestampPos < 2) {
        return 0;
      }
      return ((itsLastTimestamps[itsTimestampPos-1]-itsLastTimestamps[itsTimestampPos-2])/2);
   }
   
   public long getNextCollectEpoch()
   {
      long now = (new AbsTime()).getValue();
      long avg = getAvgPeriod();
      long last = getLastPeriod();
      if (avg == 0 || last == 0) {
        return (now + DEFAULTPERIOD);
      }
      if (Math.abs(avg - last) > DEFAULTPERIOD) {
         if (itsLast < 0) {
          itsLast = last;
        } else if (Math.abs(itsLast - last) < last/10) {
    itsTimestampPos = 0;
  }
	 return (now + DEFAULTPERIOD);
      }
      return avg + now;
   }
}
