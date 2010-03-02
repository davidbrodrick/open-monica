//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.limit;

import java.lang.reflect.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;

/**
 * Base-class for classes which indicate if a point is in an alarm state.
 * <P>
 * Sub-classes must implement an appropriate <i>checkLimits</i> method. <i>checkLimits</i>
 * should set the PointData's alarm field to True when an alarm condition is detected or
 * leave the field unchanged otherwise.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public abstract class PointLimit extends MonitorPolicy
{
    public static PointLimit factory(String arg)
    {
        if (arg.equalsIgnoreCase("null")) {
            arg = "-";
        }

        PointLimit result = null;

        // Find the specific informations
        String specifics = arg.substring(arg.indexOf("-") + 1);
        String[] limitArgs = MonitorUtils.tokToStringArray(specifics);
        // Find the type of translation
        String type = arg.substring(0, arg.indexOf("-"));
        if (type == "" || type == null || type.length() < 1) {
            type = "NONE";
        }

        try {
            Constructor Limit_con = Class.forName("atnf.atoms.mon.limit.PointLimit" + type).getConstructor(
                    new Class[] { String[].class });
            result = (PointLimit) (Limit_con.newInstance(new Object[] { limitArgs }));
        } catch (Exception e) {
            e.printStackTrace();
            result = new PointLimitNONE(new String[] {});
        }

        result.setStringEquiv(arg);

        return result;
    }

    /**
     * Check the value against the alarm criteria.
     * @param data New data value to check.
     */
    public abstract void checkLimits(PointData data);
}
