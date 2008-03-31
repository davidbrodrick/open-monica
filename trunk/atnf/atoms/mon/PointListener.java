// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Class: PointListener
 * Description: Handles PointEvents. Its an interface, what else do
 * you expect?
 * @author Le Cuong Nguyen
 **/

package atnf.atoms.mon;

import java.util.*;
 
public interface PointListener extends EventListener
{
   // Override this method
   public void onPointEvent(Object source, PointEvent evt);
}
