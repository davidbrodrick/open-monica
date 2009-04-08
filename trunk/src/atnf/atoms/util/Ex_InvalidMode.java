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
 * Indicates that the object is in an invalid mode for this
 * method to be called.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Ex_InvalidMode.java,v 1.4 1998/12/04 05:13:07 dloone Exp $
 */
public final
class Ex_InvalidMode
extends Exception
{

  // $Log: Ex_InvalidMode.java,v $
  // Revision 1.4  1998/12/04  05:13:07  dloone
  // Changed from DOS to Unix end of line formatting.
  //
  // Revision 1.3  1998/12/02  03:34:07  dloone
  // Added version log to source.
  //

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Ex_InvalidMode.java,v 1.4 1998/12/04 05:13:07 dloone Exp $";

  /**
   * Default constructor.
   *
   * Make an invalid mode exception.
   */
  public
  Ex_InvalidMode()
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
  Ex_InvalidMode(
    String description
  )
  {
    super(description);
  }

}

