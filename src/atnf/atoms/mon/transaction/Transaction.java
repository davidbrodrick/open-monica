// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import java.lang.reflect.*;
import atnf.atoms.mon.*;
import java.io.Serializable;

/**
 * Transaction objects are basically used to collect the raw information
 * for each monitor point. Most Transaction sub-classes store device-specific
 * "address" information for use by the corresponding <code>ExternalSystem</code>.
 * The ExternalSystem knows how to use this information to retrieve the
 * specific piece of information required for this monitor point. Other
 * Transaction sub-classes may collect information without use of a
 * ExternalSystem, for instance by listening for updates to other monitor points.
 *
 * @author Le Cuong Nguyen
 * @author David Brodrick
 * @version $Id: $
 **/
public abstract class
Transaction
extends MonitorPolicy
implements Serializable
{
  /** The Point this Transaction is associated with. */
  protected PointDescription itsParent = null;

  protected String itsName = null;
    
  /**
   * The channel is a string which indicates what transport/protocol
   * this transaction requires to realise the intended interation.
   * In the case of primitive points the channel will most probably
   * indicate what kind of network transport is required, this will
   * be used to ensure the transaction is given to the right kind of
   * Interactor. In the case of composite points the channel... is
   * largely unused???
   */
  protected String itsChannel = null;


  /** Override this. Use format fullname, shortname, argname, argtype, argname, argtype, ...
  */
  protected static String itsArgs[] = new String[]{"Transaction",""};


  protected
  Transaction(PointDescription parent, String specifics)
  {
    itsParent = parent;
  }


  /**
   * Return what transport is required to realise this transaction.
   * This will generally indicate what Interactor to use this Transaction
   * object with.
   */
  public String
  getChannel()
  {
    return itsChannel;
  }

  /**
   * Set the channel string. This should be set by the subclass constructor.
   */
  protected void
  setChannel(String channel)
  {
    itsChannel = channel;
  }
  
  public String getName()
  {
     return itsName;
  }


  /**
   * Create a new Transaction object by parsing all relevant information
   * from the argument string. We first look at what kind of transaction
   * channel the string indicates and then give the remainder
   * of the string to the constructor for the appropriate
   * Transaction subclass. The subclass will extract the remaining
   * information from the string, which will enable it to realise the
   * desired interaction in a subclass specific way. We also include a
   * reference to the Point that the Transaction is associated with incase
   * it is needed.
   */
  public static
  Transaction
  factory(PointDescription parent, String arg)
  {
    assert (arg!=null && arg.indexOf("-")!=-1) || arg.equalsIgnoreCase("null");

    if (arg.equalsIgnoreCase("null")) {
      arg = "-";
    }
    Transaction result = null;
    //Isolate the subclass specific data from the channel type
    //These should always be delimited by the first dash "-"
    String specifics   = arg.substring(arg.indexOf("-") + 1);

    //Look at the start of the argument string to determine the
    //type of transport channel. We use this to determine which
    //subclass to instantiate.
    String type = arg.substring(0, arg.indexOf("-"));
    if (type == "" || type == null || type.length() < 1) {
      type = "NONE";
    }
    
    try {
      Constructor Transaction_con;
      try {
        //Try to find class by assuming argument is full class name
        Transaction_con = Class.forName(type).getConstructor(new Class[]{PointDescription.class, String.class});
      } catch (Exception f) {
        //Supplied name was not a full path
        //Look in atnf.atoms.mon.translation package.
        Transaction_con = Class.forName("atnf.atoms.mon.transaction.Transaction"+type).getConstructor(new Class[]{PointDescription.class, String.class});
      }
      result = (Transaction)(Transaction_con.newInstance(new Object[]{parent, specifics}));
    } catch (Exception e) {
      System.err.println("ERROR in Transaction.factory for " + parent.getName());
      System.err.println("\tUNABLE TO FIND CONSTRUCTOR FOR CLASS " + type);
      System.err.println("\tSubstituting class NONE rather than crash...");
      result = new TransactionNONE(parent, "");
    }

    result.setStringEquiv(arg);

    return result;
  }

  public static String[] getArgs()
  {
     return itsArgs;
  }
}
