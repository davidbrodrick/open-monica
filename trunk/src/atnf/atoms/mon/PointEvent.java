// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Class: PointEvent
 * Description: Is the event object
 * @author Le Cuong Nguyen
 **/

package atnf.atoms.mon;

import java.util.*;
 
public class PointEvent extends EventObject
{
   PointData itsData = null;
   boolean itsRaw = true;
   
   public PointEvent(Object source, PointData data, boolean
   raw)
   {
      super(source);
      itsData = data;
      itsRaw = raw;
   }
   
   public PointData getPointData()
   {
      return itsData;
   }

   public boolean isRaw()
   {
      return itsRaw;
   }
}
