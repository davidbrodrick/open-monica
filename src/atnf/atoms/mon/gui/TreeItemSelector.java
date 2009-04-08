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

import atnf.atoms.mon.util.*;

/**
 * Show a tree of data and provide the user with an intuitive interface
 * for selecting (and deselecting) a set of items. The idea is that rather
 * than relying on delicate use of shift and control to select items directly
 * from the tree, we keep a table of which items the user has selected and
 * items can be inserted or removed from this table. This class can fire
 * <i>ChangeEvents</i> whenever our current selection changes.
 *
 * <P>This is an abstract class. Sub-classes must implement the
 * <i>buildTree</i> method which builds the tree of all available items.
 *
 * @author David Brodrick
 * @author Le Cuong Nguyen
 * @version $Id: TreeItemSelector.java,v 1.5 2005/07/27 01:08:31 bro764 Exp $
 * @see TreeUtil
 */
public abstract class
TreeItemSelector
extends JPanel
implements ActionListener
{
  /** The graphical Tree widget. */
  protected JTree itsTree = null;

  /** TreeUtil containing the items to display in the tree. */
  protected TreeUtil itsTreeUtil = null;

  /** The table to display current selection. */
  protected JTable itsTable = null;

  /** Keeps track of which points have currently been selected. */
  protected Vector itsSelected = new Vector();

  /** The Add button. We keep a reference so we can "press" the button
   * when data is added through some other mechanism. */
  protected JButton itsAddBut = new JButton("Add");

  /** The Remove button. We keep a reference so we can "press" the button
   * when data is removed through some other mechanism. */
  protected JButton itsRemoveBut = new JButton("Remove");

  /** Listeners for when our selection changes. */
  protected EventListenerList itsListeners = new EventListenerList();

  /** Panel the buttons are on. We keep this reference so that additional
   * buttons can be inserted by sub-classes if required. */
  protected JPanel itsButtonPanel = new JPanel();


  public TreeItemSelector()
  {
    this("Select Items");
  }


  public TreeItemSelector(String bordertitle)
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
    MouseListener ml = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
	int selRow = itsTree.getRowForLocation(e.getX(), e.getY());
	TreePath selPath = itsTree.getPathForLocation(e.getX(), e.getY());
	if(selRow != -1) {
	  if(e.getClickCount() == 2) {
      itsAddBut.doClick();
    }
	}
      }
    };
    itsTree.addMouseListener(ml);

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

    itsAddBut.addActionListener(this);
    itsAddBut.setActionCommand("Add");
    itsButtonPanel.add(itsAddBut);
    itsRemoveBut.addActionListener(this);
    itsRemoveBut.setActionCommand("Remove");
    itsButtonPanel.add(itsRemoveBut);
    temppan.add(itsButtonPanel, BorderLayout.SOUTH);
    add(temppan);
  }


  /** Recompute the contents of the selected item table. */
  protected
  void
  recomputeTable()
  {
    final String[] cnames = {"Selections"};

    if (itsTable==null) {
      //Create the table if it doesn't exist yet
      Object[][] values = new Object[0][1];
      itsTable = new JTable(values, cnames);
      //itsTable.setPreferredSize(new Dimension(120, 180));
      //Create mouse listener to remove items when they are double clicked
      itsTable.addMouseListener(new MouseAdapter()
      {
	public void mouseClicked(MouseEvent e) {
	  if (e.getClickCount() == 2) {
	    Point p = e.getPoint();
	    int row = itsTable.rowAtPoint(p);
	    if (row<0) {
        return;
      }
	    itsSelected.remove(row);
	    recomputeTable();
	    itsRemoveBut.doClick();
	  }
	}
      });
    }
    //Convert data
    Object[][] values = new Object[itsSelected.size()][1];
    for (int i=0; i<itsSelected.size(); i++) {
      values[i][0] = itsSelected.get(i);
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
	return getValueAt(0, c).getClass();
      }
    };
    itsTable.setModel(new MyTableModel());
    fireChangeEvent(new ChangeEvent(this));
  }


  /* Called when a button is pressed or tree path is altered. */
  public synchronized
  void
  actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();

    if (cmd.equals("Add")) {
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
	itsSelected.add(username);
      }
      recomputeTable();
      itsTree.getSelectionModel().clearSelection();
    } else if (cmd.equals("Remove")) {
      ListSelectionModel sm = itsTable.getSelectionModel();
      if (sm.isSelectionEmpty()) {
        return;
      }
      int i=0; int j=0;
      while (i<itsSelected.size()) {
	if (sm.isSelectedIndex(j)) {
	  itsSelected.remove(i);
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


  /** Build <i>itsTreeUtil</i>, containing the items which can be selected. */
  protected abstract
  void
  buildTree();


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


  /** Get the currently selected items.
   * @return Vector containing String names of all selected items. */
  public synchronized
  Vector
  getSelections()
  {
    return (Vector)itsSelected.clone();
  }


  /** Set the currently selected items.
   * @param v Vector containing String names of items to be selected. */
  public synchronized
  void
  setSelections(Vector v)
  {
    if (v==null || v.size()==0) {
      itsSelected.clear();
    } else {
      itsSelected = (Vector)v.clone();
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
