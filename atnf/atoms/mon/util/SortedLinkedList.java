//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.util;

import java.util.*;
import java.lang.*;

/** Simple linked list class which uses a Comparator to sort the nodes.
 * Most operations are completed in linear time in the worst case. We
 * keep track of the current size so that the <i>size</i> method runs
 * in constant time.
 *
 * @author David Brodrick
 * @version $Id: SortedLinkedList.java,v 1.1 2004/11/25 00:07:39 bro764 Exp $
 */
public class
SortedLinkedList
{
  /** Trivial class for each list node. */
  private class ListNode {
    /** The data stored at this node. */
    Object data = null;
    /** Pointer to the next node. */
    ListNode next = null;

    ListNode(Object o) {
      data = o;
    }
    ListNode(Object o, ListNode n) {
      data = o;
      next = n;
    }
  }

  /** Comparator to use. */
  protected Comparator itsComparator = null;

  /** Head of the list. */
  protected ListNode itsHead = null;

  /** Tail of the list. */
  protected ListNode itsTail = null;

  /** Record the current number of nodes in the list. */
  protected int itsSize = 0;


  /** Constructor.
   * @param c The Comparator to use to compare elements. */
  public SortedLinkedList(Comparator c)
  {
    itsComparator = c;
  }


  /** Add the Object to the set. */
  public synchronized
  void
  add(Object o)
  {
    if (o==null) return;

    ListNode newnode = new ListNode(o);
    if (itsHead==null) {
      //The list was empty
      itsHead = itsTail = new ListNode(o);
    } else {
      //Check if it needs to go right at the head
      if (itsComparator.compare(o, itsHead.data)<=0) {
	newnode.next = itsHead;
        itsHead = newnode;
      }
      //Check if it needs to go right at the tail
      //This is an unusual check but should boost performance
      else if (itsComparator.compare(o, itsTail.data)>=0) {
	itsTail.next = newnode;
	itsTail = newnode;
      }
      //It needs to be inserted into the middle of the list
      else {
	ListNode next = itsHead.next;
	ListNode prev = itsHead;
	while (itsComparator.compare(o, next.data)>0) {
	  prev = next;
	  next = next.next;
	}
	//Do the actual insertion
	prev.next = newnode;
        newnode.next = next;
      }
    }
    itsSize++;
  }


  /** Remove all instances of the object from the set. */
  public synchronized
  void
  remove(Object o)
  {
    ListNode next = itsHead;
    ListNode prev = null;
    while (next!=null) {
      if (next.data==o) {
	//Need to remove this node
        itsSize--;
	if (prev!=null) {
	  //It's not the head
          prev.next = next.next;
	} else {
	  //It was the head
	  itsHead = next.next;
	}
	//Check if it's the tail
	if (next==itsTail) {
	  itsTail = prev;
	}
      }
      prev = next;
      next = next.next;
    }
  }


  /** Return a reference to the first node but do not remove it */
  public synchronized
  Object
  first()
  {
    if (itsHead==null) return null;
    else return itsHead.data;
  }


  /** Return all elements less than the argument and remove them from the
   * list. */
  public synchronized
  Vector
  headSet(Object o)
  {
    Vector res = new Vector();

    ListNode next = itsHead;
    ListNode prev = null;

    //Keep adding the head until it no longer matches
    while (itsHead!=null && itsComparator.compare(itsHead.data, o)<0) {
      res.add(itsHead.data);
      itsSize--;
      if (itsHead==itsTail) {
	//the list has been emptied
	itsHead = itsTail = null;
	break;
      }
      itsHead = itsHead.next;
    }
    return res;
  }


  /** Check if the list is empty.
   * @return <code>True</code> if the list is empty. */
  public synchronized
  boolean
  isEmpty()
  {
    if (itsHead==null) return true;
    assert itsSize>0;
    return false;
  }


  /** Get a string representation of the list. */
  public synchronized
  String
  toString()
  {
    String res = "[ ";
    ListNode next = itsHead;
    while (next!=null) {
      res = res + next.data.toString() + " ";
      next = next.next;
    }
    res = res + " ]";
    return res;
  }


  /** Return the number of nodes currently in the list. */
  public
  int
  size()
  {
    return itsSize;
  }
}
