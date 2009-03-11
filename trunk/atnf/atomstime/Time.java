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

package atnf.atoms.time;

import java.lang.*;
import atnf.atoms.util.*;

/**
 * Container class for miscellaneous time functionality.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Time.java,v 1.7 2004/09/13 05:33:18 bro764 Exp $
 *
 * @see RelTime
 */
final public
class Time
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Time.java,v 1.7 2004/09/13 05:33:18 bro764 Exp $";

  /**
   * Represents the most significant bit of a <code>long</code>.
   */
  private static final
  long MSB = 0x8000000000000000L;

  /**
   * Constructor.
   *
   * This constructor prevents this class from being instantiated.
   */
  private
  Time()
  {}

  /**
   * Exception thrown when absolute time is required but not available
   * on the host.
   */
  public static
  class Ex_TimeNotAvailable
  extends
    Error
  {

    /**
     * Default constructor.
     */
    public
    Ex_TimeNotAvailable()
    {
      super("Absolute time not available on this host.");
    }

  }

  /**
   * Exception thrown when the time specified was too late.
   */
  public static
  class Ex_TooLate
  extends
    Exception
  {

    /**
     * Default constructor.
     */
    public
    Ex_TooLate()
    {
      super("The time specified was too late.");
      printStackTrace();
    }

  }

  /**
   * Take the difference between two absolute times and return a
   * relative time.
   *
   * <p>
   * This is a kind of housekeeping function,
   * and is not part of the <code>AbsTime</code> class because
   * subtracting two absolute times to give another absolute time
   * is not a meaningful operation.
   *
   * @param t1
   *  First operand.
   *  It must not be equal to <code>null</code>.
   *
   * @param t2
   *  Second operand.
   *  It must not be equal to <code>null</code>.
   *
   * @return
   *  The value of <samp>(t1&nbsp;-&nbsp;t2)</samp> as a relative time.
   *
   * @exception ArithmeticException
   *  Thrown when the result would be out of the range of a
   *  <code>RelTime</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either of the operands is <code>ASAP</code>
   *  of <code>NEVER</code>.
   */
  public static
  RelTime
  diff(
    AbsTime t1,
    AbsTime t2
  )
  throws
    ArithmeticException,
    IllegalArgumentException
  {
    // Check for ASAP and NEVER.
    if(t1.isASAP()) {
      throw new IllegalArgumentException("t1 == ASAP");
    }
    if(t1.isNEVER()) {
      throw new IllegalArgumentException("t1 == NEVER");
    }
    if(t2.isASAP()) {
      throw new IllegalArgumentException("t2 == ASAP");
    }
    if(t2.isNEVER()) {
      throw new IllegalArgumentException("t2 == NEVER");
    }

    // Call the operands HMSB if their most significant bit is high.
    // Java is only capable of signed integer arrithmetic, so we need
    // to remember the most significant bits of the operands, so that
    // we can check for overflow at the end.

    // Place to store the arithmetic result. Is converted to RelTime
    // in the return statement.
    long res;

    // True if t1 is MSBH.
    boolean t1High;

    // True if t2 is MSBH.
    boolean t2High;

    // True if the result is MSBH.
    boolean resHigh;

    // Record whether t1 and t2 are HMSB.
    t1High = ((t1.itsValue & MSB) == MSB);
    t2High = ((t2.itsValue & MSB) == MSB);

    // Make the subtraction. Ignore overflows.
    res = t1.itsValue - t2.itsValue;

    // Record whether the result is MSBH.
    resHigh = ((res & MSB) == MSB);

    // Check for overflow.
    if((t1High && !t2High && resHigh) ||
       (!t1High && t2High && !resHigh)) {
      throw new ArithmeticException("overflow");
    }

    // Everything seems ok, so return the result (as a RelTime).
    return RelTime.factory(res);
  }

}

