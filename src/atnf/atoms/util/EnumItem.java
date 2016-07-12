// Copyright (C)1998  CSIRO Australia Telescope National Facility
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
//     http://wwwatoms.atnf.csiro.au/doc/gnu/GLGPL.htm
// or, write to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA  02111-1307  USA

package atnf.atoms.util;

import java.util.*;
import java.util.regex.*;

/**
 * Simple class to store a single item of an enumeration, ie a string and integer.
 * 
 * @author Simon Hoyle
 */

public class EnumItem
{

  private String itsName;
  private short itsValue;

  /** Matches an EnumItem string and captures name, value */
  static Pattern p = Pattern.compile("^(\\w+)\\s+\\((\\d+)\\)");

  public EnumItem(String name, int value)
  {
      itsName = name;
      itsValue = (short)value;
  }

  public static EnumItem factory(String name, int value) { return new EnumItem(name, value); }

  /** Get the integer value */
  public int getValue()    { return itsValue; }

  /** Get the string value */
  public String getName()  { return itsName; }

  /** Return a string representation in the form "name (val)" */
  public String toString() { return (itsName + " (" + itsValue + ")"); }

  /** Create an EnumItem by parsing a string */
  public static EnumItem valueOf(String s) throws Exception
  {
      Matcher m = p.matcher(s);
      if (m.find()) {
          return new EnumItem(m.group(1), Integer.parseInt(m.group(2)));
      }
      return null;
  }

}

