//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import javax.swing.JFrame; //For test app launched by main
import java.util.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.mon.util.*;

/**
 * Show Tree of monitor point names. This makes it convienient to pick
 * particular monitor points without regard to their sources.
 *
 * @author David Brodrick
 * @see PointSourceSelector
 */
public class PointNameSelector
extends TreeItemSelector
{
  /** Cache of point names mapped to their list of sources */
  private static Hashtable<String, Vector<String>> theirPointSourceMap;

  /** C'tor. */
  public PointNameSelector()
  {
    super();
  }


  /** C'tor, with border title argument. */
  public PointNameSelector(String title)
  {
      super(title);
  }


  /** Build <i>itsTreeUtil</i>, containing the names of all monitor points. */
  protected
  void
  buildTree()
  {
    if (theirPointSourceMap == null) {
      theirPointSourceMap = MonClientUtil.getPointSourceMap();
    }
    itsTreeUtil = new TreeUtil("Points");
    Iterator keyIter = theirPointSourceMap.keySet().iterator();
    while (keyIter.hasNext()) {
        String name = (String)keyIter.next();
        itsTreeUtil.addNode(name, name);
    }
  }

  /** Simple test application. */
  public static void main(String args[])
  {
    PointNameSelector pt = new PointNameSelector();

    JFrame frame = new JFrame("PointNameSelector");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(pt);

    frame.pack();
    frame.setVisible(true);
  }
}
