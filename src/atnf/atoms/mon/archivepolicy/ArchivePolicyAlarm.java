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
 * Archives data when the alarm flag is set.
 * 
 * @author David Brodrick
 */
public class ArchivePolicyAlarm extends ArchivePolicy
{
    public ArchivePolicyAlarm(String[] args)
    {
    }

    public boolean checkArchiveThis(PointData data)
    {
        return data.getAlarm();
    }
}
