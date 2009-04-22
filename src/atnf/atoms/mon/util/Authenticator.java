//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Check if a username and password are valid on the system.
 * Currently a basic implementation that uses JPAM, if available.
 * If JPAM is not available then authentication will always fail.
 * JPAM can be obtained from here:
 * http://jpam.sourceforge.net/ */
public class
Authenticator
{
  /** Check the username and password.
   * @param username The username to be authenticated.
   * @param password The password associated with this user.
   * @return True is successful, False otherwise. */
  public static
  boolean
  check(String username, String password)
  {
    boolean res=false;
    try {
      //Use reflection to allow compilation if JPAM not installed
      Class pamclass = Class.forName("net.sf.jpam.Pam");
      Constructor ctor = pamclass.getConstructor(new Class[]{});
      Method meth = pamclass.getDeclaredMethod("authenticateSuccessful",
                                               new Class[]{String.class, String.class});
      Object instance = ctor.newInstance(new Object[]{});
      Object result = meth.invoke(instance, new Object[]{username, password});
      System.err.println("Authenticator: Result = " + result);
    } catch (Exception e) {
      System.err.println("Authenticator: " + e);
      e.printStackTrace();
    }
    return res;
  }
}
