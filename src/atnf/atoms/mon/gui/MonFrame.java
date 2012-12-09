//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

// import atnf.atoms.mon.gui.monpanel.*;
// import atnf.atoms.mon.client.*;
import atnf.atoms.mon.client.ClockErrorMonitor;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.*;
import java.util.Vector;
import java.util.StringTokenizer;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import java.io.*;

/**
 * @author David Brodrick
 * @version $Id: MonFrame.java,v 1.4 2006/09/13 23:53:43 bro764 Exp bro764 $
 * @see MonPanel
 */
public class MonFrame extends JFrame implements ActionListener {
  /**
   * Class to keep track of currently open windows, offer a menu-list of those
   * windows to the user via each window's <i>Windows</i> menu, and raise
   * windows to the foreground when they are selected.
   */
  public static class WindowManager implements ActionListener {
    /** Holds reference to all currently open MonFrames. */
    static Vector itsWindows = new Vector();

    /** Menus for each of the windows, in same order as <i>itsWindows</i>. */
    static Vector itsMenus = new Vector();

    /**
     * Called when a window is selected from the menu. The window which was
     * selected is raised to the screen foreground (hopefully).
     */
    public synchronized void actionPerformed(ActionEvent e) {
      String action = e.getActionCommand();
      if (action.equals("New Blank Window")) {
        new MonFrame();
      } else if (action.equals("recoverArrangement")) {
        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog((MonFrame) itsWindows.get(0));
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            File file = fc.getSelectedFile();
            if (!recoverArrangement(file.getAbsoluteFile().toString())) {
              throw new Exception("Unable to properly load the file");
            }
          } catch (Exception f) {
            // No Joy..
            f.printStackTrace();
            JOptionPane.showMessageDialog((MonFrame) itsWindows.get(0), "There was an error:\n" + f.getMessage() + "\n",
                "Error Loading File", JOptionPane.WARNING_MESSAGE);
          }
        }
      } else if (action.equals("saveArrangement")) {
        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showSaveDialog((MonFrame) itsWindows.get(0));
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            File file = fc.getSelectedFile();
            if (!saveArrangement(file.getAbsoluteFile().toString())) {
              throw new Exception("Unable to properly save the file");
            }
          } catch (Exception f) {
            // No Joy..
            f.printStackTrace();
            JOptionPane.showMessageDialog((MonFrame) itsWindows.get(0), "There was an error:\n" + f.getMessage() + "\n",
                "Error Saving File", JOptionPane.WARNING_MESSAGE);
          }
        }
      } else if (action.equals("Quit")) {
        System.exit(0);
      } else if (action.startsWith("Close:")) {
        // Find which window was selected
        int num = Integer.parseInt(action.substring(action.indexOf(":") + 1));
        MonFrame selected = null;
        for (int i = 0; i < itsWindows.size(); i++) {
          MonFrame tempframe = (MonFrame) itsWindows.get(i);
          if (tempframe.getNumber() == num) {
            selected = tempframe;
            break;
          }
        }
        // Close the selected Window, and exit if no windows left
        if (selected != null) {
          selected.removeAllPanels();
          selected.setVisible(false);
          remove(selected);
          // If there are no windows left, we can close the app!
          if (numWindows() == 0) {
            System.exit(0);
          }
        } else {
          System.err.println("MonFrame:WindowManager: Can't remove Monframe");
        }
      } else if (action.startsWith("Raise:")) {
        // Find which window was selected
        int num = Integer.parseInt(action.substring(action.indexOf(":") + 1));
        MonFrame selected = null;
        for (int i = 0; i < itsWindows.size(); i++) {
          MonFrame tempframe = (MonFrame) itsWindows.get(i);
          if (tempframe.getNumber() == num) {
            selected = tempframe;
            break;
          }
        }
        // Raise the selected window to the foreground
        if (selected != null) {
          selected.setVisible(true);
        }
      } else {
        MonFrame newframe = new MonFrame();
        // User must have selected a setup for a new window
        SavedSetup reqsetup = MonClientUtil.getSetup("atnf.atoms.mon.gui.MonFrame", action);
        if (reqsetup != null) {
          newframe.loadSetup(reqsetup);
        } else {
          // Setup couldn't be found
          JOptionPane.showMessageDialog(newframe, "Curiously, the setup called:\n" + "\"" + action + "\"\n" + "for class:\n" + "\""
              + this.getClass().getName() + "\"\n" + "couldn't be found!\n", "Setup Not Found", JOptionPane.WARNING_MESSAGE);
          newframe.removeAllPanels();
          newframe.setVisible(false);
          remove(newframe);
        }
      }
    }

    /**
     * Get the number of open windows.
     * 
     * @return Number of open MonFrame windows.
     */
    public synchronized int numWindows() {
      return itsWindows.size();
    }

    /**
     * Get a reference to the specified MonFrame.
     * 
     * @param i
     *          The index of the MonFrame to return.
     * @return MonFrame reference or <tt>null</tt> if invalid.
     */
    public synchronized MonFrame getWindow(int i) {
      if (i >= itsWindows.size()) {
        return null;
      }
      return (MonFrame) itsWindows.get(i);
    }

    /**
     * Add a new window to the registry. The <i>Windows</i> menu on all current
     * windows will be updated to show the new window. A menu for display on the
     * new window is returned.
     * 
     * @param frame
     *          The new Window to register.
     * @return <i>Windows</i> menu for display in the new window.
     */
    public synchronized JMenu add(MonFrame frame) {
      itsWindows.add(frame);
      JMenu menu = new JMenu("Window");
      menu.setToolTipText("You can launch new windows here");
      menu.setMnemonic(KeyEvent.VK_W);
      itsMenus.add(menu);
      // Available windows have changed so need to update all menus
      rebuildMenus();
      return menu;
    }

    /**
     * Deregister the specified window. The window will be removed from the
     * <i>Windows</i> list of all remaining windows.
     * 
     * @param frame
     *          The window to deregister.
     */
    public synchronized void remove(MonFrame frame) {
      // Get the index for the frame to be removed
      int loc = itsWindows.indexOf(frame);
      if (loc == -1) {
        System.err.println("MonFrame:WindowManager: FRAME NOT FOUND!");
        return;
      }
      // Remove frame and menu from containers
      itsWindows.remove(frame);
      itsMenus.remove(itsMenus.get(loc));
      // Rebuild menus in all remaining windows
      rebuildMenus();
    }

    /**
     * Update the <i>Windows</i> menu in all windows. The updated menus will
     * list all currently open windows as options.
     */
    public synchronized void rebuildMenus() {
      // Simply update the menu for each current window
      for (int i = 0; i < itsWindows.size(); i++) {
        rebuildMenu((JMenu) itsMenus.get(i), (MonFrame) itsWindows.get(i));
      }
    }

    /**
     * Rebuild the <i>Windows</i> menu options for the given window.
     * 
     * @param menu
     *          The menu to be updated.
     * @param frame
     *          The frame to which the menu belongs.
     */
    private synchronized void rebuildMenu(JMenu menu, MonFrame frame) {
      synchronized (menu) {
        // Remove all exiting menu entries
        menu.removeAll();
        JMenuItem temp;
        JMenu tempmenu;

        // Add some fields to add/remove Windows
        temp = new JMenuItem("Empty");
        temp.setActionCommand("New Blank Window");
        temp.addActionListener(this);
        temp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        temp.setMnemonic(KeyEvent.VK_N);
        tempmenu = new JMenu("New Window");
        tempmenu.add(temp);
        tempmenu.addSeparator();
        MonClientUtil.getSetupMenu(MonFrame.class.getName(), this, tempmenu);
        menu.add(tempmenu);
        menu.addSeparator();
        temp = new JMenuItem("Load Arrangement");
        temp.setActionCommand("recoverArrangement");
        temp.setToolTipText("Load a saved arrangement of windows");
        temp.addActionListener(this);
        menu.add(temp);
        temp = new JMenuItem("Save Arrangement");
        temp.setActionCommand("saveArrangement");
        temp.setToolTipText("Save the current window arrangement");
        temp.addActionListener(this);
        menu.add(temp);
        menu.addSeparator();
        temp = new JMenuItem("Close");
        temp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        temp.setMnemonic(KeyEvent.VK_C);
        temp.setActionCommand("Close:" + frame.getNumber());
        temp.addActionListener(this);
        menu.add(temp);
        temp = new JMenuItem("Quit");
        temp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        temp.setMnemonic(KeyEvent.VK_Q);
        temp.setActionCommand("Quit");
        temp.addActionListener(this);
        menu.add(temp);
        menu.addSeparator();

        // Add in all current windows, disable our own
        for (int i = 0; i < itsWindows.size(); i++) {
          MonFrame tempframe = (MonFrame) itsWindows.get(i);
          temp = new JMenuItem(tempframe.getTitle());
          temp.setActionCommand("Raise:" + tempframe.getNumber());
          temp.addActionListener(this);
          if (tempframe == frame) {
            temp.setEnabled(false);
          }
          menu.add(temp);
        }
      }
    }
  };

  /** Component to keep track of open windows and raise them to foreground. */
  public static WindowManager theirWindowManager = new WindowManager();

  /**
   * Records number of MonFrames which have been created. This is just used for
   * coming up with default names for the window titles.
   */
  private static int theirNumCreated = 1;

  /** Records which number MonFrame this frame is. */
  private int itsNumber = -1;

  /** Vector which hold all the MonPanels being displayed in this frame. */
  private Vector<MonPanel> itsPanels = new Vector();

  /**
   * Vector holds reference to the setup configuration panels for each MonPanel.
   */
  private Vector itsSetupPanels = new Vector();

  /** Main tabbed pane. */
  private JTabbedPane itsTabs = new JTabbedPane();

  /** The main panel to which all the display panels are added. */
  private JPanel itsMainPanel = new JPanel();

  /** The panel for controlling the screen layout. */
  private LayoutPanel itsLayoutPanel = new LayoutPanel(this);

  /** The menu bar. */
  private JMenuBar itsMenuBar = new JMenuBar();

  /** The <i>Sub-Panels</i> menu. */
  private JMenu itsSubPanelsMenu = new JMenu("Setup");

  /** The <i>Navigator</i> menu. */
  private JMenu itsDisplayMenu = new JMenu("Navigator");

  /** RCS version. */
  private static final String theirRCS = "$Id: MonFrame.java,v 1.4 2006/09/13 23:53:43 bro764 Exp bro764 $";

  /** Name of file to autodump screenshots to. */
  private String itsDumpFile = "foo.png";

  /** timer for triggering autodumps. */
  private Timer itsTimer = null;

  /** Stores information on if a custom manual layout has been implemented. */
  private static boolean itsLayoutRedrawn = false;

  /**
   * Last loaded setup, might be null if the setup has been modified since it
   * was initially loaded.
   */
  public SavedSetup itsSetup = null;

  private static int itsWindowWidth = 0;
  private static int itsWindowHeight = 0;

  /** C'tor. */
  public MonFrame() {
    super("Monitor Display " + theirNumCreated);

    itsNumber = theirNumCreated;
    theirNumCreated++;

    setJMenuBar(itsMenuBar);
    // Add the "Windows" menu
    itsMenuBar.add(theirWindowManager.add(this));
    // Add the "Display" menu
    itsDisplayMenu.setToolTipText("Select what to display");
    itsDisplayMenu.setMnemonic(KeyEvent.VK_N);
    itsMenuBar.add(itsDisplayMenu);
    rebuildDisplayMenu();
    // Add the "Sub-Panels" menu
    itsSubPanelsMenu.setToolTipText("Add/Remove data display panels");
    itsSubPanelsMenu.setMnemonic(KeyEvent.VK_S);
    itsMenuBar.add(itsSubPanelsMenu);
    rebuildSubPanelMenu();

    JMenu exportMenu = new JMenu("Export");
    exportMenu.setToolTipText("Export the information in a different format");
    exportMenu.setMnemonic(KeyEvent.VK_E);
    JMenuItem tempMenu = new JMenuItem("Print");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("Print");
    tempMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
    tempMenu.setMnemonic(KeyEvent.VK_P);
    tempMenu.setToolTipText("Print the current screen");
    exportMenu.add(tempMenu);
    tempMenu = new JMenuItem("PNG Image");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("toPNG");
    tempMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
    tempMenu.setMnemonic(KeyEvent.VK_D);
    tempMenu.setToolTipText("Dump the current screen as a .PNG image file");
    exportMenu.add(tempMenu);
    tempMenu = new JMenuItem("ASCII Data");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("Export");
    tempMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
    tempMenu.setMnemonic(KeyEvent.VK_E);
    tempMenu.setToolTipText("Write current data to an ASCII file");
    exportMenu.add(tempMenu);
    itsMenuBar.add(exportMenu);

    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    tempMenu = new JMenuItem("Intro");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("Help");
    helpMenu.add(tempMenu);
    itsMenuBar.add(helpMenu);

    // Use Y_AXIS BoxLayout to stack all display panels vertically
    itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
    itsTabs.add("Display", itsMainPanel);
    itsTabs.setForegroundAt(0, Color.blue);
    itsTabs.add("Layout", itsLayoutPanel);
    itsTabs.setForegroundAt(1, Color.orange);
    getContentPane().add(itsTabs);

    // Make handler for when the frame is closed - we will need to clean up
    final MonFrame thisframe = this;
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        thisframe.removeAllPanels();
        thisframe.setVisible(false);
        theirWindowManager.remove(thisframe);
        // If there are no windows left, we can close the app!
        if (theirWindowManager.numWindows() == 0) {
          System.exit(0);
        }
      }
    });

    setSize(new Dimension(600, 700));
    setVisible(true);
  }

  /** Return the Vector of currently displayed panels. */
  public Vector<MonPanel> getPanels() {
    return itsPanels;
  }

  /** Get a unique identification number for this MonFrame. */
  public int getNumber() {
    return itsNumber;
  }

  /**
   * Switch to the main display tab OR layout tab depending on Auto/Manual
   * preference.
   */
  public synchronized void showDisplay() {
    // if (itsLayoutPanel.itsAutoControl.isSelected()) {
    // System.out.println("Auto control selected: displaying graph");
    itsTabs.setSelectedIndex(0);
    // } else { // Switch to Layout tab in Manual mode
    // System.out.println("Manual control selected; switching to Layout tab");
    // itsTabs.setSelectedIndex(1);
    // }
  }

  /**
   * Switch to the main display tab
   */
  public void showDisplayForced() {
    itsTabs.setSelectedIndex(0);
  }

  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();

    if (action.equals("Clear Frame")) {
      // Clear the current setup of the window
      blankSetup();
    } else if (action.equals("Save Local")) {
      new SaveSetupFrame(getSetup());
    } else if (action.equals("Save Server")) {
      new SaveSetupServerFrame(getSetup());
    } else if (action.equals("Print")) {
      PrintUtilities.printComponent(itsMainPanel);
    } else if (action.startsWith("AddPanel")) {
      int i = Integer.parseInt(action.substring(8).trim());
      addPanelNow(MonPanel.getMonPanel(i));
    } else if (action.startsWith("RemovePanel")) {
      int i = Integer.parseInt(action.substring(11).trim());
      removePanel((MonPanel) itsPanels.get(i));
    } else if (action.equals("toPNG")) {
      final JFileChooser fc = new JFileChooser();
      int returnVal = fc.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          File file = fc.getSelectedFile();
          dumpPNG(file.getAbsoluteFile().toString());
        } catch (Exception f) {
          // No Joy..
          f.printStackTrace();
          JOptionPane.showMessageDialog(this, "There was an error:\n" + f.getMessage() + "\n", "Export Error",
              JOptionPane.WARNING_MESSAGE);
        }
      }
    } else if (action.equals("Export")) {
      final JFileChooser fc = new JFileChooser();
      int returnVal = fc.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          File file = fc.getSelectedFile();
          FileOutputStream fout = new FileOutputStream(file);
          PrintStream pout = new PrintStream(fout);
          for (int i = 0; i < itsPanels.size(); i++) {
            ((MonPanel) itsPanels.get(i)).export(pout);
          }
        } catch (Exception f) {
          // No Joy..
          f.printStackTrace();
          JOptionPane.showMessageDialog(this, "There was an error:\n" + f.getMessage() + "\n", "Export Error",
              JOptionPane.WARNING_MESSAGE);
        }
      }
    } else if (action.equals("Help")) {
      ;
      new HelpFrame();
    } else {
      // Must be the name of a setup to load
      SavedSetup reqsetup = MonClientUtil.getSetup(this.getClass().getName(), action);
      if (reqsetup != null) {
        loadSetup(reqsetup);
      } else {
        // Setup couldn't be found
        JOptionPane.showMessageDialog(this, "Curiously, the setup called:\n" + "\"" + action + "\"\n" + "for class:\n" + "\""
            + this.getClass().getName() + "\"\n" + "couldn't be found!\n", "Setup Not Found", JOptionPane.WARNING_MESSAGE);
      }
    }
  }

  /**
   * Add a new MonPanel to this frame.
   * 
   * @param newpan
   *          The panel to add to this frame.
   */
  protected void addPanel(MonPanel newpan) {
    // Called when a saved setup is loaded via the Navigator.
    itsSetup = null;
    newpan.setFrame(this);
    newpan.setBorder(BorderFactory.createLoweredBevelBorder());
    itsPanels.add(newpan);
    itsMainPanel.add(newpan);
    JPanel p = newpan.getControls();
    itsSetupPanels.add(p);
    if (p != null) {
      itsTabs.addTab(MonPanel.getName(newpan.getClass()), p);
    }
    rebuildSubPanelMenu();
    repaint();
    itsLayoutPanel.update();
  }

  /**
   * Add a new MonPanel to this frame and redraw screen immediately.
   * 
   * @param newpan
   *          The panel to add to this frame.
   */
  protected void addPanelNow(MonPanel newpan) {
    // This method is called when you add a new panel via Setup > Add Panel
    itsSetup = null;
    newpan.setFrame(this);
    newpan.setBorder(BorderFactory.createLoweredBevelBorder());

    itsPanels.add(newpan);
    JPanel p = newpan.getControls();
    itsSetupPanels.add(p);
    if (p != null) {
      itsTabs.addTab(MonPanel.getName(newpan.getClass()), p);
      itsTabs.setSelectedComponent(p);
    }

    // Populate the dimension tables with default values to be altered by
    // the user if in manual mode
    if (itsLayoutPanel.itsManualControl.isSelected()) {
      int panelIndex = itsPanels.size() - 1; // Panel just added
      itsLayoutPanel.defineCoordinates(panelIndex);
    }

    if (!itsLayoutRedrawn) {
      // System.out.println("MonFrame:addPanelNow: itsLayout NOT redrawn");
      itsMainPanel.add(newpan, "grow");
      rebuildSubPanelMenu();
      validate();
      repaint();
      itsLayoutPanel.update();
    } else {
      // System.out.println("MonFrame:addPanelNow: itsLayout redrawn");

      rebuildSubPanelMenu();
      redrawPanels(newpan, 0, 0, 5, 5);
    }
  }

  /**
   * Setup the MiGLayout
   */
  protected void setUpML() {
    itsWindowWidth = itsMainPanel.getWidth();
    itsWindowHeight = itsMainPanel.getHeight();

    // Creating a MigLayout with 10 columns and 10 rows
    MigLayout ml = new MigLayout();
    itsMainPanel.setLayout(ml);
  }

  protected void setUpMLAuto() {
    MigLayout ml = new MigLayout("nogrid, flowy, fill", "", "");
    itsMainPanel.setLayout(ml);
  }

  /**
   * Redraw the Display using the coordinates set by the user in the Layout
   * Control panel.
   */
  protected void redrawPanels(MonPanel panel, int x, int y, int width, int height) {
    // System.out.printf("MonFrame: redrawPanels x %d y %d width %d height %d\n",
    // x, y, width, height);

    int x2 = x + width;
    int y2 = y + height;

    itsMainPanel.add(panel, "pos " + x + "0% " + y + "0% " + x2 + "0% " + y2 + "0%");

    itsLayoutPanel.update();
    itsLayoutRedrawn = true; // Indicates layout has been manually altered
  }

  /** Redraw Display screen using Automatic layout control */
  protected void redrawPanelsAuto(MonPanel newpan) {
    itsSetup = null;
    newpan.setFrame(this);
    newpan.setBorder(BorderFactory.createLoweredBevelBorder());

    itsMainPanel.add(newpan, "grow");
    repaint();
    itsLayoutPanel.update();
    itsLayoutRedrawn = false;
  }

  /** Clear existing panels. */
  protected void clearPanels() {
    itsMainPanel.removeAll(); // Panels will be redrawn with new positions.
  }

  /**
   * Remove the specified MonPanel from this frame.
   * 
   * @param deadpan
   *          The MonPanel to remove from this frame.
   * @return <tt>true</tt> if removal went OK, <tt>false</tt> if the removal
   *         failed (eg, maybe that panel doesn't exist within this frame).
   */
  protected void removePanel(MonPanel deadpan) {
    itsSetup = null;
    if (!itsPanels.contains(deadpan)) {
      System.err.println("MonFrame:removePanel: Panel Doesn't Exist");
      return;
    }
    // Get the index for the panel to be removed
    int i;
    for (i = 0; i < itsPanels.size(); i++) {
      if (itsPanels.get(i) == deadpan) {
        break;
      }
    }

    itsMainPanel.remove(deadpan);
    itsPanels.remove(i);
    if (itsSetupPanels.get(i) != null) {
      itsTabs.remove((Component) itsSetupPanels.get(i));
    }
    itsSetupPanels.remove(i);

    // Remove the MonPanel
    deadpan.vaporise();

    rebuildSubPanelMenu();
    // Redraw screen
    validate();
    repaint();
    itsLayoutPanel.update();
  }

  /** */
  protected void removeAllPanels() {
    itsSetup = null;
    for (int i = 0; i < itsPanels.size(); i++) {
      // Get each MonPanel to free it's resources
      ((MonPanel) itsPanels.get(i)).vaporise();
    }
    itsPanels.clear();
    itsMainPanel.removeAll();
    itsSetupPanels.clear();
    while (itsTabs.getTabCount() > 2) { // This is with a LayoutPanel
      itsTabs.remove(2);
    }
    rebuildSubPanelMenu();
    // Redraw empty screen
    validate();
    repaint();
    itsLayoutPanel.update();
  }

  /** Clear any current setup. */
  public void blankSetup() {
    itsSetup = null;
    removeAllPanels();
    // setSize(new Dimension(600, 400));
    setTitle("MoniCA: Display " + itsNumber);
    rebuildSubPanelMenu();
    // Rebuild the windows menu since our title may have changed
    theirWindowManager.rebuildMenus();
    validate();
    repaint();
  }

  public void rebuildAuto() {
    removeAllPanels();
  }

  /** Rebuild the <i>Sub-Panels</i> menu bar to reflect current state. */
  private void rebuildSubPanelMenu() {
    itsSubPanelsMenu.removeAll();

    // Submenu for removing panels currently included in the display
    JMenuItem tempMenu = new JMenu("Remove Panel");
    if (itsPanels.size() > 0) {
      tempMenu.setEnabled(true);
      tempMenu.setToolTipText("Remove panels from the current display");
      for (int i = 0; i < itsPanels.size(); i++) {
        MonPanel monp = (MonPanel) itsPanels.get(i);
        String desc = MonPanel.getName(monp.getClass());
        JMenuItem tempMenu2 = new JMenuItem(desc);
        tempMenu2.setActionCommand("RemovePanel " + i);
        tempMenu2.addActionListener(this);
        tempMenu2.setToolTipText("Remove a " + desc + " from the display");
        tempMenu.add(tempMenu2);
      }
    } else {
      tempMenu.setEnabled(false);
      tempMenu.setToolTipText("No panels to remove");
    }
    itsSubPanelsMenu.add(tempMenu);

    // Submenu for adding new panels to the current display
    tempMenu = new JMenu("Add Panel");
    String[] paneltypes = MonPanel.getNames();
    if (paneltypes == null || paneltypes.length == 0) {
      tempMenu.setEnabled(false);
      tempMenu.setToolTipText("No panel types can be added");
    } else {
      tempMenu.setEnabled(true);
      tempMenu.setToolTipText("Add extra panels to the current display");

      for (int i = 0; i < paneltypes.length; i++) {
        JMenuItem tempMenu2 = new JMenuItem(paneltypes[i]);
        tempMenu2.setActionCommand("AddPanel " + i);
        tempMenu2.addActionListener(this);
        tempMenu2.setToolTipText("Add an empty " + paneltypes[i]);
        tempMenu.add(tempMenu2);
      }
    }
    itsSubPanelsMenu.add(tempMenu);

    // Menu options for clearing and saving the current setup
    itsSubPanelsMenu.addSeparator();
    tempMenu = new JMenuItem("Other Functions:");
    tempMenu.setEnabled(false);
    itsSubPanelsMenu.add(tempMenu);
    tempMenu = new JMenuItem("Clear");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("Clear Frame");
    tempMenu.setToolTipText("Remove all the panels from the window");
    if (itsPanels.size() == 0) {
      tempMenu.setEnabled(false);
    }
    itsSubPanelsMenu.add(tempMenu);
    JMenu saveMenu = new JMenu("Save Setup");
    saveMenu.setToolTipText("Save current setup so it can be reused");
    if (itsPanels.size() == 0) {
      saveMenu.setEnabled(false);
    }
    itsSubPanelsMenu.add(saveMenu);
    tempMenu = new JMenuItem("Locally");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("Save Local");
    tempMenu.setToolTipText("Save to local file for you own use");
    saveMenu.add(tempMenu);
    tempMenu = new JMenuItem("To Server");
    tempMenu.addActionListener(this);
    tempMenu.setActionCommand("Save Server");
    tempMenu.setToolTipText("Save to server for everyone's use");
    saveMenu.add(tempMenu);
  }

  /** Rebuild the options in the <i>Display</i> menu of all MonFrames. */
  public static void rebuildDisplayMenus() {
    int i = 0;
    while (i < theirWindowManager.numWindows()) {
      theirWindowManager.getWindow(i).rebuildDisplayMenu();
      i++;
    }
  }

  /** Rebuild the options in the <i>Display</i> menu. */
  public synchronized void rebuildDisplayMenu() {
    itsDisplayMenu.removeAll();
    MonClientUtil.getSetupMenu(this.getClass().getName(), this, itsDisplayMenu);
  }

  /**
   * Configure this MonFrame to use the specified setup. This method can be used
   * to restore saved states, eg what MonPanels to show in the window and what
   * information they should each display.
   * 
   * @param setup
   *          Window setup information to use.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if there
   *         was a problem and the setup cannot be used.
   */
  public boolean loadSetup(SavedSetup setup) {
    try {
      // Check if the new setup is suitable for our class
      if (!setup.checkClass(this)) {
        System.err.println("MonFrame:loadSetup: setup not for " + this.getClass().getName());
        return false;
      }

      // It's for our class - time to commit
      removeAllPanels();

      // Get the title to use for the window
      String title = (String) setup.get("title");
      // Get the number of panels to display in this window
      int numpanels = Integer.parseInt((String) setup.get("numpanels"));
      Vector<MonPanel> newpanels = new Vector(numpanels);
      Vector<SavedSetup> newsetups = new Vector(numpanels);

      // FIRST, create all the right panels, with right setups
      for (int i = 0; i < numpanels; i++) {
        String descriptor = (String) setup.get("setup" + i);

        SavedSetup panelsetup = new SavedSetup(descriptor);
        newsetups.add(panelsetup);
        // Try to create an empty instance of the correct class
        try {
          MonPanel newpanel = (MonPanel) panelsetup.getInstance();
          // Check if we were able to instantiate the panel
          if (newpanel != null) {
            newpanel.setFrame(this);
            newpanels.add(newpanel);
          } else {
            JOptionPane.showMessageDialog(this, "An error occurred while creating an\n\"" + panelsetup.getClassName() + "\"\n"
                + "for the setup called \"" + panelsetup.getName() + "\".\n\n" + "I couldn't instantiate the class.\n\n"
                + "This is a problem for a programmer...\n", "Error Loading Setup", JOptionPane.WARNING_MESSAGE);
            // Remove the not-yet-displayed panels
            for (int j = 0; j < newpanels.size(); j++) {
              ((MonPanel) newpanels.get(j)).vaporise();
            }
            blankSetup();
            return false;
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this, "An exception occurred while creating an\n\"" + panelsetup.getClassName() + "\"\n"
              + "for the setup called \"" + panelsetup.getName() + "\".\n\n" + "The type of the exception is:\n\""
              + e.getClass().getName() + "\".\n\n" + "This is a problem for a programmer...\n", "Error Loading Setup",
              JOptionPane.WARNING_MESSAGE);
          // Remove the not-yet-displayed panels
          for (int j = 0; j < newpanels.size(); j++) {
            ((MonPanel) newpanels.get(j)).vaporise();
          }
          blankSetup();
          return false;
        }
      }

      // SECOND, add all the empty panels to the frame and redraw
      for (int i = 0; i < numpanels; i++) {
        addPanel((MonPanel) newpanels.get(i));
      }
      setTitle("MoniCA: " + title);

      // LayoutPanel: Get saved panel layout information
      itsLayoutPanel.loadSetup(setup);
      itsLayoutPanel.setPanels();

      validate();

      // THIRD, configure each panel to use the required setup
      for (int i = 0; i < numpanels; i++) {
        MonPanel newpanel = (MonPanel) newpanels.get(i);
        SavedSetup newsetup = (SavedSetup) newsetups.get(i);
        if (!newpanel.loadSetup(newsetup)) {
          // The panel failed to parse it's setup
          // No need for a dialog as the panel will have already shown one
          // But we do need to abort the loading of this setup
          System.err.println("MonFrame:loadSetup: Sub-panel failed to load setup - ABORTING");
          blankSetup();
          return false;
        }
        MonPanelSetupPanel sp = (MonPanelSetupPanel) itsSetupPanels.get(i);
        if (sp != null) {
          sp.showSetup(newsetup);
        }
      }

      // Pack the window for new content
      pack();
      // Rebuild the windows menu since our title may have changed
      theirWindowManager.rebuildMenus();

      // save a reference to the setup we just loaded
      itsSetup = setup;
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "The setup \"" + setup.getName() + "\"\ncould not be parsed.\n\n"
          + "This Exception was encountered:\n" + e.getClass().getName(), "Error Loading Setup", JOptionPane.WARNING_MESSAGE);
      blankSetup();
      return false;
    }

    itsLayoutPanel.resizeWindow();

    return true;
  }

  /**
   * Get the current configuration for this MonFrame. This can be used to
   * capture the current state of the MonFrame so that it can be easily
   * recovered later.
   * 
   * @return Current window setup configuration information.
   */
  public SavedSetup getSetup() {
    SavedSetup setup = new SavedSetup("temp", this.getClass().getName());

    setup.put("title", getTitle());
    setup.put("numpanels", "" + itsPanels.size());

    // Then need to add the current setup of each panel in the window
    for (int i = 0; i < itsPanels.size(); i++) {
      SavedSetup temp = ((MonPanel) itsPanels.get(i)).getSetup();
      setup.put("setup" + i, temp.toString());
    }

    // Add the layout control information for each panel to the SavedSetup
    itsLayoutPanel.getSetup(setup);

    if (itsSetup != null && itsSetup.compareKeys(setup)) {
      // The setup is the same as when it was loaded so use old one which has
      // name
      setup = itsSetup;
    } else {
      System.err.println("MonFrame.getSetup: Setup has been modified");
      itsSetup = setup;
    }

    return setup;
  }

  /**
   * Dump the current screen to a .PNG image file. Returns false if the dump
   * failed.
   */
  protected boolean dumpPNG(String filename) {
    AbsTime now = new AbsTime();
    System.err.println("###" + now.toString(AbsTime.Format.UTC_STRING) + ": Dumping screenshot to \"" + filename + "\"");
    try {
      PNGDump dumper = new PNGDump();
      dumper.dumpComponent(new File(filename), itsMainPanel);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /** dummy class for catching autodump timer events. */
  public class DummyAutodump implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      dumpPNG(itsDumpFile);
    }
  };

  /** Configure autodump mode, use period "0" to disable. */
  public void setAutodump(int interval, String dumpfile) {
    itsDumpFile = dumpfile;
    dumpPNG(itsDumpFile);

    if (interval == 0 && itsTimer != null) {
      itsTimer.stop();
      itsTimer = null;
    } else {
      if (itsTimer != null) {
        itsTimer.stop();
      }
      itsTimer = new Timer(interval, new DummyAutodump());
      itsTimer.start();
    }
  }

  /**
   * Read a "saved state" file that describes what windows to create at
   * application start-up and how to arrange them.
   */
  public static boolean recoverArrangement(String filename) {
    try {
      // Record how many windows are currently open- we'll need to close
      // them
      int numalreadyopen = theirWindowManager.numWindows();
      BufferedReader f = new BufferedReader(new FileReader(filename));

      while (f.ready()) {
        String line = f.readLine();
        if (line.startsWith("#")) {
          continue;
        }
        StringTokenizer st = new StringTokenizer(line);
        // Get the requested setup from the database
        SavedSetup setup = MonClientUtil.getSetup("atnf.atoms.mon.gui.MonFrame", st.nextToken());
        // Parse the desired geometry
        int w = Integer.parseInt(st.nextToken());
        int h = Integer.parseInt(st.nextToken());
        int x = Integer.parseInt(st.nextToken());
        int y = Integer.parseInt(st.nextToken());
        // Make It So!
        if (setup != null) {
          MonFrame frame = new MonFrame();
          frame.loadSetup(setup);
          frame.setBounds(x, y, w, h);
          frame.validate();
        } else {
          return false;
        }
      }

      // Close the windows that were already open
      for (int i = 0; i < numalreadyopen; i++) {
        MonFrame frame = (MonFrame) theirWindowManager.itsWindows.get(0);
        theirWindowManager.actionPerformed(new ActionEvent(theirWindowManager, 0, "Close:" + frame.getNumber()));
        System.err.println("###REMOVED");
      }
    } catch (Exception e) {
      System.err.println("ERROR: Couldn't load saved arrangement \"" + filename + "\"");
      // e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Save a description of what windows are open and how they are arranged to
   * the specified file name.
   */
  public static boolean saveArrangement(String filename) {
    try {
      FileWriter fw = new FileWriter(filename, false);
      PrintWriter p = new PrintWriter(fw);
      for (int i = 0; i < WindowManager.itsWindows.size(); i++) {
        MonFrame frame = (MonFrame) WindowManager.itsWindows.get(i);
        p.print(frame.getSetup().getName() + "\t");
        Rectangle r = frame.getBounds();
        p.println(r.width + " " + r.height + " " + r.x + " " + r.y);
      }
      fw.flush();
      fw.close();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private static void usage() {
    System.err.println("\nUSAGE: monica [-d interval] [-o dir] [-f arrangement] [setup1] [setup2..]");
    System.err.println("-d\tDump screenshots to PNG images every \"interval\" seconds");
    System.err.println("-o\tDump the PNG images to this directory");
    System.err.println("-f\tLoad the following \"arrangement\" file at startup");
    System.err.println("setup\tName of setup(s) to be loaded, eg \"evironment.weather.summary\"\n");
    System.exit(1);
  }

  /** Simple test application. */
  public static void main(String[] argv) {
    ClockErrorMonitor.start();

    boolean autodump;
    int autodumpperiod = 60000;
    int numframes = 0;
    String outputdir = "";

    if (argv.length > 0) {
      for (int i = 0; i < argv.length; i++) {
        if (argv[i].equals("-h") || argv[i].equals("--help")) {
          usage();
        } else if (argv[i].equals("-d") || argv[i].equals("--dump")) {
          if (i > 0) {
            System.err.print("ERROR: \"" + argv[i] + "\" must be the first argument");
            System.exit(1);
          }
          autodump = true;
          if (argv.length == i + 1) {
            System.err.print("ERROR: You must specify an interval in " + "seconds for autodump mode! ");
            System.err.println("eg, " + argv[i] + " 60");
            System.exit(1);
          }
          try {
            autodumpperiod = 1000 * Integer.parseInt(argv[i + 1]);
          } catch (Exception e) {
            System.err.println("ERROR: You must specify an interval in " + "seconds for autodump mode!");
            System.err.println("\teg, " + argv[i] + " 60");
            System.exit(1);
          }
          autodump = true;
          i++;
        } else if (argv[i].equals("-o")) {
          // Output directory for autodumping
          if (argv.length == i + 1) {
            System.err.print("ERROR: You must specify the target directory after" + " the \"" + argv[i] + "\" argument");
            System.exit(1);
          }
          outputdir = argv[i + 1];
          if (!outputdir.endsWith("/")) {
            outputdir = outputdir + "/";
          }
          i++;
        } else if (argv[i].equals("-f") || argv[i].equals("--file")) {
          if (argv.length == i + 1) {
            System.err.print("ERROR: You must specify the file name after" + " the \"" + argv[i] + "\" argument");
            System.exit(1);
          }
          if (!recoverArrangement(argv[i + 1])) {
            System.err.println("ERROR: The arrangement file \"" + argv[i + 1] + "\" wasn't successfully loaded.");
            System.exit(1);
          }
          numframes++;
          i++;
        } else {
          // Interpret argument as an extra MonFrame with specified
          // page
          MonFrame frame = new MonFrame();
          SavedSetup def = MonClientUtil.getSetup("atnf.atoms.mon.gui.MonFrame", argv[i]);
          if (def != null) {
            frame.loadSetup(def);
            frame.pack();
            numframes++;
          } else {
            System.err.println("\nERROR: You asked me to display setup \"" + argv[i] + "\" but it doesn't exist!");
            usage();
          }
        }
      }
    }
    if (numframes == 0) {
      // Check to see if the user has a default arrangement file in their
      // home directory. Load it if they do.
      String osname = System.getProperty("os.name").toLowerCase();
      String monfile = null;
      if (osname.indexOf("win") != -1) {
        monfile = "\\Application Data\\MoniCA\\default-arrangement-" + MonClientUtil.getServerName() + ".txt";
      } else {
        monfile = "/.MoniCA/default-arrangement-" + MonClientUtil.getServerName() + ".txt";
      }
      monfile = System.getProperty("user.home") + monfile;
      if (!recoverArrangement(monfile)) {
        // Create an empty frame
        MonFrame frame = new MonFrame();
        // If there is a "default" page, then display it
        SavedSetup def = MonClientUtil.getSetup("atnf.atoms.mon.gui.MonFrame", "default");
        if (def != null) {
          frame.loadSetup(def);
        }
      }
    } else {
      // Apply the "autodump" settings
      if (autodumpperiod != 60000) {
        for (int i = 0; i < theirWindowManager.numWindows(); i++) {
          MonFrame framei = theirWindowManager.getWindow(i);
          framei.setAutodump(autodumpperiod, outputdir + framei.getSetup().getName() + ".png");
        }
      }
    }
  }
}
