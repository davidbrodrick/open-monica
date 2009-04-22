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

import java.io.*;
import java.text.*;

/**
 * Class representing an angle.
 *
 * <p>The string representation for an <code>Angle</code> object is
 * represented by an object of type <code>Angle.Format</code>.
 * When reading these strings,
 * it is possible to automatically distinguish
 * between these formats and obtain the correct internal value.</p>
 *
 * <p>Objects of this class are immutable.
 * Whenever an operation is requested on an object,
 * a new object with the result is returned and the original object
 * is not changed.</p>
 *
 * @author David G Loone
 *
 * @version $Id: Angle.java,v 1.39 2005/07/08 04:47:09 bro764 Exp $
 *
 * @see AngleDisplay
 */
public
class Angle
implements
  Cloneable,
  Immutable,
  Serializable
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Angle.java,v 1.39 2005/07/08 04:47:09 bro764 Exp $";

  /**
   * Serialised version id.
   */
  static final long serialVersionUID = -2309745966047874429L;

  /**
   * The hours format.
   */
  final private static
  NumberFormat theirHrFmt = new DecimalFormat("00");

  /**
   * The minutes format.
   */
  final private static
  NumberFormat theirMinFmt = new DecimalFormat("00");

  /**
   * The seconds format.
   */
  final private static
  NumberFormat theirSecFmt = new DecimalFormat("00");

  /**
   * String formats.
   *
   * Objects of this class represent the different string formats
   * that an <code>Angle</code> value can take.
   */
  final public static
  class Format
  extends Enum
  {

    /**
     * Represents the ordinal value of <code>DMS</code>.
     */
    final public static
    int DMS_ORD = 1;

    /**
     * Indicates degrees/minutes/seconds using UNICODE characters.
     * This form always specifies degrees, minutes and seconds
     * components.
     * The degrees field is a signed integer, followed by a \u00B0.
     * The minutes field is an unsigned integer,
     * followed by a '.
     * The seconds field is a unsigned integer,
     * followed by a ".
     * This is optionally follwed by a decimal fractional part of the
     * seconds.
     * The minutes field and the integer component of the seconds field
     * are always two characters wide,
     * with leading zeros where necessary.
     * Example formats are:
     * <ul>
     *   <li>XX\u00B0X'XX"</li>
     *   <li>-XX\u00B0XX'XX".XXX</li>
     *   <li>+XX\u00B0X'XX".X</li>
     * </ul>
     */
    final public static
    Format DMS = new Format(DMS_ORD,"DMS");

    /**
     * Represents the ordinal value of <code>DMS_ASCII</code>.
     */
    final public static
    int DMS_ASCII_ORD = 2;

    /**
     * Indicates degrees/minutes/seconds using only ASCII characters.
     * This form is very similar to <code>DEGREES</code> except that
     * the degree and minutes fields are seperated by
     * the '^' character. It is supplied for systems that do not
     * support UNICODE. On input the 'd' character is acceptable in place
     * of the '^' character.
     * Example formats are:
     * <ul>
     *   <li>XX^XX'XX"</li>
     *   <li>-XX^XX'XX".XXX</li>
     *   <li>+XX^XX'XX".X</li>
     * </ul>
     */
    final public static
    Format DMS_ASCII = new Format(DMS_ASCII_ORD,"DMS_ASCII");

    /**
     * Represents the ordinal value of <code>DEGREES</code>.
     */
    final public static
    int DEGREES_ORD = 3;

    /**
     * Indicates decimal degrees using UNICODE characters.
     * This is a signed floating number,
     * followed by a \u00B0 character.
     * Example formats are:
     * <ul>
     *   <li>XX.XXXX\u00B0</li>
     *   <li>-XX\u00B0</li>
     * </ul>
     */
    final public static
    Format DEGREES = new Format(DEGREES_ORD,"DEGREES");

    /**
     * Indicates the ordinal value of <code>HMS</code>.
     */
    final public static
    int HMS_ORD = 4;

    /**
     * Indicates hours/minutes/seconds.
     * Each of the hours, minutes and seconds fields are
     * seperated by a colon.
     * The hours, minutes and the integer part of the seconds fields
     * are in a two character field,
     * filled with leading zeros if necessary.
     * Example formats are:
     * <ul>
     *   <li>XX:XX:XX.X</li>
     *   <li>-XX:XX:XX</li>
     * </ul>
     */
    final public static
    Format HMS = new Format(HMS_ORD,"HMS");

    /**
     * Indicates the ordinal value of <code>RADIANS</code>.
     */
    final public static
    int RADIANS_ORD = 5;

    /**
     * Indicates radians (decimal).
     * This is a signed floating number.
     * Example formats include:
     * <ul>
     *   <li>XX.XXXX</li>
     *   <li>-XX</li>
     * </ul>
     */
    final public static
    Format RADIANS = new Format(RADIANS_ORD,"RADIANS");

    /**
     * The default format.
     */
    final public static
    Format DEFAULT_FORMAT = DMS;

    /**
     * Default constructor to prevent instantiation by outsiders.
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
     *  The ordinal value of the format.
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
   * The number of minutes in a degree.
   */
  private static final
  double MINS_PER_DEG = 60.0;

  /**
   * The number of minutes in an hour.
   */
  private static final
  double MINS_PER_HOUR = 60.0;

  /**
   * The number of seconds in a minute.
   */
  private static final
  double SECS_PER_MIN = 60.0;

  /**
   * The number of degrees in a radian.
   */
  private static final
  double DEGS_PER_RAD = 360.0 / (2.0 * Math.PI);

  /**
   * The number of radians in a degree.
   */
  private static final
  double RADS_PER_DEG = (2.0 * Math.PI) / 360.0;

  /**
   * The number of hours in a radian.
   */
  private static final
  double HOURS_PER_RAD = 24.0 / (2.0 * Math.PI);

  /**
   * The number of radians in an hour.
   */
  private static final
  double RADS_PER_HOUR = (2.0 * Math.PI) / 24.0;

  /**
   * An angle of 0 degrees.
   */
  public static final
  double A0 = 0.0;

  /**
   * An angle of 90 degrees.
   */
  public static final
  double A90 = Math.PI / 2.0;

  /**
   * An angle of 180 degrees.
   */
  public static final
  double A180 = Math.PI;

  /**
   * An angle of 270 degrees.
   */
  public static final
  double A270 = Math.PI * 3.0 / 2.0;

  /**
   * An angle of 360 degrees.
   */
  public static final
  double A360 = Math.PI * 2.0;

  /**
   * The current default output format.
   */
  private static
  Format theirDefaultFormat = Format.DEFAULT_FORMAT;

  /**
   * The value of the angle (in radians).
   */
  protected
  double itsValue;

  /**
   * Make a new object with a value of zero.
   */
  public static
  Angle
  factory()
  {
    return new Angle();
  }

  /**
   * Make a new object with a value of zero.
   */
  public
  Angle()
  {
    // Set the value to zero.
    itsValue = 0;
  }

  /**
   * Make a new object from a double.
   *
   * @param value
   *  The angle value (in radians).
   */
  public static
  Angle
  factory(
    double value
  )
  {
    return new Angle(value);
  }

  /**
   * Make a new object from a double.
   *
   * @param value
   *  The angle value (in radians).
   */
  private
  Angle(
    double value
  )
  {
    // Set the value to that indicated.
    itsValue = value;
  }

  /**
   * Make a new object from a double in either radians or degrees.
   *
   * @param value
   *  The angle value
   *  (in units specified by <code>units</code>).
   *
   * @param units
   *  The units of <code>value</code>.
   *  This must be equal to either <code>DEGREES</code>
   *  <code>RADIANS</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when <code>units</code> is not equal to either
   *  <code>DEGREES</code> or <code>RADIANS</code>.
   */
  public static
  Angle
  factory(
    double value,
    Format units
  )
  throws
    IllegalArgumentException
  {
    return new Angle(value,units);
  }

  /**
   * Make a new object from a double in either radians or degrees.
   *
   * @param value
   *  The angle value
   *  (in units specified by <code>units</code>).
   *
   * @param units
   *  The units of <code>value</code>.
   *  This must be equal to either <code>DEGREES</code>
   *  <code>RADIANS</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when <code>units</code> is not equal to either
   *  <code>DEGREES</code> or <code>RADIANS</code>.
   */
  private
  Angle(
    double value,
    Format units
  )
  throws
    IllegalArgumentException
  {
    if(units.ord() == Format.DEGREES.ord()) {
      itsValue = value * RADS_PER_DEG;
    }
    else if(units.ord() == Format.RADIANS.ord()) {
      itsValue = value;
    }
    else {
      throw new IllegalArgumentException("bad format: " + units);
    }
  }

  /**
   * Make a new object from an existing
   * <code>Angle</code> object.
   *
   * @param a
   *  Another <code>Angle</code> object to copy the value from.
   *  Must not be <code>null</code>.
   */
  public static
  Angle
  factory(
    Angle a
  )
  {
    return new Angle(a);
  }

  /**
   * Make a new object from an existing
   * <code>Angle</code> object.
   *
   * @param a
   *  Another <code>Angle</code> object to copy the value from.
   *  Must not be <code>null</code>.
   */
  private
  Angle(
    Angle a
  )
  {
    // Set the value that indicated.
    itsValue = a.itsValue;
  }

  /**
   * Make a new object from a string representation.
   *
   * <p>
   * The string can be any one of the valid string representations of
   * an angle.
   * The particular format is automatically determined
   * and the string is parsed accordingly.
   *
   * <p>
   * The allowed formats are generalised somewhat from the formats
   * that are printed.
   * The following shows some valid combinations:
   * <blockquote>
   * <table cellpadding=4 border=1>
   *  <tr>
   *    <td align=center><font size=+1><code><b>-2.48</b></code></font></td>
   *    <td>radians</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>3:5:27.32</b></code></font></td>
   *    <td>hours/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>-3:5:27.32</b></code></font></td>
   *    <td>hours/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>3:5:27</b></code></font></td>
   *    <td>hours/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B02'34"</b></code></font></td>
   *    <td>degrees/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B0</b></code></font></td>
   *    <td>degrees</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B0.38</b></code></font></td>
   *    <td>degrees</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B02'.38</b></code></font></td>
   *    <td>degrees/minutes</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B02'45"</b></code></font></td>
   *    <td>degrees/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>-48\u00B02'45".38</b></code></font></td>
   *    <td>degrees/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>2'.38</b></code></font></td>
   *    <td>minutes</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>2'45".38</b></code></td>
   *    <td>minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>45"</b></code></td>
   *    <td>seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>45".39</b></code></td>
   *    <td>seconds</td>
   *  </tr>
   * </table>
   * </blockquote>
   *
   * @param str
   *  A <code>String</code> representation of an
   *  <code>Angle</code> object.
   *  Whitespace at the beginning or end of the string is ignored.
   *  Must not be equal to <code>null</code>.
   *
   * @exception NumberFormatException
   *  Thrown when a <code>String</code> is not a valid string
   *  format of an angle.
   */
  public static
  Angle
  factory(
    String str
  )
  throws
    NumberFormatException
  {
    return new Angle(str);
  }

  /**
   * Make a new object from a string representation.
   *
   * <p>
   * The string can be any one of the valid string representations of
   * an angle.
   * The particular format is automatically determined
   * and the string is parsed accordingly.
   *
   * <p>
   * The allowed formats are generalised somewhat from the formats
   * that are printed.
   * The following shows some valid combinations:
   * <blockquote>
   * <table cellpadding=4 border=1>
   *  <tr>
   *    <td align=center><font size=+1><code><b>-2.48</b></code></font></td>
   *    <td>radians</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>3:5:27.32</b></code></font></td>
   *    <td>hours/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>-3:5:27.32</b></code></font></td>
   *    <td>hours/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>3:5:27</b></code></font></td>
   *    <td>hours/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B02'34"</b></code></font></td>
   *    <td>degrees/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B0</b></code></font></td>
   *    <td>degrees</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B0.38</b></code></font></td>
   *    <td>degrees</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B02'.38</b></code></font></td>
   *    <td>degrees/minutes</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>48\u00B02'45"</b></code></font></td>
   *    <td>degrees/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>-48\u00B02'45".38</b></code></font></td>
   *    <td>degrees/minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>2'.38</b></code></font></td>
   *    <td>minutes</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>2'45".38</b></code></td>
   *    <td>minutes/seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>45"</b></code></td>
   *    <td>seconds</td>
   *  </tr>
   *  <tr>
   *    <td align=center><font size=+1><code><b>45".39</b></code></td>
   *    <td>seconds</td>
   *  </tr>
   * </table>
   * </blockquote>
   *
   * @param str
   *  A <code>String</code> representation of an
   *  <code>Angle</code> object.
   *  Whitespace at the beginning or end of the string is ignored.
   *  Must not be equal to <code>null</code>.
   *
   * @exception NumberFormatException
   *  Thrown when a <code>String</code> is not a valid string
   *  format of an angle.
   */
  private
  Angle(
    String str
  )
  throws
    NumberFormatException
  {
    // If the string contains a 'd' characer, replace it with
    // '^' characters.
    if(str.indexOf("d") != -1) {
      // The chars of the string.
      char[] chars = str.toCharArray();

      // Iterate over the char array.
      for(int i = 0; i < chars.length; i++) {
        if(chars[i] == 'd') {
          chars[i] = '^';
        }
      }

      // Re-create the string.
      str = new String(chars);
    }

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
    // The class is immutable. Just return this.
    return this;
  }

  /**
   * Test for equivalence.
   *
   * @param o
   *  The object to test for equivlence against.
   *
   * @return
   *  Returns <code>true</code> if <code>o</code> is equivalent to
   *  this object,
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
    Angle o1;

    // Prerequisites.
    if((o != null) && (o.getClass() == getClass())) {
      o1 = (Angle)o;
      // Is equiv if same object.
      if(equals(o1)) {
        isEquiv = true;
      }
      else {
        // Test the attributes.
        isEquiv = (o1.itsValue == itsValue);
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
   * Test for proximity with another angle.
   *
   * @param a
   *  The angle to test against.
   *  Must not be equal to <code>null</code>.
   *
   * @param prox
   *  The proximity to test for.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if this angle is within
   *  <code>prox</code> (on either side) of <code>a</code>.
   */
  public
  boolean
  isNear(
    Angle a,
    Angle prox
  )
  {
    // Test for prox < 0.
    if(prox.itsValue < 0) {
      throw new IllegalArgumentException("prox = " + prox);
    }

    return (Math.abs(itsValue - a.itsValue) <= prox.itsValue);
  }

  /**
   * Test for this angle less than another.
   *
   * @param a
   *  The angle to test against.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if this object
   *  is less than <code>a</code>.
   */
  public
  boolean
  isLT(
    Angle a
  )
  {
    return (itsValue < a.itsValue);
  }

  /**
   * Test for this angle less than or equal to another.
   *
   * @param a
   *  The angle to test against.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if this object
   *  is less than or equal to <code>a</code>.
   */
  public
  boolean
  isLTE(
    Angle a
  )
  {
    return (itsValue <= a.itsValue);
  }

  /**
   * Test for this angle greater than another.
   *
   * @param a
   *  The angle to test against.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if this object
   *  is greater than <code>a</code>.
   */
  public
  boolean
  isGT(
    Angle a
  )
  {
    return (itsValue > a.itsValue);
  }

  /**
   * Test for this angle greater than or equal to another.
   *
   * @param a
   *  The angle to test against.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if this object
   *  is greater than or equal to <code>a</code>.
   */
  public
  boolean
  isGTE(
    Angle a
  )
  {
    return (itsValue >= a.itsValue);
  }

  /**
   * Test for not-a-number.
   *
   * @return
   *  Boolean <code>true</code> if this angle is not-a-number,
   *  <code>false</code> otherwise.
   */
  public
  boolean
  isNaN()
  {
    // Make a Double object, and call the NaN method.
    return (new Double(itsValue)).isNaN();
  }

  /**
   * Test for infinity.
   *
   * @return
   *  Boolean <code>true</code> if this angle is infinite,
   *  <code>false</code> otherwise.
   */
  public
  boolean
  isInfinite()
  {
    // Make a Double object, and call the isInfinite method.
    return (new Double(itsValue)).isInfinite();
  }

  /**
   * Returns the value of this angle.
   *
   * @return
   *  A double value.
   */
  public
  double
  getValue()
  {
    // Return the value of this angle.
    return itsValue;
  }

  /**
   * Returns the value of this angle in degrees.
   *
   * @return
   *  A double value (in degrees).
   */
  public
  double
  getValueDeg()
  {
    // Return the value of this angle in degrees.
    return (itsValue * DEGS_PER_RAD);
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
    return toString(getTheirDefaultFormat(),1);
  }

  /**
   * Returns a string representation of this angle in the default
   * format with the given number of decimal places.
   *
   * @param digits
   *  The number of decimal places in the string.
   *  Must not be less than zero.
   */
  public
  String
  toString(
    int digits
  )
  {
    return toString(getTheirDefaultFormat(),digits);
  }

  /**
   * Returns a string representation of this angle in any given
   * string format and one decimal place.
   *
   * @param format
   *  Detemrines which string format will be generated.
   *  A value of <code>null</code> indicates the default format.
   *
   * @return
   *  A <code>String</code> object of this Angle.
   */
  public
  String
  toString(
    Format format
  )
  {
    if(format == null) {
      format = getTheirDefaultFormat();
    }

    return toString(format,1);
  }

  /**
   * Returns a string representation of this angle in any given
   * string format, and to an indicated number of digits.
   *
   * @param format
   *  Determines which string representation will be generated.
   *  A value of <code>null</code> indicates the default format.
   *
   * @param digits
   *  The number of siginificant digits.
   *  The application of this parameter varies slightly with the
   *  string format requested.
   *  It will apply to whichever field is a floating point number.
   *  In the case of <code>DMS</code>,
   *  <code>DMS_ASCII</code> and <code>HMS</code>,
   *  it will apply to the seconds field.
   *  In the case of <code>DEGREES</code> and <code>RADIANS</code>,
   *  it will apply to the degrees and radians fields respectively
   *  (the only numerical field in the number).
   *  Must not be less than zero.
   *
   * @return
   *  A <code>String</code> object representing this angle,
   *  formatted according to <code>format</code>.
   */
  public
  String
  toString(
    Format format,
    int digits
  )
  {
    // The string to build the result into.
    String res = null;
    // The value in its object wrapper.
    Double itsValueObj = new Double(itsValue);

    if(format == null) {
      format = getTheirDefaultFormat();
    }

    // Need to check for the value -0.0, since this is not changed
    // by the Math.abs function.
    if(itsValueObj.toString().equals("-0.0")) {
      itsValue = -itsValue;
    }

    // Check for some special cases.
    if(itsValueObj.isNaN()) {
      res = new String("NaN");
    }
    else if(itsValueObj.isInfinite()) {
      res = new String("Infinity");
    }
    else {
      // Build a result according to the format requested.
      switch(format.ord()) {
      case Format.DMS_ORD:
        res = toStringDMS(digits,'\u00B0');
        break;
      case Format.DMS_ASCII_ORD:
        res = toStringDMS(digits,'^');
        break;
      case Format.HMS_ORD:
        res = toStringHMS(digits);
        break;
      case Format.DEGREES_ORD:
        // Make the string.
        res = toStringDegrees(digits);
        break;
      case Format.RADIANS_ORD:
        res = toStringRadians(digits);
        break;
      default:
        assert false;
        break;
      }
    }

    return res;
  }

  /**
   * Negate the value of this angle.
   * The result is returned in a new object.
   *
   * @return
   *  A reference to the new object.
   */
  public
  Angle
  negate()
  {
    return new Angle(-itsValue);
  }

  /**
   * Add a value to this angle.
   * The result is returned in a new object.
   *
   * @param valInc
   *  The value to add to this angle.
   *
   * @return
   *  A reference to the new object.
   */
  public
  Angle
  add(
    double valInc
  )
  {
    return new Angle(itsValue + valInc);
  }

  /**
   * Adds a value to this angle.
   * The result is returned in a new object.
   *
   * @param valInc
   *  The value to add to this angle.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  A reference to the new object.
   */
  public
  Angle
  add(
    Angle valInc
  )
  {
    return new Angle(itsValue + valInc.itsValue);
  }

  /**
   * Multiplies this angle by a scalar.
   * The value is returned in a new object.
   *
   * @param valMul
   *  The value to multiply this value by.
   *
   * @return
   *  A reference to the new object.
   */
  public
  Angle
  multiply(
    double valMul
  )
  {
    return new Angle(itsValue * valMul);
  }

  /**
   * Decimates this angle.
   * The result is returned in a new object.
   *
   * The resulting angle is guaranteed to be in the range
   * 0&nbsp;<=&nbsp;angle&nbsp;<&nbsp;360\u00B0.
   *
   * @return
   *  A reference to this object.
   */
  public
  Angle
  decimate()
  {
    // The new value.
    double newValue;

    // Figure out modulo 2PI.
    newValue = itsValue % (2.0 * Math.PI);
    // Could still be negative. Correct this.
    if(newValue < 0.0) {
      newValue = newValue + (2 * Math.PI);
    }

    return new Angle(newValue);
  }

  /**
   * Calculate the absolute value of this angle.
   * The result is returned in a new object.
   *
   * @return
   *  A reference to this object.
   */
  public
  Angle
  abs()
  {
    // Calculate the absolute value of the value.
    return new Angle(Math.abs(itsValue));
  }

  /**
   * Calculate the trigonometric sine of this angle.
   *
   * @return
   *  The sine of this angle.
   */
  public
  double
  sin()
  {
    // Take the trigonometric sine of the value.
    return (Math.sin(itsValue));
  }

  /**
   * Calculate the trigonometric cosine of this angle.
   *
   * @return
   *  The cosine of this angle.
   */
  public
  double
  cos()
  {
    // Take the trigonometric cosine of the value.
    return (Math.cos(itsValue));
  }

  /**
   * Calculate the trigonometric tangent of this angle.
   *
   * @return
   *  The tangent of this angle.
   */
  public
  double
  tan()
  {
    // Take the trigonometric tangent of the value.
    return (Math.tan(itsValue));
  }

  /**
   * Calculate the trigonometric arcsine of a number, and return an
   * angle with its value.
   *
   * @param x
   *  The value to take the arcsine of.
   *
   * @return
   *  A reference to a new angle whose value is the trigonometric
   *  arcsine of x.
   */
  public static
  Angle
  asin(
    double x
  )
  {
    // Make a new Angle of the correct value.
    return (new Angle(Math.asin(x)));
  }

  /**
   * Calculate the trigonometric arccosine of a number, and return an
   * angle with its value.
   *
   * @param x
   *  The value to take the arccosine of.
   *
   * @return
   *  A reference to a new angle whose value is the trigonometric
   *  arccosine of x.
   */
  public static
  Angle
  acos(
    double x
  )
  {
    // Make a new Angle of the correct value.
    return (new Angle(Math.acos(x)));
  }

  /**
   * Calculate the trigonometric arctangent of a number, and return an
   * angle with its value.
   *
   * @param x
   *  The value to take the arctangent of.
   *
   * @return
   *  A reference to a new angle whose value is the trigonometric
   *  arccosine of x.
   */
  public static
  Angle
  atan(
    double x
  )
  {
    // Make a new Angle of the correct value.
    return (new Angle(Math.atan(x)));
  }

  /**
   * Make a string of the object in DMS format.
   *
   * @param digits
   *  The number of digits for the floating part.
   *  Must not be less than zero.
   *
   * @param degsSep
   *  The degrees seperator.
   *  This is the character that is placed after the degrees field.
   *
   * @return
   *  The string represented in DMS.
   */
  private
  String
  toStringDMS(
    int digits,
    char degsSep
  )
  {
    // The string to return.
    String res;
    // Scratch value.
    double x;
    // The degrees field.
    long degs;
    // The minutes field.
    long mins;
    // The seconds field.
    long secs;
    // The seconds fraction field.
    double secsFrac;
    // String version of the seconds fraction field.
    String secsFracStr = null;
    // The formatter for the seconds fraction field.
    DecimalFormat secFmt;

    // Make the number formatter for the seconds fraction field.
    secFmt = new DecimalFormat("." + repeatChars('0',digits));

    // Convert the value to degrees (and take its absolute value).
    x = Math.abs(itsValue * DEGS_PER_RAD);
    degs = (long)x;
    // Calculate the minutes field.
    x = (x * MINS_PER_DEG) % MINS_PER_DEG;
    mins = (long)x;
    // Calculate the seconds field.
    x = (x * SECS_PER_MIN) % SECS_PER_MIN;
    secs = (long)x;
    // Calculate the seconds fraction field.
    secsFrac = x % 1.0;
    // If the requested number of decimal places is zero, we may need
    // to round up.
    if((digits == 0) && (secsFrac >=0.5)) {
      secs++;
      if(secs >= 60) {
        secs = secs - 60;
        mins++;
        if(mins >= 60) {
          mins = mins - 60;
          degs++;
        }
      }
    }
    // If the requested number of decimal places is non-zero, we may
    // still need to round up. This will be the case when the seconds
    // fraction string has been rounded up and looks like "1.xx".
    if(digits != 0) {
      secsFracStr = secFmt.format(secsFrac);
      if(secsFracStr.charAt(0) == '1') {
        // Change the seconds fraction string back to zero.
        secsFracStr = secFmt.format(0.0);
        // Round up.
        secs++;
        if(secs >= 60) {
          secs = secs - 60;
          mins++;
          if(mins >= 60) {
            mins = mins - 60;
            degs++;
          }
        }
      }
    }

    // Construct the return string.
    res = ((itsValue < 0) ? "-" : "") +
        degs + degsSep +
        theirMinFmt.format(mins) + "\'" +
        theirSecFmt.format(secs) + "\"";
    if(digits != 0) {
      res = res + secsFracStr;
    }

    return res;
  }

  /**
   * Make a string of the object in HMS format.
   *
   * @param digits
   *  The number of digits for the floating part.
   *  Must not be less than zero.
   *
   * @return
   *  The string representation of the object in HMS format.
   */
  private
  String
  toStringHMS(
    int digits
  )
  {
    // The string to return.
    String res;
    // Scratch value.
    double x;
    // The number of hours.
    long hrs;
    // The number of minutes.
    long mins;
    // The number of seconds.
    long secs;
    // The seconds fraction field.
    double secsFrac;
    // String version the seconds fraction field.
    String secsFracStr = null;
    // The formatter for the seconds fraction field.
    NumberFormat secFmt;

    // Make the number formatter for the seconds fraction field.
    secFmt = new DecimalFormat("." + repeatChars('0',digits));

    // Convert the value to hours (and take its absolute value).
    x = Math.abs(itsValue * HOURS_PER_RAD);
    hrs = (long)x;
    // Calculate the minutes field.
    x = (x * MINS_PER_HOUR) % MINS_PER_HOUR;
    mins = (long)x;
    // Calculate the seconds field.
    x = (x * SECS_PER_MIN) % SECS_PER_MIN;
    secs = (long)x;
    // Calculate the seconds fraction field.
    secsFrac = x % 1.0;
    // If the requested number of decimal places is zero, we may need
    // to round up.
    if((digits == 0) && (secsFrac >=0.5)) {
      secs++;
      if(secs >= 60) {
        secs = secs - 60;
        mins++;
        if(mins >= 60) {
          mins = mins - 60;
          hrs++;
        }
      }
    }
    // If the requested number of decimal places is non-zero, we may
    // still need to round up. This will be the case when the seconds
    // fraction string has been rounded up and looks like "1.xx".
    if(digits != 0) {
      secsFracStr = secFmt.format(secsFrac);
      if(secsFracStr.charAt(0) == '1') {
        // Change the seconds fraction string back to zero.
        secsFracStr = secFmt.format(0.0);
        // Round up.
        secs++;
        if(secs >= 60) {
          secs = secs - 60;
          mins++;
          if(mins >= 60) {
            mins = mins - 60;
            hrs++;
          }
        }
      }
    }

    // Construct the return string.
    res = ((itsValue < 0) ? "-" : "") +
        theirHrFmt.format(hrs) + ":" +
        theirMinFmt.format(mins) + ":" +
        theirSecFmt.format(secs);
    if(digits != 0) {
      res = res + secsFracStr;
    }

    return res;
  }

  /**
   * Make a string of the object in DEGREES format.
   *
   * @param digits
   *  The number of digits for the floating part.
   *  Must not be less than zero.
   *
   * @return
   *  The string represented in DEGREES format.
   */
  private
  String
  toStringDegrees(
    int digits
  )
  {
    // The string to return.
    String res;
    // The format specifier.
    NumberFormat fmt;

    // Make the format specifier for the fraction part.
    if(digits == 0) {
      fmt = new DecimalFormat("0");
    }
    else {
      fmt = new DecimalFormat("0." + repeatChars('0',digits));
    }

    // Convert it to a string.
    res = fmt.format(itsValue * DEGS_PER_RAD);
    if(digits == 0) {
      // Append a degrees sign.
      res = res + "\u00B0";
    }
    else {
      // Replace the decimal point with degrees sign and decimal point.
      res = res.substring(0,res.indexOf('.')) + "\u00B0." +
          res.substring(res.indexOf('.') + 1);
    }

    return res;
  }

  /**
   * Make a string of the object in RADIANS format.
   *
   * @param digits
   *  The number of digits for the floating part.
   *  Must not be less than zero.
   *
   * @return
   *  The object in RADIANS format.
   */
  private
  String
  toStringRadians(
    int digits
  )
  {
    // The format specifier.
    NumberFormat fmt;

    // Make the format specifier.
    if(digits == 0) {
      fmt = new DecimalFormat("#0");
    }
    else {
      fmt = new DecimalFormat("#0." + repeatChars('0',digits));
    }

    return fmt.format(itsValue);
  }

  /**
   * Parse a string.
   *
   * @param str
   *  The string to parse.
   *
   * @exception NumberFormatException
   *  Thrown when the string could not be converted.
   */
  private
  void
  parse(
    String str
  )
  {
    // Apply some heuristics to see which format the string is in.
    // As we find each, try to parse the string according to the
    // candidate format.

    // Trim whitespace from the string.
    str = str.trim();

    try {
      // See if the string can be parsed as radians.
      itsValue = parseRadians(str);
    }
    catch(NumberFormatException e1) {
      try {
        // See if the string can be parsed as HMS.
        itsValue = parseHMS(str);
      }
      catch(NumberFormatException e2) {
        try {
          // See if the string can be parsed as DMS. This will include
          // DEGREES format as on inptut is is a proper subset of DMS
          // (and DMS_ASCII).
          itsValue = parseDMS(str);
        }
        catch(NumberFormatException e3) {
          // There's no hope. Throw a number format exception with
          // the original string.
          throw new NumberFormatException("bad angle \"" + str + "\"");
        }
      }
    }
  }

  /**
   * Parse a string as DMS.
   *
   * @param str
   *  The string to parse as DMS.
   *  Assume that leading and trailing whitespace has been removed.
   *
   * @return
   *  The value of the angle in radians.
   *
   * @exception NumberFormatException
   *  The string is not a valid DMS.
   */
  private
  double
  parseDMS(
    String str
  )
  {
    // An index into the string.
    int idx;
    // Place to put the degrees value.
    long degs;
    // Place to put the minutes value.
    long mins;
    // Place to put the seconds value.
    long secs;
    // Place to put the fractional value.
    double frac;
    // The end value.
    double val;
    // True if the value is negative.
    boolean isNegative = false;

    if(str.equals("")) {
      throw new NumberFormatException("bad angle \"" + str + "\"");
    }

    try {
      // Figure out whether positive or negative. Remove the "-" from
      // the front of the string if it is negative.
      if(str.charAt(0) == '-') {
        isNegative = true;
        str = str.substring(1,str.length());
      }

      // Find the degrees string.
      idx = str.indexOf('\u00B0');
      if(idx == -1) {
        idx = str.indexOf('^');
      }
      if(idx == -1) {
        degs = 0;
      }
      else {
        // Extract a value from the degrees field.
        degs = (new Long(str.substring(0,idx))).longValue();
        if(degs < 0) {
          throw new NumberFormatException("bad angle \"" + str + "\"");
        }
        str = str.substring(idx + 1,str.length());
      }
      if(str.length() == 0) {
        // Nothing left to do.
        val = degs * RADS_PER_DEG;
      }
      else if(str.charAt(0) == '.') {
        // Fractional part is degrees. Also implies end of parsing
        // (ie, no minutes or seconds components).
        frac = (new Double(str)).doubleValue();
        if(frac < 0) {
          throw new NumberFormatException("bad angle \"" + str + "\"");
        }
        val = (degs + frac) * RADS_PER_DEG;
      }
      else {

        // Find the minutes string.
        idx = str.indexOf('\'');
        if(idx == -1) {
          mins = 0;
        }
        else {
          // Extract a value from the minutes field.
          mins = (new Long(str.substring(0,idx))).longValue();
          if(mins < 0) {
            throw new NumberFormatException("bad angle \"" + str + "\"");
          }
          str = str.substring(idx + 1,str.length());
        }
        if(str.length() == 0) {
          // Nothing left to do.
          val = (degs + mins / MINS_PER_DEG) * RADS_PER_DEG;
        }
        else if(str.charAt(0) == '.') {
          // Fractional part is minutes. Also implies end of parsing
          // (ie, no seconds component).
          frac = (new Double(str)).doubleValue();
          if(frac < 0) {
            throw new NumberFormatException("bad angle \"" + str + "\"");
          }
          val = (degs + (mins + frac) / MINS_PER_DEG) * RADS_PER_DEG;
        }
        else {

          // Find the seconds string.
          idx = str.indexOf('\"');
          if(idx == -1) {
            secs = 0;
          }
          else {
            // Extract a value from the seconds field.
            secs = (new Long(str.substring(0,idx))).longValue();
            if(secs < 0) {
              throw new NumberFormatException("bad angle \"" + str + "\"");
            }
            str = str.substring(idx + 1,str.length());
          }
          if(str.length() == 0) {
            // Nothing left to do.
            val = (degs + mins / MINS_PER_DEG +
                secs / SECS_PER_MIN / MINS_PER_DEG) * RADS_PER_DEG;
          }
          else {
            // Fractional part is seconds.
            frac = (new Double(str)).doubleValue();
            if(frac < 0) {
              throw new NumberFormatException("bad angle \"" + str + "\"");
            }
            val = (degs + mins / MINS_PER_DEG +
                (secs + frac) / SECS_PER_MIN / MINS_PER_DEG) * RADS_PER_DEG;
          }
        }
      }

    }
    catch(NumberFormatException e) {
      throw new NumberFormatException("bad angle \"" + str + "\"");
    }
    catch(Exception e) {
      throw new NumberFormatException("bad angle \"" + str + "\"");
    }

    if(isNegative) {
      val = -val;
    }

    return val;
  }

  /**
   * Parse a string as HMS.
   *
   * @param str
   *  The string to parse as HMS.
   *
   * @return
   *  THe value of the angle in radians.
   *
   * @exception NumberFormatException
   *  The string is not a valid HMS.
   */
  private
  double
  parseHMS(
    String str
  )
  {
    // The index of the first colon.
    int idx1;
    // The index of the second colon.
    int idx2;
    // Place to put the hours value.
    long hours;
    // Place to put the minutes value.
    long mins;
    // Place to put the seconds value.
    double secs;
    // The end value.
    double val;
    // True if the value is negative.
    boolean isNegative = false;

    try {
      // Find the first colon.
      idx1 = str.indexOf(':');
      // Make sure it was found.
      if(idx1 == -1) {
        throw new NumberFormatException("bad angle \"" + str + "\"");
      }
      // Find the second colon.
      idx2 = str.indexOf(':',idx1 + 1);
      // Make sure it was found.
      if(idx2 == -1) {
        throw new NumberFormatException("bad angle \"" + str + "\"");
      }

      // Extract the hours value.
      hours = Math.abs((new Long(str.substring(0,idx1))).longValue());
      if(str.charAt(0) == '-') {
        isNegative = true;
      }
      // Extract the minutes value.
      mins = (new Long(str.substring(idx1 + 1,idx2))).longValue();
      // Extract the seconds value.
      secs =
          (new Double(str.substring(idx2 + 1,str.length()))).doubleValue();

      // Figure out the value.
      val = (hours +
          (mins / MINS_PER_HOUR) +
          (secs / SECS_PER_MIN / MINS_PER_HOUR)) * RADS_PER_HOUR;
      if(isNegative) {
        val = -val;
      }
    }
    catch(Exception e) {
      throw new NumberFormatException("bad angle \"" + str + "\"");
    }

    return val;
  }

  /**
   * Parse a string as RADIANS.
   *
   * @param str
   *  The string to parse as RADIANS.
   *
   * @return
   *  The value of the angle in radians.
   *
   * @exception NumberFormatException
   *  The string is not a valid RADIANS.
   */
  private
  double
  parseRadians(
    String str
  )
  {
    // The value to return.
    double val;

    try {
      // Try to parse the string into a Double.
      val = (new Double(str)).doubleValue();
    }
    catch(Exception e) {
      throw new NumberFormatException("bad angle \"" + str + "\"");
    }

    return val;
  }

  /**
   * Make a string with a given number of a given character.
   *
   * @param ch
   *  The character to make the string out of.
   *
   * @param len
   *  The number of characters in the string.
   *
   * @return
   *  A string made up of <code>ch</code> repeated <code>len</code>
   *  times.
   */
  private static
  String
  repeatChars(
    char ch,
    int len
  )
  {
    // A string buffer to build the string in.
    StringBuffer str;

    // Make the string buffer.
    str = new StringBuffer(len);

    // Fill it with the characters.
    for(int iCh = 0 ; iCh < len ; iCh++) {
      str.append(ch);
    }

    return new String(str);
  }
}
