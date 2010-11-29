// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Set the channel to be the provided argument string, so that we don't need to define a
 * new Transaction class for channels which don't actually have ExternalSystem specific
 * fields.
 * 
 * @author David Brodrick
 */
public class TransactionGeneric extends Transaction
{
  public TransactionGeneric(PointDescription parent, String[] args)
  {
    super(parent, args);
    
    if (args.length < 1) {
      throw new IllegalArgumentException("Requires at least one argument");
    }
    
    // Replace the macro $1 with source name if present
    if (args[0].indexOf("$1") != -1) {
      args[0] = MonitorUtils.replaceTok(args[0], parent.getSource());
    }

    setChannel(args[0]);
  }
}
