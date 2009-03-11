// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

/**
 * Class: PointControl
 * Description: Subclass of PointTransaction which encapsulates all
 *  aspects of a control point. Still not done yet.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: $
 **/

package atnf.atoms.mon;
import java.util.ArrayList;

public class
PointControl extends PointInteraction
{
  public static ArrayList
  parseLine(String line)
  {
    //Comments as a rough guide only, modify algorithm as required

    //The "C" or "M" has already been parsed - we know it said "C"
    //Count number of names. Store each name in an array
    //Count the number of sources
    //for each source
      //create a new PointControl
      //call setNames so that all sources will share the same names array
      //set the itsIsControl field to true
      //set the source name

    //Check if one transaction is defined or many
    //if one transaction is defined
      //create it using Transaction.factory
      //set all the PointMonitors to share the same transaction
    //else there should be one transaction defined for each source
      //for each source
      //create its transaction using Transaction.factory()
      //set that point to use its transaction

    //Same process, one or many for Translation

    //We dont know what else may go into a control point yet.
    
    return null;
  }
  
  public void collectData(){}
}

