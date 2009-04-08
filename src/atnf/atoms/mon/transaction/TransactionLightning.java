// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Retrieves data from the site lightning detector.
 *
 * @author David Brodrick
 * @version $Id: $
 **/

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.*;

public class TransactionLightning
extends Transaction
{
  protected static String itsArgs[] = new String[]{"Transaction - Lightning",
  "Lightning"};

  public TransactionLightning(PointMonitor parent, String specifics)
  {
    super(parent, specifics);

    setChannel("lightning://");
    itsName = specifics;
  }
}
