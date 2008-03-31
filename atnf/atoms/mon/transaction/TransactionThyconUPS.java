// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import java.util.*;
import atnf.atoms.mon.PointInteraction;

/**
 * Pull data from a Thycon UPS.
 *
 * @author David Brodrick
 * @version $Id: $
 **/
public class TransactionThyconUPS
extends Transaction
{
  protected static String itsArgs[] = new String[]{"Transaction - Thycon UPS",
  "Thycon UPS"};

  public TransactionThyconUPS(PointInteraction parent, String specifics)
  {
    super(parent, specifics);
    setChannel("thyconups://");
    specifics = specifics.replace('\"','\0').trim();
    itsName = specifics;
  }
}
