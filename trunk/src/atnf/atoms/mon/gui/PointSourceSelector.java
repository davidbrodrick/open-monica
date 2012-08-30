//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.gui;

import javax.swing.JFrame; //For test app launched by main
import atnf.atoms.mon.client.*;
import atnf.atoms.mon.util.*;

/**
 * Show Tree of monitor points, with the source name as the last field
 * in the path. This makes it convienient to pick particular monitor points
 * for particular sources.
 *
 * @author David Brodrick
 * @version $Id: PointSourceSelector.java,v 1.5 2006/09/14 00:35:40 bro764 Exp $
 * @see PointNameSelector
 */
public
class PointSourceSelector
extends TreeItemSelector
{
  /** Cache of the full point/source names for each point. */
  private static String[] theirPointNames = null;
  /** cache of the node names for each of the points. */
  private static String[] theirNodeNames = null;

  /** C'tor. */
  public PointSourceSelector()
  {
    super();
  }

  /** Check if there are any other sources for the specified point. */
  private boolean isUnique(String pname) {
    int firstdot = pname.indexOf(".");
    String source = pname.substring(0, firstdot);
    String point  = pname.substring(firstdot+1);
    for (int i=0; i<theirPointNames.length; i++) {
      firstdot = theirPointNames[i].indexOf(".");
      String thissource = theirPointNames[i].substring(0, firstdot);
      String thispoint  = theirPointNames[i].substring(firstdot+1);
      if (thispoint.equals(point) && !thissource.equals(source)) {
        return false;
      }
    }
    return true;
  }

  /** Build <i>itsTreeUtil</i>, containing the names and sources for all
   * monitor points. The nodes have the source name as the last field in
   * the path but the user objects associated with the nodes in the
   * TreeUtil have the conventional name in <i>source.name</i> format. */
  protected
  void
  buildTree()
  {
    if (theirPointNames == null) {
      theirPointNames = MonClientUtil.getAllPointNames();
      theirNodeNames = new String[theirPointNames.length];
      for (int i = 0; i < theirPointNames.length; i++) {
        int firstdot = theirPointNames[i].indexOf(".");
        String source = theirPointNames[i].substring(0, firstdot);
        String point = theirPointNames[i].substring(firstdot + 1);

        if (isUnique(theirPointNames[i])) {
          theirNodeNames[i] = point;
        } else {
          theirNodeNames[i] = point + "." + source;
        }
      }
    }

    itsTreeUtil = new TreeUtil("Points");

    if (theirPointNames != null) {
      for (int i = 0; i < theirPointNames.length; i++) {
        if (!theirNodeNames[i].startsWith("hidden")) {
          itsTreeUtil.addNode(theirNodeNames[i], theirPointNames[i]);
        }
      }
    }
  }


  /** Simple test application. */
  public static void main(String args[])
  {
    PointSourceSelector pt = new PointSourceSelector();

    JFrame frame = new JFrame("PointSourceSelector");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(pt);

    frame.pack();
    frame.setVisible(true);
  }
}
