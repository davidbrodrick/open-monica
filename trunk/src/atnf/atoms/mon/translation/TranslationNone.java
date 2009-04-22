// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;

/**
 * Translation that does nothing but return the argument.
 * @author David Brodrick
 * @version $Id: TranslationNone.java,v 1.3 2004/10/06 23:19:38 bro764 Exp $
 **/
public class
TranslationNone
extends Translation
{
  protected static String itsArgs[] = new String[]{"Translation - None",""};

  public
  TranslationNone(PointDescription parent, String[] init)
  {
    super(parent, init);
  }


  /** Do the (null) translation. */
  public
  PointData
  translate(PointData data)
  {
    //Need to make new object with our parent as source/name
    PointData res = new PointData(itsParent.getName(), itsParent.getSource(),
				  data.getTimestamp(),
				  data.getRaw(), data.getData());
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }
}
