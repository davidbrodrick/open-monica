//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import java.util.*;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;

/**
 * This class reads data from a Thycon UPS with a PAC4(?) interface.
 * 
 * Your monitor-sources.txt file will need an entry like:<BR>
 * <tt>ThyconUPS caups:6000</tt>
 * 
 * <P>
 * The corresponding monitor-points.txt entries should be something like this: <tt>
 * hidden.ups.alldata "Generator Hut UPS Data"  ""            ""     site  T  Generic-"caups:6000"              -   -                             -               -           10000000 -
 * power.ups.V1IN    "Input Voltage, Phase 1"   "V1IN"        "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"V1IN", NumDecimals-"1"}  Range-"220""250" {COUNTER-6} 10000000 -
 * power.ups.V2IN    "Input Voltage, Phase 2"   "V2IN"        "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"V2IN", NumDecimals-"1"}  Range-"220""250" {COUNTER-6} 10000000 -
 * power.ups.V3IN    "Input Voltage, Phase 3"   "V3IN"        "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"V3IN", NumDecimals-"1"}  Range-"220""250" {COUNTER-6} 10000000 -
 * power.ups.I1IN    "Input Current, Phase 1"   "I1IN"        "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"I1IN", NumDecimals-"1"}  Range-"20""70"   {COUNTER-6} 10000000 -
 * power.ups.I2IN    "Input Current, Phase 2"   "I2IN"        "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"I2IN", NumDecimals-"1"}  Range-"20""70"   {COUNTER-6} 10000000 -
 * power.ups.I3IN    "Input Current, Phase 3"   "I3IN"        "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"I3IN", NumDecimals-"1"}  Range-"20""70"   {COUNTER-6} 10000000 -
 * power.ups.P1IN_R  "Real Input Power, Phase 1"      ""      "kW"   site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P1INW",  NumDecimals-"1"} Range-"5""18"   {COUNTER-6} 10000000 -
 * power.ups.P1IN_I  "Complex Input Power, Phase 1"   ""      "kVA"  site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P1INVA", NumDecimals-"1"} Range-"5""18"   {COUNTER-6} 10000000 -
 * power.ups.P2IN_R  "Real Input Power, Phase 2"      ""      "kW"   site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P2INW",  NumDecimals-"1"} Range-"5""18"   {COUNTER-6} 10000000 -
 * power.ups.P2IN_I  "Complex Input Power, Phase 2"   ""      "kVA"  site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P2INVA", NumDecimals-"1"} Range-"5""18"   {COUNTER-6} 10000000 -
 * power.ups.P3IN_R  "Real Input Power, Phase 3"      ""      "kW"   site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P3INW",  NumDecimals-"1"} Range-"5""18"   {COUNTER-6} 10000000 -
 * power.ups.P3IN_I  "Complex Input Power, Phase 3"   ""      "kVA"  site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P3INVA", NumDecimals-"1"} Range-"5""18"   {COUNTER-6} 10000000 -
 * power.ups.V1OUT   "Output Voltage, Phase 1"  "V1OUT"       "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"V1OUT", NumDecimals-"1"} Range-"220""250" {COUNTER-6} 10000000 -
 * power.ups.V2OUT   "Output Voltage, Phase 2"  "V2OUT"       "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"V2OUT", NumDecimals-"1"} Range-"220""250" {COUNTER-6} 10000000 -
 * power.ups.V3OUT   "Output Voltage, Phase 3"  "V3OUT"       "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"V3OUT", NumDecimals-"1"} Range-"220""250" {COUNTER-6} 10000000 -
 * power.ups.I1OUT   "Output Current, Phase 1"  "I1OUT"       "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"I1OUT", NumDecimals-"1"} Range-"30""70"   {COUNTER-6} 10000000 -
 * power.ups.I2OUT   "Output Current, Phase 2"  "I2OUT"       "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"I2OUT", NumDecimals-"1"} Range-"30""70"   {COUNTER-6} 10000000 -
 * power.ups.I3OUT   "Output Current, Phase 3"  "I3OUT"       "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"I3OUT", NumDecimals-"1"} Range-"30""70"   {COUNTER-6} 10000000 -
 * power.ups.P1OUT_R "Real Ouput Power, Phase 1"       ""     "kW"   site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P1OUT",   NumDecimals-"1"} Range-"5""18"  {COUNTER-6} 10000000 -
 * power.ups.P1OUT_I "Complex Output Power, Phase 1"   ""     "kVA"  site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P1OUTVA", NumDecimals-"1"} Range-"5""18"  {COUNTER-6} 10000000 -
 * power.ups.P2OUT_R "Real Ouput Power, Phase 2"       ""     "kW"   site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P2OUT",   NumDecimals-"1"} Range-"5""18"  {COUNTER-6} 10000000 -
 * power.ups.P2OUT_I "Complex Output Power, Phase 2"   ""     "kVA"  site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P2OUTVA", NumDecimals-"1"} Range-"5""18"  {COUNTER-6} 10000000 -
 * power.ups.P3OUT_R "Real Ouput Power, Phase 3"       ""     "kW"   site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P3OUT",   NumDecimals-"1"} Range-"5""18"  {COUNTER-6} 10000000 -
 * power.ups.P3OUT_I "Complex Output Power, Phase 3"   ""     "kVA"  site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"P3OUTVA", NumDecimals-"1"} Range-"5""18"  {COUNTER-6} 10000000 -
 * power.ups.DCBusV  "DC Bus Voltage"                  ""     "V"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"DCBUSV",  NumDecimals-"1"} Range-"480""500" {COUNTER-3} 10000000 -
 * power.ups.BattI   "Battery Current"                 ""     "A"    site  T  Listen-"site.hidden.ups.alldata"  -   {NV-"BATTI",   NumDecimals-"1"} Range-"-5""5"    {COUNTER-3} 10000000 -
 * power.ups.CB1     "Circuit Breaker 1 (Q1), 125V DC, 125A"  "CB1"  ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"CB1"               -               {CHANGE-}   10000000 -
 * power.ups.CB2     "Circuit Breaker 2 (Q2), 125V DC, 125A"  "CB2"  ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"CB2"               -               {CHANGE-}   10000000 -
 * power.ups.CB3     "Circuit Breaker 3 (Q4), 250V AC, 125A"  "CB3"  ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"CB3"               -               {CHANGE-}   10000000 -
 * power.ups.CB4     "Circuit Breaker 4 (Q6), Static Switch"  "CB4"  ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"CB4"               -               {CHANGE-}   10000000 -
 * power.ups.CB7     "Circuit Breaker 7 (QSR)"                "CB7"  ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"CB7"               -               {CHANGE-}   10000000 -
 * power.ups.OutageTime   "Last Outage Time"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"OUTTIM"                   -               {CHANGE-}   10000000 -
 * power.ups.OutagePeriod "Last Outage Duration"    ""        "sec"  site  T  Listen-"site.hidden.ups.alldata"  -   NV-"OUTDUR"                   -               {CHANGE-}   10000000 -
 * power.ups.OutageNum    "Number of Outages"       ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"OUTNUM"                   -               {CHANGE-}   10000000 -
 * power.ups.OutageTotal  "Total Outage Duration"   ""        "sec"  site  T  Listen-"site.hidden.ups.alldata"  -   NV-"OUTTOT"                   -               {CHANGE-}   10000000 -
 * power.ups.DischTime   "Last Discharge Time"      ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"DISCTIM"                  -               {CHANGE-}   10000000 -
 * power.ups.DischPeriod "Last Discharge Duration"  ""        "sec"  site  T  Listen-"site.hidden.ups.alldata"  -   NV-"DISCDUR"                  -               {CHANGE-}   10000000 -
 * power.ups.DischNum    "Number of Discharges"     ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"DISCNUM"                  -               {CHANGE-}   10000000 -
 * power.ups.DischTotal  "Total Discharge Duration" ""        "sec"  site  T  Listen-"site.hidden.ups.alldata"  -   NV-"DISCTOT"                  -               {CHANGE-}   10000000 -
 * power.ups.AlarmTime  "Last Alarm Time"           ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"ALRMTIM"                  -               {CHANGE-}   10000000 -
 * power.ups.AlarmMsg   "Last Alarm Message"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"ALRMMSG"                  -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg1   "Status Message 1"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD1"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg2   "Status Message 2"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD2"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg3   "Status Message 3"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD3"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg4   "Status Message 4"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD4"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg5   "Status Message 5"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD5"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg6   "Status Message 6"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD6"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg7   "Status Message 7"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD7"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg8   "Status Message 8"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD8"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg9   "Status Message 9"        ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD9"                     -               {CHANGE-}   10000000 -
 * power.ups.StatusMsg10  "Status Message 10"       ""        ""     site  T  Listen-"site.hidden.ups.alldata"  -   NV-"LCD10"                     -              {CHANGE-}   10000000 -
 * </tt>
 *
 * @author Simon Hoyle
 * @author David Brodrick
 */
class ThyconUPS extends ASCIISocket
{
    /** THYNET address of the UPS. */
    protected byte itsAddress = 0x20;

    final static byte[] INVOLTS = { 0x00 };

    final static byte[] INCUR = { 0x01 };

    final static byte[] INPOW = { 0x02 };

    final static byte[] INPERIOD = { 0x03 };

    final static byte[] OUTVOLTS = { 0x04 };

    final static byte[] OUTCUR = { 0x05 };

    final static byte[] OUTPOW = { 0x06 };

    final static byte[] OUTPERIOD = { 0x07 };

    final static byte[] BYPERIOD = { 0x0B };

    final static byte[] DCBUS = { 0x0F };

    final static byte[] STATUS = { 0x10 };

    final static byte[] BATTHIST = { 0x19 };

    final static byte[] ALRMMSG = { 0x1B };

    static Hashtable<Integer, String> alarmMsgMap = new Hashtable<Integer, String>(300);

    static String[] suffixMsgs;

    static String[] transferAbortMsgs;

    /**
     * Initialisation Block
     */
    static {
        alarmMsgMap.put(new Integer(0), "SYSTEM COLD START");
        alarmMsgMap.put(new Integer(1), "SYSTEM WARM START");
        alarmMsgMap.put(new Integer(2), "CPU POWER FAIL");
        alarmMsgMap.put(new Integer(3), "CPU CHECKSUM ERROR");
        alarmMsgMap.put(new Integer(10), "INPUT VOLTS");
        alarmMsgMap.put(new Integer(14), "INPUT FREQUENCY");
        alarmMsgMap.put(new Integer(17), "PHASE IMBALANCE");
        alarmMsgMap.put(new Integer(20), "BYPASS VOLTS");
        alarmMsgMap.put(new Integer(27), "BYPASS FREQUENCY");
        alarmMsgMap.put(new Integer(40), "RECT");
        alarmMsgMap.put(new Integer(41), "RECT INPUT Q1");
        alarmMsgMap.put(new Integer(42), "RECT FAULT");
        alarmMsgMap.put(new Integer(43), "DC LEAKAGE");
        alarmMsgMap.put(new Integer(45), "CURRENT LIMIT");
        alarmMsgMap.put(new Integer(46), "RECT OVERLOAD");
        alarmMsgMap.put(new Integer(47), "RECT OVER-CURRENT");
        alarmMsgMap.put(new Integer(49), "RECT OVER-VOLTAGE");
        alarmMsgMap.put(new Integer(58), "DC FLOAT VOLTS HIGH");
        alarmMsgMap.put(new Integer(59), "DC FLOAT VOLTS LOW");
        alarmMsgMap.put(new Integer(60), "DC DISCHARGED");
        alarmMsgMap.put(new Integer(61), "DC CAPACITY LOW");
        alarmMsgMap.put(new Integer(62), "DC DISCHARGING");
        alarmMsgMap.put(new Integer(64), "END DISCHARGE");
        alarmMsgMap.put(new Integer(67), "DC RECHARGING");
        alarmMsgMap.put(new Integer(69), "DC ON BOOST");
        alarmMsgMap.put(new Integer(71), "DC BOOST STOPPED");
        alarmMsgMap.put(new Integer(72), "DC AT FLOAT");
        alarmMsgMap.put(new Integer(73), "END BOOST");
        alarmMsgMap.put(new Integer(77), "DC Q2");
        alarmMsgMap.put(new Integer(78), "DC CURRENT");
        alarmMsgMap.put(new Integer(79), "DC 5 MIN WARNING");
        alarmMsgMap.put(new Integer(80), "NV TRANSIENT");
        alarmMsgMap.put(new Integer(82), "INV S/STATE");
        alarmMsgMap.put(new Integer(86), "INV FREQ");
        alarmMsgMap.put(new Integer(88), "INV SYNCH");
        alarmMsgMap.put(new Integer(94), "INV OUTPUT Q3");
        alarmMsgMap.put(new Integer(110), "Q6");
        alarmMsgMap.put(new Integer(116), "LOAD FAULT");
        alarmMsgMap.put(new Integer(120), "UPS TRANSIENT");
        alarmMsgMap.put(new Integer(121), "UPS TRANSIENT");
        alarmMsgMap.put(new Integer(122), "UPS S/STATE");
        alarmMsgMap.put(new Integer(126), "UPS OVERLOAD");
        alarmMsgMap.put(new Integer(127), "OVERLOAD SHUTDOWN");
        alarmMsgMap.put(new Integer(128), "INPUT OVERLOAD");
        alarmMsgMap.put(new Integer(132), "OUTPUT FAIL");
        alarmMsgMap.put(new Integer(140), "EMERGENCY STOP");
        alarmMsgMap.put(new Integer(141), "QSR");
        alarmMsgMap.put(new Integer(142), "BYPASS Q4");
        alarmMsgMap.put(new Integer(143), "UPS FAN");
        alarmMsgMap.put(new Integer(144), "MOT");
        alarmMsgMap.put(new Integer(145), "BRIDGE OVER-TEMP");
        alarmMsgMap.put(new Integer(147), "QSR FAILED TO CLOSE");
        alarmMsgMap.put(new Integer(148), "QSR FAILED TO OPEN");
        alarmMsgMap.put(new Integer(170), "END BOOST - Q2 OPEN");
        alarmMsgMap.put(new Integer(171), "END BOOST - RECT OFF");
        alarmMsgMap.put(new Integer(172), "BOOST ALREADY ON");
        alarmMsgMap.put(new Integer(173), "BOOST ACTIVATED");
        alarmMsgMap.put(new Integer(174), "AUTO START COMPLETE");
        alarmMsgMap.put(new Integer(175), "MANUAL BOOST");
        alarmMsgMap.put(new Integer(176), "DC BOOST STOPPED");
        alarmMsgMap.put(new Integer(177), "EXTENDED SYNC SELECTED");
        alarmMsgMap.put(new Integer(178), "NORMAL SYNC SELECTED");
        alarmMsgMap.put(new Integer(179), "INTERNAL OSC");
        alarmMsgMap.put(new Integer(180), "AMBIENT TEMPERATURE");
        alarmMsgMap.put(new Integer(181), "VENTILATION FAILURE");
        alarmMsgMap.put(new Integer(182), "ABORTED - Q2 OPEN");
        alarmMsgMap.put(new Integer(183), "GENERATOR SUPPLY");
        alarmMsgMap.put(new Integer(184), "STOP RECTIFIER REQUEST");
        alarmMsgMap.put(new Integer(185), "START RECTIFIER REQUEST");
        alarmMsgMap.put(new Integer(186), "AUTO START FAIL");
        alarmMsgMap.put(new Integer(188), "AUTO START REQUEST");
        alarmMsgMap.put(new Integer(189), "AUTO SHUTDOWN REQUEST");
        alarmMsgMap.put(new Integer(190), "TRANSFER TO INV REQUEST");
        alarmMsgMap.put(new Integer(191), "TRANSFER TO BYPASS REQUEST");
        alarmMsgMap.put(new Integer(192), "RECT ALREADY ON");
        alarmMsgMap.put(new Integer(193), "INPUT OUT OF TOLERANCE");
        alarmMsgMap.put(new Integer(194), "RECT FAILED TO START");
        alarmMsgMap.put(new Integer(195), "CANNOT CLOSE Q1");
        alarmMsgMap.put(new Integer(196), "Q1 TRIPPED");
        alarmMsgMap.put(new Integer(197), "INV FAILED TO START");
        alarmMsgMap.put(new Integer(198), "TRIP Q2 REQUESTED");
        alarmMsgMap.put(new Integer(199), "Q2 FAILED TO OPEN");
        alarmMsgMap.put(new Integer(200), "TRANSFER ABORTED");
        alarmMsgMap.put(new Integer(201), "AUTO TRANSFER REQUEST");
        alarmMsgMap.put(new Integer(210), "USER STATUS 1");
        alarmMsgMap.put(new Integer(211), "USER STATUS 2");
        alarmMsgMap.put(new Integer(212), "USER STATUS 3");
        alarmMsgMap.put(new Integer(213), "USER STATUS 4");
        alarmMsgMap.put(new Integer(214), "USER STATUS 5");
        alarmMsgMap.put(new Integer(215), "USER STATUS 6");
        alarmMsgMap.put(new Integer(255), "NULL ALARM");

        String[] sfxMsgs = { "", " LOW", " HIGH", " OFF", " ON", " OPEN/OFF", " CLOSED/ON", " FAIL", " OK", " WARN", " NORMAL" };
        suffixMsgs = sfxMsgs;

        String[] abrtMsgs = { "TRANSFER ABORTED - ON INV", "TRANSFER ABORTED - INV OFF", "TRANSFER ABORTED - Q6 OPEN",
                "TRANSFER ABORTED - Q3 OPEN", "ABORTED - UPS OVERLOAD", "ABORTED - NOT IN SYNC", "ABORTED - Q4 NO CLOSE",
                "ABORTED - Q3 NO CLOSE", "ABORTED - Q1 TRIP", "ABORTED - Q4 NO OPEN", "TRANSFER ABORTED - ON BYPASS",
                "BYPASS NOT IN TOLERANCE", "ABORTED - Q3 NO OPEN", "ABORTED - Q6 CLOSED", "ABORTED - Q3 TRIP",
                "ABORTED - Q3 NO OPEN", "ABORTED - Q3 NO CLOSE" };
        transferAbortMsgs = abrtMsgs;
    }

    /**
     * Constructor.
     */
    public ThyconUPS(String[] args)
    {
        super(args);
    }

    /** Convert the nibble to its BCD equivalent. */
    private static String nibble2BCD(byte nibble)
    {
        switch ((nibble) & 0x0F) {
        case 0:
            return "0";
        case 1:
            return "1";
        case 2:
            return "2";
        case 3:
            return "3";
        case 4:
            return "4";
        case 5:
            return "5";
        case 6:
            return "6";
        case 7:
            return "7";
        case 8:
            return "8";
        case 9:
            return "9";
        case 10:
            return "A";
        case 11:
            return "B";
        case 12:
            return "C";
        case 13:
            return "D";
        case 14:
            return "E";
        case 15:
            return "F";
        default:
            return "?"; // Can't happen
        }
    }

    /** Convert the byte to its BCD equivalent. */
    private static String byte2BCD(byte b)
    {
        return nibble2BCD((byte) (b >> 4)) + nibble2BCD(b);
    }

    /** Convert the BCD digit into its numeric equivalent. */
    private static byte BCD2byte(byte b)
    {
        if (b == '0')
            return 0;
        else if (b == '1')
            return 1;
        else if (b == '2')
            return 2;
        else if (b == '3')
            return 3;
        else if (b == '4')
            return 4;
        else if (b == '5')
            return 5;
        else if (b == '6')
            return 6;
        else if (b == '7')
            return 7;
        else if (b == '8')
            return 8;
        else if (b == '9')
            return 9;
        else if (b == 'a')
            return 10;
        else if (b == 'b')
            return 11;
        else if (b == 'c')
            return 12;
        else if (b == 'd')
            return 13;
        else if (b == 'e')
            return 14;
        else if (b == 'f')
            return 15;
        else if (b == 'A')
            return 10;
        else if (b == 'B')
            return 11;
        else if (b == 'C')
            return 12;
        else if (b == 'D')
            return 13;
        else if (b == 'E')
            return 14;
        else
            return 15;
    }

    /** Convert a Thycon date into an AbsTime. */
    private AbsTime toAbsTime(byte a, byte b, byte c, byte d, byte e, byte f, byte g, byte h, byte i, byte j, byte k, byte l)
    {
        int year = 2000 + BCD2byte(b) + BCD2byte(a) * 10;
        int mon = BCD2byte(d) + BCD2byte(c) * 10 - 1;
        int day = BCD2byte(f) + BCD2byte(e) * 10;
        int hr = BCD2byte(h) + BCD2byte(g) * 10;
        int min = BCD2byte(j) + BCD2byte(i) * 10;
        int sec = BCD2byte(l) + BCD2byte(k) * 10;

        GregorianCalendar cal = new GregorianCalendar(year, mon, day, hr, min, sec);
        cal.setTimeZone(SimpleTimeZone.getTimeZone("AEST"));
        return AbsTime.factory(cal.getTime());
    }

    /** Convert a Thycon floating point number into a Java Integer. */
    private Integer toInt(byte a, byte b, byte c, byte d)
    {
        int i1 = BCD2byte(b) + (BCD2byte(a) << 4) + (BCD2byte(d) << 8) + (BCD2byte(c) << 12);
        return new Integer(i1);
    }

    /** Convert a Thycon floating point number into a Java Float. */
    private Float toFloat(byte a, byte b, byte c, byte d)
    {
        int i1 = BCD2byte(b) + (BCD2byte(a) << 4) + (BCD2byte(d) << 8) + (BCD2byte(c) << 12);
        if (i1 == 0)
            return new Float(0.0);

        int e = (i1 & 0x7c00) >> 10; // exponent
        float f = ((i1 & 0x03ff) / 1024.0f) + 1.0f;

        // System.out.println("e=" + e + "\tf=" + f);
        if (e > 0 && e < 31)
            e -= 8;
        else if (e == 31)
            e = 255;
        // else if (e==0 && f!=0) {
        // e = 127 - 8;
        // f = f<<1;
        // }

        float res = (float) (f * Math.pow(2, e));
        // System.out.println("Float value=" + res);
        return new Float(res);
    }

    /** Test the response packet for valid format and checksum. */
    private boolean checkResponse(String resp)
    {
        if (resp == null) {
            System.err.println("ThyconUPS::checkResponse: Null response from UPS");
            return false;
        }
        if (resp.length() < 5) {
            System.err.println("ThyconUPS::checkResponse: Invalid response from UPS: " + resp);
            return false;
        }

        byte[] bytes = resp.getBytes();

        if (bytes[0] != '!') {
            System.err.println("ThyconUPS::checkResponse: Invalid response from UPS: " + resp);
            return false;
        }

        // /Still needs to verify checksum

        return true;
    }

    /**
     * Send a request packet using the given request code. If a valid response is obtained
     * it will be returned as a String, otherwise an Exception will be thrown.
     */
    private String sendRequest(byte[] req) throws Exception
    {
        if (!itsConnected)
            throw new Exception("Not connected to UPS");

        // Build request array of bytes
        byte[] allbytes = new byte[req.length + 2];
        allbytes[0] = itsAddress;
        for (int i = 0; i < req.length; i++)
            allbytes[i + 1] = req[i];
        // Calculate checksum
        byte checksum = 0;
        for (int i = 0; i < allbytes.length; i++)
            checksum = (byte) (checksum + allbytes[i]);
        checksum = (byte) (-checksum);
        allbytes[allbytes.length - 1] = checksum;
        // Convert to string
        String reqstr = "!";
        for (int i = 0; i < allbytes.length; i++)
            reqstr = reqstr + byte2BCD(allbytes[i]);
        reqstr = reqstr + "\r";
        // System.err.println("ThyconUPS: Request is: \t" + reqstr);
        itsWriter.write(reqstr);
        itsWriter.flush();

        String line = null;
        try {
            line = itsReader.readLine();
        } catch (Exception e) {
            System.err.println("ThyconUPS: " + AbsTime.factory().toString() + " " + e.toString());
            System.err.println("\t\tRequest is: \t" + reqstr);
        }
        if (!checkResponse(line))
            throw new Exception("Invalid response from UPS!");

        // System.err.println("ThyconUPS: Response is:\t" + line);
        return line;
    }

    /**
     * Extract meaningful information from a response packet from the UPS and insert the
     * information into the HashMap.
     */
    private void parseResponse(String resp, HashMap<String, Object> map)
    {
        byte[] bytes = resp.getBytes();

        if (bytes[3] == '8' && bytes[4] == '0') {
            // Input 3 phase voltage
            map.put("V1IN", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("V2IN", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
            map.put("V3IN", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
        } else if (bytes[3] == '8' && bytes[4] == '1') {
            // Input 3 phase current
            map.put("I1IN", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("I2IN", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
            map.put("I3IN", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
        } else if (bytes[3] == '8' && bytes[4] == '2') {
            // Input 3 phase power
            map.put("P1INW", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("P2INW", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
            map.put("P3INW", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
            map.put("P1INVA", toFloat(bytes[17], bytes[18], bytes[19], bytes[20]));
            map.put("P2INVA", toFloat(bytes[21], bytes[22], bytes[23], bytes[24]));
            map.put("P3INVA", toFloat(bytes[25], bytes[26], bytes[27], bytes[28]));
        } else if (bytes[3] == '8' && bytes[4] == '3') {
            // Input period
        } else if (bytes[3] == '8' && bytes[4] == '4') {
            // Output 3 phase voltage
            map.put("V1OUT", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("V2OUT", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
            map.put("V3OUT", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
        } else if (bytes[3] == '8' && bytes[4] == '5') {
            // Output 3 phase current
            map.put("I1OUT", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("I2OUT", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
            map.put("I3OUT", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
            /*
             * System.out.println((new AbsTime()).toString(AbsTime.Format.UTC_STRING) +
             * "\t" + toFloat(bytes[5], bytes[6], bytes[7], bytes[8]) + "\t" +
             * toFloat(bytes[9], bytes[10], bytes[11], bytes[12]) + "\t" +
             * toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
             */
        } else if (bytes[3] == '8' && bytes[4] == '6') {
            // Output 3 phase power
            map.put("P1OUT", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("P2OUT", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
            map.put("P3OUT", toFloat(bytes[13], bytes[14], bytes[15], bytes[16]));
            map.put("P1OUTVA", toFloat(bytes[17], bytes[18], bytes[19], bytes[20]));
            map.put("P2OUTVA", toFloat(bytes[21], bytes[22], bytes[23], bytes[24]));
            map.put("P3OUTVA", toFloat(bytes[25], bytes[26], bytes[27], bytes[28]));
        } else if (bytes[3] == '8' && bytes[4] == 'F') {
            // DC bus data
            map.put("DCBUSV", toFloat(bytes[5], bytes[6], bytes[7], bytes[8]));
            map.put("BATTI", toFloat(bytes[9], bytes[10], bytes[11], bytes[12]));
        } else if (bytes[3] == '9' && bytes[4] == '0') {
            // Status message
            // byte 1: NB bit0 not defined, start at bit1
            int b = (BCD2byte(bytes[5]) << 4) + BCD2byte(bytes[6]);
            if ((b & 2) == 0)
                map.put("CB1", new Boolean(false));
            else
                map.put("CB1", new Boolean(true));
            if ((b & 4) == 0)
                map.put("CB2", new Boolean(false));
            else
                map.put("CB2", new Boolean(true));
            if ((b & 8) == 0)
                map.put("CB3", new Boolean(false));
            else
                map.put("CB3", new Boolean(true));
            if ((b & 16) == 0)
                map.put("CB4", new Boolean(false));
            else
                map.put("CB4", new Boolean(true));
            if ((b & 32) == 0)
                map.put("CB5", new Boolean(false));
            else
                map.put("CB5", new Boolean(true));
            if ((b & 64) == 0)
                map.put("CB6", new Boolean(false));
            else
                map.put("CB6", new Boolean(true));
            if ((b & 128) == 0)
                map.put("CB7", new Boolean(false));
            else
                map.put("CB7", new Boolean(true));

            map.put("LCD1", "-"); // Initialise status strings to default values
            map.put("LCD2", "-");
            map.put("LCD3", "-");
            map.put("LCD4", "-");
            map.put("LCD5", "-");
            map.put("LCD6", "-");
            map.put("LCD7", "-");
            map.put("LCD8", "-");
            map.put("LCD9", "-");
            map.put("LCD10", "-");

            b = (BCD2byte(bytes[9]) << 4) + BCD2byte(bytes[10]);
            int lcd = 1;
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "UPS online - normal");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "Load on UPS power");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "Load on BYPASS power");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "Input supply off tolerance");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "Input frequency off tolerance");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "Input phase current imbalance");
                lcd++;
            }
            if ((b & 64) != 0) {
                map.put("LCD" + lcd, "Generator online");
                lcd++;
            }
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Bypass supply off tolerance");
                lcd++;
            }

            b = (BCD2byte(bytes[11]) << 4) + BCD2byte(bytes[12]);
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "Bypass frequency off tolerance");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "Inverter is off");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "Inverter is on");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "Inverter supply off tolerance");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "Inverter frequency off tolerance");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "Inverter/bypass not in sync");
                lcd++;
            }
            if ((b & 64) != 0) {
                map.put("LCD" + lcd, "Inverter DC current excess");
                lcd++;
            }
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Inverter on internal frequency reference");
                lcd++;
            }

            b = (BCD2byte(bytes[13]) << 4) + BCD2byte(bytes[14]);
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "UPS supply off tolerance");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "UPS frequency off tolerance");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "UPS in overload");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "UPS supply fail");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "Rectifier is off");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "Rectifier is on");
                lcd++;
            }
            if ((b & 64) != 0) {
                map.put("LCD" + lcd, "Rectifier tripped overcurrent");
                lcd++;
            }
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Rectifier in overload");
                lcd++;
            }

            b = (BCD2byte(bytes[15]) << 4) + BCD2byte(bytes[16]);
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "Rectifier overvoltage DC high");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "Rectifier has fault");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "Rectifier DC earth leak");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "Batteries on boost");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "Battery voltage high");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "Battery discharging");
                lcd++;
            }
            if ((b & 64) != 0) {
                map.put("LCD" + lcd, "Battery low warning");
                lcd++;
            }
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Battery discharged");
                lcd++;
            }

            b = (BCD2byte(bytes[17]) << 4) + BCD2byte(bytes[18]);
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "Battery recharging");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "Battery overcurrent");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "Manual boost selected");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "Boost requested - pending");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "Boost suspended");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "Emergenct stop on");
                lcd++;
            }
            if ((b & 64) != 0) {
                map.put("LCD" + lcd, "Magnetics overtemp");
                lcd++;
            }
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Converter overtemp");
                lcd++;
            }

            b = (BCD2byte(bytes[19]) << 4) + BCD2byte(bytes[20]);
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "Equipment Fan fail");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "Ambient overtemp");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "SSW Q6 open");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "SSW overtemp");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "Input supply available");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "Bypass supply available");
                lcd++;
            }
            if ((b & 64) != 0) {
                map.put("LCD" + lcd, "SSW is on");
                lcd++;
            }
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Extended Sync active");
                lcd++;
            }

            b = (BCD2byte(bytes[21]) << 4) + BCD2byte(bytes[22]);
            // if ((b&1)!=0) {map.put("LCD"+lcd, "undefined"); lcd++;}
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "Interlock fail");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "Battery Q2 open");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "Ventilation failed");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "QSR closed");
                lcd++;
            }
            // if ((b&32)!=0) {map.put("LCD"+lcd, "undefined"); lcd++;}
            // if ((b&64)!=0) {map.put("LCD"+lcd, "undefined"); lcd++;}
            if ((b & 128) != 0) {
                map.put("LCD" + lcd, "Internal oscillator on when generator on");
                lcd++;
            }

            b = (BCD2byte(bytes[23]) << 4) + BCD2byte(bytes[24]);
            if ((b & 1) != 0) {
                map.put("LCD" + lcd, "User status 1 active");
                lcd++;
            }
            if ((b & 2) != 0) {
                map.put("LCD" + lcd, "User status 2 active");
                lcd++;
            }
            if ((b & 4) != 0) {
                map.put("LCD" + lcd, "User status 3 active");
                lcd++;
            }
            if ((b & 8) != 0) {
                map.put("LCD" + lcd, "User status 4 active");
                lcd++;
            }
            if ((b & 16) != 0) {
                map.put("LCD" + lcd, "User status 5 active");
                lcd++;
            }
            if ((b & 32) != 0) {
                map.put("LCD" + lcd, "User status 6 active");
                lcd++;
            }
            // if ((b&64)!=0) {map.put("LCD"+lcd, "undefined"); lcd++;}
            // if ((b&128)!=0) {map.put("LCD"+lcd, "undefined"); lcd++;}

        } else if (bytes[3] == '9' && bytes[4] == '8') {
            // Outage history
            map.put("OUTTIM", toAbsTime(bytes[5], bytes[6], bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12],
                    bytes[13], bytes[14], bytes[15], bytes[16]));
            map.put("OUTDUR", toInt(bytes[17], bytes[18], bytes[19], bytes[20]));
            map.put("OUTNUM", toInt(bytes[21], bytes[22], bytes[23], bytes[24]));
            map.put("OUTTOT", toInt(bytes[25], bytes[26], bytes[27], bytes[28]));
        } else if (bytes[3] == '9' && bytes[4] == '9') {
            // Battery discharge history
            map.put("DISCTIM", toAbsTime(bytes[5], bytes[6], bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12],
                    bytes[13], bytes[14], bytes[15], bytes[16]));
            map.put("DISCDUR", toInt(bytes[17], bytes[18], bytes[19], bytes[20]));
            map.put("DISCNUM", toInt(bytes[21], bytes[22], bytes[23], bytes[24]));
            map.put("DISCTOT", toInt(bytes[25], bytes[26], bytes[27], bytes[28]));
        } else if (bytes[3] == '9' && bytes[4] == 'B') {
            // Numbered Alarm Message - SAH 5-8-09
            // Note response format: !209B95YYMMDDHHMMSSEETTPPPP
            // where YYMMDDHHMMSS == date, time
            // EE == Hex string representation of decimal alarm code. eg "48" == 72 == "DC
            // AT
            // FLOAT"
            // TT == Hex string representation of decimal type code. eg "0A" == 10 ==
            // "NORM"
            // PPPP == Hex string representation of decimal parameter code - see below.
            // The meaning of the "95" following the "9B" is unknown.
            map.put("ALRMTIM", toAbsTime(bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14],
                    bytes[15], bytes[16], bytes[17], bytes[18]));
            int errNum = (BCD2byte(bytes[19]) << 4) + BCD2byte(bytes[20]);
            String msg = (String) alarmMsgMap.get(new Integer(errNum));
            if (msg == null)
                map.put("ALRMMSG", "UNRECOGNISED ALARM CODE: " + errNum);
            else {
                if (errNum == 64) {
                    // Parameter == MMSS
                    int mins = (BCD2byte(bytes[23]) << 4) + BCD2byte(bytes[24]);
                    int secs = (BCD2byte(bytes[25]) << 4) + BCD2byte(bytes[26]);
                    map.put("ALRMMSG", msg + " " + mins + " mins, " + secs + " secs");
                } else if (errNum == 73) {
                    // Parameter == xxXX, where xx == Low order byte, XX == High order
                    // byte
                    int mins = (BCD2byte(bytes[23]) << 4) + BCD2byte(bytes[24]) + (BCD2byte(bytes[25]) << 12)
                            + (BCD2byte(bytes[26]) << 8);
                    map.put("ALRMMSG", msg + " " + mins + " mins");
                } else if (errNum == 175) {
                    // Parameter == xxXX, where xx == Low order byte, XX == High order
                    // byte
                    int hours = (BCD2byte(bytes[23]) << 4) + BCD2byte(bytes[24]) + (BCD2byte(bytes[25]) << 12)
                            + (BCD2byte(bytes[26]) << 8);
                    map.put("ALRMMSG", msg + " " + hours + " hours");
                } else if (errNum == 200) {
                    // Parameter == xxXX, where xx == Low order byte, XX == High order
                    // byte
                    // Only 10 suffixes defined, so HOB is ignored.
                    int index = (BCD2byte(bytes[23]) << 4) + BCD2byte(bytes[24]);
                    if (index < transferAbortMsgs.length)
                        map.put("ALRMMSG", transferAbortMsgs[index]);
                    else
                        map.put("ALRMMSG", msg + " - UNRECOGNISED PARAMETER: " + index);
                } else {
                    int suffixNum = (BCD2byte(bytes[21]) << 4) + BCD2byte(bytes[22]);
                    if (suffixNum < suffixMsgs.length && suffixNum > 0)
                        map.put("ALRMMSG", msg + suffixMsgs[suffixNum]);
                    else
                        map.put("ALRMMSG", msg);
                }
            }
        }
    }

    /**
     * Do the actual network transactions and parse the output of the UPS into a HashMap
     * that can be used by other monitor points.
     */
    private HashMap<String, Object> getNewData() throws Exception
    {
        if (!itsConnected)
            throw new Exception("Not connected to UPS");

        HashMap<String, Object> res = new HashMap<String, Object>();
        String resp = null;
        try {
            parseResponse(sendRequest(INVOLTS), res);
            parseResponse(sendRequest(INCUR), res);
            parseResponse(sendRequest(INPOW), res);
            parseResponse(sendRequest(OUTVOLTS), res);
            parseResponse(sendRequest(OUTCUR), res);
            parseResponse(sendRequest(OUTPOW), res);
            resp = sendRequest(DCBUS);
            // System.out.println("DC Bus data:\n\t" + resp + "\n");
            parseResponse(resp, res);
            resp = sendRequest(STATUS);
            // System.out.println("Status:\n\t" + resp + "\n");
            parseResponse(resp, res);
            // parseResponse(sendRequest(STATUS), res);
            parseResponse(sendRequest(BATTHIST), res);
            resp = sendRequest(ALRMMSG);
            // System.out.println("Alarm Message:\n\t" + resp + "\n");
            parseResponse(resp, res);
        } catch (Exception e) {
            res = null;
            MonitorMap.logger.error("ThyconUPS: " + e.toString());
            e.printStackTrace();
        }
        return res;
    }

    public void getData(PointDescription[] points) throws Exception
    {
        // Increment transaction counter
        itsNumTransactions += points.length;

        // Try to get the new data and force a reconnect if the read times out
        HashMap<String, Object> newdata = null;
        try {
            if (itsConnected)
                newdata = getNewData();
        } catch (Exception e) {
            try {
                System.err.println("ThyconUPS: " + e.getMessage());
                disconnect();
            } catch (Exception f) {
            }
        }

        // If the response was null then there must have been a parse error
        // this tends to happen after a power glitch when the detector gets
        // power cycled and spits out a heap of rubbish characters that then
        // get buffered by the media converter. Let's force a reconnect and
        // make sure the buffer has been flushed.
        if (newdata == null) {
            try {
                System.err.println("ThyconUPS: Parse error..");
                disconnect();
            } catch (Exception e) {
            }
        }

        // Fire off the new data
        for (int i = 0; i < points.length; i++) {
            PointDescription pm = points[i];
            PointData pd = new PointData(pm.getFullName(), newdata);
            pm.firePointEvent(new atnf.atoms.mon.PointEvent(this, pd, true));
        }
    }

    public final static void main(String[] argv)
    {
        if (argv.length < 1) {
            System.err.println("Missing argument: Needs IP and Port of the UPS serial/ethernet converter");
            System.exit(1);
        }
        ThyconUPS ups = new ThyconUPS(argv);

        try {
            ups.connect();
            while (true) {
                // ups.parseResponse(ups.sendRequest(OUTCUR), new HashMap());
                // String resp = ups.sendRequest(ALRMMSG);
                // System.out.println(resp);
                HashMap res = ups.getNewData();
                System.out.println("HashMap=" + res);
                RelTime sleep = RelTime.factory(5000000);
                try {
                    sleep.sleep();
                } catch (Exception j) {
                }
                // HashMap res = ups.getNewData();
                // System.err.println("HashMap=" + res);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }
}
