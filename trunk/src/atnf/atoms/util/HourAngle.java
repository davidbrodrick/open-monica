// Copyright (C)1997  CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2 
// of the License, or (at your option) any later version. 
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of 
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
// GNU Library General Public License for more details. 
// 
// A copy of the GNU Library General Public License is available at: 
// http://wwwatoms.atnf.csiro.au/doc/gnu/GLGPL.htm 
// or, write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA 

package atnf.atoms.util;

/**
 * Sub-class of <i>Angle</i>. The only difference is that by default
 * the string representation will be shown as an hour rather than in
 * radians or degrees.
 *
 * @author David Brodrick
 *
 * @version $Id: HourAngle.java,v 1.1 2005/07/08 04:47:25 bro764 Exp $
 *
 * @see Angle
 */
public
class HourAngle
extends Angle
{
  /**
   * Make a new object from a double.
   *
   * @param value
   *  The angle value (in radians).
   */
  public
  HourAngle(
    double value
  )
  {
    // Set the value to that indicated.
    itsValue = value;
  }


  /**
   * Make a new object from an existing
   * <code>Angle</code> object.
   *
   * @param a
   *  Another <code>Angle</code> object to copy the value from.
   *  Must not be <code>null</code>.
   */
  public
  HourAngle(
    Angle a
  )
  {
    // Set the value that indicated.
    itsValue = a.itsValue;
  }


  /**
   * Returns a string representation of this angle in the default
   * string format and with the default and with one decimal place.
   *
   * @return
   *  A <code>String</code> object of this Angle.
   */
  public
  String
  toString()
  {
      return toString(Format.HMS);
  }
}
