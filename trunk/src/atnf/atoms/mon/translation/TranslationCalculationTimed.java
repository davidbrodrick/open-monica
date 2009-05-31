//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.awt.event.ActionEvent;
import javax.swing.Timer;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.time.AbsTime;

/**
 * Subclass of TranslationCalculation which performs processing based on the period of
 * the parent point rather than triggering when listen-to points update.
 *
 * @author David Brodrick
 */
public class
TranslationCalculationTimed
extends TranslationCalculation
{
  /** Timer used to trigger processing. */
  protected Timer itsProcessTimer = null;
  
  public
  TranslationCalculationTimed(PointDescription parent, String[] init)
  {
    super(parent, init);
    
    itsProcessTimer = new Timer((int)(parent.getPeriod()/1000), this);
    itsProcessTimer.start();
  }
  
  /** Always returns false because we base trigger off timer, not off listened-to points. */
  protected
  boolean
  matchData()
  {
    return false;
  }

  /** Provide the current input values to the expression parser. */
  protected
  Object
  doCalculations()
  {
    Object res=null;
    boolean missingdata = false;
    
    for (int i=0; i<itsNumPoints; i++) {
      if (itsValues[i]==null || itsValues[i].getData()==null) {
        missingdata=true;
        break;
      }
      //Update the value for this variable
      String thisvar=""+((char)(('a')+i));
      itsParser.addVariableAsObject(thisvar,itsValues[i].getData());
    }

    if (!missingdata) {
      // Parse the expression using new values
      res = itsParser.getValueAsObject();

      // Check for parse error
      if (itsParser.hasError()) {
        System.err.println("TranslationCalculator(" + itsParent.getFullName() + ": " + itsParser.getErrorInfo());
      }
    }
    return res;
  }
  
  /** Called when timer expires. */
  public
  void
  actionPerformed(ActionEvent evt)
  {
    super.actionPerformed(evt);
    if (evt.getSource()==itsProcessTimer) {
      //It's time to perform the calculation and fire an update of the point
      Object resval = doCalculations();
      PointData res = new PointData(itsParent.getFullName(), new AbsTime(), resval);
      itsParent.firePointEvent(new PointEvent(this, res, true));
    }
  }
}
