// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.PointDescription;

/**
 * Pull data from a Thytec UPS.
 *
 * @author David Brodrick
 * @version $Id: TransactionThytecUPS.java,v 1.1 2008/02/28 03:16:24 bro764 Exp $
 **/
public class TransactionThytecUPS
extends Transaction
{
  protected static String itsArgs[] = new String[]{"Transaction - Thytec UPS",
  "Thytec UPS"};

  public TransactionThytecUPS(PointDescription parent, String specifics)
  {
    super(parent, specifics);
    setChannel("thytecups://");
  }
}
