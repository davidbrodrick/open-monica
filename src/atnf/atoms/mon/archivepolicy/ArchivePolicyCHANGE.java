// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.archivepolicy;

import java.lang.reflect.*;
import atnf.atoms.mon.*;

/**
 * Archives data when the value changes.
 * 
 * @author: Le Cuong Nguyen
 */
public class ArchivePolicyCHANGE extends ArchivePolicy
{
    Object itsLastSaveData = null;

    protected static String itsArgs[] = new String[] { "Data Changed", "CHANGE" };

    public ArchivePolicyCHANGE(String args)
    {
    }

    public boolean newData(PointData data)
    {
        Object newData = data.getData();
        if (newData == null && itsLastSaveData == null) {
            itsSaveNow = false;
            return itsSaveNow;
        }
        if (itsLastSaveData == null) {
            itsLastSaveData = newData;
            itsSaveNow = true;
            return itsSaveNow;
        } else if (newData == null) {
            itsLastSaveData = null;
            itsSaveNow = true;
            return itsSaveNow;
        }
        try {
            Method equalsMethod = newData.getClass().getMethod("equals", new Class[] { Object.class });
            Object res = equalsMethod.invoke(newData, new Object[] { itsLastSaveData });
            itsSaveNow = !((Boolean) res).booleanValue();
            itsLastSaveData = newData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itsSaveNow;
    }

    public static String[] getArgs()
    {
        return itsArgs;
    }
}
