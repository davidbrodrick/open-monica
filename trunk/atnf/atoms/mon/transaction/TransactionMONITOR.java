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
 * Provides communication with the monitoring system itself.
 * @author Le Cuong Nguyen
 * @version $Id: TransactionMONITOR.java,v 1.2 2004/10/29 01:59:15 bro764 Exp $
 **/
public class TransactionMONITOR
extends Transaction
{
  protected static String itsArgs[] = new String[]{"Transaction - Monitor",
  "MONITOR"};

  public TransactionMONITOR(PointInteraction parent, String specifics)
  {
    super(parent, specifics);
    setChannel("MONITOR");
    specifics = specifics.replace('\"','\0').trim();
    itsName = specifics;
  }
}
