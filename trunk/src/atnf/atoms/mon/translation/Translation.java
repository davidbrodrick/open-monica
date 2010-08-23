// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;

import atnf.atoms.mon.MonitorPolicy;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.archivepolicy.ArchivePolicy;
import atnf.atoms.mon.util.MonitorUtils;

/**
 * The <code>Translation</code> class is one of the core classes of the monitor system. Translation sub-classes are responsible
 * for converting raw data into useful information. This might happen any number of ways, for instance extracting a single value
 * from an array or by scaling a raw number into real-world units.
 * 
 * <P>
 * One of the strengths of this system is that Translations can be chained together, so that multiple steps can be used to convert
 * from the raw data to the final information. This is a very powerful feature and allows extensive reuse of existing translations.
 * As an example, the first translation might extract a single number from an array and the second translation might scale that
 * number to a different range. This new value would be the final value of the monitor point.
 * 
 * <P>
 * All Translations are constructed by calling the static <code>factory</code> method in this class.
 * 
 * <P>
 * The actual data translation happens in the <code>translate</code> method which much be implemented by each sub-class. This
 * method takes a <code>PointData</code> argument.
 * 
 * <P>
 * If all the information is available to complete the relevant translation then the translate method should return a reference to a
 * PointData which contains the translated value. If the data value is invalid by some criterion then a valid PointData should be
 * returned but it should have a null data field.
 * 
 * <P>
 * On the other hand, if there is insufficient data available to complete the translation then null should be returned from the
 * translate method. No further translations in the chain will then be called.
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public abstract class Translation extends MonitorPolicy
{
  protected String[] itsInit = null;

  protected PointDescription itsParent = null;

  protected Translation(PointDescription parent, String[] init)
  {
    itsInit = init;
    itsParent = parent;
  }

  public static Translation factory(PointDescription parent, String strdef)
  {
    // Enable use of "null" keyword
    if (strdef.equalsIgnoreCase("null")) {
      strdef = "-";
    }

    Translation result = null;

    try {
      // Find the argument strings
      String specifics = strdef.substring(strdef.indexOf("-") + 1);
      String[] transArgs = MonitorUtils.tokToStringArray(specifics);

      // Find the type of translation
      String type = strdef.substring(0, strdef.indexOf("-"));
      if (type == "" || type == null || type.length() < 1) {
        type = "None";
      }

      Constructor Translation_con;
      try {
        // Try to find class by assuming argument is full class name
        Translation_con = Class.forName(type).getConstructor(new Class[] { PointDescription.class, String[].class });
      } catch (Exception f) {
        // Supplied name was not a full path
        // Look in atnf.atoms.mon.translation package
        Translation_con = Class.forName("atnf.atoms.mon.translation.Translation" + type).getConstructor(
                new Class[] { PointDescription.class, String[].class });
      }
      result = (Translation) (Translation_con.newInstance(new Object[] { parent, transArgs }));
    } catch (Exception e) {
      Logger logger = Logger.getLogger(Translation.class.getName());
      logger.error("Error creating Translation \'" + strdef + "\' for point " + parent.getFullName() + ": " + e);
      // substitute a no-op translation instead, dunno if it is best course of action but at least system keeps running..
      result = new TranslationNone(parent, new String[] {});
    }

    return result;
  }

  /** Override this method to perform work. */
  public abstract PointData translate(PointData data);
}
