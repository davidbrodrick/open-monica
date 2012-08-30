//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import javax.swing.JFrame; //For test app launched by main
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
  private static String[] theirPointNames = null;

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
    if (theirPointNames==null) {
      theirPointNames = MonClientUtil.getAllPointNames();
    }

    itsTreeUtil = new TreeUtil("Points");

    if (theirPointNames!=null) {
      for (int i=0; i<theirPointNames.length; i++) {
        //We are only interested in the name, not source, component
        int doti = theirPointNames[i].indexOf(".");
        String name = theirPointNames[i].substring(doti+1);
        if (!name.startsWith("hidden")) {
          if (itsTreeUtil.getNode(name)==null) {
            itsTreeUtil.addNode(name, name);
          }
        }
      }
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
