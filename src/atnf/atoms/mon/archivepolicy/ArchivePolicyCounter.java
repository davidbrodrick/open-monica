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
 * Archive every <i>Nth</i> update. This requires one argument which specifies the value
 * of N.
 * 
 * Example: <code>Counter-"10"</code> Updates every 10th sample.
 * 
 * @author Le Coung Ngyuen, David Brodrick
 */
public class ArchivePolicyCounter extends ArchivePolicy
{
  private int itsCycles = 0;

  private int itsRunningCycles = 0;

  public ArchivePolicyCounter(PointDescription parent, String[] args)
  {
    super(parent, args);
    itsCycles = Integer.parseInt(args[0]);
  }

  public boolean checkArchiveThis(PointData data)
  {
    boolean savenow = false;
    itsRunningCycles++;
    if (itsRunningCycles >= itsCycles) {
      savenow = true;
      itsRunningCycles = 0;
    }
    return savenow;
  }
}
