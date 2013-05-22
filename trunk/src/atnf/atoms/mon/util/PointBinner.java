// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
package atnf.atoms.mon.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import atnf.atoms.mon.PointDescription;

/**
 * <p>
 * PointBinner class used for sorting and "binning" points into a set hierarchy for more efficient
 * retrieval. When seeking points of a certain category, it uses a tree-like structure to retrieve
 * a list of points under that category, more efficient than iterating over every point in the
 * system. Should perform in approximately log(n) time in general case, with linear time in absolute
 * worst case (all points follow exactly the same naming structure bar last segment)</p>
 * <p>
 * Named "PointBinner" for categorising things into "bins", rather than putting things into the rubbish.</p>
 * @author Kalinga Hulugalle
 */
public class PointBinner extends Thread{
	/** Array of names of the strings to "bin" in dot-delimited format.*/
	String[] points;
	
	static boolean pointsBinned = false;
	/** The internal PointHierarchy used to store and retrieve the point names when queried*/
	static PointHierarchy pointTree;

	/**
	 * Creates a new PointBinner with the specified array of Strings.
	 * @param pts The array of Strings to use as the data source for this PointBinner
	 */
	public PointBinner(String[] pts){
		points = pts;
	}
	
	/**
	 * Creates a new PointBinner with the internal array of point names as null. Relies on the
	 * main Thread method to populate the array using the {@link PointDescription#getAllUniqueNames()}
	 * method once they have been created.
	 */
	public PointBinner(){
		points = null;
	}

	public void run(){
		if (points == null || points.length == 0){
			while (!PointDescription.getPointsCreated()){
				try {
				Thread.sleep(100);
				} catch (Exception e){}
			}
			points = PointDescription.getAllUniqueNames();
		}
		pointTree = new PointHierarchy();
		for (String point : points){
			pointTree.addLeaf(point);
		}
		pointsBinned = true;
	}
	
	/**
	Returns all the "children" of a given point pattern, with full point names
	 * @param pattern The pattern that all returned children should match
	 * @return A Vector&lt;String&gt; containing the full point names of all points that match
	 * the given pattern 
	 * @throws NullPointerException if the internal PointHierarchy hasn't been instantiated yet
	 */
	public static Vector<String> getAllChildren(String pattern) throws NullPointerException{
		return pointTree.getAllChildren(pattern);
	}
	
	/**
	 * Returns the direct children of the given point pattern. That is, only the "nub" extension of
	 * that point pattern will be returned.
	 * @param pattern The pattern that the returned nubs should match
	 * @return A Vector&lt;String&gt; containing the nub segments of the next set of points that
	 * match the given pattern.
	 * @throws NullPointerException if the internal PointHierarchy hasn't been instantiated yet
	 */
	public static Vector<String> getDirectChildren(String pattern) throws NullPointerException{
		return pointTree.getDirectChildren(pattern);
	}
	
	/**
	 * Returns a boolean indicating whether the PointBinner has finished binning all the points yet.
	 * @return A boolean indicating whether this has finished binning its points yet
	 */
	public static boolean getPointsBinnedStatus(){
		return pointsBinned;
	}

	/**
	 * PointHierarchy is a recursively structured class that stores dot-delimited point names in 
	 * a tree-like format, where each node on the tree is a "nub" segment in a dot-delimited 
	 * point name. I.e,  point.name.one would create three linked PointHierarchies, with each one
	 * having the "node" value being point, name and one respectively. 
	 * @author Kalinga Hulugalle
	 *
	 */
	public static class PointHierarchy{
		/** The value of this PointHierarchy. When chained with multiple instances, can be used to
		 * recreate a point name*/
		String node;
		/** A mapping of "nubs" to PointHierarchies. This forms the "branches" of the tree*/
		HashMap<String, PointHierarchy> branches = new HashMap<String, PointHierarchy>();

		/**
		 * Constructs a root PointHierarchy, with the node value "root"
		 */
		public PointHierarchy(){
			node = "root";
		}

		/**
		 * Private constructor, used internally for creating anonymous instances in the branching
		 * tree structure
		 * @param name The node value for this PointHierarchy
		 */
		private PointHierarchy(String name){
			node = name;
		}

		/**
		 * Adds a lead to the root PointHierarchy, which breaks the pointname apart and puts them
		 * into the allotted lower PointHierarchies
		 * @param name The dot-delimited point name to add
		 */
		public void addLeaf(String name){
			StringTokenizer st = new StringTokenizer(name, ".");
			if (st.countTokens() == 1){
				String nextToken = st.nextToken();
				this.branches.put(nextToken, new PointHierarchy(nextToken));
				//				System.out.println("Creating leaf " + nextToken);
			} else if (st.countTokens() > 1){
				String nextToken = st.nextToken();
				if (!branches.containsKey(nextToken)){
					branches.put(nextToken, new PointHierarchy(nextToken));	
					//					System.out.println("Creating branch " + nextToken);
				}
				PointHierarchy mt = branches.get(nextToken);
				mt.addLeaf(name.substring(name.indexOf(".")+1));
			}
		}

		/**
		 * Returns all the "children" of a given point pattern, with full point names. Performs in 
		 * a DFS manner.
		 * @param pattern The pattern that all returned children should match
		 * @return A Vector&lt;String&gt; containing the full point names of all points that match
		 * the given pattern 
		 */
		public Vector<String> getAllChildren(String pointPattern){
			Vector<String> res = new Vector<String>();
			StringTokenizer st = new StringTokenizer(pointPattern, ".");
			PointHierarchy curr = this;
			String prefix = "";
			if (st.countTokens() > 0){
				try {
					while (st.hasMoreElements()){
						if (st.countTokens() == 1){
							String nub = st.nextToken();
							PointHierarchy nubTree = curr.getBranches().get(nub);
							res = nubTree.expandHierarchy(prefix);
						} else {
							String nub = st.nextToken();
							prefix += nub + ".";
							curr = curr.getBranches().get(nub);
						}
					}
				} catch (NullPointerException npe){
					System.err.println("Point category does not exist");
				}
			} else {
				for (PointHierarchy ph : this.getChildren()){
					res.addAll(ph.expandHierarchy(prefix));
				}
			}
			return res;
		}
		
		/**
		 * Returns the direct children of the given point pattern. That is, only the "nub" extension of
		 * that point pattern will be returned.
		 * @param pattern The pattern that the returned nubs should match
		 * @return A Vector&lt;String&gt; containing the nub segments of the next set of points that
		 * match the given pattern.
		 */
		public Vector<String> getDirectChildren(String pointPattern){
			Vector<String> res = new Vector<String>();
			StringTokenizer st = new StringTokenizer(pointPattern, ".");
			PointHierarchy curr = this;
			String prefix = "";
			if (st.countTokens() > 0){
				try {
					while (st.hasMoreElements()){
						if (st.countTokens() == 1){
							String nub = st.nextToken();
							PointHierarchy nubTree = curr.getBranches().get(nub);
							res.addAll(nubTree.getBranches().keySet());
						} else {
							String nub = st.nextToken();
							prefix += nub + ".";
							curr = curr.getBranches().get(nub);
						}
					}
				} catch (NullPointerException npe){
					System.err.println("Point category does not exist");
				}
			} else {
					res.addAll(curr.getBranches().keySet());
			}
			return res;			
		}

		/**
		 * Returns the node value for this PointHierarchy
		 * @return
		 */
		private String getNode(){
			return node;
		}

		/**
		 * Returns the HashMap of the lower PointHierarchies that this links to
		 * @return The HashMap of linked PointHierarchies
		 */
		private HashMap<String, PointHierarchy> getBranches(){
			return branches;
		}

		/**
		 * Returns an iterable Collection of PointHierarchies that this links to
		 * @return The Collection of linked PointHierarchies
		 */
		private Collection<PointHierarchy> getChildren(){
			return branches.values();
		}

		/**
		 * Determines if this PointHierarchy is an ultimate "leaf" node without children
		 * @return A boolean indicating whether this PointHierarchy links to any others
		 */
		public boolean hasChildren(){
			return !branches.isEmpty();
		}

		/**
		 * Recursive expansion algorithm for retrieving the full names of all point that match
		 * a certain prefix. Performs in a DFS (Depth-First-Search) manner
		 * @param prefix The prefix pattern that all children must match
		 * @return A Vector&lt;String&gt; that contains the full point names of all points
		 * in this and lower PointHierarchies that match the given prefix
		 */
		private Vector<String> expandHierarchy(String prefix){
			Vector<String> res = new Vector<String>();
			if (!this.hasChildren()){
				res.add(prefix + this.getNode());
			} else {
				for (PointHierarchy child : this.getChildren()){
					res.addAll(child.expandHierarchy(prefix + getNode() + "."));
				}
			}
			return res;
		}
	}

	//Test method
	public static void main(String[] args){
		PointHierarchy mt = new PointHierarchy();
		mt.addLeaf("a.b.c.d");
		mt.addLeaf("a.b.c.e");
		mt.addLeaf("a.c.b.d");
		mt.addLeaf("ca01.cabb.corr23");

		Vector<String> res = mt.getDirectChildren("a.b.c");
		for (String r : res){
			System.out.println(r);
		}
		String[] points = {"a.b.c.d", "a.b.c.e", "a.c.b.d", "ca01.cabb.corr23"};
		new PointBinner(points).start();
		while (!PointBinner.getPointsBinnedStatus()){}
		Vector<String> res1 = PointBinner.getAllChildren("");
		for (String s : res1){
			System.out.println(s);
		}
	}

}
