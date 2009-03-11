//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

//import atnf.atoms.mon.gui.monpanel.*;
//import atnf.atoms.mon.client.*;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.time.RelTime;

import java.util.Vector;
import java.util.StringTokenizer;
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;

/**
 * This is a tab that appears in each MonFrame window that allows the user
 * to control the size and layout of the different MonPanels that make up
 * the display.
 *
 * @author David Brodrick
 * @version $Id: $
 * @see MonFrame
 */
public class
LayoutPanel
extends JPanel
implements ActionListener
{
  /////////////////NESTED TABLE MODEL CLASS//////////////////
  private class
  LayoutTableModel
  extends AbstractTableModel
  {
    /** Left positions for each panel. */
    protected int[] itsLeftPos   = new int[0];
    /** Right positions for each panel. */
    protected int[] itsRightPos  = new int[0];
    /** Top positions for each panel. */
    protected int[] itsTopPos    = new int[0];
    /** Bottom positions for each panel. */
    protected int[] itsBottomPos = new int[0];

    LayoutTableModel()
    {
    }

    public
    String
    getColumnName(int col)
    {
      switch(col) {
      case 0: return "Panel";
      case 1: return "Left";
      case 2: return "Right";
      case 3: return "Top";
      case 4: return "Bottom";
      }
      return "ERROR";
    }

    /** Resize arrays to accomodate the change in the number of panels. */
    private
    void
    accomodateChanges()
    {
      /*Vector v = itsParent.getPanels();
      synchronized (v) {
	int newsize = v.size();
	int oldsize = itsLeftPos.length;

	int[] newleft   = new int[newsize];
	int[] newright  = new int[newsize];
	int[] newtop    = new int[newsize];
	int[] newbottom = new int[newsize];

	if (itsAutoControl.isSelected()) {
	  //We just use the numbers derived from panels preferred sizes
	  int toty = 0;
	  for (int i=0; i<v.size(); i++) {
	    toty += ((MonPanel)v.get(i)).getSize().height;
	  }

          int lastbot = 0;
	  for (int i=0; i<v.size(); i++) {
	    newleft[i] = 0;
	    newright[i] = 100;
	    newtop[i] = lastbot;
	    newbottom[i] = ((MonPanel)v.get(i)).getSize().height;
	    //newtop[i] = (int)(((float)lastbot)/toty);
	    //newbottom[i] = (int)(((float)(((MonPanel)v.get(i)).getSize().height))/toty);
	    //newbottom[i] = (int)(((float)(lastbot+((MonPanel)v.get(i)).getSize().height))/toty);
	    lastbot = newbottom[i]+1;
	  }
	} else {
	  //We need to let user specify the new numbers
	}

	itsLeftPos = newleft;
	itsRightPos = newright;
	itsTopPos = newtop;
        itsBottomPos = newbottom;
      } //synchronized*/
    }


    public
    int
    getRowCount()
    {
      return itsParent.getPanels().size();
    }

    public
    int
    getColumnCount()
    {
      return 5;
    }

    public
    Object
    getValueAt(int row, int col)
    {
      if (col==0) {
	Vector v = itsParent.getPanels();
	Class c = v.get(row).getClass();
        return MonPanel.getName(c);
      } else if (col==1) {
        return new Integer(itsLeftPos[row]);
      } else if (col==2) {
        return new Integer(itsRightPos[row]);
      } else if (col==3) {
        return new Integer(itsTopPos[row]);
      } else if (col==4) {
        return new Integer(itsBottomPos[row]);
      }
      return "UNKNOWN!";
    }

    public
    boolean
    isCellEditable(int row, int col)
    {
      if (col>0) return true;
      else return false;
    }

    /*  public
      void
      setValueAt(Object value, int row, int col)
      {
	//rowData[row][col] = value;
	fireTableCellUpdated(row, col);
      }*/

  }
  ////////////////////END NESTED CLASS///////////////////////

  /** The MonFrame to which this panel belongs. */
  protected MonFrame itsParent = null;
  /** Check box for selecting automatic layout control. */
  protected JRadioButton itsAutoControl = new JRadioButton("Automatic layout control");
  /** Check box for enabling manual layout control. */
  protected JRadioButton itsManualControl = new JRadioButton("Manual layout control");
  /** Text field for default width of window. */
  protected JTextField itsWindowWidth = new JTextField("500", 4);
  /** Text field for default height of window. */
  protected JTextField itsWindowHeight = new JTextField("500", 4);
  /** Button used to add a new panel to the setup. */
  protected JButton itsAddButton = new JButton("Add");
  /** Button for removing the selected panels from the display. */
  protected JButton itsRemoveSelected = new JButton("Remove Selected");
  /** Button for removing all panels. */
  protected JButton itsRemoveAll = new JButton("Remove All");
  /** Combo box for selecting what kind of MonPanel to add. */
  protected JComboBox itsPanelType = null;
  /** Reference to the TableModel. */
  protected LayoutTableModel itsTableModel = new LayoutTableModel();
  /** Reference to the Table display itself. */
  protected JTable itsTable = null;


  /** Constructor. */
  public
  LayoutPanel(MonFrame parent)
  {
    itsParent = parent;
    //Use a border layout with main layout control info in the centre
    setLayout(new BorderLayout());

    //Add the different panel options to the "add new panel" combo box
    String[] paneltypes = MonPanel.getNames();
    itsPanelType = new JComboBox(paneltypes);
    itsPanelType.setPreferredSize(new Dimension(150, 25));
    itsPanelType.setMaximumSize(new Dimension(230, 25));

    //Radio buttons for selecting between manual and auto layout control
    itsAutoControl.addActionListener(this);
    itsAutoControl.setActionCommand("auto");
    itsManualControl.addActionListener(this);
    itsManualControl.setActionCommand("manual");
    ButtonGroup group = new ButtonGroup();
    group.add(itsAutoControl);
    group.add(itsManualControl);
    itsAutoControl.setSelected(true);
    JPanel temppanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx=0;
    c.gridy=0;
    c.gridwidth=10;
    c.anchor=GridBagConstraints.LINE_START;
    temppanel.add(itsAutoControl,c);
    c.gridy=1;
    temppanel.add(itsManualControl,c);
    //Horizontal panel asking for default window size
    c.gridx=0;
    c.gridy=4;
    c.gridwidth=5;
    temppanel.add(new JLabel("Default size of window (in pixels): Width: "), c);
    c.gridx=7;
    c.gridwidth=1;
    temppanel.add(itsWindowWidth, c);
    c.gridx=8;
    temppanel.add(new JLabel(" Height: "), c);
    c.gridx=9;
    temppanel.add(itsWindowHeight, c);
    add(temppanel, BorderLayout.NORTH);

    //Create the main panel that contains the bulk of the controls
    JPanel mainpanel = new JPanel();
    mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));


    temppanel = new JPanel();
    temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
    JPanel temppanel2 = new JPanel();
    temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
    temppanel2.add(itsRemoveSelected);
    temppanel2.add(itsRemoveAll);
    temppanel.add(temppanel2);
    temppanel2 = new JPanel();
    temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
    temppanel2.add(itsAddButton);
    temppanel2.add(new JLabel(" a new panel of this type: "));
    temppanel2.add(itsPanelType);
    temppanel.add(temppanel2);
    add(temppanel, BorderLayout.SOUTH);

    //Add the table itself
    itsTable = new JTable(itsTableModel);
    add(new JScrollPane(itsTable), BorderLayout.CENTER);

    itsAutoControl.doClick();
  }


  /** Called by the MonFrame when a panel is added or removed. This provides
   * an opportunity for us to update the table display. */
  public
  void
  update()
  {
    itsTableModel.accomodateChanges();
    itsTableModel.fireTableStructureChanged();
    if (itsAutoControl.isSelected()) {
      //Copy the current window size to the fields
      Dimension d = itsParent.getSize();
      itsWindowWidth.setText(""+d.width);
      itsWindowHeight.setText(""+d.height);
    }
  }

  public
  void
  actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();
    if (cmd==null) {
      System.err.println("LayoutPanel:actionPerformed: ERK! Got null command!");
    } else if (cmd=="auto") {
      itsWindowWidth.setEnabled(false);
      itsWindowHeight.setEnabled(false);
      itsTable.setEnabled(false);
    } else if (cmd=="manual") {
      itsWindowWidth.setEnabled(true);
      itsWindowHeight.setEnabled(true);
      itsTable.setEnabled(true);
    }
  }


  /** Get a setup describing the selected layout control options. */
  public
  SavedSetup
  getSetup()
  {
    SavedSetup setup = new SavedSetup();

    return setup;
  }


  /** Configure the GUI to display the setup options described in the
   * specified setup. */
  public
  void
  loadSetup(SavedSetup setup)
  {

  }

}
