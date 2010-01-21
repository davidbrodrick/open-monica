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

import java.io.*;
import java.text.*;

import atnf.atoms.util.Immutable;
import atnf.atoms.util.Enum;

/**
 * General purpose relative time class.
 *
 * <p>
 * Relative time is a time offset,
 * and can be positive or negative.
 * Relative times can be added to each other,
 * negated, or multiplied
 * or divided by a number.
 *
 * <p>
 * The string representation of a <code>RelTime</code> is
 * specified by an object of type <code>RelTime.Format</code>.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: RelTime.java,v 1.28 2006/09/13 04:08:41 bro764 Exp $
 *
 * @see AbsTime
 */
final public
class RelTime
implements
  Cloneable,
  Serializable,
  Immutable
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: RelTime.java,v 1.28 2006/09/13 04:08:41 bro764 Exp $";

  /**
   * String formats.
   *
   * Objects of this class represent the different string formats
   * that an <code>RelTime</code> value can take.
   */
  final public static
  class Format
  extends Enum
  {

    /**
     * Indicates the ordinal value of <code>DECIMAL_BAT</code>.
     */
    final public static
    int DECIMAL_BAT_ORD = 1;

    /**
     * Indicates a string formatted as decimal BAT.
     * The string consists of a "<samp>+</samp>" or "<samp>-</samp>"
     * character, followed by an unsigned decimal integer.
     * The leading sign is <b>always</b> present.
     */
    final public static
    Format DECIMAL_BAT = new Format(DECIMAL_BAT_ORD,"DECIMAL_BAT");

    /**
     * Indicates the ordinal value of <code>SECS_BAT</code>.
     */
    final public static
    int SECS_BAT_ORD = 2;

    /**
     * Indicates a string formatted as seconds BAT.
     * The string constists of a floating point number that is
     * a number of seconds.
     * The leading sign is <b>always</b> present.
     */
    final public static
    Format SECS_BAT = new Format(SECS_BAT_ORD,"SECS_BAT");

    /**
     * Indicates the ordinal value of <code>DHMS_BAT</code>.
     */
    final public static
    int DHMS_BAT_ORD = 3;

    /**
     * Indicates a string formatted as days, hours, minutes, seconds.
     * The string has format dd hh:mm:ss.ssssss
     * The leading sign is <b>always</b> present.
     */
    final public static
    Format DHMS_BAT = new Format(DHMS_BAT_ORD,"DHMS_BAT");

    /**
     * Indicates the ordinal value of the <code>BRIEF</code> Format.
     */
    final public static
    int BRIEF_ORD = 4;

    /**
     * Indicates a string formatted as days, hours, minutes, seconds.
     * The string has format dd hh:mm:ss.ssssss.
     * Unlike DHMS we only show the fields required to fully express the
     * particular number, so for instance ss.sss is a possible output.
     */
    final public static
    Format BRIEF = new Format(BRIEF_ORD,"BRIEF");


    /**
     * Default constructor.
     *
     * Makes the class uninstantiatable by outsiders.
     */
    private
    Format()
    {
      super();
    }

    /**
     * Constructor.
     *
     * @param ord
     *  The ordinal value for the format.
     *
     * @param message
     *  The string message for the format.
     */
    private
    Format(
      int ord,
      String message
    )
    {
      super(ord,message);
    }

  }

  /**
   * Serialized version id.
   */
  static final long serialVersionUID = 6090918009284640875L;

  /**
   * Represents the most significant bit of a <code>long</code>.
   */
  final public static
  long MSB = 0x8000000000000000L;

  /**
   * The current default string format for the class.
   */
  private static
  Format theirDefaultFormat = Format.DECIMAL_BAT;

  private static NumberFormat nf2 = NumberFormat.getInstance();
  private static NumberFormat nf3 = NumberFormat.getInstance();
  {
    nf2.setMinimumIntegerDigits(2);
    nf3.setMinimumIntegerDigits(3);
  }
  
  /**
   * Relative time is kept as a variable of type <code>long</code>
   * in microseconds.
   */
  long itsValue = 0;

  /**
   * Make a new time offset of zero.
   *
   * @return
   *  The new object.
   *
   * Makes a relative time of zero.
   */
  public static
  RelTime
  factory()
  {
    return new RelTime();
  }

  /**
   * Default constructor.
   *
   * <p>Recommend to use the corresponding factory method instead.
   * This constructor remains intact in order to support
   * serialization.</p>
   *
   * Makes a relative time of zero.
   */
  public
  RelTime()
  {
    // Set the value.
    itsValue = 0;
  }

  /**
   * Construct an object from an existing <code>RelTime</code> object.
   *
   * @param t
   *  The existing <code>RelTime</code> object.
   *  It must not be equal to <code>null</code>.
   *
   * @return
   *  The new object.
   */
  public static
  RelTime
  factory(
    RelTime t
  )
  {
    return new RelTime(t);
  }

  /**
   * Construct an object from an existing <code>RelTime</code> object.
   *
   * @param t
   *  The existing <code>RelTime</code> object.
   *  It must not be equal to <code>null</code>.
   */
  private
  RelTime(
    RelTime t
  )
  {
    // Transfer the value.
    itsValue = t.itsValue;
  }

  /**
   * Construct an object representing a specific time offset given
   * as a <code>long</code>.
   *
   * @param time
   *  The new relative time in microseconds.
   *
   * @return
   *  The new object.
   */
  public static
  RelTime
  factory(
    long time
  )
  {
    return new RelTime(time);
  }

  /**
   * Construct an object representing a specific time offset given
   * as a <code>long</code>.
   *
   * @param time
   *  The new relative time in microseconds.
   */
  private
  RelTime(
    long time
  )
  {
    // Just set the value from the parameter.
    itsValue = time;
  }

  /**
   * Construct an object representing a specific time offset given
   * as a <code>String</code>.
   *
   * @param str
   *  A string representation of the relative time in any of the
   *  valid string formats.
   *  If it successfully translates to an ATOMS property,
   *  then that is used instead.
   *  It must not be equal to <code>null</code>.
   *
   * @return
   *  The new object.
   *
   * @exception NumberFormatException
   *  Thrown when <code>str</code> does not represent a valid string
   *  for a <code>RelTime</code>.
   */
  public static
  RelTime
  factory(
    String str
  )
  throws
    NumberFormatException
  {
    return new RelTime(str);
  }

  /**
   * Construct an object representing a specific time offset given
   * as a <code>String</code>.
   *
   * @param str
   *  A string representation of the relative time in any of the
   *  valid string formats.
   *  It must not be equal to <code>null</code>.
   *
   * @exception NumberFormatException
   *  Thrown when <code>str</code> does not represent a valid string
   *  for a <code>RelTime</code>.
   */
  private
  RelTime(
    String str
  )
  throws
    NumberFormatException
  {
    parse(str);
  }

  /**
   * Clone the object.
   *
   * @return
   *  A new object identical to this one.
   */
  public
  Object
  clone()
  {
    // The class is imutable. Just return this.
    return this;
  }

  /**
   * Test for equivalence.
   *
   * @param o
   *  The object to test for equivalency against.
   *
   * @return
   *  Returns <code>true</code> if <code>o</code> is
   *  equivalent to this object,
   *  <code>false</code> otherwise.
   */
  public
  boolean
  equiv(
    Object o
  )
  {
    // The value to return.
    boolean isEquiv = false;
    // Local version of o.
    RelTime o1;

    // Prerequisites.
    if((o != null) && (o.getClass() == getClass())) {
      o1 = (RelTime)o;
      // Is equiv if same object.
      if(equals(o1)) {
        isEquiv = true;
      }
      else {
        // Test the attributes.
        isEquiv = (this.itsValue == o1.itsValue);
      }
    }

    return isEquiv;
  }

  /**
   * Set the default string format for the class.
   *
   * @param fmt
   *  The new default string format for the class.
   */
  public static
  void
  setTheirDefaultFormat(
    Format fmt
  )
  {
    theirDefaultFormat = (Format)fmt.clone();
  }

  /**
   * Get the default string format for the class.
   *
   * @return
   *  The current default string format for the class.
   */
  public static
  Format
  getTheirDefaultFormat()
  {
    return (Format)theirDefaultFormat.clone();
  }

  /**
   * Return the value of the time as a long.
   *
   * @return
   *  The value of the time as a long.
   */
  public
  long
  getValue()
  {
    return itsValue;
  }

  /**
   * Return the value of the time in seconds.
   *
   * @return
   *  The value of the time as seconds.
   */
  public
  double
  getAsSeconds()
  {
    return itsValue / 1000000.0;
  }

  /**
   * Convert the <code>RelTime</code> to a string.
   *
   * @return
   *  A string version of the object in the "default" format.
   */
  public
  String
  toString()
  {
    return toString((Format)null);
  }

  /**
   * Convert the <code>RelTime</code> to a string in any of the
   * string formats.
   *
   * @param format
   *  One of the valid string format specifiers.
   *  A value of <code>null</code> indicates to use the current default
   *  format for the class.
   *
   * @return
   *  A string representation of the object in the requested format.
   */
  public
  String
  toString(
    Format format
  )
  {
    // The string to return.
    String res = null;

    // Check for a null value passed in.
    if(format == null) {
      format = getTheirDefaultFormat();
    }

    // Check against each available format.
    switch(format.ord()) {
      case Format.DECIMAL_BAT_ORD: {
        if(itsValue < 0) {
          res = Long.toString(itsValue);
        }
        else {
          res = "+" + Long.toString(itsValue);
        }
        break;
      }
      case Format.SECS_BAT_ORD: {
        // Convert it to a string as DECIMAL_BAT, then insert the
        // decimal point.
        if(itsValue >= 0) {
          res = "+" + (new DecimalFormat("0000000")).format(itsValue);
        }
        else {
          res = (new DecimalFormat("0000000")).format(itsValue);
        }
        res = res.substring(0,res.length() - 6) + "." +
            res.substring(res.length() - 6);
        break;
      }
      case Format.DHMS_BAT_ORD: {
        // Convert it to a string as DECIMAL_BAT, then insert the
        // decimal point.
        long micros = itsValue;
        String sign = "+";
        if(itsValue < 0) {
          micros = -micros;
          sign = "-";
        }
        int s = (int)(micros/1000000l);
        micros = (micros-s*1000000l)/1000l;
        int m = s/60;
        s = s - m*60;
        int h = m/60;
        m = m - h*60;
        int d = h/24;
        h = h - d*24;
        res = sign + d + "d "
               + nf2.format(h)+":"
               + nf2.format(m)+":"
               + nf2.format(s)+"."
               + nf3.format(micros);
        break;
      }
      case Format.BRIEF_ORD: {
        // Brief human readable format
        long micros = itsValue;
        if(itsValue < 0) {
          micros = -micros;
        }
        int s = (int)(micros/1000000l);
        micros = (micros-s*1000000l)/1000l;
        int m = s/60;
        s = s - m*60;
        int h = m/60;
        m = m - h*60;
        int d = h/24;
	h = h - d*24;

        //Add sign only if period is negative
	if (itsValue<0) {
    res = "-";
  } else {
    res = "";
  }

        if (m==0 && h==0 && d==0) {
	  //Only need seconds to express this period
	  res += nf2.format(s);
	  //Add in the decimal part of a second if required
	  if (micros>0) {
      res += "." + nf3.format(micros);
    }
	} else if (d==0) {
	  //Don't need to express the number of days
	  res += nf2.format(h)+":"
	      + nf2.format(m)+":"
	      + nf2.format(s);
	  //Add in the decimal part of a second if required
	  if (micros>0) {
      res += "." + nf3.format(micros);
    }
	} else {
	  res += d + "d "
	      + nf2.format(h)+":"
	      + nf2.format(m)+":"
	      + nf2.format(s);
	  //Add in the decimal part of a second if required
	  if (micros>0) {
      res += "." + nf3.format(micros);
    }
	}
	break;
      }
      default: {
        assert false;
        break;
      }
    }

    return res;
  }


  /**
   * Add a <code>RelTime</code> to this object and return the result as
   * a new object.
   * This object is left unchanged.
   *
   * @param t2
   *  The time to add to this object.
   *  It must not be equal to <code>null</code>.
   *
   * @return
   *  A reference to the new object.
   *
   * @exception ArithmeticException
   *  Thrown if the resulting time would be larger than can be
   *  represented by a <code>RelTime</code>.
   */
  public
  RelTime
  add(
    RelTime t2
  )
  throws
    ArithmeticException
  {
    // Return the new object.
    return new RelTime(itsValue + t2.itsValue);
  }

  /**
   * Negate this relative time and return the result as a new object.
   *
   * @return
   *  A reference to the new object.
   *
   * @exception ArithmeticException
   *  Thrown when the result would be outside the range that a
   *  <code>RelTime</code> can represent.
   */
  public
  RelTime
  negate()
  throws
    ArithmeticException
  {
    return new RelTime(-itsValue);
  }

  /**
   * Multiply this relative time by an integer and return the result as
   * a new object.
   *
   * @param i
   *  The number to multiply by.
   *
   * @return
   *  A reference to the new object.
   *
   * @exception ArithmeticException
   *  Thrown if there was an overflow in the result.
   */
  public
  RelTime
  multiply(
    long i
  )
  throws
    ArithmeticException
  {
    return new RelTime(itsValue = itsValue * i);
  }

  /**
   * Divide this relative time by an integer and return the result
   * as a new object.
   *
   * @param i
   *  The number to divide by.
   *
   * @return
   *  A reference to the new object.
   *
   * @exception ArithmeticException
   *  Thrown if there was an overflow in the result,
   *  including a divide by zero.
   */
  public
  RelTime
  divide(
    long i
  )
  throws
    ArithmeticException
  {
    return new RelTime(itsValue = itsValue / i);
  }

  /**
   * Make the calling thread wait for time duration.
   * The method is implemented in such a way that it is safe
   * for many threads to be sleeping on the same object simultaneously.
   *
   * @exception IllegalArgumentException
   *  Thrown when the object represents a negative relative time.
   *
   * @exception InterruptedException
   *  The thread was awoken during the sleep.
   *
   * @exception Time.Ex_TooLate
   *  The relative time represented by this object is negative.
   */
  public
  void
  sleep()
  throws
    IllegalArgumentException,
    InterruptedException,
    Time.Ex_TooLate
  {
    if(itsValue < 0) {
      Thread.dumpStack();
      throw new Time.Ex_TooLate();
    }

    // Just sleep for the specified duration.
    Thread.sleep(itsValue / 1000);
  }

  /**
   * Parse a string as a relative time.
   *
   * @param str
   *  A string representation of the relative time in any of the
   *  valid string formats.
   *
   * @exception NumberFormatException
   *  Thrown when <code>str</code> does not represent a valid string
   *  for a <code>RelTime</code>.
   */
  public
  void
  parse(
    String str
  )
  throws
    NumberFormatException
  {
    if(str.indexOf('.') != -1) {
      // The string contains a period. Try to parse it as SECS_BAT.
      parseSecsBAT(str);
    }
    else {
      // Only other format.
      parseDecimalBAT(str);
    }
  }

  /**
   * Parse a string formatted as <code>DECIMAL_BAT</code>.
   *
   * @param str
   *  The string formatted as <code>DECIMAL_BAT</code>.
   *
   * @exception NumberFormatException
   *  Thrown when <code>str</code> is not a valid
   *  <code>DECIMAL_BAT</code> formatted string.
   */
  private
  void
  parseDecimalBAT(
    String str
  )
  throws
    NumberFormatException
  {
    // Then parse the string as a decimal number.
    try {
      // If first char is a "+", need to remove it first.
      if(str.charAt(0) == '+') {
        itsValue = Long.parseLong(str.substring(1),10);
      }
      else {
        itsValue = Long.parseLong(str,10);
      }
    }
    catch(NumberFormatException e) {
      throw new NumberFormatException("bad relative time: \"" + str + "\"");
    }
  }

  /**
   * Parse a string formatted as <code>SECS_BAT</code>.
   *
   * @param str
   *  The string formatted as <code>SECS_BAT</code>.
   *
   * @exception NumberFormatException
   *  Thrown when <code>str</code> is not a valid
   *  <code>SECS_BAT</code> formatted string.
   */
  private
  void
  parseSecsBAT(
    String str
  )
  throws
    NumberFormatException
  {
    // The first char has to be a "+" or "-".
    if((str.charAt(0) != '+') && (str.charAt(0) != '-')) {
      throw new NumberFormatException("bad relative time: \"" + str + "\"");
    }

    // If the first char is a "+", need to remove it first.
    if(str.charAt(0) == '+') {
      str = str.substring(1);
    }

    // Then parse the string as a double.
    try {
      itsValue = (long)((new Double(str)).doubleValue() * 1000000.0);
    }
    catch(NumberFormatException e) {
      throw new NumberFormatException("bad relative time: \"" + str + "\"");
    }
  }

}

