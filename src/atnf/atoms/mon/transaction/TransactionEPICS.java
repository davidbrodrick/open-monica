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
 * If this Transaction is being used for output/writes, or you need to read the data as a
 * specific data type, then the DBRType can be specified as an additional argument, eg
 * "DBR_STS_STRING". Doing this at the STS level also provides MoniCA with additional
 * information such as the record's alarm severity and allows UNDefined values to be
 * recognised. If you do not specify a DBRType then operations will be performed using the
 * channel's native type, at the STS level.
 * 
 * @author David Brodrick
 */
public class TransactionEPICS extends Transaction
{
  /** The EPICS Process Variable we need to monitor. */
  private String itsPV;

  /** The data type to use for channel access gets. */
  private DBRType itsType = null;

  /** Constructor which registers the point for EPICS monitor updates. */
  public TransactionEPICS(PointDescription parent, String[] args)
  {
    super(parent, args);

    // Set the channel (to find the EPICS ExternalSystem)
    setChannel("EPICS");

    // Replace the macro $1 with source name if present
    if (args[0].indexOf("$1") != -1) {
      args[0] = MonitorUtils.replaceTok(args[0], parent.getSource());
    }
    // Record name of the PV to communicate with
    itsPV = args[0].trim();

    // Get the data type to used, if specified
    if (args.length > 1) {
      itsType = DBRType.forName(args[1].trim());
    }
  }

  /** Return the name of the PV to use for this point. */
  public String getPVName()
  {
    return itsPV;
  }

  /** Return the DBRType to use. */
  public DBRType getType()
  {
    return itsType;
  }

  /** Specify the DBRType to use. */
  public void setType(DBRType type)
  {
    itsType = type;
  }
}
