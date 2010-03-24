// Copyright (C)1997 CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Library General Public License for more details.
// 
// A copy of the GNU Library General Public License is available at:
// http://wwwatoms.atnf.csiro.au/doc/gnu/GLGPL.htm
// or, write to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA 02111-1307 USA

package atnf.atoms.time;

import java.io.*;
import java.util.*;
import java.text.*;

import atnf.atoms.util.Immutable;
import atnf.atoms.util.Enum;

/**
 * General purpose absolute time class.
 * 
 * <p>
 * An absolute time represents an epoch and cannot be negative. Adding two
 * <code>AbsTime</code> values makes no sense, but it is possible to add a
 * <code>RelTime</code> value to a <code>AbsTime</code>. It is also possible to find
 * the difference between two <code>AbsTime</code> values, with the result being a
 * <code>RelTime</code> value.
 * 
 * <p>
 * There are two "special" times, which are represented by their own objects they are:
 * <ul>
 * <li><code>NEVER</code>: The time will never come. All times are "before"
 * <code>NEVER</code> except <code>NEVER</code> itself. Adding any relative time to
 * <code>NEVER</code> yields <code>NEVER</code>.</li>
 * <li><code>ASAP</code>: The required time is as soon as reasonably possible.
 * <code>ASAP</code> is "before" any other times except <code>ASAP</code> itself.</li>
 * </ul>
 * 
 * <p>
 * The string representation of a <code>AbsTime</code> is specified by any object of
 * type <code>AbsTime.Format</code>. Each of these formats is augmenged by the
 * following literals:
 * <ul>
 * <li><code>NEVER</code></li>
 * <li><code>ASAP</code></li>
 * </ul>
 * 
 * @author David G Loone
 * 
 * @version $Id: AbsTime.java,v 1.46 2006/09/13 04:09:02 bro764 Exp $
 * 
 * @see RelTime
 */
final public class AbsTime implements Cloneable, Serializable, Immutable, Comparable
{

    /**
     * The RCS id.
     */
    final public static String RCSID = "$Id: AbsTime.java,v 1.46 2006/09/13 04:09:02 bro764 Exp $";

    /**
     * String formats for absolute times.
     * 
     * Objects of this class represent the different string formats that an
     * <code>AbsTime</code> value can take.
     */
    final public static class Format extends Enum
    {

        /**
         * Indicates the ordinal value of <code>DECIMAL_BAT</code>.
         */
        final public static int DECIMAL_BAT_ORD = 1;

        /**
         * Indicates a string formatted as decimal BAT. This format is equivalent to an
         * unsigned decimal integer.
         */
        final public static Format DECIMAL_BAT = new Format(DECIMAL_BAT_ORD, "DECIMAL_BAT");

        /**
         * Indicates the ordinal value of <code>FORMATTED_BAT</code>.
         */
        final public static int FORMATTED_BAT_ORD = 2;

        /**
         * Indicates a string formatted as formatted BAT. The string consists of the time
         * represented in hexadecimal with a space every four digits. The format looks
         * something like "<samp>XXXX&nbsp;XXXX&nbsp;XXXX&nbsp;XXXX</samp>". There is
         * always 16 hexadecimal digits (filled with leading zeros if necessary). The
         * total length of the string is always 19 digits.
         */
        final public static Format FORMATTED_BAT = new Format(FORMATTED_BAT_ORD, "FORMATTED_BAT");

        /**
         * Indicates the ordinal value of <code>HEX_BAT</code>.
         */
        final public static int HEX_BAT_ORD = 3;

        /**
         * Indicates a string formatted as simple BAT. The string consists of the time
         * represented as a hexadecimal number. The first string is prefixed by "<samp>0x</samp>",
         * then between 1 and 16 hexadecimal digits.
         */
        final public static Format HEX_BAT = new Format(HEX_BAT_ORD, "HEX_BAT");

        /**
         * Indicates the ordinal value of <code>SECS_BAT</code>.
         */
        final public static int SECS_BAT_ORD = 4;

        /**
         * Indicates a string formatted as decimal seconds. The string looks like a
         * floating number which is the seconds equivalent of the numerical BAT.
         */
        final public static Format SECS_BAT = new Format(SECS_BAT_ORD, "SECS_BAT");

        /**
         * Indicates the ordinal value of <code>UTC_STRING</code>.
         */
        final public static int UTC_STRING_ORD = 5;

        /**
         * Indicates a string formatted as a UTC date/time string in the format YYYY-MM-DD
         * HH:MM:SS.sss
         */
        final public static Format UTC_STRING = new Format(UTC_STRING_ORD, "UTC_STRING");

        /**
         * Default constructor.
         * 
         * Makes the class uninstantiatable by outsiders.
         */
        private Format()
        {
            super();
        }

        /**
         * Constructor.
         * 
         * @param ord The ordinal value of the format.
         * 
         * @param message The string message for the format.
         */
        private Format(int ord, String message)
        {
            super(ord, message);
        }

    }

    /**
     * Serialized version id.
     */
    static final long serialVersionUID = 7041403759606247682L;

    /**
     * An object representing ASAP.
     */
    final public static AbsTime ASAP = new AbsTime(false);

    /**
     * An object representing NEVER.
     */
    final public static AbsTime NEVER = new AbsTime(true);

    /**
     * The special value used to represent <code>ASAP</code>.
     */
    final static long ASAP_CODE = 0x0L;

    /**
     * The special value used to represent <code>NEVER</code>.
     */
    final static long NEVER_CODE = 0xFFFFFFFFFFFFFFFFL;

    /**
     * The current default output format.
     */
    private static Format theirDefaultFormat = Format.HEX_BAT;

    /**
     * Time is stored as a <code>long</code> and converted to whatever else is requred.
     * The <code>long</code> data type provides us with the 64-bit number required to
     * store a BAT.
     * 
     * <p>
     * The following special values apply:
     * <ul>
     * <li><code>ASAP_CODE</code>: <code>ASAP</code></li>
     * <li><code>NEVER_CODE</code>: <code>NEVER</code></li>
     * </ul>
     */
    long itsValue = 0L;

    /**
     * Makes a time that is equivalent to NOW to the best ability of the host. Note that
     * the quality of this time may vary wildly between hosts and implementations.
     * 
     * @return The new object.
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when absolute time is not available on
     * the host.
     */
    public static AbsTime factory() throws Time.Ex_TimeNotAvailable
    {
        return new AbsTime();
    }

    /**
     * Makes a time that is equivalent to NOW to the best ability of the host. Note that
     * the quality of this time may vary wildly between hosts and implementations.
     * 
     * <p>
     * Recommend to use the corresponding factory method instead. This constructor remains
     * intact only in order to support serialization.
     * </p>
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when absolute time is not available on
     * the host.
     */
    public AbsTime() throws Time.Ex_TimeNotAvailable
    {
        itsValue = timeNow();
    }

    /**
     * Construct an object representing a specific time given by a <code>long</code>.
     * 
     * @param t A number that is the required BAT.
     * 
     * @return The new object.
     */
    public static AbsTime factory(long t)
    {
        // The value to return.
        AbsTime result;

        // Check for ASAP and NEVER.
        if (t == ASAP_CODE) {
            // No need to make a new object.
            result = ASAP;
        } else if (t == NEVER_CODE) {
            // No need to make a new object.
            result = NEVER;
        } else {
            // Not a special case. Make a new object.
            result = new AbsTime(t);
        }

        return result;
    }

    /**
     * Construct an object representing a specific time given by a <code>long</code>.
     * 
     * @param t A number that is the required BAT. Must not be one of the special codes.
     */
    private AbsTime(long t) throws IllegalArgumentException
    {
        itsValue = t;
    }

    /**
     * Construct an object from an existing <code>AbsTime</code> object.
     * 
     * @param t The existing <code>AbsTime</code> object. It must not be equal to
     * <code>null</code>.
     * 
     * @return The new value.
     */
    public static AbsTime factory(AbsTime t)
    {
        // The value to return.
        AbsTime result;

        if (t.isASAP() || t.isNEVER()) {
            // No need to make a new object.
            result = t;
        } else {
            // Not a special case. Make a new object.
            result = new AbsTime(t);
        }

        return result;
    }

    /**
     * Construct an object from an existing <code>AbsTime</code> object.
     * 
     * @param t The existing <code>AbsTime</code> object. It must not be equal to
     * <code>null</code>.
     */
    private AbsTime(AbsTime t)
    {
        // Copy the value.
        itsValue = t.itsValue;
    }

    /**
     * Construct an object representing a specific time. The time represented by the
     * object will be now, plus a specified relative time.
     * 
     * @param dt The time offset for the new absolute time. A positive value will make a
     * time in the future. Must not be equal to <code>null</code>.
     * 
     * @return The new object.
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when absolute time is not available on
     * the host.
     * 
     * @exception IllegalArgumentException Thrown if <code>dt</code> is
     * <code>null</code>.
     */
    public static AbsTime factory(RelTime dt) throws Time.Ex_TimeNotAvailable, IllegalArgumentException
    {
        return new AbsTime(dt);
    }

    /**
     * Construct an object representing a specific time. The time represented by the
     * object will be now, plus a specified relative time.
     * 
     * @param dt The time offset for the new absolute time. A positive value will make a
     * time in the future. Must not be equal to <code>null</code>.
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when absolute time is not available on
     * the host.
     * 
     * @exception IllegalArgumentException Thrown if <code>dt</code> is
     * <code>null</code>.
     */
    private AbsTime(RelTime dt) throws Time.Ex_TimeNotAvailable, IllegalArgumentException
    {
        // The value of 3506716800000000L is the BAT as at midnight on
        // 1-Jan-1970, which is the base of the time that the system
        // gives us. It has to be adjusted for leap seconds though.
        itsValue = (System.currentTimeMillis() * 1000L) + DUTC.get() * 1000000L + 3506716800000000L;

        // Add the specified time offset.
        itsValue = itsValue + dt.itsValue;
    }

    /**
     * Construct an object representing a specific time specified by a string.
     * 
     * The following special string tokens are accepted:
     * <ul>
     * <li><code>ASAP</code></li>
     * <li><code>NEVER</code></li>
     * <li><code>NOW</code></li>
     * </ul>
     * 
     * @param t A string representation of an absolute time in any of the valid string
     * formats.
     * 
     * @return The new value.
     * 
     * @exception NumberFormatException Thrown when <code>t</code> is not a valid string
     * representation of an <code>AbsTime</code> object.
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when a time of <code>NOW</code> is
     * requested, but real time is not available on this host.
     */
    public static AbsTime factory(String t) throws NumberFormatException, Time.Ex_TimeNotAvailable
    {
        // The value to return.
        AbsTime result;

        // Check for ASAP and NEVER.
        if (t.equals("ASAP")) {
            // No need to make a new object.
            result = ASAP;
        } else if (t.equals("NEVER")) {
            // No need to make a new object.
            result = NEVER;
        } else if (t.equals("NOW")) {
            // Make a new object using null constructor.
            result = new AbsTime();
        } else {
            // Not a special case. Need to make a new object.
            result = new AbsTime(t);
        }

        return result;
    }

    /**
     * Construct an object representing a specific time specified by a string.
     * 
     * @param t A string representation of an absolute time in any of the valid string
     * formats.
     * 
     * @exception NumberFormatException Thrown when <code>t</code> is not a valid string
     * representation of an <code>AbsTime</code> object.
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when a time of <code>NOW</code> is
     * requested, but real time is not available on this host.
     */
    private AbsTime(String t) throws NumberFormatException, Time.Ex_TimeNotAvailable
    {
        // Parse the string.
        parse(t);
    }

    /**
     * Constructs an object representing the java Date
     * @param date Can you guess?
     * @return The object created
     */
    private AbsTime(Date date)
    {
        // The value of 3506716800000000L is the BAT as at midnight on
        // 1-Jan-1970, which is the base of the time that the system
        // gives us. It has to be adjusted for leap seconds though.
        itsValue = date.getTime() * 1000L + DUTC.get() * 1000000L + 3506716800000000L;
    }

    /**
     * Make an object from a <code>java.util.Date</code> object.
     * @param date The date to convert. Really shouldn't be null
     * @return The AbsTime object created
     */
    public static AbsTime factory(Date date)
    {
        return new AbsTime(date);
    }

    /**
     * Construct an object representing either <code>ASAP</code> or <code>NEVER</code>.
     * 
     * @param t A value of <code>false</code> makes <code>ASAP</code>. A value of
     * <code>true</code> makes <code>NEVER</code>.
     * 
     * @deprecated Use the corresponding factory method instead.
     */
    private AbsTime(boolean t)
    {
        if (!t) {
            itsValue = ASAP_CODE;
        } else {
            itsValue = NEVER_CODE;
        }
    }

    /**
     * Clone the object.
     * 
     * @return A new object identical to this one.
     */
    public Object clone()
    {
        // The class is imutable. Just return this.
        return this;
    }

    /**
     * Test for equivalence.
     * 
     * @param o The object to test for equivalence against.
     * 
     * @return Returns <code>true</code> if <code>o</code> is equivalent to this
     * object, <code>false</code> otherwise.
     */
    public boolean equiv(Object o)
    {
        // The value to return.
        boolean isEquiv = false;
        // Local version of o.
        AbsTime o1;

        // Prerequisites.
        if ((o != null) && (o.getClass() == getClass())) {
            o1 = (AbsTime) o;
            // Is equiv if same object.
            if (equals(o1)) {
                isEquiv = true;
            } else {
                // Test the attributes.
                isEquiv = (o1.itsValue == itsValue);
            }
        }

        return isEquiv;
    }

    /**
     * Set the default string format for the class.
     * 
     * @param fmt The new default string format for the class.
     */
    public static void setTheirDefaultFormat(Format fmt)
    {
        theirDefaultFormat = (Format) fmt.clone();
    }

    /**
     * Get the default string format for the class.
     * 
     * @return The current default string format for the class.
     */
    public static Format getTheirDefaultFormat()
    {
        return (Format) theirDefaultFormat.clone();
    }

    /**
     * Return the value of the time as a long.
     * 
     * @return The value of the time as a long.
     */
    public long getValue()
    {
        return itsValue;
    }

    /**
     * Get the value of the time as seconds.
     * 
     * @return The value of the time as seconds since the zero epoch.
     */
    public double getAsSeconds()
    {
        return itsValue / 1000000.0;
    }

    /**
     * Convert the time to a string in the "default" format.
     * 
     * @return A string containing the representation of the object in the "default"
     * format.
     */
    public String toString()
    {
        return toString((Format) null);
    }

    /**
     * Convert the time to a string in any given format.
     * 
     * @param format Any object of type <code>Format</code>. A value of
     * <code>null</code> indicates to use the current default format for the class.
     * 
     * @return A string containing the representation of the number according to the
     * format defined by <code>format</code>.
     */
    public String toString(Format format)
    {
        // The result to return.
        String res = null;

        // Check for null value passed in.
        if (format == null) {
            format = getTheirDefaultFormat();
        }

        // Check for the special values.
        if (isASAP()) {
            res = "ASAP";
        } else if (isNEVER()) {
            res = "NEVER";
        } else {
            // Check format against each available format.
            switch (format.ord()) {
            case Format.HEX_BAT_ORD: {
                // Just use the default hex conversion function, and prepend
                // the "0x".
                res = "0x" + Long.toHexString(itsValue);
                break;
            }
            case Format.FORMATTED_BAT_ORD: {
                // String to hold the zero filled BAT.
                String fBAT;

                // Make the filled BAT string.
                fBAT = fillStr(Long.toHexString(itsValue), 16);
                // Disect the filled BAT string and create the formatted
                // string.
                res = fBAT.substring(0, 4) + " " + fBAT.substring(4, 8) + " " + fBAT.substring(8, 12) + " "
                        + fBAT.substring(12, 16);
                break;
            }
            case Format.DECIMAL_BAT_ORD: {
                // Just use the default decimal conversion function.
                res = Long.toString(itsValue);
                break;
            }
            case Format.SECS_BAT_ORD: {
                // Convert it to a string as DECIMAL_BAT, then insert the
                // decimal point.
                res = toString(Format.DECIMAL_BAT);
                res = res.substring(0, res.length() - 6) + "." + res.substring(res.length() - 6);
                break;
            }
            case Format.UTC_STRING_ORD: {
                // Convert to UTC date/time string
                Date d = this.getAsDate();
                DateFormat outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                outdfm.setTimeZone(TimeZone.getTimeZone("GMT"));
                res = outdfm.format(d);
                break;
            }
            default: {
                assert false;
                break;
            }
            }
        }

        return res;
    }

    /**
     * Add a period to this object and return it as a new object. This method just
     * instanciates a RelTime object and calls the other add method. It is provided for
     * convenience only.
     * <P>
     * Adding to <code>ASAP</code> is not defined and throws an exception. Adding to
     * <code>NEVER</code> always yields <code>NEVER</code>.
     * 
     * @param t2 The amount of time to add to this object.
     * 
     * @return A reference to the new object.
     * 
     * @exception ArithmeticException Thrown if
     * <ul>
     * <li>the resulting time would be out of the range of an <code>AbsTime</code>.</li>
     * <li>this object is <code>ASAP</code>.</li>
     * </ul>
     */
    public AbsTime add(long t2) throws ArithmeticException
    {
        return add(RelTime.factory(t2));
    }

    /**
     * Add a relative time to this object and return it as a new object. Adding to
     * <code>ASAP</code> is not defined and throws an exception. Adding to
     * <code>NEVER</code> always yields <code>NEVER</code>.
     * 
     * @param t2 The time to add to this object. Must not be equal to <code>null</code>.
     * 
     * @return A reference to the new object.
     * 
     * @exception ArithmeticException Thrown if
     * <ul>
     * <li>the resulting time would be out of the range of an <code>AbsTime</code>.</li>
     * <li>this object is <code>ASAP</code>.</li>
     * </ul>
     */
    public AbsTime add(RelTime t2) throws ArithmeticException
    {
        // The result of the addition.
        long res;
        // The result of the addition as an object.
        AbsTime resObj;

        // Check this for ASAP.
        if (isASAP()) {
            throw new ArithmeticException("t2 == ASAP");
        }

        // Check this for NEVER.
        if (isNEVER()) {
            // Never plus anything gives never.
            resObj = NEVER;
        } else {
            // Add the offset. Check for a negative result.
            res = itsValue + t2.itsValue;
            if (res <= 0) {
                throw new ArithmeticException("overflow");
            }
            resObj = new AbsTime(res);
        }

        // Return the new time.
        return resObj;
    }

    /**
     * Test for the special time <code>ASAP</code>.
     * 
     * @return The value <code>true</code> if this object is equal to the special time
     * <code>ASAP</code>.
     */
    public boolean isASAP()
    {
        return equiv(ASAP);
    }

    /**
     * Test the for special time <code>NEVER</code>.
     * 
     * @return The value <code>true</code> if this object is equal to the special time
     * <code>NEVER</code>.
     */
    public boolean isNEVER()
    {
        return equiv(NEVER);
    }

    /**
     * Compare with another absolute time.
     * 
     * @param refEpoch The reference epoch to test against. Must not be equal to
     * <code>null</code>.
     * 
     * @return The value -1 if this time is before the <code>refEpoch</code>, 0 if
     * times are equal, +1 otherwise.
     */
    public int compare(AbsTime refEpoch)
    {
        // Code for NEVER should have been 0x7FFFFFFFFFFFFFFFL
        final long LAST = 0x7FFFFFFFFFFFFFFFL;
        long e1 = (itsValue == NEVER_CODE ? LAST : itsValue);
        long e2 = (refEpoch.itsValue == NEVER_CODE ? LAST : refEpoch.itsValue);

        return (e1 < e2 ? -1 : (e1 == e2 ? 0 : 1));
    }

    /**
     * Compare for before another absolute time.
     * 
     * @param refEpoch The reference epoch to test against. Must not be equal to
     * <code>null</code>.
     * 
     * @return The value <code>true</code> this time is before the <code>refEpoch</code>,
     * <code>false</code> otherwise.
     */
    public boolean isBefore(AbsTime refEpoch)
    {
        // The result to return
        boolean res;

        // Check for special values.
        if (isASAP()) {
            // ASAP is before any other time, except ASAP.
            if (refEpoch.isASAP()) {
                res = false;
            } else {
                res = true;
            }
        } else if (isNEVER()) {
            // NEVER is after any other time, except NEVER.
            res = false;
        } else if (refEpoch.isASAP()) {
            // No time is before ASAP, including ASAP.
            res = false;
        } else if (refEpoch.isNEVER()) {
            // Any time is before NEVER, except NEVER, which is taken care
            // of above.
            res = true;
        } else {
            // Do the comparison.
            res = (itsValue < refEpoch.itsValue);
        }

        return res;
    }

    /**
     * Compare for before or same as another absolute time.
     * 
     * @param refEpoch The reference epoch to test against. Must not be equal to
     * <code>null</code>.
     * 
     * @return The value <code>true</code> this time is before or equal to
     * <code>refEpoch</code>, <code>false</code> otherwise.
     */
    public boolean isBeforeOrEquals(AbsTime refEpoch)
    {
        // The result to return.
        boolean res;

        // Check for special values.
        if (isASAP()) {
            // ASAP is before or equal to any time, including ASAP.
            res = true;
        } else if (isNEVER()) {
            // NEVER only before or equal to NEVER.
            if (refEpoch.isNEVER()) {
                res = true;
            } else {
                res = false;
            }
        } else if (refEpoch.isASAP()) {
            // No time is before or equal ASAP, except ASAP, which is already
            // considered above.
            res = false;
        } else if (refEpoch.isNEVER()) {
            // Any time is before or equal to NEVER, including NEVER.
            res = true;
        } else {
            // Do the comparison.
            res = (itsValue <= refEpoch.itsValue);
        }

        return res;
    }

    /**
     * Compare for after another absolute time.
     * 
     * @param refEpoch The reference epoch to test against. Must not be equal to
     * <code>null</code>.
     * 
     * @return The value <code>true</code> this time is after <code>refEpoch</code>,
     * <code>false</code> otherwise.
     */
    public boolean isAfter(AbsTime refEpoch)
    {
        return !isBeforeOrEquals(refEpoch);
    }

    /**
     * Compare for after or equal to another absolute time.
     * 
     * @param refEpoch The reference epoch to test against. Must not be equal to
     * <code>null</code>.
     * 
     * @return The value <code>true</code> this time is after or equal to
     * <code>refEpoch</code>, <code>false</code> otherwise.
     */
    public boolean isAfterOrEquals(AbsTime refEpoch)
    {
        return !isBefore(refEpoch);
    }

    /**
     * Make the calling thread wait until the time represented by this object. Speeping
     * until <code>ASAP</code> is defined as doing nothing. Sleeping until
     * <code>NEVER</code> is not defined and an exception is thrown. The method is
     * implemented in such a way that it is safe for many threads to be sleeping on the
     * same object simultaneously.
     * 
     * @exception InterruptedException The thread was awoken during its sleep.
     * 
     * @exception IllegalArgumentException Thrown when the object represents
     * <code>NEVER</code>.
     * 
     * @exception Ex_TimeNotAvailable Time is not available on this host.
     * 
     * @exception Ex_TooLate The time represented by the object is in the past.
     */
    public void sleep() throws InterruptedException, IllegalArgumentException, Time.Ex_TimeNotAvailable, Time.Ex_TooLate
    {
        // The current time.
        AbsTime now;
        // The duration to wait.
        RelTime duration;

        // Make error if NEVER.
        if (isNEVER()) {
            throw new IllegalArgumentException("this == NEVER");
        }

        // Don't do anything if ASAP.
        if (!isASAP()) {

            // Calculate the time now.
            now = new AbsTime();

            // Figure out how long to wait for.
            duration = Time.diff(this, now);

            // If duration is negative, throw the Ex_TooLate exception.
            if (duration.getValue() < 0L) {
                throw new Time.Ex_TooLate();
            }

            // Do the wait.
            duration.sleep();
        }
    }

    /**
     * Calcualte the time of NOW.
     * 
     * @return The current BAT (from the system clock).
     */
    private long timeNow()
    {
        // The value of 3506716800000000L is the BAT as at midnight on
        // 1-Jan-1970, which is the base of the time that the system
        // gives us. It has to be adjusted for leap seconds though.
        return (System.currentTimeMillis() * 1000L) + DUTC.get() * 1000000L + 3506716800000000L;
    }

    /**
     * Parse a string into an absolute time.
     * 
     * @param str The string to parse.
     * 
     * @exception NumberFormatException Thrown when <code>t</code> is not a valid string
     * representation of an <code>AbsTime</code> object.
     * 
     * @exception Time.Ex_TimeNotAvailable Thrown when a time of <code>NOW</code> is
     * requested, but real time is not available on this host.
     */
    private void parse(String str) throws NumberFormatException, Time.Ex_TimeNotAvailable
    {
        // Remove any extraneous whitespace before parsing
        str = str.trim();

        // Apply some heuristics to str to figure out what format it
        // is in, then call the appropriate parsing function.
        if ((str.length() >= 3) && (str.substring(0, 2).equals("0x"))) {
            // If the string begins with "0x", then it should be HEX_BAT.
            parseHexBAT(str);
        } else if (str.charAt(4) == ' ') {
            // Fifth character is a space, therefore should be in FORMATTED_BAT
            // format.
            parseFormattedBAT(str);
        } else if (str.charAt(4) == '-') {
            // Fifth character is a "-", therefore should be in UTC_STRING format.
            parseUTCString(str);
        } else if (str.indexOf('.') != -1) {
            // The string contains a "." charcater. Try to parse it as SECS_BAT.
            parseSecsBAT(str);
        } else {
            // Fall through to DECIMAL_BAT format.
            parseDecimalBAT(str);
        }
        // Check that we didn't create an ASAP or NEVER by mistake.
        if (isASAP() || isNEVER()) {
            throw new NumberFormatException("absolute time reserved value: \"" + str + "\"");
        }
    }

    /**
     * Parse a string as <code>UTC_STRING</code> format.
     * 
     * @param str The string formatted as <code>UTC_STRING</code>.
     * 
     * @exception NumberFormatException Thrown when <code>str</code> is not a valid
     * <code>UTC_STRING</code> string. The value of the object is left unchanged.
     */
    private void parseUTCString(String str)
    {
        SimpleDateFormat formatter;
        // Treat the sub-second part as optional
        if (str.indexOf(".") != -1) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = formatter.parse(str);
            itsValue = date.getTime() * 1000L + DUTC.get() * 1000000L + 3506716800000000L;
        } catch (Exception e) {
            throw new NumberFormatException("Couldn't parse date as UTC_STRING");
        }
    }

    /**
     * Attempt to parse a string as <code>HEX_BAT</code>. Set the value of this object
     * to the result.
     * 
     * <p>
     * Note: broken for times greater than 0x0FFFFFFFFFFFFFFF. This is left because these
     * times are not physically relevent.
     * 
     * @param str The string formatted as <code>HEX_BAT</code>.
     * 
     * @exception NumberFormatException Thrown when <code>str</code> is not a valid
     * <code>HEX_BAT</code> string. The value of the object is left unchanged.
     */
    private void parseHexBAT(String str) throws NumberFormatException
    {
        // A trimmed version of the input string.
        String trimStr;

        // We know that the first two characters are "0x". Remove them.
        trimStr = str.substring(2, str.length());

        // Make sure the first character is a "-" character. That would
        // be a valid Java hex value, but not a valid HEX_BAT format.
        if (trimStr.charAt(0) == '-') {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }

        // Look for the special value 0xFFFFFFFFFFFFFFFF.
        if (str.equals("0xFFFFFFFFFFFFFFFF")) {
            itsValue = NEVER_CODE;
        } else {
            // Looks ok. Try to parse the string as a hex long.
            try {
                itsValue = Long.parseLong(trimStr, 16);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("bad absolute time: \"" + str + "\"");
            }
        }

    }

    /**
     * Attempt to parse a string as <code>FORMATTED_BAT</code>. Set the value of this
     * object to the result.
     * 
     * <p>
     * Note: broken for times greater than 0x0FFFFFFFFFFFFFFF. This is left because these
     * times are not physically relevent.
     * 
     * @param str The string formatted as <code>FORMATTED_BAT</code>.
     * 
     * @exception NumberFormatException Thrown when <code>str</code> is not a valid
     * <code>FORMATTED_BAT</code> string. The value of the object is left unchanged.
     */
    private void parseFormattedBAT(String str) throws NumberFormatException
    {
        // A trimed version of the input string.
        String trimStr;

        // Check for first char is "-", since this would make a valid
        // long, but which we don't want to be a valid time.
        if (str.charAt(0) == '-') {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }

        // Remove all the spaces.
        trimStr = removeWhitespace(str);

        // Should be ok now to parse the string as a hex long.
        try {
            itsValue = Long.parseLong(trimStr, 16);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }
    }

    /**
     * Attempt to parse a string as <code>DECIMAL_BAT</code>. Set the value of this
     * object to the result.
     * 
     * <p>
     * Note: broken for times greater than 0x7FFFFFFFFFFFFFFF. This is left because these
     * times are not physically relevent.
     * 
     * @param str The string formatted as <code>DECIMAL_BAT</code>.
     * 
     * @exception NumberFormatException Thrown when <code>str</code> is not a valid
     * <code>DECIMAL_BAT</code> string. The value of the object is left unchanged.
     */
    private void parseDecimalBAT(String str) throws NumberFormatException
    {
        // The value.
        long theValue;

        // Try to parse the string as a decimal long.
        try {
            theValue = Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }

        // Check for a negative number and reject if so.
        if (theValue <= 0) {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }

        // Set the value in the object.
        itsValue = theValue;
    }

    /**
     * Attempt to parse a string as <code>SECS_BAT</code>. Set the value of the object
     * to the result.
     * 
     * param str The string formatted as <code>SECS_BAT</code>.
     * 
     * @exception NumberFormatException Thrown when <code>str</code> is not a valid
     * <code>SECS_BAT</code> string. The value of the object is left unchanged.
     */
    private void parseSecsBAT(String str)
    {
        // The value.
        long theValue;

        // Try to parse the string as a double.
        try {
            // theValue = (long)(Double.parseDouble(str) * 1000000.0);
            theValue = (long) ((new Double(str)).doubleValue() * 1000000.0);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }

        // Check for a negative number.
        if (theValue <= 0) {
            throw new NumberFormatException("bad absolute time: \"" + str + "\"");
        }

        // Set the value in the object.
        itsValue = theValue;
    }

    /**
     * Get the value of this object as a Date
     */
    public Date getAsDate()
    {
        // Nope, shouldn't do that
        if (isASAP() || isNEVER()) {
            return null;
        }

        // The value of 3506716800000000L is the BAT as at midnight on
        // 1-Jan-1970, which is the base of the time that the system
        // gives us. It has to be adjusted for leap seconds though.
        return new Date((itsValue - DUTC.get() * 1000000L - 3506716800000000L) / 1000L);
    }

    /**
     * Get the value of this object as a decimal Modified Julian Date.
     */
    public double getAsMJD()
    {
        return (itsValue - DUTC.get(itsValue) * 1000000l) / 86400000000.0;
    }

    /**
     * Fill a string representation of a number with leading zeros to the indicated string
     * length.
     * 
     * @param str The input string.
     * 
     * @param len The character length of the output string.
     * 
     * @return A string equivalent to <code>str</code>, but filled with leading zeros
     * and the length given by <code>len</code>.
     * 
     * @exception IllegalArgumentException Thrown when the requested string length,
     * <code>len</code>, is not greater than the existing string length,
     * <code>str.length()</code>.
     */
    private static String fillStr(String str, int len) throws IllegalArgumentException
    {
        // String buffer containing the leading zeros.
        StringBuffer leaders;

        // The requested length must be greater or equal to the
        // string length.
        if (len <= str.length()) {
            throw new IllegalArgumentException("len less than length of string");
        }

        // Make the string buffer with leading zeros and fill it.
        leaders = new StringBuffer(len - str.length());
        for (int iChar = 0; iChar < (len - str.length()); iChar++) {
            leaders.append('0');
        }

        // Construct and return the resulting string.
        return (leaders.toString() + str);
    }

    /**
     * Trim all white space from a string (leading, trailing and included).
     * 
     * @param str The string to trim whitespace from.
     * 
     * @return A string that is <code>str</code> with all the white space removed.
     */
    private static String removeWhitespace(String str)
    {
        // A string buffer to build the result in.
        StringBuffer res;

        // Create the result string buffer.
        res = new StringBuffer(str.length());

        // Iterate through the input string, appending chars to the
        // result if they are not white space.
        for (int iChar = 0; iChar < str.length(); iChar++) {
            if (!Character.isWhitespace(str.charAt(iChar))) {
                res.append(str.charAt(iChar));
            }
        }

        return res.toString();
    }

    /**
     * Print usage and exit.
     */
    private static void usage()
    {
        System.out.println("usage:");
        System.out.println("  ??? clock [-bg color] [-f fontsize] [-fg color]");
        System.out.println("  ??? formats");
        System.out.println("  ??? print [-f fmt] time");
        System.out.println("  ??? now [-r] [-f fmt]");
        System.exit(1);
    }

    /**
     * Command line utilities.
     * 
     * <p>
     * The first argument is a command, subsequent options and arguments are command
     * specific. Valid commands are (along with subsequent options and arguments):
     * <ul>
     * <li><code><b>clock</b></code><br>
     * Make a GUI BAT clock. Options are:
     * <ul>
     * <li><code>-bg <i>color</i></code><br>
     * Specifies the initial background color.</li>
     * <li><code>-f <i>fontsize</i></code><br>
     * Specifies the font size of the clock display.</li>
     * <li><code>-fg <i>color</i></code><br>
     * Specifies the initial foreground color.</li>
     * </ul>
     * <li><code><b>formats</b></code><br>
     * Print a list of the valid formats.</li>
     * <li><code><b>print [-f <i>fmt</i>] <i>time</i></b></code><br>
     * Print the value of <code><i>time</i></code>. Options are:
     * <ul>
     * <li><code>-f <i>fmt</i></code><br>
     * Specifies the format to print the time in. The <i>fmt</i> is one of the valid
     * format specifiers. If this option is not specified, the default format is used.</li>
     * </ul>
     * The string <code><i>time</i></code> can be in any of the valid string formats
     * for an absolute time.</li>
     * <li><code><b>now [-f <i>fmt</i>] [-r]</b></code><br>
     * Print the current time. Options are:
     * <ul>
     * <li><code>-f <i>fmt</i></code><br>
     * Specifies the format to print the time in. The <i>fmt</i> is one of the valid
     * format specifiers. If this option is not specified, the default format is used.</li>
     * <li><code>-r</code><br>
     * Specifies that the time is automatically updated every second.</li>
     * </ul>
     * </li>
     * </ul>
     */
    public static void main(String args[])
    {
        // Check for no arguments.
        if (args.length == 0) {
            usage();
        }

        // Check the command.
        if (args[0].equals("test")) {

            AbsTime now = new AbsTime();
            AbsTime later = new AbsTime(RelTime.factory(10));

            System.out.println("compare now, later: " + now.compare(later));
            System.out.println("compare later, now: " + later.compare(now));
            System.out.println("compare now, now: " + now.compare(now));
            System.out.println("compare ASAP, later: " + ASAP.compare(later));
            System.out.println("compare NEVER, later: " + NEVER.compare(later));
            System.out.println("compare now, ASAP: " + now.compare(ASAP));
            System.out.println("compare now, NEVER: " + now.compare(NEVER));
            System.out.println("compare ASAP, ASAP: " + ASAP.compare(ASAP));
            System.out.println("compare NEVER, NEVER: " + NEVER.compare(NEVER));
            System.out.println("compare ASAP, NEVER: " + ASAP.compare(NEVER));
            System.out.println("compare NEVER, ASAP: " + NEVER.compare(ASAP));

            AbsTime.setTheirDefaultFormat(Format.UTC_STRING);
            System.out.println(new AbsTime() + " doing 200000 compares");
            for (int i = 0; i < 100000; i++) {
                now.compare(later);
                now.compare(NEVER);
            }
            System.out.println(new AbsTime() + " doing 200000 isBefore()s");
            for (int i = 0; i < 100000; i++) {
                now.isBefore(later);
                now.isBefore(NEVER);
            }
            System.out.println(new AbsTime() + " doing 200000 isAfter()s");
            for (int i = 0; i < 100000; i++) {
                now.isAfter(later);
                now.isAfter(NEVER);
            }
            System.out.println(new AbsTime() + " done");

        } else if (args[0].equals("clock")) {
            // The current argument.
            int iArg;

            // Parse the command line options.
            iArg = 1;
            while (iArg < args.length) {
                usage();
            }

            // Make the clock.
            // BATClock clock = new BATClock(reqFont,reqBGColor,reqFGColor);
        } else if (args[0].equals("formats")) {
            System.out.println("DECIMAL_BAT");
            System.out.println("DEFAULT_FORMAT");
            System.out.println("FORMATTED_BAT");
            System.out.println("HEX_BAT");
            System.out.println("UTC_STRING");
        } else if (args[0].equals("print")) {
            // The current argument.
            int iArg;
            // The requested format.
            Format reqFmt = AbsTime.getTheirDefaultFormat();
            // The time.
            AbsTime time = null;

            // Parse the command line options.
            iArg = 1;
            while (iArg < args.length) {
                if (args[iArg].equals("-f")) {
                    iArg++;
                    if (iArg >= args.length) {
                        usage();
                    }
                    if (args[iArg].equals("DECIMAL_BAT")) {
                        reqFmt = Format.DECIMAL_BAT;
                    } else if (args[iArg].equals("DEFAULT_FORMAT")) {
                        reqFmt = AbsTime.getTheirDefaultFormat();
                    } else if (args[iArg].equals("FORMATTED_BAT")) {
                        reqFmt = Format.FORMATTED_BAT;
                    } else if (args[iArg].equals("HEX_BAT")) {
                        reqFmt = Format.HEX_BAT;
                    } else if (args[iArg].equals("UTC_STRING")) {
                        reqFmt = Format.UTC_STRING;
                    } else {
                        usage();
                    }
                    iArg++;
                } else if (args[iArg].charAt(0) != '-') {
                    // Not an option, must be the time.
                    try {
                        time = new AbsTime(args[iArg]);
                    } catch (Time.Ex_TimeNotAvailable e) {
                        System.out.println(e.toString());
                    }
                    iArg++;
                    // Check that this is the last arg.
                    if (iArg < args.length) {
                        usage();
                    }
                } else {
                    // Invalid arg.
                    usage();
                }
            }

            // Print the time object.
            if (time != null) {
                System.out.println(time.toString(reqFmt));
            } else {
                usage();
            }
        } else if (args[0].equals("now")) {
            // The current argument.
            int iArg;
            // The requested format.
            Format reqFmt = AbsTime.getTheirDefaultFormat();
            // True if repeat was requested.
            boolean doRpt = false;

            // Parse the command line options.
            iArg = 1;
            while (iArg < args.length) {
                if (args[iArg].equals("-f")) {
                    iArg++;
                    if (iArg >= args.length) {
                        usage();
                    }
                    if (args[iArg].equals("DECIMAL_BAT")) {
                        reqFmt = Format.DECIMAL_BAT;
                    } else if (args[iArg].equals("DEFAULT_FORMAT")) {
                        reqFmt = AbsTime.getTheirDefaultFormat();
                    } else if (args[iArg].equals("FORMATTED_BAT")) {
                        reqFmt = Format.FORMATTED_BAT;
                    } else if (args[iArg].equals("HEX_BAT")) {
                        reqFmt = Format.HEX_BAT;
                    } else if (args[iArg].equals("UTC_STRING")) {
                        reqFmt = Format.UTC_STRING;
                    } else {
                        usage();
                    }
                    iArg++;
                } else if (args[iArg].equals("-r")) {
                    doRpt = true;
                    iArg++;
                } else {
                    usage();
                }
            }

            // Make an AbsTime object, then print it in the appropriate
            // format.
            try {
                if (doRpt) {
                    while (true) {
                        System.out.print((new AbsTime()).toString(reqFmt) + "\r");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    System.out.println((new AbsTime()).toString(reqFmt));
                }
            } catch (Time.Ex_TimeNotAvailable e) {
                System.out.println("time not available on this host");
                System.exit(1);
            }
        } else {
            // Bad command argument.
            usage();
        }
    }

    /**
     * Compare the timestamp with another AbsTime.
     */
    public int compareTo(Object obj)
    {
        if (obj instanceof AbsTime) {
            if (((AbsTime) obj).getValue() < itsValue) {
                return 1;
            }
            if (((AbsTime) obj).getValue() > itsValue) {
                return -1;
            }
            return 0;
        } else {
            System.err.println("AbsTime: compareTo: UNKNOWN TYPE!");
            return -1;
        }
    }
}
