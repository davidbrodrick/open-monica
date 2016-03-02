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
import java.util.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.mon.util.*;

/**
 * Show Tree of monitor points, with the source name as the last field
 * in the path. This makes it convienient to pick particular monitor points
 * for particular sources.
 *
 * @author David Brodrick, Simon Hoyle
 * @version $Id: PointSourceSelector.java,v 1.5 2006/09/14 00:35:40 bro764 Exp $
 * @see PointNameSelector
 */
public
class PointSourceSelector
extends TreeItemSelector
{
  /** Cache of point names mapped to their list of sources */
  private static Hashtable<String, Vector<String>> theirPointSourceMap;

  /** C'tor. */
  public PointSourceSelector()
  {
    super();
  }

  /** Build <i>itsTreeUtil</i>, containing the names and sources for all
   * monitor points. The nodes have the source name as the last field in
   * the path but the user objects associated with the nodes in the
   * TreeUtil have the conventional name in <i>source.name</i> format. */
  protected
  void
  buildTree()
  {
    if (theirPointSourceMap == null) {
      theirPointSourceMap = MonClientUtil.getPointSourceMap();
    }
    String name, source;
    itsTreeUtil = new TreeUtil("Points");
    Iterator keyIter = theirPointSourceMap.keySet().iterator();
    while (keyIter.hasNext()) {
        name = (String)keyIter.next();
	Vector<String> v = theirPointSourceMap.get(name);
	if (v.size() == 1) { // only one source
	     source = (String)v.get(0);
             itsTreeUtil.addNode(name, source + "." + name);
	}
	else {
	   for (int i = 0; i < v.size(); ++i) {
	      source = (String)v.get(i);
              itsTreeUtil.addNode(name + "." + source, source + "." + name);   
	    }
        }
    }   
  }

  public String[] getAllPointNames(){
    return MonClientUtil.getAllPointNames();
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
