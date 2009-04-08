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
 * Indicates that something could not be shut down properly.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Ex_ShutdownFailure.java,v 1.2 1999/10/06 03:53:09 dloone Exp $
 */
public final
class Ex_ShutdownFailure
extends
  Exception
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Ex_ShutdownFailure.java,v 1.2 1999/10/06 03:53:09 dloone Exp $";

  /**
   * Make a shutdown failure exception.
   */
  public
  Ex_ShutdownFailure()
  {
    super();
  }

  /**
   * Make a shutdown failure exception.
   *
   * @param description
   *  A string to describe the exception.
   */
  public
  Ex_ShutdownFailure(
    String description
  )
  {
    super(description);
  }

}

