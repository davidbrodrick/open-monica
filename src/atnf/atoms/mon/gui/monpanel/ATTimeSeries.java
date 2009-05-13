//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.client.*;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.time.*;
import atnf.atoms.util.*;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.lang.Number;
import java.text.SimpleDateFormat;
import java.io.PrintStream;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.time.*;
import org.jfree.chart.renderer.xy.*;

/**
 * Class representing a value versus time line graph. The graph can be
 * configured to display various monitor points, to display data in a static
 * archival mode or a dynamic updating mode, and also has other features.
 * 
 * @author David Brodrick
 * @version $Id: ATTimeSeries.java,v 1.11 2006/06/22 04:39:07 bro764 Exp $
 * @see MonPanel
 */
public class ATTimeSeries extends MonPanel implements ActionListener, Runnable {
  static {
    MonPanel.registerMonPanel("Time Series", ATTimeSeries.class);
  }

  // /////////////////////// NESTED CLASS ///////////////////////////////
  /**
   * Nested class to provide GUI controls for configuring an ATTimeSeries
   * MonPanel.
   */
  public class ATTimeSeriesSetupPanel extends MonPanelSetupPanel
    implements ActionListener {
    /** Nested class provides the panel to setup each axis. */
    public class AxisSetup extends JPanel implements ActionListener {
      /** Reference to the point source selector for this axis. */
      public PointSourceSelector itsPoints = new PointSourceSelector();

      /** The checkbox to enable auto-scaling. */
      public JRadioButton itsAutoScale = new JRadioButton("Auto-Scale");

      /** If autoscaling, should we include zero? */
      public JCheckBox itsAutoZero = new JCheckBox("Include Zero");

      /** The checkbox to enable manual scaling. */
      public JRadioButton itsSpecifyScale = new JRadioButton("Specify Scale");

      /** Textfield to hold the scale minimum. */
      public JTextField itsScaleMin = new JTextField(8);

      /** Textfield to hold the scale maximum. */
      public JTextField itsScaleMax = new JTextField(8);

      /** Textfield to hold the label for the axis. */
      public JTextField itsAxisLabel = new JTextField("Value", 16);

      /** Radio button for drawing data with lines. */
      public JRadioButton itsDrawLines = new JRadioButton("Lines");

      /** Check box to allow discontinuous lines. */
      public JCheckBox itsDiscontinuousLines = new JCheckBox("Allow Breaks");

      /** Radio button for drawing data with dots. */
      public JRadioButton itsDrawDots = new JRadioButton("Dots");

      /** Radio button for drawing data with symbols. */
      public JRadioButton itsDrawSymbols = new JRadioButton("Shapes");

      public AxisSetup() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createLineBorder(Color.red));

        ButtonGroup tempgroup = new ButtonGroup();
        tempgroup.add(itsAutoScale);
        tempgroup.add(itsSpecifyScale);
        tempgroup = new ButtonGroup();
        tempgroup.add(itsDrawLines);
        tempgroup.add(itsDrawDots);
        tempgroup.add(itsDrawSymbols);

        itsPoints.setToolTipText("Select point to be graphed");
        itsPoints.setPreferredSize(new Dimension(180, 180));
        add(itsPoints);

        JPanel temppanel = new JPanel();
        JLabel templabel = new JLabel("Axis Label:");
        templabel.setForeground(Color.black);
        templabel.setToolTipText("Enter label for value axis");
        temppanel.add(templabel);
        itsAxisLabel.setToolTipText("Enter label for value axis");
        temppanel.add(itsAxisLabel);
        add(temppanel);

        temppanel = new JPanel();
        itsAutoScale.setToolTipText("Automatically scale the axis");
        itsAutoScale.addActionListener(this);
        itsAutoScale.setActionCommand("Auto-Scale");
        itsAutoScale.doClick();
        temppanel.add(itsAutoScale);
        itsAutoZero.setToolTipText("Always include zero level in scale");
        itsAutoZero.setSelected(false);
        temppanel.add(itsAutoZero);
        add(temppanel);

        temppanel = new JPanel();
        itsSpecifyScale
            .setToolTipText("Manually specify max and min range for the axis");
        itsSpecifyScale.addActionListener(this);
        itsSpecifyScale.setActionCommand("Specify-Scale");
        temppanel.add(itsSpecifyScale);
        templabel = new JLabel("Min:");
        templabel.setForeground(Color.black);
        templabel.setToolTipText("Specify minimum value for the value axis");
        temppanel.add(templabel);
        itsScaleMin.setToolTipText("Specify minimum value for the value axis");
        temppanel.add(itsScaleMin);
        templabel = new JLabel("Max:");
        templabel.setForeground(Color.black);
        templabel.setToolTipText("Specify maximum value for the value axis");
        temppanel.add(templabel);
        itsScaleMax.setToolTipText("Specify maximum value for the value axis");
        temppanel.add(itsScaleMax);
        add(temppanel);

        temppanel = new JPanel();
        templabel = new JLabel("Plot As:");
        templabel.setForeground(Color.black);
        templabel.setToolTipText("Select data renderer");
        temppanel.add(templabel);
        itsDrawLines.setToolTipText("Draw lines between data points");
        itsDrawLines.setSelected(true);
        temppanel.add(itsDrawLines);
        itsDiscontinuousLines.setSelected(true);
        itsDiscontinuousLines
            .setToolTipText("Allow breaks in the line where data is missing");
        temppanel.add(itsDiscontinuousLines);
        itsDrawDots.setToolTipText("Draw a dot for each data point");
        temppanel.add(itsDrawDots);
        itsDrawSymbols
            .setToolTipText("Draw an unfilled shape for each data point");
        temppanel.add(itsDrawSymbols);
        add(temppanel);
      }

      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Auto-Scale")) {
          itsScaleMin.setEnabled(false);
          itsScaleMax.setEnabled(false);
          itsScaleMin.setBackground(Color.lightGray);
          itsScaleMax.setBackground(Color.lightGray);
          itsAutoZero.setEnabled(true);
        } else if (cmd.equals("Specify-Scale")) {
          itsScaleMin.setEnabled(true);
          itsScaleMax.setEnabled(true);
          itsScaleMin.setBackground(Color.white);
          itsScaleMax.setBackground(Color.white);
          itsAutoZero.setEnabled(false);
        }
      }

      /** Configure GUI to indicate state specified by the string. */
      public void showSetup(SavedSetup setup, int number) {
        try {
          String temp = (String) setup.get("label" + number);
          itsAxisLabel.setText(temp);
          temp = (String) setup.get("scalemode" + number);
          if (temp.equals("auto")) {
            itsAutoScale.doClick();
            temp = (String) setup.get("showzero" + number);
            if (temp.equals("true")) {
              itsAutoZero.setSelected(true);
            } else if (temp.equals("false")) {
              itsAutoZero.setSelected(false);
            }
          } else if (temp.equals("fixed")) {
            itsSpecifyScale.doClick();
            temp = (String) setup.get("scalemin" + number);
            itsScaleMin.setText(temp);
            temp = (String) setup.get("scalemax" + number);
            itsScaleMax.setText(temp);
          } else {
            return;
          }

          temp = (String) setup.get("style" + number);
          if (temp.equals("lines")) {
            itsDrawLines.doClick();
            temp = (String) setup.get("gaps" + number);
            if (temp.equals("true")) {
              itsDiscontinuousLines.setSelected(true);
            } else if (temp.equals("false")) {
              itsDiscontinuousLines.setSelected(false);
            }
          } else if (temp.equals("dots")) {
            itsDrawDots.doClick();
          } else if (temp.equals("symbols")) {
            itsDrawSymbols.doClick();
          } else {
            return;
          }

          temp = (String) setup.get("numpoints" + number);
          int numpoints = Integer.parseInt(temp);
          Vector<String> points = new Vector<String>(numpoints);

          temp = (String) setup.get("points" + number);
          StringTokenizer st = new StringTokenizer(temp, ":");
          for (int i = 0; i < numpoints; i++) {
            points.add(st.nextToken());
          }
          itsPoints.setSelections(points);
        } catch (Exception e) {
        }
      }

      /** Return a string summary of the current setup. */
      public void getSetup(SavedSetup setup, int number) throws Exception {
        // Ensure user has not entered any reserved characters
        if (!checkString(itsAxisLabel.getText())) {
          JOptionPane.showMessageDialog(this, "The axis label:\n" + "\""
              + itsAxisLabel.getText() + "\"\n"
              + "contains reserved characters.\n"
              + "You must not use ~ : or `\n", "Reserved Characters",
              JOptionPane.WARNING_MESSAGE);
          return;
        }
        Vector selpoints = itsPoints.getSelections();

        setup.put("label" + number, itsAxisLabel.getText());

        if (itsAutoScale.isSelected()) {
          setup.put("scalemode" + number, "auto");
          if (itsAutoZero.isSelected()) {
            setup.put("showzero" + number, "true");
          } else {
            setup.put("showzero" + number, "false");
          }
        } else {
          setup.put("scalemode" + number, "fixed");
          // Check that the numeric fields contain parsable number
          try {
            Double.parseDouble(itsScaleMax.getText());
            Double.parseDouble(itsScaleMin.getText());
          } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "The fields for minimum and maximum\n"
                    + "scales must contain numbers, eg, \"42\"\n"
                    + "or \"99.9\". You can select the Auto-Scale\n"
                    + "option if you don't know what scale to use.\n",
                "Bad Scale Entered", JOptionPane.WARNING_MESSAGE);
            return;
          }
          setup.put("scalemin" + number, itsScaleMin.getText());
          setup.put("scalemax" + number, itsScaleMax.getText());
        }
        if (itsDrawLines.isSelected()) {
          setup.put("style" + number, "lines");
          if (itsDiscontinuousLines.isSelected()) {
            setup.put("gaps" + number, "true");
          } else {
            setup.put("gaps" + number, "false");
          }
        } else if (itsDrawDots.isSelected()) {
          setup.put("style" + number, "dots");
        } else if (itsDrawSymbols.isSelected()) {
          setup.put("style" + number, "symbols");
        }

        // Ensure the user selected at least one point for this axis!
        if (selpoints.size() == 0) {
          JOptionPane.showMessageDialog(this,
              "No points were selected for axis\n" + "number " + (number + 1)
                  + ". You need to select\n"
                  + "at least one point for each axis!", "No Points Selected!",
              JOptionPane.WARNING_MESSAGE);
          throw new Exception();
        }

        setup.put("numpoints" + number, "" + selpoints.size());
        String temp = "";
        for (int i = 0; i < selpoints.size(); i++) {
          temp += (String) selpoints.get(i) + ":";
        }
        setup.put("points" + number, temp);
      }
    }

    /** The main panel which hold our GUI controls. */
    protected JPanel itsSetupPanel = new JPanel();

    /** TextField for the graph title. */
    protected JTextField itsTitleField = new JTextField(40);

    /** Label which records the number of value axis. */
    protected JLabel itsNumAxis = new JLabel("1");

    /** Vector which contains the setup panels for each of the axis. */
    protected Vector<AxisSetup> itsAxis = new Vector<AxisSetup>();

    /** Panel which contains the AxisSetup components. */
    protected JPanel itsAxisPanel = new JPanel();

    /** Check box, records whether to show the Legend. */
    protected JCheckBox itsShowLegend = new JCheckBox("Show Legend");

    /** Text field hold the time span of the graph. */
    protected JTextField itsPeriod = new JTextField("12", 6);

    /** Options for the time period selection combo box. */
    final private String[] itsTimeOptions = { "Hours", "Days", "Minutes" };

    /** combo box to give time unit selection */
    protected JComboBox itsPeriodUnits = new JComboBox(itsTimeOptions);

    /** Options for the time zone selection. */
    final private String[] itsTimeZones = { "Local", "UT" };

    /** The timezone string corresponding to the timezones. */
    final private String[] itsTimeZoneNames = { null, "GMT+00" };

    /** Combo box for choosing timezones. */
    protected JComboBox itsZone = new JComboBox(itsTimeZones);

    /** Records if the graph should update in real-time. */
    protected JRadioButton itsDynamic = new JRadioButton("Real-Time");

    /** Records if the graph should remain static. */
    protected JRadioButton itsStatic = new JRadioButton("Archival");

    /** Allows user to enter graph start time for archival mode. */
    protected JTextField itsStart = new JTextField(16);

    /** The format of the dates we parse. */
    protected SimpleDateFormat itsFormatter = new SimpleDateFormat(
        "yyyy/MM/dd HH:mm");

    /** How often to update the graph in seconds. */
    protected JTextField itsUpdateRate = new JTextField("60", 4);

    /** Check box, should we supser-speed the graph? */
    protected JCheckBox itsSuperSpeed = new JCheckBox(
        "Enable Fast Graphs. Max samples displayed = ");

    /** Max number of samples to be displayed. */
    protected JTextField itsMaxSamps = new JTextField("500", 6);

    /** Construct the setup editor for the specified panel. */
    public ATTimeSeriesSetupPanel(ATTimeSeries panel, JFrame frame) {
      super(panel, frame);

      itsSetupPanel.setLayout(new BorderLayout());
      JPanel globalpanel = new JPanel();
      globalpanel.setLayout(new BoxLayout(globalpanel, BoxLayout.Y_AXIS));
      JPanel temppanel = new JPanel();
      temppanel.setLayout(new BorderLayout());

      itsDynamic.addActionListener(this);
      itsDynamic.setActionCommand("Dynamic");
      itsStatic.addActionListener(this);
      itsStatic.setActionCommand("Static");
      itsSuperSpeed.addActionListener(this);
      itsSuperSpeed.setActionCommand("SuperSpeed");
      itsSuperSpeed.doClick();

      ButtonGroup tempgroup = new ButtonGroup();
      tempgroup.add(itsDynamic);
      tempgroup.add(itsStatic);

      itsDynamic.doClick();
      itsShowLegend.setSelected(true);

      JPanel temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));
      JLabel templabel = new JLabel("Graph Title:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter a title for the graph");
      temppanel2.add(templabel);
      temppanel.add(temppanel2, BorderLayout.WEST);

      itsTitleField.addActionListener(this);
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));
      itsTitleField.setToolTipText("Enter a title for the graph");
      temppanel2.add(itsTitleField);
      temppanel.add(temppanel2, BorderLayout.CENTER);

      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);

      itsPeriod.addActionListener(this);
      temppanel = new JPanel();
      // temppanel.setLayout(new GridLayout(3,1));
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
      temppanel2 = new JPanel();
      templabel = new JLabel("Graph Time Span:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter time span for graph data");
      temppanel2.add(templabel);
      temppanel2.add(itsPeriod);
      temppanel2.add(itsPeriodUnits);
      temppanel2.add(Box.createRigidArea(new Dimension(20, 0)));
      templabel = new JLabel("Time Zone:");
      templabel.setForeground(Color.black);
      templabel
          .setToolTipText("Data will be displayed for the specified timezone");
      temppanel2.add(templabel);
      itsZone
          .setToolTipText("Data will be displayed for the specified timezone");
      temppanel2.add(itsZone);
      temppanel.add(temppanel2);

      itsStart.addActionListener(this);
      temppanel2 = new JPanel();
      itsDynamic
          .setToolTipText("Graph will automatically update to show new data");
      temppanel.add(itsDynamic);
      temppanel2 = new JPanel();
      itsStatic.setToolTipText("Graph will only show the specified data");
      temppanel2.add(itsStatic);
      templabel = new JLabel("Graph Start Time (yyyy/MM/dd HH:mm):");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter data start time - yyyy/MM/dd HH:mm");
      temppanel2.add(templabel);
      itsStart
          .setToolTipText("Format: yyyy/MM/dd HH:mm in timezone selected above");
      itsStart.setText(itsFormatter.format(new Date()));
      temppanel2.add(itsStart);
      temppanel.add(temppanel2);
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);

      itsUpdateRate.addActionListener(this);
      itsMaxSamps.addActionListener(this);
      temppanel2 = new JPanel();
      temppanel2.setLayout(new GridLayout(2, 2));
      itsUpdateRate.setToolTipText("How often to update/redraw the graph "
          + "(faster chews more CPU)");
      templabel = new JLabel("Graph update/redraw interval (in seconds) = ");
      templabel.setToolTipText("How often to update/redraw the graph "
          + "(faster chews more CPU)");
      temppanel2.add(templabel);
      temppanel2.add(itsUpdateRate);
      itsSuperSpeed
          .setToolTipText("Makes graph redraws faster but shows less data");
      temppanel2.add(itsSuperSpeed);
      temppanel2.add(itsMaxSamps);
      temppanel2.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel2);

      temppanel = new JPanel();
      itsShowLegend.setToolTipText("Should a legend be shown on the graph?");
      temppanel.add(itsShowLegend);
      temppanel.add(Box.createRigidArea(new Dimension(20, 0)));
      templabel = new JLabel("Number of Value Axis:");
      templabel.setForeground(Color.black);
      temppanel.add(templabel);
      temppanel.add(Box.createRigidArea(new Dimension(5, 0)));
      temppanel.add(itsNumAxis);
      temppanel.add(Box.createRigidArea(new Dimension(5, 0)));
      JButton tempbut = new JButton("+");
      tempbut.addActionListener(this);
      tempbut.setActionCommand("Add-Axis");
      tempbut.setToolTipText("Add another value axis to the graph");
      temppanel.add(tempbut);
      tempbut = new JButton("-");
      tempbut.addActionListener(this);
      tempbut.setActionCommand("Remove-Axis");
      tempbut.setToolTipText("Remove the last value axis from the graph");
      temppanel.add(tempbut);
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);
      itsSetupPanel.add(globalpanel, BorderLayout.NORTH);

      itsAxisPanel.setLayout(new BoxLayout(itsAxisPanel, BoxLayout.Y_AXIS));
      JScrollPane scroller = new JScrollPane(itsAxisPanel);
      scroller.setPreferredSize(new Dimension(500, 350));
      scroller.setMaximumSize(new Dimension(2000, 350));
      scroller.setBorder(BorderFactory.createLoweredBevelBorder());
      itsSetupPanel.add(scroller, BorderLayout.CENTER);

      itsAxis.add(new AxisSetup());
      itsAxisPanel.add((JPanel) itsAxis.get(0));

      add(itsSetupPanel, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
      super.actionPerformed(e);
      String cmd = e.getActionCommand();
      if (e.getSource() == itsTitleField || e.getSource() == itsUpdateRate
          || e.getSource() == itsMaxSamps || e.getSource() == itsStart
          || e.getSource() == itsPeriod) {
        okClicked();
        return;
      }
      if (cmd.equals("Dynamic")) {
        itsStart.setEnabled(false);
        itsStart.setBackground(Color.lightGray);
      } else if (cmd.equals("Static")) {
        itsStart.setEnabled(true);
        itsStart.setBackground(Color.white);
      } else if (cmd.equals("SuperSpeed")) {
        if (itsSuperSpeed.isSelected()) {
          itsMaxSamps.setEnabled(true);
          itsMaxSamps.setBackground(Color.white);
        } else {
          itsMaxSamps.setEnabled(false);
          itsMaxSamps.setBackground(Color.lightGray);
        }
      } else if (cmd.equals("Add-Axis")) {
        itsAxis.add(new AxisSetup());
        itsAxisPanel.add((JPanel) itsAxis.get(itsAxis.size() - 1));
        itsNumAxis.setText("" + itsAxis.size());
        itsAxisPanel.invalidate();
        itsAxisPanel.repaint();
      } else if (cmd.equals("Remove-Axis")) {
        if (itsAxis.size() == 1) {
          Toolkit.getDefaultToolkit().beep();
        } else {
          itsAxisPanel.remove((JPanel) itsAxis.get(itsAxis.size() - 1));
          itsAxis.remove(itsAxis.size() - 1);
          itsNumAxis.setText("" + itsAxis.size());
          itsAxisPanel.invalidate();
          itsAxisPanel.repaint();
        }
      } else {
        System.err.println("ATTimeSeries:SetupPanel:actionPerformed: " + cmd);
      }
    }

    /**
     * Return the current setup, as determined by the GUI controls. It provides
     * the means of extracting the setup specified by the user into a useable
     * format.
     * 
     * @return SavedSetup specified by GUI controls, or <tt>null</tt> if no
     *         setup can be extracted from the GUI at present.
     */
    protected SavedSetup getSetup() {
      // Ensure user has not entered any reserved characters
      if (!checkString(itsTitleField.getText())) {
        JOptionPane.showMessageDialog(this, "The Graph Title:\n" + "\""
            + itsTitleField.getText() + "\"\n"
            + "contains reserved characters.\n" + "You must not use \"`\"\n",
            "Bad Characters in Title", JOptionPane.WARNING_MESSAGE);
        return null;
      }

      SavedSetup setup = new SavedSetup("temp",
          "atnf.atoms.mon.gui.monpanel.ATTimeSeries");
      // Save the graph title
      String temp = itsTitleField.getText();
      if (temp == null || temp.equals("")) {
        temp = "Data vs. Time";
      }
      setup.put("title", temp);

      // Save the timezone, if one has been specified
      if (itsTimeZoneNames[itsZone.getSelectedIndex()] != null) {
        setup.put("timezone", itsTimeZoneNames[itsZone.getSelectedIndex()]);
      }

      // Check that the numeric period field and save it, if okay
      try {
        Double.parseDouble(itsPeriod.getText());
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "The field for the Graph Time Span must contain\n"
                + "a valid number, eg, \"42\" or \"99.9\".\n",
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
      setup.put("period", "" + (long) numtime);

      if (itsDynamic.isSelected()) {
        setup.put("mode", "dynamic");
      } else if (itsStatic.isSelected()) {
        setup.put("mode", "static");
        String startstr = itsStart.getText();
        Date date = null;
        try {
          date = itsFormatter.parse(startstr);
        } catch (Exception e) {
          date = null;
        }
        if (date == null) {
          JOptionPane.showMessageDialog(this,
              "The Graph Start Time you entered\n"
                  + "could not be parsed. The time must\n"
                  + "be in \"yyyy/MM/dd HH:mm\" format, eg:\n" + "\""
                  + itsFormatter.format(new Date()) + "\"\n", "Bad Start Time",
              JOptionPane.WARNING_MESSAGE);
          return null;
        }
        AbsTime start = AbsTime.factory(date);
        setup.put("start", "" + start.getValue());
      } else {
        return null;
      }
      if (itsShowLegend.isSelected()) {
        setup.put("legend", "true");
      } else {
        setup.put("legend", "false");
      }

      // Check that the update interval is numeric
      long upd = 60000000;
      try {
        upd = Long.parseLong(itsUpdateRate.getText());
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "The field for the graph update/redraw interval\n"
                + "must contain a valid integer, eg, \"60\".\n",
            "Bad Update Rate", JOptionPane.WARNING_MESSAGE);
        return null;
      }
      // Do a reality check on the actual value
      if (upd < 1 || upd > 1000000) {
        JOptionPane.showMessageDialog(this,
            "The field for the graph update/redraw interval\n"
                + "contains an unreasonable value...\n", "Bad Update Rate",
            JOptionPane.WARNING_MESSAGE);
        return null;

      }
      setup.put("update", "" + upd * 1000000);

      if (itsSuperSpeed.isSelected()) {
        setup.put("speedup", "true");
        int maxsamps = 0;
        try {
          maxsamps = Integer.parseInt(itsMaxSamps.getText());
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this,
              "The field for maximum number of samples\n"
                  + "must contain a valid integer, eg, \"500\".\n",
              "Bad Maximum Samples", JOptionPane.WARNING_MESSAGE);
          return null;
        }
        setup.put("maxsamps", "" + maxsamps);
      } else {
        setup.put("speedup", "false");
      }

      setup.put("numaxis", "" + itsAxis.size());
      for (int i = 0; i < itsAxis.size(); i++) {
        try {
          ((AxisSetup) itsAxis.get(i)).getSetup(setup, i);
        } catch (Exception e) {
          return null;
        }
      }
      return setup;
    }

    /** Make the controls show information about the given setup. */
    public void showSetup(SavedSetup setup) {
      try {
        // title
        String temp = (String) setup.get("title");
        itsTitleField.setText(temp);

        // timezone
        temp = (String) setup.get("timezone");
        if (temp == null) {
          // Default timezone
          itsZone.setSelectedIndex(0);
        } else {
          // Specified timezone
          boolean foundit = false;
          for (int i = 1; i < itsTimeZones.length; i++) {
            if (itsTimeZoneNames[i].equals(temp)) {
              // Found the specified timezone
              foundit = true;
              itsZone.setSelectedIndex(i);
            }
          }
          if (!foundit) {
            // Unknown timezone, select default
            itsZone.setSelectedIndex(0);
          }
        }

        // graph mode
        temp = (String) setup.get("mode");
        if (temp.equals("dynamic")) {
          itsDynamic.doClick();
        } else {
          itsStatic.doClick();
          temp = (String) setup.get("start");
          long start = Long.parseLong(temp);
          AbsTime atemp = AbsTime.factory(start);
          Date dtemp = atemp.getAsDate();
          itsStart.setText(itsFormatter.format(dtemp));
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

        // legend
        temp = (String) setup.get("legend");
        if (temp.equals("true")) {
          itsShowLegend.setSelected(true);
        } else {
          itsShowLegend.setSelected(false);
        }

        // update interval
        temp = (String) setup.get("update");
        if (temp != null) {
          long upd = Long.parseLong(temp);
          upd /= 1000000;
          itsUpdateRate.setText("" + upd);
        } else {
          itsUpdateRate.setText("60");
        }

        temp = (String) setup.get("speedup");
        if (temp == null || temp.equals("false")) {
          itsSuperSpeed.setSelected(false);
          itsMaxSamps.setEnabled(false);
          itsMaxSamps.setBackground(Color.lightGray);
        } else {
          itsSuperSpeed.setSelected(true);
          itsMaxSamps.setEnabled(true);
          itsMaxSamps.setBackground(Color.white);
          String foo = (String) setup.get("maxsamps");
          if (foo == null) {
            foo = "500";
          }
          itsMaxSamps.setText(foo);
        }

        // Purge any old axis setups from the display
        while (itsAxis.size() > 0) {
          AxisSetup byebye = (AxisSetup) itsAxis.get(0);
          itsAxisPanel.remove(byebye);
          itsAxis.remove(byebye);
        }

        // number of axis
        temp = (String) setup.get("numaxis");
        itsNumAxis.setText(temp);
        int numaxis = Integer.parseInt(temp);
        // Next we need to parse the information for each axis
        for (int i = 0; i < numaxis; i++) {
          AxisSetup thisaxis = new AxisSetup();
          thisaxis.showSetup(setup, i);
          itsAxis.add(thisaxis);
          itsAxisPanel.add(thisaxis);
        }
      } catch (Exception e) {
        System.err.println("ATTimeSeriesSetupPanel:showSetup: "
            + e.getMessage());
      }
    }
  }

  // ///////////////////// END NESTED CLASS /////////////////////////////

  /**
   * Static object to use as a semaphore. JFreeChart can do the strangest things
   * when multiple threads use it at once...
   */
  private static Object theirLock = new Object();

  /** Network connection to the monitor server. */
  private MonitorClientCustom itsServer = null;

  /** Number of axis contained in the graph. */
  private int itsNumAxis = 0;

  /** Contains the TimeSeries for each axis. */
  private Vector<TimeSeriesCollection> itsData = new Vector<TimeSeriesCollection>();

  /** Contains Vectors holding the point names for each axis. */
  private Vector<Vector> itsPointNames = new Vector<Vector>();

  /**
   * Contains Vectors holding AbsTimes for the last data collected for each
   * point, for each axis. We can use this info together with the 'getSince'
   * method to ensure we don't miss any data.
   */
  private Vector<Vector> itsPointEpochs = new Vector<Vector>();

  /** Reference to our graph. */
  private JFreeChart itsGraph = null;

  /** Reference to the ChartPanel which contains our graph. */
  private ChartPanel itsChartPanel = null;

  /** Title string for the graph. */
  private String itsTitle = "Time Series Graph";

  /**
   * Records if the graph should update in real-time. If the value is
   * <tt>true</tt> the graph will self-update, if the value is <tt>false</tt>
   * the graph will remain static.
   */
  private boolean itsRealtime = true;

  /** Update interval for dynamic graphs. */
  private RelTime itsUpdateInterval = RelTime.factory(10000000);

  /** Should a legend be shown? */
  private boolean itsShowLegend = true;

  /** The period of time the data on the graph should span. */
  private RelTime itsPeriod = RelTime.factory(12 * 3600000000l);

  /**
   * Timestamp of the oldest data to display. Any data which is older than this
   * epoch is considered expired and should no longer be displayed in our graph.
   */
  private AbsTime itsStart = new AbsTime();

  /**
   * Flag to indicate if the graph needs to be reinitialised. This will be set
   * when there has been a change to the data we need to display.
   */
  private boolean itsReInit = false;

  /**
   * Flag to indicate if the thread should keep running. If this is set to
   * <tt>true</tt> the thread will keep running. If set to <tt>false</tt>
   * the thread will exit.
   */
  private boolean itsKeepRunning = true;

  /** Timezone string to display data in, or <tt>null</tt> for local time. */
  private String itsTimeZone = null;
  
  /** Actual TimeZone object to be used for display. */
  private TimeZone itsTZ = null;

  /** Copy of the setup we are currently using. */
  private SavedSetup itsSetup = null;

  /** Timer that forces updates at the user specified frequency. */
  private javax.swing.Timer itsTimer = null;

  /** Max number of samples to display (0 means show all samples) */
  private int itsMaxSamps = 0;

  private Image itsImage = null;

  private int itsOldWidth = 0;

  private int itsOldHeight = 0;

  private JLabel itsPleaseWait = new JLabel(
      "Downloading graph data from server " + "- PLEASE WAIT", JLabel.CENTER);

  /** C'tor. */
  public ATTimeSeries() {
    setLayout(new java.awt.BorderLayout());
    findServer();

    setPreferredSize(new Dimension(500, 300));
    setMinimumSize(new Dimension(200, 200));

    // Start the data collection thread
    new Thread(this).start();
  }

  /** Main loop for data-update thread. */
  public void run() {
    final JPanel realthis = this;

    Runnable notsetup = new Runnable() {
      public void run() {
        add(new JLabel("Configure graph options under the \"Time Series\" tab",
            JLabel.CENTER));
        // realthis.repaint();
      }
    };
    try {
      // Need to do the notification using event thread
      SwingUtilities.invokeLater(notsetup);
    } catch (Exception e) {
      e.printStackTrace();
    }

    itsTimer = new javax.swing.Timer((int) (itsUpdateInterval.getValue() / 1000), this);
    itsTimer.start();

    while (itsKeepRunning) {
      // check if a new setup has just been loaded
      if (itsReInit) {
        loadSetupReal();
      }

      if (itsRealtime) {
        // Recalculate the epoch of the earliest data to display
        itsStart = (new AbsTime()).add(itsPeriod.negate());
      }

      if (itsChartPanel != null && itsChartPanel.getChart() != null) {
        // Update the time/date axis of the graph
        DateAxis timeaxis = (DateAxis) itsChartPanel.getChart().getXYPlot()
            .getDomainAxis();
        timeaxis.setAutoRange(false);
        timeaxis.setMaximumDate((itsStart.add(itsPeriod)).getAsDate());
        timeaxis.setMinimumDate(itsStart.getAsDate());
      }

      if (itsRealtime) {
        synchronized (itsPointNames) {
          for (int a = 0; a < itsNumAxis; a++) {
            Vector points = (Vector) itsPointNames.get(a);
            Vector times = (Vector) itsPointEpochs.get(a);
            for (int i = 0; i < points.size(); i++) {
              AbsTime last = (AbsTime) times.get(i);
              // Get any new data
              Vector<PointData> v = getSince((String) points.get(i), last);
              // If we need to down-sample it, then do that
              if (itsMaxSamps > 1 && v != null && v.size() > 0) {
                Vector newv = new Vector();
                AbsTime next = last;
                RelTime increment = RelTime.factory(itsPeriod.getValue()
                    / itsMaxSamps);
                int s = 0;
                while (s < v.size()) {
                  next = next.add(increment);
                  AbsTime thissamp = ((PointData) v.get(s)).getTimestamp();
                  while (thissamp.isBefore(next)) {
                    s++;
                    if (s >= v.size()) {
                      break;
                    }
                    thissamp = ((PointData) v.get(s)).getTimestamp();
                  }
                  if (s < v.size()) {
                    newv.add(v.get(s));
                    s++;
                  }
                }
                v = newv;
              }
              mergeData(a, i, v);
            }
          }
        }
      } // synchronized

      if (itsNumAxis > 0) {
        // Let the graph know that the data sets have been updated
        Runnable ud = new Runnable() {
          public void run() {
            ((TimeSeriesCollection) itsData.get(0)).getSeries(0)
                .setNotify(true);
            ((TimeSeriesCollection) itsData.get(0)).getSeries(0).setNotify(
                false);
          }
        };
        try {
          // Need to do the notification using event thread
          SwingUtilities.invokeAndWait(ud);
        } catch (Exception e) {
          e.printStackTrace();
        }

        // Update our cached image of the graph
        int w = getSize().width;
        int h = getSize().height;
        if (itsGraph != null) {
          itsImage = itsGraph.createBufferedImage(w, h);
        }

        // Redraw our display using new image - also done by event thread
        Runnable ud2 = new Runnable() {
          public void run() {
            realthis.removeAll();
            realthis.repaint();
          }
        };
        try {
          SwingUtilities.invokeLater(ud2);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      // check if a new setup was loaded while we were busy, before we sleep
      if (itsReInit) {
        loadSetupReal();
      }

      // Wait here for a while
      synchronized (this) {
        try {
          wait();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    itsTimer.stop();
    itsTimer = null;
  }

  /**
   * Draw our cached image of the graph to the screen. Also checks for resize
   * events and resizes the image if required.
   */
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (itsImage != null) {
      int w = getSize().width;
      int h = getSize().height;
      if (itsOldWidth != w || itsOldHeight != h) {
        // We've been resized, so need to resize the image of the graph
        itsOldWidth = w;
        itsOldHeight = h;
        itsImage = itsGraph.createBufferedImage(w, h);
      }
      // Draw our image to the display
      g.drawImage(itsImage, 0, 0, this);
    }
  }

  /** Free all resources so that this MonPanel can disappear. */
  public void vaporise() {
    // synchronized (itsPointNames) {
    itsKeepRunning = false;
    // Awake our thread so it can clean-up and die
    synchronized (this) {
      this.notifyAll();
    }
    // }
  }

  /**
   * Merge new data into the data sets used by the graphing widget. A few of the
   * activities need to be performed by calling the GUI event thread to prevent
   * the data sets from becoming corrupted.
   */
  private void mergeData(int axis, int series, Vector data) {
    if (data == null) {
      data = new Vector(); // Create dummy data
    }

    final TimeSeries ts = ((TimeSeriesCollection) itsData.get(axis))
        .getSeries(series);
    synchronized (ts) {
      // We need to knock any old data off the end
      int end = 0;
      while (end < ts.getItemCount()) {
        // Get timestamp for the next data in the collection
        AbsTime t = AbsTime.factory(ts.getTimePeriod(end).getStart());
        if (t.isBefore(itsStart)) {
          // Data is too old to keep
          end++;
        } else {
          break; // No more data needs to be removed
        }
      }

      if (end > 0) {
        final int last = end;
        // We found some data to remove
        // Need to use swing, in case redraw happens mid-deletion
        Runnable delOldData = new Runnable() {
          public void run() {
            ts.delete(0, last - 1);
          }
        };
        try {
          SwingUtilities.invokeAndWait(delOldData);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      // Convert and add new data points to the new series
      final Vector newdata = new Vector();
      for (int i = 0; i < data.size(); i++) {
        PointData pd = (PointData) data.get(i);
        TimeSeriesDataItem di = toTimeSeriesDataItem(pd);
        if (di != null) {
          newdata.add(di);
        } else {
          // System.err.println("ProviderATTimeSeries:mergeData: Bad Data
          // Point");
        }
      }

      // Use the event-dispatch thread to alert the GUI of the update
      Runnable newDataAdded = new Runnable() {
        public void run() {
          for (int i = 0; i < newdata.size(); i++) {
            try {
              ts.add((TimeSeriesDataItem) newdata.get(i));
            } catch (Exception e) {
              System.err.println("ProviderATTimeSeries:mergeData2: "
                  + e.getClass().getName() + "\"" + e.getMessage() + "\"");
            }
          }
        }
      };
      try {
        SwingUtilities.invokeAndWait(newDataAdded);
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
    if (data.size() > 0) {
      // Record the timestamp of the most recent data
      itsPointEpochs.get(axis).set(series, ((PointData) data
          .lastElement()).getTimestamp());
    }
  }

  /** */
  public void actionPerformed(ActionEvent e) {
    System.err.println("ATTimeSeries: Time to update graph \"" + itsTitle
        + "\"");
    synchronized (this) {
      this.notifyAll();
    }
  }

  /** Clear any current setup. */
  public void blankSetup() {
    itsPointNames.clear();
    itsPointEpochs.clear();
    for (int i = 0; i < itsNumAxis; i++) {
      ((TimeSeriesCollection) itsData.get(i)).removeAllSeries();
    }
    itsData.clear();
    itsTimeZone = null;
    itsTZ=null;
    synchronized (this) {
      this.notifyAll();
    }
  }

  /**
   * Configure this MonPanel to use the specified setup. This method can be used
   * to restore saved states, eg what monitor points to graph and over what time
   * range.
   * 
   * @param setup
   *          class-specific setup information.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if
   *         there was a problem and the setup cannot be used.
   */
  public boolean loadSetup(SavedSetup setup) {
    if (!setup.checkClass(this)) {
      System.err.println("ATTimeSeries:loadSetup: setup not for "
          + this.getClass().getName());
      return false;
    }
    // the copy of the setup held by the frame is now incorrect
    if (itsFrame instanceof MonFrame) {
      ((MonFrame) itsFrame).itsSetup = null;
    }

    itsSetup = setup;
    // Tell the thread that we have been reconfigured
    itsReInit = true;
    // Wake it up if it is sleeping..
    synchronized (this) {
      this.notifyAll();
    }
    return true;
  }

  public boolean loadSetupReal() {
    try {
      itsReInit = false;
      SavedSetup setup = itsSetup;
      itsImage = null;
      itsTimer.stop();

      // title
      String temp = (String) setup.get("title");
      itsTitle = temp;

      // TimeZone
      temp = (String) setup.get("timezone");
      if (temp == null) {
        itsTimeZone = null;
        itsTZ= TimeZone.getDefault();
      } else {
        itsTimeZone = temp;
        itsTZ= TimeZone.getTimeZone(itsTimeZone);        
        System.err.println("ATTimeSeries:loadData: Found TimeZone="
            + itsTimeZone);
      }

      // period
      temp = (String) setup.get("period");
      itsPeriod = RelTime.factory(Long.parseLong(temp));

      // update interval
      temp = (String) setup.get("update");
      if (temp != null) {
        itsUpdateInterval = RelTime.factory(Long.parseLong(temp));
      } else {
        itsUpdateInterval = RelTime.factory(60000000);
      }

      // graph mode
      temp = (String) setup.get("mode");
      if (temp.equals("dynamic")) {
        itsRealtime = true;
        itsStart = AbsTime.factory((new AbsTime()).getValue()
            - itsPeriod.getValue());
      } else {
        itsRealtime = false;
        // Read the start epoch for the static graph
        temp = (String) setup.get("start");
        itsStart = AbsTime.factory(Long.parseLong(temp));
      }

      // legend
      temp = (String) setup.get("legend");
      if (temp.equals("true")) {
        itsShowLegend = true;
      } else {
        itsShowLegend = false;
      }

      // display a please wait message to the user
      Runnable pleasewait = new Runnable() {
        public void run() {
          removeAll();
          add(itsPleaseWait);
          repaint();
        }
      };
      try {
        // Need to do the notification using event thread
        SwingUtilities.invokeLater(pleasewait);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // clear old data
      itsPointNames.clear();
      itsPointEpochs.clear();
      itsData.clear();

      // Read the number of axis
      temp = (String) setup.get("numaxis");
      itsNumAxis = Integer.parseInt(temp);
      // allocate new containers for data storage
      for (int i = 0; i < itsNumAxis; i++) {
        itsPointNames.add(new Vector());
        itsPointEpochs.add(new Vector());
        itsData.add(new TimeSeriesCollection(itsTZ));
      }

      // Make new graph
      String tunits = "Time";
      if (itsPeriod.getValue() > 3 * 86400000000l) {
        tunits = "Date";
      }
      if (itsTimeZone != null) {
        tunits = tunits + " (" + itsTZ.getDisplayName() + ")";
      }
      final JFreeChart chart = ChartFactory.createTimeSeriesChart(itsTitle,
          tunits, "Value", (TimeSeriesCollection) itsData.get(0),
          itsShowLegend, true, false);

      XYPlot plot = chart.getXYPlot();

      for (int i = 0; i < itsNumAxis; i++) {
        // Read the title for this axis
        String axislabel = (String) setup.get("label" + i);

        NumberAxis newaxis = null;
        if (i == 0) {
          newaxis = (NumberAxis) plot.getRangeAxis();
          newaxis.setLabel(axislabel);
          DateAxis timeaxis = (DateAxis) plot.getDomainAxis();
          timeaxis.setTimeZone(itsTZ);
          timeaxis.setAutoRange(false);
          timeaxis.setMaximumDate((itsStart.add(itsPeriod)).getAsDate());
          timeaxis.setMinimumDate(itsStart.getAsDate());
        } else {
          newaxis = new NumberAxis(axislabel);
          newaxis.setAutoRangeIncludesZero(false);
          plot.setRangeAxis(i, newaxis);
          plot.setDataset(i, (TimeSeriesCollection) itsData.get(i));
          plot.mapDatasetToRangeAxis(i, new Integer(i));
        }

        // Is there a limit to the number of samples to display?
        temp = (String) setup.get("maxsamps");
        if (temp == null) {
          itsMaxSamps = 0;
        } else {
          itsMaxSamps = Integer.parseInt(temp);
        }

        // Read whether to auto-scale data or use a specified scale
        temp = (String) setup.get("scalemode" + i);
        if (temp.equals("auto")) {
          // Autoscale this axis
          newaxis.setAutoRange(true);
          temp = (String) setup.get("showzero" + i);
          if (temp.equals("true")) {
            newaxis.setAutoRangeIncludesZero(true);
          } else {
            newaxis.setAutoRangeIncludesZero(false);
          }
        } else if (temp.equals("fixed")) {
          // Use the specified scale
          newaxis.setAutoRange(false);
          temp = (String) setup.get("scalemin" + i);
          double min = Double.parseDouble(temp);
          temp = (String) setup.get("scalemax" + i);
          double max = Double.parseDouble(temp);
          newaxis.setLowerBound(min);
          newaxis.setUpperBound(max);
        } else {
          System.err.println("ATTimeSeries:loadData: Unknown mode \"" + temp
              + "\"");
          blankSetup();
          return false;
        }

        // Read which renderer to use to draw the data
        XYItemRenderer renderer = null;
        temp = (String) setup.get("style" + i);
        if (temp.equals("lines")) {
          // Render with lines
          // Check if we should enable discontinuous lines
          temp = (String) setup.get("gaps" + i);
          if (temp.equals("true")) {
            renderer = new StandardXYItemRenderer(
                StandardXYItemRenderer.DISCONTINUOUS_LINES);
            ((StandardXYItemRenderer) renderer).setPlotLines(true);
            // I don't actually understand what this threashold means!
            ((StandardXYItemRenderer) renderer).setGapThreshold(30.0);
          } else {
            renderer = new StandardXYItemRenderer();
            ((StandardXYItemRenderer) renderer).setPlotLines(true);
          }
        } else if (temp.equals("dots")) {
          // Render with dots
          renderer = new XYDotRenderer();
        } else if (temp.equals("symbols")) {
          // Render with symbols
          renderer = new StandardXYItemRenderer();
          ((StandardXYItemRenderer) renderer).setPlotLines(false);
          ((StandardXYItemRenderer) renderer).setBaseShapesVisible(true);
          ((StandardXYItemRenderer) renderer).setBaseShapesFilled(false);
        } else {
          System.err.println("ATTimeSeries:loadData: Unknown style \"" + temp
              + "\"");
          blankSetup();
          return false;
        }
        plot.setRenderer(i, renderer);

        // Read the number of monitor points for this axis
        temp = (String) setup.get("numpoints" + i);
        int numpoints = Integer.parseInt(temp);
        temp = (String) setup.get("points" + i);
        StringTokenizer st = new StringTokenizer(temp, ":");
        Vector<String> thesepoints = itsPointNames.get(i);
        Vector<AbsTime> thesetimes = itsPointEpochs.get(i);
        for (int j = 0; j < numpoints; j++) {
          thesepoints.add(st.nextToken());
          thesetimes.add(AbsTime.factory(itsStart));
        }
      } // end of "set up each axis" loop

      makeSeries(); // Create new containers for the data

      Runnable displaygraph = new Runnable() {
        public void run() {
          setGraph(chart);
        }
      };
      try {
        // Need to do the notification using event thread
        SwingUtilities.invokeAndWait(displaygraph);
      } catch (Exception e) {
        e.printStackTrace();
      }

      for (int a = 0; a < itsNumAxis; a++) {
        Vector points = (Vector) itsPointNames.get(a);
        for (int i = 0; i < points.size(); i++) {
          // Get archival data from the archive
          AbsTime endtime = itsStart.add(itsPeriod);
          Vector v = getArchive((String) points.get(i), itsStart, endtime);
          mergeData(a, i, v);

          Runnable drawnow = new Runnable() {
            public void run() {
              // Update our cached image of the graph
              ((TimeSeriesCollection) itsData.get(0)).getSeries(0).setNotify(
                  true);
              ((TimeSeriesCollection) itsData.get(0)).getSeries(0).setNotify(
                  false);
              int w = getSize().width;
              int h = getSize().height;
              if (itsGraph != null) {
                itsImage = itsGraph.createBufferedImage(w, h);
              }
              repaint();
            }
          };
          try {
            // Need to do the notification using event thread
            SwingUtilities.invokeAndWait(drawnow);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

      itsTimer.setRepeats(true);
      itsTimer.setDelay((int) (itsUpdateInterval.getValue() / 1000));
      itsTimer.stop();
      itsTimer.start();

    } catch (Exception e) {
      System.err.println("ATTimeSeries:loadSetup: " + e.getClass().getName()
          + " " + e.getMessage());
      e.printStackTrace();
      blankSetup();
      return false;
    }

    return true;
  }

  /**
   * Get the current class-specific configuration for this MonPanel. This can be
   * used to capture the current state of the MonPanel so that it can be easily
   * recovered later.
   * 
   * @return class-specific configuration information.
   */
  public SavedSetup getSetup() {
    return itsSetup;
  }

  /**
   * Configure this panel to display the new graph. This will also remove the
   * old graph if it exists.
   * 
   * @param The
   *          graph to display.
   */
  private void setGraph(JFreeChart newgraph) {
    if (itsChartPanel == null) {
      itsChartPanel = new ChartPanel(newgraph);
      itsChartPanel.setMouseZoomable(true, false);
      itsChartPanel.setPreferredSize(new Dimension(600, 250));
      itsChartPanel.setMinimumSize(new Dimension(100, 100));
      // add(itsChartPanel);
    } else {
      itsChartPanel.setChart(newgraph);
    }
    itsGraph = newgraph;
    validate();
    repaint();
  }

  /**
   * Determine which monitor data server to use and try to establish a
   * connection to it.
   */
  private void findServer() {
    itsServer = MonClientUtil.getServer();
  }

  /** Request archival data from the monitor data server. */
  protected Vector getArchive(String pointname, AbsTime t1, AbsTime t2) {
    Vector v = null;
    if (itsMaxSamps == 0) {
      v = itsServer.getPointData(pointname, t1, t2);
    } else {
      v = itsServer.getPointData(pointname, t1, t2, itsMaxSamps);
    }
    return v;
  }

  /**
   * Request all data since the specified time from the monitor data server.
   */
  protected Vector<PointData> getSince(String pointname, AbsTime t1) {
    // Request the data from the server
    Vector<PointData> v = itsServer.getPointData(pointname, t1, new AbsTime());
    // Remove first element - we already have that
    if (v != null && v.size() > 0) {
      v.remove(v.firstElement());
    }
    return v;
  }

  /** Build container for data and invent sensible names for data series. */
  private void makeSeries() {
    synchronized (theirLock) {
      for (int a = 0; a < itsNumAxis; a++) {
        Vector points = (Vector) itsPointNames.get(a);
        TimeSeriesCollection series = (TimeSeriesCollection) itsData.get(a);
        for (int i = 0; i < points.size(); i++) {
          // Create TimeSeries for this point on this axis
          String label = (String) points.get(i);
          int firstdot = label.indexOf(".");
          int lastdot = label.lastIndexOf(".");
          if (firstdot != -1 && lastdot != -1 && firstdot != lastdot) {
            label = label.substring(0, firstdot) + " "
                + label.substring(lastdot + 1);
          }
          TimeSeries ts = new TimeSeries(label);
          // Turn notify off so the graph doesn't redraw unnecessarily
          ts.setNotify(false);
          series.addSeries(ts);
        }
      }
    }
  }

  /**
   * Convert the monitor 'PointData' to a JFreeChart 'TimeSeriesDataItem'. This
   * converts the timestamp to a suitable format and also converts the data into
   * a 'Number' type. If no conversion was possible, eg because we cannot
   * convert the data type into a Number, then <tt>null</tt> will be returned.
   * 
   * @param pd
   *          The PointData to convert
   * @return TimeSeriesDataItem, or <tt>null</tt> if no conversion was
   *         possible.
   */
  protected TimeSeriesDataItem toTimeSeriesDataItem(PointData pd) {
    if (pd == null || pd.getTimestamp() == null || pd.getData() == null) {
      return null;
    }

    try {
      Millisecond t = new Millisecond(pd.getTimestamp().getAsDate(), itsTZ, Locale.getDefault());
      Number d = toNumber(pd.getData());
      if (d == null) {
        return null;
      }
      return new TimeSeriesDataItem(t, d);
    } catch (Exception e) {
      // Some odd problems happen in the code called above...
      System.err
          .println("ATTimeSeries:toTimeSeriesDataItem: " + e.getMessage());
      return null;
    }
  }

  /**
   * Convert the given data to a <i>Number</i>, if possible. <tt>null</tt>
   * will be returned if no conversion is possible.
   */
  protected static Number toNumber(Object data) {
    if (data == null) {
      return null;
    }
    if (data instanceof Boolean) {
      if (((Boolean) data).booleanValue()) {
        return new Integer(1);
      } else {
        return new Integer(0);
      }
    }
    if (data instanceof Number) {
      return (Number) data;
    }
    if (data instanceof HourAngle) {
      return new Double(((Angle) data).getValue() * 24.0 / (2 * Math.PI));
    }
    if (data instanceof Angle) {
      return new Double(((Angle) data).getValueDeg());
    }
    if (data instanceof AbsTime) {
      return new Double(((AbsTime) data).getAsSeconds());
    }
    if (data instanceof RelTime) {
      return new Double(((RelTime) data).getAsSeconds());
    }
    if (data instanceof String) {
      try {
        return new Double((String) data);
      } catch (Exception e) {
        return null;
      }
    }

    return null;
  }

  /**
   * Get a Panel with all the controls required to configure this provider.
   * 
   * @return GUI controls to configure this data provider.
   */
  public MonPanelSetupPanel getControls() {
    return new ATTimeSeriesSetupPanel(this, itsFrame);
  }

  /**
   * Dump current data to the given output stream. This is the mechanism through
   * which data can be exported to a file.
   * 
   * @param p
   *          The print stream to write the data to.
   */
  public void export(PrintStream p) {
    final String rcsid = "$Id: ATTimeSeries.java,v 1.11 2006/06/22 04:39:07 bro764 Exp $";
    p.println("#Dump from ATTimeSeries " + rcsid);
    p.println("#Data dumped at "
        + (new AbsTime().toString(AbsTime.Format.UTC_STRING)));
    p.println("#Each data sample has three, comma separated columns:");
    p
        .println("#1) Hex Binary Atomic Time (BAT) timestamp, e.g. 0x104f629d0c68e0");
    p
        .println("#2) Formatted time stamp, Universal Time, e.g. 2004-05-10 05:42:35.596");
    p.println("#3) Numeric data value, e.g. 7.49");

    for (int a = 0; a < itsNumAxis; a++) {
      TimeSeriesCollection series = (TimeSeriesCollection) itsData.get(a);
      for (int c = 0; c < series.getSeriesCount(); c++) {
        TimeSeries data = series.getSeries(c);
        if (data.getItemCount() == 0) {
          continue;
        }
        // Print the header information for this series
        p.println("#" + data.getItemCount() + " data for " + data.getDescription()
            + " follow");
        for (int i = 0; i < data.getItemCount(); i++) {
          AbsTime tm = AbsTime.factory(data.getTimePeriod(i).getStart());
          p.print(tm.toString(AbsTime.Format.HEX_BAT) + ", ");
          p.print(tm.toString(AbsTime.Format.UTC_STRING) + ", ");
          p.println(data.getValue(i));
        }
        p.println();
      }
    }

    p.println();
    p.println();
  }

  public String getLabel() {
    return null;
  }
}
