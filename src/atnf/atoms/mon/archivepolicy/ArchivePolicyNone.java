/**
 * Class: ArchivePolicyDUMMY
 * Description: A test class to test ArchivePolicy
 * @author Le Cuong Nguyen
 **/

package atnf.atoms.mon.archivepolicy;

import atnf.atoms.mon.*;

public class ArchivePolicyNone extends ArchivePolicy
{
   protected static String itsArgs[] = new String[]{"No Archiving", ""};
   
   public ArchivePolicyNone(String cmd)
   {
   }
   
   public boolean checkArchiveThis(PointData data)
   {
      return false;
   }
   
   public static String[] getArgs()
   {
      return itsArgs;
   }
}
