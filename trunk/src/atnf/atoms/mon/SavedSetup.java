//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon;

import java.util.*;
import java.io.*;
import java.lang.reflect.Constructor;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.util.NamedObject;

/**
 * An extended <i>HashMap</i> used for storing the setup for a MonPanel.
 * 
 * <P>
 * In the first MonPanel implementation, each class had a routine for
 * serialising its current setup into a fixed format String. This was okay but
 * it was difficult to extend the fixed String format, eg, if we wanted to add a
 * new field.
 * 
 * <P>
 * The selected solution is to use a named-value format for storing MonPanel
 * setup information. However because we don't want to be locked-in to a
 * particular JVM/HashMap version we need to manage the serialisation ourselves.
 * This class is basically just a HashMap which has some home-grown methods for
 * serialising/deserialising to/from a String.
 * 
 * @author David Brodrick
 * @version $Id: SavedSetup.java,v 1.1 2005/07/25 01:20:56 bro764 Exp bro764 $
 */
public class SavedSetup extends HashMap<String, String> implements NamedObject, Comparable
{
  /** Serialized version id. */
  static final long serialVersionUID = -4564836140257941860L;

  /** Holds all known <i>'saved setups</i> for the clients to use. */
  private static TreeMap<String, SavedSetup> itsSetupMap = new TreeMap<String, SavedSetup>();

  /** Class that this setup information belongs to. */
  String itsClass = null;

  /** Hierarchical name for this setup. */
  String itsName = "";

  /** Add the new SavedSetup to the system. */
  public static synchronized void addSetup(SavedSetup setup)
  {
    if (itsSetupMap.get(setup.getLongName()) != null) {
      // Map already contains a setup with that name. Remove and reinsert.
      itsSetupMap.remove(setup.getLongName());
    }
    itsSetupMap.put(setup.getLongName(), setup);
  }

  /** Remove the setup with the given name from the system. */
  public static synchronized void removeSetup(String setupname)
  {
    SavedSetup setup = (SavedSetup) itsSetupMap.get(setupname);
    if (setup != null) {
      itsSetupMap.remove(setup);
    }
  }

  /** Return all SavedSetups on the system. */
  public static synchronized SavedSetup[] getAllSetups()
  {
    Object[] allkeys = itsSetupMap.keySet().toArray();
    if (allkeys == null || allkeys.length == 0) {
      return null;
    }

    SavedSetup[] res = new SavedSetup[allkeys.length];
    for (int i = 0; i < allkeys.length; i++) {
      res[i] = (SavedSetup) itsSetupMap.get(allkeys[i]);
    }
    return res;
  }

  /** Construct an empty setup. */
  public SavedSetup()
  {
    super();
  }

  /** Construct a setup with the given name and class. */
  public SavedSetup(String name, String classname)
  {
    super();
    itsName = name;
    itsClass = classname;
  }

  /** Construct a setup with the given name and class and initial capacity. */
  public SavedSetup(String name, String classname, int len)
  {
    super(len);
    itsName = name;
    itsClass = classname;
  }

  /** Construct a new setup with the given initial capacity. */
  public SavedSetup(int size)
  {
    super(size);
  }

  /** Construct a new setup using the same mappings as the given Map. */
  public SavedSetup(Map<String, String> t)
  {
    super(t);
  }

  /** Escape any reserved characters. */
  protected static String escape(String arg)
  {
    String res = "";
    for (int i = 0; i < arg.length(); i++) {
      if (i + 1 < arg.length() && arg.substring(i, i + 2).equals("\\'")) {
        // Need to escape the escape..
        res += "\\\\'";
        i++;
      } else if (arg.substring(i, i + 1).equals("`")) {
        // Need to escape the reserved character
        res += "\\'";
      } else if (arg.substring(i, i + 1).equals("\n")) {
        res += "\\n";
      } else if (arg.substring(i, i + 1).equals("\r")) {
        res += "\\r";
      } else {
        // Add this character to the result as is
        res += arg.substring(i, i + 1);
      }
    }
    return res;
  }

  /** Unescape any reserved character sequences. */
  protected static String unescape(String arg)
  {
    String res = "";
    for (int i = 0; i < arg.length(); i++) {
      if (i + 2 < arg.length() && arg.substring(i, i + 3).equals("\\\\'")) {
        // Need to unescape an escape..
        res += "\\'";
        i += 2;
      } else if (i + 1 < arg.length() && arg.substring(i, i + 2).equals("\\'")) {
        // Need to replace the escape with the reserved character
        res += "`";
        i++;
      } else if (i + 1 < arg.length() && arg.substring(i, i + 2).equals("\\n")) {
        res += "\n";
        i++;
      } else if (i + 1 < arg.length() && arg.substring(i, i + 2).equals("\\r")) {
        res += "\r";
        i++;
      } else {
        // Add this character to the result as is
        res += arg.substring(i, i + 1);
      }
    }
    return res;
  }

  /** Get the setup in String form. */
  public String toString()
  {
    String res = "{";
    if (itsClass != null && !itsClass.equals("")) {
      res += itsClass + "`";
    } else {
      res += "null`";
    }
    if (itsName != null && !itsName.equals("")) {
      res += itsName + "`";
    } else {
      res += "null`";
    }

    res += size();

    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry me = (Map.Entry) it.next();
      String temp = "`" + escape((String) me.getKey()) + "`" + escape((String) me.getValue());
      res += temp;
    }
    if (size() > 0) {
      res += "`}";
    } else {
      res += "}";
    }
    return res;
  }

  /** Get a setup by parsing the string. */
  public static SavedSetup fromString(String str) throws IllegalArgumentException
  {
    if (!str.startsWith("{")) {
      throw new IllegalArgumentException();
    }

    StringTokenizer st = new StringTokenizer(str, "`");
    if (st.countTokens() < 3) {
      // Must be a problem with this setup
      throw new IllegalArgumentException();
    }
    // Expect "{class`name`size`key1`value1`}"
    String cls = st.nextToken().substring(1).trim();
    if (cls.equals("null")) {
      cls = null;
    }
    String name = st.nextToken().trim();
    if (name.equals("null")) {
      name = "";
    }
    int size = Integer.parseInt(st.nextToken().trim());
    SavedSetup mps = new SavedSetup(name, cls, size);

    for (int i = 0; i < size; i++) {
      if (!st.hasMoreTokens()) {
        throw new IllegalArgumentException();
      }
      String key = unescape(st.nextToken());
      String value = unescape(st.nextToken());
      mps.put(key, value);
    }

    if (!st.nextToken().equals("}")) {
      throw new IllegalArgumentException();
    }
    return mps; // done!
  }

  /**
   * Create an instance of the class this setup is for, using the no-arguments
   * constructor..
   */
  public Object getInstance() throws Exception
  {
    Class theclass = Class.forName(itsClass);
    Class[] cargc = {};
    Object[] cargv = {};
    // Find the c'tor with no arguments
    Constructor ctor = theclass.getConstructor(cargc);
    if (ctor == null) {
      return null;
    } else {
      return ctor.newInstance(cargv);
    }
  }

  /** Set the class to associate with this setup. */
  public void setClass(Class c)
  {
    setClass(c.getName());
  }

  /** Set the class to associate with this setup. */
  public void setClass(String c)
  {
    itsClass = c;
  }

  /** Return the name of the class associated with this setup. */
  public String getClassName()
  {
    return itsClass;
  }

  /**
   * Check if this SavedSetup is for the same class as the test Object.
   * @param test The object who's class we should check.
   * @return <tt>true</tt> if the test object matches the class for this
   * SavedSetup, <tt>false</tt> if the classes do not match.
   */
  public boolean checkClass(Object test)
  {
    if (test == null) {
      return false;
    }
    if (!itsClass.equals(test.getClass().getName())) {
      return false;
    }
    return true;
  }

  /**
   * Check if this SavedSetup is suitable for specified class.
   * @param classname String name of the class we should check against.
   * @return <tt>true</tt> if the test class matches the class for this
   * SavedSetup, <tt>false</tt> if the classes do not match.
   */
  public boolean checkClass(String classname)
  {
    if (classname == null) {
      return false;
    }
    if (!itsClass.equals(classname)) {
      return false;
    }
    return true;
  }

  /** Gets the long name of the object. */
  public String getLongName()
  {
    return itsName;
  }

  /** gets the short name associated with this object. */
  public String getName()
  {
    return itsName;
  }

  /** gets the name at the index specified. */
  public String getName(int i)
  {
    if (i == 0) {
      return itsName;
    } else {
      return null;
    }
  }

  /** gets the total number of names this object has (one). */
  public int getNumNames()
  {
    return 1;
  }

  /** set the hierarchical name for this setup. */
  public void setName(String name)
  {
    itsName = name;
  }

  /** Compare our name with the other SavedSetup. */
  public int compareTo(Object obj)
  {
    if (!(obj instanceof SavedSetup)) {
      System.err.println("SavedSetup:compareTo: Only for SavedSetup objects");
      return 0;
    }
    return (getLongName().compareTo(((SavedSetup) obj).getLongName()));
  }

  /**
   * Recover the SavedSetups stored in the file.
   * @param setupfilename The name of the file containing the SavedSetups.
   * @return Vector containing the setups, possibly empty.
   */
  public static Vector<SavedSetup> parseFile(String setupfilename) throws Exception
  {
    return parseFile(new FileReader(setupfilename));
  }

  /**
   * Recover the SavedSetups stored in the file.
   * @param setupfile The file containing the SavedSetups.
   * @return Vector containing the setups, possibly empty.
   */
  public static Vector<SavedSetup> parseFile(Reader setupfile) throws Exception
  {
    Vector<SavedSetup> res = new Vector<SavedSetup>();

    // Pre-process the file, exit if empty
    String[] lines = MonitorUtils.parseFile(setupfile);
    if (lines == null) {
      return res;
    }

    // Try to parse the lines one at a time
    for (int i = 0; i < lines.length; i++) {
      try {
        SavedSetup tempsetup = fromString(lines[i]);
        if (tempsetup != null) {
          res.add(tempsetup);
        }
      } catch (Exception e) {
        System.err.println("ERROR PARSING SavedSetup (" + i + "): " + e.getMessage());
      }
    }

    return res;
  }

  /** Simple test code. */
  public static final void main(String[] args)
  {
    SavedSetup mps = new SavedSetup();
    mps.put("title", "foo");
    mps.put("count", "1`3");
    System.out.println(mps);
    SavedSetup mps2 = new SavedSetup();
    mps2.put("title", "`other title!`");
    mps2.put("field1", mps.toString());
    String foo = mps2.toString();
    System.out.println(foo);
    SavedSetup mps3 = SavedSetup.fromString(foo);
    System.out.println(mps3);
    String foo2 = (String) mps3.get("field1");
    SavedSetup mps4 = SavedSetup.fromString(foo2);
    System.out.println(mps4);
  }
}
