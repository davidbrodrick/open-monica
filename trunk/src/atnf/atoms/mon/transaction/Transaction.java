// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import atnf.atoms.mon.*;

/**
 * Transaction objects store the information required to inform MoniCA how to get or set a
 * point's value, eg. what device to talk to and what address information to use within
 * that device.
 * 
 * <P>
 * The Channel field is used to locate the appropriate ExternalSystem class used to
 * interface with the end-point. The ExternalSystem knows how to use any other fields
 * present in the Transaction to locate the appropriate specific datum (eg. hardware
 * register).
 * 
 * @author Le Cuong Nguyen
 * @author David Brodrick
 */
public abstract class Transaction
{
  /** The Point this Transaction is associated with. */
  protected PointDescription itsParent = null;

  /**
   * The Channel definition used for finding the right ExternalSystem instance, where
   * appropriate.
   */
  protected String itsChannel = null;

  protected Transaction(PointDescription parent, String[] args)
  {
    itsParent = parent;
  }

  /**
   * Return what transport is required to realise this transaction. This will generally
   * indicate what Interactor to use this Transaction object with.
   */
  public String getChannel()
  {
    return itsChannel;
  }

  /**
   * Set the channel string. This should be set by the subclass constructor.
   */
  protected void setChannel(String channel)
  {
    itsChannel = channel;
  }
}
