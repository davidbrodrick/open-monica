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

import java.util.*;
import java.io.*;

/**
 * Class representing an enumeration.
 *
 * <p>
 * An enumeration class is one that extends this class.
 * Such a class should have the following features:
 * <ul>
 *  <li>it extends
 *    <code><a href="Enum.html">atnf.atoms.util.Enum</a></code>.</li>
 *  <li>it implements a default constructor that has the
 *    <code>private</code> attribute.
 *    This prevents default instantiation of the class.
 *    This constructor should look like:
 *    <ul>
 *<pre>private
 *<i>Xxx</i>()
 *{
 *  super();
 *}</pre>
 *    </ul></li>
 *  <li>it implements a "real" constructor,
 *    also with the <code>private</code> attribute.
 *    This constructor should have the signature
 *    <code><i>xxx</i>(int,String)</code>,
 *    and simply calls the corresponding constructor
 *    of this class.
 *    This constructor should look like:
 *    <ul>
 *<pre>private
 *<i>Xxx</i>(
 *  int ord,
 *  <a href="/java.lang.String">String</a> message
 *)
 *{
 *  <a href="#Enum(int, java.lang.String)">super</a>(ord,message);
 *}</pre>
 *    </ul></li>
 * </ul>
 *
 * <p>
 * This class provides some facilities for manipulating classes
 * that extend it.
 * These features are provided by some static methods:
 * <ul>
 *  <li><code><a href="#getAll(java.lang.Class)">getAll</a>(<a href="/java.lang.Class">Class</a>)</code>:
 *    Retrieves an array of all the values of the enum
 *    indexed on their ordinal values.</li>
 *  <li><code><a href="#factory(java.lang.Class, java.lang.Strin)">factory</a>(<a href="/java.lang.Class">Class</a>,<a href="/java.lang.String">String</a>)</code>:
 *    Retrieves an enum element given its string representation.</li>
 *  <li><code><a href="#factory(java.lang.Class, int)">factory</a>(<a href="/java.lang.Class">Class</a>,int)</code>:
 *    Retrieves an enum element given its ordinal value.</li>
 * </ul>
 *
 * <p>
 * The enumeration class should declare static members
 * (with the <code>static</code> and <code>final</code> attributes)
 * that represent the actual values of the enumeration.
 * The ATOMS recommendation is that these variables should
 * use all uppercase characters.
 * It should also declare symbolic constants of type <code>int</code>
 * (also with the <code>static</code> and <code>final</code> attributes)
 * that represent the ordinal values of each value of the enumeration.
 * The ATOMS recommendation is that these variables should have the
 * same name as the actual enumeration value,
 * with "<code>_ORD</code>" appended.
 *
 * @author
 *  David G Loone
 *
 * @version $Id: Enum.java,v 1.23 2004/09/07 05:08:58 bro764 Exp $
 *
 * @see atnf.atoms.util.ShapeInclude
 * @see EnumEditor
 */
abstract public
class Enum
implements
  Cloneable,
  Immutable,
  Serializable
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: Enum.java,v 1.23 2004/09/07 05:08:58 bro764 Exp $";

  /**
   * All the alignments that can be used to put an enumeration into
   * a fixed text field.
   */
  final public static
  class
  Align
  extends
    Enum
  {

    /**
     * Represents ordinal value of LEFT.
     */
    final public static
    int
    LEFT_ORD = 0;

    /**
     * Represents left alignment.
     */
    final public static
    Align
    LEFT = new Align(LEFT_ORD,"LEFT");

    /**
     * Represents the ordinal value of "CENTRE".
     */
    final public static
    int
    CENTRE_ORD = 1;

    /**
     * Represents centre alignment.
     */
    final public static
    Align
    CENTRE = new Align(CENTRE_ORD,"CENTRE");

    /**
     * Represents the ordinal value of RIGHT.
     */
    final public static
    int
    RIGHT_ORD = 2;

    /**
     * Represents right alignment.
     */
    final public static
    Align
    RIGHT = new Align(RIGHT_ORD,"RIGHT");

    /**
     * Don't allow null instantiations.
     */
    private
    Align()
    {}

    /**
     * Make a new alignment object.
     *
     * @param ord
     *  The ordinal value of the alignment.
     *
     * @param tag
     *  The string value of the alignment.
     */
    private
    Align(
      int ord,
      String tag
    )
    {
      super(ord,tag);
    }

  }

  /**
   * An iterator class for a particular enum class.
   */
  final private static
  class
  EnumIterator
  implements
    Iterator
  {

    /**
     * The array that we are iterating over.
     */
    private
    Object[] itsElements;

    /**
     * The current element.
     */
    private
    int itsIndex;

    /**
     * Make a new enum iterator object.
     *
     * @param enumClass
     *  The enum class to make the iterator for.
     */
    public
    EnumIterator(
      Class enumClass
    )
    {
      // Find the array list corresponding to this enum class.
      itsElements = Enum.getAll(enumClass);

      // Set the index.
      itsIndex = 0;
    }

    /**
     * See if there are any more elements to iterate over.
     *
     * @return
     *  The value <code>true</code> if there are more elements,
     *  <code>false</code> otherwise.
     */
    public
    boolean
    hasNext()
    {
      return (itsIndex < itsElements.length);
    }

    /**
     * Return the next element.
     *
     * @return
     *  The next element in the iteration.
     *
     * @exception NoSuchElementException
     *  There are no more elements.
     */
    public
    Object
    next()
    throws
      NoSuchElementException
    {
      // The value to return.
      Object result;

      if(itsIndex == itsElements.length) {
        throw new NoSuchElementException();
      }
      else {
        result = itsElements[itsIndex++];
      }

      return result;
    }

    /**
     * Remove the current element.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    void
    remove()
    {
      throw new UnsupportedOperationException();
    }

  }

  /**
   * A hashtable that maps enumeration classes to vectors of elements.
   * Keys of this map are the enumeration classes
   * (objects of class <code><a href="/java.lang.Class">Class</a></code>).
   * Values of this map are vectors of the values of that enum class
   * (objects of class <code><a href="/java.util.ArrayList">ArrayList</a></code>).
   */
  private static
  Hashtable theirClasses = new Hashtable();

  /**
   * A hashtable that maps enumeration classes to field lengths.
   * Keys of this map are the enumeration classes
   * (objects fo class <code><a href="/java.lang.Class">Class</a></code>).
   * Values of this map are integers
   * (objects of class <code><a href="/java.lang.Integer">Integer</a></code>).
   */
  private static
  Hashtable theirFieldLengths = new Hashtable();

  /**
   * The ordinal value of the enumeration.
   * A value of -1 indicates that the object has no ordinal value.
   */
  private
  int itsOrd;

  /**
   * The message associated with the enumeration.
   */
  private
  String itsMessage;

  /**
   * Default constructor to prevent outside instantiations.
   */
  protected
  Enum()
  {}

  /**
   * Make a new enum object.
   * Use this form where ordinal values of the objects in a
   * particular enum class are unimportant.
   *
   * <p>For a given enum class,
   * this form of the constructor should never be mixed with the
   * other form of constructor,
   * <i>ie</i>, all elements of a particular enum set should have
   * ordinal values,
   * or they all should not have ordinal values.</p>
   *
   * @param message
   *  The string message for this value of the enumeration.
   *  This string is returned by the <code>toString()</code>
   *  method of the enumeration.
   */
  protected
  Enum(
    String message
  )
  {
    assert message!=null;

    synchronized(theirClasses) {

      // This class.
      Class thisClass = getClass();
      // The vector representing this enum class.
      ArrayList elements = (ArrayList)theirClasses.get(thisClass);
      // The field length.
      Integer fieldLength = (Integer)theirFieldLengths.get(thisClass);

      // Fill in the attributes for this enumeration value.
      itsOrd = -1;
      itsMessage = message;

      // Make sure the vector of elements for this enum class is in the map.
      if(elements == null) {
        // Haven't got any elements of this enum class yet, so need
        // to make the vector.
        elements = new ArrayList();
        theirClasses.put(thisClass,elements);
      }

      // Now add this enum object to the object vector.
      elements.add(this);

      // Figure out the field length.
      if(fieldLength == null) {
        fieldLength = new Integer(message.length());
      }
      else {
        // The message length.
        Integer messageLength = new Integer(message.length());

        if(fieldLength.compareTo(messageLength) < 0) {
          fieldLength = messageLength;
        }
      }
      theirFieldLengths.put(thisClass,fieldLength);
    }
  }

  /**
   * Make a new enum object.
   * Use this form where the elements are to have ordinal values.
   *
   * <p>For a given enum class,
   * this form of the constructor should never be mixed with the
   * other form of constructor,
   * <i>ie</i>, all elements of a particular enum set should have
   * ordinal values,
   * or they all should not have ordinal values.</p>
   *
   * @param ord
   *  The ordinal value for this value of the enumeration.
   *  This ordinal value for this enum class must be empty.
   *
   * @param message
   *  The string message for this value of the enumeration.
   *  This string is returned by the <code>toString()</code>
   *  method of the enumeration.
   */
  protected
  Enum(
    int ord,
    String message
  )
  {
    assert message != null;

    synchronized(theirClasses) {

      // This class.
      Class thisClass = getClass();
      // The vector representing this enum class.
      ArrayList elements = (ArrayList)theirClasses.get(thisClass);
      // The field length.
      Integer fieldLength = (Integer)theirFieldLengths.get(thisClass);

      // Fill in the attributes for this enumeration value.
      itsOrd = ord;
      itsMessage = message;

      // Make sure the vector of elements for this enum class is in the map.
      if(elements == null) {
        // Haven't got any elements of this enum class yet, so need
        // to make the vector.
        elements = new ArrayList();
        theirClasses.put(thisClass,elements);
      }

      // Now add this enum object to the object vector, first making sure
      // that it isn't already filled.
      if(elements.size() <= ord) {
        while(elements.size() < ord) {
          elements.add(null);
        }
        elements.add(this);
      }
      else {
        assert (elements.size() < ord) || (elements.get(ord) == null);
        elements.set(ord,this);
      }

      // Figure out the field length.
      if(fieldLength == null) {
        fieldLength = new Integer(message.length());
      }
      else {
        // The message length.
        Integer messageLength = new Integer(message.length());

        if(fieldLength.compareTo(messageLength) < 0) {
          fieldLength = messageLength;
        }
      }
      theirFieldLengths.put(thisClass,fieldLength);
    }
  }

  /**
   * Clone the object.
   *
   * @return
   *  A new object identical to this one.
   */
  final public
  Object
  clone()
  {
    // This object is imutable, so just return this.
    return this;
  }

  /**
   * Test for equivalence between two objects.
   *
   * @param o
   *  The object to test for equivalence against.
   *
   * @return
   *  The value <code>true</code> if the objects are equivalent,
   *  <code>false</code> otherwise.
   */
  final public
  boolean
  equiv(
    Object o
  )
  {
    // This object is imutable, so test against this.
    return (o == this);
  }

  /**
   * Get the object's ordinal value.
   *
   * @return
   *  The ordinal value of the enumeration.
   *  A value of -1 indicates that the object has no ordinal value.
   */
  final public
  int
  ord()
  {
    return itsOrd;
  }

  /**
   * Make a string of the object.
   *
   * @return
   *  The message associated with the enumeration.
   */
  final public
  String
  toString()
  {
    return itsMessage;
  }

  /**
   * Make a string of the object,
   * in a field that is the size of the longest string representation
   * of is enumeration syblings.
   *
   * @return
   *  A string the length of the longest enumeration value,
   *  containing the string representation of this object,
   *  centre aligned and padded with spaces.
   */
  final public
  String
  toStringInField()
  {
    return toStringInField(Align.CENTRE);
  }

  /**
   * Make a string of the object,
   * in a field that is the size of the longest string representation
   * of is enumeration syblings.
   *
   * @param align
   *  The alignment of the string representation of the object
   *  within the field.
   *
   * @return
   *  A string the length of the longest enumeration value,
   *  containing the string representation of this object.
   */
  final public
  String
  toStringInField(
    Align align
  )
  {
    // The field length.
    int fieldLength =
        ((Integer)theirFieldLengths.get(this.getClass())).intValue();
    // The value to return.
    StringBuffer result = new StringBuffer(fieldLength);

    switch(align.ord()) {
      case Align.LEFT_ORD: {
        result.append(itsMessage);
        for(int i = 0; i < (fieldLength - itsMessage.length()); i++) {
          result.append(" ");
        }
        break;
      }
      case Align.CENTRE_ORD: {
        // Spaces before.
        int spacesBefore = (fieldLength - itsMessage.length()) / 2;
        // Spaces after.
        int spacesAfter = fieldLength - itsMessage.length() - spacesBefore;

        for(int i = 0; i < spacesBefore; i++) {
          result.append(" ");
        }
        result.append(itsMessage);
        for(int i = 0; i < spacesAfter; i++) {
          result.append(" ");
        }
        break;
      }
      case Align.RIGHT_ORD: {
        for(int i = 0; i < (fieldLength - itsMessage.length()); i++) {
          result.append(" ");
        }
        result.append(itsMessage);
        break;
      }
      default: {
        // Should never happen.
        assert false;
        result = null;
        break;
      }
    }

    return result.toString();
  }

  /**
   * Get an array of all the members of an enum.
   *
   * @param enumClass
   *  The enumeration class to get all the values of.
   *  Must be a subclass of <code>Enum</code>.
   *
   * @return
   *  An array containing all the elements of the enum.
   *  None of the elements will be equal to <code>null</code>.
   */
  public static
  Object[]
  getAll(
    Class enumClass
  )
  {
    assert enumClass != null;
    assert Enum.class.isAssignableFrom(enumClass);

    // The value to return.
    Object[] result;

    synchronized(theirClasses) {

      // The vector of elements.
      ArrayList elements = (ArrayList)theirClasses.get(enumClass);

      // Check the vector of elements.
      if(elements == null) {
        result = new Object[0];
      }
      else {
        // The number of elements.
        int nElements;
        // The result element counter.
        int iResult;

        // Calculate the number of elements.
        nElements = 0;
        for(Iterator iE = elements.iterator(); iE.hasNext(); ) {
          if(iE.next() != null) {
            nElements++;
          }
        }

        // Make the result array and fill it.
        result = new Object[nElements];
        iResult = 0;
        for(Iterator iE = elements.iterator(); iE.hasNext(); ) {
          // The next element.
          Object element = iE.next();

          if(element != null) {
            result[iResult++] = element;
          }
        }
        assert iResult == result.length;
      }

    }

    return result;
  }

  /**
   * Get an array of all the members of an enum.
   *
   * @param enumClass
   *  The enumeration class to get all the values of.
   *  Must be a subclass of <code>Enum</code>.
   *
   * @return
   *  An array containing all the elements of the enum,
   *  indexed by their ordinal values.
   *  Elements corresponding to ordinal values that do not exist will
   *  be <code>null</code>.
   */
  public static
  Object[]
  getAllOrd(
    Class enumClass
  )
  {
    assert enumClass != null;
    assert Enum.class.isAssignableFrom(enumClass);

    // The value to return.
    Object[] result;

    synchronized(theirClasses) {

      // The vector of elements.
      ArrayList elements = (ArrayList)theirClasses.get(enumClass);

      // Check the vector of elements.
      if(elements == null) {
        result = new Object[0];
      }
      else {
        result = elements.toArray();
      }

    }

    return result;
  }

  /**
   * Make an iterator for a particular enum class.
   *
   * @param enumClass
   *  The enum class to return an iterator for.
   */
  public static
  Iterator
  iterator(
    Class enumClass
  )
  {
    return new EnumIterator(enumClass);
  }

  /**
   * Generate an enum from an int.
   *
   * @param enumClass
   *  The enumeration class to get the value from.
   *  Must be a subclass of <code>Enum</code>.
   *
   * @param ord
   *  The ordinal value.
   *  Must not be negative.
   *
   * @return
   *  The enumeration value for that ordinal value.
   *  If there was no value corresponding to the ordinal value,
   *  then a <code>null</code> is returned.
   *
   * @exception IllegalArgumentException
   *  Thrown when <code>enumClass</code> is does not extend
   *  <code>Enum</code>.
   */
  public static
  Enum
  factory(
    Class enumClass,
    int ord
  )
  throws
    IllegalArgumentException
  {
    assert enumClass != null;
    assert Enum.class.isAssignableFrom(enumClass);
    assert ord >= 0;

    // The enum to return.
    Enum result;

    synchronized(theirClasses) {

      // The vector of elements.
      ArrayList elements = (ArrayList)theirClasses.get(enumClass);

      // Check the vector of elements.
      if(elements == null) {
        result = null;
      }
      else {
        result = (Enum)elements.get(ord);
      }

    }

    return result;
  }

  /**
   * Generate an enum from a string.
   *
   * @param enumClass
   *  The enumeration class to get the value from.
   *  Must be a subclass of <code>Enum</code>.
   *
   * @param str
   *  The string to search for.
   *
   * @return
   *  The enum element found.
   *
   * @exception IllegalArgumentException
   *  Thrown when <code>enumClass</code> is does not extend
   *  <code>Enum</code>.
   *
   * @exception NumberFormatException
   *  An enum object could not be found for that string.
   */
  public static
  Enum
  factory(
    Class enumClass,
    String str
  )
  throws
    IllegalArgumentException,
    NumberFormatException
  {
    assert str != null;

    // The enum to return.
    Enum result;

    // The elements of this enum class.
    Object[] elements = getAll(enumClass);

    // Iterate over the elements, checking the strings.
    result = null;
    for(int i = 0; i < elements.length; i++) {
      if(elements[i].toString().equals(str)) {
        result = (Enum)elements[i];
        break;
      }
    }

    // Check that we found one.
    if(result == null) {
      throw new NumberFormatException(
          "The string \"" + str + "\" is not an element of \"" +
          enumClass.getName() + "\"");
    }

    return result;
  }
}

