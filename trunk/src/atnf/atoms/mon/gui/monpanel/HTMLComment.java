//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.SavedSetup;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.PrintStream;


/**
 * Simple class to allow HTML comments to be added to display setups in
 * order to provide background information to augment the data display.
 *
 * @author David Brodrick
 * @version $Id: HTMLComment.java,v 1.2 2005/10/05 04:40:35 bro764 Exp $
 * @see MonPanel
 */
public class
HTMLComment
extends MonPanel
implements ActionListener
{
  static {
    MonPanel.registerMonPanel("HTML Comment", HTMLComment.class);
  }

  public class
  HTMLCommentSetupPanel
  extends MonPanelSetupPanel
  implements ActionListener
  {
    /** The main panel which hold our GUI controls. */
    protected JPanel itsSetupPanel = new JPanel();

    /** Check box to enable or disable popup mode */
    protected JCheckBox itsPopupMode = new JCheckBox("Popup mode - enter button label:");

    /** Text field to allow user to enter button label for popup mode. */
    protected JTextField itsButtonLabel = new JTextField("Click Here For More Info", 40);

    /** Box to allow the operator to enter the HTML text. */
    protected JTextPane itsText = new JTextPane();

    /** Construct the setup editor for the specified panel. */
    public HTMLCommentSetupPanel(HTMLComment panel, JFrame frame)
    {
      super(panel, frame);
      setPreferredSize(new Dimension(500,300));
      setMinimumSize(new Dimension(200,200));
      setMaximumSize(new Dimension(2000,2000));

      //itsText.setBackground(this.getBackground());

      itsSetupPanel.setLayout(new BoxLayout(itsSetupPanel, BoxLayout.Y_AXIS));

      JPanel temppanel = new JPanel();
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      itsPopupMode.setToolTipText("HTML text will popup in new window when button is pushed");
      itsPopupMode.addActionListener(this);
      itsPopupMode.setActionCommand("popup");
      temppanel.add(itsPopupMode);
      temppanel.add(itsButtonLabel);
      temppanel.setPreferredSize(new Dimension(200, 25));
      temppanel.setMaximumSize(new Dimension(2000, 30));
      temppanel.setMinimumSize(new Dimension(100, 20));
      itsSetupPanel.add(temppanel);

      JLabel templabel = new JLabel("Enter HTML text below:");
      temppanel = new JPanel();
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      temppanel.add(itsText);
      itsSetupPanel.add(templabel);
      itsSetupPanel.add(temppanel);
      add(itsSetupPanel);

      itsPopupMode.doClick();
    }

    public
    void
    actionPerformed(ActionEvent e)
    {
      super.actionPerformed(e);
      String cmd = e.getActionCommand();
      if (cmd.equals("popup")) {
	if (itsPopupMode.isSelected()) {
    itsButtonLabel.setEnabled(true);
  } else {
    itsButtonLabel.setEnabled(false);
  }
      }
    }

    /** Return the current setup, as determined by the GUI controls.
     * It provides the means of extracting the setup specified by the
     * user into a useable format.
     * @return SavedSetup specified by GUI controls, or <tt>null</tt> if
     *         no setup can be extracted from the GUI at present. */
    protected
    SavedSetup
    getSetup()
    {
      //Ensure user has not entered any reserved characters
      if (!checkString(itsText.getText())) {
	JOptionPane.showMessageDialog(this,
				      "The Text contains reserved characters.\n" +
				      "You must not use \"`\"\n",
				      "Bad Characters in Title",
				      JOptionPane.WARNING_MESSAGE);
	return null;
      }

      SavedSetup setup = new SavedSetup("temp",
                             "atnf.atoms.mon.gui.monpanel.HTMLComment");
      String temp = itsText.getText();
      if (temp.equals("")) {
        temp=" ";
      }
      setup.put("comment", temp);
      if (itsPopupMode.isSelected()) {
	setup.put("popup", "true");
	setup.put("label", itsButtonLabel.getText());
      } else {
	setup.put("popup", "false");
      }
      return setup;
    }


    /** Make the controls show information about the given setup. */
    public
    void
    showSetup(SavedSetup setup)
    {
      try {
	String temp = (String)setup.get("comment");
	itsText.setText(temp);

	temp = (String)setup.get("popup");
	if (temp==null||temp.equals("false")) {
	  itsPopupMode.setSelected(false);
	  itsButtonLabel.setEnabled(false);
	} else {
	  itsPopupMode.setSelected(true);
	  itsButtonLabel.setEnabled(true);
	  temp = (String)setup.get("label");
	  if (temp==null) {
      temp = "Click Here For More Info";
    }
          itsButtonLabel.setText(temp);
	}
      } catch (Exception e) {
	System.err.println("HTMLCommentSetupPanel:showSetup: " + e.getMessage());
      }
    }
  }

  /////////////////////// END NESTED CLASS /////////////////////////////

  /** Copy of the setup we are currently using. */
  private SavedSetup itsSetup = null;

  /** Widget to display our formatted HTML */
  private JEditorPane itsEditor = new JEditorPane();

  /** Are we in popup mode? */
  private boolean itsPopupMode = true;

  /** The button that launches the popup window. */
  private JButton itsLaunchButton = new JButton("Click Here For More Info");

  /** C'tor. */
  public
  HTMLComment()
  {
    setLayout(new java.awt.BorderLayout());

    itsEditor.setContentType("text/html");
    itsEditor.setEditable(false);
    itsEditor.setBackground(this.getBackground());

    itsLaunchButton.setToolTipText("Launch popup window with more details");
    itsLaunchButton.addActionListener(this);
    itsLaunchButton.setActionCommand("popup");

    if (itsPopupMode) {
      setPreferredSize(new Dimension(100,25));
      setMinimumSize(new Dimension(50,20));
      setMaximumSize(new Dimension(2000,25));
      add(itsLaunchButton);
    } else {
      setPreferredSize(new Dimension(500,300));
      setMinimumSize(new Dimension(200,200));
      setMaximumSize(new Dimension(2000,2000));
      add(new JScrollPane(itsEditor));
    }
  }


  /** Free all resources so that this MonPanel can disappear. */
  public
  void
  vaporise()
  {
  }


  /** Clear any current setup. */
  public synchronized
  void
  blankSetup()
  {
    itsEditor.setText("");
    synchronized (this) { this.notifyAll(); }
  }

  public
  void
  actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();
    if (cmd.equals("popup")) {
      new HTMLPopup();
    }
  }


  /** Configure this MonPanel to use the specified setup. This method can
   * be used to restore saved states, eg what monitor points to graph and
   * over what time range.
   * @param setup class-specific setup information.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if
   *         there was a problem and the setup cannot be used.
   */
  public synchronized
  boolean
  loadSetup(SavedSetup setup)
  {
    try {
      //check if the setup is suitable for our class
      if (!setup.checkClass(this)) {
	System.err.println("HTMLComment:loadSetup: setup not for "
			   + this.getClass().getName());
	return false;
      }

      removeAll();

      String text = (String)setup.get("comment");
      if (text==null) {
	itsEditor.setText("No Comment..");
      } else {
	itsEditor.setText(text);
	setPreferredSize(new Dimension(500,text.length()*5));
      }

      text = (String)setup.get("popup");
      if (text==null||text.equals("true")) {
	itsPopupMode = true;
	setPreferredSize(new Dimension(100,25));
	setMinimumSize(new Dimension(50,20));
	setMaximumSize(new Dimension(2000,25));
	text = (String)setup.get("label");
        if (text==null) {
          text = "Click Here for More Info";
        }
	itsLaunchButton.setText(text);
	add(itsLaunchButton);
      } else {
	itsPopupMode = false;
	setPreferredSize(new Dimension(500,itsEditor.getText().length()*3));
	setMinimumSize(new Dimension(200,100));
	setMaximumSize(new Dimension(2000,2000));
	add(new JScrollPane(itsEditor));
      }

      itsSetup = setup;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }


  /** Get the current class-specific configuration for this MonPanel.
   * This can be used to capture the current state of the MonPanel so that
   * it can be easily recovered later.
   * @return class-specific configuration information.
   */
  public synchronized
  SavedSetup
  getSetup()
  {
    return itsSetup;
  }


  /** Get a Panel with all the controls required to configure this panel.
   * @return GUI controls to configure this MonPanel. */
  public 
  MonPanelSetupPanel
  getControls()
  {
    return new HTMLCommentSetupPanel(this, itsFrame);
  }


  public String getLabel() { return null; }


  /** Dump current data to the given output stream. This is the mechanism
   * through which data can be exported to a file.
   * @param p The print stream to write the data to. */
  public synchronized
  void
  export(PrintStream p)
  {
    final String rcsid = "$Id: HTMLComment.java,v 1.2 2005/10/05 04:40:35 bro764 Exp $";
  }

  /** Nested class that shows the HTML text in a popup window. */
  public class
  HTMLPopup
  extends JFrame
  implements ActionListener
  {
    public HTMLPopup() {
      getContentPane().setLayout(new BoxLayout(getContentPane(),
					       BoxLayout.Y_AXIS));
      itsEditor.setMinimumSize(new Dimension(200,200));
      itsEditor.setPreferredSize(new Dimension(650,500));
      getContentPane().add(new JScrollPane(itsEditor));
      JPanel temppanel = new JPanel();
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      temppanel.setBorder(BorderFactory.createLoweredBevelBorder());
      JButton tempbutton = new JButton("CLOSE");
      tempbutton.addActionListener(this);
      tempbutton.setActionCommand("close");
      tempbutton.setToolTipText("Click here to close this window");
      tempbutton.setMaximumSize(new Dimension(2000, 25));
      temppanel.add(tempbutton);
      getContentPane().add(temppanel);
      pack();
      setVisible(true);
    }

    public
    void
    actionPerformed(ActionEvent e)
    {
      String cmd = e.getActionCommand();
      if (cmd.equals("close")) {
	setVisible(false);
      }
    }
  }

  /** Simple test application. */
  public static void main(String[] argv)
  {
    JFrame foo = new JFrame("HTMLComment Test App");
    foo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    foo.getContentPane().setLayout(new BoxLayout(foo.getContentPane(), BoxLayout.Y_AXIS));

    HTMLComment ts1 = new HTMLComment();
    foo.getContentPane().add(ts1);
    foo.pack();
    foo.setVisible(true);

    JFrame foo2 = new JFrame("Setup Window");
    foo2.getContentPane().add(ts1.getControls());
    foo2.pack();
    foo2.setVisible(true);
  }
}

