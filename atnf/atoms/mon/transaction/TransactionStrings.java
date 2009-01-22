//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.transaction;

import java.util.*;
import atnf.atoms.mon.PointInteraction;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * Generic transaction for DataSources which require strings to retrieve the
 * relevant data. The first string argument for the Transaction must be the
 * channel/protocol that corresponds to the appropriate DataSource. Any
 * subsequent arguments are arbitrary strings that are made available for
 * use by the DataSource.
 *
 * @author David Brodrick
 * @version $Id: $
 */
public class TransactionStrings
extends Transaction
{
  /** The strings required to update the monitor point. */
  Vector itsStrings = new Vector();

  protected static String itsArgs[] = new String[]{"", "", "", ""};

  public TransactionStrings(PointInteraction parent, String specifics)
  {
    super(parent, specifics);

    String[] tokens=specifics.split("\"");
    assert tokens.length>2;

    //Replace the macro $1 with source name if present
    if (tokens[1].indexOf("$1")!=-1) {
      tokens[1]=MonitorUtils.replaceTok(tokens[1], parent.getSource());
    }
    //Set the channel (used to determine which DataSource to use)
    setChannel(tokens[1]);

    //Add the remaining strings to our list
    for (int i=3; i<tokens.length; i++) {
      String thisstring=tokens[i].trim();
      if (!thisstring.equals("")) {
        itsStrings.add(thisstring);
      }
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
