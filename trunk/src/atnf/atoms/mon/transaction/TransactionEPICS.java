// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.util.MonitorUtils;
import gov.aps.jca.dbr.DBRType;

/**
 * Transaction which holds a process variable name for polling or putting values over
 * EPICS Channel Access. This Transaction requires the name of the Process Variable to be
 * polled as an argument.
 * 
 * <P>
 * If this Transaction is being used for output, or you need to read the data as a
 * specific type, then the DBRType can be specified as an additional argument, eg
 * "DBR_STRING".
 * @author David Brodrick
 */
public class TransactionEPICS extends Transaction {
  /** The EPICS Process Variable we need to monitor. */
  private String itsPV;

  /** The data type to use for channel access gets. */
  private DBRType itsType = null;

  /** Constructor which registers the point for EPICS monitor updates. */
  public TransactionEPICS(PointDescription parent, String specifics) {
    super(parent, specifics);
    // Set the channel (to find the EPICS ExternalSystem)
    setChannel("EPICS");

    String[] tokens = MonitorUtils.getTokens(specifics);

    // Replace the macro $1 with source name if present
    if (tokens[0].indexOf("$1") != -1) {
      tokens[0] = MonitorUtils.replaceTok(tokens[0], parent.getSource());
    }
    // Record name of the PV to communicate with
    itsPV = tokens[0].trim();

    // Get the data type to used, if specified
    if (tokens.length > 1) {
      itsType = DBRType.forName(tokens[1].replace('\"', ' ').trim());
    }
  }

  /** Return the name of the PV to use for this point. */
  public String getPVName() {
    return itsPV;
  }

  /** Return the DBRType to use. */
  public DBRType getType() {
    return itsType;
  }
}
