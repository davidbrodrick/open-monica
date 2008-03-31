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

import java.lang.*;

/**
 * Indicates that the operation requested is not yet implemented.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Ex_NotImplemented.java,v 1.5 2000/01/27 02:24:13 dloone Exp $
 */
public final
class Ex_NotImplemented
extends RuntimeException
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Ex_NotImplemented.java,v 1.5 2000/01/27 02:24:13 dloone Exp $";

  /**
   * Default constructor.
   *
   * Make a "not implemented" exception.
   */
  public
  Ex_NotImplemented()
  {
    super();
  }

  /**
   * Constructor.
   *
   * Make a "not implemented" exception.
   *
   * @param description
   *  A string to describe the exception.
   */
  public
  Ex_NotImplemented(
    String description
  )
  {
    super(description);
  }

}

