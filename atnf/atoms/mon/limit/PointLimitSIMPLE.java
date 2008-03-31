/**
 * Class: PointLimitSIMPLE
 * Description: A PointLimit class with simple lower and upper limits
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.limit;

import java.util.StringTokenizer;
import atnf.atoms.mon.*;

class PointLimitSIMPLE extends PointLimit
{
   private double itsLower = 0;
   private double itsUpper = 0;
   private boolean itsCheck = false;
   protected static String itsArgs[] = new String[]{"Limits - None",""};
   
   public PointLimitSIMPLE(String[] args)
   {
      if (args.length > 0) {
	 itsCheck = true;
	 itsLower = Double.parseDouble(args[0]);
	 itsUpper = Double.parseDouble(args[1]);
      }
   }
   
   /**
    * Checks the upper and lower limits.
    * Correct for integer type data, i.e. 1 is recognised as 1 and not
    * 1.0000000001
    **/ 
   public boolean checkLimits(PointData data)
   {
      if (!itsCheck) return true;

      Object myData = data.getData();
      if (!(myData instanceof Number) || !(myData instanceof Object[] &&
      ((Object[])myData)[0] instanceof Number)) return false;
      if (myData instanceof Object[]) return checkLimitsArray((Object[])myData);
      if (myData instanceof Integer || myData instanceof Long || myData
      instanceof Short || myData instanceof Byte) {
         int intData = ((Number)myData).intValue();
	 if (intData < (int)itsLower || intData > (int)itsUpper) return false;
         return true;
      }
      double doubleData = ((Number)myData).doubleValue();
      if (doubleData < itsLower || doubleData > itsUpper) return false;      
      return true;
   }
   
   /**
    * Goes through an array and checks their limits.
    * I'll figure out a better way of doing this later
    **/
   private boolean checkLimitsArray(Object[] data)
   {
      for (int i = 0; i < data.length; i++)
      {
	 if (data[i] instanceof Integer || data[i] instanceof Long || data[i]
	 instanceof Short || data[i] instanceof Byte) {
            int intData = ((Number)data[i]).intValue();
	    if (intData < (int)itsLower || intData > (int)itsUpper) return false;
	 } else {
	 double doubleData = ((Number)data[i]).doubleValue();
	 if (doubleData < itsLower || doubleData > itsUpper) return false;      
         }
      }
      return true;
   }         
}
