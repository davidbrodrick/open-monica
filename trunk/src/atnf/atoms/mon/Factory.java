package atnf.atoms.mon;

import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;

import atnf.atoms.mon.util.MonitorUtils;

/**
 * Contains a factory method which parses a supplied string to determine a class, which
 * takes a PointDescription and array of strings as constructor arguments, to be
 * relectively instanciated.
 * 
 * <P>
 * Example: The string <tt>demo.com.FooBar-"abc""123"</tt> would create a new instance
 * of the class <tt>demo.com.FooBar</tt>. The string arguments would be <tt>abc</tt>
 * and <tt>123</tt> enoded in an array.
 * 
 * <P>
 * If the supplied class name cannot be found then the provided <i>defpackage</i>
 * argument will be prepended to the class part of the string and an attempt will be made
 * to find an instanciate a class with the resulting name.
 * 
 * @author David Brodrick
 */
public class Factory
{
  public static Object factory(PointDescription parent, String strdef, String defpackage)
  {
    // Find the class type
    String type = strdef.substring(0, strdef.indexOf("-"));
    if (type.equals("")) {
      // No class specified
      return null;
    }

    // Find the specific constructor arguments
    String allargs = strdef.substring(strdef.indexOf("-") + 1);
    String[] args = MonitorUtils.tokToStringArray(allargs);

    Object result = null;
    try {
      Constructor con;
      try {
        // Try to find class by assuming argument is full class name
        con = Class.forName(type).getConstructor(new Class[] { PointDescription.class, String[].class });
      } catch (Exception f) {
        // Supplied name was not a full path
        // Look in default package name provided
        con = Class.forName(defpackage + type).getConstructor(new Class[] { PointDescription.class, String[].class });
        result = (con.newInstance(new Object[] { parent, args }));
      }
    } catch (Exception e) {
      Logger logger = Logger.getLogger(Factory.class.getName());
      logger.error("Error creating field \'" + strdef + "\' for point " + parent.getFullName() + ": " + e);
    }

    return result;
  }
}
