// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.archivepolicy;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;

/**
 * Periodic archiver. Argument must be the archive interval in integer seconds.
 * 
 * Example: <code>Timer-"60"</code> Archives data every 60 seconds.
 * 
 * @author Le Coung Nguyen, David Brodrick
 */
public class ArchivePolicyTimer extends ArchivePolicy
{
    /** The interval at which data should be saved. */
    RelTime itsPeriod = null;

    /** Timestamp for when data was last saved. */
    AbsTime itsLastSaved = AbsTime.factory();

    public ArchivePolicyTimer(String[] args)
    {
        itsPeriod = RelTime.factory(1000000l*Long.parseLong(args[0]));
    }

    public boolean checkArchiveThis(PointData data)
    {
        boolean savenow = false;
        if (data.getTimestamp().isAfter(itsLastSaved.add(itsPeriod))) {
            savenow = true;
            itsLastSaved = data.getTimestamp();
        }
        return savenow;
    }
}
