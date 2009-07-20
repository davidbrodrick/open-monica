// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.*;
import atnf.atoms.mon.externalsystem.*;
import atnf.atoms.mon.util.MonitorUtils;

/** Subscribe to updates via the EPICS Channel Access monitor mechanism. This Transaction
 * requires the name of the Process Variable to be monitored as an argument. 
 * @author David Brodrick */
public class TransactionEPICSMonitor extends Transaction {
  /** The EPICS Process Variable we need to monitor. */
  private String itsPV;
  
  /** Constructor which registers the point for EPICS monitor updates. */
  public TransactionEPICSMonitor(PointDescription parent, String specifics)
  {
    super(parent, specifics);

    String[] tokens=MonitorUtils.getTokens(specifics);
    //Replace the macro $1 with source name if present
    if (tokens[0].indexOf("$1")!=-1) {
      tokens[0]=MonitorUtils.replaceTok(tokens[0], parent.getSource());
    }
    //Record name of the PV to monitor
    itsPV = tokens[0].trim();

    EPICS es = (EPICS)ExternalSystem.getExternalSystem("EPICS");
    if (es==null) {
      MonitorMap.logger.error("TransactionEPICS (" + itsParent.getFullName() + "): EPICS ExternalSystem is not running!");
    } else {
      es.registerMonitor(parent, itsPV);
    }
  }
}
