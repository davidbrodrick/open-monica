/**
 * Class: ArchivePolicyCOUNTER
 * Description: determines whether archiving should be done based
 *              upon a running count of PointMonitors
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.archivepolicy;

import atnf.atoms.mon.*;

public class ArchivePolicyCOUNTER extends ArchivePolicy
{
   private int itsCycles = 0;
   private int itsRunningCycles = 0;
   protected static String itsArgs[] = new String[]{"Counter","COUNTER","Count","java.lang.Integer"};
   
   public ArchivePolicyCOUNTER(String args)
   {
      itsCycles = Integer.parseInt(args);
   }
   
   public ArchivePolicyCOUNTER(int cycles)
   {
      itsCycles = cycles;
   }
   
   public boolean checkArchiveThis(PointData data)
   {
      itsRunningCycles++;
      if (itsRunningCycles >= itsCycles) {
         itsSaveNow = true;
	 itsRunningCycles = 0;
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
