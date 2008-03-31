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

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.beans.*;
import java.io.*;
import java.net.*;

import atnf.atoms.util.*;

/**
 * Models a list of strongly typed name/value pairs.
 *
 * <p>Objects of this class maintain a list of name/value pairs
 * where each value is forced to be an object of a given class.
 * When the object is constructed the set of names is given and
 * is fixed for the life of the object.
 * The data type for each entry is also given at object construction
 * time and fixed for the life of the object.
 * It is <b>not</b> possible to add or remove name/value pairs
 * after construction.</p>
 *
 * <p>The object keeps track of which values have been modified,
 * and makes it possible to get a list of only the modified
 * values.
 * This makes it possible to efficiently write the value back to a
 * device where it is expected that only a small number of the values
 * will have been changed.</p>
 *
 * @author
 *  David G Loone
 *
 * @version $Id: NamedValueList.java,v 1.21 2004/09/07 04:53:55 bro764 Exp $
 *
 * @see NamedValueListEditor
 * @see atnf.atoms.beans.UniversalPropertyEditor
 *
 * @see <a href="/rt/tcldoc/?javaClass=atnf.atoms.util.NamedValueList">Tcl representation</a>
 */
public
class NamedValueList
implements
  Cloneable,
  Serializable,
  Map
{

  /**
   * The RCS id.
   */
  final public static
  String RCSID = "$Id: NamedValueList.java,v 1.21 2004/09/07 04:53:55 bro764 Exp $";

  // We store:
  //  - an array containing the names (itsNames).
  //  - an array containing the data types (itsTypes).
  //  - an array containing the values (itsValues).
  //  - an array containing the old values (itsOldValues).
  //  - an array containing the modified flags (itsModified).
  //  - an array containing the readonly flags (itsReadonlyFlags).
  // The use of arrays provides for efficient iteration through the
  // list. This is possible since insertion and removal of elements
  // from the named value list is not supported.
  //
  // There is also a hash table (itsNameToIndexMap) that can be used
  // to look up the index into the above arrays given a name. This is
  // used for efficiency, since we would otherwise have to do a
  // linear search on hte itsNames array.
  //
  // Note the use of package private attributes to allow access from
  // the NamedValueListEditor class.

  /**
   * Class to provide a set view of the keys in the value list as per
   * the documentation for the <code>Map</code> interface.
   * Since elements cannot be added to or removed from the named value list,
   * neither can elements be added to or removed from this set view.
   * This provides support for the <code>getKeySet</code> and
   * <code>getKeySetModified</code> methods.
   * The main function of the class is to support creation of an
   * iterator for both those cases,
   * so some of the methods are not implemented.
   */
  class KeySetView
  implements
    Set
  {

    /**
     * Key set view iterator class for all elements.
     * This iterator visits all the elements of the map.
     */
    class KeySetViewIterator
    implements
      Iterator
    {

      // No removal or insertion into the set is allowed, so it's
      // pretty simple.

      /**
       * The index of the iterator.
       */
      private
      int itsIndex = -1;

      /**
       * Make a new key set view iterator.
       */
      public
      KeySetViewIterator()
      {}

      /**
       * See if there is a next object.
       *
       * @return
       *  The value <code>true</code> if there is a next element,
       *  <code>false</code> otherwise.
       */
      public
      boolean
      hasNext()
      {
        return ((itsIndex + 1) != itsNames.length);
      }

      /**
       * Get the next element.
       *
       * @return
       *  The next element.
       */
      public
      Object
      next()
      {
        itsIndex++;
        if(itsIndex == itsNames.length) {
          throw new NoSuchElementException();
        }
        return itsNames[itsIndex];
      }

      /**
       * Remove the current element from the set.
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
     * Key set view iterator class for modified elements only.
     * This iterator visits only the elements of the named value list
     * whose modified flag is set.
     */
    class KeySetViewIteratorModified
    implements
      Iterator
    {

      // No removal or insertion into the set is allowed, so it's
      // pretty simple.

      /**
       * The index of the iterator.
       */
      private
      int itsIndex = -1;

      /**
       * Make a new key set view iterator.
       */
      public
      KeySetViewIteratorModified()
      {}

      /**
       * See if there is a next object.
       *
       * @return
       *  The value <code>true</code> if there is a next element,
       *  <code>false</code> otherwise.
       */
      public
      boolean
      hasNext()
      {
        // The result to return.
        boolean result;

        result = false;
        for(int i = itsIndex + 1; i < itsNames.length; i++) {
          if(itsModified[i]) {
            result = true;
            break;
          }
        }

        return result;
      }

      /**
       * Get the next element.
       *
       * @return
       *  The next element.
       */
      public
      Object
      next()
      {
        itsIndex++;
        while(itsIndex < itsNames.length) {
          if(itsModified[itsIndex]) {
            break;
          }
          itsIndex++;
        }
        if(itsIndex == itsNames.length) {
          throw new NoSuchElementException();
        }

        return itsNames[itsIndex];
      }

      /**
       * Remove the current element from the set.
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
     * Am I a set of modified values only?
     */
    private
    boolean itsModifiedOnly;

    /**
     * Make a new set view of the value list.
     *
     * @param modifiedOnly
     *  Set this to <code>true</code> if the set is only of the
     *  modified values in the named value list.
     *  Set it to <code>false</code> if the set is to include all
     *  the values in the named value list.
     */
    public
    KeySetView(
      boolean modifiedOnly
    )
    {
      itsModifiedOnly = modifiedOnly;
    }

    /**
     * Return the size of the set.
     *
     * @return
     *  The number of elements in the set.
     */
    public
    int
    size()
    {
      return itsThis.size();
    }

    /**
     * See if the set is empty.
     *
     * @return
     *  The value <code>true</code> if the set is empty,
     *  <code>false</code> otherwise.
     */
    public
    boolean
    isEmpty()
    {
      return itsThis.isEmpty();
    }

    /**
     * See if the list contains an element with a given name.
     *
     * @param key
     *  The name to check for.
     *  Must be an object of class <code>String</code>.
     *  Must not be equal to <code>null</code>.
     *
     * @return
     *  The value <code>true</code> if the list contains an element
     *  called <code>key</code>,
     *  <code>false</code> otherwise.
     *
     * @exception ClassCastException
     *  Thrown if the key is not of class <code>String</code>.
     *
     * @exception NullPointerException
     *  Thrown if <code>key</code> is equal to <code>null</code>.
     */
    public synchronized
    boolean
    contains(
      Object key
    )
    throws
      ClassCastException,
      NullPointerException
    {
      return itsThis.containsKey(key);
    }

    /**
     * Returns an iterator over the elements of the set.
     * Currently not implemented.
     *
     * @return
     *  An iterator over the elemetns in the set.
     */
    public
    Iterator
    iterator()
    {
      // The iterator to return.
      Iterator result;

      if(itsModifiedOnly) {
        result = new KeySetViewIteratorModified();
      }
      else {
        result = new KeySetViewIterator();
      }

      return result;
    }

    /**
     * Returns an array containing all the elements in the set.
     *
     * @return
     *  An array containing all the elements in the set.
     */
    public
    Object[]
    toArray()
    {
      return toArray(new Object[0]);
    }

    /**
     * Returns an array containing all the elements in the set whose
     * runtime type is that of the specified array.
     *
     * @param a
     *  The array to fill.
     *
     * @return
     *  An array containing the elements.
     */
    public
    Object[]
    toArray(
      Object[] a
    )
    {
      // The names to return.
      String[] names;
      // The array iterator.
      int i;

      // Figure out which names to return.
      if(itsModifiedOnly) {
        // Only the modified names. Make an array of just the modified
        // names.

        // The number of modified elements.
        int nModified;
        // Iterator.
        int j;

        // First figure out how many have been modified.
        nModified = 0;
        for(int k = 0; k < itsModified.length; k++) {
          if(itsModified[k]) {
            nModified++;
          }
        }

        // Make the names array.
        names = new String[nModified];

        // And fill it.
        j = 0;
        for(int k = 0; k < itsModified.length; k++) {
          if(itsModified[k]) {
            names[j] = itsNames[k];
            j++;
          }
        }
      }
      else {
        // This is the simple case. Return all names.
        names = itsNames;
      }

      // See if we need to make a new array, or use the one passed to us.
      if(a.length < names.length) {
        // Make the new array.
        a = (Object[])Array.newInstance(a.getClass().getComponentType(),
            names.length);
      }

      // Transfer elements from itsNames to a.
      for(i = 0; i < names.length; i++) {
        a[i] = names[i];
      }

      // If there is room to spare then set the next element to null.
      if(i < a.length) {
        a[i] = null;
      }

      return a;
    }

    /**
     * Add an element to the set.
     *
     * @param o
     *  The object to add.
     *
     * @return
     *  The value <code>true</code> if the set did not already contain
     *  the element.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    boolean
    add(
      Object o
    )
    throws
      UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Remove an object from the set.
     *
     * @param o
     *  The object to remove.
     *
     * @return
     *  The value <code>true</code> if the set containied the element.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    boolean
    remove(
      Object o
    )
    throws
      UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * See if the set contains all the elements of a collection.
     * Currently not implemented.
     *
     * @param c
     *  The collection to test against.
     *
     * @return
     *  The value <code>true</code> if the set is a superset of the
     *  colleciton.
     */
    public
    boolean
    containsAll(
      Collection c
    )
    {
      throw new Ex_NotImplemented();
    }

    /**
     * Adds all the elements of a collection to the set.
     *
     * @param c
     *  The collection to add.
     *
     * @return
     *  The value <code>true</code> if the set changed as a result.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    boolean
    addAll(
      Collection c
    )
    throws
      UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Retain only the elements of the set that are in a collection.
     *
     * @param c
     *  The collection whose elements we should retain.
     *
     * @return
     *  The value <code>true</code>if the set changed as a result of
     *  the call.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    boolean
    retainAll(
      Collection c
    )
    throws
      UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Remove all the elements in a collection from the set.
     *
     * @param c
     *  The collection whose elemets we remove from the set.
     *
     * @return
     *  The value <code>true</code> if the set changed as a result of
     *  the call.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    boolean
    removeAll(
      Collection c
    )
    throws
      UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Remove all elements from the set.
     *
     * @exception UnsupportedOperationException
     *  Always thrown.
     */
    public
    void
    clear()
    throws
      UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

  }

  /**
   * A reference to ourself for inner classes to use.
   */
  private
  Map itsThis = this;

  /**
   * The list of names.
   * There is one element for each named value.
   */
  String[] itsNames;

  /**
   * The list of data types.
   * The length of this array will always be the same as the length of
   * the <code>itsNames</code> array.
   */
  Class[] itsTypes;

  /**
   * The list of values.
   * The length of this array will always be the same as the length of
   * the <code>itsNames</code> array.
   * If the required type is an array,
   * then the corresponding element of <code>itsValues</code> will be
   * an array.
   * This array maintains its size for the lifecycle of the object.
   */
  Object[] itsValues;

  /**
   * The list of old values.
   * The length of this array will always be the same as the length of
   * the <code>itsNames</code> array.
   */
  Object[] itsOldValues;

  /**
   * The list of which values have changed.
   * The length of this array will always be the same as the length of
   * the <code>itsNames</code> array.
   * Synchronize on this object when things are being modified.
   */
  boolean[] itsModified;

  /**
   * The readonly flags.
   * The length of this array will always be the same as the length of
   * the <code>itsNames</code> array.
   */
  boolean[] itsReadonlyFlags;

  /**
   * A property editor for each element.
   * A value of null in an element of this array means that we must
   * find the property editor.
   * Declared transient so that property editors are not transported
   * across an RMI connection
   * (they will be re-constituted on the other side).
   * If the element of the named value list is an array,
   * then the corresponding element of this array is an array of
   * property editors.
   */
  transient
  Object[] itsEditors = null;

  /**
   * A map of names to the index for that name.
   * Keys of this map are the value names
   * (objects of class <code>String</code>).
   * Values of this map are the indexes into the
   * <code>itsNames</code>, etc arrays
   * (objects of class <code>Integer</code>).
   * This is more efficient than doing a linear search of the
   * <code>itsNames</code> array.
   */
  Hashtable itsNameToIndexMap;

  /**
   * The base URL for looking up documentation on the named values
   * stored herein.
   */
  private
  URL itsDocURL = null;

  /**
   * Make a new object given an array of names and an array of types.
   * The values are all set to <code>null</code>.
   * Every value is read/write.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @return
   *  The object created.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code> and <code>types</code> are
   *      not the same.</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    String[] names,
    Class[] types
  )
  throws
    IllegalArgumentException
  {
    return new NamedValueList(names,types);
  }

  /**
   * Make a new object given an array of names and an array of types.
   * The values are all set to <code>null</code>.
   * Every value is read/write.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code> and <code>types</code> are
   *      not the same.</li>
   *  </ul>
   */
  private
  NamedValueList(
    String[] names,
    Class[] types
  )
  throws
    IllegalArgumentException
  {
    this(names,types,null,null);
  }

  /**
   * Make a new object given an array of names, an array of types and
   * an array of values.
   * Every value is read/write.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @param values
   *  The array of values.
   *  If this is <code>null</code> then it is treated as an array
   *  full of <code>null</code> values.
   *
   * @return
   *  The objected created.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code>, <code>types</code> and
   *      <code>values</code> arrays are not the same.</li>
   *    <li>an element of <code>values</code> is not assignment
   *      compatible with the class indicated
   *      by the corresponding element of <code>types</code>.</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    String[] names,
    Class[] types,
    Object[] values
  )
  {
    return new NamedValueList(names,types,values);
  }

  /**
   * Make a new object given an array of names, an array of types and
   * an array of values.
   * Every value is read/write.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @param values
   *  The array of values.
   *  If this is <code>null</code> then it is treated as an array
   *  full of <code>null</code> values.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code>, <code>types</code> and
   *      <code>values</code> arrays are not the same.</li>
   *    <li>an element of <code>values</code> is not assignment
   *      compatible with the class indicated
   *      by the corresponding element of <code>types</code>.</li>
   *  </ul>
   */
  private
  NamedValueList(
    String[] names,
    Class[] types,
    Object[] values
  )
  {
    this(names,types,values,null);
  }

  /**
   * Make a new object given an array of names, an array of types and
   * an array of readonly flags.
   * All the values are set to <code>null</code>.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @param readonlyFlags
   *  An array containing a flag for each value.
   *  If the flag is true then setting that value is not allowed.
   *  If this is <code>null</code> then it is treated as an array
   *  of <code>false</code> values.
   *
   * @return
   *  The object created.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code>, <code>types</code> and
   *      <code>readonlyFlags</code> arrays are not the same.</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    String[] names,
    Class[] types,
    boolean[] readonlyFlags
  )
  throws
    IllegalArgumentException
  {
    return new NamedValueList(names,types,readonlyFlags);
  }

  /**
   * Make a new object given an array of names, an array of types and
   * an array of readonly flags.
   * All the values are set to <code>null</code>.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @param readonlyFlags
   *  An array containing a flag for each value.
   *  If the flag is true then setting that value is not allowed.
   *  If this is <code>null</code> then it is treated as an array
   *  of <code>false</code> values.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code>, <code>types</code> and
   *      <code>readonlyFlags</code> arrays are not the same.</li>
   *  </ul>
   */
  private
  NamedValueList(
    String[] names,
    Class[] types,
    boolean[] readonlyFlags
  )
  throws
    IllegalArgumentException
  {
    this(names,types,null,readonlyFlags);
  }

  /**
   * Make a new object given an array of names, an array of types and
   * an array of values.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @param values
   *  The array of values.
   *  If this is <code>null</code> then it is treated as an array
   *  full of <code>null</code> values.
   *
   * @param readonlyFlags
   *  An array containing a flag for each value.
   *  If the flag is true then setting that value is not allowed.
   *  If this is <code>null</code> then it is treated as an array
   *  of <code>false</code> values.
   *
   * @return
   *  The object created.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code>, <code>types</code>,
   *      <code>readonlyFlags</code> and <code>values</code> arrays
   *      are not the same.</li>
   *    <li>an element of <code>values</code> is not assignment
   *      compatible with the class indicated
   *      by the corresponding element of <code>types</code>.</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    String[] names,
    Class[] types,
    Object[] values,
    boolean[] readonlyFlags
  )
  throws
    IllegalArgumentException
  {
    return new NamedValueList(names,types,values,readonlyFlags);
  }

  /**
   * Make a new object given an array of names, an array of types and
   * an array of values.
   *
   * @param names
   *  The array of names.
   *
   * @param types
   *  The array of types.
   *  Each element describes the data type of the corresponding element
   *  of <code>names</code>.
   *
   * @param values
   *  The array of values.
   *  If this is <code>null</code> then it is treated as an array
   *  full of <code>null</code> values.
   *
   * @param readonlyFlags
   *  An array containing a flag for each value.
   *  If the flag is true then setting that value is not allowed.
   *  If this is <code>null</code> then it is treated as an array
   *  of <code>false</code> values.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>the <code>names</code> or <code>types</code> args were
   *      equal to <code>null</code>.</li>
   *    <li>the <code>names</code> or <code>types</code> arrays had
   *      one or more null elements.</li>
   *    <li>the length of <code>names</code>, <code>types</code>,
   *      <code>readonlyFlags</code> and <code>values</code> arrays
   *      are not the same.</li>
   *    <li>an element of <code>values</code> is not assignment
   *      compatible with the class indicated
   *      by the corresponding element of <code>types</code>.</li>
   *  </ul>
   */
  private
  NamedValueList(
    String[] names,
    Class[] types,
    Object[] values,
    boolean[] readonlyFlags
  )
  throws
    IllegalArgumentException
  {
    // Check for null args.
    if(names == null) {
      throw new IllegalArgumentException(
          "The \"names\" arg is null.");
    }
    if(types == null) {
      throw new IllegalArgumentException(
          "The \"types\" arg is null.");
    }

    // If the values array does not exist, make it now.
    if(values == null) {
      values = new Object[names.length];
      for(int i = 0; i < names.length; i++) {
        values[i] = null;
      }
    }

    // If the readonly flags array does not exist, make it now.
    if(readonlyFlags == null) {
      readonlyFlags = new boolean[names.length];
      for(int i = 0; i < names.length; i++) {
        readonlyFlags[i] = false;
      }
    }

    // Check that the array lengths are correct.
    if(names.length != types.length) {
      throw new IllegalArgumentException(
          "The length of the \"types\" arg is not the same as the " +
          "length of the \"names\" arg.");
    }
    if(names.length != values.length) {
      throw new IllegalArgumentException(
          "The length of the \"values\" arg is not the same as the " +
          "length of the \"names\" arg.");
    }
    if(names.length != readonlyFlags.length) {
      throw new IllegalArgumentException(
          "The length of the \"readonlyFlags\" arg is not the same as the " +
          "length of the \"names\" arg.");
    }

    // Check that the values are of the correct type or null elemnts.
    for(int i = 0; i < names.length; i++) {
      if(names[i] == null) {
        throw new IllegalArgumentException(
            "The value of \"names[" + i + "]\" is null.");
      }
      if(types[i] == null) {
        throw new IllegalArgumentException(
            "The type for key \"" + names[i] + "\" (index " + i +
            ") is null.");
      }
      if((values[i] != null) && !types[i].isInstance(values[i])) {
        throw new IllegalArgumentException(
            "The value for key \"" + names[i] + "\" (index " + i +
            ") is \"" + values[i].getClass().getName() +
            "\" which is not assignable to type \"" +
            types[i].getName() + "\".");
      }
    }

    // Remember the arrays.
    itsNames = new String[names.length];
    itsTypes = new Class[names.length];
    itsValues = new Object[names.length];
    itsOldValues = new Object[names.length];
    itsModified = new boolean[names.length];
    itsReadonlyFlags = new boolean[names.length];
    for(int i = 0; i < names.length; i++) {
      itsNames[i] = names[i];
      itsTypes[i] = types[i];
      if(itsTypes[i].isArray() && (values[i] == null)) {
        // The requested type is an array, but the value us null. Replace
        // the value with a zero-length array.
        itsValues[i] = Array.newInstance(itsTypes[i].getComponentType(),0);
      }
      else {
        itsValues[i] = values[i];
      }
      if(itsTypes[i].isArray()) {
        // The array length.
        int length = ((Object[])itsValues[i]).length;

        // Make the old values array.
        itsOldValues[i] = Array.newInstance(itsTypes[i].getComponentType(),
            length);

        // Fill the old values array.
        for(int j = 0; j < length; j++) {
          ((Object[])itsOldValues[i])[j] = ((Object[])itsValues[i])[j];
        }        
      }
      else {
        itsOldValues[i] = values[i];
      }
      itsModified[i] = false;
      itsReadonlyFlags[i] = readonlyFlags[i];
    }

    // Build the map of names to indexes.
    buildIndexMap();
  }

  public void addNewName(String name, Class type, Object value, boolean
  readonly)
  {
     int num = itsNames.length;
     
     String[] nameStorage = new String[num];
     Class[] classStorage = new Class[num];
     Object[] objectStorage = new Object[num];
     boolean[] readonlyStorage = new boolean[num];
     Object[] oldVals = new Object[num];
     boolean[] modified = new boolean[num];

     for (int i = 0; i < num; i++) {
        nameStorage[i] = itsNames[i];
	classStorage[i] = itsTypes[i];
	objectStorage[i] = itsValues[i];
	readonlyStorage[i] = itsReadonlyFlags[i];
	oldVals[i] = itsOldValues[i];
	modified[i] = itsModified[i];
     }

     itsNames = new String[num+1];
     itsTypes = new Class[num+1];
     itsValues = new Object[num+1];
     itsReadonlyFlags = new boolean[num+1];
     itsOldValues = new Object[num+1];
     itsModified = new boolean[num+1];
     
     itsNames[num] = name;
     itsTypes[num] = type;
     itsValues[num] = value;
     itsReadonlyFlags[num] = readonly;
     itsOldValues[num] = value;
     itsModified[num] = false;

     for (int i = 0; i < num; i++) {
        itsNames[i] = nameStorage[i];
	itsTypes[i] = classStorage[i];
	itsValues[i] = objectStorage[i];
	itsReadonlyFlags[i] = readonlyStorage[i];
	itsOldValues[i] = oldVals[i];
	itsModified[i] = modified[i];
     }
     buildIndexMap();
  }
	
  /**
   * Make a new object given a map of names to data types.
   * All the values will be set to <code>null</code>.
   *
   * @param types
   *  The map of names to data types.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the class of the values
   *  (objects of class <code>Class</code>).
   *  Every key in the <code>types</code> map must also be a key
   *  in the <code>values</code> map.
   *  After this constructor is called,
   *  this map and its contents should not be changed.
   *
   * @return
   *  The object created.
   *
   * @exception IllegalArgumentException
   *  Thrown when:
   *  <ul>
   *    <li>one of the keys in <code>types</code> is not a string</li>
   *    <li>one of the values in <code>types</code> is not a class</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    Map types
  )
  throws
    IllegalArgumentException
  {
    return new NamedValueList(types);
  }

  /**
   * Make a new object given a map of names to data types.
   * All the values will be set to <code>null</code>.
   *
   * @param types
   *  The map of names to data types.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the class of the values
   *  (objects of class <code>Class</code>).
   *  Every key in the <code>types</code> map must also be a key
   *  in the <code>values</code> map.
   *  After this constructor is called,
   *  this map and its contents should not be changed.
   *
   * @exception IllegalArgumentException
   *  Thrown when:
   *  <ul>
   *    <li>one of the keys in <code>types</code> is not a string</li>
   *    <li>one of the values in <code>types</code> is not a class</li>
   *  </ul>
   */
  private
  NamedValueList(
    Map types
  )
  throws
    IllegalArgumentException
  {
    this(types,null,null);
  }

  /**
   * Make a new object given a map of names to data types and
   * a map of names to values.
   *
   * @param types
   *  The map of names to data types.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the class of the values
   *  (objects of class <code>Class</code>).
   *  Every key in the <code>types</code> map must also be a key
   *  in the <code>values</code> map.
   *
   * @param values
   *  The map of names to values.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the values and must be of the class
   *  indicated in <code>types</code>.
   *  Every key in the <code>values</code> map must also be a key
   *  in the <code>types</code> map.
   *  A value of <code>null</code> is treated as a map with every
   *  element equal to <code>null</code>.
   *
   * @return
   *  The value created.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>a key in either <code>values</code> or <code>types</code> is
   *      not an object of class <code>String</code>.</li>
   *    <li>a value in <code>types</code> is not of class
   *      <code>Class</code>.</li>
   *    <li>a key exists in <code>values</code> that is not
   *      in <code>types</code>, or vice versa.</li>
   *    <li>a value in <code>values</code> is not assignment compatible
   *      with the class given in the corresponding element of
   *      <code>types</code>.</li>
   *    <li>the data type for one of the values does not conform to
   *      the requirements for constructors.</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    Map types,
    Map values
  )
  throws
    IllegalArgumentException
  {
    return new NamedValueList(types,values);
  }

  /**
   * Make a new object given a map of names to data types and
   * a map of names to values.
   *
   * @param types
   *  The map of names to data types.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the class of the values
   *  (objects of class <code>Class</code>).
   *  Every key in the <code>types</code> map must also be a key
   *  in the <code>values</code> map.
   *
   * @param values
   *  The map of names to values.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the values and must be of the class
   *  indicated in <code>types</code>.
   *  Every key in the <code>values</code> map must also be a key
   *  in the <code>types</code> map.
   *  A value of <code>null</code> is treated as a map with every
   *  element equal to <code>null</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>a key in either <code>values</code> or <code>types</code> is
   *      not an object of class <code>String</code>.</li>
   *    <li>a value in <code>types</code> is not of class
   *      <code>Class</code>.</li>
   *    <li>a key exists in <code>values</code> that is not
   *      in <code>types</code>, or vice versa.</li>
   *    <li>a value in <code>values</code> is not assignment compatible
   *      with the class given in the corresponding element of
   *      <code>types</code>.</li>
   *    <li>the data type for one of the values does not conform to
   *      the requirements for constructors.</li>
   *  </ul>
   */
  private
  NamedValueList(
    Map types,
    Map values
  )
  throws
    IllegalArgumentException
  {
    this(types,values,null);
  }

  /**
   * Make a new object given a map of names to data types,
   * a map of names to values and a map of names to readonly flags.
   *
   * @param types
   *  The map of names to data types.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the class of the values
   *  (objects of class <code>Class</code>).
   *  Every key in the <code>types</code> map must also be a key
   *  in the <code>values</code> map.
   *
   * @param values
   *  The map of names to values.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the values and must be of the class
   *  indicated in <code>types</code>.
   *  Every key in the <code>values</code> map must also be a key
   *  in the <code>types</code> map.
   *  A value of <code>null</code> is treated as a map with every
   *  element equal to <code>null</code>.
   *
   * @param readonlyFlags
   *  The map of readonly flags.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the readonly flags
   *  (objects of class <code>Boolean</code>).
   *  Every key in the <code>values</code> map must also be a key
   *  in the <code>types</code> map.
   *  A value of <code>null</code> is treated as a map with every
   *  element equal to <code>false</code>.
   *
   * @return
   *  The object created.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>a key in either <code>values</code> or <code>types</code> is
   *      not an object of class <code>String</code>.</li>
   *    <li>a value in <code>types</code> is not of class
   *      <code>Class</code>.</li>
   *    <li>a key exists in <code>values</code> that is not
   *      in <code>types</code>, or vice versa.</li>
   *    <li>a value in <code>values</code> is not assignment compatible
   *      with the class given in the corresponding element of
   *      <code>types</code>.</li>
   *    <li>the data type for one of the values does not conform to
   *      the requirements for constructors.</li>
   *  </ul>
   */
  public static
  NamedValueList
  factory(
    Map types,
    Map values,
    Map readonlyFlags
  )
  throws
    IllegalArgumentException
  {
    return new NamedValueList(types,values,readonlyFlags);
  }

  /**
   * Make a new object given a map of names to data types,
   * a map of names to values and a map of names to readonly flags.
   *
   * @param types
   *  The map of names to data types.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the class of the values
   *  (objects of class <code>Class</code>).
   *  Every key in the <code>types</code> map must also be a key
   *  in the <code>values</code> map.
   *
   * @param values
   *  The map of names to values.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the values and must be of the class
   *  indicated in <code>types</code>.
   *  Every key in the <code>values</code> map must also be a key
   *  in the <code>types</code> map.
   *  A value of <code>null</code> is treated as a map with every
   *  element equal to <code>null</code>.
   *
   * @param readonlyFlags
   *  The map of readonly flags.
   *  Keys of this map are the names of the entries
   *  (objects of class <code>String</code>).
   *  Values of this map are the readonly flags
   *  (objects of class <code>Boolean</code>).
   *  Every key in the <code>values</code> map must also be a key
   *  in the <code>types</code> map.
   *  A value of <code>null</code> is treated as a map with every
   *  element equal to <code>false</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li>a key in either <code>values</code> or <code>types</code> is
   *      not an object of class <code>String</code>.</li>
   *    <li>a value in <code>types</code> is not of class
   *      <code>Class</code>.</li>
   *    <li>a key exists in <code>values</code> that is not
   *      in <code>types</code>, or vice versa.</li>
   *    <li>a value in <code>values</code> is not assignment compatible
   *      with the class given in the corresponding element of
   *      <code>types</code>.</li>
   *    <li>the data type for one of the values does not conform to
   *      the requirements for constructors.</li>
   *  </ul>
   */
  private
  NamedValueList(
    Map types,
    Map values,
    Map readonlyFlags
  )
  throws
    IllegalArgumentException
  {
    // An name objects.
    Object[] nameObjs;

    // Check that all keys in types are strings and values in types
    // are classes. Also check that each key has a corresponding key in
    // values, and that the value is the correct type.
    for(Iterator iKey = types.keySet().iterator(); iKey.hasNext(); ) {
      // The key.
      Object key = iKey.next();

      if(!(key instanceof String)) {
        throw new IllegalArgumentException(
            "The key \"" + key + "\" must be of type \"" +
            String.class.getName() + "\" but is of type \"" +
            key.getClass().getName() + "\".");
      }
      if(!(types.get(key) instanceof Class)) {
        throw new IllegalArgumentException(
            "The value must be of type \"" + Class.class.getName() +
            "\" but is of class \"" + types.get(key).getClass().getName() +
            "\".");
      }

      // Only check values if it exists.
      if(values != null) {
        // Check that the key also exists in values.
        if(!values.containsKey(key)) {
          throw new IllegalArgumentException(
              "The \"values\" arg does not contain a key for the \"" +
              key + "\" key.");
        }

        // Check that the value in values is the correct type.
        if((values.get(key) != null) &&
            !((Class)types.get(key)).isInstance(values.get(key))) {
          throw new IllegalArgumentException(
              "The value for key \"" + key + "\" is of type \"" +
              values.get(key).getClass().getName() +
              "\" which is not assignable to \"" +
              ((Class)types.get(key)).getName() + "\".");
        }
      }
    }

    // Make the arrays.
    nameObjs = types.keySet().toArray();
    itsNames = new String[nameObjs.length];
    itsTypes = new Class[nameObjs.length];
    itsValues = new Object[nameObjs.length];
    itsOldValues = new Object[nameObjs.length];
    itsModified = new boolean[nameObjs.length];
    itsReadonlyFlags = new boolean[nameObjs.length];
    for(int i = 0; i < nameObjs.length; i++) {
      // Set the name element.
      itsNames[i] = (String)nameObjs[i];
      // Set the type element.
      itsTypes[i] = (Class)types.get(itsNames[i]);
      // Set the value element.
      if(values == null) {
        itsValues[i] = null;
      }
      else {
        if(itsTypes[i].isArray() && (values.get(itsNames[i]) == null)) {
          // Requested type is an array, but the given value is null. Replace
          // the value with a zero length array.
          itsValues[i] = Array.newInstance(itsTypes[i].getComponentType(),0);
        }
        else {
          itsValues[i] = values.get(itsNames[i]);
        }
      }
      // Set the old values element.
      if(itsTypes[i].isArray()) {
        // The array length.
        int length = ((Object[])itsValues[i]).length;

        // Make the old values array.
        itsOldValues[i] = Array.newInstance(itsTypes[i].getComponentType(),
            length);

        // Fill the old values array.
        for(int j = 0; j < length; j++) {
          ((Object[])itsOldValues[i])[j] = ((Object[])itsValues[i])[j];
        }        
      }
      else {
        itsOldValues[i] = itsValues[i];
      }
      // Value has not been modified yet.
      itsModified[i] = false;
      // Set the readonly flags.
      if(readonlyFlags == null) {
        itsReadonlyFlags[i] = false;
      }
      else {
        itsReadonlyFlags[i] =
            ((Boolean)readonlyFlags.get(itsNames[i])).booleanValue();
      }
    }

    // Build the map of names to indexes.
    buildIndexMap();
  }

  /**
   * Make a new object from a properties object.
   * All values in the named value list will be of type
   * <code>String</code>.
   *
   * @param props
   *  The properties object to load.
   *
   * @return
   *  The object created.
   */
  public static
  NamedValueList
  factory(
    Properties props
  )
  {
    return new NamedValueList(props);
  }

  /**
   * Make a new object from a properties object.
   * All values in the named value list will be of type
   * <code>String</code>.
   *
   * @param props
   *  The properties object to load.
   */
  private
  NamedValueList(
    Properties props
  )
  {
    this(props,false);
  }

  /**
   * Make a new object from a properties object.
   * All values in the named value list will be of type
   * <code>String</code>.
   *
   * @param props
   *  The properties object to load.
   *
   * @param readonly
   *  Determines whether all the values are readonly.
   *
   * @return
   *  The object crated.
   */
  public static
  NamedValueList
  factory(
    Properties props,
    boolean readonly
  )
  {
    return new NamedValueList(props,readonly);
  }

  /**
   * Make a new object from a properties object.
   * All values in the named value list will be of type
   * <code>String</code>.
   *
   * @param props
   *  The properties object to load.
   *
   * @param readonly
   *  Determines whether all the values are readonly.
   */
  private
  NamedValueList(
    Properties props,
    boolean readonly
  )
  {
    // The property keys.
    Object[] keys = props.keySet().toArray();
    // The string constructor.
    Constructor constructor;

    // Find the string constructor.
    try {
      constructor = String.class.getConstructor(new Class[] {String.class});
    }
    catch(Exception e) {
      constructor = null;
    }

    // Sort the keys.
    Arrays.sort(keys);

    // Make the arrays.
    itsNames = new String[keys.length];
    itsTypes = new Class[keys.length];
    itsValues = new Object[keys.length];
    itsOldValues = new Object[keys.length];
    itsModified = new boolean[keys.length];
    itsReadonlyFlags = new boolean[keys.length];
    for(int i = 0; i < keys.length; i++) {
      // Set the name element.
      itsNames[i] = (String)keys[i];
      // Set the type element.
      itsTypes[i] = String.class;
      // Set the value element.
      itsValues[i] = props.getProperty((String)keys[i]);
      // Set the old values element.
      itsOldValues[i] = itsValues[i];
      // Set the modified flag.
      itsModified[i] = false;
      // Set the readonly flag.
      itsReadonlyFlags[i] = readonly;
    }

    // Build the map of names to indexes.
    buildIndexMap();
  }

  /**
   * Build the map of names to indexes into <code>itsNameToIndexMap</code>.
   */
  private
  void
  buildIndexMap()
  {
    itsNameToIndexMap = new Hashtable(itsNames.length);
    for(int i = 0; i < itsNames.length; i++) {
      itsNameToIndexMap.put(itsNames[i],new Integer(i));
    }
  }

  /**
   * Clone the object.
   *
   * @return
   *  A new object identical to this one.
   *  A shallow copy is created.
   */
  public synchronized
  Object
  clone()
  {
    // The clone to return.
    NamedValueList clone = null;

    // Make the clone.
    try {
      clone = (NamedValueList)super.clone();
    }
    catch(CloneNotSupportedException e) {
      // Should never happen.
      assert false;
    }

    // Don't copy the names. They never change.
    clone.itsNames = itsNames;

    // Don't copy the types. They never change.
    clone.itsTypes = itsTypes;

    // Don't copy the names to indexes map. They never change.
    clone.itsNameToIndexMap = itsNameToIndexMap;

    // Don't copy the readonly flags. They never change.
    clone.itsReadonlyFlags = itsReadonlyFlags;

    // Copy the values and modified flags.
    clone.itsThis = clone;
    clone.itsValues = new Object[itsNames.length];
    clone.itsOldValues = new Object[itsNames.length];
    clone.itsModified = new boolean[itsNames.length];
    for(int i = 0; i < itsValues.length; i++) {
      clone.itsValues[i] = itsValues[i];
      if(itsTypes[i].isArray()) {
        // The array length.
        int length = ((Object[])itsValues[i]).length;

        // Make the old values array.
        clone.itsOldValues[i] =
            Array.newInstance(itsTypes[i].getComponentType(),length);

        // Fill the old values array.
        for(int j = 0; j < length; j++) {
          ((Object[])clone.itsOldValues[i])[j] =
              ((Object[])itsOldValues[i])[j];
        }        
      }
      else {
        clone.itsOldValues[i] = itsOldValues[i];
      }
      clone.itsModified[i] = itsModified[i];
    }

    // Make a null property editors array in the clone. The clone will
    // fill this in when required.
    clone.itsEditors = null;
    
    // Return the clone.
    return clone;
  }

  /**
   * Test for equivalence.
   * This is currently functionally the same as the <code>equivTypes</code>
   * method
   * (<i>ie</i>, it does not do a proper comparison).
   *
   * @param o
   *  The object to compare with.
   *
   * @return
   *  Returns <code>true</code> if the two objects are semantically
   *  equal, <code>false</code> otherwise.
   */
  public synchronized
  boolean
  equiv(
    Object o
  )
  {
    // The value to return.
    boolean isEquiv = false;
    // Local version of o.
    NamedValueList o1;

    // Prerequisits.
    if((o != null) && (o.getClass() == getClass())) {
      o1 = (NamedValueList)o;
      // Is equiv if same object.
      if(o1 == this) {
        isEquiv = true;
      }
      else {
        // Test the attributes.
        //* Place test of attributes of the objects here.
        isEquiv = equivTypes((NamedValueList)o);
      }
    }

    return isEquiv;
  }

  /**
   * See if two named value lists are of the same type.
   * That is,
   * that they have exactly the same names with the same data type
   * for each name.
   *
   * @param o
   *  The object to compare with.
   *
   * @return
   *  The value <code>true</code> if they are the same type,
   *  <code>false</code> otherwise.
   */
  public
  boolean
  equivTypes(
    NamedValueList o
  )
  {
    // The value to return.
    boolean result = true;

    if(o == null) {
      result = false;
    }
    else {
      // See that the values are the same length.
      if(itsNames.length != o.itsNames.length) {
        result = false;
      }
      else {
        // Iterate over the names array.
        for(int i = 0; i < itsNames.length; i++) {
          // The index of this name in o.
          int oIndex = o.getIndexForName(itsNames[i]);
          // See if the name exists in o.
          if(oIndex == -1) {
            result = false;
            break;
          }
          // See if its type in o is the same.
          if(o.itsTypes[oIndex] != itsTypes[i]) {
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * See if two named value lists are of the same type.
   * That is,
   * that they have exactly the same names with the same data type
   * for each name,
   * and with the same order.
   *
   * @param o
   *  The object to compare with.
   *
   * @return
   *  The value <code>true</code> if they are the same type,
   *  <code>false</code> otherwise.
   */
  boolean
  equivTypesWithOrder(
    NamedValueList o
  )
  {
    // The value to return.
    boolean result = true;

    if(o == null) {
      result = false;
    }
    else {
      // See that the values are the same length.
      if(itsNames.length != o.itsNames.length) {
        result = false;
      }
      else {
        // Iterate over the names array.
        for(int i = 0; i < itsNames.length; i++) {
          if(!o.itsNames[i].equals(itsNames[i])) {
            result = false;
            break;
          }
          if(o.itsTypes[i] != itsTypes[i]) {
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Return the number of elements.
   *
   * @return
   *  The number of elements in the list.
   */
  public synchronized
  int
  size()
  {
    return itsNames.length;
  }

  /**
   * See if the list is empty.
   *
   * @return
   *  The value <code>true</code> if the list is empty,
   *  <code>false</code> otherwise.
   */
  public synchronized
  boolean
  isEmpty()
  {
    return (size() == 0);
  }

  /**
   * See if the list contains an element with a given name.
   *
   * @param key
   *  The name to check for.
   *  Must be an object of class <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if the list contains an element
   *  called <code>key</code>,
   *  <code>false</code> otherwise.
   *
   * @exception ClassCastException
   *  Thrown if the <code>key</code> is not of class <code>String</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #containsKey(String)
   */
  public synchronized
  boolean
  containsKey(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    return containsKey((String)key);
  }

  /**
   * See if the list contains an element with a given name.
   * This is equivalent to the
   * <code>get(Object)</code> method,
   * except that it enforces type safety on the key.
   *
   * @param key
   *  The name to check for.
   *  Must be an object of class <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The value <code>true</code> if the list contains an element
   *  called <code>key</code>,
   *  <code>false</code> otherwise.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #containsKey(Object)
   */
  public synchronized
  boolean
  containsKey(
    String key
  )
  throws
    NullPointerException
  {
    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    return (getIndexForName((String)key) != -1);
  }

  /**
   * See if the list contains a given value.
   *
   * @param value
   *  The value to check for.
   *
   * @return
   *  The value <code>true</code> if the list contains an element
   *  with value <code>vlaue</code>,
   *  <code>false</code> otherwise.
   */
  public synchronized
  boolean
  containsValue(
    Object value
  )
  {
    // The value to return.
    boolean result;

    // Iterate over the values array.
    result = false;
    for(int i = 0; i < itsValues.length; i++) {
      if(itsValues[i] == null) {
        if(value == null) {
          result = true;
          break;
        }
      }
      else {
        if(itsValues[i].equals(value)) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get the type for a given key.
   *
   * @param key
   *  The key to get the type for.
   *
   * @return
   *  The class representing the data type of the entry,
   *  or <code>null</code> if there is no entry for <code>key</code>.
   *
   * @exception ClassCastException
   *  Thrown if the <code>key</code> is not of class <code>String</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #getType(String)
   */
  public synchronized
  Class
  getType(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    return(getType((String)key));
  }

  /**
   * Get the type for a given key.
   * This is functionally equivalent to the
   * <code>getType(Object)</code> method,
   * except that it enforces type safety on the key.
   *
   * @param key
   *  The key to get the type for.
   *
   * @return
   *  The class representing the data type of the entry
   *  of <code>null</code> if there is no entry for <code>key</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #getType(Object)
   */
  public synchronized
  Class
  getType(
    String key
  )
  throws
    NullPointerException
  {
    // The result to return.
    Class result;
    // The index into the content arrays.
    int index;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Get the index.
    index = getIndexForName(key);
    if(index == -1) {
      result = null;
    }
    else {
      result = itsTypes[index];
    }

    return result;
  }

  /**
   * Get the readonly flag for a given key.
   *
   * @param key
   *  The key to get the readonly flag for.
   *
   * @return
   *  The value <code>true</code> if the entry for <code>key</code> is
   *  readonly, <code>false</code> otherwise.
   *  If there is no entry for <code>key</code> then <code>false</code>
   *  is returned.
   *
   * @exception ClassCastException
   *  Thrown if the <code>key</code> is not of class <code>String</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #getReadonly(String)
   */
  public synchronized
  boolean
  getReadonly(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    return(getReadonly((String)key));
  }

  /**
   * Get the readonly flag for a given key.
   * This is functionally equivalent to the
   * <code>getReadonly(Object)</code> method,
   * except that it enforces type safety on the key.
   *
   * @param key
   *  The key to get the readonly flag for.
   *
   * @return
   *  The value <code>true</code> if the entry for <code>key</code> is
   *  readonly, <code>false</code> otherwise.
   *  If there is no entry for <code>key</code> then <code>false</code>
   *  is returned.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #getType(Object)
   */
  public synchronized
  boolean
  getReadonly(
    String key
  )
  throws
    NullPointerException
  {
    // The result to return.
    boolean result;
    // The index into the content arrays.
    int index;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Get the index.
    index = getIndexForName(key);
    if(index == -1) {
      result = true;
    }
    else {
      result = itsReadonlyFlags[index];
    }

    return result;
  }

  /**
   * Get the value for a given key.
   *
   * @param key
   *  The key to get the value for.
   *  Must be an object of class <code>String</code>.
   *
   * @return
   *  The value corresponding to <code>key</code>,
   *  or <code>null</code> if there is no entry for <code>key</code>.
   *
   * @exception ClassCastException
   *  Thrown if the <code>key</code> is not of class
   *  <code>String</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #get(String)
   */
  public synchronized
  Object
  get(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    return get((String)key);
  }

  /**
   * Get the value for a given key.
   * This is functionally equivalent to the
   * <code>get(Object)</code> method except that it enforces
   * type safety on the <code>key</code> argument.
   *
   * @param key
   *  The key to get the value for.
   *
   * @return
   *  The value corresponding to <code>key</code>,
   *  or <code>null</code> if there is no entry for <code>key</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #get(Object)
   */
  public synchronized
  Object
  get(
    String key
  )
  throws
    NullPointerException
  {
    // The result to return.
    Object result;
    // The index into the content arrays.
    int index;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Get the index.
    index = getIndexForName(key);
    if(index == -1) {
      result = null;
    }
    else {
      result = itsValues[index];
    }

    return result;
  }

  /**
   * Get the "old" value for a given key.
   *
   * @param key
   *  The key to get the value for.
   *
   * @return
   *  The old value of the entry.
   *  That is,
   *  the value at construction time,
   *  or just before the modified flags were last reset.
   *
   * @exception ClassCastException
   *  Thrown if the <code>key</code> is not of class
   *  <code>String</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #getOldValue(String)
   */
  public synchronized
  Object
  getOldValue(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    return getOldValue((String)key);
  }

  /**
   * Get the "old" value for a given key.
   * This is functionally equivalent to the
   * <code>getOldValue(Object)</code> method,
   * except that it enforces type safety on the key.
   *
   * @param key
   *  The key to get the value for.
   *
   * @return
   *  The old value of the entry.
   *  That is,
   *  the value at construction time,
   *  or just before the modified flags were last reset.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   *
   * @see #getOldValue(Object)
   */
  public synchronized
  Object
  getOldValue(
    String key
  )
  throws
    NullPointerException
  {
    // The result to return.
    Object result;
    // The index into the content arrays.
    int index;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Get the index.
    index = getIndexForName(key);
    if(index == -1) {
      result = null;
    }
    else {
      result = itsOldValues[index];
    }

    return result;
  }

  /**
   * Get the value for a given key as a string.
   *
   * @param key
   *  The key to get the value for.
   *  Must be an object of class <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The string representation of the value.
   *  The date type of the value must not be an array.
   *
   * @exception ClassCastException
   *  Thrown if the key is not of class <code>String</code> or the
   *  value corresponding to <code>key</code> is an array type.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   */
  public synchronized
  String
  getAsString(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // The result to return.
    String result;
    // The index into the content arrays.
    int index;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    // Get the index.
    index = getIndexForName((String)key);
    if(index == -1) {
      result = null;
    }
    else {
      // Make the result.
      if(itsTypes[index].isArray()) {
        throw new ClassCastException(
            "The data type for \"" + key + "\" is an array.\".");
      }
      // Then extract the value from the property editor.
      result = ((PropertyEditor)itsEditors[index]).getAsText();
    }

    return result;
  }

  /**
   * Get the value for a given key as an array of strings.
   *
   * @param key
   *  The key to get the value for.
   *  Must be an object of class <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The string representation of the value.
   *  This is always an array.
   *  If the data type of the value is not an array then the returned
   *  value will be an array of length 1.
   *
   * @exception ClassCastException
   *  Thrown if the key is not of class <code>String</code>.
   *
   * @exception NullPointerException
   *  Thrown if <code>key</code> is equal to <code>null</code>.
   */
  public synchronized
  String[]
  getAsStrings(
    Object key
  )
  throws
    ClassCastException,
    NullPointerException
  {
    // The result to return.
    String[] result;
    // The index into the content arrays.
    int index;
    if (itsEditors == null) { 
      throw new NullPointerException(
          "You need to call makeEditors() first");     
    }

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    // Get the index.
    index = getIndexForName((String)key);
    if(index == -1) {
      result = null;
    }
    else {
      // Make sure the property editor(s) exist.
      // makeEditors();

      if(itsTypes[index].isArray()) {
        // Make sure one of the values of this array is not updated
        // while we are reading out.
        synchronized(itsEditors[index]) {
          // The value.
          Object[] value = (Object[])itsValues[index];
          // The property editors.
          PropertyEditor[] editors = (PropertyEditor[])itsEditors[index];

          // Make the result and fill in each value.
          result = new String[value.length];
          for(int i = 0; i < result.length; i++) {
            result[i] = editors[i].getAsText();
          }
        }
      }
      else {
        result = new String[1];
        result[0] = ((PropertyEditor)itsEditors[index]).getAsText();
      }

    }

    return result;
  }

  /**
   * Replace the value for a given key with a new value.
   * If successful, the entry is marked as modified.
   *
   * @param key
   *  The name of the value to replace.
   *  Must be an object of type <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @param value
   *  The new value for <code>key</code>.
   *
   * @return
   *  The previous value of <code>key</code>.
   *
   * @exception ClassCastException
   *  Thrown when <code>key</code> is not of type <code>String</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li><code>key</code> is not in the list</li>
   *    <li>the data type of <code>value</code> is the wrong type
   *      for <code>key</code></li>
   *    <li>the value for <code>key</code> is readonly</li>
   *  </ul>
   *
   * @exception NullPointerException
   *  Thrown when <code>key</code> is equal to <code>null</code>.
   */
  public synchronized
  Object
  put(
    Object key,
    Object value
  )
  throws
    ClassCastException,
    IllegalArgumentException,
    NullPointerException
  {
    // The index into the content arrays.
    int index;
    // The require data type for the value.
    Class type;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    // Check that key is in the list.
    index = getIndexForName((String)key);
    if(index == -1) {
      throw new IllegalArgumentException(
          "The key \"" + key + "\" is not one of the names in the list.");
    }

    // Check that the value is not readonly.
    if(itsReadonlyFlags[index]) {
      throw new IllegalArgumentException(
          "The key \"" + key + "\" is readonly.");
    }

    // Put the new value. First check that the new value is the proper
    // type.
    type = itsTypes[index];
    if(type.isArray()) {
      // Array. Check that each element of the array is the correct
      // type.

      // The component type.
      Class cType = type.getComponentType();

      // Check that value really is an array.
      if(!value.getClass().isArray()) {
        throw new IllegalArgumentException(
            "The type for key \"" + key +
            "\" is an array, but a scalar was supplied.");
      }
      // Check the size of the array.
      if(((Object[])value).length != ((Object[])itsValues[index]).length) {
        throw new IllegalArgumentException(
            "The type for key \"" + key + "\" is an array of length " +
            ((Object[])itsValues[index]).length +
            " but an array of length " + ((Object[])value).length +
            " was supplied.");
      }
      // Check each element of the array for the correct type.
      for(int i = 0; i < ((Object[])value).length; i++) {
        if(!cType.isInstance(((Object[])value)[i])) {
          throw new IllegalArgumentException("The type for key \"" +
              key + "\" must be \"" + type.getName() +
              "\" but element number " + i +
              " of the new value is of type \"" +
              ((Object[])value)[i].getClass() + "\".");
        }
      }
    }
    else {
      // Check for the correct type.
      if(!type.isInstance(value)) {
        throw new IllegalArgumentException("The value for \"" +
            key + "\" must be of type \"" + type.getName() +
            "\" but is of type \"" +
            value.getClass().getName() + "\".");
      }
    }

    // Update the value.
    updateValue(index,value);

    return itsOldValues[index];
  }

  /**
   * Replace the value for a given key with a new value,
   * represented as a string.
   * The data type for the key must not be an array.
   * If successful, the entry is marked as modified.
   *
   * <p>This differs from the <code>put(Object,Object)</code>
   * method in that the value is represented as a string.
   * The method uses the property editor for the data type
   * indicated for the element to create a new object.</p>
   *
   * @param key
   *  The name of the value to replace.
   *  Must be an object of type <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @param value
   *  The new value for <code>key</code>.
   *
   * @return
   *  The previous value of <code>key</code>.
   *
   * @exception ClassCastException
   *  Thrown when <code>key</code> is not of type <code>String</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li><code>key</code> is not in the list</li>
   *    <li>the class that we have to create is not suitable for use</li>
   *    <li>the value for <code>key</code> is readonly</li>
   *  </ul>
   *
   * @exception NumberFormatException
   *  The value could not be parsed into the required class.
   *
   * @exception NullPointerException
   *  Thrown when <code>key</code> is equal to <code>null</code>.
   */
  public synchronized
  Object
  put(
    Object key,
    String value
  )
  throws
    ClassCastException,
    IllegalArgumentException,
    NumberFormatException,
    NullPointerException
  {
    // The index into the content arrays.
    int index;
    // The require data type.
    Class type;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    // Check that key is in the list.
    index = getIndexForName((String)key);
    if(index == -1) {
      throw new IllegalArgumentException(
          "The key \"" + key + "\" is not one of the names in the list.");
    }

    // Check that the value is not readonly.
    if(itsReadonlyFlags[index]) {
      throw new IllegalArgumentException(
          "The key \"" + key + "\" is readonly.");
    }

    // Check that the type is not an array.
    if(itsTypes[index].isArray()) {
      throw new IllegalArgumentException(
          "The type for key \"" + key +
          " is array, which cannot be set from a string.");
    }

    // Update the value.
    updateValueAsString(index,value);

    return itsOldValues[index];
  }

  /**
   * Replace the value for a given key with a new value,
   * represented as an array of strings.
   * The data type for the key must be an array.
   * If successful, the entry is marked as modified.
   *
   * <p>This differs from the <code>put(Object,Object)</code>
   * method in that the value is represented as a string.
   * The method uses the property editor for the data type
   * indicated for the element to create a new object.</p>
   *
   * @param key
   *  The name of the value to replace.
   *  Must be an object of type <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @param value
   *  The new value for <code>key</code>.
   *
   * @return
   *  The previous value of <code>key</code>.
   *
   * @exception ClassCastException
   *  Thrown when <code>key</code> is not of type <code>String</code>.
   *
   * @exception IllegalArgumentException
   *  Thrown when either:
   *  <ul>
   *    <li><code>key</code> is not in the list</li>
   *    <li>the class that we have to create is not suitable for use</li>
   *    <li>the value for <code>key</code> is readonly</li>
   *  </ul>
   *
   * @exception NumberFormatException
   *  The value could not be parsed into the required class.
   *
   * @exception NullPointerException
   *  Thrown when <code>key</code> is equal to <code>null</code>.
   */
  public synchronized
  Object
  put(
    Object key,
    String[] values
  )
  throws
    ClassCastException,
    IllegalArgumentException,
    NumberFormatException,
    NullPointerException
  {
    // The index into the content arrays.
    int index;
    // The required data type.
    Class type;
    // The component type of the array.
    Class cType;

    // Check that <code>key</code> is not null.
    if(key == null) {
      throw new NullPointerException(
          "The value of the \"key\" arg must be non-null.");
    }

    // Check that the key is a string.
    if(!(key instanceof String)) {
      throw new ClassCastException(
          "The key must be of type \"" + String.class.getName() +
          "\" but is of type \"" + key.getClass().getName() + "\".");
    }

    // Check that key is in the list.
    index = getIndexForName((String)key);
    if(index == -1) {
      throw new IllegalArgumentException(
          "The key \"" + key + "\" is not one of the names in the list.");
    }

    // Check that the value is not readonly.
    if(itsReadonlyFlags[index]) {
      throw new IllegalArgumentException(
          "The key \"" + key + "\" is readonly.");
    }

    // Check that the required data type for the key is an array.
    type = itsTypes[index];
    if(!type.isArray()) {
      throw new IllegalArgumentException(
        "The required data type is \"" + type.getName() +
        "\" which is not an array, but the array method has been called.");
    }

    // Check that the new value has just enough elements.
    if(((Object[])itsValues[index]).length != values.length) {
      throw new IllegalArgumentException(
          "The type for key \"" + key + "\" is an array of length " +
          ((Object[])itsValues[index]).length +
          " but an array of length " + values.length +
          " was supplied.");
    }

    // Set the values.
    updateValueAsString(index,values);

    return itsOldValues[index];
  }

  /**
   * Remove an element from the list.
   * This operation is not supported.
   *
   * @param key
   *  The key of the element to remove.
   *  Must be an object of type <code>String</code>.
   *  Must not be equal to <code>null</code>.
   *
   * @return
   *  The old value.
   *
   * @exception UnsupportedOperationException
   *  Always thrown.
   */
  public
  Object
  remove(
    Object key
  )
  throws
    UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Copies all the mappings from the specified map to this map.
   * This operation is not supported.
   *
   * @param t
   *  The map to copy from.
   *  The map <code>t</code> must not contains any keys that aren't
   *  in this map.
   *  Values in <code>t</code> must be of compatible types to the types
   *  mandated for values in this map at construction time.
   *
   * @exception IllegalArgumentException
   *  Thrown when:
   *    <ul>
   *      <li><code>map</code> contains a key that is not in this map.</li>
   *      <li>there was a problem parsing the string from <code>map</code>
   *        into the data type mandated by the this map.</li>
   *    </ul>
   *
   * @exception ClassCastException
   *  Thrown when one of the keys of <code>map</code> is not a string.
   */
  public
  void
  putAll(
    Map map
  )
  throws
    IllegalArgumentException
  {
    // Iterate over the map.
    for(Iterator iKey = map.keySet().iterator(); iKey.hasNext(); ) {
      // The name.
      Object next = (String)iKey.next();
      // The name.
      String name;
      // The value.
      Object value;

      // Make sure the key is a string.
      if(!(next instanceof String)) {
        throw new ClassCastException("One of the keys is not a string.");
      }
      name = (String)next;

      // Make sure the list has a key of that name.
      if(!containsKey(name)) {
        throw new IllegalArgumentException(
            "There is no key \"" + name + "\" in the map.");
      }

      // Get and check the value.
      value = map.get(name);

      // Set it.
      try {
        if(value instanceof String) {
          put(name,(String)value);
        }
        else {
          put(name,value);
        }
      }
      catch(NumberFormatException e) {
        throw new IllegalArgumentException(
            "Exception while setting key \"" + name +
            "\" to \"" + value + "\". The data type of the value is \"" +
            getType(name).getName() + "\".");
      }
      catch(IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Exception while setting key \"" + name +
            "\" to \"" + value + "\". The data type of the value is \"" +
            getType(name).getName() + "\".");
      }
      catch(ClassCastException e) {
        // Should never happen.
        assert false;
      }
      catch(NullPointerException e) {
        // Should never happen.
        assert false;
      }
    }
  }

  /**
   * Removes all elements from the list.
   * This operation is not supported.
   *
   * @exception UnsupportedOperationException
   *  Always thrown.
   */
  public
  void
  clear()
  throws
    UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a set view of all the names in the list.
   * The resulting set is backed by the list.
   * Changes to the list will be reflected in changes to the set.
   *
   * @return
   *  A set containing all the names in the list.
   */
  public synchronized
  Set
  keySet()
  {
    return new KeySetView(false);
  }

  /**
   * Returns a collection view of the values in the list.
   * Currently not implemented.
   *
   * @return
   *  A collection view of the values in the list.
   */
  public synchronized
  Collection
  values()
  {
    throw new Ex_NotImplemented();
  }

  /**
   * Returns an array of the names in the list.
   * The order of the array is as they were specified in the constructor
   * (if there was an order in the constructor at all).
   */
  public
  List
  getNamesAsList()
  {
    return Arrays.asList(itsNames);
  }

  /**
   * Returns a set view of the mappings in the list.
   * Currently not implemented.
   *
   * @return
   *  A set containing the mappings in the list.
   */
  public synchronized
  Set
  entrySet()
  {
    throw new Ex_NotImplemented();
  }

  /**
   * Reset the list of modified values.
   * The modified flag of all the elements in the list is cleared,
   * without changing their values.
   * The old values for each element that has its modified flag cleared
   * are set to the current value.
   */
  public synchronized
  void
  resetModified()
  throws NullPointerException
  {
    if (itsEditors == null) { 
      throw new NullPointerException(
          "You need to call makeEditors() first");     
    }
    // Make sure the property editors exist.
    // makeEditors();

    // Iterate over all the entries.
    for(int i = 0; i < itsNames.length; i++) {
      if(itsTypes[i].isArray()) {
        // The old value.
        Object[] oldValues = (Object[])itsOldValues[i];
        // The current value.
        Object[] values = (Object[])itsValues[i];
        // The property editors.
        PropertyEditor[] editors = (PropertyEditor[])itsEditors[i];

        // Copy the values from current to old.
        synchronized(itsEditors[i]) {
          for(int j = 0; j < oldValues.length; j++) {
            oldValues[j] = values[j];
            editors[j].setValue(values[j]);
          }
        }
        itsModified[i] = false;
      }
      else {
        synchronized(itsEditors[i]) {
          itsOldValues[i] = itsValues[i];
          ((PropertyEditor)itsEditors[i]).setValue(itsValues[i]);
          itsModified[i] = false;
        }
      }
    }
  }

  /**
   * Revert to the "old" values.
   * Every element that has its modified flag set is set to its
   * old value,
   * and the modified flag is cleared.
   */
  public synchronized
  void
  revert()
  throws NullPointerException
  {
    if (itsEditors == null) { 
      throw new NullPointerException(
          "You need to call makeEditors() first");     
    }
    // Make sure the property editors exist.
    //makeEditors();

    // Iterate over all the entries.
    for(int i = 0; i < itsNames.length; i++) {
      if(itsModified[i]) {
        if(itsTypes[i].isArray()) {
          // The old value.
          Object[] oldValue = (Object[])itsOldValues[i];
          // The current value.
          Object[] value = (Object[])itsValues[i];
          // The property editors.
          PropertyEditor[] editors = (PropertyEditor[])itsEditors[i];

          // Copy the values from old to current.
          synchronized(itsEditors[i]) {
            for(int j = 0; j < oldValue.length; j++) {
              value[j] = oldValue[j];
              editors[j].setValue(value[j]);
            }
            itsModified[i] = false;
          }
        }
        else {
          synchronized(itsEditors[i]) {
            updateValue(i,itsOldValues[i]);
            ((PropertyEditor)itsEditors[i]).setValue(itsValues[i]);
            itsModified[i] = false;
          }
        }
      }
    }
  }

  /**
   * Sets the modified flag for all the elements in the list.
   */
  public synchronized
  void
  setAllModified()
  {
    for(int i = 0; i < itsNames.length; i++) {
      itsModified[i] = true;
    }
  }

  /**
   * Returns a set view of all the modified names in the list.
   * The resulting set is backed by the list.
   * Changes in the list are reflected in changes to the set.
   * Changes to the modified state of flags in the list
   * are reflected in the set.
   *
   * @return
   *  A set view of all the modified names in the list.
   */
  public synchronized
  Set
  keySetModified()
  {
    return new KeySetView(true);
  }

  /**
   * Find the index for a name.
   *
   * @param name
   *  The name to look for.
   *
   * @return
   *  The index,
   *  or -1 if the name is not found.
   */
  int
  getIndexForName(
    String name
  )
  throws
    IllegalArgumentException
  {
    // The value to return.
    Integer result = (Integer)itsNameToIndexMap.get(name);
    if(result == null) {
      result = new Integer(-1);
    }

    return result.intValue();
  }

  /**
   * Makes a properties object from the list.
   *
   * @return
   *  A properties object with a key for every key in the named value list,
   *  and with corresponding values of the string representations of the
   *  values in the named value list.
   */
/*  public
  Properties
  toProperties()
  {
    // The value to return.
    Properties result = new Properties();

    // Add each element of the named value list to the properties object.
    for(int i = 0; i < itsNames.length; i++) {
      try {
        result.setProperty(itsNames[i],getAsTcl(null,itsNames[i]).toString());
      }
      catch(Exception e) {
        // Hopefully, should never happen.
        assert false;
      }
    }

    return result;
  }
  */

  /**
   * Make a string of the object.
   *
   * @return
   *  The string representation of the object.
   */
  public
  String
  toString()
  {
    // The value to return.
    String result = "";

    for(int i = 0; i < itsNames.length; i++) {
      // Space if not first name.
      if(i != 0) {
        result = result + "\n";
      }

      // Name.
      result = result + "{" + itsNames[i];
      // Value.
      if(itsTypes[i].isArray()) {
        Object[] values = (Object[])itsValues[i];
        result = result + " {";
        for(int j = 0; j < values.length; j++) {
          if(j != 0) {
            result = result + " ";
          }
          if(values[j] == null) {
            result = result + "{}";
          }
          else {
            String str = values[j].toString();
            if(str.indexOf(" ") == -1) {
              result = result + str;
            }
            else {
              result = result + "{" + str + "}";
            }
          }
        }
        result = result + "}";
      }
      else {
        if(itsValues[i] == null) {
          result = result + " {}";
        }
        else {
          String str = itsValues[i].toString();
          if(str.indexOf(" ") == -1) {
            result = result + " " + str;
          }
          else {
            result = result + " {" + str + "}";
          }
        }
      }
      // Type
      result = result + " " + itsTypes[i].getName();
      // Readonly flag.
      if(itsReadonlyFlags[i]) {
        result = result + " 1";
      }
      else {
        result = result + " 0";
      }
      // Modified flag.
      if(itsModified[i]) {
        result = result + " 1";
      }
      else {
        result = result + " 0";
      }
      result = result + "}";
    }

    // Make the string representation by converting it to a Tcl object.
//    try {
//      result = toTclObject(null).toString();
//    }
//    catch(TclException e) {
//      // Hopefully, should never happen.
//      Assert.fail(e);
//      result = null;
//    }

    return result;
  }

  /**
   * Set the documentation URL for names stored herein.
   *
   * @param url
   *  The documentation base URL.
   */
  public
  void
  setDocURL(
    URL url
  )
  {
    itsDocURL = url;
  }

  /**
   * Get the documentation URL for names stored herin.
   *
   * @return
   *  The documentation base URL.
   */
  public
  URL
  getDocURL()
  {
    return itsDocURL;
  }

  /**
   * Call this method to set a value in <code>itsValues</code>.
   * It makes sure that the property editors are synchronized.
   *
   * @param index
   *  The index of the value for the property editor to update.
   *
   * @param newValue
   *  The new value.
   *  This is assumed to have already been thoroughly checked, etc.
   */
  private
  void
  updateValue(
    int index,
    Object newValue
  )
  {
    // Make sure the property editors exist.
    // makeEditors();

    if(itsTypes[index].isArray()) {
      updateValue(index,(Object[])newValue);
    }
    else {
      // The property editor.
      if (itsEditors == null) {
        itsModified[index] = true;
        itsValues[index] = newValue;
      } else {
        PropertyEditor editor = (PropertyEditor)itsEditors[index];

        // Make sure the property editor and the value stay in synch.
        synchronized(editor) {
          itsModified[index] = true;
          itsValues[index] = newValue;
          editor.setValue(newValue);
        }
      }
    }
  }

  /**
   * Call this method to set a value in <code>itsValues</code>.
   * It makes sure that the property editors are synchronized.
   *
   * @param index
   *  The index of the value for the property editor to update.
   *
   * @param newValues
   *  The new value.
   *  This is assumed to have already been thoroughly checked, etc.
   */
  private
  void
  updateValue(
    int index,
    Object[] newValues
  )
  {
    // Make sure the property editors exist.
    //makeEditors();

    if (itsEditors == null) {
      // The value.
      itsModified[index] = true;
      Object[] value = (Object[])itsValues[index];
      for (int i=0; i < value.length; i++) {
        value[i] = newValues[i];
      }
    } else {
        // The property editors.
      PropertyEditor[] editors = (PropertyEditor[])itsEditors[index];
      // The value.
      Object[] value = (Object[])itsValues[index];

      // Make sure the property editors and the values stay in synch.
      synchronized(editors) {
        itsModified[index] = true;
        for(int i = 0; i < editors.length; i++) {
          value[i] = newValues[i];
          editors[i].setValue(newValues[i]);
        }
      }
    }
  }

  /**
   * Call this method to set a value in <code>itsValues</code>.
   * It makes sure that the property editors are synchronized.
   *
   * @param index
   *  The index of the value for the property editor to update.
   *  The element must not be an array type.
   *
   * @param newValue
   *  The new value.
   *  Use the property editor to make a new object.
   */
  private
  void
  updateValueAsString(
    int index,
    String newValue
  )
  {
    assert false;
    // Make sure the property editors exist.
    /*makeEditors();

    // The property editor.
    PropertyEditor editor = (PropertyEditor)itsEditors[index];

    // Make sure the property editor and the value stay in synch.
    synchronized(editor) {
      itsModified[index] = true;
      editor.setAsText(newValue);
      itsValues[index] = editor.getValue();
    }*/
  }

  /**
   * Call this method to set a value in <code>itsValues</code>.
   * It makes sure that the property editors are synchronized.
   *
   * @param index
   *  The index of the value for the property editor to update.
   *  The element must be an array type.
   *
   * @param newValues
   *  The strings to set the new values from.
   */
  private
  void
  updateValueAsString(
    int index,
    String[] newValues
  )
  {
    assert false;
    // Make sure the property editors exist.
    /*makeEditors();

    // The property editors.
    PropertyEditor[] editors = (PropertyEditor[])itsEditors[index];

    // Make sure the property editors and the values stay in synch.
    synchronized(editors) {
      itsModified[index] = true;
      for(int i = 0; i < newValues.length; i++) {
        editors[i].setAsText(newValues[i]);
        ((Object[])itsValues[index])[i] = editors[i].getValue();
      }
    }*/
  }
}

