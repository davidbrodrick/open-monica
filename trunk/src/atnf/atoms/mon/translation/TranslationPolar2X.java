//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.PointDescription;
import atnf.atoms.util.Angle;

/**
 * Listen to two points which represent the magnitude and angle of a vector in polar form
 * and return the X cartesian component of the vector.
 * 
 * The names of the two points to listen to must be given as constructor <i>init</i>
 * arguments, with magnitude being the first argument. By default the data from the angle
 * point is considered to represent an angle in radians but an optional third argument can
 * be set to "d" and the angle will be interpreted as degrees.
 * 
 * <P>
 * The angle can either be a Number of an Angle object.
 * 
 * @author David Brodrick
 */
public class TranslationPolar2X extends TranslationDualListen
{
    /** Set to true if the angle represents degrees. */
    private boolean itsDegrees = false;

    public TranslationPolar2X(PointDescription parent, String[] init)
    {
        super(parent, init);

        if (init.length == 3 && init[2].toLowerCase().equals("d")) {
            // System.err.println("TranslationPolar2X: Will interpret as degrees");
            itsDegrees = true;
        } else {
            // System.err.println("TranslationPolar2X: Will interpret as radians");
        }
    }

    protected Object doCalculations(Object val1, Object val2)
    {
        if (!(val1 instanceof Number) || !((val2 instanceof Number) || (val2 instanceof Angle))) {
            System.err.println("TranslationPolar2X: " + itsParent.getFullName() + ": ERROR got invalid data!");
            return null;
        }

        double mag = ((Number) val1).doubleValue();
        double angle;
        if (val2 instanceof Number) {
            angle = ((Number) val2).doubleValue();
            if (itsDegrees) {
                angle = Math.PI * angle / 180.0;
            }
        } else {
            angle = ((Angle) val2).getValue();
        }

        return new Float(mag * Math.sin(angle));
    }
}
