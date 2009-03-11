//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.SavedSetup;
//import atnf.atoms.time.RelTime;
//import atnf.atoms.util.ATOMS;

//import java.util.Vector;
//import java.util.StringTokenizer;
import java.awt.*;
//import java.awt.print.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;


/**
 * Show a window which prompts the user for the information required to
 * save a particular setup to a file. This can be subclassed to save the
 * setup to other desitinations, by implementing <i>doSave()</i> in the
 * derived class. Subclasses may also wish to overwrite the text in
 * <i>itsDescription</i> and add new GUI components to <i>itsMainPanel</i>.
 *
 * @author David Brodrick
 * @version $Id: SaveSetupFrame.java,v 1.5 2005/09/20 03:59:58 bro764 Exp $
 * @see SavedSetup
 */
public class
SaveSetupFrame
extends JFrame
implements ActionListener
{
  /** The SavedSetup we are saving. */
  protected SavedSetup itsSetup = null;

  /** The panel where the bulk of the widgets are placed. */
  protected JPanel itsMainPanel = new JPanel();

  /** the panel which contains the buttons. */
  protected JPanel itsButtonPanel = new JPanel();

  /** Text field which holds the hierarchical name to use for the setup. */
  protected JTextField itsName = new JTextField(25);

  /** Text field which holds the title to give this setup. */
  protected JTextField itsTitle = new JTextField(25);

  /** Text area to contain a description for the user. */
  protected JTextArea itsDescription = new JTextArea();

  public SaveSetupFrame(SavedSetup setup)
  {
    super("Save Setup");
    itsSetup = setup;

    itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

    JButton tempbut = new JButton("Cancel");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("Cancel");
    itsButtonPanel.add(tempbut);
    tempbut = new JButton("OK");
    tempbut.addActionListener(this);
    tempbut.setActionCommand("OK");
    itsButtonPanel.add(tempbut);

    itsDescription.setText("You can save the current display setup onto your local " +
			   "computer, so that you can view the same information again " +
			   "simply by selecting the appropriate option from the menus.\n\n" +
			   "You will need to enter a short descriptive title for the " +
			   "display setup and also a hierarchical name in dot notation. " +
			   "The hierarchical name will determine where your new setup " +
			   "appears in the menu options.\n" +
			   "As a suggestion, you might like to start the hierarchical name " +
			   "of all of your personal saved setups with a standard string, like:\n" +
			   "\"local.ant.drives\"\twhere local has been prepended, or\n" +
                           "\"bob.site.weather\"\twhere Bob has prepended his name.\n\n");
    itsDescription.setLineWrap(true);
    itsDescription.setWrapStyleWord(true);
    itsDescription.setEditable(false);
    itsDescription.setBackground(Color.lightGray);
    itsDescription.setFont(new Font("Serif", Font.BOLD, 14));

    JPanel temppanel = new JPanel();
    temppanel.setLayout(new BorderLayout());
    temppanel.add(itsDescription, BorderLayout.CENTER);
    temppanel.add(itsMainPanel, BorderLayout.SOUTH);

    JLabel templabel = new JLabel("Setup Title: eg, \"My Weather\"");
    templabel.setToolTipText("Enter a descriptive title for this setup");
    itsMainPanel.add(templabel);
    itsMainPanel.add(itsTitle);
    templabel = new JLabel("Menu Location: eg, \"bob.site.weather\"");
    templabel.setToolTipText("Enter the hierarchical location to save the new setup");
    itsMainPanel.add(templabel);
    itsMainPanel.add(itsName);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(temppanel, BorderLayout.CENTER);
    getContentPane().add(itsButtonPanel, BorderLayout.SOUTH);

    //pack();
    setSize(new Dimension(450,400));
    setVisible(true);
  }


  /** Called when a button is pressed. */
  public
  void
  actionPerformed(ActionEvent e)
  {
    String command = e.getActionCommand();
    if (command.equals("OK")) {
      if (checkName() && checkTitle()) {
        //Set the hierarchical name for the setup
	itsSetup.setName(itsName.getText().trim());
        itsSetup.put("title", itsTitle.getText());
        //Try to do the actual save
	if (doSave()) {
	  //Pleasure to be of service..
	  setVisible(false);
	  //Update the active registry and menu options
	  MonClientUtil.mergeSetup(itsSetup);
          MonFrame.theirWindowManager.rebuildMenus();
          MonFrame.rebuildDisplayMenus();
	} else {
	  //Couldn't save the setup for some reason
	  JOptionPane.showMessageDialog(this,
                                        "The setup save operation did\n" +
                                        "not complete successfully.\n",
                                        "Didn't Save Setup",
					JOptionPane.WARNING_MESSAGE);
	}
      }
    } else if (command.equals("Cancel")) {
      setVisible(false);
    }
  }


  /** Check if the hierarchical name is valid. */
  protected
  boolean
  checkName()
  {
    String name = itsName.getText().trim();
    if (name==null || name.equals("")) {
      //No name was entered
      JOptionPane.showMessageDialog(this,
				    "You must enter a hierarchical name so\n" +
				    "that the setup can be saved and then\n" +
                                    "retrieved via the predefined setup menus\n",
				    "Must Enter Name",
				    JOptionPane.WARNING_MESSAGE);
      itsName.setText("");
      return false;
    }
    if (name.indexOf("~")!=-1 ||
	name.indexOf(":")!=-1 ||
	name.indexOf("`")!=-1) {
      //An invalid character was used
      JOptionPane.showMessageDialog(this,
				    "The characters ~ : and ` cannot be\n" +
                                    "used in the setup hierarchical name.\n",
				    "Illegal Characters",
				    JOptionPane.WARNING_MESSAGE);
      return false;
    }
    itsName.setText(name.trim().toLowerCase());
    return true;
  }


  /** Check if the title string is valid. */
  protected
  boolean
  checkTitle()
  {
    String name = itsTitle.getText().trim();
    if (name==null || name.equals("")) {
      //No name was entered
      JOptionPane.showMessageDialog(this,
				    "You must enter a Title for the display setup.",
				    "Must Enter Title",
				    JOptionPane.WARNING_MESSAGE);
      itsTitle.setText("");
      return false;
    }
    if (name.indexOf("~")!=-1 ||
	name.indexOf(":")!=-1 ||
	name.indexOf("`")!=-1) {
      //An invalid character was used
      JOptionPane.showMessageDialog(this,
				    "The characters ~ : and ` cannot be\n" +
                                    "used in the title for the setup.\n",
				    "Illegal Characters",
				    JOptionPane.WARNING_MESSAGE);
      return false;
    }
    return true;
  }


  /** Save the setup to the appropriate destination. This class writes the
   * setup to the users local setup file, but sub-classes might take
   * alternate actions, such as uploading the setup to a server. */
  protected
  boolean
  doSave()
  {
    //Figure out which platform we are on. Pretty silly.
    String osname = System.getProperty("os.name").toLowerCase();
    String monfile = null;
    if (osname.indexOf("win")!=-1) {
      //Must be some flavour of winblows
      monfile = "\\Application Data\\MoniCA\\local-setups.txt";  
      osname = "win32";
    } else {
      //But what about other platforms?
      osname = "unix";
      monfile = "/.MoniCA/local-setups.txt";
    }
    monfile = System.getProperty("user.home") + monfile;

    System.err.println("### Saving setup to " + monfile);

    try {
      //Ensure all directories exist
      File tempf = new File(monfile).getParentFile();
      tempf.mkdirs();
      //Open file in APPEND mode and write the setup out
      PrintStream file = new PrintStream(new FileOutputStream(monfile, true));
      file.println(itsSetup);
      file.close();
    } catch (Exception e) {
      System.err.println("SaveSetupFrame: doSave: An ERROR occurred:");
      System.err.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
