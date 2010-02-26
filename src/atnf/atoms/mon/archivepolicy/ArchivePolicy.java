/**
 * Class: ArchivePolicy
 * Description: Specifies how Points are to be archived
 * @author Le Cuong Nguyen
 **/

package atnf.atoms.mon.archivepolicy;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import java.lang.reflect.*;

public abstract class ArchivePolicy extends MonitorPolicy
{
   protected int itsCount = 0;
   protected AbsTime itsLastSaveTimestamp = null;
   protected boolean itsSaveNow = false;
   protected boolean itsFirst = true;
   
   /** Override this. Use format fullname, shortname, argname, argtype, argname, argtype, ...
   */
   protected static String itsArgs[] = new String[]{"ArchivePolicy",""};
   
   protected ArchivePolicy()
   {
   }
   
   public static ArchivePolicy factory(String arg)
   {
      assert (arg!=null && arg.indexOf("-")!=-1) || arg.equalsIgnoreCase("null");
      if (arg.equalsIgnoreCase("null")) {
        arg= "-";
      }

      ArchivePolicy result = null;
      String specifics   = arg.substring(arg.indexOf("-") + 1);
      String type = arg.substring(0, arg.indexOf("-"));
      if (type == "" || type == null || type.length()<1) {
        type = "DUMMY";
      }
            
      try {
	 Constructor AP_con = Class.forName("atnf.atoms.mon.archivepolicy.ArchivePolicy"+type).getConstructor(new Class[]{String.class});
	 result = (ArchivePolicy)(AP_con.newInstance(new Object[]{specifics}));
      } catch (Exception e) {e.printStackTrace();result = new ArchivePolicyDUMMY("");}

      result.setStringEquiv(arg);

      return result;
   }
   
   // Override this method with the correct procedures
   public abstract boolean checkArchiveThis(PointData data);
       
   public static String[] getArgs()
   {
      return itsArgs;
   }
}
