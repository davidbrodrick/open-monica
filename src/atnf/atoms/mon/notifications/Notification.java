// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.notifications;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * @author David Brodrick
 */
public abstract class Notification
{
  protected String[] itsInit = null;

  protected PointDescription itsParent = null;

  protected Notification(PointDescription parent, String[] init)
  {
    itsInit = init;
    itsParent = parent;
  }

  /** Check if a notification needs to be issued and send it if so. */
  public abstract void checkNotify(PointData data);
}
