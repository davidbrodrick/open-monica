package atnf.atoms.mon.client;

import atnf.atoms.time.*;
import atnf.atoms.mon.comms.*;

/**
 * Thread that periodically estimates the error on the local clock. The result
 * of this calculation can be used to streamline communication with the server.
 * 
 * @author David Brodrick
 */
public class ClockErrorMonitor {
  /** Delay between time queries to the server. */
  private static RelTime theirInterval = RelTime.factory(30000000);

  /** Current estimate of the local clock error. */
  private static RelTime theirClockError = RelTime.factory(0);

  /** The thread polling the server. */
  private static ClockErrorMonitorThread theirThread;

  /** Start the polling thread if not already started. */
  public static void start() {
    if (theirThread == null) {
      theirThread = new ClockErrorMonitorThread();
    }
  }

  /** Return the current estimate of the clock error. */
  public static RelTime getClockError() {
    start();
    return theirClockError;
  }

  public static class ClockErrorMonitorThread extends Thread {
    public ClockErrorMonitorThread() {
      super("Clock Error Monitor");
      start();
    }

    /** Main loop for collection thread. */
    public void run() {
      while (true) {
        MoniCAClient server = MonClientUtil.getServer();
        try {
          if (server != null) {
            theirClockError = server.getClockError();
          }
        } catch (Exception e) {
        }
        try {
          theirInterval.sleep();
        } catch (Exception e) {
        }
      }
    }
  }
}
