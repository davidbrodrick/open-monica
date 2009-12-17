package atnf.atoms.mon.gui.monpanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.StandardGradientPaintTransformer;

import javax.swing.*;

import java.util.*;

import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.client.*;

public class DialPanel extends MonPanel
{
    static {
        MonPanel.registerMonPanel("Dial Gauge", DialPanel.class);
    }

    /** Setup controls for a single dial. */
    public class SingleDialSetupPanel extends JPanel
    {
        /** Reference to the point source selector for this axis. */
        public PointSourceSelector itsSelector = new PointSourceSelector();

        /** TextField for the dial title. */
        protected JTextField itsTitleField = new JTextField(30);

        /** Textfield to hold the scale minimum. */
        public JTextField itsScaleMin = new JTextField(8);

        /** Textfield to hold the scale maximum. */
        public JTextField itsScaleMax = new JTextField(8);

        /** Widget for choosing points to display. */
        public SingleDialSetupPanel()
        {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createLineBorder(Color.red));
            itsSelector.setToolTipText("Select points to be displayed");
            itsSelector.setPreferredSize(new Dimension(180, 180));
            add(itsSelector);

            JPanel temppanel = new JPanel();
            // temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
            JLabel templabel = new JLabel("Label:");
            templabel.setForeground(Color.black);
            templabel.setToolTipText("Enter a label for this dial");
            temppanel.add(templabel);
            temppanel.add(itsTitleField);
            templabel = new JLabel("Min:");
            templabel.setForeground(Color.black);
            templabel.setToolTipText("Specify minimum value for the dial");
            temppanel.add(templabel);
            itsScaleMin.setToolTipText("Specify minimum value for the dial");
            temppanel.add(itsScaleMin);
            templabel = new JLabel("Max:");
            templabel.setForeground(Color.black);
            templabel.setToolTipText("Specify maximum value for the dial");
            temppanel.add(templabel);
            itsScaleMax.setToolTipText("Specify maximum value for the dial");
            temppanel.add(itsScaleMax);
            add(temppanel);
        }

        /** Return a string summary of the current setup. */
        public void getSetup(SavedSetup setup, int number) throws Exception
        {
            // Ensure user has not entered any reserved characters
            if (!checkString(itsTitleField.getText())) {
                JOptionPane.showMessageDialog(this, "The axis label:\n" + "\"" + itsTitleField.getText() + "\"\n"
                                + "contains reserved characters.\n" + "You must not use ~ : or `\n", "Reserved Characters",
                                JOptionPane.WARNING_MESSAGE);
                return;
            }

            Vector selpoints = itsSelector.getSelections();
            // Ensure the user selected at least one point for this axis!
            if (selpoints.size() == 0) {
                JOptionPane.showMessageDialog(this, "No points were selected for dial\n" + "number " + (number + 1)
                                + ". You need to select\n" + "at least one point!", "No Points Selected!",
                                JOptionPane.WARNING_MESSAGE);
                throw new Exception();
            }
            setup.put("numpoints" + number, "" + selpoints.size());
            String temp = "";
            for (int i = 0; i < selpoints.size(); i++) {
                temp += (String) selpoints.get(i) + ":";
            }
            setup.put("points" + number, temp);

            setup.put("label" + number, itsTitleField.getText());

            try {
                Double.parseDouble(itsScaleMax.getText());
                Double.parseDouble(itsScaleMin.getText());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "The fields for minimum and maximum\n"
                                + "scales must contain numbers, eg, \"42\"\n" + "or \"99.9\".", "Bad Scale Entered",
                                JOptionPane.WARNING_MESSAGE);
                return;
            }
            setup.put("min" + number, itsScaleMin.getText());
            setup.put("max" + number, itsScaleMax.getText());
        }

        public void showSetup(SavedSetup setup, int number)
        {
            try {
                String temp = (String) setup.get("label" + number);
                itsTitleField.setText(temp);
                temp = (String) setup.get("min" + number);
                itsScaleMin.setText(temp);
                temp = (String) setup.get("max" + number);
                itsScaleMax.setText(temp);
                temp = (String) setup.get("numpoints" + number);

                int numpoints = Integer.parseInt(temp);
                Vector<String> points = new Vector<String>(numpoints);

                temp = (String) setup.get("points" + number);
                StringTokenizer st = new StringTokenizer(temp, ":");
                for (int i = 0; i < numpoints; i++) {
                    points.add(st.nextToken());
                }
                itsSelector.setSelections(points);
            } catch (Exception e) {
            }
        }
    }

    public class DialSetupPanel extends MonPanelSetupPanel implements ActionListener
    {
        /** The main panel which hold our GUI controls. */
        protected JPanel itsSetupPanel = new JPanel();

        /** Label for number of dials. */
        protected JLabel itsNumDials = new JLabel("1");

        /** Vector which contains the setup panels for each of the dials. */
        protected Vector<SingleDialSetupPanel> itsDials = new Vector<SingleDialSetupPanel>();

        /** Panel which contains the setup controls for each dial. */
        protected JPanel itsDialsPanel = new JPanel();

        /** Button for horizontal stacking. */
        protected JRadioButton itsHorizontal = new JRadioButton("Horizontally");

        /** Button for verical stacking. */
        protected JRadioButton itsVertical = new JRadioButton("Vertically");

        public DialSetupPanel(DialPanel panel, JFrame frame)
        {
            super(panel, frame);

            itsSetupPanel.setLayout(new BorderLayout());

            JPanel temppanel2 = new JPanel();
            temppanel2.setLayout(new BoxLayout(temppanel2, BoxLayout.Y_AXIS));

            JPanel temppanel = new JPanel();
            temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.X_AXIS));
            ButtonGroup tempgroup = new ButtonGroup();
            tempgroup.add(itsHorizontal);
            tempgroup.add(itsVertical);
            itsHorizontal.doClick();
            JLabel templabel = new JLabel("Stack Gauges:");
            templabel.setForeground(Color.black);
            templabel.setToolTipText("Select how the dials will be aligned");
            temppanel.add(templabel);
            itsHorizontal.setForeground(Color.black);
            itsHorizontal.setToolTipText("Align in a horizontal row");
            itsVertical.setForeground(Color.black);
            itsVertical.setToolTipText("Align in a vertical column");
            temppanel.add(itsHorizontal);
            temppanel.add(itsVertical);
            temppanel2.add(temppanel);

            temppanel = new JPanel();
            templabel = new JLabel("Number of Dials:");
            templabel.setForeground(Color.black);
            temppanel.add(templabel);
            temppanel.add(Box.createRigidArea(new Dimension(5, 0)));
            temppanel.add(itsNumDials);
            temppanel.add(Box.createRigidArea(new Dimension(5, 0)));
            JButton tempbut = new JButton("+");
            tempbut.addActionListener(this);
            tempbut.setActionCommand("Add-Dial");
            tempbut.setToolTipText("Add another dial to the display");
            temppanel.add(tempbut);
            tempbut = new JButton("-");
            tempbut.addActionListener(this);
            tempbut.setActionCommand("Remove-Dial");
            tempbut.setToolTipText("Remove the last dial from the display");
            temppanel.add(tempbut);
            temppanel.setBorder(BorderFactory.createLineBorder(Color.black));
            temppanel2.add(temppanel);
            add(temppanel2, BorderLayout.NORTH);

            itsDialsPanel.setLayout(new BoxLayout(itsDialsPanel, BoxLayout.Y_AXIS));
            JScrollPane scroller = new JScrollPane(itsDialsPanel);
            scroller.setPreferredSize(new Dimension(500, 350));
            scroller.setMaximumSize(new Dimension(2000, 350));
            scroller.setBorder(BorderFactory.createLoweredBevelBorder());
            itsSetupPanel.add(scroller, BorderLayout.CENTER);

            itsDials.add(new SingleDialSetupPanel());
            itsDialsPanel.add(itsDials.get(0));

            add(itsSetupPanel, BorderLayout.CENTER);
        }

        /**
         * Return the current setup, as determined by the GUI controls. It provides the
         * means of extracting the setup specified by the user into a useable format.
         * 
         * @return SavedSetup specified by GUI controls, or <tt>null</tt> if no setup
         * can be extracted from the GUI at present.
         */
        protected SavedSetup getSetup()
        {
            SavedSetup setup = new SavedSetup("temp", "atnf.atoms.mon.gui.monpanel.DialPanel");
            if (itsHorizontal.isSelected()) {
                setup.put("align", "horizontal");
            } else {
                setup.put("align", "vertical");
            }

            setup.put("numdials", "" + itsDials.size());
            for (int i = 0; i < itsDials.size(); i++) {
                try {
                    itsDials.get(i).getSetup(setup, i);
                } catch (Exception e) {
                    return null;
                }
            }

            return setup;
        }

        /** Make the controls show information about the given setup. */
        public void showSetup(SavedSetup setup)
        {
            try {
                // Alignment
                String temp = setup.get("align");
                if (temp.equals("vertical")) {
                    itsVertical.doClick();
                } else {
                    itsHorizontal.doClick();
                }

                // Purge any old dial setups from the display
                while (itsDials.size() > 0) {
                    SingleDialSetupPanel byebye = itsDials.get(0);
                    itsDialsPanel.remove(byebye);
                    itsDials.remove(byebye);
                }

                // Number of dials
                temp = (String) setup.get("numdials");
                itsNumDials.setText(temp);
                int numdials = Integer.parseInt(temp);
                // Next we need to parse the information for each dial
                for (int i = 0; i < numdials; i++) {
                    SingleDialSetupPanel thisdial = new SingleDialSetupPanel();
                    thisdial.showSetup(setup, i);
                    itsDials.add(thisdial);
                    itsDialsPanel.add(thisdial);
                }
            } catch (Exception e) {
                System.err.println("DialSetupPanel:showSetup: " + e.getMessage());
            }
        }

        public void actionPerformed(ActionEvent e)
        {
            super.actionPerformed(e);
            String cmd = e.getActionCommand();
            if (cmd.equals("Add-Dial")) {
                itsDials.add(new SingleDialSetupPanel());
                itsDialsPanel.add(itsDials.get(itsDials.size() - 1));
                itsNumDials.setText("" + itsDials.size());
                itsDialsPanel.invalidate();
                itsDialsPanel.repaint();
            } else if (cmd.equals("Remove-Dial")) {
                if (itsDials.size() == 1) {
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    itsDialsPanel.remove(itsDials.get(itsDials.size() - 1));
                    itsDials.remove(itsDials.size() - 1);
                    itsNumDials.setText("" + itsDials.size());
                    itsDialsPanel.invalidate();
                    itsDialsPanel.repaint();
                }
            }
        }
    };

    public class DataListener implements PointListener
    {
        /** The jfreechart container for the value. */
        DefaultValueDataset itsDataset;

        /** The name of the monitor point we are subscribed to. */
        String itsPoint;

        /** The Dial itself. */
        DialPlot itsDial;

        /** The Pointer. */
        DialPointer.Pointer itsPointer;

        DataListener(String point, DefaultValueDataset dataset, DialPlot dial, DialPointer.Pointer pointer)
        {
            itsPoint = point;
            itsDataset = dataset;
            itsDial = dial;
            itsPointer = pointer;
            DataMaintainer.subscribe(itsPoint, this);
        }

        public void onPointEvent(Object source, final PointEvent evt)
        {
            Runnable newdata = new Runnable() {
                public void run()
                {
                    try {
                        PointData pd = evt.getPointData();
                        double val = ((Number) pd.getData()).doubleValue();
                        StandardDialScale scale = ((StandardDialScale) itsDial.getScale(0));
                        if (val < scale.getLowerBound()) {
                            scale.setLowerBound(val);
                        } else if (val > scale.getUpperBound()) {
                            scale.setUpperBound(val);
                        }
                        if (pd.getAlarm()) {
                            itsPointer.setFillPaint(Color.RED);
                        } else {
                            itsPointer.setFillPaint(Color.DARK_GRAY);
                        }
                        itsDataset.setValue(val);
                    } catch (Exception e) {
                    }
                }
            };
            try {
                // Need to do the notification using event thread
                SwingUtilities.invokeLater(newdata);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void unsubscribe()
        {
            DataMaintainer.unsubscribe(itsPoint, this);
        }
    }

    /** Holds the listeners for each point being displayed. */
    private Vector<DataListener> itsListeners = new Vector<DataListener>();

    /** The setup currently being displayed. */
    private SavedSetup itsSetup = null;

    public DialPanel()
    {
        add(new JLabel("Configure dial display options under the \"Dial Gauge\" setup tab", JLabel.CENTER));
    }

    /**
     * Dump current data to the given output stream. This is the mechanism through which
     * data can be exported to a file.
     * 
     * @param p The print stream to write the data to.
     */
    public void export(PrintStream p)
    {
        // Not very meaningful to export data from a dial..
    }

    /** Return the SetupPanel. */
    public MonPanelSetupPanel getControls()
    {
        return new DialSetupPanel(this, itsFrame);
    }

    public String getLabel()
    {
        return "Dial Panel";
    }

    /**
     * Return the current setup, as determined by the GUI controls. It provides the means
     * of extracting the setup specified by the user into a useable format.
     * 
     * @return SavedSetup specified by GUI controls, or <tt>null</tt> if no setup can be
     * extracted from the GUI at present.
     */
    public SavedSetup getSetup()
    {
        return itsSetup;
    }

    /** Make a dial by parsing the information from the setup. */
    private JFreeChart makeDial(SavedSetup setup, int num)
    {
        int numpoints = Integer.parseInt(setup.get("numpoints" + num));

        DialPlot plot = new DialPlot();
        plot.setDialFrame(new StandardDialFrame());
        GradientPaint gp = new GradientPaint(new Point(), new Color(255, 255, 255), new Point(), new Color(170, 170, 220));
        DialBackground db = new DialBackground(gp);
        db.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL));
        plot.setBackground(db);
        DialCap cap = new DialCap();
        plot.setCap(cap);

        // Set dial scale
        StandardDialScale scale = new StandardDialScale();
        scale.setLowerBound(Float.parseFloat(setup.get("min" + num)));
        scale.setUpperBound(Float.parseFloat(setup.get("max" + num)));
        scale.setStartAngle(-120);
        scale.setExtent(-300);
        plot.addScale(0, scale);

        // Only show the value if we are only displaying one point
        if (numpoints == 1) {
            DialValueIndicator dvi = new DialValueIndicator(0);
            plot.addLayer(dvi);
        }

        String allpoints = setup.get("points" + num);
        StringTokenizer st = new StringTokenizer(allpoints, ":");
        for (int i = 0; i < numpoints; i++) {
            DialPointer.Pointer p = new DialPointer.Pointer(i);
            plot.addPointer(p);

            DefaultValueDataset dataset = new DefaultValueDataset(i * 10.0);
            plot.setDataset(i, dataset);

            DataListener thislistener = new DataListener(st.nextToken(), dataset, plot, p);
            itsListeners.add(thislistener);
        }

        DialTextAnnotation annotation1 = new DialTextAnnotation(setup.get("label" + num));
        annotation1.setFont(new Font("Dialog", Font.BOLD, 14));
        annotation1.setRadius(0.7);

        return new JFreeChart(setup.get("label" + num), plot);
    }

    /**
     * Remove all dials from the display and unsubscribe from data updates.
     */
    public void removeAll()
    {
        super.removeAll();
        Iterator<DataListener> i = itsListeners.iterator();
        while (i.hasNext()) {
            i.next().unsubscribe();
        }
        itsListeners.clear();
    }

    /**
     * Configure this MonPanel to use the specified setup.
     * 
     * @param setup class-specific setup information.
     * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if there was a
     * problem and the setup cannot be used.
     */
    public boolean loadSetup(SavedSetup setup)
    {
        try {
            // Remove current widgets and unsubscribe listeners
            removeAll();

            int numdials = Integer.parseInt(setup.get("numdials"));

            if (setup.get("align").equals("horizontal")) {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                setPreferredSize(new Dimension(numdials * 180, 180));
            } else {
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                setPreferredSize(new Dimension(180, numdials * 180));
            }

            for (int i = 0; i < numdials; i++) {
                JFreeChart thisdial = makeDial(setup, i);
                add(new ChartPanel(thisdial));
            }
        } catch (Exception e) {
            itsSetup = null;
            System.err.println("Dial:loadSetup: " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        itsSetup = setup;
        return true;
    }

    public void vaporise()
    {
        removeAll();
    }
}
