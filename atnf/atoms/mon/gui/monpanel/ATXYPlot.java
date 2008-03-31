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
import atnf.atoms.mon.util.MonitorTimer;
import atnf.atoms.time.*;
import atnf.atoms.util.*;

import java.util.Vector;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.lang.Number;
import java.text.SimpleDateFormat;
import java.io.PrintStream;
import javax.swing.table.*;
import javax.swing.event.*;
import java.net.URL;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.data.time.*;
import org.jfree.data.*;


/**
 * Class for matching the time-stamps of monitor points and plotting the
 * value of one vs. the value of the other.
 *
 * @author David Brodrick
 * @version $Id: ATXYPlot.java,v 1.7 2006/05/26 01:32:13 bro764 Exp $
 * @see MonPanel
 */
public class
ATXYPlot
extends MonPanel
implements ActionListener, Runnable
{
  static {
    MonPanel.registerMonPanel("X vs Y Plot", ATXYPlot.class);
  }


  public class
  ATXYPlotSetupPanel
  extends MonPanelSetupPanel
  implements ActionListener
  {
    public class ImagePanel extends JPanel {
      private Image itsImage = null;

      /** Specify the image to be displayed. */
      public void setImage(Image arg)
      {
	itsImage = arg;
        repaint();
      }

      /** paints this object onto the window */
      public void paintComponent(Graphics g)
      {
	super.paintComponent(g); //paint background

	//Draw image at its natural size first.
	int w = getWidth();
	int h = getHeight();
	if (itsImage!=null)
	  g.drawImage(itsImage, 0, 0, w, h, this);
      }
    }

    /** The main panel which hold our GUI controls. */
    protected JPanel itsSetupPanel = new JPanel();
    /** Sub-panel that holds the time-matching-mode diagram. */
    protected ImagePanel itsImagePanel = new ImagePanel();

    /** TextField for the graph title. */
    protected JTextField itsTitleField = new JTextField(40);
    /** Label which records the number of value axis. */
    protected JLabel itsNumAxis = new JLabel("1");
    /** Vector which contains the setup panels for each of the axis. */
    protected Vector itsAxis = new Vector();
    /** Panel which contains the AxisSetup components. */
    protected JPanel itsAxisPanel = new JPanel();
    /** Check box, records whether to show the Legend. */
    protected JCheckBox itsShowLegend = new JCheckBox("Show Legend");
    /** Text field hold the time span of the graph. */
    protected JTextField itsPeriod = new JTextField("12", 6);
    /** Options for the time period selection combo box. */
    final private String[] itsTimeOptions = {"Hours", "Days", "Minutes"};
    /** combo box to give time unit selection */
    protected JComboBox itsPeriodUnits = new JComboBox(itsTimeOptions);
    /** Records if the graph should update in real-time. */
    protected JRadioButton itsDynamic = new JRadioButton("Real-Time (Dynamic plot showing latest data)");
    /** Records if the graph should remain static. */
    protected JRadioButton itsStatic = new JRadioButton("Archival");
    /** Allows user to enter graph start time for archival mode. */
    protected JTextField itsStart = new JTextField(16);
    /** The format of the dates we parse. */
    protected SimpleDateFormat itsFormatter
      = new SimpleDateFormat ("yyyy/MM/dd HH:mm");
    /** How often to update the graph in seconds. */
    protected JTextField itsUpdateRate = new JTextField("60",4);
    /** The different options for how to find matching X and Y points. */
    final private String[] itsMatchOptions = {"Nearest-in-time",
    "Interpolated value", "All neighbours", "First after"};
    /** Lets user select time-matching mode. */
    protected JComboBox itsMatch = new JComboBox(itsMatchOptions);
    /** Diagrams for the different matching modes. */
    protected Image[] itsDiagrams = null;

    /** Reference to the point source selector for this axis. */
    public XvsYTreeSelector itsPoints = new XvsYTreeSelector();

    /** The checkbox to enable auto-scaling. */
    public JRadioButton itsXAutoScale = new JRadioButton("Auto-Scale");
    /** If autoscaling, should we include zero? */
    public JCheckBox itsXAutoZero = new JCheckBox("Include Zero");
    /** The checkbox to enable manual scaling. */
    public JRadioButton itsXSpecifyScale = new JRadioButton("Specify Scale");
    /** Textfield to hold the scale minimum. */
    public JTextField itsXScaleMin = new JTextField(8);
    /** Textfield to hold the scale maximum. */
    public JTextField itsXScaleMax = new JTextField(8);

    /** The checkbox to enable auto-scaling. */
    public JRadioButton itsYAutoScale = new JRadioButton("Auto-Scale");
    /** If autoscaling, should we include zero? */
    public JCheckBox itsYAutoZero = new JCheckBox("Include Zero");
    /** The checkbox to enable manual scaling. */
    public JRadioButton itsYSpecifyScale = new JRadioButton("Specify Scale");
    /** Textfield to hold the scale minimum. */
    public JTextField itsYScaleMin = new JTextField(8);
    /** Textfield to hold the scale maximum. */
    public JTextField itsYScaleMax = new JTextField(8);

    /** Textfield to hold the label for the X axis. */
    public JTextField itsXAxisLabel = new JTextField("X axis", 16);
    /** Textfield to hold the label for the Y axis. */
    public JTextField itsYAxisLabel = new JTextField("Y axis", 16);

    /** Radio button for drawing data with lines. */
    public JRadioButton
      itsDrawLines = new JRadioButton("Lines");
    /** Check box to allow discontinuous lines. */
    public JCheckBox itsDiscontinuousLines = new JCheckBox("Allow Breaks");
    /** Radio button for drawing data with dots. */
    public JRadioButton itsDrawDots = new JRadioButton("Dots");
    /** Radio button for drawing data with symbols. */
    public JRadioButton itsDrawSymbols = new JRadioButton("Shapes");

    /** Radio button to select the display of all X-Y matches. */
    public JRadioButton itsShowAllMatches = new JRadioButton("Show all matched X/Y points");
    /** Radio button to enable display of post-processed data. */
    public JRadioButton itsPostProcess = new JRadioButton("Post-process into bins of");
    /** Textfield for bin size entry. */
    public JTextField itsBinSize = new JTextField("1.0");
    /** Post-processing options. */
    final private String[] itsPPOptions = {"Average", "RMS", "Count of"};
    protected JComboBox itsPPCombo = new JComboBox(itsPPOptions);

    /** Construct the setup editor for the specified panel. */
    public ATXYPlotSetupPanel(ATXYPlot panel, JFrame frame)
    {
      super(panel, frame);

      /** Load the diagrams for the different time-matching modes. */
      itsDiagrams = new Image[4];
      String imagepath = "atnf/atoms/mon/gui/monpanel/";
      URL url = this.getClass().getClassLoader().getResource(imagepath+"XvsY-nearest.gif");
      itsDiagrams[0] = Toolkit.getDefaultToolkit().getImage(url);
      url = this.getClass().getClassLoader().getResource(imagepath+"XvsY-interp.gif");
      itsDiagrams[1] = Toolkit.getDefaultToolkit().getImage(url);
      url = this.getClass().getClassLoader().getResource(imagepath+"XvsY-allnearest.gif");
      itsDiagrams[2] = Toolkit.getDefaultToolkit().getImage(url);
      url = this.getClass().getClassLoader().getResource(imagepath+"XvsY-after.gif");
      itsDiagrams[3] = Toolkit.getDefaultToolkit().getImage(url);
      itsImagePanel.setImage(itsDiagrams[0]);
      itsImagePanel.setPreferredSize(new Dimension(400,100));
      itsImagePanel.setToolTipText("Each green line would be a point " +
				   "in your plot");

      itsTitleField.addActionListener(this);
      itsStart.addActionListener(this);
      itsUpdateRate.addActionListener(this);
      itsPeriod.addActionListener(this);

      itsSetupPanel.setLayout(new BorderLayout());
      JPanel globalpanel = new JPanel();
      globalpanel.setLayout(new BoxLayout(globalpanel, BoxLayout.Y_AXIS));
      JPanel temppanel = new JPanel();
      temppanel.setLayout(new BorderLayout());

      itsDynamic.addActionListener(this);
      itsDynamic.setActionCommand("Dynamic");
      itsStatic.addActionListener(this);
      itsStatic.setActionCommand("Static");
      ButtonGroup tempgroup = new ButtonGroup();
      tempgroup.add(itsDynamic);
      tempgroup.add(itsStatic);

      itsShowAllMatches.addActionListener(this);
      itsShowAllMatches.setActionCommand("show-all");
      itsShowAllMatches.setToolTipText("Draw a point for every matched X and Y point");
      itsPostProcess.addActionListener(this);
      itsPostProcess.setActionCommand("post-proc");
      itsPostProcess.setToolTipText("Display a processed quantity, not just every X/Y match");
      tempgroup = new ButtonGroup();
      tempgroup.add(itsShowAllMatches);
      tempgroup.add(itsPostProcess);

      itsDynamic.doClick();
      itsShowLegend.setSelected(true);

      JPanel temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));
      JLabel templabel = new JLabel("Plot Title:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter a descriptive title for this plot");
      temppanel2.add(templabel);
      temppanel.add(temppanel2, BorderLayout.WEST);

      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));
      itsTitleField.setToolTipText("Enter a descriptive title for this plot");
      temppanel2.add(itsTitleField);
      temppanel.add(temppanel2, BorderLayout.CENTER);

      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);

      temppanel = new JPanel();
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
      temppanel2 = new JPanel();
      templabel = new JLabel("Input Data Time Span:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter time span for graph data");
      temppanel2.add(templabel);
      temppanel2.add(itsPeriod);
      temppanel2.add(itsPeriodUnits);
      temppanel.add(temppanel2);
      temppanel2 = new JPanel();
      itsDynamic.setToolTipText("Graph will automatically update to show new data");
      temppanel.add(itsDynamic);
      temppanel2 = new JPanel();
      itsStatic.setToolTipText("Graph will only show the specified data");
      temppanel2.add(itsStatic);
      templabel = new JLabel("Graph Start Time (yyyy/MM/dd HH:mm):");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter data start time - yyyy/MM/dd HH:mm");
      temppanel2.add(templabel);
      itsStart.setToolTipText("Format: yyyy/MM/dd HH:mm in timezone selected above");
      itsStart.setText(itsFormatter.format(new Date()));
      temppanel2.add(itsStart);
      temppanel.add(temppanel2);
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);

      temppanel = new JPanel();
      //temppanel.setLayout(new GridLayout(1,2));
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      itsUpdateRate.setToolTipText("How often to update/redraw the graph "
				   + "(faster chews more CPU)");
      templabel = new JLabel("Graph update/redraw interval (in seconds)");
      templabel.setToolTipText("How often to update/redraw the graph "
			       + "(faster chews more CPU)");
      temppanel.add(templabel);
      temppanel.add(itsUpdateRate);
      itsShowLegend.setToolTipText("Should a legend be shown on the graph?");
      temppanel.add(itsShowLegend);
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);

      temppanel = new JPanel();
      tempgroup = new ButtonGroup();
      tempgroup.add(itsXAutoScale);
      tempgroup.add(itsXSpecifyScale);
      tempgroup = new ButtonGroup();
      tempgroup.add(itsYAutoScale);
      tempgroup.add(itsYSpecifyScale);
      tempgroup = new ButtonGroup();
      tempgroup.add(itsDrawLines);
      tempgroup.add(itsDrawDots);
      tempgroup.add(itsDrawSymbols);

      itsPoints.setToolTipText("Select point to be graphed");
      itsPoints.setPreferredSize(new Dimension(180,180));
      globalpanel.add(itsPoints);

      temppanel = new JPanel();
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BorderLayout());
      templabel = new JLabel("Select matching mode:", JLabel.LEFT);
      templabel.setToolTipText("Determines how X and Y points are "
			       + "matched for plotting");
      temppanel2.add(templabel, BorderLayout.CENTER);
      itsMatch.setToolTipText("Determines how X and Y points are "
			      + "matched for plotting");
      itsMatch.setActionCommand("Match");
      itsMatch.addActionListener(this);
      itsMatch.setMaximumSize(new Dimension(200, 30));
      temppanel2.add(itsMatch, BorderLayout.SOUTH);
      temppanel.add(temppanel2);
      temppanel.add(itsImagePanel);
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      globalpanel.add(temppanel);

      temppanel = new JPanel();
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
      temppanel2 = new JPanel();
      //temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
      itsShowAllMatches.setAlignmentY(Component.BOTTOM_ALIGNMENT);
      temppanel2.setAlignmentY(Component.BOTTOM_ALIGNMENT);
      temppanel2.add(itsShowAllMatches);
      temppanel.add(temppanel2);
      temppanel2 = new JPanel();
      //temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.X_AXIS));
      temppanel2.add(itsPostProcess);
      itsBinSize.setPreferredSize(new Dimension(45, 21));
      itsBinSize.setMinimumSize(new Dimension(25, 17));
      itsBinSize.setMaximumSize(new Dimension(55, 21));
      temppanel2.add(itsBinSize);
      templabel = new JLabel("\"X units\" and then plot the ");
      temppanel2.add(templabel);
      itsPPCombo.setToolTipText("Select what value to calculate");
      itsPPCombo.setPreferredSize(new Dimension(83, 24));
      temppanel2.add(itsPPCombo);
      templabel = new JLabel(" Y value(s)");
      temppanel2.add(templabel);
      temppanel.add(temppanel2);
      globalpanel.add(temppanel);
      itsShowAllMatches.doClick();

      JPanel axispanel = new JPanel();
      axispanel.setLayout(new BoxLayout(axispanel, BoxLayout.X_AXIS));
      axispanel.setBorder(BorderFactory.createLineBorder(Color.black));
      templabel = new JLabel("X axis:");
      templabel.setForeground(Color.black);
      axispanel.add(templabel);

      temppanel = new JPanel();
      //temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));
      templabel = new JLabel("Axis Label:");
      templabel.setForeground(Color.black);
      temppanel.add(templabel);

      itsXAxisLabel.setToolTipText("Enter label for X axis");
      temppanel.add(itsXAxisLabel);
      temppanel2.add(temppanel);

      temppanel = new JPanel();
      //temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      itsXAutoScale.setToolTipText("Automatically scale X-axis");
      itsXAutoScale.addActionListener(this);
      itsXAutoScale.setActionCommand("X-Auto-Scale");
      itsXAutoScale.doClick();
      temppanel.add(itsXAutoScale);
      itsXAutoZero.setToolTipText("Always include zero level in scale");
      itsXAutoZero.setSelected(false);
      temppanel.add(itsXAutoZero);
      temppanel2.add(temppanel);

      temppanel = new JPanel();
      //temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
      itsXSpecifyScale.setToolTipText("Manually specify max and min range for the axis");
      itsXSpecifyScale.addActionListener(this);
      itsXSpecifyScale.setActionCommand("X-Specify-Scale");
      temppanel.add(itsXSpecifyScale);
      templabel = new JLabel("Min:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Specify minimum value for the value axis");
      temppanel.add(templabel);
      itsXScaleMin.setToolTipText("Specify minimum value for the value axis");
      temppanel.add(itsXScaleMin);
      templabel = new JLabel("Max:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Specify maximum value for the value axis");
      temppanel.add(templabel);
      itsXScaleMax.setToolTipText("Specify maximum value for the value axis");
      temppanel.add(itsXScaleMax);
      temppanel2.add(temppanel);
      axispanel.add(temppanel2);
      globalpanel.add(axispanel);

      axispanel = new JPanel();
      axispanel.setLayout(new BoxLayout(axispanel, BoxLayout.X_AXIS));
      axispanel.setBorder(BorderFactory.createLineBorder(Color.black));
      templabel = new JLabel("Y axis:");
      templabel.setForeground(Color.black);
      axispanel.add(templabel);

      temppanel = new JPanel();
      temppanel2 = new JPanel();
      temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));
      templabel = new JLabel("Axis Label:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Enter label for Y axis");
      temppanel.add(templabel);
      itsYAxisLabel.setToolTipText("Enter label for Y axis");
      temppanel.add(itsYAxisLabel);
      temppanel2.add(temppanel);

      temppanel = new JPanel();
      itsYAutoScale.setToolTipText("Automatically scale Y axis");
      itsYAutoScale.addActionListener(this);
      itsYAutoScale.setActionCommand("Y-Auto-Scale");
      itsYAutoScale.doClick();
      temppanel.add(itsYAutoScale);
      itsYAutoZero.setToolTipText("Always include zero level in scale");
      itsYAutoZero.setSelected(false);
      temppanel.add(itsYAutoZero);
      temppanel2.add(temppanel);

      temppanel = new JPanel();
      itsYSpecifyScale.setToolTipText("Manually specify max and min range for the axis");
      itsYSpecifyScale.addActionListener(this);
      itsYSpecifyScale.setActionCommand("Y-Specify-Scale");
      temppanel.add(itsYSpecifyScale);
      templabel = new JLabel("Min:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Specify minimum value for the value axis");
      temppanel.add(templabel);
      itsYScaleMin.setToolTipText("Specify minimum value for the value axis");
      temppanel.add(itsYScaleMin);
      templabel = new JLabel("Max:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Specify maximum value for the value axis");
      temppanel.add(templabel);
      itsYScaleMax.setToolTipText("Specify maximum value for the value axis");
      temppanel.add(itsYScaleMax);
      temppanel2.add(temppanel);
      axispanel.add(temppanel2);
      globalpanel.add(axispanel);

      temppanel = new JPanel();
      temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
      templabel = new JLabel("Plot As:");
      templabel.setForeground(Color.black);
      templabel.setToolTipText("Select data renderer");
      temppanel.add(templabel);
      itsDrawDots.setToolTipText("Draw a dot for each data point");
      temppanel.add(itsDrawDots);
      itsDrawSymbols.setToolTipText("Draw an unfilled shape for each data point");
      itsDrawSymbols.setSelected(true);
      temppanel.add(itsDrawSymbols);
      itsDrawLines.setToolTipText("Draw lines between data points");
      temppanel.add(itsDrawLines);
      itsDiscontinuousLines.setSelected(true);
      itsDiscontinuousLines.setToolTipText("Allow breaks in the line where data is missing");
      temppanel.add(itsDiscontinuousLines);
      globalpanel.add(temppanel);


      /*itsAxisPanel.setLayout(new BoxLayout(itsAxisPanel, BoxLayout.Y_AXIS));
       JScrollPane scroller = new JScrollPane(itsAxisPanel);
       scroller.setPreferredSize(new Dimension(500, 350));
       scroller.setMaximumSize(new Dimension(2000, 350));
       scroller.setBorder(BorderFactory.createLoweredBevelBorder());
       itsSetupPanel.add(scroller, BorderLayout.CENTER);

       itsAxis.add(new AxisSetup());
       itsAxisPanel.add((JPanel)itsAxis.get(0));*/

      //add(new JScrollPane(itsSetupPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      //  		  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
      itsSetupPanel.add(new JScrollPane(globalpanel), BorderLayout.CENTER);
      itsSetupPanel.setPreferredSize(new Dimension(500, 650));
      itsSetupPanel.setMinimumSize(new Dimension(400, 500));
      itsSetupPanel.setMaximumSize(new Dimension(2000, 2000));
      add(itsSetupPanel, BorderLayout.CENTER);
    }


    public
    void
    actionPerformed(ActionEvent e)
    {
      super.actionPerformed(e);
      String cmd = e.getActionCommand();
      if (e.getSource()==itsTitleField ||
	  e.getSource()==itsUpdateRate ||
	  e.getSource()==itsStart ||
	  e.getSource()==itsPeriod) {
	okClicked();
        return;
      }
      if (cmd.equals("Dynamic")) {
	itsStart.setEnabled(false);
	itsStart.setBackground(Color.lightGray);
      } else if (cmd.equals("Static")) {
	itsStart.setEnabled(true);
	itsStart.setBackground(Color.white);
      } else if (cmd.equals("X-Auto-Scale")) {
	itsXScaleMin.setEnabled(false);
	itsXScaleMax.setEnabled(false);
	itsXScaleMin.setBackground(Color.lightGray);
	itsXScaleMax.setBackground(Color.lightGray);
	itsXAutoZero.setEnabled(true);
      } else if (cmd.equals("X-Specify-Scale")) {
	itsXScaleMin.setEnabled(true);
	itsXScaleMax.setEnabled(true);
	itsXScaleMin.setBackground(Color.white);
	itsXScaleMax.setBackground(Color.white);
	itsXAutoZero.setEnabled(false);
      } else if (cmd.equals("Y-Auto-Scale")) {
	itsYScaleMin.setEnabled(false);
	itsYScaleMax.setEnabled(false);
	itsYScaleMin.setBackground(Color.lightGray);
	itsYScaleMax.setBackground(Color.lightGray);
	itsYAutoZero.setEnabled(true);
      } else if (cmd.equals("Y-Specify-Scale")) {
	itsYScaleMin.setEnabled(true);
	itsYScaleMax.setEnabled(true);
	itsYScaleMin.setBackground(Color.white);
	itsYScaleMax.setBackground(Color.white);
	itsYAutoZero.setEnabled(false);
      } else if (cmd.equals("show-all")) {
	itsBinSize.setEnabled(false);
	itsPPCombo.setEnabled(false);
      } else if (cmd.equals("post-proc")) {
	itsBinSize.setEnabled(true);
	itsPPCombo.setEnabled(true);
      } else if (cmd.equals("Match")) {
	String curval = (String)itsMatch.getSelectedItem();
	for (int i=0; i<itsMatchOptions.length; i++) {
	  if (itsMatchOptions[i].equals(curval)) {
	    itsImagePanel.setImage(itsDiagrams[i]);
            break;
	  }
	}
      } else {
	System.err.println("ATXYPlotSetupPanel: got unhandled action: " +
			   cmd);
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
      if (!checkString(itsTitleField.getText())) {
	JOptionPane.showMessageDialog(this,
				      "The Graph Title:\n" +
				      "\"" + itsTitleField.getText() + "\"\n" +
				      "contains reserved characters.\n" +
				      "You must not use \"`\"\n",
				      "Bad Characters in Title",
				      JOptionPane.WARNING_MESSAGE);
	return null;
      }

      SavedSetup setup = new SavedSetup("temp",
                             "atnf.atoms.mon.gui.monpanel.ATXYPlot");

      //Check that all X points have corresponding Y points and vice versa
      Vector xpoints = itsPoints.getXSelections();
      Vector ypoints = itsPoints.getYSelections();
      if (xpoints.size()==0 || ypoints.size()==0
	  || xpoints.size()!=ypoints.size()) {
	JOptionPane.showMessageDialog(this,
				      "You need to select at least one\n" +
				      "X and Y point, and there must be\n" +
				      "a corresponding X point for every\n" +
                                      "Y point and vice versa",
				      "Bad selections for X and Y points",
				      JOptionPane.WARNING_MESSAGE);
	return null;
      }
      //record the number of X and Y points
      setup.put("numpairs", ""+xpoints.size());
      String tempx = "";
      String tempy = "";
      for (int i=0; i<xpoints.size(); i++) {
	tempx += (String)xpoints.get(i) + ":";
	tempy += (String)ypoints.get(i) + ":";
      }
      setup.put("xpoints", tempx);
      setup.put("ypoints", tempy);

      //Save the graph title
      String temp = itsTitleField.getText();
      if (temp==null || temp.equals("")) temp = "X vs. Y";
      setup.put("title", temp);

      //Check that the numeric period field and save it, if okay
      try {
	double foo = Double.parseDouble(itsPeriod.getText());
      } catch (Exception e) {
	JOptionPane.showMessageDialog(this,
				      "The field for the Graph Time Span must contain\n" +
				      "a valid number, eg, \"42\" or \"99.9\".\n",
				      "Bad Period Entered",
				      JOptionPane.WARNING_MESSAGE);
	return null;
      }
      double numtime = Double.parseDouble(itsPeriod.getText());
      String units = (String)itsPeriodUnits.getSelectedItem();
      if (units.equals("Minutes")) {
	numtime *= 60000000l;
      } else if (units.equals("Hours")) {
	numtime *= 60*60000000l;
      } else if (units.equals("Days")) {
	numtime *= 24*60*60000000l;
      }
      setup.put("period", ""+(long)numtime);

      if (itsDynamic.isSelected()) {
	setup.put("mode", "dynamic");
      } else if (itsStatic.isSelected()) {
        setup.put("mode", "static");
	String startstr = itsStart.getText();
	Date date = null;
	try {
	  date = itsFormatter.parse(startstr);
	} catch (Exception e) { date=null; }
	if (date==null) {
	  JOptionPane.showMessageDialog(this,
					"The Graph Start Time you entered\n" +
					"could not be parsed. The time must\n" +
					"be in \"yyyy/MM/dd HH:mm\" format, eg:\n" +
					"\"" + itsFormatter.format(new Date()) + "\"\n",
					"Bad Start Time",
					JOptionPane.WARNING_MESSAGE);
	  return null;
	}
	AbsTime start = AbsTime.factory(date);
	setup.put("start", ""+start.getValue());
      } else return null;
      if (itsShowLegend.isSelected()) {
	setup.put("legend", "true");
      } else {
	setup.put("legend", "false");
      }

      //Check that the update interval is numeric
      long upd = 60000000;
      try {
	upd = Long.parseLong(itsUpdateRate.getText());
      } catch (Exception e) {
	JOptionPane.showMessageDialog(this,
				      "The field for the graph update/redraw interval\n" +
				      "must contain a valid integer, eg, \"60\".\n",
				      "Bad Update Rate",
				      JOptionPane.WARNING_MESSAGE);
	return null;
      }
      //Do a reality check on the actual value
      if (upd<1 || upd>1000000) {
	JOptionPane.showMessageDialog(this,
				      "The field for the graph update/redraw interval\n" +
				      "contains an unreasonable value...\n",
				      "Bad Update Rate",
				      JOptionPane.WARNING_MESSAGE);
	return null;

      }
      setup.put("update", ""+upd*1000000);

      setup.put("matchmode", (String)itsMatch.getSelectedItem());

      /////INSERT KEYS SPECIFIC TO THE X AXIS////////////////////////////
      //Ensure user has not entered any reserved characters
      if (!checkString(itsXAxisLabel.getText())) {
          JOptionPane.showMessageDialog(this,
                                        "The X axis label:\n" +
                                        "\"" + itsXAxisLabel.getText() + "\"\n" +
                                        "contains reserved characters.\n" +
                                        "You must not use ~ : or `\n",
                                        "Reserved Characters",
                                        JOptionPane.WARNING_MESSAGE);
          return null;
      }
      setup.put("Xlabel", itsXAxisLabel.getText());
      if (itsXAutoScale.isSelected()) {
          setup.put("Xscalemode", "auto");
          if (itsXAutoZero.isSelected()) setup.put("Xshowzero", "true");
          else setup.put("Xshowzero", "false");
      } else {
          setup.put("Xscalemode", "fixed");
          //Check that the numeric fields contain parsable number
          try {
	    double foo = Double.parseDouble(itsXScaleMax.getText());
	    foo = Double.parseDouble(itsXScaleMin.getText());
          } catch (Exception e) {
	    JOptionPane.showMessageDialog(this,
					  "The fields for X-scale minimum and maximum\n" +
					  "scales must contain numbers, eg, \"42\"\n" +
					  "or \"99.9\". You can select the Auto-Scale\n" +
					  "option if you don't know what scale to use.\n",
					  "Bad Scale Entered",
					  JOptionPane.WARNING_MESSAGE);
              return null;
          }
          setup.put("Xscalemin", itsXScaleMin.getText());
          setup.put("Xscalemax", itsXScaleMax.getText());
      }

      /////INSERT KEYS SPECIFIC TO THE Y AXIS////////////////////////////
      //Ensure user has not entered any reserved characters
      if (!checkString(itsYAxisLabel.getText())) {
          JOptionPane.showMessageDialog(this,
                                        "The Y axis label:\n" +
                                        "\"" + itsYAxisLabel.getText() + "\"\n" +
                                        "contains reserved characters.\n" +
                                        "You must not use ~ : or `\n",
                                        "Reserved Characters",
                                        JOptionPane.WARNING_MESSAGE);
          return null;
      }
      setup.put("Ylabel", itsYAxisLabel.getText());
      if (itsYAutoScale.isSelected()) {
          setup.put("Yscalemode", "auto");
          if (itsYAutoZero.isSelected()) setup.put("Yshowzero", "true");
          else setup.put("Yshowzero", "false");
      } else {
          setup.put("Yscalemode", "fixed");
          //Check that the numeric fields contain parsable number
          try {
	    double foo = Double.parseDouble(itsYScaleMax.getText());
	    foo = Double.parseDouble(itsYScaleMin.getText());
          } catch (Exception e) {
	    JOptionPane.showMessageDialog(this,
					  "The fields for Y-scale minimum and maximum\n" +
					  "scales must contain numbers, eg, \"42\"\n" +
					  "or \"99.9\". You can select the Auto-Scale\n" +
					  "option if you don't know what scale to use.\n",
					  "Bad Scale Entered",
					  JOptionPane.WARNING_MESSAGE);
              return null;
          }
          setup.put("Yscalemin", itsYScaleMin.getText());
          setup.put("Yscalemax", itsYScaleMax.getText());
      }

      if (itsDrawLines.isSelected()) {
          setup.put("style", "lines");
          if (itsDiscontinuousLines.isSelected())
              setup.put("gaps", "true");
          else setup.put("gaps", "false");
      } else if (itsDrawDots.isSelected()) {
          setup.put("style", "dots");
      } else if (itsDrawSymbols.isSelected()) {
          setup.put("style", "symbols");
      }

      if (itsPostProcess.isSelected()) {
	setup.put("post-proc", "true");
	//Check that the numeric field contains a parsable number
	try {
	  double foo = Double.parseDouble(itsBinSize.getText());
	} catch (Exception e) {
	  JOptionPane.showMessageDialog(this,
					"The field for the number of \"X units\"\n" +
					"for the post-processing bin size could\n" +
					"not be parsed. It MUST contain a valid\n" +
					"number, in the same units that the X axis\n" +
                                        "will display\n",
					"Bad \"Bin Size\" Entered",
					JOptionPane.WARNING_MESSAGE);
	  return null;
	}
	setup.put("binsize", itsBinSize.getText());
	if (((String)itsPPCombo.getSelectedItem()).equals("Average"))
	  setup.put("proc-mode", "avg");
	else if (((String)itsPPCombo.getSelectedItem()).equals("Count of"))
	  setup.put("proc-mode", "cnt");
	else if (((String)itsPPCombo.getSelectedItem()).equals("RMS"))
	  setup.put("proc-mode", "rms");
	else
	  setup.put("proc-mode", "avg");
      } else {
	setup.put("post-proc", "false");
      }

      return setup;
    }


    /** Make the controls show information about the given setup. */
    public
    void
    showSetup(SavedSetup setup)
    {
      try {
	//title
	String temp = (String)setup.get("title");
	itsTitleField.setText(temp);

	//graph mode
	temp = (String)setup.get("mode");
	if (temp.equals("dynamic")) {
	  itsDynamic.doClick();
	} else {
	  itsStatic.doClick();
	  temp = (String)setup.get("start");
	  long start = Long.parseLong(temp);
	  AbsTime atemp = AbsTime.factory(start);
	  Date dtemp = atemp.getAsDate();
	  itsStart.setText(itsFormatter.format(dtemp));
	}

	//graph time span
	temp = (String)setup.get("period");
	long period = Long.parseLong(temp);
	if (period>=2*86400000000l) {
	  //Best displayed in days
	  itsPeriodUnits.setSelectedItem("Days");
	  itsPeriod.setText("" + (period/86400000000l));
	} else if (period>=2*3600000000l) {
	  //Best displayed in hours
	  itsPeriodUnits.setSelectedItem("Hours");
	  itsPeriod.setText("" + (period/3600000000l));
	} else {
	  //Let's show it in minutes
	  itsPeriodUnits.setSelectedItem("Minutes");
	  itsPeriod.setText("" + (period/60000000l));
	}

	//legend
        temp = (String)setup.get("legend");
	if (temp.equals("true")) {
	  itsShowLegend.setSelected(true);
	} else {
	  itsShowLegend.setSelected(false);
	}

	//update interval
	temp = (String)setup.get("update");
	if (temp!=null) {
	  long upd = Long.parseLong(temp);
	  upd/=1000000;
	  itsUpdateRate.setText(""+upd);
	} else {
          itsUpdateRate.setText("60");
	}

	//time matching mode
	temp = (String)setup.get("matchmode");
        itsMatch.setSelectedItem(temp);

        //Extract details about the X axis setup
	temp = (String)setup.get("Xlabel");
        itsXAxisLabel.setText(temp);
        temp = (String)setup.get("Xscalemode");
        if (temp.equals("auto")) {
	  itsXAutoScale.doClick();
	  temp = (String)setup.get("Xshowzero");
	  if (temp.equals("true")) itsXAutoZero.setSelected(true);
	  else if (temp.equals("false")) itsXAutoZero.setSelected(false);
        } else if (temp.equals("fixed")) {
	  itsXSpecifyScale.doClick();
	  temp = (String)setup.get("Xscalemin");
	  itsXScaleMin.setText(temp);
	  temp = (String)setup.get("Xscalemax");
	  itsXScaleMax.setText(temp);
	} else return;

        //Extract details about the Y axis setup
	temp = (String)setup.get("Ylabel");
	itsYAxisLabel.setText(temp);
        temp = (String)setup.get("Yscalemode");
        if (temp.equals("auto")) {
	  itsYAutoScale.doClick();
	  temp = (String)setup.get("Yshowzero");
	  if (temp.equals("true")) itsYAutoZero.setSelected(true);
	  else if (temp.equals("false")) itsYAutoZero.setSelected(false);
        } else if (temp.equals("fixed")) {
	  itsYSpecifyScale.doClick();
	  temp = (String)setup.get("Yscalemin");
	  itsYScaleMin.setText(temp);
	  temp = (String)setup.get("Yscalemax");
	  itsYScaleMax.setText(temp);
	} else return;

        temp = (String)setup.get("style");
        if (temp.equals("lines")) {
	  itsDrawLines.doClick();
	  temp = (String)setup.get("gaps");
	  if (temp.equals("true")) itsDiscontinuousLines.setSelected(true);
	  else if (temp.equals("false")) itsDiscontinuousLines.setSelected(false);
        } else if (temp.equals("dots")) {
            itsDrawDots.doClick();
        } else if (temp.equals("symbols")) {
            itsDrawSymbols.doClick();
        } else return;

        //Parse the number of points
        temp = (String)setup.get("numpairs");
	int numpoints = Integer.parseInt(temp);
	Vector xpoints = new Vector(numpoints);
        Vector ypoints = new Vector(numpoints);
        //Parse the X points
        temp = (String)setup.get("xpoints");
        StringTokenizer st = new StringTokenizer(temp, ":");
	for (int i=0; i<numpoints; i++) {
	  xpoints.add(st.nextToken());
        }
        itsPoints.setXSelections(xpoints);
        //Parse the Y points
	temp = (String)setup.get("ypoints");
	st = new StringTokenizer(temp, ":");
	for (int i=0; i<numpoints; i++) {
	  ypoints.add(st.nextToken());
        }
	itsPoints.setYSelections(ypoints);

	temp = (String)setup.get("post-proc");
	if (temp==null||temp.equals("false")) {
          itsShowAllMatches.doClick();
	} else {
	  itsPostProcess.doClick();
	  temp = (String)setup.get("binsize");
	  itsBinSize.setText(temp);
	  temp = (String)setup.get("proc-mode");
	  if (temp==null || temp.equals("avg"))
	    itsPPCombo.setSelectedItem("Average");
	  else if (temp.equals("rms"))
	    itsPPCombo.setSelectedItem("RMS");
	  else if (temp.equals("cnt"))
	    itsPPCombo.setSelectedItem("Count of");
	  else 
	    itsPPCombo.setSelectedItem("?UNKNOWN?");

	}
      } catch (Exception e) {
	System.err.println("ATXYPlotSetupPanel:showSetup: " + e.getMessage());
      }
    }
  }

  /////////////////////// END NESTED CLASS /////////////////////////////

  /** Abstract base class for */
  public abstract class DataMatcher {
    public abstract XYSeries getMatches(Vector xdata, Vector ydata);

    /** Convert the given data to a <i>Number</i>, if possible.
     * <tt>null</tt> will be returned if no conversion is possible. */
    protected
    Number
    toNumber(Object data)
    {
      if (data==null) return null;
      if (data instanceof Boolean) {
	if (((Boolean)data).booleanValue())
	  return new Integer(1);
	else
	  return new Integer(0);
      }
      if (data instanceof Number) return (Number)data;
      if (data instanceof HourAngle) return new Double(((Angle)data).getValue()*24.0/(2*Math.PI));
      if (data instanceof Angle) return new Double(((Angle)data).getValueDeg());
      if (data instanceof AbsTime) return new Double(((AbsTime)data).getAsSeconds());
      if (data instanceof RelTime) return new Double(((RelTime)data).getAsSeconds());

      return null;
    }

    /** Get an XYDataItem from the two PointDatas. */
    protected XYDataItem theseMatch(PointData x, PointData y) {
      Number xnum = toNumber(x.getData());
      if (xnum==null) return null;
      Number ynum = toNumber(y.getData());
      if (ynum==null) return null;
      return new XYDataItem(xnum, ynum);
    }
  }

  /** DataMatcher that finds the first Y point <i>after or coincident</i>
   * with the time-stamp of each X points. However it will not map two X
   * points to the same Y point, so if the same Y point matches two X's,
   * the first X will be discarded and only the second X will be matched. */
  public class FirstAfter extends DataMatcher {
    public XYSeries getMatches(Vector xdata, Vector ydata)
    {
      XYSeries res = new XYSeries("temp");
      if (xdata==null || ydata==null) return res;
      int lasty = 0;
      for (int x=0; x<xdata.size(); x++) {
	PointData thisx = (PointData)xdata.get(x);
	AbsTime xtime = thisx.getTimestamp();
	for (int y=lasty; y<ydata.size(); y++) {
          PointData thisy = (PointData)ydata.get(y);
          AbsTime ytime = thisy.getTimestamp();
	  if (ytime.isAfterOrEquals(xtime)) {
	    //We've found the first Y that comes after this X, but we don't
	    //want to use this match if there is a later X that also
	    //matches this same Y, because that combination would be better
	    if (x+1==xdata.size() ||
		ytime.isBefore(((PointData)xdata.get(x+1)).getTimestamp())) {
	      XYDataItem n = theseMatch(thisx, thisy);
	      if (n!=null) res.add(n);
	    }
            lasty=y;
            break;
	  }
	}
      }
      return res;
    }
  }

  /** DataMatcher that will find the Y point that was sampled nearest to
   * the time that each X point was sampled. However each X and Y point can
   * be used at most once, so if the same Y point is the nearest Y point to
   * two or more X points, only the X point that is closest to the epoch
   * of the y point will actually be matched. */
  public class NearestInTime extends DataMatcher {
    public XYSeries getMatches(Vector xdata, Vector ydata)
    {
      XYSeries res = new XYSeries("temp");
      if (xdata==null || ydata==null) return res;
      int lasty = 0;
      for (int x=0; x<xdata.size(); x++) {
	PointData thisx = (PointData)xdata.get(x);
	AbsTime xtime = thisx.getTimestamp();
	PointData besty = null;
        long bestdiff = 0;
	for (int y=lasty; y<ydata.size(); y++) {
          PointData thisy = (PointData)ydata.get(y);
	  AbsTime ytime = thisy.getTimestamp();
          //Get (absolute-valued) offset to this Y point
	  long thisdiff = xtime.getValue() - ytime.getValue();
          if (thisdiff<0) thisdiff = -thisdiff;
          //Check if this point is better than the last one
	  if (besty==null || thisdiff<bestdiff) {
	    besty = thisy;
	    bestdiff = thisdiff;
	  }
	  if (besty!=thisy || y==ydata.size()-1) {
	    //We've found the closest Y yo this X, but need to check if
	    //this X the closest to that Y.
	    if (x+1==xdata.size()) {
	      XYDataItem n = theseMatch(thisx, besty);
	      if (n!=null) res.add(n);
	    } else {
	      thisdiff = ((PointData)xdata.get(x+1)).getTimestamp().getValue()
	                 - besty.getTimestamp().getValue();
	      if (thisdiff<0) thisdiff = -thisdiff;
	      if (thisdiff>bestdiff) {
		//The original X point is better, so let's use it
		XYDataItem n = theseMatch(thisx, besty);
		if (n!=null) res.add(n);
		lasty=y+1;
	      }
	    }
            break;
	  }
	}
      }
      return res;
    }
  }


  /** DataMatcher that will match all Y points to the nearest X point.
   * This differs from most of the other DataMatchers in that the others
   * return only the best match, not all matches. */
  public class AllNearest extends DataMatcher {
    public XYSeries getMatches(Vector xdata, Vector ydata)
    {
      //For each Y point, we need to find the X point that is nearest
      XYSeries res = new XYSeries("temp");
      if (xdata==null || ydata==null) return res;
      int lastx = 0;
      for (int y=0; y<ydata.size(); y++) {
	PointData thisy = (PointData)ydata.get(y);
	AbsTime ytime = thisy.getTimestamp();
        long bestdiff = -1;
	for (int x=lastx; x<xdata.size(); x++) {
          PointData thisx = (PointData)xdata.get(x);
	  AbsTime xtime = thisx.getTimestamp();
	  long tdiff = xtime.getValue() - ytime.getValue();
	  if (tdiff<0) tdiff = -tdiff;
	  if (bestdiff==-1 || tdiff<bestdiff) {
            bestdiff = tdiff;
	  } else {
	    //Let's use the previous X point
	    XYDataItem n = theseMatch((PointData)xdata.get(x-1), thisy);
	    if (n!=null) res.add(n);
	    lastx = x-1;
            break;
	  }
	}
      }
      return res;
    }
  }


  /** DataMatcher that finds the Y points that bound each X point
   * and then interpolates to find the Y value at the instant that
   * each X point was sampled. */
  public class Interpolated extends DataMatcher {
    public XYSeries getMatches(Vector xdata, Vector ydata)
    {
      XYSeries res = new XYSeries("temp");
      if (xdata==null || ydata==null) return res;
      int lasty = 0;
      for (int x=0; x<xdata.size(); x++) {
	PointData thisx = (PointData)xdata.get(x);
	AbsTime xtime = thisx.getTimestamp();
	int besty = -1;
	long bestdiff = 0;
        //We need to find the nearest Y point to this X point
	for (int y=lasty; y<ydata.size(); y++) {
          PointData thisy = (PointData)ydata.get(y);
	  AbsTime ytime = thisy.getTimestamp();
	  long thisdiff = xtime.getValue() - ytime.getValue();
          if (thisdiff<0) thisdiff = -thisdiff;
	  if (besty==-1 || thisdiff<bestdiff) {
            //This Y point is closer to the X point
	    besty = y;
	    bestdiff = thisdiff;
	  }
	  if (besty!=y || y==ydata.size()-1) {
	    //We've found the nearest Y point to this X point, next
            //we need to get the other Y point that bounds this X point
	    Number yd1 = null;
	    Number yd2 = null;
	    AbsTime yt1 = null;
            AbsTime yt2 = null;
	    if (xtime.getValue() - ((PointData)ydata.get(besty)).getTimestamp().getValue()>0) {
	      //X is after Y, so need to use following Y as well
	      yd1 = toNumber(((PointData)ydata.get(besty)).getData());
	      yt1 = ((PointData)ydata.get(besty)).getTimestamp();
	      if (besty<ydata.size()-1) {
		yd2 = toNumber(((PointData)ydata.get(besty+1)).getData());
		yt2 = ((PointData)ydata.get(besty+1)).getTimestamp();
	      }
	    } else {
	      //X is before Y, so need to use previous Y as well
	      yd2 = toNumber(((PointData)ydata.get(besty)).getData());
	      yt2 = ((PointData)ydata.get(besty)).getTimestamp();
	      if (besty>0) {
		yd1 = toNumber(((PointData)ydata.get(besty-1)).getData());
                yt1 = ((PointData)ydata.get(besty-1)).getTimestamp();
	      }
	    }
	    if (yd1!=null && yd2!=null) {
	      //We've found the two Y points that bound this X point so
	      //we can go ahead and calculate the Y value interpolated
              //to the instant of the X point.
	      double slope = (yd2.doubleValue() - yd1.doubleValue()) /
		(yt2.getValue()/1000000 - yt1.getValue()/1000000);
	      double Yval = yd1.doubleValue() + slope*(xtime.getValue()/1000000-yt1.getValue()/1000000);
	      Number Xval = toNumber(thisx.getData());
	      if (Xval!=null) res.add(new XYDataItem(Xval, new Double(Yval)));
	    }
	    lasty = besty;
            break;
	  }
	}
      }
      return res;
    }
  }


  /** DataMatcher used to match X and Y points. */
  private DataMatcher itsMatcher = new AllNearest();

  /** Static object to use as a semaphore. JFreeChart can do the strangest
   * things when multiple threads use it at once... */
  private static Object theirLock = new Object();

  /** Network connection to the monitor server. */
  private MonitorClientCustom itsServer = null;

  /** Container for the individual datasets. */
  private XYSeriesCollection itsData = new XYSeriesCollection();

  /** Contains Vectors holding the names for the X axis points. */
  private Vector itsXPointNames = new Vector();
  /** Contains Vectors holding AbsTimes for the last data collected for each
   * X axis point. */
  private Vector itsXPointEpochs = new Vector();
  /** Contains Vectors containing ALL the data for each of the X points. */
  private Vector itsXData = new Vector();

  /** Contains Vectors holding the names for the Y axis points. */
  private Vector itsYPointNames = new Vector();
  /** Contains Vectors holding AbsTimes for the last data collected for each
   * Y axis point. */
  private Vector itsYPointEpochs = new Vector();
  /** Contains Vectors containing ALL the data for each of the Y points. */
  private Vector itsYData = new Vector();

  /** Reference to our graph. */
  private JFreeChart itsGraph = null;

  /** Reference to the ChartPanel which contains our graph. */
  private ChartPanel itsChartPanel = null;

  /** Title string for the graph. */
  private String itsTitle = "X vs. Y Plot";
  /** Label for X axis. */
  private String itsXLabel = "X";
  /** Label for Y axis. */
  private String itsYLabel = "Y";

  /** Records if the graph should update in real-time. If the value is
   * <tt>true</tt> the graph will self-update, if the value is <tt>false</tt>
   * the graph will remain static. */
  private boolean itsRealtime = true;

  /** Update interval for dynamic graphs. */
  private RelTime itsUpdateInterval = RelTime.factory(10000000);

  /** Should a legend be shown? */
  private boolean itsShowLegend = true;

  /** The period of time the data on the graph should span. */
  private RelTime itsPeriod = RelTime.factory(12*3600000000l);

  /** Timestamp of the oldest data to display. Any data which is older
   * than this epoch is considered expired and should no longer be
   * displayed in our graph. */
  private AbsTime itsStart = new AbsTime();

  /** Flag to indicate if the graph needs to be reinitialised. This will be
   * set when there has been a change to the data we need to display. */
  private boolean itsReInit = false;

  /** Flag to indicate if the thread should keep running. If this is set
   * to <tt>true</tt> the thread will keep running. If set to <tt>false</tt>
   * the thread will exit. */
  private boolean itsKeepRunning = true;

  /** Indicates if post-processing should be performed after the X and Y
   * points have been matched. */
  private boolean itsPostProcess = false;
  /** X bin size for post-processing. */
  private double itsBinSize = 1.0;
  /** Should average numbers be reported? */
  private boolean itsShowAverages = false;
  /** Should RMS numbers be reported? */
  private boolean itsShowRMS = false;
  /** Should the Y counts for each X bin be displayed? */
  private boolean itsShowCounts = false;

  /** Copy of the setup we are currently using. */
  private SavedSetup itsSetup = null;

  /** Timer that forces updates at the user specified frequency. */
  private Timer itsTimer = null;

  private Image itsImage = null;
  private int itsOldWidth = 0;
  private int itsOldHeight = 0;
  private JLabel itsPleaseWait = new JLabel("Downloading graph data from server "
					    + "- PLEASE WAIT", JLabel.CENTER);

  /** C'tor. */
  public
  ATXYPlot()
  {
    setLayout(new java.awt.BorderLayout());
    findServer();

    setPreferredSize(new Dimension(500,300));
    setMinimumSize(new Dimension(200,200));

    //Start the data collection thread
    new Thread(this).start();
  }


  /** Main loop for data-update thread. */
  public
  void
  run()
  {
    final JPanel realthis = this;

    Runnable notsetup = new Runnable() {
      public void run() {
	add(new JLabel("Configure graph options under the \"X vs Y Plot\" tab",
		       JLabel.CENTER));
	//realthis.repaint();
      }
    };
    try {
      //Need to do the notification using event thread
      SwingUtilities.invokeLater(notsetup);
    } catch (Exception e) {e.printStackTrace();}

    itsTimer = new Timer((int)(itsUpdateInterval.getValue()/1000), this);
    itsTimer.start();

    while (itsKeepRunning) {
      if (itsReInit) loadSetupReal();

      if (itsRealtime) {
	//Recalculate the epoch of the earliest data to be used
	itsStart = (new AbsTime()).add(itsPeriod.negate());

	for (int i=0; i<itsXPointNames.size(); i++) {
	  Vector x = (Vector)itsXData.get(i);
	  //Update the time-stamp for the last collected data
	  if (x.size()>0) {
	    itsXPointEpochs.remove(i);
	    PointData pd = (PointData)x.lastElement();
	    itsXPointEpochs.insertElementAt(pd.getTimestamp(), i);
	  }
	  //Discard any data that is too old to use
	  while (x.size()>0) {
	    PointData pd = (PointData)x.get(0);
	    if (pd.getTimestamp().isAfter(itsStart)) break;
	    x.removeElementAt(0);
	  }
	  //Load any new data that is available
	  Vector v = getSince((String)itsXPointNames.get(i),
			      (AbsTime)itsXPointEpochs.get(i));
	  x.addAll(v);
	}
	//Then also update the Y axis
	for (int i=0; i<itsYPointNames.size(); i++) {
	  Vector y = (Vector)itsYData.get(i);
	  if (y.size()>0) {
	    itsYPointEpochs.remove(i);
	    PointData pd = (PointData)y.lastElement();
	    itsYPointEpochs.insertElementAt(pd.getTimestamp(), i);
	  }
	  while (y.size()>0) {
	    PointData pd = (PointData)y.get(0);
	    if (pd.getTimestamp().isAfter(itsStart)) break;
	    y.removeElementAt(0);
	  }
	  Vector v = getSince((String)itsYPointNames.get(i),
			      (AbsTime)itsYPointEpochs.get(i));
	  y.addAll(v);
	}
      }

      System.err.println("Xpoints="+itsXPointNames.size() +
			 " Ypoints="+itsYPointNames.size() +
			 " Series="+itsData.getSeriesCount());
      for (int i=0; i<itsXPointNames.size(); i++) {
	Vector xdata = (Vector)itsXData.get(i);
	Vector ydata = (Vector)itsYData.get(i);
	XYSeries xy1 = itsMatcher.getMatches(xdata, ydata);
	//If post-processing is required, go do it
	if (itsPostProcess) {
	  if (itsShowAverages) xy1 = getAverages(xy1);
	  else if (itsShowRMS) xy1 = getRMS(xy1);
	  else if (itsShowCounts) xy1 = getCounts(xy1);
	}
	XYSeries xy2 = itsData.getSeries(i);
	xy2.clear();
	System.err.println("ATXYPlot: series " + i + " has " + xy1.getItemCount() + " matches from " + xdata.size() + "/" + ydata.size());
	for (int j=0; j<xy1.getItemCount(); j++) {
	  xy2.add(xy1.getDataItem(j));
	}
      }

      //Let the graph know that the data sets have been updated
      if (itsData.getSeriesCount()>0) {
	Runnable ud = new Runnable() {
	  public void run() {
	    itsData.getSeries(0).setNotify(true);
	    itsData.getSeries(0).setNotify(false);
	  }
	};
	try {
	  //Need to do the notification using event thread
	  SwingUtilities.invokeAndWait(ud);
	} catch (Exception e) {e.printStackTrace();}
      }

      //Update our cached image of the graph
      int w = getSize().width;
      int h = getSize().height;
      if (itsGraph!=null) itsImage = itsGraph.createBufferedImage(w,h);

      //Redraw our display using new image - also done by event thread
      Runnable ud2 = new Runnable() {
	public void run() {
	  realthis.removeAll();
	  realthis.repaint();
	}
      };
      try {
	SwingUtilities.invokeLater(ud2);
      } catch (Exception e) {e.printStackTrace();}

      if (itsReInit) loadSetupReal();

      //Wait here for a while
      synchronized (this) {
	try { wait(); } catch (Exception e) { e.printStackTrace(); }
      }
    }

    itsTimer.stop();
    itsTimer = null;
  }


  /** Draw our cached image of the graph to the screen. Also checks for
   * resize events and resizes the image if required. */
  public
  void
  paintComponent (Graphics g) {
    super.paintComponent(g);

    if (itsImage!=null) {
      int w = getSize().width;
      int h = getSize().height;
      if (itsOldWidth!=w || itsOldHeight!=h) {
	//We've been resized, so need to resize the image of the graph
	itsOldWidth  = w;
	itsOldHeight = h;
	itsImage = itsGraph.createBufferedImage(w,h);
      }
      //Draw our image to the display
      g.drawImage (itsImage, 0, 0, this);
    }
  }


  /** Free all resources so that this MonPanel can disappear. */
  public
  void
  vaporise()
  {
    synchronized (itsXPointNames) {
      itsKeepRunning = false;
      //Awake our thread so it can clean-up and die
      synchronized (this) { this.notifyAll(); }
    }
  }


  /** */
  public
  void
  actionPerformed(ActionEvent e)
  {
    System.err.println("ATXYPlot: Time to update graph \""
		       + itsTitle + "\"");
    synchronized (this) { this.notifyAll(); }
  }


  /** Clear any current setup. */
  public
  void
  blankSetup()
  {
    itsXPointNames.clear();
    itsXPointEpochs.clear();
    itsYPointNames.clear();
    itsYPointEpochs.clear();
    //((TimeSeriesCollection)itsData.get(i)).removeAllSeries();
    //itsData.clear();
    synchronized (this) { this.notifyAll(); }
  }


  /** Configure this MonPanel to use the specified setup. This method can
   * be used to restore saved states, eg what monitor points to graph and
   * over what time range.
   * @param setup class-specific setup information.
   * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if
   *         there was a problem and the setup cannot be used.
   */
  public
  boolean
  loadSetup(SavedSetup setup)
  {
    //check if the setup is suitable for our class
    if (!setup.checkClass(this)) {
      System.err.println("ATXYPlot:loadSetup: setup not for "
			 + this.getClass().getName());
      return false;
    }
    //the copy of the setup held by the frame is now incorrect
    if (itsFrame instanceof MonFrame) ((MonFrame)itsFrame).itsSetup=null;
    itsSetup = setup;
    itsReInit = true;
    synchronized (this) { this.notifyAll(); }
    return true;
  }


  /** Does the real loading of a setup, using the worker thread. */
  public
  boolean
  loadSetupReal()
  {
    try {
      itsReInit = false;
      SavedSetup setup = itsSetup;
      itsImage = null;
      itsTimer.stop();

      //title
      String temp = (String)setup.get("title");
      itsTitle = temp;

      //period
      temp = (String)setup.get("period");
      itsPeriod = RelTime.factory(Long.parseLong(temp));

      //update interval
      temp = (String)setup.get("update");
      if (temp!=null) {
	itsUpdateInterval = RelTime.factory(Long.parseLong(temp));
      } else {
	itsUpdateInterval = RelTime.factory(60000000);
      }

      //graph mode
      temp = (String)setup.get("mode");
      if (temp.equals("dynamic")) {
	itsRealtime = true;
	itsStart = AbsTime.factory((new AbsTime()).getValue()
				   - itsPeriod.getValue());
      } else {
	itsRealtime = false;
	//Read the start epoch for the static graph
	temp = (String)setup.get("start");
	itsStart = AbsTime.factory(Long.parseLong(temp));
      }

      //legend
      temp = (String)setup.get("legend");
      if (temp.equals("true")) {
	itsShowLegend = true;
      } else {
	itsShowLegend = false;
      }

      //Matching mode
      temp = (String)setup.get("matchmode");
      if (temp==null||temp.equals("Nearest-in-time"))
	itsMatcher = new NearestInTime();
      else if (temp.equals("Interpolated value"))
	itsMatcher = new Interpolated();
      else if (temp.equals("All neighbours"))
	itsMatcher = new AllNearest();
      else if (temp.equals("First after"))
	itsMatcher = new Interpolated();
      else {
	System.err.println("ATXYPlot: loadSetup: WARNING: \"" + temp
			   + "\" is not a valid matching mode");
	itsMatcher = new NearestInTime();
      }

      //axis labels
      itsXLabel = (String)setup.get("Xlabel");
      itsYLabel = (String)setup.get("Ylabel");

      itsXPointNames.clear();
      itsXPointEpochs.clear();
      itsYPointNames.clear();
      itsYPointEpochs.clear();
      itsData.removeAllSeries();

      //make user think that something is happening
      Runnable pleasewait = new Runnable() {
	public void run() {
	  removeAll();
	  add(itsPleaseWait);
	  repaint();
	}
      };
      try {
	//Need to do the notification using event thread
	SwingUtilities.invokeLater(pleasewait);
      } catch (Exception e) {e.printStackTrace();}

      final JFreeChart chart = ChartFactory.createXYLineChart(itsTitle,
							      itsXLabel, itsYLabel,
							      itsData,
							      PlotOrientation.VERTICAL,
							      itsShowLegend, false, false);
      XYPlot plot = chart.getXYPlot();

      NumberAxis xaxis = (NumberAxis)plot.getDomainAxis();
      xaxis.setLabel(itsXLabel);
      NumberAxis yaxis = (NumberAxis)plot.getRangeAxis();
      yaxis.setLabel(itsYLabel);

      temp = (String)setup.get("Xscalemode");
      if (temp.equals("auto")) {
	//Autoscale this axis
	xaxis.setAutoRange(true);
	temp = (String)setup.get("Xshowzero");
	if (temp.equals("true")) xaxis.setAutoRangeIncludesZero(true);
	else xaxis.setAutoRangeIncludesZero(false);
      } else if (temp.equals("fixed")) {
	//Use the specified scale
	xaxis.setAutoRange(false);
	temp = (String)setup.get("Xscalemin");
	double min = Double.parseDouble(temp);
	temp = (String)setup.get("Xscalemax");
	double max = Double.parseDouble(temp);
	xaxis.setLowerBound(min);
	xaxis.setUpperBound(max);
      } else {
	System.err.println("ATXYPlot:loadData: Unknown X mode \"" +
			   temp + "\"");
	blankSetup();
	return false;
      }

      temp = (String)setup.get("Yscalemode");
      if (temp.equals("auto")) {
	//Autoscale this axis
	yaxis.setAutoRange(true);
	temp = (String)setup.get("Yshowzero");
	if (temp.equals("true")) yaxis.setAutoRangeIncludesZero(true);
	else yaxis.setAutoRangeIncludesZero(false);
      } else if (temp.equals("fixed")) {
	//Use the specified scale
	yaxis.setAutoRange(false);
	temp = (String)setup.get("Yscalemin");
	double min = Double.parseDouble(temp);
	temp = (String)setup.get("Yscalemax");
	double max = Double.parseDouble(temp);
	yaxis.setLowerBound(min);
	yaxis.setUpperBound(max);
      } else {
	System.err.println("ATXYPlot:loadData: Unknown Y mode \"" +
			   temp + "\"");
	blankSetup();
	return false;
      }

      //Read which renderer to use to draw the data
      XYItemRenderer renderer = null;
      temp = (String)setup.get("style");
      if (temp.equals("lines")) {
	//Render with lines
	//Check if we should enable discontinuous lines
	temp = (String)setup.get("gaps");
	if (temp.equals("true")) {
	  renderer = new StandardXYItemRenderer(StandardXYItemRenderer.DISCONTINUOUS_LINES);
	  ((StandardXYItemRenderer)renderer).setPlotLines(true);
	  ((StandardXYItemRenderer)renderer).setPlotShapes(false);
	  //I don't actually understand what this threashold means!
	  ((StandardXYItemRenderer)renderer).setGapThreshold(30.0);
	} else {
	  renderer = new StandardXYItemRenderer();
	  ((StandardXYItemRenderer)renderer).setPlotLines(true);
	  ((StandardXYItemRenderer)renderer).setPlotShapes(false);
	}
      } else if (temp.equals("dots")) {
	//Render with dots
	renderer = new XYDotRenderer();
      } else if (temp.equals("symbols")) {
	//Render with symbols
	renderer = new StandardXYItemRenderer();
	((StandardXYItemRenderer)renderer).setPlotLines(false);
	((StandardXYItemRenderer)renderer).setPlotShapes(true);
	((StandardXYItemRenderer)renderer).setDefaultShapesFilled(Boolean.FALSE);
      } else {
	System.err.println("ATXYPlot:loadData: Unknown style \"" +
			   temp + "\"");
	blankSetup();
	return false;
      }
      plot.setRenderer(renderer);

      //Read the number of X/Y pairs to be displayed
      temp = (String)setup.get("numpairs");
      int numpoints = Integer.parseInt(temp);
      //Load the X points
      temp = (String)setup.get("xpoints");
      StringTokenizer st = new StringTokenizer(temp, ":");
      for (int j=0; j<numpoints; j++) {
	itsXPointNames.add(st.nextToken());
	itsXPointEpochs.add(AbsTime.factory(itsStart));
      }
      //Load the Y points
      temp = (String)setup.get("ypoints");
      st = new StringTokenizer(temp, ":");
      for (int j=0; j<numpoints; j++) {
	itsYPointNames.add(st.nextToken());
	itsYPointEpochs.add(AbsTime.factory(itsStart));
      }

      //Extract any post-processing options
      temp = (String)setup.get("post-proc");
      if (temp==null || temp.equals("false")) {
	itsPostProcess = false;
	itsShowCounts = false;
	itsShowAverages = false;
      } else {
	itsPostProcess = true;
	temp = (String)setup.get("binsize");
	itsBinSize = Double.parseDouble(temp);
	temp = (String)setup.get("proc-mode");
	if (temp==null || temp.equals("avg")) {
	  itsShowAverages = true;
	  itsShowCounts = false;
	  itsShowRMS = false;
	} else if (temp.equals("rms")) {
	  itsShowAverages = false;
	  itsShowCounts = false;
	  itsShowRMS = true;
	} else {
	  itsShowAverages = false;
	  itsShowCounts = true;
	  itsShowRMS = false;
	}
      }

      //Create new containers for the data
      makeSeries();
      //Load all the specified data
      for (int i=0; i<itsXPointNames.size(); i++) {
	//Get archival data from the archive
	AbsTime endtime = itsStart.add(itsPeriod);
	Vector v = getArchive((String)itsXPointNames.get(i),
			      itsStart, endtime);
	itsXData.add(v);
	v = getArchive((String)itsYPointNames.get(i),
		       itsStart, endtime);
	itsYData.add(v);
      }

      for (int i=0; i<itsXPointNames.size(); i++) {
	Vector xdata = (Vector)itsXData.get(i);
	Vector ydata = (Vector)itsYData.get(i);
	XYSeries xy1 = itsMatcher.getMatches(xdata, ydata);
	//If post-processing is required, go do it
	if (itsPostProcess) {
	  if (itsShowAverages) xy1 = getAverages(xy1);
	  else if (itsShowRMS) xy1 = getRMS(xy1);
	  else if (itsShowCounts) xy1 = getCounts(xy1);
	}
	XYSeries xy2 = itsData.getSeries(i);
	xy2.clear();
	System.err.println("ATXYPlot: series " + i + " has " + xy1.getItemCount() + " matches from " + xdata.size() + "/" + ydata.size());
	for (int j=0; j<xy1.getItemCount(); j++) {
	  xy2.add(xy1.getDataItem(j));
	}
      }


      Runnable displaygraph = new Runnable() {
	public void run() {
	  setGraph(chart);
	  //if (itsFrame instanceof MonFrame) itsFrame.pack();
	  //realthis.repaint();
	}
      };
      try {
	//Need to do the notification using event thread
	SwingUtilities.invokeLater(displaygraph);
      } catch (Exception e) {e.printStackTrace();}

      itsTimer.setRepeats(true);
      itsTimer.setDelay((int)(itsUpdateInterval.getValue()/1000));
      itsTimer.stop();
      itsTimer.start();
      System.err.println("ATXYPlot: loaded new setup OK");
    } catch (Exception e) {
      System.err.println("ATXYPlot:loadSetup: " + e.getClass().getName()
			 + " " + e.getMessage());
      e.printStackTrace();
      blankSetup();
      return false;
    }

    return true;
  }

  /** Get the current class-specific configuration for this MonPanel.
   * This can be used to capture the current state of the MonPanel so that
   * it can be easily recovered later.
   * @return class-specific configuration information.
   */
  public
  SavedSetup
  getSetup()
  {
    return itsSetup;
  }


  /** Configure this panel to display the new graph. This will also remove
   * the old graph if it exists.
   * @param The graph to display. */
  private
  void
  setGraph(JFreeChart newgraph)
  {
    if (itsChartPanel==null) {
      itsChartPanel = new ChartPanel(newgraph);
      itsChartPanel.setMouseZoomable(true, false);
      itsChartPanel.setPreferredSize(new Dimension(600,250));
      itsChartPanel.setMinimumSize(new Dimension(100,100));
      //add(itsChartPanel);
    } else {
      itsChartPanel.setChart(newgraph);
    }
    itsGraph = newgraph;
    validate();
    repaint();
  }


  /** Determine which monitor data server to use and try to establish
   * a connection to it. */
  private
  void
  findServer()
  {
    itsServer = MonClientUtil.getServer();
  }


  /** Request archival data from the monitor data server. */
  protected
  Vector
  getArchive(String pointname, AbsTime t1, AbsTime t2)
  {
    //if (!theirServer.isConnected()) return null;

    //Otherwise, we're connected and ready to get the data
    return itsServer.getPointData(pointname, t1, t2);
  }


  /** Request all data since the specified time from the monitor data
   * server. */
  protected
  Vector
  getSince(String pointname, AbsTime t1)
  {
    //Request the data from the server
    Vector v = itsServer.getPointDataSince(pointname, t1);
    //Remove first element - we already have that
    if (v!=null && v.size()>0) v.remove(v.firstElement());
    return v;
  }


  /** Build container for data and invent sensible names for data series. */
  private
  void
  makeSeries()
  {
    synchronized (theirLock) {
      itsData.removeAllSeries();
      itsXData.clear();
      itsYData.clear();
      for (int i=0; i<itsXPointNames.size(); i++) {
        //Get a short name for the X axis point
	String xlabel = (String)itsXPointNames.get(i);
	int firstdot = xlabel.indexOf(".");
	int lastdot = xlabel.lastIndexOf(".");
	if (firstdot!=-1 && lastdot!=-1 && firstdot!=lastdot) {
	  xlabel = xlabel.substring(0,firstdot) + " "
	    + xlabel.substring(lastdot+1);
	}
        //And then a short name for the Y axis point
	String ylabel = (String)itsYPointNames.get(i);
	firstdot = ylabel.indexOf(".");
	lastdot = ylabel.lastIndexOf(".");
	if (firstdot!=-1 && lastdot!=-1 && firstdot!=lastdot) {
	  ylabel = ylabel.substring(0,firstdot) + " "
	    + ylabel.substring(lastdot+1);
	}
        //Create the new series
	XYSeries xy = new XYSeries(xlabel + " vs " + ylabel, true);

	//Turn notify off so the graph doesn't redraw unnecessarily
	xy.setNotify(false);
	itsData.addSeries(xy);
      }
    }
  }


  /** Get a Panel with all the controls required to configure this provider.
   * @return GUI controls to configure this data provider. */
  public 
  MonPanelSetupPanel
  getControls()
  {
    return new ATXYPlotSetupPanel(this, itsFrame);
  }


  /** Dump current data to the given output stream. This is the mechanism
   * through which data can be exported to a file.
   * @param p The print stream to write the data to. */
  public
  void
  export(PrintStream p)
  {
    final String rcsid = "$Id: ATXYPlot.java,v 1.7 2006/05/26 01:32:13 bro764 Exp $";
    p.println("#Dump from ATXYPlot " + rcsid);
    p.println("#Data dumped at "
	      + (new AbsTime().toString(AbsTime.Format.UTC_STRING)) + " UTC");
    p.println("#Each data record has two, comma separated columns:");
    p.println("#1) X-value");
    p.println("#2) Y-value");
    p.println();

    for (int a=0; a<itsData.getSeriesCount(); a++) {
      XYSeries series = (XYSeries)itsData.getSeries(a);
      if (series.getItemCount()==0) continue;
      //Print the header information for this series
      p.println("#" + series.getItemCount() + " data points for \""
		+ series.getName() + "\" follow.");
      p.println("#X monitor point: " + itsXPointNames.get(a));
      p.println("#Y monitor point: " + itsYPointNames.get(a));
      for (int i=0; i<series.getItemCount(); i++) {
	p.print(series.getXValue(i) + ", ");
	p.println(series.getYValue(i));
      }
      p.println();
    }

    p.println();
    p.println();
  }


  /** Bin the XY points according to <i>itsBinSize</i> and then return a
   * new XYSeries that contains the average Y value for each X bin. */
  protected
  XYSeries
  getAverages(XYSeries xypoints)
  {
    //Have to find the maximum and minimum X values
    int xylen = xypoints.getItemCount();
    double xmin=0.0;
    double xmax=0.0;
    for (int i=0; i<xylen; i++) {
      double thisx = xypoints.getDataItem(i).getX().doubleValue();
      if (thisx<xmin || i==0) xmin = thisx;
      if (thisx>xmax || i==0) xmax = thisx;
    }
    int numbins = (int)Math.ceil((xmax-xmin)/itsBinSize);
    System.err.println("ATXYPlot: got " + numbins + " bins for xmin=" + xmin + " and xmax=" + xmax);

    //Then we need some arrays to hold the sum and counts for each bin
    int[] counts = new int[numbins];
    double[] sums = new double[numbins];
    for (int i=0; i<numbins; i++) {
      counts[i] = 0;
      sums[i] = 0.0;
    }

    //Assign each XY pair to the right bin
    for (int i=0; i<xylen; i++) {
      double thisx = xypoints.getDataItem(i).getX().doubleValue();
      double thisy = xypoints.getDataItem(i).getY().doubleValue();
      int bin = (int)((thisx-xmin)/itsBinSize);
      if (bin==numbins) bin=numbins-1;
      counts[bin]++;
      sums[bin]+=thisy;
    }

    //Get the average Y values and build return structure
    XYSeries res = new XYSeries("");
    for (int i=0; i<numbins; i++) {
      if (counts[i]!=0) {
	double avgy = sums[i]/counts[i];
	double midx = xmin + i*itsBinSize + itsBinSize/2;
        res.add(new XYDataItem(midx, avgy));
      }
    }
    return res;
  }

  /** Bin the XY points according to <i>itsBinSize</i> and then return a
   * new XYSeries that contains the RMS Y value for each X bin. */
  protected
  XYSeries
  getRMS(XYSeries xypoints)
  {
    //Have to find the maximum and minimum X values
    int xylen = xypoints.getItemCount();
    double xmin=0.0;
    double xmax=0.0;
    for (int i=0; i<xylen; i++) {
      double thisx = xypoints.getDataItem(i).getX().doubleValue();
      if (thisx<xmin || i==0) xmin = thisx;
      if (thisx>xmax || i==0) xmax = thisx;
    }
    int numbins = (int)Math.ceil((xmax-xmin)/itsBinSize);
    System.err.println("ATXYPlot: got " + numbins + " bins for xmin=" + xmin + " and xmax=" + xmax);

    //Then we need some arrays to hold the sum and counts for each bin
    int[] counts = new int[numbins];
    double[] sumsquares = new double[numbins];
    for (int i=0; i<numbins; i++) {
      counts[i] = 0;
      sumsquares[i] = 0.0;
    }

    //Assign each XY pair to the right bin
    for (int i=0; i<xylen; i++) {
      double thisx = xypoints.getDataItem(i).getX().doubleValue();
      double thisy = xypoints.getDataItem(i).getY().doubleValue();
      int bin = (int)((thisx-xmin)/itsBinSize);
      if (bin==numbins) bin=numbins-1;
      counts[bin]++;
      sumsquares[bin]+=thisy*thisy;
    }

    //Get the RMS Y values and build return structure
    XYSeries res = new XYSeries("");
    for (int i=0; i<numbins; i++) {
      if (counts[i]!=0) {
	double rms = Math.sqrt(sumsquares[i]/counts[i]);
	double midx = xmin + i*itsBinSize + itsBinSize/2;
        res.add(new XYDataItem(midx, rms));
      }
    }
    return res;
  }

  /** Bin the XY points according to <i>itsBinSize</i> and then return a
   * new XYSeries that contains the number of Y values in each X bin. */
  protected
  XYSeries
  getCounts(XYSeries xypoints)
  {
    //Have to find the maximum and minimum X values
    int xylen = xypoints.getItemCount();
    double xmin=0.0;
    double xmax=0.0;
    for (int i=0; i<xylen; i++) {
      double thisx = xypoints.getDataItem(i).getX().doubleValue();
      if (thisx<xmin || i==0) xmin = thisx;
      if (thisx>xmax || i==0) xmax = thisx;
    }
    int numbins = (int)Math.ceil((xmax-xmin)/itsBinSize);
    System.err.println("ATXYPlot: got " + numbins + " bins for xmin=" + xmin + " and xmax=" + xmax);

    //Then we need some arrays to hold the sum and counts for each bin
    int[] counts = new int[numbins];
    for (int i=0; i<numbins; i++) counts[i] = 0;

    //Assign each XY pair to the right bin
    for (int i=0; i<xylen; i++) {
      double thisx = xypoints.getDataItem(i).getX().doubleValue();
      int bin = (int)((thisx-xmin)/itsBinSize);
      if (bin==numbins) bin=numbins-1;
      counts[bin]++;
    }

    //Build return structure containing counts
    XYSeries res = new XYSeries("");
    for (int i=0; i<numbins; i++) {
      if (counts[i]!=0) {
	double cnt = counts[i];
	double midx = xmin + i*itsBinSize + itsBinSize/2;
        res.add(new XYDataItem(midx, cnt));
      }
    }
    return res;
  }


  public String getLabel() { return null; }

  /** Simple test application. */
  public static void main(String[] argv)
  {
/*    JFrame foo = new JFrame("ATXYPlot Test App");
    foo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    foo.getContentPane().setLayout(new BoxLayout(foo.getContentPane(), BoxLayout.Y_AXIS));

    ATXYPlot ts1 = new ATXYPlot();
    SavedSetup s1 = new SavedSetup("ant.temps", "atnf.atoms.mon.gui.monpanel.ATXYPlot",
        			   "Antenna Pedestal Temperatures:true:86400000000:0:2:ca05.ant.PEDTEM:ca06.ant.PEDTEM");
    SavedSetup s2 = new SavedSetup("seemon.phases", "atnf.atoms.mon.gui.monpanel.ATXYPlot",
				   "Seeing Monitor Phases:true:86400000000:0:1:seemon.site.seemon.Phase:");
    SavedSetup s3 = new SavedSetup("clock.tickphase", "atnf.atoms.mon.gui.monpanel.ATXYPlot",
				   "Clock Tick Phase:true:86400000000:0:1:caclock.site.clock.TickPhase:");
    ts1.loadSetup(s3);
    foo.getContentPane().add(ts1);
    foo.pack();
    foo.setVisible(true);

    JFrame foo2 = new JFrame("Setup Window");
    foo2.getContentPane().add(ts1.getControls());
    foo2.pack();
    foo2.setVisible(true);
    //ts1.getControls();
  */
  }
}

