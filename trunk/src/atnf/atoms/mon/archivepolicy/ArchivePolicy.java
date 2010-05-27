// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.archivepolicy;

import atnf.atoms.time.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.translation.Translation;
import atnf.atoms.mon.translation.TranslationNone;
import atnf.atoms.mon.util.MonitorUtils;

import java.lang.reflect.*;

/**
 * Base class for ArchivePolicy classes, which determine when it is appropriate to archive
 * the data value of a point.
 * 
 * <P>
 * Subclasses need to implement the <code>checkArchiveThis</code> method, which returns
 * <code>true</code> if the value should be archived or <code>false</code> if this
 * value doesn't need to be archived.
 * 
 * @author Le Cuong Nguyen
 */
public abstract class ArchivePolicy extends MonitorPolicy
{
    protected ArchivePolicy()
    {
    }

    public static ArchivePolicy factory(PointDescription parent, String strdef)
    {
        // Enable use of "null" keyword
        if (strdef.equalsIgnoreCase("null")) {
            strdef = "-";
        }

        ArchivePolicy result = null;

        try {
            // Find the argument strings
            String argstring = strdef.substring(strdef.indexOf("-") + 1);
            String[] args = MonitorUtils.tokToStringArray(argstring);

            // Find the type of ArchivePolicy
            String type = strdef.substring(0, strdef.indexOf("-"));
            if (type == "" || type == null || type.length() < 1) {
                type = "None";
            }

            Constructor ctor;
            try {
                // Try to find class by assuming argument is full class name
                ctor = Class.forName(type).getConstructor(new Class[] { String[].class });
            } catch (Exception f) {
                // Supplied name was not a full path
                // Look in atnf.atoms.mon.archivepolicy package
                ctor = Class.forName("atnf.atoms.mon.archivepolicy.ArchivePolicy" + type).getConstructor(
                        new Class[] { String[].class });
            }
            result = (ArchivePolicy) (ctor.newInstance(new Object[] { args }));
        } catch (Exception e) {
            System.err.println("ArchivePolicy: Error Creating ArchivePolicy!!");
            System.err.println("\tFor Point: " + parent.getFullName());
            System.err.println("\tFor ArchivePolicy:   " + strdef);
            System.err.println("\tException: " + e);
            result = new ArchivePolicyNone(new String[] {});
        }

        result.setStringEquiv(strdef);

        return result;
    }

    /**
     * Override this method to implement the desired behaviour.
     * 
     * @param data The latest data which may or may not be archived.
     * @return True if the data should be archived, False is no archiving should take
     * place.
     */
    public abstract boolean checkArchiveThis(PointData data);
}
