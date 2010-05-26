/**
 * Class: ArchivePolicyCOUNTER
 * Description: determines whether archiving should be done based
 *              upon a running count of PointMonitors
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.archivepolicy;

import atnf.atoms.mon.*;

public class ArchivePolicyCounter extends ArchivePolicy {
  private int itsCycles = 0;

  private int itsRunningCycles = 0;

  protected static String itsArgs[] = new String[] { "Counter", "COUNTER", "Count", "java.lang.Integer" };

  public ArchivePolicyCounter(String args) {
    args = args.replace("\"", "");
    itsCycles = Integer.parseInt(args);
  }

  public ArchivePolicyCounter(int cycles) {
    itsCycles = cycles;
  }

  public boolean checkArchiveThis(PointData data) {
    itsRunningCycles++;
    if (itsRunningCycles >= itsCycles) {
      itsSaveNow = true;
      itsRunningCycles = 0;
    } else {
      itsSaveNow = false;
    }
    return itsSaveNow;
  }

  public static String[] getArgs() {
    return itsArgs;
  }
}
