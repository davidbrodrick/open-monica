//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.datasource;

import atnf.atoms.time.AbsTime;
import atnf.atoms.mon.*;

/**
 * Used to simulate a data
 * @author Le Cuong Nguyen
 * @version $Id: DataSourceDummy.java,v 1.2 2004/11/19 01:21:46 bro764 Exp $
 **/
class DataSourceDummy
extends DataSource
{
  public DataSourceDummy(String[] args)
  {
    super(args[0]);
  }
   
  protected
  void
  getData(Object[] points)
  throws Exception
  {
    //Precondition
    if (points==null || points.length==0) {
      return;
    }

    Object[] buf = null;
    for (int i=0; i<buf.length; i++) {
      PointMonitor pm = (PointMonitor)buf[i];
      pm.firePointEvent(new PointEvent(this, new
         PointData(pm.getName(), pm.getSource(),
         AbsTime.factory(), null), true));
    }
  }
}
