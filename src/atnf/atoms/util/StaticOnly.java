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

/**
 * Tag interface for uninstantiable classes.
 * That is, classes that do not allow themselves to be instantiated.
 * Such classes usually have only static methods and attributes.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: StaticOnly.java,v 1.1 1999/06/25 02:19:01 dloone Exp $
 */
public
interface StaticOnly
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: StaticOnly.java,v 1.1 1999/06/25 02:19:01 dloone Exp $";

}

