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
 * Indicates that the object is in an invalid state for this
 * method to be called.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Ex_BadState.java,v 1.1 2000/02/02 02:40:37 dloone Exp $
 */
public final
class Ex_BadState
extends Exception
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Ex_BadState.java,v 1.1 2000/02/02 02:40:37 dloone Exp $";

  /**
   * Default constructor.
   *
   * Make an invalid mode exception.
   */
  public
  Ex_BadState()
  {
    super();
  }

  /**
   * Constructor.
   *
   * Make an invalid mode exception.
   *
   * @param description
   *  A string to describe the exception.
   */
  public
  Ex_BadState(
    String description
  )
  {
    super(description);
  }

}

