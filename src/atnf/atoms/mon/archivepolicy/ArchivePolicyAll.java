// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.archivepolicy;

import atnf.atoms.mon.*;

/**
 * Archives every data update.
 * 
 * @author David Brodrick
 */
public class ArchivePolicyAll extends ArchivePolicy
{
  public ArchivePolicyAll(PointDescription parent, String[] args)
  {
    super(parent, args);
  }

  public boolean checkArchiveThis(PointData data)
  {
    return true;
  }
}
