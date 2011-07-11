//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * Round the Number input to the nearest Integer.
 * 
 * @author David Brodrick
 */
public class TranslationRoundToInt extends Translation
{
    public TranslationRoundToInt(PointDescription parent, String[] init)
    {
        super(parent, init);
    }

    public PointData translate(PointData data)
    {
        Object val = data.getData();
        if (val == null) {
            return data;
        }

        if (! (val instanceof Number)) {
            System.err.println("TranslationRoundToInt: " + itsParent.getFullName() + ": Got NON-NUMERIC data!");
            return null;
        }
        
        Integer intval = new Integer(new Double(Math.round(((Number)val).doubleValue())).intValue());
        return new PointData(itsParent.getFullName(), intval);
    }
}
