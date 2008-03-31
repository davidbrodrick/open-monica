// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.translation;

import atnf.atoms.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;

import java.awt.event.*;
import java.lang.reflect.*;
import java.util.StringTokenizer;
import javax.swing.*;


/**
 * The <code>Translation</code> class is one of the core classes of
 * the monitor system. Translation sub-classes are responsible for
 * converting raw data into useful information. This might happen
 * any number of ways, for instance extracting a single value from an
 * array or by scaling a raw number into real-world units.
 *
 * <P>One of the strengths of this system is that Translations can
 * be chained together, so that multiple steps can be used to convert
 * from the raw data to the final information. This is a very powerful
 * feature and allows extensive reuse of existing translations. As an
 * example, the first translation might extract a single number from
 * an array and the second translation might scale that number to a
 * different range. This new value would be the final value of the
 * monitor point.
 *
 * <P>All Translations are constructed by calling the static
 * <code>factory</code> method in this class.
 *
 * <P>The actual data translation happens in the <code>translate</code>
 * method which much be implemented by each sub-class. This method takes
 * a <code>PointData</code> argument. The first translation which is
 * applied for a given monitor point would process the <i>raw</i> data from
 * the PointData. Subsequent translations in the chain will have an
 * argument set to indicate that they should manipulate pre-translated
 * data and leave the raw data alone. At the end of the translation
 * process the PointData will still have the original raw data value
 * from before translation and will also have the final value generated
 * by the last translation in the chain.
 *
 * <P>If all the information is available to complete the relevant
 * translation then the translate method should return a reference to
 * a PointData which contains the translated value (this will usually
 * be the same object that was passed as an argument). If the data
 * value is invalid by some criterion then a valid PointData should
 * be returned but it should have a null data field.
 *
 * <P>On the other hand, if there is insufficient data available to
 * complete the translation then null should be returned from the
 * translate method. No further translations in the chain will then
 * be called.
 *
 * <P>In general, Translations should leave the raw data field of the
 * PointData intact. However it might be useful for specific sub-classes
 * to null this field in some cases. For instance if the translation
 * just extracts one value from a very large array then it is
 * inefficient to continue to maintain a copy of the entire raw data array.
 * Such a translation could extract the value of interest and then null
 * the raw data field.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: $
 **/
public abstract class
Translation
extends MonitorPolicy
//implements PointListener, ActionListener
{
   protected String[] itsInit = null;
   protected PointMonitor itsParent = null;
//   protected MonitorTimer itsTimer = new MonitorTimer(20, this);
   
   /** Override this. Use format fullname, shortname, argname, argtype, argname, argtype, ...
   */
   protected static String itsArgs[] = new String[]{"Translation",""};
   
   protected Translation(PointMonitor parent, String[] init)
   {
      itsInit = init;
      itsParent = parent;
      //itsParent.addPointListener(this);
      //itsTimer.setRepeats(true);
   }


   // Needs a reference back to parent in order to inform that PointMonitor
   // that data has been translated
   public static Translation factory(PointMonitor parent, String arg)
   {
     //Enable use of "null" keyword
     if (arg.equalsIgnoreCase("null")) arg= "-";

     Translation result = null;

     // Find the specific informations
     String specifics = arg.substring(arg.indexOf("-") + 1);
     String[] transArgs = MonitorUtils.tokToStringArray(specifics);

     // Find the type of translation
     String type = arg.substring(0, arg.indexOf("-"));
     if (type == "" || type == null || type.length()<1) type = "None";

     try {
       Constructor Translation_con = Class.forName("atnf.atoms.mon.translation.Translation"+type).getConstructor(new Class[]{PointMonitor.class,String[].class});
       result = (Translation)(Translation_con.newInstance(new Object[]{parent,transArgs}));
     } catch (Exception e) {
       System.err.println("Translation: Error Creating Translation!!");
       System.err.println("\tFor Monitor Point: " + parent.getName());
       System.err.println("\tFor Translation:   " + arg);
       //substitude a no-op translation instead, dunno if it is best course
       //of action but at least system keeps running..
       result = new TranslationNone(parent, new String[]{});
     }

     result.setStringEquiv(arg);

     PointBuffer.add(result);

     return result;
   }


   /** Override this method to perform work. */
   public abstract
   PointData
   translate(PointData data);


   // Data has been collected
/*   public
   void
   onPointEvent(Object source, PointEvent evt)
   {

     PointData newData = translate(evt.getPointData());
     if (newData != null) {
       PointEvent newDataEvt = new PointEvent(this, newData, false);
       itsParent.firePointEvent(newDataEvt);
     }
   }
*/

   public static String[] getArgs()
   {
      return itsArgs;
   }

//   public void actionPerformed(ActionEvent ae){}
}
