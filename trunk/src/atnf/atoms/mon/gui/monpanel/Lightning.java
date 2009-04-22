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
import java.util.Vector;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.*;
import java.lang.Number;
import java.text.SimpleDateFormat;
import java.io.PrintStream;


/**
 * GUI display panel for Viasala lightning detector.
 *
 * @author David Brodrick
 * @version $Id: Lightning.java,v 1.7 2008/02/12 03:01:37 bro764 Exp $
 * @see MonPanel
 */
public class
Lightning
extends MonPanel
implements ActionListener, Runnable
{
  static {
    MonPanel.registerMonPanel("Lightning Chart", Lightning.class);
  }

  ///////////////////////// NESTED CLASS ///////////////////////////////
  /** Nested class to provide GUI controls for configuring an ATTimeSeries
   * MonPanel. */
  public class
  LightningSetupPanel
  extends MonPanelSetupPanel
  implements ActionListener
  {
    /** Radio button for showing the latest frame only. */
    private JRadioButton itsShowLatest = new JRadioButton("Show the latest frame, updating every minute");
    /** Radio button for showing a short loop. */
    private JRadioButton itsShowLoop = new JRadioButton("Show a loop spanning the last ");
    /** Text field for setting delay between loop frames. */
    private JTextField itsFrameDelay = new JTextField("200",5);
    /** Text field for specifying the pause duration at the end of the loop. */
    private JTextField itsLoopPause = new JTextField("1500",5);
    /** Text field for overall loop time span, in minutes. */
    private JTextField itsLoopSpan = new JTextField("30",5);
    /** Radio button for archive replay. */
    private JRadioButton itsArchiveLoop = new JRadioButton("Show loop from archive:");
    /** Allows user to enter start time for archival mode. */
    private JTextField itsStart = new JTextField(16);
    /** Allows user to enter end time for archival mode. */
    private JTextField itsEnd = new JTextField(16);
    /** The format of the dates we parse. */
    private SimpleDateFormat itsFormatter
                                 = new SimpleDateFormat ("yyyy/MM/dd HH:mm");
    /** Options for map selection. */
    private String[] itsMaps = {"None"};
    /** Actual file names corresponding to the maps. */
    private String[] itsRealMaps = null;
    /** Combo box for map selection. */
    private JComboBox itsMapSelection = new JComboBox(itsMaps);
    /** Check box for displaying the legend. */
    private JCheckBox itsLegend = new JCheckBox("Show legend");
    /** Check box for displaying the section overlay. */
    private JCheckBox itsOverlay = new JCheckBox("Show map section overlay");
    /** Check box for displaying the total number of strikes and cloud strikes. */
    private JCheckBox itsShowTotals = new JCheckBox("Show total and cloud strike counts");

    public LightningSetupPanel(Lightning panel, JFrame frame) {
      super(panel, frame);

      ///Want to be able to add other background maps here
      itsRealMaps = new String[1];
      itsRealMaps[0] = "none";

      //Associate radio button group for three running modes
      ButtonGroup tempgroup = new ButtonGroup();
      tempgroup.add(itsShowLatest);
      tempgroup.add(itsShowLoop);
      tempgroup.add(itsArchiveLoop);
      //Setup event handlers
      itsShowLatest.addActionListener(this);
      itsShowLatest.setActionCommand("showlatest");
      itsShowLoop.addActionListener(this);
      itsShowLoop.setActionCommand("showloop");
      itsArchiveLoop.addActionListener(this);
      itsArchiveLoop.setActionCommand("archiveloop");
      //Select default settings
      itsShowLatest.doClick();
      itsLegend.doClick();
      itsOverlay.doClick();
      itsShowTotals.doClick();

      JPanel mainpanel = new JPanel();
      mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));

      JPanel temppanel = new JPanel(new GridBagLayout());
      Border blackline = BorderFactory.createLineBorder(Color.black);
      temppanel.setBorder(BorderFactory.createTitledBorder(blackline,
							   "Mode"));
      GridBagConstraints c = new GridBagConstraints();
      c.gridx=0;
      c.gridy=0;
      c.gridwidth=6;
      c.anchor=GridBagConstraints.LINE_START;
      temppanel.add(itsShowLatest, c);

      JPanel temppanel2 = new JPanel();
      temppanel2.add(itsShowLoop);
      temppanel2.add(itsLoopSpan);
      temppanel2.add(new JLabel(" minutes"));
      c.gridx=0;
      c.gridy=1;
      temppanel.add(temppanel2, c);
      c.gridy=2;
      temppanel.add(itsArchiveLoop, c);

      c.gridwidth=4;
      c.gridx=1;
      c.gridy=3;
      temppanel.add(new JLabel("Archive loop start time (yyyy/MM/dd HH:mm) "), c);
      c.gridwidth=1;
      c.gridx=5;
      temppanel.add(itsStart, c);

      c.gridwidth=4;
      c.gridx=1;
      c.gridy=4;
      temppanel.add(new JLabel("Archive loop end time  (yyyy/MM/dd HH:mm) "), c);
      c.gridwidth=1;
      c.gridx=5;
      temppanel.add(itsEnd, c);
      mainpanel.add(temppanel);


      temppanel = new JPanel(new GridBagLayout());
      c.anchor=GridBagConstraints.LINE_START;
      blackline = BorderFactory.createLineBorder(Color.black);
      temppanel.setBorder(BorderFactory.createTitledBorder(blackline,
							   "Loop parameters"));
      c.gridx=0;
      c.gridy=0;
      c.gridwidth=3;
      temppanel.add(new JLabel("Delay between frames in loop (ms) "), c);
      c.gridx=4;
      c.gridwidth=1;
      temppanel.add(itsFrameDelay, c);
      mainpanel.add(temppanel);

      c.gridx=0;
      c.gridy=1;
      c.gridwidth=3;
      temppanel.add(new JLabel("Pause length at end of loop (ms) "), c);
      c.gridx=4;
      c.gridwidth=1;
      temppanel.add(itsLoopPause, c);
      mainpanel.add(temppanel);

      temppanel = new JPanel(new GridBagLayout());
      c.anchor=GridBagConstraints.LINE_START;
      blackline = BorderFactory.createLineBorder(Color.black);
      temppanel.setBorder(BorderFactory.createTitledBorder(blackline,
							   "Chart settings"));
      c.gridx=0;
      c.gridy=0;
      c.gridwidth=1;
      temppanel.add(new JLabel("Background map: "), c);
      c.gridx=1;
      temppanel.add(itsMapSelection, c);
      c.gridx=0;
      c.gridy=1;
      c.gridwidth=2;
      temppanel.add(itsLegend, c);
      c.gridy=2;
      temppanel.add(itsOverlay, c);
      c.gridy=3;
      temppanel.add(itsShowTotals, c);
      mainpanel.add(temppanel);

      add(mainpanel, BorderLayout.NORTH);
      add(new JPanel(), BorderLayout.CENTER);
    }

    public
    void
    actionPerformed(ActionEvent e)
    {
      super.actionPerformed(e);
      String cmd = e.getActionCommand();
      if (cmd.equals("showlatest")) {
        itsFrameDelay.setEnabled(false);
        itsLoopPause.setEnabled(false);
        itsStart.setEnabled(false);
        itsEnd.setEnabled(false);
        itsLoopSpan.setEnabled(false);
      } else if (cmd.equals("showloop")) {
        itsFrameDelay.setEnabled(true);
        itsLoopPause.setEnabled(true);
        itsStart.setEnabled(false);
        itsEnd.setEnabled(false);
        itsLoopSpan.setEnabled(true);
      } else if (cmd.equals("archiveloop")) {
        itsFrameDelay.setEnabled(true);
        itsLoopPause.setEnabled(true);
        itsStart.setEnabled(true);
        itsEnd.setEnabled(true);
        itsLoopSpan.setEnabled(false);
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
      SavedSetup setup = new SavedSetup("temp",
                             "atnf.atoms.mon.gui.monpanel.Lightning");

      //Insert the selected map image
      setup.put("map", itsRealMaps[itsMapSelection.getSelectedIndex()]);

      if (itsShowLatest.isSelected()) {
        //Not looping, just showing the latest frame
        setup.put("mode", "latest");
      } else {
        if (itsShowLoop.isSelected()) {
          setup.put("mode", "realtimeloop");
          int loopspan=60;
          try {
            loopspan = Integer.parseInt(itsLoopSpan.getText());
            if (loopspan<5 || loopspan>3600) {
              throw new Exception();
            }
          } catch (Exception e) {
             JOptionPane.showMessageDialog(this,
                "The entry for Loop Span couldn't\n"+
                "be parsed, I expect an integer number\n"+
                "between 5 and 3600 minutes.",
                "Bad Loop Span Setting",
                JOptionPane.WARNING_MESSAGE);
             return null;
          }
          setup.put("loopspan", itsLoopSpan.getText());
        } else {
          setup.put("mode", "archiveloop");
          //Parse the archive loop start time
          String startstr = itsStart.getText();
          Date date = null;
          try {
            date = itsFormatter.parse(startstr);
          } catch (Exception e) { date=null; }
          if (date==null) {
            JOptionPane.showMessageDialog(this,
              "The Archive Loop Start Time you entered\n" +
              "could not be parsed. The time must\n" +
              "be in \"yyyy/MM/dd HH:mm\" format, eg:\n" +
              "\"" + itsFormatter.format(new Date()) + "\"\n",
              "Bad Start Time",
              JOptionPane.WARNING_MESSAGE);
            return null;
          } 
          AbsTime start = AbsTime.factory(date);
          setup.put("loopstart", ""+start.getValue());
          //Next parse the archive loop end time
          startstr = itsEnd.getText();
          date = null;
          try {
            date = itsFormatter.parse(startstr);
          } catch (Exception e) { date=null; }
          if (date==null) {
            JOptionPane.showMessageDialog(this,
              "The Archive Loop End Time you entered\n" +
              "could not be parsed. The time must\n" +
              "be in \"yyyy/MM/dd HH:mm\" format, eg:\n" +
              "\"" + itsFormatter.format(new Date()) + "\"\n",
              "Bad Start Time",
              JOptionPane.WARNING_MESSAGE);
            return null;
          }
          start = AbsTime.factory(date);
          setup.put("loopend", ""+start.getValue());
        }
        //One of the loop mode, store loop settings
        int framedelay=200;
        try {
          framedelay = Integer.parseInt(itsFrameDelay.getText());
          if (framedelay<0 || framedelay>10000) {
            throw new Exception();
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this,
            "The entry for Frame Delay couldn't\n"+
            "be parsed, I expect an integer number\n"+
            "between 0 and 10000 milliseconds.",
            "Bad Frame Delay Setting",
            JOptionPane.WARNING_MESSAGE);
          return null;
        }
        setup.put("framedelay", ""+framedelay);

        int looppause=1500;
        try {
          looppause  = Integer.parseInt(itsLoopPause.getText());
          if (looppause<0 || looppause>10000) {
            throw new Exception();
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this,
            "The entry for Loop Pause couldn't\n"+
            "be parsed, I expect an integer number\n"+
            "between 0 and 10000 milliseconds.",
            "Bad Loop Pause Setting",
            JOptionPane.WARNING_MESSAGE);
          return null;
        }
        setup.put("looppause", ""+looppause);
      }

      //Should we show an overlay of the different map segments?
      if (itsOverlay.isSelected()) {
        setup.put("overlay", "true");
      } else {
        setup.put("overlay", "false");
      }

      //Should we show the legend?
      if (itsLegend.isSelected()) {
        setup.put("legend", "true");
      } else {
        setup.put("legend", "false");
      }

      //Should we show the total number and number of cloud strikes?
      if (itsShowTotals.isSelected()) {
        setup.put("totals", "true");
      } else {
        setup.put("totals", "false");
      }

      return setup;
    }


    /** Make the controls show information about the given setup. */
    public
    void
    showSetup(SavedSetup setup)
    {
      try {
        String temp = (String)setup.get("mode");
        if (temp==null || temp.equals("latest")) {
          itsShowLatest.doClick();
        } else {
          if (temp.equals("realtimeloop")) {
            itsShowLoop.doClick();
            itsLoopSpan.setText((String)setup.get("loopspan"));
          } else {
            itsArchiveLoop.doClick();
            temp = (String)setup.get("loopstart");
            long start = Long.parseLong(temp);
            AbsTime atemp = AbsTime.factory(start);
            Date dtemp = atemp.getAsDate();
            itsStart.setText(itsFormatter.format(dtemp));
            temp = (String)setup.get("loopend");
            long end = Long.parseLong(temp);
            atemp = AbsTime.factory(end);
            dtemp = atemp.getAsDate();
            itsEnd.setText(itsFormatter.format(dtemp));
          }
          itsFrameDelay.setText((String)setup.get("framedelay"));
          itsLoopPause.setText((String)setup.get("looppause"));
        }
        temp = (String)setup.get("overlay");
        if (temp==null || temp.equals("true")) {
          itsOverlay.setSelected(true);
        } else {
          itsOverlay.setSelected(false);
        }

        temp = (String)setup.get("legend");
        if (temp==null || temp.equals("true")) {
          itsLegend.setSelected(true);
        } else {
          itsLegend.setSelected(false);
        }

        temp = (String)setup.get("totals");
        if (temp==null || temp.equals("true")) {
          itsShowTotals.setSelected(true);
        } else {
          itsShowTotals.setSelected(false);
        }

        temp = (String)setup.get("map");
        if (temp==null) {
          itsMapSelection.setSelectedIndex(0);
        } else {
          for (int i=0; i<itsRealMaps.length; i++) {
            if (itsRealMaps[i].equals(temp)) {
              itsMapSelection.setSelectedIndex(i);
              break;
            }
          }
        }
      } catch (Exception e) {
        System.err.println("LightningSetupPanel:showSetup: " + e.getMessage());
      }
    }
  }

  /////////////////////// END NESTED CLASS /////////////////////////////

  /** Reference to the setup we're currently displaying. */
  private SavedSetup itsSetup = null;
  /** Indicates if our thread should continue to run. */
  private boolean itsKeepRunning = true;
  /** Network connection to the monitor server. */
  private MonitorClientCustom itsServer = null;
  /** The actual array of strike data to be displayed. Each frame has
   * 17 data points (overhead + 8 inner octants + 8 outer octants). */
  private int[][] itsStrikeData = new int[0][];
  /** Contains timestamps for each frame of the loop. */
  private AbsTime[] itsStrikeTimes = null;
  /** Time we last tried to collect data from the server. */
  private AbsTime itsLastTried = null;
  /** The current phase of the loop (if we're displaying a loop). */
  private int itsLoopPhase = 0;
  /** Raw background map image at its native scale. */
  private Image itsRawBackground = null;
  /** Background map image scaled to the screen size. */
  private Image itsBackgroundMap = null;
  /** Current width of the scaled background image. */
  private int itsOldWidth = 0;
  /** Current height of the scaled background image. */
  private int itsOldHeight = 0;

  /** Running mode where we only show the latest frame of data. */
  private boolean itsShowLatest = true;
  /** Running mode for displaying a real-time loop. */
  private boolean itsShowLoop = false;

  /** Time span for the real-time loop. */
  private RelTime itsLoopSpan = null;
  /** Start epoch for archival loop. */
  private AbsTime itsLoopStart = null;
  /** End epoch for archival loop. */
  private AbsTime itsLoopEnd = null;
  /** Inter-frame loop delay. */
  private int itsFrameDelay = 200;
  /** Delay at end of loop. */
  private int itsLoopPause = 1500;
  /** Should we display the section overlay? */
  private boolean itsOverlay = true;
  /** Should we display the legend? */
  private boolean itsLegend = true;
  /** Should we show the total number, and number of cloud strikes. */
  private boolean itsShowTotals = true;
  /** Have we just been reinitialised with a new setup? */
  private boolean itsJustInitialised = false;

  /** Vector containing the names of the monitor points we need to use
   * to get the lightning strike data. First is overhead, then are the 8
   * outer octants clockwise from North and then the 8 inner octants
   * clockwise from North. The final two points are the total number of
   * dtected strikes and then the number of cloud strikes. */
  private Vector itsPointNames = new Vector(20);

  /** C'tor. */
  public
  Lightning()
  {
    setLayout(new java.awt.BorderLayout());
    findServer();

    setPreferredSize(new Dimension(550,550));
    setMinimumSize(new Dimension(200,200));

    //Should avoid hard-coding these
    itsPointNames.add("site.environment.lightning.overhead");
    itsPointNames.add("site.environment.lightning.far_N");
    itsPointNames.add("site.environment.lightning.far_NE");
    itsPointNames.add("site.environment.lightning.far_E");
    itsPointNames.add("site.environment.lightning.far_SE");
    itsPointNames.add("site.environment.lightning.far_S");
    itsPointNames.add("site.environment.lightning.far_SW");
    itsPointNames.add("site.environment.lightning.far_W");
    itsPointNames.add("site.environment.lightning.far_NW");
    itsPointNames.add("site.environment.lightning.near_N");
    itsPointNames.add("site.environment.lightning.near_NE");
    itsPointNames.add("site.environment.lightning.near_E");
    itsPointNames.add("site.environment.lightning.near_SE");
    itsPointNames.add("site.environment.lightning.near_S");
    itsPointNames.add("site.environment.lightning.near_SW");
    itsPointNames.add("site.environment.lightning.near_W");
    itsPointNames.add("site.environment.lightning.near_NW");
    itsPointNames.add("site.environment.lightning.total");
    itsPointNames.add("site.environment.lightning.cloud");
    itsPointNames.add("site.environment.lightning.uptime");

    //Start the data collection thread
    new Thread(this).start();
  }


  /** Main loop for data-update thread. */
  public
  void
  run()
  {
    Timer timer = new Timer(61000, this);
    final Lightning realthis = this;

    while (itsKeepRunning) {
      synchronized (this) {
        if (itsShowLatest) {
          if (itsJustInitialised) {
            itsBackgroundMap = null;
            itsJustInitialised = false;
          }
          itsLoopPhase = 0;

          itsStrikeData  = new int[1][];
          itsStrikeTimes = new AbsTime[1];
          int[] newdata = new int[20];
          AbsTime newtime = getLatest(newdata);
          itsStrikeTimes[0] = newtime;
          itsStrikeData[0] = newdata;

          timer.stop();
          if (newtime!=null) {
            //Schedule for just after next expected update
            long currentage = (new AbsTime()).getValue() - newtime.getValue();
            long timetogo = 61000000 - currentage;
            timer = new Timer((int)(timetogo/1000), this);
            System.err.println("Lightning: " + timetogo/1000000 + " seconds until next update");
          } else {
            //We don't know when the next update will happen, keep trying
            timer = new Timer(30000, this);
          }
          //timer.stop();
          timer.setRepeats(true);
          timer.start();
        } else {
          if (itsJustInitialised) {
            itsBackgroundMap = null;
            itsJustInitialised = false;
            if (itsShowLoop) {
              //Real-time loop, so load up the recent data
              itsLoopEnd   = new AbsTime();
              itsLoopStart = AbsTime.factory(itsLoopEnd.getValue()-itsLoopSpan.getValue());
            }
            getArchive(itsLoopStart, itsLoopEnd);
            itsLoopPhase = itsStrikeData.length-1;
            if (timer!=null) {
              timer.stop();
            }
            timer = new Timer(itsFrameDelay, this);
            timer.setRepeats(true);
            timer.start();
          }
          itsLoopPhase--;
          if (itsLoopPhase==0) {
            if (timer!=null) {
              timer.stop();
            }
            timer = new Timer(itsLoopPause, this);
            timer.setRepeats(false);
            timer.start();
          } else {
            if (itsLoopPhase<0) {
              if (itsShowLoop) {
                //We're running in real-time loop mode, since a loop has
                //just completed, now is a good time to discard any old
                //data and collect updated data if required
                AbsTime now = new AbsTime();
                while (itsStrikeData.length>0 &&
                       now.getValue()-itsStrikeTimes[itsStrikeData.length-1].getValue()>itsLoopSpan.getValue()) 
                {
                  //System.err.println("Purging oldest lightning data");
                  int[][] newdata = new int[itsStrikeData.length-1][];
                  AbsTime[] newtimes = new AbsTime[itsStrikeData.length-1];
                  for (int i=0; i<itsStrikeData.length-1; i++) {
                    newdata[i] = itsStrikeData[i];
                    newtimes[i] = itsStrikeTimes[i];
                  }
                  itsStrikeData = newdata;
                  itsStrikeTimes = newtimes;
                }

                if ((itsLastTried==null || now.getValue()-itsLastTried.getValue()>6000000) &&
                    (itsStrikeTimes.length==0 || now.getValue()-itsStrikeTimes[0].getValue()>62000000)) 
                {
                  //System.err.println("Requesting latest data from server");
                  int[] newdata = new int[20];
                  AbsTime newtime = getLatest(newdata);
                  if (newtime!=null) {
                    int[][] replacedata = new int[itsStrikeData.length+1][];
                    AbsTime[] replacetimes = new AbsTime[itsStrikeData.length+1];
                    for (int i=0; i<itsStrikeData.length; i++) {
                      replacedata[i+1] = itsStrikeData[i];
                      replacetimes[i+1] = itsStrikeTimes[i];
                    }
                    replacedata[0]  = newdata;
                    replacetimes[0] = newtime;
                    itsStrikeData = replacedata;
                    itsStrikeTimes = replacetimes;
                  }
                }
              }

              itsLoopPhase = itsStrikeData.length-1;
              if (timer!=null) {
                timer.stop();
              }
              timer = new Timer(itsFrameDelay, this);
              timer.setRepeats(true);
              timer.start();
            }
          }
        }
      }

      //Redraw our display using new image - also done by event thread
      Runnable ud2 = new Runnable() {
        public void run() {
          //realthis.removeAll();
          realthis.repaint();
        }
      };
      try {
        SwingUtilities.invokeLater(ud2);
      } catch (Exception e) {e.printStackTrace();}


      //Wait here for a while
      synchronized (this) {
        try { wait(); } catch (Exception e) { e.printStackTrace(); }
      }
    }

    if (timer!=null) {
      timer.stop();
    }
    timer = null;
  }


  /** Draw our cached image of the graph to the screen. Also checks for
   * resize events and resizes the image if required. */
  public
  void
  paintComponent(Graphics g) {
    super.paintComponent(g);

    synchronized (this) {
      int w = getSize().width;
      int h = getSize().height;
      int minsize = (w>h)?h:w;

      if (itsRawBackground!=null) {
        if (w!=itsOldWidth || h!=itsOldHeight || itsBackgroundMap==null) {
          //We've been resized
          itsOldWidth = w;
          itsOldHeight = h;
          itsBackgroundMap = itsRawBackground.getScaledInstance(minsize, minsize, 0);
        }
        //Draw our background image to the display
        g.drawImage(itsBackgroundMap, 0, 0, this);
      }

      //Render the actual strike data
      if (itsStrikeData!=null && itsStrikeData.length>itsLoopPhase &&
          itsStrikeTimes!=null && itsStrikeTimes.length>itsLoopPhase &&
          itsLoopPhase>=0 && itsStrikeData.length==itsStrikeTimes.length &&
          itsStrikeData[itsLoopPhase]!=null && itsStrikeTimes[itsLoopPhase]!=null) 
      {
        drawOverhead(itsStrikeData[itsLoopPhase][0], minsize, g);
        for (int i=0; i<8; i++) {
          drawOuterOctant(i, itsStrikeData[itsLoopPhase][i+1], minsize, g);
          drawInnerOctant(i, itsStrikeData[itsLoopPhase][i+9], minsize, g);
        }

        g.setColor(Color.black);
        if (itsShowTotals) {
          g.fillRect(0, 0, 107, 48);
          g.setColor(Color.white);
          g.drawString("" + itsStrikeData[itsLoopPhase][17] + " strikes total", 4, 29);
          g.drawString("" + itsStrikeData[itsLoopPhase][18] + " cloud strikes", 4, 45);
        } else {
          g.fillRect(0, 0, 107, 16);
          g.setColor(Color.white);
        }
        String tstring = itsStrikeTimes[itsLoopPhase].toString(AbsTime.Format.UTC_STRING);
        tstring = tstring.substring(5,tstring.lastIndexOf(":")) + " UT";
        tstring = tstring.replace('-', '/');
        g.drawString(tstring, 4, 13);
        if (itsOverlay) {
          drawSections(minsize, g);
        }
        if (itsLegend) {
          drawLegend(minsize, g);
        }
        g.setColor(Color.red);
        //Check the uptime
        if (itsStrikeData[itsLoopPhase][19]<15) {
          g.drawString("DETECTOR WAS POWER CYCLED", 10, minsize-40);
          g.drawString("DATA ACCUMULATION INCOMPLETE", 10, minsize-20);
          g.drawString("DETECTOR WAS POWER CYCLED", minsize-240, 20);
          g.drawString("DATA ACCUMULATION INCOMPLETE", minsize-240, 40);
        }
        g.setColor(Color.white);
      } else {
        if (itsOverlay) {
          drawSections(minsize, g);
        }
        if (itsLegend) {
          drawLegend(minsize, g);
        }
        g.fillRect(0, 0, 75, 16);
        g.setColor(Color.white);
        g.drawString("NO DATA!!!", 4, 13);
      }
    }
  }

  /** Get an appropriate colour for the specified strike intensity. The
   * colours are graded from yellow (least intense) through orange to red
   * (most intense). */
  private Color getColor(int numstrikes) {
    if (numstrikes==0) {
      return Color.black;
    }
    float red = 1.0f;
    float blue = 0.0f;
    if (numstrikes>10) {
      numstrikes=10;
    }
    float green = (float)(1.0 - (numstrikes/10.0));
    return new Color(red, green, blue);
  }


  /** Get the angle to leave between adjacent radial lines for the specified
   * strike intensity. More instense lightning is shown using radial lines
   * that are spaced more closely. */
  private double getAngle(int numstrikes) {
    if (numstrikes<3) {
      return (Math.PI/180.0)*8.0;
    } else if (numstrikes<8) {
      return (Math.PI/180.0)*4.0;
    } else {
      return (Math.PI/180.0)*2.0;
    }
  }


  /** Draw a legend to the screen. */
  private void
  drawLegend(int size, Graphics g)
  {
    final int xsize = 100;
    g.setColor(Color.black);
    g.fillRect(size-xsize, size-46, xsize, 46);
    g.setColor(Color.white);
    g.drawString("Ground", size-xsize+xsize/4, size-34);
    g.drawString("Strikes", size-xsize+xsize/4+3, size-21);
    g.drawString("1",   size-xsize+5, size-18);
    g.drawString("10+", size-29, size-18);

    g.setColor(getColor(1));
    g.drawLine(size-xsize+5,  size-1, size-xsize+5, size-15);
    g.drawLine(size-xsize+20, size-1, size-xsize+20, size-15);
    g.setColor(getColor(3));
    g.drawLine(size-xsize+33, size-1, size-xsize+33, size-15);
    g.drawLine(size-xsize+45, size-1, size-xsize+45, size-15);
    g.setColor(getColor(5));
    g.drawLine(size-xsize+56, size-1, size-xsize+56, size-15);
    g.drawLine(size-xsize+66, size-1, size-xsize+66, size-15);
    g.setColor(getColor(7));
    g.drawLine(size-xsize+74, size-1, size-xsize+74, size-15);
    g.drawLine(size-xsize+80, size-1, size-xsize+80, size-15);
    g.setColor(getColor(10));
    g.drawLine(size-xsize+85, size-1, size-xsize+85, size-15);
    g.drawLine(size-xsize+90, size-1, size-xsize+90, size-15);
    g.drawLine(size-xsize+95, size-1, size-xsize+95, size-15);
    g.setColor(Color.black);
  }


  /** Draw lines to indicate the different map sections. */
  private void
  drawSections(int size, Graphics g)
  {
    g.setColor(Color.black);
    int radius9km  = (int)(9.0*size/112.0);
    int radius20km = (int)(20.0*size/112.0);
    int radius56km = (int)(56.0*size/112.0);
    //Draw the concentric circles
    g.drawOval(size/2-radius9km, size/2-radius9km, 2*radius9km, 2*radius9km);
    g.drawOval(size/2-radius20km, size/2-radius20km, 2*radius20km, 2*radius20km);
    g.drawOval(size/2-radius56km, size/2-radius56km, 2*radius56km, 2*radius56km);
    //Draw the radial lines
    for (double a=Math.PI*22.5/180.0; a<Math.PI*360.0/180.0; a+=Math.PI*45.0/180.0) {
      int startx = size/2 + (int)(radius9km*Math.cos(a));
      int starty = size/2 + (int)(radius9km*Math.sin(a));
      int endx   = size/2 + (int)(radius56km*Math.cos(a));
      int endy   = size/2 + (int)(radius56km*Math.sin(a));
      g.drawLine(startx, starty, endx, endy);
    }
    //Draw the distance markers
    double c=Math.cos(Math.PI*-67.5/180.0);
    double s=Math.sin(Math.PI*-67.5/180.0);
    g.fillRect((int)(size/2+radius9km*c)-8, (int)(size/2+radius9km*s)-6,31,15);
    g.fillRect((int)(size/2+radius20km*c)-8, (int)(size/2+radius20km*s)-6,38,15);
    g.fillRect((int)(size/2+radius56km*c)-8, (int)(size/2+radius56km*s)-6,38,15);
    g.setColor(Color.white);
    g.drawString("9km", (int)(size/2+radius9km*c)-6, (int)(size/2+radius9km*s)+6);
    g.drawString("20km", (int)(size/2+radius20km*c)-6, (int)(size/2+radius20km*s)+6);
    g.drawString("56km", (int)(size/2+radius56km*c)-6, (int)(size/2+radius56km*s)+6);
  }


  /** Draw strike information for the overhead zone (within 9km). */
  private void
  drawOverhead(int numstrikes, int size, Graphics g)
  {
    //don't draw anything if there are no strikes in this area
    if (numstrikes==0) {
      return;
    }

    int radius9km = (int)(9.0*size/112.0);
    g.setColor(getColor(numstrikes));
    //Draw the appropriate number of radial lines
    double anglestep = getAngle(numstrikes);
    for (double thisangle=0; thisangle<2*Math.PI; thisangle+=anglestep) {
      int x = (int)(size/2.0 + radius9km*Math.sin(thisangle));
      int y = (int)(size/2.0 - radius9km*Math.cos(thisangle));
      g.drawLine(size/2, size/2, x, y);
    }
    //Draw a filled rectangle as background for the number of strikes text
    int textwidth = numstrikes<10?17:25;
    g.fillRect(size/2-9, size/2-9, textwidth, 20);
    //Reset the colour and draw the number of strikes in black
    g.setColor(Color.black);
    g.drawString(""+numstrikes, size/2-5, size/2+5);
  }


  /** Draw strike data for the nominated "distant" range octant. */
  private void
  drawOuterOctant(int octant, int numstrikes, int size, Graphics g)
  {
    //don't draw anything if there are no strikes in this area
    if (numstrikes==0) {
      return;
    }

    g.setColor(getColor(numstrikes));

    double angle1 = Math.PI*(octant*45.0 - 22.5)/180.0;
    double angle2 = Math.PI*(octant*45.0 + 22.5)/180.0;
    int radius20km = (int)(20.0*size/112.0);
    int radius56km = (int)(56.0*size/112.0);
    //Draw the appropriate number of radial lines for this octant
    double anglestep = getAngle(numstrikes);
    for (double thisangle=angle1; thisangle<=angle2; thisangle+=anglestep) {
      int startx1 = (int)(size/2.0 + radius20km*Math.sin(thisangle));
      int starty1 = (int)(size/2.0 - radius20km*Math.cos(thisangle));
      int endx1   = (int)(size/2.0 + radius56km*Math.sin(thisangle));
      int endy1   = (int)(size/2.0 - radius56km*Math.cos(thisangle));
      g.drawLine(startx1, starty1, endx1, endy1);
    }
    //Draw a filled rectangle as background for the number of strikes text
    double textangle = Math.PI*octant*45.0/180.0;
    int textradius = (int)(38.0*size/112.0);
    int textx = (int)(size/2.0 + textradius*Math.sin(textangle));
    int texty = (int)(size/2.0 - textradius*Math.cos(textangle));
    int textwidth = numstrikes<10?17:25;
    g.fillRect(textx-4, texty-14, textwidth, 20);
    //Reset the colour and draw the number of strikes in black
    g.setColor(Color.black);
    g.drawString(""+numstrikes, textx, texty);
  }


  /** Draw strike data for the nominated "near" range octant. */
  private void
  drawInnerOctant(int octant, int numstrikes, int size, Graphics g)
  {
    //don't draw anything if there are no strikes in this area
    if (numstrikes==0) {
      return;
    }

    g.setColor(getColor(numstrikes));

    double angle1 = Math.PI*(octant*45.0 - 22.5)/180.0;
    double angle2 = Math.PI*(octant*45.0 + 22.5)/180.0;
    int radius9km  = (int)(9.0*size/112.0);
    int radius20km = (int)(20.0*size/112.0);
    //Draw the appropriate number of radial lines for this octant
    double anglestep = getAngle(numstrikes);
    for (double thisangle=angle1; thisangle<=angle2; thisangle+=anglestep) {
      int startx1 = (int)(size/2.0 + radius9km*Math.sin(thisangle));
      int starty1 = (int)(size/2.0 - radius9km*Math.cos(thisangle));
      int endx1   = (int)(size/2.0 + radius20km*Math.sin(thisangle));
      int endy1   = (int)(size/2.0 - radius20km*Math.cos(thisangle));
      g.drawLine(startx1, starty1, endx1, endy1);
    }
    //Draw a filled rectangle as background for the number of strikes text
    double textangle = Math.PI*octant*45.0/180.0;
    int textradius = (int)(15.0*size/112.0);
    int textx = (int)(size/2.0 + textradius*Math.sin(textangle));
    int texty = (int)(size/2.0 - textradius*Math.cos(textangle));
    int textwidth = numstrikes<10?17:25;
    g.fillRect(textx-4, texty-14, textwidth, 20);
    //Reset the colour and draw the number of strikes in black
    g.setColor(Color.black);
    g.drawString(""+numstrikes, textx, texty);
  }

  /** Free all resources so that this MonPanel can disappear. */
  public
  void
  vaporise()
  {
    itsKeepRunning = false;
    //Awake our thread so it can clean-up and die
    synchronized (this) { this.notifyAll(); }
  }


  /** Called when the timer fires. */
  public
  void
  actionPerformed(ActionEvent e)
  {
    if (itsShowLatest) {
      System.err.println("Time to get the latest lightning data");
    }
    synchronized (this) { this.notifyAll(); }
  }



  /** Get the latest set of lightning strike data. The array will be
   * populated with strike data and the corresponding timestamp will be
   * returned. If no data could be obtained <tt>null</tt> will be
   * returned. */
  private
  AbsTime
  getLatest(int[] strikes) {
    itsLastTried = new AbsTime();
    Vector newdata = null;
    try {
      newdata = itsServer.getPointData(itsPointNames);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    if (newdata==null) {
      return null;
    }
    AbsTime tstamp = null;
    for (int i=0; i<20; i++) {
      PointData pd = (PointData)newdata.get(i);
      if (pd==null) {
        return null;
      }
      Number n = null;
      if (pd.getData() instanceof Number) {
        n = (Number)pd.getData();
      } else if (pd.getData() instanceof RelTime) {
        n = new Integer((int)(((RelTime)pd.getData()).getValue()/60000000));
      }
      if (n==null) {
        return null;
      }
      strikes[i] = n.intValue();
      if (tstamp==null) {
        tstamp=pd.getTimestamp();
        if (itsStrikeTimes!=null && itsStrikeTimes.length>0 &&
            itsStrikeTimes[0]!=null &&
            tstamp.getValue()==itsStrikeTimes[0].getValue()) 
        {
          //Oi! this is the same data you gave us last time!
          return null;
        }
      }
    }
    return tstamp;
  }


  /** Get lightning strike data during the specified range of times.
   * the internal structures itsStrikeData and itsStrikeTimes are filled
   * out according to the data. */
  private
  void
  getArchive(AbsTime start, AbsTime end)
  {
    //System.err.println("Making archive request:");
    //System.err.println("\t" + start.toString(AbsTime.Format.UTC_STRING));
    //System.err.println("\t" + end.toString(AbsTime.Format.UTC_STRING));

    try {
      boolean first = true;
      //We retrieve each monitor points archival data in turn
      for (int i=0; i<20; i++) {
        Vector thisdata = itsServer.getPointData((String)itsPointNames.get(i),
                                                 itsLoopStart, itsLoopEnd);
        if (i==19 && (thisdata==null || thisdata.size()<itsStrikeData[0].length)) 
        {
          //No uptime info was available, assume it is >15 minutes
          for (int j=0; j<itsStrikeData.length; j++) {
            itsStrikeData[j][19] = 16+5*j;
          }
        } else {
          if (first) {
            first = false;
            itsStrikeData = new int[thisdata.size()][];
            for (int j=0; j<thisdata.size(); j++) {
              itsStrikeData[j] = new int[20];
            }
            itsStrikeTimes = new AbsTime[thisdata.size()];
            for (int j=0; j<thisdata.size(); j++) {
              AbsTime thistime = ((PointData)(thisdata.get(j))).getTimestamp();
              if (thistime==null) {
                throw new Exception();
              }
              itsStrikeTimes[thisdata.size()-j-1] = thistime;
            }
          }

          for (int j=0; j<thisdata.size(); j++) {
            Number n = null;
            if (((PointData)(thisdata.get(j))).getData() instanceof Number) {
              n = (Number)((PointData)(thisdata.get(j))).getData();
            } else if (((PointData)(thisdata.get(j))).getData() instanceof RelTime) {
              n = new Integer((int)(((RelTime)((PointData)(thisdata.get(j))).getData()).getValue()/60000000));
            }

            if (n==null) {
              throw new Exception();
            }
            itsStrikeData[thisdata.size()-j-1][i] = n.intValue();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      itsStrikeData = new int[0][];
      itsStrikeTimes = new AbsTime[0];;
    }
  }


  /** Clear any current setup. */
  public synchronized
  void
  blankSetup()
  {
    synchronized (this) { this.notifyAll(); }
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
    synchronized (this) {
      try {
        //check if the setup is suitable for our class
        if (!setup.checkClass(this)) {
          System.err.println("Lightning:loadSetup: setup not for "
                             + this.getClass().getName());
          return false;
        }
        //the copy of the setup held by the frame is now incorrect
        if (itsFrame instanceof MonFrame) {
          ((MonFrame)itsFrame).itsSetup=null;
        }

/*	String mapname = (String)setup.get("map");
	if (mapname==null || mapname.equals("") || mapname.equals("none")) {
	  itsRawBackground = null;
	} else {
	  String imagename = "atnf/atoms/mon/gui/monpanel/" + mapname;
	  URL url = this.getClass().getClassLoader().getResource(imagename);
	  itsRawBackground = Toolkit.getDefaultToolkit().getImage(url);
	}*/

        String temp = (String)setup.get("legend");
        if (temp==null || temp.equals("true")) {
          itsLegend = true;
        } else {
          itsLegend = false;
        }

        temp = (String)setup.get("overlay");
        if (temp==null || temp.equals("true")) {
          itsOverlay = true;
        } else {
          itsOverlay = false;
        }

        temp = (String)setup.get("totals");
        if (temp==null || temp.equals("true")) {
          itsShowTotals = true;
        } else {
          itsShowTotals = false;
        }


        temp = (String)setup.get("mode");
        if (temp.equals("latest")) {
          itsShowLatest = true;
          itsShowLoop = false;
        } else {
          if (temp.equals("realtimeloop")) {
            itsShowLoop = true;
            itsShowLatest = false;
            int nummins = Integer.parseInt((String)setup.get("loopspan"));
            itsLoopSpan = RelTime.factory(nummins*60000000l);
          } else if (temp.equals("archiveloop")) {
            itsShowLoop = false;
            itsShowLatest = false;
            itsLoopStart = AbsTime.factory(Long.parseLong((String)setup.get("loopstart")));
            itsLoopEnd   = AbsTime.factory(Long.parseLong((String)setup.get("loopend")));
          }
          //Get the other loop parameters
          temp = (String)setup.get("framedelay");
          itsFrameDelay = Integer.parseInt(temp);
          temp = (String)setup.get("looppause");
          itsLoopPause = Integer.parseInt(temp);
        }

        itsSetup = setup;
        itsJustInitialised = true;
        //Make thread up so it can use new settings
        synchronized (this) { this.notifyAll(); }
      } catch (Exception e) {
        System.err.println("Lightning:loadSetup: " + e.getClass().getName()
                           + " " + e.getMessage());
        e.printStackTrace();
        blankSetup();
        return false;
      }
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


  /** Determine which monitor data server to use and try to establish
   * a connection to it. */
  private
  void
  findServer()
  {
    itsServer = MonClientUtil.getServer();
  }


  /** Get a Panel with all the controls required to configure this provider.
   * @return GUI controls to configure this data provider. */
  public 
  MonPanelSetupPanel
  getControls()
  {
    return new LightningSetupPanel(this, itsFrame);
  }


  /** Dump current data to the given output stream. This is the mechanism
   * through which data can be exported to a file.
   * @param p The print stream to write the data to. */
  public synchronized
  void
  export(PrintStream p)
  {
    final String rcsid = "$Id: Lightning.java,v 1.7 2008/02/12 03:01:37 bro764 Exp $";
    p.println("#Dump from Lightning " + rcsid);
    p.println("#Data dumped at "
	      + (new AbsTime().toString(AbsTime.Format.UTC_STRING)));

    p.println();
    p.println();
  }

  public String getLabel() { return null; }

  /** Simple test application. */
  public static void main(String[] argv)
  {
/*    JFrame foo = new JFrame("ATTimeSeries Test App");
    foo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    foo.getContentPane().setLayout(new BoxLayout(foo.getContentPane(), BoxLayout.Y_AXIS));

    ATTimeSeries ts1 = new ATTimeSeries();
    SavedSetup s1 = new SavedSetup("ant.temps", "atnf.atoms.mon.gui.monpanel.ATTimeSeries",
        			   "Antenna Pedestal Temperatures:true:86400000000:0:2:ca05.ant.PEDTEM:ca06.ant.PEDTEM");
    SavedSetup s2 = new SavedSetup("seemon.phases", "atnf.atoms.mon.gui.monpanel.ATTimeSeries",
				   "Seeing Monitor Phases:true:86400000000:0:1:seemon.site.seemon.Phase:");
    SavedSetup s3 = new SavedSetup("clock.tickphase", "atnf.atoms.mon.gui.monpanel.ATTimeSeries",
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

