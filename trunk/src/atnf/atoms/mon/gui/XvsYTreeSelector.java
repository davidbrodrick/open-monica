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
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Vector;

import atnf.atoms.mon.client.*;
import atnf.atoms.mon.util.*;

/**
 * Let the user select points from a tree and assign them into two
 * different categories, X and Y. This was developed for the setup panel
 * for the ATXYPlot MonPanel.
 *
 * @author David Brodrick
 * @version $Id: $
 * @see TreeUtil
 */
public class
XvsYTreeSelector
extends JPanel
implements ActionListener
{
  /** The graphical Tree widget. */
  protected JTree itsTree = null;

  /** TreeUtil containing the items to display in the tree. */
  protected TreeUtil itsTreeUtil = null;

  /** The table to display current selection. */
  protected JTable itsTable = null;

  /** Keeps track of which X points are selected. */
  protected Vector itsXSelected = new Vector();
  /** Keeps track of which Y points are selected. */
  protected Vector itsYSelected = new Vector();

  /** Add the selected point to the X vector */
  protected JButton itsXAddBut = new JButton("Add X");
  /** Add the selected point to the Y vector */
  protected JButton itsYAddBut = new JButton("Add Y");
  /** Remove the selected point from the X vector */
  protected JButton itsXRemoveBut = new JButton("Remove X");
  /** Remove the selected point from the Y vector */
  protected JButton itsYRemoveBut = new JButton("Remove Y");

  /** Listeners for when our selection changes. */
  protected EventListenerList itsListeners = new EventListenerList();

  /** Panel the buttons are on. We keep this reference so that additional
   * buttons can be inserted by sub-classes if required. */
  protected JPanel itsButtonPanel = new JPanel();

  public XvsYTreeSelector()
  {
    this("Select Items");
  }


  public XvsYTreeSelector(String bordertitle)
  {
    super();

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    TitledBorder title = BorderFactory.createTitledBorder(bordertitle);
    setBorder(title);

    //Build the data tree
    buildTree();
    itsTreeUtil.addActionListener(this);
    itsTree = itsTreeUtil.getTree();
    //itsTree.setRootVisible(false);
    itsTree.getSelectionModel().setSelectionMode(
     			 TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    //Create a mouse listener to add leaf items when they are double clicked
    /*MouseListener ml = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
	int selRow = itsTree.getRowForLocation(e.getX(), e.getY());
	TreePath selPath = itsTree.getPathForLocation(e.getX(), e.getY());
	if(selRow != -1) {
	  if(e.getClickCount() == 2) itsAddBut.doClick();
	}
      }
    };
    itsTree.addMouseListener(ml);*/

    JScrollPane sp = new JScrollPane(itsTree);
    sp.setPreferredSize(new Dimension(170, 200));
    sp.setMinimumSize(new Dimension(140, 100));
    add(sp);

    //Create and configure the table
    recomputeTable();

    sp = new JScrollPane(itsTable);
    sp.setPreferredSize(new Dimension(200, 180));
    sp.setMinimumSize(new Dimension(80, 100));
    JPanel temppan = new JPanel();
    temppan.setLayout(new BorderLayout());
    temppan.add(sp, BorderLayout.CENTER);

    JPanel temppan2 = new JPanel();
    temppan2.setBorder(BorderFactory.createLineBorder(Color.black));
    itsXAddBut.addActionListener(this);
    itsXAddBut.setActionCommand("AddX");
    temppan2.add(itsXAddBut);
    itsXRemoveBut.addActionListener(this);
    itsXRemoveBut.setActionCommand("RemoveX");
    temppan2.add(itsXRemoveBut);
    itsButtonPanel.add(temppan2);

    temppan2 = new JPanel();
    temppan2.setBorder(BorderFactory.createLineBorder(Color.black));
    itsYAddBut.addActionListener(this);
    itsYAddBut.setActionCommand("AddY");
    temppan2.add(itsYAddBut);
    itsYRemoveBut.addActionListener(this);
    itsYRemoveBut.setActionCommand("RemoveY");
    temppan2.add(itsYRemoveBut);
    itsButtonPanel.add(temppan2);
    temppan.add(itsButtonPanel, BorderLayout.SOUTH);
    add(temppan);
  }


  /** Recompute the contents of the selected item table. */
  protected
  void
  recomputeTable()
  {
    final String[] cnames = {"X", "vs", "Y"};

    if (itsTable==null) {
      //Create the table if it doesn't exist yet
      Object[][] values = new Object[0][3];
      itsTable = new JTable(values, cnames);

      //itsTable.setPreferredSize(new Dimension(120, 180));
      //Create mouse listener to remove items when they are double clicked
/*      itsTable.addMouseListener(new MouseAdapter()
      {
	public void mouseClicked(MouseEvent e) {
	  if (e.getClickCount() == 2) {
	    Point p = e.getPoint();
	    int row = itsTable.rowAtPoint(p);
	    if (row<0) return;
	    itsSelected.remove(row);
	    recomputeTable();
	    itsRemoveBut.doClick();
	  }
	}
      });*/
    }

    //Convert data
    int maxlen = 0;
    if (itsXSelected.size()>itsYSelected.size()) {
      maxlen = itsXSelected.size();
    } else {
      maxlen = itsYSelected.size();
    }

    Object[][] values = new Object[maxlen][3];
    for (int i=0; i<itsXSelected.size(); i++) {
      values[i][0] = itsXSelected.get(i);
    }
    for (int i=0; i<itsYSelected.size(); i++) {
      values[i][2] = itsYSelected.get(i);
    }
    for (int i=0; i<maxlen; i++) {
      values[i][1] = "vs";
    }

    //create and use a new model of the table data
    final Object[][] values2 = values;
    class MyTableModel extends AbstractTableModel {
      final String[] columnNames = cnames;
      final Object[][] data = values2;
      public int getColumnCount() { return columnNames.length; }
      public int getRowCount() { return data.length; }
      public String getColumnName(int col) { return columnNames[col]; }
      public Object getValueAt(int row, int col) { return data[row][col]; }
      public Class getColumnClass(int c) {
	return String.class;//getValueAt(0, c).getClass();
      }
    };
    itsTable.setModel(new MyTableModel());
    TableColumn column = itsTable.getColumnModel().getColumn(1);
    column.setPreferredWidth(30);
    column.setMaxWidth(30);
    column.setMinWidth(17);
    column = itsTable.getColumnModel().getColumn(0);
    column.setPreferredWidth(100);
    column = itsTable.getColumnModel().getColumn(2);
    column.setPreferredWidth(100);
    fireChangeEvent(new ChangeEvent(this));
  }


  /* Called when a button is pressed or tree path is altered. */
  public synchronized
  void
  actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();

    if (cmd.equals("AddX")) {
      TreePath[] selection = itsTree.getSelectionPaths();
      if (selection==null || selection.length==0) {
        return;
      }
      for (int i=0; i<selection.length; i++) {
	Object[] path = selection[i].getPath();

	if (path==null || path.length<2 || !((DefaultMutableTreeNode)(selection[i].getLastPathComponent())).isLeaf()) {
    continue;
  }
        String thispath = "";
	for (int j=1; j<path.length; j++) {
	  thispath += path[j];
	  if (j!=path.length-1) {
      thispath+=".";
    }
	}
        String username = (String)itsTreeUtil.getNodeObject(thispath);
	itsXSelected.add(username);
      }
      recomputeTable();
      itsTree.getSelectionModel().clearSelection();
    } else if (cmd.equals("RemoveX")) {
      ListSelectionModel sm = itsTable.getSelectionModel();
      if (sm.isSelectionEmpty()) {
        return;
      }
      int i=0; int j=0;
      while (i<itsXSelected.size()) {
	if (sm.isSelectedIndex(j)) {
	  itsXSelected.remove(i);
	  i--;
	}
	i++;
        j++;
      }
      recomputeTable();
    } else if (cmd.equals("AddY")) {
      TreePath[] selection = itsTree.getSelectionPaths();
      if (selection==null || selection.length==0) {
        return;
      }
      for (int i=0; i<selection.length; i++) {
	Object[] path = selection[i].getPath();

	if (path==null || path.length<2 || !((DefaultMutableTreeNode)(selection[i].getLastPathComponent())).isLeaf()) {
    continue;
  }
        String thispath = "";
	for (int j=1; j<path.length; j++) {
	  thispath += path[j];
	  if (j!=path.length-1) {
      thispath+=".";
    }
	}
        String username = (String)itsTreeUtil.getNodeObject(thispath);
	itsYSelected.add(username);
      }
      recomputeTable();
      itsTree.getSelectionModel().clearSelection();
    } else if (cmd.equals("RemoveY")) {
      ListSelectionModel sm = itsTable.getSelectionModel();
      if (sm.isSelectionEmpty()) {
        return;
      }
      int i=0; int j=0;
      while (i<itsYSelected.size()) {
	if (sm.isSelectedIndex(j)) {
	  itsYSelected.remove(i);
	  i--;
	}
	i++;
        j++;
      }
      recomputeTable();
    } else if (cmd != null && cmd.length() > 1) {
      TreePath path = itsTreeUtil.makeTreePath(cmd);
      itsTree.setSelectionPath(path);
    }
  }


  /**
   * Build <i>itsTreeUtil</i>, containing the names and sources for all monitor
   * points. The nodes have the source name as the last field in the path but
   * the user objects associated with the nodes in the TreeUtil have the
   * conventional name in <i>source.name</i> format.
   */
  protected void buildTree() {
    itsTreeUtil = new TreeUtil("Points");
    String[] points = MonClientUtil.getAllPointNames();
    if (points != null && points.length > 0) {
      for (int i = 0; i < points.length; i++) {
        int firstdot = points[i].indexOf(".");
        String source = points[i].substring(0, firstdot);
        String point = points[i].substring(firstdot + 1);
        String newname = point + "." + source;
        if (!newname.startsWith("hidden")) {      
          itsTreeUtil.addNode(newname, points[i]);
        }        
      }
    }
  }


  /** Add a listener to be notified whenever our selection changes.
   * @param The ChangeListener to add to our list. */
  public
  void
  addChangeListener(ChangeListener listener)
  {
    itsListeners.add(ChangeListener.class, listener);
  }


  /** Remove the specified change listener.
   * @param listener The ChangeListener to remove from our list. */
  public
  void
  removeChangeListener(ChangeListener listener)
  {
    itsListeners.remove(ChangeListener.class, listener);
  }


  /** Fire event to notify all listeners that our selection has changed.
   * @param ce The event to fire to all ChangeListeners. */
  protected
  void
  fireChangeEvent(ChangeEvent ce)
  {
    Object[] listeners = itsListeners.getListenerList();
    for (int i = 0; i < listeners.length; i +=2) {
      if (listeners[i] == ChangeListener.class) {
        ((ChangeListener)listeners[i+1]).stateChanged(ce);
      }
    }
  }


  /** Get the currently selected X items.
   * @return Vector containing String names of selected X items. */
  public synchronized
  Vector
  getXSelections()
  {
    return (Vector)itsXSelected.clone();
  }

  /** Get the currently selected Y items.
   * @return Vector containing String names of selected Y items. */
  public synchronized
  Vector
  getYSelections()
  {
    return (Vector)itsYSelected.clone();
  }


  /** Set the currently selected X items.
   * @param v Vector containing String names of items to be selected. */
  public synchronized
  void
  setXSelections(Vector v)
  {
    if (v==null || v.size()==0) {
      itsXSelected.clear();
    } else {
      itsXSelected = (Vector)v.clone();
    }
    recomputeTable();
    fireChangeEvent(new ChangeEvent(this));
  }

  /** Set the currently selected Y items.
   * @param v Vector containing String names of items to be selected. */
  public synchronized
  void
  setYSelections(Vector v)
  {
    if (v==null || v.size()==0) {
      itsYSelected.clear();
    } else {
      itsYSelected = (Vector)v.clone();
    }
    recomputeTable();
    fireChangeEvent(new ChangeEvent(this));
  }


  /** Add the specified component to our button panel.
   * @param j The component to add. */
  public synchronized
  void
  addButton(JComponent j)
  {
    itsButtonPanel.add(j);
  }
}
