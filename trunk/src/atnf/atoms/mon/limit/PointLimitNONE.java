//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.limit;

import atnf.atoms.mon.PointData;

/** Trivial alarm checker which always indicates that data is okay. 
 * @author David Brodrick */
public class PointLimitNONE extends PointLimit {
  /** Constructor. */
  public PointLimitNONE(String[] args)
  {
    //Do nothing
  }
  
  /** Always indicates that data is okay. */
  public void checkLimits(PointData data) {
    return;
  }

}
