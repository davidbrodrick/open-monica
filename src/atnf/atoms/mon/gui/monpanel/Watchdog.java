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

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.sound.sampled.*;

public class Watchdog extends MonPanel implements PointListener, ActionListener {
  static {
    MonPanel.registerMonPanel("Watchdog", Watchdog.class);
  }

  // /////////////////////// NESTED CLASS ///////////////////////////////
  /** Nested class to provide GUI controls for configuring the watchdog */
  public class WatchdogSetupPanel extends MonPanelSetupPanel {
    /** Widget to allow selection of the points to display. */
    private PointSourceSelector itsPointSelector = new PointSourceSelector();

    /** Main panel for our setup components. */
    private JPanel itsMainPanel = new JPanel();

    /** Check box for whether unavailable data is an alarm condition. */
    private JCheckBox itsStaleBad = new JCheckBox("Unavailable data is an alarm condition.");

    /** Spinner for selecting inertia. */
    private JSpinner itsInertia = new JSpinner();

    /** Construct the setup editor for the specified panel. */
    public WatchdogSetupPanel(Watchdog panel, JFrame frame) {
      super(panel, frame);

      itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

      JPanel temppan = new JPanel();
      temppan.setLayout(new BoxLayout(temppan, BoxLayout.X_AXIS));
      temppan.add(itsStaleBad);
      itsInertia.setValue(new Integer(3));
      itsInertia.setMaximumSize(new Dimension(45, 25));
      temppan.add(new JLabel("Updates before alarm is raised:"));
      temppan.add(itsInertia);
      itsMainPanel.add(temppan);

      itsPointSelector.setPreferredSize(new Dimension(340, 150));
      itsMainPanel.add(itsPointSelector);

      add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);

      // Display the current setup on the GUI
      if (itsInitialSetup != null) {
        showSetup(itsInitialSetup);
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
      ss.setClass("atnf.atoms.mon.gui.monpanel.Watchdog");
      ss.setName("temp");

      // Make a parsable string from the list of point names
      Vector points = itsPointSelector.getSelections();
      String p = "";
      if (points.size() > 0) {
        p += points.get(0);
        // Then add rest of point names with a delimiter
        for (int i = 1; i < points.size(); i++) {
          p += ":" + points.get(i);
        }
      }
      ss.put("points", p);

      if (itsStaleBad.isSelected()) {
        ss.put("stale", "true");
      } else {
        ss.put("stale", "false");
      }

      ss.put("inertia", itsInertia.getValue().toString());
      if (((Integer) itsInertia.getValue()).intValue() <= 0) {
        JOptionPane.showMessageDialog(this, "The number of out-of-range update\n" + "cycles before an alarm is raised must\n" + "be greater than zero!\n",
            "Bad Value for Parameter", JOptionPane.WARNING_MESSAGE);
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
        System.err.println("WatchdogSetupPanel:showSetup: Setup is NULL");
        return;
      }
      if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.Watchdog")) {
        System.err.println("WatchdogSetupPanel:showSetup: Setup is for wrong class");
        return;
      }

      String p = (String) setup.get("points");
      StringTokenizer stp = new StringTokenizer(p, ":");
      Vector<String> points = new Vector<String>(stp.countTokens());
      while (stp.hasMoreTokens()) {
        points.add(stp.nextToken());
      }
      itsPointSelector.setSelections(points);

      String i = (String) setup.get("inertia");
      if (i != null) {
        itsInertia.setValue(new Integer(i));
      }

      String s = (String) setup.get("inertia");
      if (s != null) {
        if (s.equals("true")) {
          itsStaleBad.setSelected(true);
        } else {
          itsStaleBad.setSelected(false);
        }
      }
    }
  }

  // ///////////////////// END NESTED CLASS /////////////////////////////

  // /////////////////////// NESTED CLASS ///////////////////////////////
  /** Nested class to render the tables. */
  public class WatchdogTableModel extends AbstractTableModel implements TableCellRenderer {
    /** Names of all PointInteractions to be displayed in the table. */
    protected Vector<String> itsPoints = new Vector<String>();

    /** Hash mapping of the current values for each point. */
    protected HashMap<String, PointData> itsValues = new HashMap<String, PointData>();

    /** The table this model is associated with. */
    protected JTable itsTable = null;

    /** Constructor. */
    public WatchdogTableModel() {
      super();
    }

    /** Set the table this model is associated with. */
    public void setTable(JTable table) {
      itsTable = table;
    }

    /** Set the column sizes. */
    public void setSizes() {
      TableColumn column = itsTable.getColumnModel().getColumn(0);
      column.setPreferredWidth(85);
      column.setMaxWidth(120);
      column.setMinWidth(60);
      column = itsTable.getColumnModel().getColumn(1);
      column.setPreferredWidth(250);
      column.setMinWidth(150);
      column = itsTable.getColumnModel().getColumn(2);
      column.setPreferredWidth(60);
      column.setMinWidth(40);
    }

    /** Set the list of monitor points to display in this table. */
    public void setPoints(Vector<String> points) {
      itsPoints = points;
      fireTableStructureChanged();
    }

    /** Get the list of monitor points currently displayed in this table. */
    public Vector getPoints() {
      return itsPoints;
    }

    /** Add the given point to the table. */
    public void addPoint(String point) {
      synchronized (itsPoints) {
        itsPoints.add(point);
      }
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          fireTableStructureChanged();
        }
      });
    }

    /** Add the given point to the table with the current value. */
    public void addPoint(String point, PointData value) {
      synchronized (itsPoints) {
        itsPoints.add(point);
        itsValues.put(point, value);
      }
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          fireTableStructureChanged();
        }
      });
    }

    /** Remove the given point from the table. */
    public void removePoint(String point) {
      boolean wasupdated = false;
      synchronized (itsPoints) {
        int i = itsPoints.indexOf(point);
        if (i != -1) {
          itsPoints.remove(point);
          wasupdated = true;
        }
      }
      if (wasupdated) {
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            fireTableStructureChanged();
          }
        });        
      }
    }

    /** Remove the given point from the table and return its name. */
    public String removePoint(int point) {
      String res = null;
      synchronized (itsPoints) {
        if (point < 0 && point >= itsPoints.size()) {
          return null;
        }
        res = (String) itsPoints.get(point);
        itsPoints.remove(point);
      }
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          fireTableStructureChanged();
        }
      });
      return res;
    }

    /** Update the value for a given point. */
    public void updateValue(String point, PointData value) {
      synchronized (itsPoints) {
        itsValues.put(point, value);
        // If we care about this point than fire an event
        final int ourindex = itsPoints.indexOf(point);
        if (ourindex != -1) {
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              fireTableCellUpdated(ourindex, 0);
            }
          });
        }
      }
    }

    /** Update table structure and set column widths. */
    public void fireTableStructureChanged() {
      super.fireTableStructureChanged();
      if (itsTable != null) {
        setSizes();
      }
    }

    /** Return the number of rows in the table. */
    public int getRowCount() {
      synchronized (itsPoints) {
        if (itsPoints == null) {
          return 0;
        } else {
          return itsPoints.size();
        }
      }
    }

    /** Return the number of columns in the table. */
    public int getColumnCount() {
      return 3;
    }

    /** Return the title for the specified column. */
    public String getColumnName(int column) {
      String res = null;
      if (column == 0) {
        res = "Value";
      } else if (column == 1) {
        res = "Point";
      } else if (column == 2) {
        res = "Source";
      }
      return res;
    }

    public Object getValueAt(int row, int column) {
      Object res = null;
      synchronized (itsPoints) {
        String pointname = (String) itsPoints.get(row);
        PointDescription pm = PointDescription.getPoint(pointname);
        if (column == 0) {
          PointData pd = (PointData) itsValues.get(pointname);
          if (pd == null) {
            res = "?";
          } else {
            res = pd.getData();
          }
        } else if (column == 1) {
          res = pm.getLongDesc();
        } else {
          res = pm.getSource();
        }
      }
      return res;
    }

    /** Get a GUI component for each cell - allows value highlightning. */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component res = null;
      synchronized (itsPoints) {
        if (value == null) {
          return null;
        }

        String pointname = (String) itsPoints.get(row);
        PointData pd = (PointData) itsValues.get(pointname);
        PointDescription pm = PointDescription.getPoint(pointname);
        if (pd == null || !pd.getAlarm()) {
          res = new JLabel(value.toString());
        } else {
          res = new JLabel(value.toString());
          if (res instanceof JComponent) {
            ((JComponent) res).setOpaque(true);
          }
          res.setForeground(Color.red);
          res.setBackground(Color.yellow);
        }

        // Make sure the highlight of the other columns matches the value's
        if (column == 0) {
          fireTableCellUpdated(row, 1);
          fireTableCellUpdated(row, 2);
        }
      }
      return res;
    }

    /** Return true if we're currently displaying points in the alarm state. */
    public boolean hasAlarms() {
      synchronized (itsPoints) {
        for (int i = 0; i < itsPoints.size(); i++) {
          String pointname = (String) itsPoints.get(i);
          PointData pd = (PointData) itsValues.get(pointname);
          if (pd == null || pd.getAlarm()) {
            return true;
          }
        }
        return false;
      }
    }
  }

  // ///////////////////// END NESTED CLASS /////////////////////////////

  // /////////////////////// NESTED CLASS ///////////////////////////////
  public class AudioWarning extends Thread {
    public void run() {
      RelTime sleep = RelTime.factory(10000000);
      while (!itsRemoved) {
        if (itsActiveModel.hasAlarms()) {
          playAudio("atnf/atoms/mon/gui/monpanel/watchdog.wav");
        }
        try {
          sleep.sleep();
        } catch (Exception e) {
        }
      }
    }
  }

  // ///////////////////// END NESTED CLASS /////////////////////////////

  /** The Table of current alarms. */
  JTable itsActiveTable = null;

  /** The TableModel used for current alarms. */
  WatchdogTableModel itsActiveModel = new WatchdogTableModel();

  /** The Table of silenced alarms. */
  JTable itsSilencedTable = null;

  /** The TableModel used for current alarms. */
  WatchdogTableModel itsSilencedModel = new WatchdogTableModel();

  /** All the points which are being monitored. */
  Vector<String> itsPoints = new Vector<String>();

  /** A textual label for the number of points we are monitoring. */
  JLabel itsSizeLabel = new JLabel("");

  /** How many updates must be in alarm state before we raise an alarm. */
  int itsInertia = 3;

  /** For recording how many updates have been in alarm state for each point. */
  HashMap<String, Integer> itsAlarmCounts = new HashMap<String, Integer>();

  /** True if unavailable data is considered an okay condition. */
  boolean itsStaleOK = true;

  /** Set to true when the watchdog has been removed. */
  boolean itsRemoved = false;

  /** C'tor. */
  public Watchdog() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    itsActiveTable = new JTable(itsActiveModel);
    itsActiveTable.setDefaultRenderer(Object.class, itsActiveModel);
    itsActiveModel.setTable(itsActiveTable);
    itsActiveModel.setSizes();
    itsActiveTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          Point p = e.getPoint();
          int row = itsActiveTable.rowAtPoint(p);
          String movepoint = itsActiveModel.removePoint(row);
          if (movepoint != null) {
            itsSilencedModel.addPoint(movepoint);
          }
        }
      }
    });

    itsSilencedTable = new JTable(itsSilencedModel);
    itsSilencedTable.setDefaultRenderer(Object.class, itsSilencedModel);
    itsSilencedModel.setTable(itsSilencedTable);
    itsSilencedModel.setSizes();
    itsSilencedTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          Point p = e.getPoint();
          int row = itsSilencedTable.rowAtPoint(p);
          String movepoint = itsSilencedModel.removePoint(row);
          if (movepoint != null) {
            itsActiveModel.addPoint(movepoint);
          }
        }
      }
    });

    JPanel temppan = new JPanel();
    temppan.setLayout(new BorderLayout());
    temppan.setPreferredSize(new Dimension(650, 22));
    JLabel templabel = new JLabel("Active Alarms:");
    templabel.setMaximumSize(new Dimension(200, 22));
    temppan.add(templabel, BorderLayout.WEST);
    JButton tempbut = new JButton("Silence All Current Alarms");
    tempbut.setActionCommand("silence");
    tempbut.addActionListener(this);
    tempbut.setMaximumSize(new Dimension(400, 22));
    temppan.add(tempbut, BorderLayout.EAST);
    temppan.setMaximumSize(new Dimension(2000, 23));
    add(temppan);

    JScrollPane tempscroll = new JScrollPane(itsActiveTable);
    tempscroll.setPreferredSize(new Dimension(650, 220));
    tempscroll.setBackground(Color.lightGray);
    add(tempscroll);

    temppan = new JPanel();
    temppan.setLayout(new BorderLayout());
    temppan.setPreferredSize(new Dimension(650, 22));
    templabel = new JLabel("Silenced Alarms:");
    templabel.setMaximumSize(new Dimension(200, 22));
    temppan.add(templabel, BorderLayout.WEST);
    tempbut = new JButton("Reactivate Silenced Alarms");
    tempbut.setActionCommand("reactivate");
    tempbut.addActionListener(this);
    tempbut.setMaximumSize(new Dimension(400, 22));
    temppan.add(tempbut, BorderLayout.EAST);
    temppan.setMaximumSize(new Dimension(2000, 23));
    add(temppan);

    tempscroll = new JScrollPane(itsSilencedTable);
    tempscroll.setPreferredSize(new Dimension(650, 220));
    tempscroll.setBackground(Color.lightGray);
    add(tempscroll);

    temppan = new JPanel();
    temppan.setLayout(new BoxLayout(temppan, BoxLayout.X_AXIS));
    itsSizeLabel.setText("This watchdog is monitoring " + itsPoints.size() + " points");
    temppan.add(itsSizeLabel, BorderLayout.CENTER);
    add(temppan);

    temppan = new JPanel();
    temppan.setLayout(new BoxLayout(temppan, BoxLayout.X_AXIS));
    templabel = new JLabel("Use double-click to silence/reactivate individual alarms");
    temppan.add(templabel, BorderLayout.CENTER);
    add(temppan);

    AudioWarning audiothread = new AudioWarning();
    audiothread.start();
  }

  /** For action events. */
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("silence")) {
      // Need to silence all currently active alarms
      Vector current = itsActiveModel.getPoints();
      while (current.size() > 0) {
        String thispoint = (String) current.get(0);
        itsActiveModel.removePoint(thispoint);
        itsSilencedModel.addPoint(thispoint);
      }
    } else if (e.getActionCommand().equals("reactivate")) {
      // Need to stop silencing all currently silenced alarms
      Vector weresilenced = itsSilencedModel.getPoints();
      itsSilencedModel.setPoints(new Vector<String>());
      for (int i = 0; i < weresilenced.size(); i++) {
        String thispoint = (String) weresilenced.get(i);
        itsActiveModel.addPoint(thispoint);
      }
    }
  }

  /** Clear any current setup. */
  public void blankSetup() {
    for (int i = 0; i < itsPoints.size(); i++) {
      DataMaintainer.unsubscribe((String) itsPoints.get(i), this);
    }
    itsPoints = new Vector<String>();
    itsActiveModel.setPoints(new Vector<String>());
    itsSilencedModel.setPoints(new Vector<String>());
    itsAlarmCounts = new HashMap<String, Integer>();
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
    // Clear the old setup
    blankSetup();

    try {
      // check if the setup is suitable for our class
      if (!setup.checkClass(this)) {
        System.err.println("WatchDog:loadSetup: setup not for " + this.getClass().getName());
        return false;
      }

      // Get the list of points to be monitored
      String p = (String) setup.get("points");
      StringTokenizer stp = new StringTokenizer(p, ":");
      while (stp.hasMoreTokens()) {
        itsPoints.add(stp.nextToken());
      }
      DataMaintainer.subscribe(itsPoints, this);
      itsSizeLabel.setText("This watchdog is monitoring " + itsPoints.size() + " points");

      // Get the inertia value
      String i = (String) setup.get("inertia");
      if (i != null) {
        itsInertia = Integer.parseInt(i);
      }

      // See whether unavailable data is to be treated as an alarm condition
      String s = (String) setup.get("stale");
      if (s != null) {
        if (s.equals("false")) {
          itsStaleOK = true;
        } else {
          itsStaleOK = false;
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
      if (itsFrame != null) {
        JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
            + "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
            JOptionPane.WARNING_MESSAGE);
      } else {
        System.err.println("WatchDog:loadData: " + e.getClass().getName());
      }
      blankSetup();
      return false;
    }

    return true;
  }

  /**
   * Get the current sub-class-specific configuration for this MonPanel. This can be used to capture the current state of the
   * MonPanel so that it can be easily recovered later.
   * 
   * @return String containing sub-class-specific configuration information.
   */
  public synchronized SavedSetup getSetup() {
    SavedSetup ss = new SavedSetup();
    ss.setClass(getClass().getName());
    ss.setName("temp");

    // Make a parsable string from the list of point names
    String p = "";
    if (itsPoints.size() > 0) {
      p += itsPoints.get(0);
      // Then add rest of point names with a delimiter
      for (int i = 1; i < itsPoints.size(); i++) {
        p += ":" + itsPoints.get(i);
      }
    }
    ss.put("points", p);

    if (itsStaleOK) {
      ss.put("stale", "false");
    } else {
      ss.put("stale", "true");
    }

    ss.put("inertia", "" + itsInertia);

    return ss;
  }

  /** Called whenever a new value is available for one of our points. */
  public void onPointEvent(Object source, PointEvent evt) {
    if (!evt.isRaw()) {
      PointData newval = evt.getPointData();
      if (newval != null) {
        String fullname = newval.getName();
        PointDescription pm = PointDescription.getPoint(fullname);
        long age = (new AbsTime()).getValue() - newval.getTimestamp().getValue();
        long period = pm.getPeriod();
        if (newval.isValid() && (period == 0 || age < 5 * period)) {
          // Get alarm counter for this point
          Integer icount = (Integer) itsAlarmCounts.get(fullname);
          int count = 0;
          if (icount != null) {
            count = icount.intValue();
          }
          // Check if current update is in alarm state
          if (!newval.getAlarm()) {
            // Value is currently okay
            if (count > 0) {
              count--;
            }
            if (count == 0) {
              // Was in alarm state but now okay, remove from table
              itsActiveModel.removePoint(fullname);
            }
          } else {
            // Value is in alarm state
            if (count <= itsInertia) {
              count++;
            }
            if (count == itsInertia) {
              // Ensure alarm is triggered if point is not ignored
              if (itsActiveModel.getPoints().indexOf(fullname) == -1 && itsSilencedModel.getPoints().indexOf(fullname) == -1) {
                itsActiveModel.addPoint(fullname, newval);
              }
            }
          }
          itsAlarmCounts.put(fullname, new Integer(count));
        } else {
          // Point is invalid or data is stale
          if (!itsStaleOK) {
            // Ensure alarm is triggered if point is not ignored
            if (itsActiveModel.getPoints().indexOf(fullname) == -1 && itsSilencedModel.getPoints().indexOf(fullname) == -1) {
              itsActiveModel.addPoint(fullname, newval);
            }
          }
        }
        // Give the new value to our tables
        itsActiveModel.updateValue(fullname, newval);
        itsSilencedModel.updateValue(fullname, newval);
      }
    }
  }

  /** Free all resources so that this MonPanel can disappear. */
  public void vaporise() {
    // Force the table model to unsubscribe from all points
    // itsModel.set(null, null);
    itsRemoved = true;
  }

  /**
   * Get a panel with the controls required to configure this MonPanel.
   * 
   * @return GUI controls to configure this MonPanel.
   */
  public MonPanelSetupPanel getControls() {
    return new WatchdogSetupPanel(this, itsFrame);
  }

  /**
   * Dump current data to the given output stream. This is the mechanism through which data can be exported to a file.
   * 
   * @param p
   *          The print stream to write the data to.
   */
  public synchronized void export(PrintStream p) {
    final String rcsid = "$Id: $";
    p.println("#Dump from WatchDog " + rcsid);
    p.println("#Data dumped at " + (new AbsTime().toString(AbsTime.Format.UTC_STRING)));
    // itsModel.export(p);
    p.println();
    p.println();
  }

  /** Play an audio sample on the sound card. */
  private boolean playAudio(String resname) {
    RelTime sleep = RelTime.factory(1000000);
    try {
      InputStream in = Watchdog.class.getClassLoader().getResourceAsStream(resname);
      AudioInputStream soundIn = AudioSystem.getAudioInputStream(in);
      DataLine.Info info = new DataLine.Info(Clip.class, soundIn.getFormat());
      Clip clip = (Clip) AudioSystem.getLine(info);
      clip.open(soundIn);
      sleep.sleep(); // Clips start of clip without this
      clip.start();
      // Wait until clip is finished then release the sound card
      while (clip.isActive()) {
        Thread.yield();
      }
      clip.drain();
      sleep.sleep(); // Clips end of clip without this
      clip.close();
    } catch (Exception e) {
      System.err.println("Watchdog.playAudio: " + e.getClass());
      return false;
    }
    return true;
  }

  public String getLabel() {
    return null;
  }

  /** Basic test application. */
  public static void main(String[] argv) {
    JFrame frame = new JFrame("WatchDog Test App");
    // frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

    /*
     * SavedSetup seemon = new SavedSetup("temp", "atnf.atoms.mon.gui.monpanel.ATPointTable",
     * "true:3:site.seemon.Lock1:site.seemon.Lock2:site.seemon.Lock3:1:seemon");
     */
    Watchdog wd = new Watchdog();
    // wd.loadSetup(seemon);
    // frame.getContentPane().add(pt);
    frame.setContentPane(wd);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);

    /*
     * try { RelTime sleepy = RelTime.factory(15000000l); sleepy.sleep(); } catch (Exception e) { e.printStackTrace(); }
     * 
     * SavedSetup ss = pt.getSetup(); pt.loadSetup(ss);
     */
  }
}
