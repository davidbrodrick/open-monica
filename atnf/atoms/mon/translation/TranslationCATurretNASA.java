//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;

/**
 * Look at the monitor points that indicate the different turret positions
 * and return a string indicating the current turret position.
 *
 * This uses a different designation for the bands to suit NASA requirements.
 *
 * @author David Brodrick
 * @version $Id: TranslationCATurretNASA.java,v 1.1 2008/01/30 03:54:50 bro764 Exp $
 */
public class
TranslationCATurretNASA
extends Translation
{
  /** Name of the monitor point that indicates L/S feed state. */
  String itsLS = null;
  int itsLSstate = -1;
  /** Name of the monitor point that indicates C/X feed state. */
  String itsCX = null;
  int itsCXstate = -1;
  /** Name of the monitor point that indicates K/Q feed state. */
  String itsKQ = null;
  int itsKQstate = -1;
  /** Name of the monitor point that indicates W/F feed state. */
  String itsWF = null;
  int itsWFstate = -1;
  /** Name of the monitor point for the translator home switch. */
  String itsTranslator = null;
  int itsTranslatorState = -1;

  protected static String[] itsArgs = new String[]{
    "L/S Feed MP", "what goes here",
    "C/X Feed MP", "what goes here",
    "K/Q Feed MP", "what goes here",
    "W/F Feed MP", "what goes here",
    "Translator Home", "its a mystery"};

  public
  TranslationCATurretNASA(PointMonitor parent, String[] init)
  {
    super(parent, init);

    if (init==null||init.length<5) {
      //What to do?
      System.err.println("TranslationCATurret: NOT ENOUGH ARGUMENTS: for " +
			 parent.getSource() + "." + parent.getName());
    } else {
      //Record the names of the points we need to listen to
      itsLS = init[0];
      itsCX = init[1];
      itsKQ = init[2];
      itsWF = init[3];
      itsTranslator = init[4];

      //Substittude the name of our source if the $1 macro was used
      if (itsLS.indexOf("$1") > -1)
	itsLS = MonitorUtils.replaceTok(itsLS, parent.getSource());
      if (itsCX.indexOf("$1") > -1)
	itsCX = MonitorUtils.replaceTok(itsCX, parent.getSource());
      if (itsKQ.indexOf("$1") > -1)
	itsKQ = MonitorUtils.replaceTok(itsKQ, parent.getSource());
      if (itsWF.indexOf("$1") > -1)
	itsWF = MonitorUtils.replaceTok(itsWF, parent.getSource());
      if (itsTranslator.indexOf("$1") > -1)
	itsTranslator = MonitorUtils.replaceTok(itsTranslator, parent.getSource());
    }
  }


  /**  */
  public
  PointData
  translate(PointData data)
  {
    //Precondition
    if (data==null) return null;

    //Check that the data value is not null
    if (data.getData()==null) {
      //There is no data, can't calculate a new value
      //This may cause probs for some sub-classes??
      return null;
    }

    //Lets only produce an updated value when the "first" listened-to
    //point updates or else we will be producing 4 times too much data.
    boolean letsUpdate = false;
    if ((data.getSource()+"."+data.getName()).equals(itsLS)) {
      itsLSstate = ((Number)data.getData()).intValue();
      letsUpdate = true;
    } else if ((data.getSource()+"."+data.getName()).equals(itsCX))
      itsCXstate = ((Number)data.getData()).intValue();
    else if ((data.getSource()+"."+data.getName()).equals(itsKQ))
      itsKQstate = ((Number)data.getData()).intValue();
    else if ((data.getSource()+"."+data.getName()).equals(itsWF))
      itsWFstate = ((Number)data.getData()).intValue();
    else if ((data.getSource()+"."+data.getName()).equals(itsTranslator)) {
      String strstate = (String)data.getData();
      if (strstate.equals("K")) itsTranslatorState=0;
      else if (strstate.equals("Q")) itsTranslatorState=1;
      else itsTranslatorState=2;
    }
    if (!letsUpdate) return null;
    if (itsLSstate==-1) return null;
    if (itsCXstate==-1) return null;
    if (itsKQstate==-1) return null;
    if (itsWFstate==-1) return null;
    if (itsTranslatorState==-1) return null;

    String state;
    if (itsLSstate>0) state = "L/S";
    else if (itsCXstate>0) state = "X";
    else if (itsKQstate>0 && itsTranslatorState==0) state = "K";
    else if (itsKQstate>0 && itsTranslatorState==1) state = "Ka";
    else if (itsWFstate>0) state = "W";
    else state="off-axis";

    //System.err.println("CATurret: " + itsParent.getSource() + "\t" + state + " is on-axis");

    PointData res = new PointData(itsParent.getName(), itsParent.getSource());
    res.setData(state);
    return res;
  }

  public static String[] getArgs()
  {
     return itsArgs;
  }
}
