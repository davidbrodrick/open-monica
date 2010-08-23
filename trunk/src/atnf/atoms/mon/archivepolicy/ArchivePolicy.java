// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archivepolicy;

import atnf.atoms.mon.*;

/**
 * Base class for ArchivePolicy classes, which determine when it is appropriate
 * to archive the data value of a point.
 * 
 * <P>
 * Subclasses need to implement the <code>checkArchiveThis</code> method,
 * which returns <code>true</code> if the value should be archived or
 * <code>false</code> if this value doesn't need to be archived.
 * 
 * @author Le Cuong Nguyen
 */
public abstract class ArchivePolicy
{
  /** The point that we are archiving. */
  protected PointDescription itsParent;
  
  /** Constructor. */
  protected ArchivePolicy(PointDescription parent, String[] args)
  {
    itsParent = parent;
  }

  /**
   * Override this method to implement the desired behaviour.
   * 
   * @param data The latest data which may or may not be archived.
   * @return True if the data should be archived, False is no archiving should
   * take place.
   */
  public abstract boolean checkArchiveThis(PointData data);
}
