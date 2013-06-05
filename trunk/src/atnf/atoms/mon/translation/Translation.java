// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import org.apache.log4j.Logger;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

/**
 * The <code>Translation</code> class is one of the core classes of the monitor system. Translation sub-classes are responsible for
 * converting raw data into useful information. This might happen any number of ways, for instance extracting a single value from an
 * array or by scaling a raw number into real-world units.
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
 * The actual data translation happens in the <code>translate</code> method which much be implemented by each sub-class. This method
 * takes a <code>PointData</code> argument.
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
public abstract class Translation {
  protected String[] itsInit = null;

  protected PointDescription itsParent = null;

  protected static Logger theirLogger = Logger.getLogger(Translation.class.getName());

  protected Translation(PointDescription parent, String[] init) {
    itsInit = init;
    itsParent = parent;
  }

  /** Override this method to perform work. */
  public abstract PointData translate(PointData data);
}
