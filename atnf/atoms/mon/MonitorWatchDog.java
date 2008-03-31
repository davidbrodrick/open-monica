// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Handles the watchdog for the entire monitoring
 * @author Le Cuong Nguyen
 **/
package atnf.atoms.mon;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import atnf.atoms.mon.util.*;

class MonitorWatchDog implements ActionListener
{
   private static HashSet itsHashSet = new HashSet();
   private static MonitorWatchDog itsMain = new MonitorWatchDog();
   static {
//      javax.swing.Timer itsTimer = new javax.swing.Timer(10000, itsMain);
      MonitorTimer itsTimer = new MonitorTimer(10000, itsMain);
      itsTimer.setRepeats(true);
      itsTimer.start();
   }
   private static Object[] itsLast = null;
   private static boolean itsDebug = false;
   static {
      Object db = MonitorConfig.getProperty("Debug");
      if (db != null && ((String)db).equalsIgnoreCase("true")) itsDebug = true;
      else itsDebug = false;
   }
   
   private MonitorWatchDog()
   {}
   
   public static void watch(ActionListener listener)
   {
      synchronized(itsHashSet) {
	 itsHashSet.add(listener);
      }
   }
   
   public static void ignore(ActionListener listener)
   {
      synchronized(itsHashSet) {
	 itsHashSet.remove(listener);
	 if (itsLast == null) return;
	 for (int i = 0; i < itsLast.length; i++)
            if (itsLast[i] == listener) itsLast[i] = null;
      }
   }
   
   public void actionPerformed(ActionEvent ae)
   {
      if (itsDebug) System.out.println("Event");
      ArrayList tempList = new ArrayList();
      synchronized(itsHashSet) {
	 if (itsLast == null) {
	    // Simple way of making a copy of the list
            itsLast = itsHashSet.toArray();
	    return;
	 }

         // If a point is on the list now and before, then there's a problem
	 for (int i = 0; i < itsLast.length; i++)
            if (itsLast[i] != null && itsHashSet.contains(itsLast[i])) tempList.add(itsLast[i]);
	 itsLast = itsHashSet.toArray();
      }
      Object[] tempArray = tempList.toArray();
      for (int i = 0; i < tempArray.length; i++) ((ActionListener)tempArray[i]).actionPerformed(null);
   }
}
