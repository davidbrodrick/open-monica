//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import atnf.atoms.mon.util.*;
import atnf.atoms.mon.SavedSetup;

import javax.swing.JPanel;
import javax.swing.JFrame;
import java.util.Vector;
import java.lang.reflect.Constructor;
import java.io.*;


/**
 *@author David Brodrick
 *@version $Id: MonPanel.java,v 1.1 2005/09/19 23:38:01 bro764 Exp bro764 $
 */
public abstract class
MonPanel
extends JPanel
{
  /** Container for the descriptions of registered MonPanel sub-classes. */
  private static Vector theirNames;

  /** Container for the class types of registered MonPanel sub-classes. */
  private static Vector theirClasses;


  //static block to force loading of MonPanel sub-classes
  static {
    theirNames = new Vector();
    theirClasses = new Vector();
    loadMonPanels();
  }


  /** Force the loading of MonPanel sub-classes. This will cause the
   * sub-classes to register themselves by calling <i>registerMonPanel</i>
   * so that we have an inventory of what classes are available. */
  private static
  void
  loadMonPanels()
  {
    InputStream preloadfile = MonPanel.class.getClassLoader().getResourceAsStream("monitor-preloads.txt");
    if (preloadfile==null) {
      System.err.println("ERROR: Could not find monitor-preloads.txt configuration file");
      System.exit(1);
    }
    //Load the class list file
    String[] classes = MonitorUtils.parseFile(new InputStreamReader(preloadfile));
    if (classes==null) {
      System.err.println("WARNING: monitor-preloads.txt specifies no classes");
      return;
    }

    if (classes!=null) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      for (int i=0; i<classes.length; i++) {
	try {
	  Class c = loader.loadClass(classes[i]);
	  if (c==null) {
	    System.err.println("WARNING: Class " + classes[i] + " was not loaded");
	  } else {
	    //For some reason loading and resolving the classes doesn't
	    //actually execute their static blocks, so create an instance of
            //each class to ensure static blocks are executed.
	    Object temp = null;
	    try {
	      Constructor ctor = c.getConstructor((Class[])null);
	      temp = ctor.newInstance((Object[])null);
              if (temp instanceof MonPanel) {
                ((MonPanel)temp).vaporise();
              }
              System.err.println("OK for " + classes[i]);
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    System.out.println("Preloaded class " + c.getName() + " OK");
	  }
	} catch (Exception e) {
	  System.err.println("WARNING: Class " + classes[i] + " was not found");
        }
      }
    }
  }


  /** Register a new MonPanel sub-class. Once registered, the class can
   * be selected by the user at runtime to create a new instance.
   * @param name Descriptive string for the MonPanel sub-class.
   * @param type Class of the new MonPanel sub-class. */
  protected synchronized static
  void
  registerMonPanel(String name, Class type)
  {
    theirNames.add(name);
    theirClasses.add(type);
  }


  /** Get a new instance of the MonPanel sub-class at the specified index.
   * @param name Index of the MonPanel class to instanciate.
   * @return New MonPanel instance, or null if the class wasn't found. */
  public synchronized static
  MonPanel
  getMonPanel(int index)
  {
    if (index<0 || index>=theirClasses.size()) {
      return null;
    }

    MonPanel res = null;
    try {
      Constructor ctor = ((Class)theirClasses.get(index)).getConstructor((Class[])null);
      res = (MonPanel)ctor.newInstance((Object[])null);
    } catch (Exception e) {
      e.printStackTrace();
      res = null;
    }
    return res;
  }


  /** Get a new instance of the MonPanel sub-class with the given descriptive
   * string.
   * @param name Descriptive string for the MonPanel class to instanciate.
   * @return New MonPanel instance, or null if the class wasn't found. */
  public synchronized static
  MonPanel
  getMonPanel(String name)
  {
    return getMonPanel(theirNames.indexOf(name));
  }


  /** Get an array containing descriptive names for all registered MonPanel
   * classes. This method might be used to present the available options
   * to the user so they can select which MonPanel sub-class to instanciate.
   * @return Array of MonPanel class descriptions, or <tt>null</tt> if there
   *         are no registered MonPanel classes. */
  public synchronized static
  String[]
  getNames()
  {
    int numnames = theirNames.size();
    if (numnames==0) {
      return null;
    }
    String[] res = new String[numnames];
    for (int i=0; i<numnames; i++) {
      res[i] = (String)theirNames.get(i);
    }
    return res;
  }


  /** Get the descriptive name for the specified class.
   * @param c Class to get the description for.
   * @returns The description, or <tt>null</tt> if class not found. */
  public synchronized static
  String
  getName(Class c)
  {
    for (int i=0; i<theirClasses.size(); i++) {
      if (((Class)theirClasses.get(i)).getName().equals(c.getName())) {
        return (String)theirNames.get(i);
      }
    }
    return null;
  }


  /** Get the descriptive name for the class at the specified index.
   * @param i Index of class to get the description for.
   * @returns The description, or <tt>null</tt> if class not found. */
  public synchronized static
  String
  getName(int i)
  {
    if (i>=0 && i<theirNames.size()) {
      return (String)theirNames.get(i);
    } else {
      return null;
    }
  }


  /** Get the class at the specified index.
   * @param i Index to get the class for.
   * @returns The class, or <tt>null</tt> if class not found. */
  public synchronized static
  Class
  getClass(int i)
  {
    if (i>=0 && i<theirClasses.size()) {
      return (Class)theirClasses.get(i);
    } else {
      return null;
    }
  }


  /** Get the class for the given descriptive name.
   * @param name The descriptive name to get the class for.
   * @returns The class, or <tt>null</tt> if name not found. */
  public synchronized static
  Class
  getClass(String name)
  {
    for (int i=0; i<theirNames.size(); i++) {
      if (((String)theirNames.get(i)).equals(name)) {
        return (Class)theirClasses.get(i);
      }
    }
    return null;
  }


  /** Reference to the frame in which this panel is displayed. */
  protected JFrame itsFrame = null;


  /** Provide a reference to the Frame this panel is displayed in. We
   * need this for some operations, eg, bringing up error dialogs. The
   * MonFrame class is expected to call this method when we are added.
   * @param frame The JFrame this panel is displayed in. */
  public
  void
  setFrame(JFrame frame)
  {
    itsFrame = frame;
  }


  /** Get a Frame with the controls required to configure this MonPanel.
   * <tt>null</tt> is a legitimate return value if the panel has no
   * configuration options.
   * @return GUI controls to configure this MonPanel. */
  public abstract
  MonPanelSetupPanel
  getControls();


  /** Configure this MonPanel to use the specified setup. The setup will
   * specify sub-class-specific information, so this method can be used
   * to restore saved MonPanel states.
   * @param setup sub-class-specific setup information.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if
   *         there  was a problem and the setup cannot be used.
   */
  public abstract
  boolean
  loadSetup(SavedSetup setup);


  /** Get the current sub-class-specific configuration for this MonPanel.
   * This can be used to capture the current state of the MonPanel so that
   * it can be easily recovered later.
   * @return sub-class-specific configuration information.
   */
  public abstract
  SavedSetup
  getSetup();


  /** Free all resources so that this MonPanel can disappear.
   * This is basically a destructor, perhaps there is a better way to
   * realise this in Java?
   * <P><tt>
   * The Collaborative International Dictionary of English v.0.44: <BR>
   * Vaporize \Vap"o*rize\, v. i.<BR>
   *  To pass off in vapor.<BR>
   *  [1913 Webster] </tt>
   */
  public abstract
  void
  vaporise();


  /** Dump current data to the given output stream. This is the mechanism
   * through which data can be exported to a file. For instance a graph
   * panel may dump the current data in CSV format, a clock panel would
   * write the current times, etc.
   * @param p The print stream to write the data to. */
  public abstract
  void
  export(PrintStream p);


  /** Return a short string, used to label the setup tab for the MonPanel.
   * If each instance of the class has some kind of <i>title</i> then it may
   * be used, otherwise a generic fixed string is okay.
   * @return A label for the setup tab. */
  public abstract
  String
  getLabel();


  /** Check the string for reserved characters. This is a utility method
   * which can be used to check if user entered text contains characters
   * that are reserved for the internal use of MonPanels. For instance
   * you might use this to check a title string entered by the user, to
   * determine if it contains any of the illegal characters.
   * @param s String to check for reserved characters.
   * @returns <tt>true</tt> if the string is okay, <tt>false</tt> if the
   *          string contains reserved characters. */
  public
  boolean
  checkString(String s)
  {
    if (s.indexOf("`")!=-1) {
      return false;
    } else {
      return true;
    }
  }

}
