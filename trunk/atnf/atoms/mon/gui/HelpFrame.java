//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Show a window with a brief introduction on how to use the software.
 *
 * @author David Brodrick
 * @version $Id: HelpFrame.java,v 1.1 2005/07/25 05:34:05 bro764 Exp $
 */
public class
HelpFrame
extends JFrame
implements ActionListener
{
  /** The actual help string as HTML fragment. This is what you will need
   * to edit in order to change the Help message. */
  protected String itsHelpText =
    "<h2>Basic use:</h2>" +
    "This program connects to the Observatory monitor point server and " +
    "enables you to explore the current and archival values of " +
    "different monitor points available from around the site and " +
    "antennas, for instance receiver cryogenics temperatures or data from " +
    "the site weather station. " +
    "<P>Use the <i>Navigator</i> menu to select from pre-prepared displays " +
    "of monitor point info. If desired, you can open multiple windows " +
    "using the <i>Window</i> menu. Advanced users can create new " +
    "displays that combine whatever monitor information is required, as " +
    "described below." +
    "<P>" +
    "<h2>Advanced use:</h2>" +
    "The model used in this software is that the server maintains a large " +
    "database of all monitor point values, and the client features different " +
    "display widgets, called <i>Panels</i>, that you can use to visualise " +
    "the available data. Example Panels include graphs of monitor " +
    "point values vs. time, or tables that show current monitor point " +
    "values (you can see the list of available Panel types in the " +
    "<i>Setup :: Add Panel</i> menu). By combining different kinds of " +
    "Panels and configuring them to display different monitor points, " +
    "you have a lot of flexibility in how the available monitor data can be viewed. " +
    "<P>" +
    "In order to define a new display setup, you should follow " +
    "these steps: " +
    "<ol>" +
    "<li>Clear the current display by clicking on the <i>Setup</i> menu " +
    "and then selecting <i>Clear</i>." +
    "<li>Add whatever display panels you want your new display to include " +
    "by selecting them one at a time, from the <i>Setup :: Add Panel</i> " +
    "menu. For instance if you want a graph with a table beneath it then " +
    "first select <i>Time Series</i> and then <i>Point Table</i>.</li>" +
    "<li>Each time you add a panel to the display an extra tab will " +
    "appear in the main window. You can use these tabs to configure " +
    "precisely what you would like each Panel to display.</li>" +
    "<li>After you have configured each Panel, you can view the display " +
    "output by selecting the <i>Display</i> tab.</li>" +
    "</ol>" +
    "<P>" +
    "If you'd like to be able to view your new display in the future " +
    "without needing to go through the setup process again, you can save " +
    "the display setup either, locally for your own use, or you can " +
    "upload it to the server for others to share (only certain users " +
    "have permission to do this). In order to save the setup use the " +
    "<i>Setup :: Save Setup</i> menu.";



  /** Text area to contain a description for the user. */
  protected JEditorPane itsDescription = new JEditorPane("text/html",
							 itsHelpText);

  public HelpFrame()
  {
    super("Help!");

    JButton tempbut = new JButton("OK");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("OK");

    itsDescription.setBackground(this.getBackground());
    itsDescription.setEditable(false);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(new JScrollPane(itsDescription),
			 BorderLayout.CENTER);
    getContentPane().add(tempbut, BorderLayout.SOUTH);

    //pack();
    setSize(new Dimension(550,600));
    setVisible(true);
  }


  /** Called when a button is pressed. */
  public
  void
  actionPerformed(ActionEvent e)
  {
    String command = e.getActionCommand();
    if (command.equals("OK")) {
      setVisible(false);
    }
  }
}
