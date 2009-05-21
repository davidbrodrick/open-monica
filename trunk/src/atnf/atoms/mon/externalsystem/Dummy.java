//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.externalsystem;

import atnf.atoms.time.AbsTime;
import atnf.atoms.mon.*;

/**
 * Used to simulate a data
 * @author Le Cuong Nguyen
 * @version $Id: Dummy.java,v 1.2 2004/11/19 01:21:46 bro764 Exp $
 **/
class Dummy
extends ExternalSystem
{
  public Dummy(String[] args)
  {
    super(args[0]);
  }
   
  protected
  void
  getData(PointDescription[] points)
  throws Exception
  {
    Object[] buf = null;
    for (int i=0; i<buf.length; i++) {
      PointDescription pm = (PointDescription)buf[i];
      pm.firePointEvent(new PointEvent(this, new
         PointData(pm.getFullName(), AbsTime.factory(), null), true));
    }
  }
}
