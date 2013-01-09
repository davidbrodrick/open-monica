//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.tree.TreeSelectionModel;

import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.util.TreeUtil;

/**
 * Show a tree of data and provide the user with an intuitive interface for
 * selecting (and deselecting) a set of items or a single item in the tree.
 * 
 * 
 * @author Kalinga Hulugalle
 * @see TreeUtil
 */
public class SimpleTreeSelector extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9040656298636157658L;

	/** The graphical Tree widget. */
	protected JTree itsTree = null;

	/** TreeUtil containing the items to display in the tree. */
	protected TreeUtil itsTreeUtil = null;

	/** Cache of the full point/source names for each point. */
	private static String[] theirPointNames = null;
	/** cache of the node names for each of the points. */
	private static String[] theirNodeNames = null;

	/** Keeps track of which points have currently been selected. */
	protected Vector<Object> itsSelected = new Vector<Object>();

	/** Listeners for when our selection changes. */
	protected EventListenerList itsListeners = new EventListenerList();

	public SimpleTreeSelector() {
		this("Select Items");
	}

	public SimpleTreeSelector(String bordertitle) {
		super();

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		TitledBorder title = BorderFactory.createTitledBorder(bordertitle);
		setBorder(title);

		// Build the data tree
		buildTree();
		itsTree = itsTreeUtil.getTree();
		this.setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		JScrollPane sp = new JScrollPane(itsTree);
		sp.setPreferredSize(new Dimension(170, 200));
		sp.setMinimumSize(new Dimension(140, 100));
		add(sp);
		sp.setPreferredSize(new Dimension(200, 180));
		sp.setMinimumSize(new Dimension(80, 100));
		JPanel temppan = new JPanel();
		temppan.setLayout(new BorderLayout());
		temppan.add(sp, BorderLayout.CENTER);

		add(temppan);
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

	/**
	 * Change the selection mode of this TreeItemSelector. As in TreeSelectionModel, 
	 * the different types of selection modes are SINGLE_TREE_SELECTION, 
	 * CONTIGUOUS_TREE_SELECTION and DISCONTIGUOUS_TREE_SELECTION. 
	 * @param mode the int mask for the mode to change it to
	 * @see TreeSelectionModel
	 */
	public void setSelectionMode(int mode){
		itsTree.getSelectionModel().setSelectionMode(mode);
	}
	
	public Object getSelection(){
		//TODO still, not too sure if this will work
		return itsTree.getSelectionModel().getSelectionPath();
	}

	/**
	 * Add a listener to be notified whenever our selection changes.
	 * @param The ChangeListener to add to our list.
	 */
	public void addChangeListener(ChangeListener listener) {
		itsListeners.add(ChangeListener.class, listener);
	}

	/**
	 * Remove the specified change listener.
	 * @param listener The ChangeListener to remove from our list.
	 */
	public void removeChangeListener(ChangeListener listener) {
		itsListeners.remove(ChangeListener.class, listener);
	}

	/**
	 * Fire event to notify all listeners that our selection has changed.
	 * @param ce The event to fire to all ChangeListeners.
	 */
	protected void fireChangeEvent(ChangeEvent ce) {
		Object[] listeners = itsListeners.getListenerList();
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ChangeListener.class) {
				((ChangeListener) listeners[i + 1]).stateChanged(ce);
			}
		}
	}

	/**
	 * Get the currently selected items.
	 * @return Vector containing String names of all selected items.
	 */
	@SuppressWarnings("unchecked")
	public synchronized Vector<Object> getSelections() {
		return (Vector<Object>) itsSelected.clone();
	}

	/**
	 * Set the currently selected items.
	 * @param v Vector containing String names of items to be selected.
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setSelections(Vector<Object> v) {
		if (v == null || v.size() == 0) {
			itsSelected.clear();
		} else {
			itsSelected = (Vector<Object>) v.clone();
		}
		fireChangeEvent(new ChangeEvent(this));
	}
}
