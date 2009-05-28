//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.transaction;

import java.util.*;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Generic transaction for DataSources which require strings to retrieve the
 * relevant data. The first string argument for the Transaction must be the
 * channel/protocol that corresponds to the appropriate ExternalSystem. Any
 * subsequent arguments are arbitrary strings that are made available for
 * use by the ExternalSystem.
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class TransactionStrings
extends Transaction
{
  /** The strings required to update the monitor point. */
  Vector<String> itsStrings = new Vector<String>();

  protected static String itsArgs[] = new String[]{"", "", "", ""};

  public TransactionStrings(PointDescription parent, String specifics)
  {
    super(parent, specifics);

    String[] tokens=MonitorUtils.getTokens(specifics);
    assert tokens.length>1;

    //Replace the macro $1 with source name if present
    if (tokens[0].indexOf("$1")!=-1) {
      tokens[0]=MonitorUtils.replaceTok(tokens[1], parent.getSource());
    }
    //Set the channel (used to determine which ExternalSystem to use)
    setChannel(tokens[0]);

    //Add the remaining strings to our list
    for (int i=2; i<tokens.length; i++) {
      itsStrings.add(tokens[i].trim());
    }
  }
    
  /** Return the first string. */
  public String
  getString()
  {
    return (String)itsStrings.get(0);
  }

  /** Return the string at the specified index. */
  public String
  getString(int i)
  {
    return (String)itsStrings.get(i);
  }

  /** Return the number of strings. */
	public int
	getNumStrings()
	{
	  return itsStrings.size();
	}

  public static String[] getArgs()
  {
     return itsArgs;
  }
}
