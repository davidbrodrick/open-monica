/**
 * Class: MonitorNativeLinux
 * Description: A collection of native methods for use in the monitoring system
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.util;

public abstract class MonitorNativeLinux
{
   static {
      try {
         System.loadLibrary("MonitorNativeLinux");
      } catch (Exception e) {e.printStackTrace();}
   }
   
   public static native long getCPUTime();
   public static native long getCPUUserTime();
   public static native long getCPUSystemTime();
}
