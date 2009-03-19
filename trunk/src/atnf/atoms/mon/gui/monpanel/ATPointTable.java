//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import atnf.atoms.mon.*;
import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.time.*;

import java.util.Vector;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;


public class ATPointTable
extends MonPanel
{
  static {
    MonPanel.registerMonPanel("Point Table", ATPointTable.class);
  }


  ///////////////////////// NESTED CLASS ///////////////////////////////
  /** Nested class to provide GUI controls for configuring an ATTimeSeries
   * MonPanel. */
  public class
  ATPointTableSetupPanel
  extends MonPanelSetupPanel
  {
    /** PointNameSelector sub-class with a <i>Blank Row</i> button. */
    public class RowSelector
    extends PointNameSelector
    implements ActionListener
    {
      public RowSelector() {
        super("Select Rows");
        JButton tempbut = new JButton("Space");
        tempbut.setActionCommand("Blank-Row");
        tempbut.addActionListener(this);
        addButton(tempbut);
      }

      public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getActionCommand().equals("Blank-Row")) {
          Vector sel = getSelections();
          sel.add("-");
          setSelections(sel);
        }
      }
    }
    /** Widget to allow selection of the point names to display. */
    private PointNameSelector itsPointSelector = new RowSelector();

    /** Widget to allow source name selection. */
    private SourceChooser itsSourceChooser = null;

    /** Main panel for our setup components. */
    private JPanel itsMainPanel = new JPanel();

    /** Construct the setup editor for the specified panel. */
    public ATPointTableSetupPanel(ATPointTable panel, JFrame frame)
    {
      super(panel, frame);

      itsPointSelector.setPreferredSize(new Dimension(340, 150));
      itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.X_AXIS));
      itsMainPanel.add(itsPointSelector);
      itsSourceChooser = new SourceChooser(itsPointSelector, "Select Columns");
      itsSourceChooser.setPreferredSize(new Dimension(80, 150));
      itsMainPanel.add(itsSourceChooser);
      add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);

      //Display the current setup on the GUI
      if (itsInitialSetup!=null) showSetup(itsInitialSetup);
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
      SavedSetup ss = new SavedSetup();
      ss.setClass("atnf.atoms.mon.gui.monpanel.ATPointTable");
      ss.setName("temp");

      //Make a parsable string from the list of point names
      Vector points = itsPointSelector.getSelections();
      String p = "";
      if (points.size()>0) {
        p += points.get(0);
        //Then add rest of point names with a delimiter
        for (int i=1; i<points.size(); i++)
          p += ":" + points.get(i);
      }
      ss.put("points", p);

      //Make a parsable string from the list of source names
      Vector sources = itsSourceChooser.getSelections();
      String s = "";
      if (sources.size()>0) {
        s += sources.get(0);
        //Then add rest of point names with a delimiter
        for (int i=1; i<sources.size(); i++)
          s += ":" + sources.get(i);
      }
      ss.put("sources", s);

      return ss;
    }

    /** Configure the GUI to display the given setup.
     * @param setup The setup to display to the user. */
    protected
    void
    showSetup(SavedSetup setup)
    {
      itsInitialSetup = setup;
      if (setup==null) {
        System.err.println("ATPointTableSetupPanel:showSetup: Setup is NULL");
        return;
      }
      if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.ATPointTable")) {
        System.err.println("ATPointTableSetupPanel:showSetup: Setup is for wrong class");
        return;
      }

      String p = (String)setup.get("points");
      StringTokenizer stp = new StringTokenizer(p, ":");
      Vector points = new Vector(stp.countTokens());
      while (stp.hasMoreTokens())
        points.add(stp.nextToken());

      String s = (String)setup.get("sources");
      StringTokenizer sts = new StringTokenizer(s, ":");
      Vector sources = new Vector(sts.countTokens());
      while (sts.hasMoreTokens())
        sources.add(sts.nextToken());

      itsPointSelector.setSelections(points);
      itsSourceChooser.setSelections(sources);
    }
  }
  /////////////////////// END NESTED CLASS /////////////////////////////


  /** The Table to display on the panel. */
  JTable itsTable = null;

  /** The TableModel used for rendering monitor data. */
  PointTableModel itsModel = new PointTableModel();

  /** Scrolling panel used to contain the table. */
  JScrollPane itsScroll = null;


  /** C'tor. */
  public
  ATPointTable()
  {
    setLayout(new java.awt.BorderLayout());
    itsTable = new JTable(itsModel);
    itsTable.setDefaultRenderer(Object.class, itsModel);
    //Disable tooltips to prevent constant rerendering on mouse-over
    ToolTipManager.sharedInstance().unregisterComponent(itsTable);
    ToolTipManager.sharedInstance().unregisterComponent(itsTable.getTableHeader());

    //final JTable temptable = itsTable;
    itsTable.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2) {
          Point p = e.getPoint();
          int row = itsTable.rowAtPoint(p);
          int column = itsTable.columnAtPoint(p); // This is the view column!
          System.err.println("POINT: " + itsModel.getPoint(row, column));
          System.err.println("DOUBLE-CLICKED: "
               +itsModel.getPoint(row, column).getLongName()
               +" (" + itsModel.getPoint(row, column).getSource() + ")");
        }
      }
    });

    itsScroll = new JScrollPane(itsTable);
    itsScroll.setBackground(Color.lightGray);
    add(itsScroll);
  }


  /** Clear any current setup. */
  public 
  void
  blankSetup()
  {
    Vector p = new Vector();
    Vector s = new Vector();
    itsModel.set(p, s);
    itsModel.setSizes(itsTable, itsScroll);
  }


  /** Configure this MonPanel to use the specified setup. The setup will
   * specify sub-class-specific information, so this method can be used
   * to restore saved MonPanel states.
   * @param setup String containing sub-class-specific setup information.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if
   *         there was a problem and the setup cannot be used.
   */
  public synchronized
  boolean
  loadSetup(final SavedSetup setup)
  {
    try {
      //check if the setup is suitable for our class
      if (!setup.checkClass(this)) {
        System.err.println("ATPointTable:loadSetup: setup not for "
                            + this.getClass().getName());
        return false;
      }

      //the copy of the setup held by the frame is now incorrect
      if (itsFrame instanceof MonFrame) ((MonFrame)itsFrame).itsSetup=null;

      ///BUT WHAT DO THESE DO IF THERE ARE NO TOKENS?
      String p = (String)setup.get("points");
      StringTokenizer stp = new StringTokenizer(p, ":");
      Vector points = new Vector(stp.countTokens());
      while (stp.hasMoreTokens())
        points.add(stp.nextToken());

      String s = (String)setup.get("sources");
      StringTokenizer sts = new StringTokenizer(s, ":");
      Vector sources = new Vector(sts.countTokens());
      while (sts.hasMoreTokens())
        sources.add(sts.nextToken());

      //Configure our table to use the new setup
      itsModel.set(points, sources);
      itsModel.setSizes(itsTable, itsScroll);
    } catch (final Exception e) {
      if (itsFrame!=null) {
        JOptionPane.showMessageDialog(itsFrame,
              "The setup called \"" + setup.getName() + "\"\n" +
              "for class \"" + setup.getClassName() + "\"\n" +
              "could not be parsed.\n\n" +
              "The type of exception was:\n\"" +
              e.getClass().getName() + "\"\n\n",
              "Error Loading Setup",
              JOptionPane.WARNING_MESSAGE);
      } else {
        System.err.println("ATPointTable:loadData: " + e.getClass().getName());
      }
      blankSetup();
      return false;
    }

    return true;
  }


  /** Get the current sub-class-specific configuration for this MonPanel.
   * This can be used to capture the current state of the MonPanel so that
   * it can be easily recovered later.
   * @return String containing sub-class-specific configuration information.
   */
  public synchronized
  SavedSetup
  getSetup()
  {
    SavedSetup ss = new SavedSetup();
    ss.setClass(getClass().getName());
    ss.setName("temp");

    //Make a parsable string from the list of point names
    Vector points  = itsModel.getPoints();
    String p = "";
    if (points.size()>0) {
      p += points.get(0);
      //Then add rest of point names with a delimiter
      for (int i=1; i<points.size(); i++)
        p += ":" + points.get(i);
    }
    ss.put("points", p);

    //Make a parsable string from the list of source names
    Vector sources = itsModel.getSources();
    String s = "";
    if (sources.size()>0) {
      s += sources.get(0);
      //Then add rest of point names with a delimiter
      for (int i=1; i<sources.size(); i++)
        s += ":" + sources.get(i);
    }
    ss.put("sources", s);

    return ss;
  }


  /** Free all resources so that this MonPanel can disappear. */
  public
  void
  vaporise()
  {
    //Force the table model to unsubscribe from all points
    itsModel.set(null, null);
  }


  /** Get a panel with the controls required to configure this MonPanel.
   * @return GUI controls to configure this MonPanel. */
  public
  MonPanelSetupPanel
  getControls()
  {
    return new ATPointTableSetupPanel(this, itsFrame);
  }


  /** Dump current data to the given output stream. This is the mechanism
   * through which data can be exported to a file.
   * @param p The print stream to write the data to. */
  public synchronized
  void
  export(PrintStream p)
  {
    final String rcsid = "$Id: ATPointTable.java,v 1.6 2006/02/05 22:47:38 bro764 Exp $";
    p.println("#Dump from ATPointTable " + rcsid);
    p.println("#Data dumped at "
	      + (new AbsTime().toString(AbsTime.Format.UTC_STRING)));
    itsModel.export(p);
    p.println();
    p.println();
  }


  public String getLabel() { return null; }

  /** Basic test application. */
  public static void main(String[] argv) {
    JFrame frame = new JFrame("ATPointTable Test App");
//    frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

/*    SavedSetup seemon = new SavedSetup("temp",
				       "atnf.atoms.mon.gui.monpanel.ATPointTable",
				       "true:3:site.seemon.Lock1:site.seemon.Lock2:site.seemon.Lock3:1:seemon");

    ATPointTable pt = new ATPointTable();
    pt.loadSetup(seemon);
//    frame.getContentPane().add(pt);
    frame.setContentPane(pt);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
*/
/*    try {
      RelTime sleepy = RelTime.factory(15000000l);
      sleepy.sleep();
    } catch (Exception e) { e.printStackTrace(); }

    SavedSetup ss = pt.getSetup();
    pt.loadSetup(ss);*/
  }
}
