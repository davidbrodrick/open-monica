/**
 * Class: ArchivePolicyCHANGE
 * Description: Saves data when it has changed
 * @author: Le Cuong Nguyen
 **/
package atnf.atoms.mon.archivepolicy;

import java.lang.reflect.*;
import atnf.atoms.mon.*;

public class ArchivePolicyCHANGE extends ArchivePolicy
{
   Object itsLastSaveData = null;

   protected static String itsArgs[] = new String[]{"Data Changed","CHANGE"};
   
   public ArchivePolicyCHANGE(String args)
   {
   }
   
   public boolean newData(PointData data)
   {
      Object newData = data.getData();
      if (newData == null && itsLastSaveData == null) {
         itsSaveNow = false;
	 return itsSaveNow;
      }
      if (itsLastSaveData == null) {
         itsLastSaveData = newData;
	 itsSaveNow = true;
	 return itsSaveNow;
      } else if (newData == null) {
         itsLastSaveData = null;
	 itsSaveNow = true;
	 return itsSaveNow;
      }
      try {
	 Method equalsMethod = newData.getClass().getMethod("equals",new Class[]{Object.class});
	 Object res = equalsMethod.invoke(newData, new Object[]{itsLastSaveData});
	 itsSaveNow = !((Boolean)res).booleanValue();
	 itsLastSaveData = newData;
      }
      catch (Exception e) {e.printStackTrace();}
      return itsSaveNow;
   }
   
   public static String[] getArgs()
   {
      return itsArgs;
   }
}
