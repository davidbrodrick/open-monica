//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.client.MonitorClientCustom;
import atnf.atoms.mon.SavedSetup;

import java.awt.*;
import javax.swing.*;

/**
 * Frame which asks for the details required to upload a <i>SavedSetup</i> to
 * the monitor server. This augments the basic SaveSetupFrame in that a
 * username and password are required, and the SavedSetup is saved across
 * the network rather than to a local file.
 *
 * @author David Brodrick
 * @version $Id: SaveSetupServerFrame.java,v 1.3 2005/09/20 04:06:38 bro764 Exp $
 * @see SavedSetup
 * @see SaveSetupFrame
 */
public class
SaveSetupServerFrame
extends SaveSetupFrame
{
  /** Text field to hold the user name. */
  protected JTextField itsUser = new JTextField(25);

  /** Password entry field. */
  protected JPasswordField itsPass = new JPasswordField(25);

  public SaveSetupServerFrame(SavedSetup setup)
  {
    super(setup);

    itsDescription.setText("You can upload the current display setup information to the " +
			   "server, so that all users can view the same display " +
			   "simply by selecting the appropriate option from the display menu.\n" +
			   "ONLY SOME USERS HAVE PERMISSION TO DO THIS.\n\n" +
			   "You will need to enter a short descriptive title for the setup " +
			   "and also a hierarchical name in dot notation. You should CAREFULLY " +
			   "pick the hierarchical name so that it fits well with the options " +
			   "already available in the menus.\n\n" +
			   "You need to verify your identity by entering your username " +
                           "and password on the remote system.\n");
    itsMainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    JLabel templabel = new JLabel("Enter Username: eg, \"mee123\"");
    templabel.setToolTipText("Enter your username on the monitor server");
    itsMainPanel.add(templabel);
    itsMainPanel.add(itsUser);
    templabel = new JLabel("Enter Password, it will be hidden:");
    templabel.setToolTipText("Enter your password");
    itsMainPanel.add(templabel);
    itsMainPanel.add(itsPass);

    //pack();
    setSize(new Dimension(450,500));
    setVisible(true);
  }


  /** Save the setup to the appropriate destination. This class writes the
   * setup to the users local setup file, but sub-classes might take
   * alternate actions, such as uploading the setup to a server. */
  protected
  boolean
  doSave()
  {
    //Let's warn the user if they are attempting to create a new root level
    //entry in the heirarchy.
    String firstcomponent;
    int dotloc = itsSetup.getName().indexOf(".");
    if (dotloc==-1) {
      firstcomponent = itsSetup.getName();
    } else {
      firstcomponent = itsSetup.getName().substring(0,dotloc-1);
    }
    String[] setupnames = MonClientUtil.getSetupNames("atnf.atoms.mon.gui.MonFrame");
    boolean alreadyexists = false;
    for (int i=0; i<setupnames.length; i++) {
      if (setupnames[i].startsWith(firstcomponent)) {
        alreadyexists=true;
      }
    }
    if (!alreadyexists) {
      String[] options = {"Continue", "Cancel"};
      int n = JOptionPane.showOptionDialog(this,
					   "The location you nominated for the setup,\n" +
					   "ie, \"" + itsSetup.getName() + "\",\n" +
					   "would involve creating a new root-level entry in\n" +
					   "the hierarchical tree of setups (" + firstcomponent + ")\n" +
					   "If we get too many root-level entries, the hierarchy\n" +
					   "may become messy\n" +
					   "Are you sure you want to do this?",
					   "Really add root-level node?",
					   JOptionPane.YES_NO_OPTION,
					   JOptionPane.QUESTION_MESSAGE,
					   null,     //don't use a custom Icon
					   options,  //the titles of buttons
					   "Cancel"); //default button title
      if (n==1) {
        return false;
      }
    }
    try {
      if (itsUser.getText().equals("") || itsPass.getPassword().equals("")) {
	JOptionPane.showMessageDialog(this,
				      "You must enter a username and password",
				      "Insufficient Information",
				      JOptionPane.WARNING_MESSAGE);
        return false;
      }
      MonitorClientCustom serverconn = MonClientUtil.getServer();
      if (!serverconn.addSetup(itsSetup, itsUser.getText(),
			       new String(itsPass.getPassword()))) {
	return false;
      }
    } catch (Exception e) {
      System.err.println("SaveSetupServerFrame: doSave: An ERROR occurred:");
      System.err.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
