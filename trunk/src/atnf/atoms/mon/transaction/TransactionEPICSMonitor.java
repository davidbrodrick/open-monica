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
import gov.aps.jca.dbr.DBRType;
import org.apache.log4j.Logger;

/**
 * Subscribe to updates via the EPICS Channel Access monitor mechanism. This Transaction
 * requires the name of the Process Variable to be monitored as an argument.
 * 
 * <P>
 * If you need to read the data as a specific data type, then the DBRType can be specified
 * as an additional argument, eg "DBR_STS_STRING". Doing this at the STS level also
 * provides MoniCA with additional information such as the record's alarm severity and
 * allows UNDefined values to be recognised. If you do not specify a DBRType then
 * operations will be performed using the channel's native type, at the STS level.
 * 
 * @author David Brodrick
 */
public class TransactionEPICSMonitor extends Transaction {
  /** The EPICS Process Variable we need to monitor. */
  private String itsPV;

  /** The DBRType to be collected, or null to take the default type. */
  private DBRType itsType;

  /** Constructor which registers the point for EPICS monitor updates. */
  public TransactionEPICSMonitor(PointDescription parent, String[] args) {
    super(parent, args);

    // Replace the macro $1 with source name if present
    if (args[0].indexOf("$1") != -1) {
      args[0] = MonitorUtils.replaceTok(args[0], parent.getSource());
    }
    // Record name of the PV to monitor
    itsPV = args[0].trim();

    // Get the data type to used, if specified
    if (args.length > 1) {
      if (!args[1].equals("-")) {
        itsType = DBRType.forName(args[1].trim());
      }
    }

    EPICS es = (EPICS) ExternalSystem.getExternalSystem("EPICS");
    if (es == null) {
      Logger logger = Logger.getLogger(this.getClass().getName());
      logger.error("(" + itsParent.getFullName() + "): EPICS ExternalSystem is not running!");
    } else {
      es.registerMonitor(parent, itsPV, itsType);
    }
  }

  /** Return the DBRType to use for input, if specified. */
  public DBRType getType() {
    return itsType;
  }

  /** Specify the DBRType to use. */
  public void setType(DBRType type) {
    itsType = type;
  }
}
