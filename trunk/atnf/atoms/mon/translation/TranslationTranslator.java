//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import java.util.*;
import atnf.atoms.mon.*;
import atnf.atoms.util.*;
import atnf.atoms.mon.util.*;

/**
 * Convert the dataset output for the mm receiver translator position into
 * a position measurement.
 *
 * @author David Brodrick
 * @version $Id: TranslationTranslator.java,v 1.1 2007/09/14 03:03:49 bro764 Exp $
 */
public class
TranslationTranslator
extends Translation
{
  protected static String[] itsArgs = new String[]{"Translation Translator",
  "Translator"};
   
  public TranslationTranslator(PointMonitor parent, String[] init)
  {
    super(parent, init);
  }


  /** Calculate the translator position. */
  public
  PointData
  translate(PointData data)
  {
    Object val = data.getData();
    if (val==null || !(val instanceof Number)) {
      //Return point with a null data field
      return new PointData(itsParent.getName(), itsParent.getSource());
    }

    //Get argument as an int
    int ival = ((Number)val).intValue();
    //Extract the three full BCD digits
    float fres = 100*((ival&0xF00)>>8) + 10*((ival&0xF0)>>4) + (ival&0xF);
    //Check if the 100mm bit is set
    if ((ival&0x1000)!=0) fres=fres+1000;
    //Check the sign bit
    if ((ival&0x8000)!=0) fres=-fres;
    //Divide by 10 to obtain final result in mm
    fres=fres/10.0f;

    //Make result object
    PointData res = new PointData(itsParent.getName(),
				  itsParent.getSource(),
				  data.getTimestamp(),
				  new Float(fres));
    //Set the "raw" field to be the argument datum and return result
    res.setRaw(val);
    return res;
  }


  public static String[] getArgs()
  {
     return itsArgs;
  }

/*  public final static void main(String[] argv)
  {
    int ival = 45144;
    //Extract the three full BCD digits
    float fres = 100*((ival&0xF00)>>8) + 10*((ival&0xF0)>>4) + (ival&0xF);
    //Check if the 100mm bit is set
    if ((ival&0x1000)!=0) fres=fres+1000;
    //Check the sign bit
    if ((ival&0x2000)!=0) fres=-fres;
    //Divide by 10 to obtain final result in mm
    fres=fres/10.0f;
    System.out.println(fres);
  }*/
}
