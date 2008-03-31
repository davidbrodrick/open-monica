//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
//import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Vector;

import atnf.atoms.mon.client.*;
//import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;

/**
 * Listen for selection changes from a PointNameSelector and allow the user
 * to choose from among the sources available for the selected monitor points.
 *
 * @author David Brodrick
 * @version $Id: $
 * @see PointNameSelector
 */
public class
SourceChooser
extends JPanel
implements ChangeListener
{

  class SourceChooserModel extends AbstractTableModel {
    final String[] columnNames = {"Selected", "Source"};

    public int getColumnCount() {
      return columnNames.length;
    }
    
    public int getRowCount() {
      return itsSources.size();
    }

    public String getColumnName(int col) {
      return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
      if (col==0) return itsSelections.get(row);
      else return itsSources.get(row);
    }

    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
      if (col==0) return true;
      else return false;
    }

    public void setValueAt(Object value, int row, int col) {
      if (col!=0) return;
      itsSelections.set(row, value);
      fireTableCellUpdated(row, col);
    }
  }


  /** The PointNameChooser with which we are associated. */
  protected PointNameSelector itsPNS = null;

  /** The Table in which we show the source name options. */
  protected JTable itsTable = null;

  /** The TableModel */
  protected SourceChooserModel itsModel = new SourceChooserModel();

  /** Vector containing the names of all available sources. */
  protected Vector itsSources = new Vector();

  /** Vector containing selection status of each source. */
  protected Vector itsSelections = new Vector();


  /** C'tor with selector to listen to. */
  public
  SourceChooser(PointNameSelector pns)
  {
    this(pns, "Select Sources");
  }


  /** C'tor with selector and title arguments. */
  public
  SourceChooser(PointNameSelector pns, String bordertitle)
  {
    TitledBorder title = BorderFactory.createTitledBorder(bordertitle);
    setBorder(title);
    setLayout(new BorderLayout());
    itsPNS = pns;
    itsPNS.addChangeListener(this);
    itsTable = new JTable(itsModel);
    add(new JScrollPane(itsTable), BorderLayout.CENTER);
  }


  /** Called when selection in PointNameSelector is changed. */
  public
  void
  stateChanged(ChangeEvent e)
  {
    updateSourceList();
  }


  /** Get the new list of source options. The selection status of any sources
   * already in the list will not be changed. New sources will be automatically
   * selected.
   * @param */
  private
  void
  updateSourceList()
  {
    Thread realWork = new Thread() {
      public void run() {
	Vector points = itsPNS.getSelections();
	Vector newsources = new Vector();

	if (points.size()>0) {
	  //Ask the server to list the sources for each of the selected points
	  Vector sresp = MonClientUtil.getServer().getSources(points);

	  for (int i=0; i<sresp.size(); i++) {
	    String[] s = (String[])sresp.get(i);
	    if (s==null || s.length==0) continue;
	    for (int j=0; j<s.length; j++) {
	      if (!newsources.contains(s[j])) newsources.add(s[j]);
	    }
	  }
	}

	//Sort source list alphabetically
	Vector sortedsources = new Vector();
	while (newsources.size()>0) {
	  String next = (String)newsources.get(0);
	  for (int i=1; i<newsources.size(); i++) {
	    String test = (String)newsources.get(i);
	    if (next.compareTo(test)>0) next = test;
	  }
	  newsources.remove(next);
	  sortedsources.add(next);
	}

	//Work out selection status for each source
	Vector newselections = new Vector();
	for (int i=0; i<sortedsources.size(); i++) {
	  String source = (String)sortedsources.get(i);
	  int oldindex = itsSources.indexOf(source);
	  if (oldindex==-1) {
	    newselections.add(new Boolean(true));
	  } else {
	    newselections.add(itsSelections.get(oldindex));
	  }
	}
	itsSources = sortedsources;
	itsSelections = newselections;

	//We've finished the slow network I/O, tell GUI to update
	Runnable tellSwing = new Runnable() {
	  public void run() {
	    itsModel.fireTableStructureChanged();
	  }
	};
	try {
	  SwingUtilities.invokeLater(tellSwing);
	} catch (Exception e) {e.printStackTrace();}
      }
    };
    realWork.start();

  }


  /** Return currently selected sources. */
  public
  Vector
  getSelections()
  {
    Vector res = new Vector();
    for (int i=0; i<itsSources.size(); i++) {
      Boolean selstatus = (Boolean)itsSelections.get(i);
      if (selstatus.booleanValue()) {
	//Source is selected, need to add it to result
        res.add(itsSources.get(i));
      }
    }
    return res;
  }


  /** Specify which sources are currently selected. All the sources named
   * in the argument Vector will be selected, all other sources will be
   * deselected.
   * @param sel Vector containing the points to select. */
  public synchronized
  void
  setSelections(Vector sel)
  {
    //First, ensure all nominated sources are selected
    for (int i=0; i<sel.size(); i++) {
      int j = itsSources.indexOf(sel.get(i));
      if (j!=-1) {
        itsSelections.set(j, new Boolean(true));
      }
    }
    //Secondly, ensure all absent sources are deselected
    for (int i=0; i<itsSources.size(); i++) {
      int j = sel.indexOf(itsSources.get(i));
      if (j==-1) {
        //It's not in argument, so deselect it
        itsSelections.set(i, new Boolean(false));
      }
    }
    itsModel.fireTableStructureChanged();
  }
}
