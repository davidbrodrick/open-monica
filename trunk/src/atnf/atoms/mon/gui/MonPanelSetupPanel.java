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
//import atnf.atoms.mon.client.*;
import atnf.atoms.mon.*;
//import atnf.atoms.mon.util.*;

/**
 * Base class for all MonPanel configuration windows.
 *
 * @author David Brodrick
 * @version $Id: MonPanelSetupPanel.java,v 1.1 2006/05/12 03:13:48 bro764 Exp $
 * @see MonPanel
 */
public abstract class
MonPanelSetupPanel
extends JPanel
implements ActionListener
{
  /** The Frame we're displayed in. We need this reference for bringing
   * up dialog boxes. */
  protected JFrame itsFrame = null;

  /** The MonPanel instance we are configuring. */
  protected MonPanel itsMonPanel = null;

  /** The setup of the MonPanel before we started messing with it. This
   * copy will be used to "undo" and changes if the user hits the cancel
   * button. */
  protected SavedSetup itsInitialSetup = null;

  /** Records if we've altered the MonPanel's setup. If this is <tt>false</tt>
   * then we haven't touched the panel's setup, it should still be the same
   * as the intial setup. */
  protected boolean itsChangedSetup = false;

  /** C'tor, must be called by sub-classes.
   * @param monp The MonPanel we are going to configure. Must NOT be
   *             <tt>null</tt>.
   * @param frame The JFrame our panel will be displayed in. */
  protected
  MonPanelSetupPanel(MonPanel monp, JFrame frame)
  {
    super();
    itsMonPanel = monp;
    itsInitialSetup = itsMonPanel.getSetup();
    itsFrame = frame;

    setLayout(new BorderLayout());

    JPanel temppanel = new JPanel();
    temppanel.setLayout(new GridLayout(1,5));

    JButton tempbut = new JButton("Peek");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("Peek");
    temppanel.add(tempbut);

    /*tempbut = new JButton("Apply");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("Apply");
    temppanel.add(tempbut);*/

    tempbut = new JButton("Cancel");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("Cancel");
    temppanel.add(tempbut);

    tempbut = new JButton("OK");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("OK");
    temppanel.add(tempbut);

    add(temppanel, BorderLayout.SOUTH);
  }


  /** Called when buttons are pushed. Sub-classes should ensure that they
   * call this super method prior to performing their own handling. */
  public
  void
  actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();
    if (cmd == null) {
      return;
    }
    //If the command was one of ours, deal with it.
    if (cmd.equals("Peek")) {
      peekClicked();
    } else if (cmd.equals("Apply")) {
      applyClicked();
    } else if (cmd.equals("Cancel")) {
      cancelClicked();
    } else if (cmd.equals("OK")) {
      okClicked();
    }
  }


  /** Called when the <i>Peak</i> button is pressed. */
  protected
  void
  peekClicked()
  {
    SavedSetup setup = getSetup();
    if (setup==null) {
      //Bring up Dialog telling user we cannot extract a setup
      JOptionPane.showMessageDialog(itsFrame,
				    "Can't get Setup from GUI controls.\n\n" +
				    "You probably need to fully configure\n" +
				    "the GUI options before you can PEAK at\n" +
				    "this Setup\n",
				    "Setup Not Ready",
				    JOptionPane.WARNING_MESSAGE);
    } else {
      //Bring up dialog with setup info (pref in selectable text field)
      JOptionPane.showMessageDialog(itsFrame,
				    "Each Saved Setup is represented internally\n" +
				    "by a (somewhat complicated) string. The details\n" +
				    "of your new Setup follow:\n" +
				    "Class: " + setup.getClassName() + "\n" +
				    "Location: " + setup.getName() + "\n" +
				    "Setup: " + setup + "\n\n",
				    "Setup Description",
				    JOptionPane.PLAIN_MESSAGE);
    }
  }


  /** Called when the <i>Apply</i> button is pressed. */
  protected
  void
  applyClicked()
  {
    SavedSetup setup = getSetup();
    if (setup==null) {
      //Bring up Dialog telling user we cannot extract a setup
      JOptionPane.showMessageDialog(itsFrame,
				    "Can't get Setup from GUI controls.\n\n" +
				    "You probably need to fully configure\n" +
				    "the GUI options before you can APPLY\n" +
				    "this Setup.",
				    "Setup Not Ready",
				    JOptionPane.WARNING_MESSAGE);
    } else {
      //We need to try to apply the setup and then close our frame
      //If the setup cannot be used, the MonPanel will issue a dialog
      itsChangedSetup = true;
      if (itsMonPanel.loadSetup(setup)==false) {
	//There was an error. The MonPanel has probably already issued
	//a dialog to the user to explain the problem.
      } else {
        if (itsFrame instanceof MonFrame) {
          ((MonFrame)itsFrame).showDisplay();
        }
      }
    }
  }


  /** Called when the <i>OK</i> button is pressed. */
  protected
  void
  okClicked()
  {
    SavedSetup setup = getSetup();
    if (setup==null) {
      //Bring up Dialog telling user we cannot extract a setup
      JOptionPane.showMessageDialog(itsFrame,
				    "Can't get Setup from GUI controls.\n\n" +
				    "You probably need to fully configure\n" +
				    "the GUI options before you can use\n" +
				    "this Setup\n",
				    "Setup Not Ready",
				    JOptionPane.WARNING_MESSAGE);
    } else {
      //We need to try to apply the setup and then close our frame
      itsChangedSetup = false;
      if (itsMonPanel.loadSetup(setup)==false) {
	//There was an error. The MonPanel has probably already issued
	//a dialog to the user to explain the problem.
      } else {
	if (itsFrame instanceof MonFrame) {
    ((MonFrame)itsFrame).showDisplay();
  } else {
    itsFrame.setVisible(false);
  }
	//Now that the setup has been OKayed, we should save it
        itsInitialSetup = setup;
      }
      //this.setVisible(false);
    }
  }


  /** Called when the <i>Cancel</i> button is pressed. */
  protected
  void
  cancelClicked()
  {
    //Revert MonPanel to original setup, if we changed it's setup
    if (itsChangedSetup) {
      itsMonPanel.loadSetup(itsInitialSetup);
      showSetup(itsInitialSetup);
    }
  }


  /** Return the current setup, as determined by the GUI controls.
   * All sub-classes must implement this method. It provides the means of
   * extracting the setup specified by the user into a useable format.
   * @return SavedSetup specified by GUI controls, or <tt>null</tt> if
   *         no setup can be extracted from the GUI at present. */
  protected abstract
  SavedSetup
  getSetup();

  /** Configure the GUI to display the given setup.
   * @param setup The setup to display to the user. */
  protected abstract
  void
  showSetup(SavedSetup setup);
}
