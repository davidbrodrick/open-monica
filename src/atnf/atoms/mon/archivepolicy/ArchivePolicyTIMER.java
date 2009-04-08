/**
 * Class: ArchivePolicyTIMER
 * Description: determines whether archiving should be done based
 *              upon a a perioditic timer of new PointMonitors
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.archivepolicy;
import atnf.atoms.time.*;
import atnf.atoms.mon.*;

public class ArchivePolicyTIMER extends ArchivePolicy
{
   RelTime itsPeriod = null;
   AbsTime itsLastSaved = AbsTime.factory();
   protected static String[] itsArgs = new String[]{"Timer", "TIMER",
   "period", "java.lang.Integer"};

   public ArchivePolicyTIMER(String args)
   {
      itsPeriod = RelTime.factory(Integer.parseInt(args));
   }
      
   public ArchivePolicyTIMER(RelTime period)
   {
      itsPeriod = RelTime.factory(period);
   }

   public ArchivePolicyTIMER(int period)
   {
      itsPeriod = RelTime.factory(period);
   }
   
   public boolean newData(PointData data)
   {
      if (data.getTimestamp().isAfter(itsLastSaved.add(itsPeriod))) {
         itsSaveNow = true;
	 itsLastSaved = data.getTimestamp();
      } else {
        itsSaveNow = false;
      }
      return itsSaveNow;
   }
   
   public static String[] getArgs()
   {
      return itsArgs;
   }
}
