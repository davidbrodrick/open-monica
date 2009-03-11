// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Tests the timer I wrote 
 **/
package atnf.atoms.mon;

import java.awt.event.*;
import atnf.atoms.mon.util.*;

class MonitorTimerTest implements ActionListener
{
   public long itsStart;
   private String itsName = "";
   
   public MonitorTimerTest(String name) {itsName = name;}
   
   public void actionPerformed(ActionEvent ae)
   {
      System.out.println(itsName + " Done:\t\t"+(System.currentTimeMillis()-itsStart));
   }
   
   public static void main(String[] args)
   {
      MonitorTimerTest mtt = new MonitorTimerTest("one");
      MonitorTimerTest mtt2 = new MonitorTimerTest("two");
      MonitorTimerTest mtt3 = new MonitorTimerTest("three");
      MonitorTimerTest mtt4 = new MonitorTimerTest("four");
            
      mtt.itsStart = System.currentTimeMillis();
      mtt2.itsStart = System.currentTimeMillis();
      mtt3.itsStart = System.currentTimeMillis();
      mtt4.itsStart = System.currentTimeMillis();

      for (int i = 0; i < 50000; i++) {
         MonitorTimer me = new MonitorTimer(i,mtt);
	 me.start();
	 if ((i % 2) == 0) me.stop();
      }
      for (int i = 0; i < 50000; i++) {
         MonitorTimer me = new MonitorTimer(i*2,mtt2);
	 me.start();
	 if ((i % 4) == 0) me.stop();
      }
      for (int i = 0; i < 50000; i++) {
         MonitorTimer me = new MonitorTimer(i*3,mtt3);
	 if ((i % 3) == 0) me.start();
      }
      for (int i = 0; i < 1000; i++) {
      MonitorTimer me2 = new MonitorTimer(300, mtt4);
      me2.setRepeats(true);
      me2.start();}
      try {
         synchronized(mtt) {
	    mtt.wait(50000);
	 }
      } catch (Exception e) {}
   }
}
