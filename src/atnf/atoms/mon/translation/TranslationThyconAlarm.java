//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.mon.translation.Translation;
import java.util.Hashtable;

/**
 * Maps an integer to the corresponding Thycon alarm message.
 * 
 * @author David Brodrick
 */
public class TranslationThyconAlarm extends Translation {
  /** Table of alarm codes. */
  static Hashtable<Integer, String> theirAlarmMap = new Hashtable<Integer, String>(300);

  /**
   * Initialisation Block
   */
  static {
    theirAlarmMap.put(new Integer(0), "SYSTEM COLD START");
    theirAlarmMap.put(new Integer(1), "SYSTEM WARM START");
    theirAlarmMap.put(new Integer(2), "CPU POWER FAIL");
    theirAlarmMap.put(new Integer(3), "CPU CHECKSUM ERROR");
    theirAlarmMap.put(new Integer(10), "INPUT VOLTS");
    theirAlarmMap.put(new Integer(14), "INPUT FREQUENCY");
    theirAlarmMap.put(new Integer(17), "PHASE IMBALANCE");
    theirAlarmMap.put(new Integer(20), "BYPASS VOLTS");
    theirAlarmMap.put(new Integer(27), "BYPASS FREQUENCY");
    theirAlarmMap.put(new Integer(40), "RECT");
    theirAlarmMap.put(new Integer(41), "RECT INPUT Q1");
    theirAlarmMap.put(new Integer(42), "RECT FAULT");
    theirAlarmMap.put(new Integer(43), "DC LEAKAGE");
    theirAlarmMap.put(new Integer(45), "CURRENT LIMIT");
    theirAlarmMap.put(new Integer(46), "RECT OVERLOAD");
    theirAlarmMap.put(new Integer(47), "RECT OVER-CURRENT");
    theirAlarmMap.put(new Integer(49), "RECT OVER-VOLTAGE");
    theirAlarmMap.put(new Integer(58), "DC FLOAT VOLTS HIGH");
    theirAlarmMap.put(new Integer(59), "DC FLOAT VOLTS LOW");
    theirAlarmMap.put(new Integer(60), "DC DISCHARGED");
    theirAlarmMap.put(new Integer(61), "DC CAPACITY LOW");
    theirAlarmMap.put(new Integer(62), "DC DISCHARGING");
    theirAlarmMap.put(new Integer(64), "END DISCHARGE");
    theirAlarmMap.put(new Integer(67), "DC RECHARGING");
    theirAlarmMap.put(new Integer(69), "DC ON BOOST");
    theirAlarmMap.put(new Integer(71), "DC BOOST STOPPED");
    theirAlarmMap.put(new Integer(72), "DC AT FLOAT");
    theirAlarmMap.put(new Integer(73), "END BOOST");
    theirAlarmMap.put(new Integer(77), "DC Q2");
    theirAlarmMap.put(new Integer(78), "DC CURRENT");
    theirAlarmMap.put(new Integer(79), "DC 5 MIN WARNING");
    theirAlarmMap.put(new Integer(80), "NV TRANSIENT");
    theirAlarmMap.put(new Integer(82), "INV S/STATE");
    theirAlarmMap.put(new Integer(86), "INV FREQ");
    theirAlarmMap.put(new Integer(88), "INV SYNCH");
    theirAlarmMap.put(new Integer(94), "INV OUTPUT Q3");
    theirAlarmMap.put(new Integer(110), "Q6");
    theirAlarmMap.put(new Integer(116), "LOAD FAULT");
    theirAlarmMap.put(new Integer(120), "UPS TRANSIENT");
    theirAlarmMap.put(new Integer(121), "UPS TRANSIENT");
    theirAlarmMap.put(new Integer(122), "UPS S/STATE");
    theirAlarmMap.put(new Integer(126), "UPS OVERLOAD");
    theirAlarmMap.put(new Integer(127), "OVERLOAD SHUTDOWN");
    theirAlarmMap.put(new Integer(128), "INPUT OVERLOAD");
    theirAlarmMap.put(new Integer(132), "OUTPUT FAIL");
    theirAlarmMap.put(new Integer(140), "EMERGENCY STOP");
    theirAlarmMap.put(new Integer(141), "QSR");
    theirAlarmMap.put(new Integer(142), "BYPASS Q4");
    theirAlarmMap.put(new Integer(143), "UPS FAN");
    theirAlarmMap.put(new Integer(144), "MOT");
    theirAlarmMap.put(new Integer(145), "BRIDGE OVER-TEMP");
    theirAlarmMap.put(new Integer(147), "QSR FAILED TO CLOSE");
    theirAlarmMap.put(new Integer(148), "QSR FAILED TO OPEN");
    theirAlarmMap.put(new Integer(170), "END BOOST - Q2 OPEN");
    theirAlarmMap.put(new Integer(171), "END BOOST - RECT OFF");
    theirAlarmMap.put(new Integer(172), "BOOST ALREADY ON");
    theirAlarmMap.put(new Integer(173), "BOOST ACTIVATED");
    theirAlarmMap.put(new Integer(174), "AUTO START COMPLETE");
    theirAlarmMap.put(new Integer(175), "MANUAL BOOST");
    theirAlarmMap.put(new Integer(176), "DC BOOST STOPPED");
    theirAlarmMap.put(new Integer(177), "EXTENDED SYNC SELECTED");
    theirAlarmMap.put(new Integer(178), "NORMAL SYNC SELECTED");
    theirAlarmMap.put(new Integer(179), "INTERNAL OSC");
    theirAlarmMap.put(new Integer(180), "AMBIENT TEMPERATURE");
    theirAlarmMap.put(new Integer(181), "VENTILATION FAILURE");
    theirAlarmMap.put(new Integer(182), "ABORTED - Q2 OPEN");
    theirAlarmMap.put(new Integer(183), "GENERATOR SUPPLY");
    theirAlarmMap.put(new Integer(184), "STOP RECTIFIER REQUEST");
    theirAlarmMap.put(new Integer(185), "START RECTIFIER REQUEST");
    theirAlarmMap.put(new Integer(186), "AUTO START FAIL");
    theirAlarmMap.put(new Integer(188), "AUTO START REQUEST");
    theirAlarmMap.put(new Integer(189), "AUTO SHUTDOWN REQUEST");
    theirAlarmMap.put(new Integer(190), "TRANSFER TO INV REQUEST");
    theirAlarmMap.put(new Integer(191), "TRANSFER TO BYPASS REQUEST");
    theirAlarmMap.put(new Integer(192), "RECT ALREADY ON");
    theirAlarmMap.put(new Integer(193), "INPUT OUT OF TOLERANCE");
    theirAlarmMap.put(new Integer(194), "RECT FAILED TO START");
    theirAlarmMap.put(new Integer(195), "CANNOT CLOSE Q1");
    theirAlarmMap.put(new Integer(196), "Q1 TRIPPED");
    theirAlarmMap.put(new Integer(197), "INV FAILED TO START");
    theirAlarmMap.put(new Integer(198), "TRIP Q2 REQUESTED");
    theirAlarmMap.put(new Integer(199), "Q2 FAILED TO OPEN");
    theirAlarmMap.put(new Integer(200), "TRANSFER ABORTED");
    theirAlarmMap.put(new Integer(201), "AUTO TRANSFER REQUEST");
    theirAlarmMap.put(new Integer(210), "USER STATUS 1");
    theirAlarmMap.put(new Integer(211), "USER STATUS 2");
    theirAlarmMap.put(new Integer(212), "USER STATUS 3");
    theirAlarmMap.put(new Integer(213), "USER STATUS 4");
    theirAlarmMap.put(new Integer(214), "USER STATUS 5");
    theirAlarmMap.put(new Integer(215), "USER STATUS 6");
    theirAlarmMap.put(new Integer(255), "NULL ALARM");
  }  
  
  public TranslationThyconAlarm(PointDescription parent, String[] init) {
    super(parent, init);
  }

  /** Perform the actual data translation. */
  public PointData translate(PointData data) {
    Object val = data.getData();
    if (val == null || !(val instanceof Number)) {
      // Return a null result
      return new PointData(itsParent.getFullName());
    }

    String message = theirAlarmMap.get(((Number)val).intValue());
    return new PointData(itsParent.getFullName(), data.getTimestamp(), message);
  }
}
