//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MonitorUtils;
import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.time.*;
import atnf.atoms.util.Angle;

import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

/**
 * Display monitor point values against time in a table.
 * 
 * <P>
 * When real-time mode is enabled this class uses the <i>DataMaintainer</i> to stay current with any changes to the monitor point
 * values.
 * 
 * <P>
 * The <i>PointData</i> objects for all monitor point updates are stored in the field <i>itsData</i>. When it is time to update the
 * table we call the method <i>processData</i> which looks at what options were selected by the user and organises <i>itsData</i>
 * into appropriate table rows. The rows are stored in the field <i>itsRows</i>, which is accessed whenever swing wants to redraw
 * any part of the table.
 * 
 * @author David Brodrick
 */
public class HistoryTable extends MonPanel implements PointListener, Runnable, TableCellRenderer {
  static {
    MonPanel.registerMonPanel("History Table", HistoryTable.class);
  }

  // /////////////////////// NESTED CLASS ///////////////////////////////
  /** Nested class to provide GUI controls for configuring the MonPanel. */
  public class HistoryTableSetupPanel extends MonPanelSetupPanel {
    /** Widget to allow selection of the points to display. */
    private PointSourceSelector itsPointSelector = new PointSourceSelector();
    /** Text field hold the history time span. */
    protected JTextField itsPeriod = new JTextField("30", 6);
    /** Options for the time period selection combo box. */
    final private String[] itsTimeOptions = { "Minutes", "Hours", "Days" };
    /** combo box to give time unit selection */
    protected JComboBox itsPeriodUnits = new JComboBox(itsTimeOptions);
    /** Options for the time zone selection. */
    final private String[] itsTimeZones = { "Local", "UTC" };
    /** The timezone string corresponding to the timezones. */
    final private String[] itsTimeZoneNames = { "local", "GMT+00" };
    /** Combo box for choosing timezones. */
    protected JComboBox itsZone = new JComboBox(itsTimeZones);
    /** Records if the graph should update in real-time. */
    protected JRadioButton itsDynamic = new JRadioButton("Real-Time Mode");
    /** Records if the graph should remain static. */
    protected JRadioButton itsStatic = new JRadioButton("Archival Mode. Start Time (yyyy/MM/dd HH:mm):");
    /** Allows user to enter graph start time for archival mode. */
    protected JTextField itsStart = new JTextField(16);
    /** The format of the dates we parse. */
    protected SimpleDateFormat itsFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    /** New row for every change/update? */
    protected JRadioButton itsNewRow = new JRadioButton("New row generated when ANY monitor point updates");
    /** Or new row when first point updates? */
    protected JRadioButton itsFirstRow = new JRadioButton("New row generated when FIRST SELECTED monitor point updates");
    /** Or row for each update interval? */
    protected JRadioButton itsSharedRow = new JRadioButton("New row generated every ");
    /** Interval between rows (in seconds), if enabled. */
    protected JTextField itsRowInterval = new JTextField("10", 3);
    /** Don't show rows if there's been no changes?. */
    protected JCheckBox itsChangesOnly = new JCheckBox("Hide repeated rows (rows with no changes to data values)");
    /** Should we hide incomplete rows? */
    protected JCheckBox itsHideIncomplete = new JCheckBox("Hide incomplete rows (rows where no data is available for one or more points)");
    /** Should a sparse table be shown? */
    protected JCheckBox itsSparse = new JCheckBox("Sparse table (don't plot values in columns that didn't change)");
    /** Will we place a limit on the number of rows displayed? */
    protected JCheckBox itsLimit = new JCheckBox("Limit number of rows to ");
    /** Max number of rows to be displayed. */
    protected JTextField itsMaxRows = new JTextField("10", 6);
    /** Suppress the date in data timestamps? */
    protected JCheckBox itsNoDate = new JCheckBox("Suppress date in timestamps");
    /** Suppress the milliseconds in data timestamps? */
    protected JCheckBox itsNoMS = new JCheckBox("Suppress milliseconds in timestamps");
    /** Should things be shown in chronological order? */
    protected JCheckBox itsReverse = new JCheckBox("Chronological order (default is reverse chronological order)");

    /** Main panel for our setup components. */
    private JPanel itsMainPanel = new JPanel();

    /** Construct the setup editor for the specified panel. */
    public HistoryTableSetupPanel(HistoryTable panel, JFrame frame) {
      super(panel, frame);

      itsDynamic.addActionListener(this);
      itsDynamic.setActionCommand("Dynamic");
      itsStatic.addActionListener(this);
      itsStatic.setActionCommand("Static");
      itsNewRow.addActionListener(this);
      itsNewRow.setActionCommand("NewRow");
      itsFirstRow.addActionListener(this);
      itsFirstRow.setActionCommand("FirstRow");
      itsSharedRow.addActionListener(this);
      itsSharedRow.setActionCommand("SharedRow");
      itsLimit.addActionListener(this);
      itsLimit.setActionCommand("Limit");
      itsMaxRows.addActionListener(this);
      itsMaxRows.setEnabled(false);
      itsMaxRows.setBackground(Color.lightGray);

      itsStart.addActionListener(this);
      itsPeriod.addActionListener(this);
      itsRowInterval.addActionListener(this);

      ButtonGroup tempgroup = new ButtonGroup();
      tempgroup.add(itsDynamic);
      tempgroup.add(itsStatic);
      itsDynamic.doClick();

      tempgroup = new ButtonGroup();
      tempgroup.add(itsNewRow);
      tempgroup.add(itsSharedRow);
      tempgroup.add(itsFirstRow);
      // itsSharedRow.doClick();
      itsFirstRow.doClick();

      itsChangesOnly.setSelected(true);
      itsNoDate.setSelected(true);
      itsNoMS.setSelected(true);
      itsHideIncomplete.setSelected(true);

      itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

      JPanel temppanel = new JPanel();
      temppanel.setLayout(new GridLayout(3, 1));
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      JPanel temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
      JLabel templabel = new JLabel("Data Time Span:  ");
      templabel.setForeground(Color.black);
      temppanel2.add(templabel);
      itsPeriod.setMaximumSize(new Dimension(60, 30));
      temppanel2.add(itsPeriod);
      itsPeriodUnits.setMaximumSize(new Dimension(90, 30));
      temppanel2.add(itsPeriodUnits);
      temppanel2.add(Box.createRigidArea(new Dimension(60, 0)));
      templabel = new JLabel("Time Zone:   ");
      templabel.setForeground(Color.black);
      temppanel2.add(templabel);
      itsZone.setMaximumSize(new Dimension(60, 30));
      temppanel2.add(itsZone);

      temppanel.add(temppanel2);
      temppanel.add(itsDynamic);
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
      temppanel2.add(itsStatic);
      itsStart.setText(itsFormatter.format(new Date()));
      itsStart.setMaximumSize(new Dimension(160, 30));
      temppanel2.add(itsStart);
      templabel = new JLabel();
      templabel.setPreferredSize(new Dimension(20, 20));
      temppanel2.add(templabel);
      temppanel.add(temppanel2);
      itsMainPanel.add(temppanel);

      temppanel = new JPanel();
      temppanel.setLayout(new GridLayout(3, 1));
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      temppanel.add(itsFirstRow);
      temppanel.add(itsNewRow);
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
      temppanel2.add(itsSharedRow);
      itsRowInterval.setMaximumSize(new Dimension(60, 30));
      temppanel2.add(itsRowInterval);
      templabel = new JLabel(" seconds");
      templabel.setForeground(Color.black);
      temppanel2.add(templabel);
      templabel = new JLabel("");
      temppanel2.add(templabel);
      temppanel.add(temppanel2);
      itsMainPanel.add(temppanel);

      temppanel = new JPanel();
      temppanel.setLayout(new GridLayout(6, 1));
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      temppanel.add(itsChangesOnly);
      temppanel.add(itsHideIncomplete);
      temppanel.add(itsNoDate);
      temppanel.add(itsNoMS);
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
      temppanel2.add(itsLimit);
      itsMaxRows.setMaximumSize(new Dimension(60, 30));
      temppanel2.add(itsMaxRows);
      templabel = new JLabel("");
      temppanel2.add(templabel);
      temppanel.add(temppanel2);
      temppanel.add(itsReverse);
      itsMainPanel.add(temppanel);

      itsPointSelector.setPreferredSize(new Dimension(340, 150));
      itsMainPanel.add(itsPointSelector);

      add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);

      // Display the current setup on the GUI
      if (itsInitialSetup != null) {
        showSetup(itsInitialSetup);
      }
    }

    /** Called when some GUI elements are selected by the user. */
    public void actionPerformed(ActionEvent e) {
      super.actionPerformed(e);
      String cmd = e.getActionCommand();
      if (e.getSource() == itsStart || e.getSource() == itsPeriod || e.getSource() == itsMaxRows || e.getSource() == itsRowInterval) {
        okClicked();
        return;
      }
      if (cmd.equals("Dynamic")) {
        itsStart.setEnabled(false);
        itsStart.setBackground(Color.lightGray);
      } else if (cmd.equals("Static")) {
        itsStart.setEnabled(true);
        itsStart.setBackground(Color.white);
      } else if (cmd.equals("NewRow") || cmd.equals("FirstRow")) {
        itsRowInterval.setEnabled(false);
        itsRowInterval.setBackground(Color.lightGray);
      } else if (cmd.equals("SharedRow")) {
        itsRowInterval.setEnabled(true);
        itsRowInterval.setBackground(Color.white);
      } else if (cmd.equals("Limit")) {
        if (itsLimit.isSelected()) {
          itsMaxRows.setEnabled(true);
          itsMaxRows.setBackground(Color.white);
        } else {
          itsMaxRows.setEnabled(false);
          itsMaxRows.setBackground(Color.lightGray);
        }
      } else {
        System.err.println("HistoryTable:SetupPanel:actionPerformed: " + cmd);
      }
    }

    /**
     * Return the current setup, as determined by the GUI controls. It provides the means of extracting the setup specified by the
     * user into a useable format.
     * 
     * @return SavedSetup specified by GUI controls, or <tt>null</tt> if no setup can be extracted from the GUI at present.
     */
    protected SavedSetup getSetup() {
      SavedSetup ss = new SavedSetup();
      ss.setClass("atnf.atoms.mon.gui.monpanel.HistoryTable");
      ss.setName("temp");

      // Make a parsable string from the list of point names
      Vector<String> points = itsPointSelector.getSelections();
      String p = "";
      if (points.size() > 0) {
        p += points.get(0);
        // Then add rest of point names with a delimiter
        for (int i = 1; i < points.size(); i++) {
          p += ":" + points.get(i);
        }
      }
      ss.put("points", p);

      if (itsHideIncomplete.isSelected()) {
        ss.put("hidei", "true");
      } else {
        ss.put("hidei", "false");
      }

      if (itsNoDate.isSelected()) {
        ss.put("nodate", "true");
      } else {
        ss.put("nodate", "false");
      }

      if (itsReverse.isSelected()) {
        ss.put("reverse", "true");
      } else {
        ss.put("reverse", "false");
      }

      if (itsNoMS.isSelected()) {
        ss.put("noms", "true");
      } else {
        ss.put("noms", "false");
      }

      if (itsSparse.isSelected()) {
        ss.put("sparse", "true");
      } else {
        ss.put("sparse", "false");
      }

      if (itsLimit.isSelected()) {
        try {
          int dummy = Integer.parseInt(itsMaxRows.getText());
          if (dummy <= 0) {
            throw new Exception();
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this, "The field for maximum number of rows\n" + "to be displayed must contain a valid\n"
              + "integer greater than zero. eg \"20\"\n", "Invalid Maximum Rows", JOptionPane.WARNING_MESSAGE);
          return null;
        }
        ss.put("limit", "true");
        ss.put("maxrows", "" + itsMaxRows.getText());
      } else {
        ss.put("limit", "false");
      }

      if (itsNewRow.isSelected()) {
        ss.put("rowmode", "new");
      } else if (itsFirstRow.isSelected()) {
        ss.put("rowmode", "first");
      } else {
        ss.put("rowmode", "shared");
        int dummy = 10;
        try {
          dummy = Integer.parseInt(itsRowInterval.getText());
          if (dummy <= 0) {
            throw new Exception();
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this, "The field that specifies how many seconds\n" + "between producing new rows, must be a valid\n"
              + "integer greater than zero. eg \"10\"\n", "Time Interval Between Rows", JOptionPane.WARNING_MESSAGE);
          return null;
        }
        ss.put("rowperiod", "" + (dummy * 1000000l));
      }

      if (itsChangesOnly.isSelected()) {
        ss.put("changesonly", "true");
      } else {
        ss.put("changesonly", "false");
      }

      // Check that the numeric period field and save it, if okay
      try {
        Double.parseDouble(itsPeriod.getText());
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "The field for the Data Time Span must contain\n" + "a valid number, eg, \"30\" or \"1.5\".\n",
            "Bad Period Entered", JOptionPane.WARNING_MESSAGE);
        return null;
      }
      double numtime = Double.parseDouble(itsPeriod.getText());
      String units = (String) itsPeriodUnits.getSelectedItem();
      if (units.equals("Minutes")) {
        numtime *= 60000000l;
      } else if (units.equals("Hours")) {
        numtime *= 60 * 60000000l;
      } else if (units.equals("Days")) {
        numtime *= 24 * 60 * 60000000l;
      }
      ss.put("period", "" + (long) numtime);

      // Save the timezone, if one has been specified
      if (itsTimeZoneNames[itsZone.getSelectedIndex()].equals("local")) {
        ss.put("timezone", "local");
        itsFormatter.setTimeZone(TimeZone.getDefault());
      } else {
        ss.put("timezone", itsTimeZoneNames[itsZone.getSelectedIndex()]);
        itsFormatter.setTimeZone(TimeZone.getTimeZone(itsTimeZoneNames[itsZone.getSelectedIndex()]));
      }

      if (itsDynamic.isSelected()) {
        ss.put("mode", "dynamic");
      } else if (itsStatic.isSelected()) {
        ss.put("mode", "static");
        String startstr = itsStart.getText();
        Date date = null;
        try {
          date = itsFormatter.parse(startstr);
        } catch (Exception e) {
          date = null;
        }
        if (date == null) {
          JOptionPane.showMessageDialog(this, "The Start Time you entered could\n" + "not be parsed. The time must be\n"
              + "in \"yyyy/MM/dd HH:mm\" format, eg:\n" + "\"" + itsFormatter.format(new Date()) + "\"\n", "Bad Start Time", JOptionPane.WARNING_MESSAGE);
          return null;
        }
        AbsTime start = AbsTime.factory(date);
        ss.put("start", "" + start.getValue());
      } else {
        return null;

      }
      return ss;
    }

    /**
     * Configure the GUI to display the given setup.
     * 
     * @param setup
     *          The setup to display to the user.
     */
    protected void showSetup(SavedSetup setup) {
      itsInitialSetup = setup;

      if (setup == null) {
        System.err.println("HistoryTableSetupPanel:showSetup: Setup is NULL");
        return;
      }
      if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.HistoryTable")) {
        System.err.println("HistoryTableSetupPanel:showSetup: Setup is for wrong class");
        return;
      }

      String temp = (String) setup.get("mode");
      if (temp == null || temp.equals("dynamic")) {
        itsDynamic.doClick();
      } else {
        itsStatic.doClick();
        temp = (String) setup.get("start");
        try {
          long tlong = Long.parseLong(temp);
          AbsTime ttime = AbsTime.factory(tlong);
          Date tdate = ttime.getAsDate();
          itsStart.setText(itsFormatter.format(tdate));
        } catch (Exception e) {
          itsStart.setText("ERROR");
        }
      }

      // graph time span
      temp = (String) setup.get("period");
      long period = Long.parseLong(temp);
      if (period >= 2 * 86400000000l) {
        // Best displayed in days
        itsPeriodUnits.setSelectedItem("Days");
        itsPeriod.setText("" + (period / 86400000000l));
      } else if (period >= 2 * 3600000000l) {
        // Best displayed in hours
        itsPeriodUnits.setSelectedItem("Hours");
        itsPeriod.setText("" + (period / 3600000000l));
      } else {
        // Let's show it in minutes
        itsPeriodUnits.setSelectedItem("Minutes");
        itsPeriod.setText("" + (period / 60000000l));
      }

      // timezone
      temp = (String) setup.get("timezone");
      if (temp == null || temp.equals("local")) {
        // Default timezone
        itsZone.setSelectedIndex(0);
      } else {
        // Specified timezone
        boolean foundit = false;
        for (int i = 1; i < itsTimeZones.length; i++) {
          if (itsTimeZoneNames[i].equals(temp)) {
            foundit = true; // Found the specified timezone
            itsZone.setSelectedIndex(i);
          }
        }
        if (!foundit) {
          System.out.println("HistoryTable:HistoryTableSetupPanel:showSetup: Unknown timezone");
          itsZone.setSelectedIndex(0);
        }
      }

      temp = (String) setup.get("limit");
      if (temp == null || temp.equals("false")) {
        itsLimit.setSelected(true);
        itsLimit.doClick();
      } else {
        itsLimit.setSelected(false);
        itsLimit.doClick();
        temp = (String) setup.get("maxrows");
        try {
          long tlong = Long.parseLong(temp);
          itsMaxRows.setText("" + tlong);
        } catch (Exception e) {
          itsMaxRows.setText("10");
        }
      }

      temp = (String) setup.get("hidei");
      if (temp == null || temp.equals("true")) {
        itsHideIncomplete.setSelected(true);
      } else {
        itsHideIncomplete.setSelected(false);
      }

      temp = (String) setup.get("noms");
      if (temp == null || temp.equals("true")) {
        itsNoMS.setSelected(true);
      } else {
        itsNoMS.setSelected(false);
      }

      temp = (String) setup.get("nodate");
      if (temp == null || temp.equals("true")) {
        itsNoDate.setSelected(true);
      } else {
        itsNoDate.setSelected(false);
      }

      temp = (String) setup.get("reverse");
      if (temp == null || temp.equals("false")) {
        itsReverse.setSelected(false);
      } else {
        itsReverse.setSelected(true);
      }

      temp = (String) setup.get("changesonly");
      if (temp == null || temp.equals("true")) {
        itsChangesOnly.setSelected(true);
      } else {
        itsChangesOnly.setSelected(false);
      }

      temp = (String) setup.get("rowmode");
      if (temp == null || temp.equals("first")) {
        itsFirstRow.doClick();
      } else if (temp.equals("new")) {
        itsNewRow.doClick();
      } else {
        itsSharedRow.doClick();
        temp = (String) setup.get("rowperiod");
        try {
          long tlong = Long.parseLong(temp);
          itsRowInterval.setText("" + tlong / 1000000);
        } catch (Exception e) {
          itsRowInterval.setText("10");
        }
      }

      String p = (String) setup.get("points");
      StringTokenizer stp = new StringTokenizer(p, ":");
      Vector<String> points = new Vector<String>(stp.countTokens());
      while (stp.hasMoreTokens()) {
        points.add(stp.nextToken());
      }

      itsPointSelector.setSelections(points);
    }
  }

  // ///////////////////// END NESTED CLASS /////////////////////////////
  /** The Table to display on the panel. */
  protected JTable itsTable = null;
  /** The TableModel used for rendering monitor data. */
  protected HistoryTableModel itsModel = new HistoryTableModel();
  /** Scrolling panel used to contain the table. */
  protected JScrollPane itsScroll = null;

  /** The setup we're currently using. */
  protected SavedSetup itsSetup = null;

  /** Contains the names of the monitor points we are displaying. */
  protected Vector<String> itsPoints = new Vector<String>();

  /** The time period that the table covers. */
  protected RelTime itsPeriod = RelTime.factory(600000000);
  /** Records whether we are running in real-time or archival mode. */
  protected boolean itsRealTime = true;
  /** If we are in archival mode, this records the specified start time. */
  protected AbsTime itsStartTime = null;
  /** Records if we add a new row whenever the first point updates. */
  protected boolean itsFirstRow = true;
  /** Records if we add a new row for every monitor point update. */
  protected boolean itsNewRow = true;
  /** Records if we add a new row every XX seconds. */
  protected boolean itsSharedRow = true;
  /** The time interval between generating rows. */
  protected RelTime itsRowInterval = RelTime.factory(10000000);
  /** Records if we should skip rows where there were no updates. */
  protected boolean itsSkipRows = true;
  /** Should we hide rows with incomplete data? */
  protected boolean itsHideIncomplete = true;
  /** Are we in sparse table mode (only show values that changed)? */
  protected boolean itsSparse = true;
  /** Should we limit the number of displayed rows? */
  protected boolean itsLimitRows = true;
  /** Maximum number of rows to be displayed. */
  protected int itsMaxRows = 10;
  /** Should the date component of time-stamps be suppressed? */
  protected boolean itsNoDate = true;
  /** Should the millisecond component of time-stamps be suppressed? */
  protected boolean itsNoMS = true;
  /** Should rows be shown in reverse chronological order? */
  protected boolean itsReverse = false;
  /** Each entry is a Vector containing the data for an individual row. */
  protected Vector<Vector<Object>> itsRows = new Vector<Vector<Object>>();
  /** Contains all the raw data for the monitor points. */
  protected Vector<Vector<PointData>> itsData = new Vector<Vector<PointData>>();
  /** Indicates that the worker thread should continue to run. */
  protected boolean itsKeepRunning = true;
  /** Indicates if we've just been configured to show a different setup. */
  protected boolean itsReInit = false;
  /** Timezone string to display data in, or <tt>null</tt> for local time. */
  protected String itsTimeZone = null;
  /** Actual TimeZone object to be used for display. */
  protected TimeZone itsTZ = null;
  /** Records if we're waiting on swing to update the GUI. */
  protected boolean itsGUIUpdatePending = false;

  /** C'tor. */
  public HistoryTable() {
    setLayout(new java.awt.BorderLayout());
    itsTable = new JTable(itsModel);
    itsTable.setDefaultRenderer(Object.class, this);
    // Disable tooltips to prevent constant rerendering on mouse-over
    ToolTipManager.sharedInstance().unregisterComponent(itsTable);
    ToolTipManager.sharedInstance().unregisterComponent(itsTable.getTableHeader());

    itsTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          Point p = e.getPoint();
          int row = itsTable.rowAtPoint(p);
          int column = itsTable.columnAtPoint(p);
          System.err.println("DOUBLE-CLICKED: " + row + ", " + column);
        }
      }
    });

    itsScroll = new JScrollPane(itsTable);
    itsScroll.setBackground(Color.lightGray);
    add(itsScroll);

    // Start the data collection thread
    new Thread(this).start();
  }

  /** Main loop for table-update thread. */
  public void run() {
    while (itsKeepRunning) {
      if (itsReInit) {
        itsReInit = false;
        Runnable clearTable = new Runnable() {
          public void run() {
            removeAll();
            add(new JLabel("Retrieving monitor data from server..", JLabel.CENTER));
            validate();
          }
        };
        try {
          SwingUtilities.invokeAndWait(clearTable);
        } catch (Exception e) {
          e.printStackTrace();
        }
        getInitialData();

        Runnable informProcessing = new Runnable() {
          public void run() {
            removeAll();
            add(new JLabel("Processing data from server..", JLabel.CENTER));
            validate();
          }
        };
        try {
          SwingUtilities.invokeAndWait(informProcessing);
        } catch (Exception e) {
          e.printStackTrace();
        }
        processData();

        Runnable updateTable = new Runnable() {
          public void run() {
            removeAll();
            add(itsScroll);
            itsModel.fireTableStructureChanged();
            repaint();
          }
        };
        try {
          SwingUtilities.invokeAndWait(updateTable);
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (itsRealTime) {
          DataMaintainer.subscribe(itsPoints, this);
        }
      }
      synchronized (this) {
        try {
          wait();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** Clear any current setup. */
  public void blankSetup() {
    // If currently real-time then unsubscribe from any points
    if (itsRealTime && itsPoints != null && itsPoints.size() > 0) {
      DataMaintainer.unsubscribe(itsPoints, this);
    }
    Runnable clearTable = new Runnable() {
      public void run() {
        removeAll();
        add(new JLabel("Select display options using the tabs", JLabel.CENTER));
        repaint();
      }
    };
    try {
      SwingUtilities.invokeAndWait(clearTable);
    } catch (Exception e) {
      e.printStackTrace();
    }
    itsTimeZone = null;
    itsTZ = null;
    System.out.println("Cleared HistoryTable current setup blankSetup");
  }

  /**
   * Configure this MonPanel to use the specified setup. The setup will specify sub-class-specific information, so this method can
   * be used to restore saved MonPanel states.
   * 
   * @param setup
   *          String containing sub-class-specific setup information.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if there was a problem and the setup cannot be used.
   */
  public synchronized boolean loadSetup(final SavedSetup setup) {
    try {
      // check if the setup is suitable for our class
      if (!setup.checkClass(this)) {
        System.err.println("HistoryTable:loadSetup: setup not for " + this.getClass().getName());
        return false;
      }

      // the copy of the setup held by the frame is now incorrect
      if (itsFrame instanceof MonFrame) {
        ((MonFrame) itsFrame).itsSetup = null;
      }

      // If currently real-time then unsubscribe from any points
      if (itsRealTime && itsPoints != null && itsPoints.size() > 0) {
        DataMaintainer.unsubscribe(itsPoints, this);
      }

      String temp = (String) setup.get("sparse");
      if (temp == null || temp.equals("true")) {
        itsSparse = true;
      } else {
        itsSparse = false;
      }

      temp = (String) setup.get("nodate");
      if (temp == null || temp.equals("true")) {
        itsNoDate = true;
      } else {
        itsNoDate = false;
      }

      temp = (String) setup.get("noms");
      if (temp == null || temp.equals("true")) {
        itsNoMS = true;
      } else {
        itsNoMS = false;
      }

      temp = (String) setup.get("mode");
      if (temp == null || temp.equals("dynamic")) {
        // Realtime/dynamic mode
        itsRealTime = true;
      } else {
        // Archival mode - need to read the start epoch
        itsRealTime = false;
        temp = (String) setup.get("start");
        itsStartTime = AbsTime.factory(Long.parseLong(temp));
      }

      temp = (String) setup.get("period");
      itsPeriod = RelTime.factory(Long.parseLong(temp));

      // Timezone
      temp = (String) setup.get("timezone");
      if (temp == null || temp.equals("local")) {
        itsTimeZone = "local";
        itsTZ = TimeZone.getDefault();
      } else {
        itsTimeZone = temp;
        itsTZ = TimeZone.getTimeZone(itsTimeZone);
      }

      temp = (String) setup.get("limit");
      if (temp.equals("true")) {
        itsLimitRows = true;
        temp = (String) setup.get("maxrows");
        itsMaxRows = Integer.parseInt(temp);
      } else {
        itsLimitRows = false;
      }

      temp = (String) setup.get("rowmode");
      if (temp.equals("new")) {
        itsNewRow = true;
        itsFirstRow = false;
        itsSharedRow = false;
      } else if (temp.equals("first")) {
        itsFirstRow = true;
        itsNewRow = false;
        itsSharedRow = false;
      } else {
        itsSharedRow = true;
        itsFirstRow = false;
        itsNewRow = false;
        temp = (String) setup.get("rowperiod");
        itsRowInterval = RelTime.factory(Long.parseLong(temp));
      }

      temp = (String) setup.get("hidei");
      if (temp != null && temp.equals("true")) {
        itsHideIncomplete = true;
      } else {
        itsHideIncomplete = false;
      }

      temp = (String) setup.get("changesonly");
      if (temp != null && temp.equals("true")) {
        itsSkipRows = true;
      } else {
        itsSkipRows = false;
      }

      temp = (String) setup.get("reverse");
      if (temp != null && temp.equals("true")) {
        itsReverse = true;
      } else {
        itsReverse = false;
      }

      String p = (String) setup.get("points");
      StringTokenizer stp = new StringTokenizer(p, ":");
      Vector<String> points = new Vector<String>(stp.countTokens());
      while (stp.hasMoreTokens()) {
        points.add(stp.nextToken());
      }

      itsPoints = points;

      TableColumn column = itsTable.getColumnModel().getColumn(0);
      int extra = 0;
      if (!itsNoMS) {
        extra += 30;
      }
      if (!itsNoDate) {
        extra += 70;
      }
      column.setPreferredWidth(100 + extra);
      column.setMaxWidth(250 + extra);
      column.setMinWidth(100);

      itsReInit = true;
      synchronized (this) {
        this.notifyAll();
      }
    } catch (final Exception e) {
      e.printStackTrace();
      if (itsFrame != null) {
        JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
            + "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
            JOptionPane.WARNING_MESSAGE);
      } else {
        System.err.println("HistoryTable:loadData: " + e.getClass().getName());
      }
      blankSetup();
      return false;
    }
    itsSetup = setup;

    return true;
  }

  /**
   * Get the current sub-class-specific configuration for this MonPanel. This can be used to capture the current state of the
   * MonPanel so that it can be easily recovered later.
   * 
   * @return String containing sub-class-specific configuration information.
   */
  public synchronized SavedSetup getSetup() {
    return itsSetup;
  }

  /** Free all resources so that this MonPanel can disappear. */
  public void vaporise() {
    itsKeepRunning = false;
    // If currently real-time then unsubscribe from any points
    if (itsRealTime && itsPoints != null && itsPoints.size() > 0) {
      DataMaintainer.unsubscribe(itsPoints, this);
    }
  }

  /** Called when we receive a real-time update from a monitor point. */
  public void onPointEvent(Object source, PointEvent evt) {
    PointData pd = evt.getPointData();
    String src = pd.getName();
    boolean foundit = false;
    for (int i = 0; i < itsPoints.size(); i++) {
      if (((String) itsPoints.get(i)).equals(src)) {
        itsData.get(i).add(pd);
        foundit = true;
      }
    }
    if (!foundit) {
      System.err.println("HistoryTable: got update for \"" + src + "\" but not subscribed to it");
    } else {
      if (!itsGUIUpdatePending) {
        // Don't alter the table data while the GUI is rendering it
        processData();
        itsGUIUpdatePending = true;
        Runnable updateTable = new Runnable() {
          public void run() {
            itsModel.fireTableDataChanged();
            itsGUIUpdatePending = false;
          }
        };
        try {
          SwingUtilities.invokeAndWait(updateTable);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Get a panel with the controls required to configure this MonPanel.
   * 
   * @return GUI controls to configure this MonPanel.
   */
  public MonPanelSetupPanel getControls() {
    return new HistoryTableSetupPanel(this, itsFrame);
  }

  /**
   * Dump current data to the given output stream. This is the mechanism through which data can be exported to a file.
   * 
   * @param p
   *          The print stream to write the data to.
   */
  public synchronized void export(PrintStream p) {
    p.println("#Dump from HistoryTable");
    p.println("#Data dumped at " + (new AbsTime().toString(AbsTime.Format.UTC_STRING)));
    for (int r = 0; r < itsRows.size(); r++) {
      Vector<Object> row = itsRows.get(r);
      AbsTime t = (AbsTime) row.get(0);
      p.print(t.toString(AbsTime.Format.HEX_BAT) + ", ");
      p.print(t.toString(AbsTime.Format.UTC_STRING));
      for (int c = 1; c < row.size(); c++) {
        p.print(", ");
        Object o = row.get(c);
        if (o != null) {
          if (o instanceof Angle) {
            p.print(((Angle) o).toString(Angle.Format.DEGREES));
          } else {
            p.print(o.toString());
          }
        }
      }
      p.println();
    }
    p.println();
    p.println();
  }

  /** Discards old data and organised current data into rows for the table. */
  public void processData() {
    AbsTime now = new AbsTime();
    RelTime per = itsPeriod.negate();
    AbsTime cutoff = now.add(per);
    // First go through and purge any expired data
    if (itsRealTime) {
      for (int i = 0; i < itsData.size(); i++) {
        Vector<PointData> thisdata = itsData.get(i); // Always 0
        if (thisdata.isEmpty()) {
          continue;
        }
        while (thisdata.size() > 0) {
          if (thisdata.get(0).getTimestamp().isBeforeOrEquals(cutoff)) {
            // Keep one point in the past for populating cells with no later updates
            if (thisdata.size() > 1 && thisdata.get(1).getTimestamp().isBeforeOrEquals(cutoff)) {
              thisdata.remove(0);
            } else {
              break;
            }
          } else {
            break;
          }
        }
      }
    }

    try {
      Vector<Vector<Object>> newrows = new Vector<Vector<Object>>();
      Vector<Object> lastrow = null;

      while (true) {
        lastrow = getNextRow(lastrow);
        if (lastrow == null) {
          break;
        }
        // Only display the row if it is within the time range
        if (((AbsTime) lastrow.get(0)).isAfterOrEquals(cutoff)) {
          newrows.add(lastrow);
        }
      }

      // Limit the number of rows, if requested by the user
      if (itsLimitRows) {
        while (newrows.size() > itsMaxRows) {
          newrows.remove(0);
        }
      }
      // We're finished processing the new data
      itsRows = newrows;

      // Keep track of which data is actually used in rows so we can prune unused data
      if (itsSkipRows) {
        HashSet<PointData> useddata = new HashSet<PointData>(1000, 1000);
        for (int i = 0; i < itsRows.size(); i++) {
          for (int j = 1; j < itsRows.get(i).size(); j++) {
            Object thisdatum = itsRows.get(i).get(j);
            if (thisdatum instanceof PointData) {
              // This data appears in a row, therefore record it as being used
              useddata.add((PointData) thisdatum);
              // System.err.println("Using " + thisdatum);
            }
          }
        }
        // Now traverse the data store pruning data that wasn't used
        for (int i = 0; i < itsData.size(); i++) {
          Vector<PointData> thisdata = itsData.get(i);
          // Don't remove latest data as we may still need that in the future
          for (int j = thisdata.size() - 2; j >= 0; j--) {
            PointData thisdatum = thisdata.get(j);
            if (!useddata.contains(thisdatum)) {
              //System.err.println("Can purge " + thisdatum);
              thisdata.remove(thisdatum);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Return the values for the next row based on time/data of previous row and user options.
   */
  public Vector<Object> getNextRow(Vector<Object> prevrow) {
    Vector<Object> res = null;
    boolean done = false;

    while (!done) {
      res = new Vector<Object>(itsPoints.size() + 1);

      AbsTime lasttime = null;
      if (prevrow != null) {
        lasttime = (AbsTime) prevrow.get(0);
      }

      AbsTime nexttime = null;
      if (itsFirstRow) {
        // Next row will correspond to the next update of column 1
        Vector<PointData> v = itsData.get(0);
        if (v == null || v.size() == 0) {
          return null;
        }
        if (lasttime == null) {
          // First row
          nexttime = v.firstElement().getTimestamp();
        } else {
          int nexti = MonitorUtils.getNextPointData(v, lasttime);
          if (nexti != -1) {
            // Found the next row
            nexttime = v.get(nexti).getTimestamp();
          }
        }
      } else if (itsNewRow) {
        // Next row will correspond to next update of any column
        AbsTime earliest = null;
        for (int i = 0; i < itsData.size(); i++) {
          Vector<PointData> v = itsData.get(i);

          if (v == null || v.size() == 0) {
            continue;
          }
          for (int j = 0; j < v.size(); j++) {
            PointData pd = (PointData) v.get(j);
            AbsTime thistime = pd.getTimestamp();
            if ((lasttime == null || thistime.isAfter(lasttime)) && (earliest == null || thistime.isBefore(earliest))) {
              earliest = thistime;
            }
          }
        }
        if (earliest == null) {
          return null; // No data to be displayed!
        }
        nexttime = earliest;
      } else {
        // Next row is controlled by user-defined time interval
        if (lasttime != null) {
          nexttime = lasttime.add(itsRowInterval);
        } else {
          // Start at first 10 second mark after first data
          AbsTime earliest = null;
          for (int i = 0; i < itsData.size(); i++) {
            Vector<PointData> v = itsData.get(i);
            if (v == null || v.size() == 0) {
              continue;
            }
            PointData pd = (PointData) v.get(0);
            AbsTime thistime = pd.getTimestamp();
            if (earliest == null || thistime.isBefore(earliest)) {
              earliest = thistime;
            }
          }
          if (earliest == null) {
            return null; // No data to be displayed!
          }
          nexttime = earliest;
        }
        if (itsRealTime) {
          AbsTime now = new AbsTime();
          if (now.isBefore(nexttime)) {
            return null;
          }
        } else {
          AbsTime cutoff = itsStartTime.add(itsPeriod);
          if (cutoff.isBefore(nexttime)) {
            return null;
          }
        }
      }

      if (nexttime == null) {
        return null;
      }

      res.add(nexttime);

      boolean founddata = false;
      for (int i = 0; i < itsData.size(); i++) {
        Vector<PointData> v = itsData.get(i);
        if (v == null || v.size() == 0) {
          res.add(null);
          continue;
        }
        int previ = MonitorUtils.getPrevEqualsPointData(v, nexttime);
        if (previ == -1) {
          res.add(null);
        } else {
          res.add(v.get(previ));
          founddata = true;
        }
      }
      if (!founddata) {
        res = null;
      }

      if (itsHideIncomplete) {
        boolean wasincomplete = false;
        for (int i = 1; i < res.size(); i++) {
          if (res.get(i) == null) {
            wasincomplete = true;
            prevrow = res;
            break;
          }
        }
        if (wasincomplete) {
          continue;
        }
      }

      if (itsSkipRows && res != null && prevrow != null) {
        boolean keepit = false;
        for (int i = 1; i < res.size(); i++) {
          Object data1 = null, data2 = null;
          if (res.get(i) != null && res.get(i) instanceof PointData)
            data1 = ((PointData) res.get(i)).getData();
          if (prevrow.get(i) != null && prevrow.get(i) instanceof PointData)
            data2 = ((PointData) prevrow.get(i)).getData();
          if (!compare(data1, data2)) {
            keepit = true;
            break;
          }
        }
        if (keepit) {
          break;
        } else {
          prevrow = res;
        }
      } else {
        break;
      }
    }

    if (itsSparse && prevrow != null) {
      Vector<Object> newres = new Vector<Object>(res.size());
      newres.add(res.get(0));
      for (int i = 1; i < res.size(); i++) {
        if (compare(res.get(i), prevrow.get(i))) {
          newres.add(null);
        } else {
          newres.add(res.get(i));
        }
      }
      res = newres;
    }

    return res;
  }

  /**
   * Compare two objects to see if they are "the same".
   * 
   * @return True if objects the same, False if they are different.
   */
  protected boolean compare(Object o1, Object o2) {
    if (o1 == null && o2 == null) {
      return true;
    }
    if (o1 == null || o2 == null) {
      return false;
    }
    if (o1 instanceof String && o2 instanceof String) {
      if (((String) o1).equals((String) o2)) {
        return true;
      } else {
        return false;
      }
    }
    if (o1 instanceof Number && o2 instanceof Number) {
      if (((Number) o1).doubleValue() == ((Number) o2).doubleValue()) {
        return true;
      } else {
        return false;
      }
    }
    if (o1 instanceof Angle && o2 instanceof Angle) {
      if (((Angle) o1).getValue() == ((Angle) o2).getValue()) {
        return true;
      } else {
        return false;
      }
    }
    if (o1 instanceof AbsTime && o2 instanceof AbsTime) {
      if (((AbsTime) o1).getValue() == ((AbsTime) o2).getValue()) {
        return true;
      } else {
        return false;
      }
    }
    if (o1 instanceof RelTime && o2 instanceof RelTime) {
      if (((RelTime) o1).getValue() == ((RelTime) o2).getValue()) {
        return true;
      } else {
        return false;
      }
    }
    if (o1 instanceof Boolean && o2 instanceof Boolean) {
      if (((Boolean) o1).booleanValue() == ((Boolean) o2).booleanValue()) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  /** Download archival data from the monitor server. */
  public void getInitialData() {
    AbsTime start, end;
    AbsTime now = new AbsTime();
    if (itsRealTime) {
      start = now.add(itsPeriod.negate());
      end = now;
    } else {
      start = itsStartTime;
      end = start.add(itsPeriod);
    }
    Vector<Vector<PointData>> alldata = new Vector<Vector<PointData>>();
    for (int i = 0; i < itsPoints.size(); i++) {
      try {
        Vector<PointData> v = MonClientUtil.getServer().getArchiveData(itsPoints.get(i), start, end);
        if (v != null) {
          alldata.add(v);
        } else {
          alldata.add(new Vector<PointData>());
        }
      } catch (Exception e) {
      }
    }
    // Try and get the last data before the start for populating sparsely archived points
    try {
      Vector<PointData> initialdata = MonClientUtil.getServer().getBefore(itsPoints, start);
      for (int i = 0; i < initialdata.size(); i++) {
        if (initialdata.get(i) != null && initialdata.get(i).getData() != null) {
          //System.err.println("Got initial data = " + initialdata.get(i));
          alldata.get(i).insertElementAt(initialdata.get(i), 0);
        }
      }
    } catch (Exception e) {
    }
    itsData = alldata;
  }

  public String getLabel() {
    return null;
  }

  public class HistoryTableModel extends AbstractTableModel {
    public int getRowCount() {
      return itsRows.size();
    }

    public int getColumnCount() {
      return 1 + itsPoints.size();
    }

    public Object getValueAt(int row, int column) {
      if (row >= itsRows.size()) {
        return "";
      }
      if (!itsReverse) {
        row = itsRows.size() - row - 1;
      }
      Vector<Object> r = itsRows.get(row);
      if (column >= r.size()) {
        return "";
      }
      Object data = r.get(column);
      if (data == null) {
        return "";
      }
      if (column == 0 && (itsNoDate || itsNoMS)) {
        // timestamp, might need manipulation
        Date thisdate = ((AbsTime) data).getAsDate();
        DateFormat outdfm = null;
        if (itsNoDate) {
          if (itsNoMS) {
            outdfm = new SimpleDateFormat("HH:mm:ss");
          } else {
            outdfm = new SimpleDateFormat("HH:mm:ss.SSS");
          }
        } else {
          outdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        if (itsTimeZone.equals("local")) {
          outdfm.setTimeZone(TimeZone.getDefault());
        } else {
          outdfm.setTimeZone(TimeZone.getTimeZone(itsTimeZone));
        }
        String s = outdfm.format(thisdate);
        // if (itsNoMS) s = s.substring(0, s.indexOf("."));
        // if (itsNoDate) s = s.substring(s.indexOf(" ")+1);
        data = s;
      }
      if (data instanceof String) {
        // JLabel representing Time value
        data = new JLabel((String) data);
      }
      return data;
    }

    public String getColumnName(int column) throws NullPointerException {
      String res = "COLUMN";
      String tz = new String("");

      try {
        if (itsTimeZone.equals("local")) {
          tz = "Local";
        } else {
          tz = "UTC";
        }
      } catch (Exception e) {
        // Timezone NULL
      }

      if (column == 0) {
        res = "Time (" + tz + ")";
      } else {
        // If "source" is the same for all monitor points then we don't
        // need
        // to include it in the column header
        boolean showsource = false;
        String firstsrc = ((String) itsPoints.get(0)).substring(0, ((String) itsPoints.get(0)).indexOf("."));
        for (int i = 1; i < itsPoints.size(); i++) {
          if (!firstsrc.equals(((String) itsPoints.get(i)).substring(0, ((String) itsPoints.get(i)).indexOf(".")))) {
            showsource = true;
            break;
          }
        }
        res = ((String) itsPoints.get(column - 1)).substring(((String) itsPoints.get(column - 1)).lastIndexOf(".") + 1);
        if (showsource) {
          String thissrc = ((String) itsPoints.get(column - 1)).substring(0, ((String) itsPoints.get(column - 1)).indexOf("."));
          res = res + " (" + thissrc + ")";
        }
      }

      return res;
    }
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component res = null;
    if (value == null) {
      return null;
    }

    if (value instanceof Component) {
      res = (Component) value;
    } else if (value instanceof String) {
      res = new JLabel((String) value);
    } else {
      PointData pd = (PointData) value;
      Object dataval = pd.getData();
      if (dataval != null) {
        res = new JLabel(dataval.toString());
      } else {
        res = new JLabel("?");
      }
      if (pd.getAlarm()) {
        // Highlight this cell as the value was out of range
        res.setForeground(Color.red);
        if (res instanceof JComponent) {
          ((JComponent) res).setOpaque(true);
        }
        res.setBackground(Color.yellow);
      }
    }

    return res;
  }

  /** Get the preferred size of the panel. */
  public Dimension getPreferredSize() {
    if (itsLimitRows) {
      return new Dimension(400, 20 + 20 * itsMaxRows);
    } else {
      return itsTable.getPreferredSize();
    }
  }

  /** Basic test application. */
  public static void main(String[] argv) {
    // JFrame frame = new JFrame("HistoryTable Test App");
    // frame.getContentPane().setLayout(new
    // BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

    /*
     * SavedSetup seemon = new SavedSetup("temp", "atnf.atoms.mon.gui.monpanel.HistoryTable",
     * "true:3:site.seemon.Lock1:site.seemon.Lock2:site.seemon.Lock3:1:seemon" );
     * 
     * HistoryTable pt = new HistoryTable(); pt.loadSetup(seemon); // frame.getContentPane().add(pt); frame.setContentPane(pt);
     * 
     * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.pack(); frame.setVisible(true);
     */
    /*
     * try { RelTime sleepy = RelTime.factory(15000000l); sleepy.sleep(); } catch (Exception e) { e.printStackTrace(); }
     * 
     * SavedSetup ss = pt.getSetup(); pt.loadSetup(ss);
     */
  }
}
