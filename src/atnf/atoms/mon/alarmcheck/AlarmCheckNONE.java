//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.alarmcheck;

import atnf.atoms.mon.*;

/**
 * Trivial alarm checker which always indicates that data is okay.
 * @author David Brodrick
 */
public class AlarmCheckNONE extends AlarmCheck
{
    /** Constructor. */
    public AlarmCheckNONE(PointDescription parent, String[] args)
    {
      super(parent, args);
    }

    /** Always indicates that data is okay. */
    public void checkAlarm(PointData data)
    {
        return;
    }

}
