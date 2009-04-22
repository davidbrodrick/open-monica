/**
 * Class: MonitorTimer
 * Description: A simple threaded Timer
 * There is an approx. 30ms delay in the construction of the first
 * instance of this object
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.EventListenerList;

public class MonitorTimer implements Runnable
{
   private long itsPeriod;
   private long itsStarted;
   private EventListenerList itsListeners = new EventListenerList();
   private static Thread itsThread = null;
   private MonitorTimer itsNextTimer = null;
   private MonitorTimer itsLastTimer = null;
   private static MonitorTimer itsFirstTimer = null;
   private boolean itsRepeats = false;
   private volatile static Object itsLock = new Object();
   private boolean itsRun = false;
   private long itsNextExecTime = 0;
   private static MonitorTimer[] itsWorkers = new MonitorTimer[1];
         
   /**
    * The period (in ms) of this Timer
    **/
   public long getPeriod()
   {
      return itsPeriod;
   }

   public boolean isRunning()
   {
      return itsRun;
   }

   public void setRunning(boolean flag)
   {
      itsRun = flag;
   }

   private synchronized static void newThread()
   {
      for (int i = 0; i < itsWorkers.length; i++) {
         if (itsWorkers[i] == null) {
	    itsWorkers[i] = new MonitorTimer();
            Thread temp = new Thread(itsWorkers[i], "MonitorTimer Worker "+i);
            temp.setDaemon(true);
            temp.start();
	 }
      }
   }
   
   /**
    * When this Timer was started using standard Java time conventions
    **/
   public long getStartedTime()
   {
      return itsStarted;
   }

   /**
    * This is for internel use in the linked list, don't mess with it
    **/
   public MonitorTimer getLastTimer()
   {
      return itsLastTimer;
   }

   /**
    * For the linked list
    **/
   public MonitorTimer getNextTimer()
   {
      return itsNextTimer;
   }

   /**
    * The time of this Timer will run next
    **/
   public long getNextExecTime()
   {
      return itsNextExecTime;
   }
   
   /**
    * Should this Timer restart after firing?
    **/
   public boolean getRepeats()
   {
      return itsRepeats;
   }
   
   /**
    * Sets the period
    **/
   public void setPeriod(int period)
   {
      itsPeriod = period;
   }

   /**
    * Sets the period
    **/
   public void setPeriod(long period)
   {
      itsPeriod = period;
   }
   
   /**
    * Mainly intended for internal use, but you can specify when the
    * Timer started.
    **/
   public void setStartedTime(long start)
   {
      itsStarted = start;
   }

   /**
    * Which method to run when Timer is fired?
    **/
   public void addActionListener(ActionListener listener)
   {
      itsListeners.add(ActionListener.class, listener);
   }

   /**
    * Don't run this actionListener when Timer is fired anymore
    **/
   public void removeActionListener(ActionListener listener)
   {
      itsListeners.remove(ActionListener.class, listener);
   }

   /**
    * Fire an event, do the stuff in the listeners
    **/
   public void fireActionEvent(ActionEvent ae)
   {
      Object[] listeners = itsListeners.getListenerList();
      for (int i = 0; i < listeners.length; i +=2) {
        if (listeners[i] == ActionListener.class) {
          ((ActionListener)listeners[i+1]).actionPerformed(ae);
        }
      }
   }

   /**
    * Again for the linked list
    **/
   public void setLastTimer(MonitorTimer timer)
   {
      itsLastTimer = timer;
   }

   /**
    * Perhaps linked lists are not the best way to go?
    **/
   public void setNextTimer(MonitorTimer timer)
   {
      itsNextTimer = timer;
   }
   
   /**
    * Tells the timer to reset after firing
    **/
   public void setRepeats(boolean repeats)
   {
      itsRepeats = repeats;
   }
   
   /**
    * New Timer with defaults - Repeats: false, no period, no listeners
    **/
   public MonitorTimer()
   {
   }

   /**
    * Uses a similar constructor to the javax.swing.Timer, so that you can
    * simply find/replace them.
    **/
   public MonitorTimer(int period, ActionListener listener)
   {
      itsPeriod = period;
      addActionListener(listener);
   }

   /**
    * The full constructor.
    **/
   public MonitorTimer(int period, ActionListener listener, boolean repeats)
   {
      itsPeriod = period;
      addActionListener(listener);
      itsRepeats = repeats;
   }
   
   /**
    * Puts the timer into the queue, starts it. Use when Timer has not
    * been fully initialised
    **/
   public void start(int period, ActionListener listener)
   {
      if (itsWorkers[0] == null) {
        newThread();
      }
      itsPeriod = period;
      addActionListener(listener);
      start();
   }
   
   /**
    * starts the Timer
    **/
   public void start()
   {
      // This is in case the main Timer has not been initialised yet.
      if (itsWorkers[0] == null) {
        newThread();
      }
      if (itsRun) {
         stop();
	 start();
	 return;
      }
      itsRun = true;
      itsStarted = System.currentTimeMillis();
      itsNextExecTime = itsStarted + itsPeriod;
      addTimer(this);
   }
   
   /**
    * Removes the timer
    **/
   public void stop()
   {
      if (!itsRun) {
        return;
      }
      if (itsWorkers[0] == null) {
        newThread();
      }
      itsRun = false;
      removeTimer(this);
   }

   
   public void addTimer(MonitorTimer timer)
   {
      synchronized(itsLock) {
         MonitorTimer prevTimer = null;
	 MonitorTimer nextTimer = itsFirstTimer;
	 
	 long nextExecTime = timer.getNextExecTime();

	 nextTimer = itsFirstTimer;

	 // Go through the list, find the first timer with exec time greater than
	 // the new timer's exec time.
	 while (nextTimer != null) {
	    if (nextTimer.getNextExecTime() > nextExecTime) {
        break;
      }
	    prevTimer = nextTimer;
	    nextTimer = nextTimer.getNextTimer();
	 }

	 if (prevTimer == null) {
	    // Add to start of list
	    itsFirstTimer = timer;
	    itsLock.notifyAll();
	 } else {
    prevTimer.setNextTimer(timer);
  }
      }
   }
   
   public void removeTimer(MonitorTimer timer)
   {
      if (timer == null) {
        return;
      }
      synchronized(itsLock) {
         MonitorTimer nextTimer = itsFirstTimer;
	 MonitorTimer prevTimer = null;
         // Search for the timer in the queue
	 while (nextTimer != null) {
	    if (nextTimer == timer) {
	       if (prevTimer == null) {
	          // remove first timer
	          itsFirstTimer = timer.getNextTimer();
		  timer.setLastTimer(null);
		  timer.setNextTimer(null);
		  itsLock.notifyAll();
		  break;
	       } else {
          prevTimer.setNextTimer(nextTimer.getNextTimer());
        }
               if (nextTimer.getNextTimer() != null) {
                nextTimer.getNextTimer().setLastTimer(prevTimer);
              }
	       timer.setLastTimer(null);
	       timer.setNextTimer(null);
	       break;
	    }
	    // Look at next in list
	    prevTimer = nextTimer;
	    if (nextTimer.getNextTimer() == nextTimer) {
	       System.out.println("Something is terribly wrong!");
	       System.exit(1);
	    }
	    nextTimer = nextTimer.getNextTimer();
	 }
      }
   }

   public void run()
   {
      MonitorTimer timer = null;
      boolean runnow = false;

      while (true) {
         try {
	    synchronized(itsLock) {
	       // Wait until we get a timer
               while (itsFirstTimer == null) {
                itsLock.wait();
              }
	       // Wait until we have to run the first timer
	       // Careful, because of the waits, the first timer may be not what
	       // we expected.
	       if (itsFirstTimer == null) {
          continue;
        }
	       long waitTime = itsFirstTimer.getNextExecTime() - System.currentTimeMillis();
	       runnow = (waitTime <= 0);
	       if (runnow) {
        	  timer = itsFirstTimer;
		  timer.stop();
	       } else {
          itsLock.wait(waitTime);
        }
	    }
            if (timer == null) {
              continue;
            }
	    if (!runnow) {
        continue;
      }
	    
            synchronized(timer) {
               // Run the timer
	       timer.fireActionEvent(new ActionEvent(timer, 0, "timer"));
               // Repeat if necessary
	       if (timer.getRepeats()) {
          timer.start();
        }
	    }
	 } catch (Exception e) {e.printStackTrace();}
      }
   }
}
