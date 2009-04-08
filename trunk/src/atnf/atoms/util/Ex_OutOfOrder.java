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
//     http://wwwatoms.atnf.csiro.au/doc/gnu/GLGPL.htm 
// or, write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA 

package atnf.atoms.util;

/**
 * Indicates that the operation was called in the incorrect
 * order.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Ex_OutOfOrder.java,v 1.5 2000/03/15 05:45:03 dloone Exp $
 */
public final
class Ex_OutOfOrder
extends
  Error
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Ex_OutOfOrder.java,v 1.5 2000/03/15 05:45:03 dloone Exp $";

  /**
   * Default constructor.
   *
   * Make an out of order exception.
   */
  public
  Ex_OutOfOrder()
  {
    super();
  }

  /**
   * Constructor.
   *
   * Make an out of order exception.
   *
   * @param description
   *  A string to describe the exception.
   */
  public
  Ex_OutOfOrder(
    String description
  )
  {
    super(description);
  }

}

