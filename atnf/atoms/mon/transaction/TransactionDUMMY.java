// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.*;

/**
 * A test transaction class, it actually gathers no information..
 *
 * @author Le Cuong Nguyen
 * @version $Id: TransactionDUMMY.java,v 1.1 2004/09/16 04:56:45 bro764 Exp $
 */
public class
TransactionDUMMY
extends Transaction
{
  protected static String itsArgs[] = new String[]{"Transaction - None",""};

  public TransactionDUMMY(PointInteraction parent, String specifics)
  {
    //Invoke c'tor of super-class
    super(parent, specifics);

    //Set the channel type
    setChannel("NONE");
    itsName = specifics;
    //Since this is the dummy, there is no info to extract from the string
  }

  public static String[] getArgs()
  {
     return itsArgs;
  }
}
