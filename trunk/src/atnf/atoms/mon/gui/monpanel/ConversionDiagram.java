// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.awt.geom.*;

import javax.swing.*;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.DataMaintainer;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.*;

/**
 * Class representing a conversion diagram.
 * 
 * @author Camille Nicodemus
 * @see MonPanel
 */
public class ConversionDiagram extends MonPanel {
	/** Current antenna whose monitor point information is being displayed */
	protected static String focusAntenna = "1";
	/** Position of F14 switch */
	protected static int itsF14RFSELA = 1;
	protected static int itsF14RFSELB = 1;
	protected static int itsC34RFA = 1;
	protected static int itsC34RFB = 1;
	/** Listener strings for displaying on diagram */
	protected static String RFA = "0.0";
	/** Stores current values for the monitor points subscribed to */
	protected static Hashtable<String, String> itsMonPointsHash = new Hashtable<String, String>();
	/** Stores all current data listeners */
	protected static Vector<DataListener> itsDataListeners = new Vector<DataListener>();

	/** Array of monitor points used for DataListener */
	protected final String[] itsMonPoints = { "conversion.C34.RFA",
			"conversion.C34.RFB", "conversion.C26.RFA", "conversion.C26.RFB",
			"conversion.C21.LO", "conversion.C21.LOCK", "conversion.C28.RFA",
			"conversion.C28.RFB", "conversion.F14.ATTENA_M",
			"conversion.F14.ATTENB_M", "conversion.F14.RFSELA_M_raw",
			"conversion.F14.RFSELB_M_raw", "conversion.C34.RFA1M_raw",
			"conversion.C34.RFB1M_raw", "conversion.C21.RFA",
			"conversion.C21.RFB", "LO.L86.PLLLock", "LO.sitesynth.freq",
			"cabb.correlator.RMS-1A", "cabb.correlator.RMS-1B",
			"cabb.correlator.RMS-2A", "cabb.correlator.RMS-2B",
			"cabb.data_links.OptPow-1A-0R", "cabb.data_links.OptPow-1A-1Y",
			"cabb.data_links.OptPow-1A-2G", "cabb.data_links.OptPow-1A-3B",
			"cabb.data_links.OptPow-1B-0R", "cabb.data_links.OptPow-1B-1Y",
			"cabb.data_links.OptPow-1B-2G", "cabb.data_links.OptPow-1B-3B",
			"cabb.data_links.OptPow-2A-0R", "cabb.data_links.OptPow-2A-1Y",
			"cabb.data_links.OptPow-2A-2G", "cabb.data_links.OptPow-2A-3B",
			"cabb.data_links.OptPow-2B-0R", "cabb.data_links.OptPow-2B-1Y",
			"cabb.data_links.OptPow-2B-2G", "cabb.data_links.OptPow-2B-3B",
			"cabb.cl1f1.AttenA", "cabb.cl1f1.AttenB",
			"cabb.cl1f2.AttenA", "cabb.cl1f2.AttenB",
			"cabb.cl1f1.PLLLock", "cabb.cl1f2.PLLLock",
			"receiver.CX.F16.AttenA", "receiver.CX.F16.AttenB",
			"receiver.LS.F15.AttenA", "receiver.LS.F15.AttenB"};

	static {
		MonPanel.registerMonPanel("Conversion Diagram", ConversionDiagram.class);
	}

	// /// NESTED CLASS: CONVERSION DIAGRAM SETUP PANEL /////
	public class ConversionDiagramSetupPanel extends MonPanelSetupPanel
			implements ActionListener {
		/** The main panel which hold our GUI controls. */
		protected JPanel itsSetupPanel = new JPanel();

		public ConversionDiagramSetupPanel(ConversionDiagram panel, JFrame frame) {
			super(panel, frame);

			JLabel temp = new JLabel("Select which antenna you wish to use: ");
			String[] antennae = { "1", "2", "3", "4", "5", "6" };
			JComboBox antennaCombo = new JComboBox(antennae);
			antennaCombo.addActionListener(this);
			antennaCombo.setActionCommand("setantenna");

			itsSetupPanel.add(temp);
			itsSetupPanel.add(antennaCombo);
			add(itsSetupPanel);
		}

		protected SavedSetup getSetup() {
			SavedSetup setup = new SavedSetup("temp",
					"atnf.atoms.mon.gui.monpanel.ConversionDiagram");
			setup.put("focus", focusAntenna);

			return setup;
		}

		protected void showSetup(SavedSetup setup) {
		}

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			String cmd = e.getActionCommand();
			if (cmd.equals("setantenna")) {
				JComboBox cb = (JComboBox) e.getSource();
				focusAntenna = (String) cb.getSelectedItem();
			}
		}
	}

	// /// END NESTED CLASS /////

	/**
	 * PaintDiagram contains methods for painting and displaying the conversion
	 * diagram
	 */
	private class PaintDiagram extends JPanel {
		/** RGB defined colors for painting components */
		private Color thisGreen = new Color(85, 187, 102); // Green
		private Color thisOrange = new Color(255, 119, 034); // Orange
		private Color thisPurple = new Color(119, 034, 136); // Purple
		private Color thisBlue = new Color(034, 170, 255); // Blue
		private Color thisYellow = new Color(255, 255, 100); // Yellow

		/** Various strokes for painting */
		private Stroke dottedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0);
		private Stroke thickStroke = new BasicStroke(2, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_BEVEL, 0, null, 0);
		private Stroke semithickStroke = new BasicStroke((float) 1.5,
				BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, null, 0);
		private final double circlesize = 4;

		/** Various fonts for drawing */
		private Font receiverFont = new Font("Helvetica", Font.BOLD, 11);
		private Font normalFont = new Font("Helvetica", Font.PLAIN, 12);

		public PaintDiagram() {
			for (int i = 0; i < itsMonPoints.length; i++) {
				if (itsMonPoints[i].equals("conversion.F14.RFSELA_M_raw")
						| itsMonPoints[i].equals("conversion.F14.RFSELB_M_raw")
						| itsMonPoints[i].equals("conversion.C34.RFA1M_raw")
						| itsMonPoints[i].equals("conversion.C34.RFB1M_raw")
						| itsMonPoints[i].startsWith("receiver")) {
					itsMonPointsHash.put(itsMonPoints[i], "0");
				} else {
					itsMonPointsHash.put(itsMonPoints[i], "0.0");
				}
			}
		}

		public Dimension getPreferredSize() {
			return new Dimension(1200, 1000);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			// ///// BLACK C34 ///////
			int[] c34d = { 250, 410, 200, 90 }; // Dotted box {x, y, width,
												// height}
			double[] c34 = { 300, 435, 100, 30 }; // {x, y, width, height}

			g2d.setColor(Color.black);
			drawDottedBox(g2d, Color.black, c34d[0], c34d[1], c34d[2], c34d[3]);
			g2d.drawString("C34", c34d[0] + 5, c34d[1] + c34d[3] - 5);
			g2d.setStroke(semithickStroke);

			g2d.draw(new Rectangle2D.Double(c34[0], c34[1], c34[2], c34[3]));
			g2d.draw(new Line2D.Double(c34[0] + c34[2] / 2, c34[1] + c34[3],
					c34[0] + c34[2] / 2, c34[1] + c34[3] + 35));
			g2d.draw(new Ellipse2D.Double(c34[0] + c34[2] / 2 - 2, c34[1]
					+ c34[3], circlesize, circlesize));

			// C34 ARROW
			itsC34RFA = Integer.parseInt(itsMonPointsHash
					.get("conversion.C34.RFA1M_raw"));
			itsC34RFB = Integer.parseInt(itsMonPointsHash
					.get("conversion.C34.RFB1M_raw"));
			drawC34RFA(g2d, itsC34RFA);
			drawC34RFB(g2d, itsC34RFB);

			// Small circles
			g2d.setColor(thisPurple);
			g2d.draw(new Ellipse2D.Double(c34[0] + 12, c34[1] + 5, circlesize,
					circlesize));
			g2d.setColor(thisGreen);
			g2d.draw(new Ellipse2D.Double(c34[0] + 37, c34[1] + 5, circlesize,
					circlesize));
			g2d.setColor(thisBlue);
			g2d.draw(new Ellipse2D.Double(c34[0] + 62, c34[1] + 5, circlesize,
					circlesize));
			g2d.draw(new Ellipse2D.Double(c34[0] + 87, c34[1] + 5, circlesize,
					circlesize));

			// RFA Mon point
			int[] monRFA = { 295, 485 };
			g2d.setColor(thisOrange);
			drawMonitorPoint(g2d, monRFA[0] + 25, monRFA[1] - 7, 4); // LO
			g2d.draw(new Line2D.Double(monRFA[0] + 55, monRFA[1] - 5,
					monRFA[0] + 28, monRFA[1] - 5));
			g2d.drawString("RF (V)", monRFA[0] - 10, monRFA[1]);
			drawMonPointBox(g2d, monRFA[0], monRFA[1] + 5, false, false);
			drawValues(g2d, "conversion.C34.RFA", monRFA[0] + 8,
					monRFA[1] + 16, 1);
			drawValues(g2d, "conversion.C34.RFB", monRFA[0] + 8,
					monRFA[1] + 37, 2);

			// //// PURPLE F14 ////////
			int[] f14d = { 25, 50, 580, 250 }; // x, y, width, height
			double[] f14r = { 300, 10, 30, 10, 30 }; // Receiver x, y, width,
														// height, crossed
														// circle diameter
			double[] f14dt = { 160, 80, 30, 15 }; // Leftmost doubler x, y,
													// width, height
			double[] f14l86 = { 0, f14dt[1] + 155, 20 }; // L86 x, y, diameter
			double[] f14m = { 280, 125, 200, 50 }; // Multibox x, y, width,
													// height
			double[] c26 = { 130, 325, 100, 70 }; // C26 x, y, width, height

			g2d.setColor(thisPurple);
			drawDottedBox(g2d, thisPurple, f14d[0] + 65, f14d[1], f14d[2] - 65,
					f14d[3]);
			g2d.drawString("F14", f14d[0] + 70, f14d[1] + f14d[3] - 5);
			g2d.setStroke(semithickStroke);
			g2d.setFont(receiverFont);
			g2d.drawString("12mm", (int) f14r[0] - 27, (int) f14r[1] + 35);
			g2d.drawString("7mm", (int) f14r[0] + 38, (int) f14r[1] + 35);
			g2d.drawString("3mm", (int) f14r[0] + 110, (int) f14r[1] + 35);
			drawReceiver(g2d, f14r[0], f14r[1], f14r[2], f14r[3]); // 1
			drawReceiver(g2d, f14r[0] + 60, f14r[1], f14r[2], f14r[3]); // 2
			drawReceiver(g2d, f14r[0] + 130, f14r[1], f14r[2], f14r[3]); // 3

			g2d.setFont(normalFont);

			g2d.draw(new Line2D.Double(f14r[0] + f14r[2] / 2, f14r[1] + 35,
					f14r[0] + f14r[2] / 2, f14r[1] + 60));
			g2d.draw(new Line2D.Double(f14r[0] + f14r[2] / 2 + 60,
					f14r[1] + 35, f14r[0] + f14r[2] / 2 + 60, f14r[1] + 60));
			g2d.draw(new Line2D.Double(f14r[0] + f14r[2] / 2 + 130,
					f14r[1] + 35, f14r[0] + f14r[2] / 2 + 130, f14r[1] + 60));
			drawCrossedCircle(g2d, f14r[0], f14r[1] + 60, f14r[4]);
			drawCrossedCircle(g2d, f14r[0] + 60, f14r[1] + 60, f14r[4]);
			drawCrossedCircle(g2d, f14r[0] + 130, f14r[1] + 60, f14r[4]);
			// Connecting Receivers to Multibox
			g2d.draw(new Line2D.Double(f14r[0] + f14r[2] / 2, f14r[1] + 60
					+ f14r[4], f14r[0] + f14r[2] / 2, f14r[1] + 60 + f14r[4]
					+ 10)); // Rcv 1
			g2d.draw(new Line2D.Double(f14r[0] + f14r[2] / 2, f14r[1] + 60
					+ f14r[4] + 10, f14m[0] + 14, f14r[1] + 60 + f14r[4] + 10));
			g2d.draw(new Line2D.Double(f14m[0] + 14, f14r[1] + 60 + f14r[4]
					+ 10, f14m[0] + 14, f14m[1] + 5));
			g2d.draw(new Line2D.Double(f14r[0] + 60 + f14r[2] / 2 - 5, f14r[1]
					+ 60 + f14r[4], f14r[0] + 60 + f14r[2] / 2 - 5, f14r[1]
					+ 60 + f14r[4] + 10)); // Rcv 2
			g2d.draw(new Line2D.Double(f14r[0] + 60 + f14r[2] / 2 - 5, f14r[1]
					+ 60 + f14r[4] + 10, f14m[0] + 49, f14r[1] + 60 + f14r[4]
					+ 10));
			g2d.draw(new Line2D.Double(f14m[0] + 49, f14r[1] + 60 + f14r[4]
					+ 10, f14m[0] + 49, f14m[1] + 5));
			g2d.draw(new Line2D.Double(f14r[0] + 60 + f14r[2] / 2 + 5, f14r[1]
					+ 60 + f14r[4], f14r[0] + 60 + f14r[2] / 2 + 5, f14r[1]
					+ 60 + f14r[4] + 20)); // Rcv 2
			g2d.draw(new Line2D.Double(f14r[0] + 60 + f14r[2] / 2 + 5, f14r[1]
					+ 60 + f14r[4] + 20, f14m[0] + 14 + 35 * 2, f14r[1] + 60
					+ f14r[4] + 20));
			g2d.draw(new Line2D.Double(f14m[0] + 14 + 35 * 2, f14r[1] + 60
					+ f14r[4] + 20, f14m[0] + 14 + 35 * 2, f14m[1] + 5));
			g2d.draw(new Line2D.Double(f14r[0] + 130 + f14r[2] / 2 - 5, f14r[1]
					+ 60 + f14r[4], f14r[0] + 130 + f14r[2] / 2 - 5, f14r[1]
					+ 60 + f14r[4] + 10)); // Rcv 3
			g2d.draw(new Line2D.Double(f14r[0] + 130 + f14r[2] / 2 - 5, f14r[1]
					+ 60 + f14r[4] + 10, f14m[0] + 14 + 35 * 3, f14r[1] + 60
					+ f14r[4] + 10));
			g2d.draw(new Line2D.Double(f14m[0] + 14 + 35 * 3, f14r[1] + 60
					+ f14r[4] + 10, f14m[0] + 14 + 35 * 3, f14m[1] + 5));
			g2d.draw(new Line2D.Double(f14r[0] + 130 + f14r[2] / 2 + 5, f14r[1]
					+ 60 + f14r[4], f14r[0] + 130 + f14r[2] / 2 + 5, f14r[1]
					+ 60 + f14r[4] + 10)); // Rcv 3
			g2d.draw(new Line2D.Double(f14r[0] + 130 + f14r[2] / 2 + 5, f14r[1]
					+ 60 + f14r[4] + 10, f14m[0] + 14 + 35 * 5, f14r[1] + 60
					+ f14r[4] + 10));
			g2d.draw(new Line2D.Double(f14m[0] + 14 + 35 * 5, f14r[1] + 60
					+ f14r[4] + 10, f14m[0] + 14 + 35 * 5, f14m[1] + 5));
			g2d.draw(new Line2D.Double(f14m[0] + 14 + 35 * 4, f14m[1] + 5,
					f14m[0] + 14 + 35 * 4, f14m[1] - 10)); // Random one

			// Doublers/Triplers
			g2d.draw(new Rectangle2D.Double(f14dt[0], f14dt[1], f14dt[2],
					f14dt[3])); // X2
			g2d.draw(new Rectangle2D.Double(f14dt[0] + 30, f14dt[1] + 45,
					f14dt[2], f14dt[3])); // X3
			g2d.draw(new Rectangle2D.Double(f14dt[0] + 80, f14dt[1] + 45,
					f14dt[2], f14dt[3])); // X6/1
			g2d.draw(new Rectangle2D.Double(f14dt[0] + 80, f14dt[1] + 75,
					f14dt[2], f14dt[3])); // X6/2
			g2d.draw(new Rectangle2D.Double(f14dt[0] + 80, f14dt[1] + 105,
					f14dt[2], f14dt[3])); // X6/3
			g2d.draw(new Rectangle2D.Double(f14dt[0] + 10, f14dt[1] + 155,
					f14dt[2] + 70, f14dt[3] + 7));
			g2d.drawString("X2", (int) f14dt[0] + 5, (int) f14dt[1] + 12);
			g2d.drawString("X3", (int) f14dt[0] + 35, (int) f14dt[1] + 57);
			g2d.drawString("X2", (int) f14dt[0] + 85, (int) f14dt[1] + 57);
			g2d.drawString("X2", (int) f14dt[0] + 85, (int) f14dt[1] + 87);
			g2d.drawString("X2", (int) f14dt[0] + 85, (int) f14dt[1] + 117);
			// Connecting Lines
			g2d.draw(new Line2D.Double(f14dt[0] + 30 + f14dt[2] / 2, f14dt[1]
					+ 45 + f14dt[3], f14dt[0] + 30 + f14dt[2] / 2,
					f14dt[1] + 155));// X3 to Combining Box
			g2d.draw(new Line2D.Double(f14dt[0] + f14dt[2], f14dt[1] + f14dt[3]
					/ 2, f14r[0], f14dt[1] + f14dt[3] / 2)); // X2 to Receiver
			g2d.draw(new Line2D.Double(f14dt[0] + 30 + f14dt[2] / 2,
					f14dt[1] + 45, f14dt[0] + 30 + f14dt[2] / 2, f14d[1] + 10)); // X3
																					// to
																					// Receiver
			g2d.draw(new Line2D.Double(f14dt[0] + 30 + f14dt[2] / 2,
					f14d[1] + 10, f14r[0] + 60 - 20, f14d[1] + 10));
			g2d.draw(new Line2D.Double(f14r[0] + 60 - 20, f14d[1] + 10,
					f14r[0] + 60 - 20, f14d[1] + 34));
			g2d.draw(new Line2D.Double(f14r[0] + 60 - 20, f14d[1] + 34,
					f14r[0] + 60, f14d[1] + 34));
			g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2] / 2,
					f14dt[1] + 45, f14dt[0] + 80 + f14dt[2] / 2, f14d[1] + 15)); // X6
																					// to
																					// Receiver
			g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2] / 2,
					f14d[1] + 15, f14r[0] + 130 - 20, f14d[1] + 15));
			g2d.draw(new Line2D.Double(f14r[0] + 130 - 20, f14d[1] + 15,
					f14r[0] + 130 - 20, f14d[1] + 37));
			g2d.draw(new Line2D.Double(f14r[0] + 130 - 20, f14d[1] + 37,
					f14r[0] + 130, f14d[1] + 37));
			g2d.draw(new Line2D.Double(f14dt[0] + f14dt[2] / 2, f14dt[1]
					+ f14dt[3], f14dt[0] + f14dt[2] / 2, f14dt[1] + 155)); // X2
																			// to
																			// Combining
																			// box
			g2d.draw(new Line2D.Double(f14l86[0] + f14l86[2], f14l86[1]
					+ f14l86[2] / 2, f14dt[0] + 10, f14l86[1] + f14l86[2] / 2)); // Combining
																					// box
																					// to
																					// L86
			g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2] / 2, f14dt[1]
					+ 45 + f14dt[3], f14dt[0] + 80 + f14dt[2] / 2,
					f14dt[1] + 75)); // X6/1 to X6/2
			g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2] / 2, f14dt[1]
					+ 75 + f14dt[3], f14dt[0] + 80 + f14dt[2] / 2,
					f14dt[1] + 105)); // X6/2 to X6/3
			g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2] / 2, f14dt[1]
					+ 105 + f14dt[3], f14dt[0] + 80 + f14dt[2] / 2,
					f14dt[1] + 155)); // X6/3 to Combining Box
			drawCurvedThing(g2d, f14l86[0], f14l86[1], f14l86[2]);
			g2d.drawString("L86", (int) f14l86[0] + 23, (int) f14l86[1] + 5);

			// L86 Frequency and lock states
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);

			drawMonitorPoint(g2d, f14l86[0] + 10, f14l86[1] - 20, circlesize); // Frequency
			g2d.drawString("Freq (GHz)", (int) f14l86[0] + 19,
					(int) f14l86[1] - 15);

			g2d.draw(new Line2D.Double(f14l86[0] + 10, f14l86[1] + 20,
					f14l86[0] + 10, f14l86[1] + 30));
			g2d.draw(new Line2D.Double(f14l86[0] + 10, f14l86[1] + 30,
					f14l86[0] + 30, f14l86[1] + 30));
			drawMonitorPoint(g2d, f14l86[0] + 30, f14l86[1] + 28, circlesize); // LOCK
			g2d.drawString("LOCK", (int) f14l86[0] + 38, (int) f14l86[1] + 34);

			drawMonPointBox(g2d, f14l86[0] + 19, f14l86[1] - 44, true, false); // Freq
			drawMonPointBox(g2d, f14l86[0] + 35, f14l86[1] + 40, true, false); // LOCK
			drawValues(g2d, "LO.sitesynth.freq", (int) f14l86[0] + 23,
					(int) f14l86[1] - 33, 3);
			drawValues(g2d, "LO.L86.PLLLock", (int) f14l86[0] + 38,
					(int) f14l86[1] + 52, 3);

			g2d.setColor(thisPurple);

			// Multibox
			g2d.draw(new Rectangle2D.Double(f14m[0], f14m[1], f14m[2], f14m[3]));
			g2d.draw(new Ellipse2D.Double(f14m[0] + 12, f14m[1] + 5,
					circlesize, circlesize));
			g2d.draw(new Ellipse2D.Double(f14m[0] + 12 + 35, f14m[1] + 5,
					circlesize, circlesize));
			g2d.draw(new Ellipse2D.Double(f14m[0] + 12 + 35 * 2, f14m[1] + 5,
					circlesize, circlesize));
			g2d.draw(new Ellipse2D.Double(f14m[0] + 12 + 35 * 3, f14m[1] + 5,
					circlesize, circlesize));
			g2d.draw(new Ellipse2D.Double(f14m[0] + 12 + 35 * 4, f14m[1] + 5,
					circlesize, circlesize));
			g2d.draw(new Ellipse2D.Double(f14m[0] + 12 + 35 * 5, f14m[1] + 5,
					circlesize, circlesize));

			g2d.draw(new Ellipse2D.Double(f14m[0] + f14m[2] / 2, f14m[1]
					+ f14m[3], circlesize, circlesize));

			// F14 ARROW
			itsF14RFSELA = Integer.parseInt(itsMonPointsHash
					.get("conversion.F14.RFSELA_M_raw"));
			itsF14RFSELB = Integer.parseInt(itsMonPointsHash
					.get("conversion.F14.RFSELB_M_raw"));
			drawF14RFSELA(g2d, itsF14RFSELA);
			drawF14RFSELB(g2d, itsF14RFSELB);

			// Attenuator
			g2d.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
					+ f14m[3] + 4, f14m[0] + f14m[2] / 2 + 2, f14m[1] + f14m[3]
					+ 25 + 10));

			drawAttenuator(g2d, (int) f14m[0] + (int) f14m[2] / 2 - 20,
					(int) f14m[1] + (int) f14m[3] + 35, 40, 40);

			// ///// C26 //////////
			g2d.drawString("C26", (int) c26[0] + 5, (int) c26[1] + (int) c26[3]
					- 5);
			drawDottedBox(g2d, thisPurple, (int) c26[0], (int) c26[1],
					(int) c26[2], (int) c26[3]);
			g2d.setStroke(semithickStroke);
			g2d.draw(new Line2D.Double(c26[0] + c26[2] / 2, c26[1], c26[0]
					+ c26[2] / 2, c26[1] + c26[3]));
			// Connecting Attenuator and C26
			g2d.draw(new Line2D.Double(f14m[0] + f14m[2] / 2, f14m[1] + f14m[3]
					+ 75, f14m[0] + f14m[2] / 2, f14dt[1] + 205));
			g2d.draw(new Line2D.Double(f14m[0] + f14m[2] / 2, f14dt[1] + 205,
					c26[0] + c26[2] / 2, f14dt[1] + 205));
			g2d.draw(new Line2D.Double(c26[0] + c26[2] / 2, f14dt[1] + 205,
					c26[0] + c26[2] / 2, c26[1]));
			// Connect C26 and C34
			g2d.draw(new Line2D.Double(c26[0] + c26[2] / 2, c26[1] + c26[3],
					c26[0] + c26[2] / 2, c26[1] + c26[3] + 30));
			g2d.draw(new Line2D.Double(c26[0] + c26[2] / 2, c26[1] + c26[3]
					+ 30, c34[0] + 14, c26[1] + c26[3] + 30));
			g2d.draw(new Line2D.Double(c34[0] + 14, c26[1] + c26[3] + 30,
					c34[0] + 14, c34[1] + 5));

			// C26 Monitor points
			int[] monC26RF = { 204, 338 };
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double(c26[0] + c26[2] / 2, c26[1] + 10, c26[0]
					+ c26[2] / 2 + 15, c26[1] + 10)); // RFA
			drawMonitorPoint(g2d, c26[0] + c26[2] / 2 + 15, c26[1] + 8,
					circlesize);
			g2d.drawString("RF (V)", monC26RF[0], monC26RF[1]);

			drawMonPointBox(g2d, monC26RF[0], monC26RF[1] + 5, true, true);
			drawMonPointBox(g2d, monC26RF[0], monC26RF[1] + 25, true, true);

			drawValues(g2d, "conversion.C26.RFA", monC26RF[0] + 5,
					monC26RF[1] + 16, 1);
			drawValues(g2d, "conversion.C26.RFB", monC26RF[0] + 5,
					monC26RF[1] + 37, 2);

			g.setColor(thisOrange);

			// F14 Mon points
			int[] F14A = { 424, 228 };
			g2d.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 - 25 + 45, f14m[1]
					+ f14m[3] + 25 + 25, f14m[0] + f14m[2] / 2 - 25 + 45 + 15,
					f14m[1] + f14m[3] + 25 + 25)); // Atten monpt
			drawMonitorPoint(g2d, f14m[0] + f14m[2] / 2 - 25 + 45 + 15, f14m[1]
					+ f14m[3] + 25 + 23, circlesize);
			g2d.drawString("ATTENUATOR (dB)", F14A[0], F14A[1]);

			drawMonPointBox(g2d, F14A[0], F14A[1] + 5, false, false);

			drawValues(g2d, "conversion.F14.ATTENA_M", F14A[0] + 20,
					F14A[1] + 17, 1);
			drawValues(g2d, "conversion.F14.ATTENB_M", F14A[0] + 20,
					F14A[1] + 37, 2);
			/*
			 * int[] monF14K = { (int) f14dt[0] + (int) f14dt[3] / 2 + 24, (int)
			 * f14dt[1] - 7 };
			 * 
			 * g2d.draw(new Line2D.Double(f14dt[0] + f14dt[3] / 2, f14dt[1],
			 * f14dt[0] + f14dt[3] / 2, f14dt[1] - 10)); // X2 g2d.draw(new
			 * Line2D.Double(f14dt[0] + f14dt[3] / 2, f14dt[1] - 10, f14dt[0] +
			 * f14dt[3] / 2 + 15, f14dt[1] - 10)); drawMonitorPoint(g2d,
			 * f14dt[0] + f14dt[3] / 2 + 15, f14dt[1] - 12, circlesize);
			 * g2d.drawString("K-LO (V)", monF14K[0], monF14K[1]);
			 * 
			 * drawMonPointBox(g2d, monF14K[0], monF14K[1] - 30, false, true);
			 * drawValues(g2d, "conversion.F14.K-LO_A", monF14K[0], monF14K[1] -
			 * 18, 1); drawValues(g2d, "conversion.F14.K-LO_B", monF14K[0] + 50,
			 * monF14K[1] - 18, 2);
			 * 
			 * g2d.setColor(thisOrange);
			 * 
			 * g2d.draw(new Line2D.Double(f14dt[0] + 30 + f14dt[3] / 2, f14dt[1]
			 * + 45, f14dt[0] + 30 + f14dt[3] / 2, f14dt[1] + 35)); // X3
			 * g2d.draw(new Line2D.Double(f14dt[0] + 30 + f14dt[3] / 2, f14dt[1]
			 * + 35, f14dt[0] + 30 + f14dt[3] / 2 + 15, f14dt[1] + 35));
			 * drawMonitorPoint(g2d, f14dt[0] + 30 + f14dt[3] / 2 + 15, f14dt[1]
			 * + 33, circlesize); g2d.drawString("W-50TRBL (V)", (int) f14dt[0]
			 * + 30 + (int) f14dt[3] / 2 + 24, (int) f14dt[1] + 38);
			 * 
			 * int[] F14W = { (int) f14dt[0] + 80 + (int) f14dt[2] + 24, (int)
			 * f14dt[1] + 43 + (int) f14dt[3] / 2 + 5 };
			 * 
			 * g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2], f14dt[1] +
			 * 45 + f14dt[3] / 2, f14dt[0] + 80 + f14dt[2] + 15, f14dt[1] + 45 +
			 * f14dt[3] / 2)); // X6/1 drawMonitorPoint(g2d, f14dt[0] + 80 +
			 * f14dt[2] + 15, f14dt[1] + 43 + f14dt[3] / 2, circlesize);
			 * g2d.drawString("W-100GDBL (V)", F14W[0], F14W[1]); g2d.draw(new
			 * Line2D.Double(f14dt[0] + 80 + f14dt[2], f14dt[1] + 75 + f14dt[3]
			 * / 2, f14dt[0] + 80 + f14dt[2] + 15, f14dt[1] + 75 + f14dt[3] /
			 * 2)); // X6/2 drawMonitorPoint(g2d, f14dt[0] + 80 + f14dt[2] + 15,
			 * f14dt[1] + 73 + f14dt[3] / 2, circlesize);
			 * g2d.drawString("W-50GDBL (V)", F14W[0], F14W[1] + 30);
			 * g2d.draw(new Line2D.Double(f14dt[0] + 80 + f14dt[2], f14dt[1] +
			 * 105 + f14dt[3] / 2, f14dt[0] + 80 + f14dt[2] + 15, f14dt[1] + 105
			 * + f14dt[3] / 2)); // X6/3 drawMonitorPoint(g2d, f14dt[0] + 80 +
			 * f14dt[2] + 15, f14dt[1] + 103 + f14dt[3] / 2, circlesize);
			 * g2d.drawString("W-25GDBL (V)", F14W[0], F14W[1] + 59);
			 * 
			 * drawMonPointBox(g2d, F14W[0], F14W[1] + 3, true, true);
			 * drawMonPointBox(g2d, F14W[0], F14W[1] + 30 + 3, true, true);
			 * drawMonPointBox(g2d, F14W[0], F14W[1] + 59 + 3, true, true);
			 * 
			 * drawValues(g2d, "conversion.F14.W-100GDBL", F14W[0], F14W[1] +
			 * 15, 3); drawValues(g2d, "conversion.F14.W-50GDBL", F14W[0],
			 * F14W[1] + 45, 3); drawValues(g2d, "conversion.F14.W-25GDBL",
			 * F14W[0], F14W[1] + 75, 3);
			 */
			/*
			 * drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20,
			 * (int)f14m[1]-2, circlesize); drawMonitorPoint(g2d,
			 * (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+10, circlesize);
			 * drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20,
			 * (int)f14m[1]+23, circlesize); drawMonitorPoint(g2d,
			 * (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+36, circlesize);
			 * drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20,
			 * (int)f14m[1]+49, circlesize); drawMonitorPoint(g2d,
			 * (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+62, circlesize);
			 * drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20,
			 * (int)f14m[1]+75, circlesize); g2d.draw(new
			 * Line2D.Double(f14m[0]+f14m[2]+24, f14m[1], f14m[0]+f14m[2]+24+15,
			 * f14m[1])); g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24,
			 * f14m[1]+12, f14m[0]+f14m[2]+24+15, f14m[1]+12)); g2d.draw(new
			 * Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+25,
			 * f14m[0]+f14m[2]+24+15, f14m[1]+25)); g2d.draw(new
			 * Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+38,
			 * f14m[0]+f14m[2]+24+15, f14m[1]+38)); g2d.draw(new
			 * Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+51,
			 * f14m[0]+f14m[2]+24+15, f14m[1]+51)); g2d.draw(new
			 * Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+64,
			 * f14m[0]+f14m[2]+24+15, f14m[1]+64)); g2d.draw(new
			 * Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+77,
			 * f14m[0]+f14m[2]+24+15, f14m[1]+77));arg1 g2d.drawString("K-DCOK",
			 * (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+5);
			 * g2d.drawString("Q-DCOK", (int)f14m[0]+(int)f14m[2]+44,
			 * (int)f14m[1]+12+5); g2d.drawString("W-DCOK",
			 * (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+25+5);
			 * g2d.drawString("IF-DCOK", (int)f14m[0]+(int)f14m[2]+44,
			 * (int)f14m[1]+38+5); g2d.drawString("K-LOSLEEP",
			 * (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+51+5);
			 * g2d.drawString("Q-LOSLEEP", (int)f14m[0]+(int)f14m[2]+44,
			 * (int)f14m[1]+64+5); g2d.drawString("W-LOSLEEP",
			 * (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+77+5);
			 */

			// ///// GREEN C21 /////////
			int[] c21d = { 815, 110, 150, 190 }; // Dotted box x, y, width,
													// height
			double[] c21r = { 910, 10, 30, 10, 30 }; // Receiver x, y, width,
														// height

			g2d.setColor(thisGreen);
			drawDottedBox(g2d, thisGreen, c21d[0], c21d[1], c21d[2], c21d[3]);
			g2d.drawString("C21", c21d[0] + 5, c21d[1] + c21d[3] - 5);
			g2d.setStroke(semithickStroke);
			drawReceiver(g2d, c21r[0], c21r[1], c21r[2], c21r[3]);
			drawAttenuator(g2d, (int) c21r[0] - 5, (int) c21r[1] + 50, 40, 40);
			g2d.draw(new Line2D.Double(c21r[0] + c21r[2] / 2, c21r[1] + c21r[4]
					+ 2, c21r[0] + c21r[2] / 2, c21r[1] + 50));
			g2d.setFont(receiverFont);
			g2d.drawString("16cm", (int) c21r[0] - 25, (int) c21r[1] + 35);
			g2d.setFont(normalFont);
			g2d.draw(new Line2D.Double(c21r[0] + c21r[2] / 2, c21r[1] + 90,
					c21r[0] + c21r[2] / 2, c21r[1] + 115));
			drawCrossedCircle(g2d, c21r[0], c21r[1] + 115, c21r[4]);
			g2d.draw(new Line2D.Double(c21r[0], c21r[1] + 130, c21r[0] - 65,
					c21r[1] + 130)); // Line to Curved thing
			g2d.draw(new Line2D.Double(c21r[0] + c21r[2] / 2,
					c21r[1] + 115 + 30, c21r[0] + c21r[2] / 2, c21r[1] + 390)); // Line(1-3)
																				// extending
																				// to
																				// C34
			g2d.draw(new Line2D.Double(c21r[0] + c21r[2] / 2, c21r[1] + 390,
					c34[0] + 39, c21r[1] + 390));
			g2d.draw(new Line2D.Double(c34[0] + 39, c21r[1] + 390, c34[0] + 39,
					c34[1] + 5));
			drawCurvedThing(g2d, c21r[0] - 85, c21r[1] + 120, 20);

			int[] monC21 = { 870, 132 };
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double(c21r[0] - 75, c21r[1] + 140,
					c21r[0] - 75, c21r[1] + 150));
			g2d.draw(new Line2D.Double(c21r[0] - 75, c21r[1] + 150,
					c21r[0] - 55, c21r[1] + 150));
			drawMonitorPoint(g2d, c21r[0] - 55, c21r[1] + 148, circlesize); // LOCK
			g2d.drawString("LOCK", monC21[0] - 5, monC21[1] + 32);
			drawMonitorPoint(g2d, (int) c21r[0] - 55, c21r[1] + 117, circlesize); // LO
			g2d.drawString("LO (V)", monC21[0] - 5, monC21[1] + 2);

			drawMonPointBox(g2d, monC21[0] - 10, monC21[1] - 26, true, false); // LO
			drawMonPointBox(g2d, monC21[0] - 5, monC21[1] + 35, true, false);

			drawValues(g2d, "conversion.C21.LO", monC21[0] - 10,
					monC21[1] - 14, 3);
			drawValues(g2d, "conversion.C21.LOCK", monC21[0] - 5,
					monC21[1] + 47, 3);

			int[] monC21RF = { (int) c21r[0] + (int) c21r[2] / 2 - 58,
					(int) c21r[1] + 210 };
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double(c21r[0] + c21r[2] / 2, c21r[1] + 205,
					(c21r[0] + c21r[2] / 2) - 15, c21r[1] + 205));
			drawMonitorPoint(g2d, c21r[0] + c21r[2] / 2 - 19, c21r[1] + 203,
					circlesize);
			g2d.drawString("RF (V)", monC21RF[0], monC21RF[1]);
			drawMonPointBox(g2d, monC21RF[0], monC21RF[1] + 5, false, false);
			drawValues(g2d, "conversion.C21.RFA", monC21RF[0] + 10,
					monC21RF[1] + 17, 1);
			drawValues(g2d, "conversion.C21.RFB", monC21RF[0] + 10,
					monC21RF[1] + 37, 2);
			
			//			drawAttenuator(g2d, (int) c21r[0] - 5, (int) c21r[1] + 50, 40, 40);
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double((int) c21r[0]+35, (int) c21r[1] + 70, (int) c21r[0]+50, (int) c21r[1] + 70));
			drawMonitorPoint(g2d, (int) c21r[0]+50, (int) c21r[1] + 70-2, circlesize);
			g2d.drawString("ATTEN (0-15 dB)", (int) c21r[0] +57, (int) c21r[1] + 75);
			drawMonPointBox(g2d, (int) c21r[0] +55, (int) c21r[1] + 80, false, false);
			drawValues(g2d, "receiver.LS.F15.AttenA", (int) c21r[0] +72, (int) c21r[1] + 91, 1);
			drawValues(g2d, "receiver.LS.F15.AttenB", (int) c21r[0] +72, (int) c21r[1] + 111, 2);

			

			// /////// BLUE C28 ////////
			int[] c28d = { 635, 110, 150, 190 }; // Dotted box x, y, width,
													// height
			double[] c28r = { 695, 10, 30, 10, 30 }; // Dotted box x, y, width,
														// height

			g2d.setColor(thisBlue);
			drawDottedBox(g2d, thisBlue, c28d[0], c28d[1], c28d[2], c28d[3]);
			g2d.drawString("C28", c28d[0] + 5, c28d[1] + c28d[3] - 5);
			g2d.setStroke(semithickStroke);
			drawReceiver(g2d, c28r[0], c28r[1], c28r[2], c28r[3]);
			drawAttenuator(g2d, (int) c28r[0] - 5, (int) c28r[1] + 50, 40, 40);
			g2d.draw(new Line2D.Double(c28r[0] - 5 + 20, c28r[1] + c28r[4] + 2,
					c28r[0] - 5 + 20, c28r[1] + 50));
			g2d.setFont(receiverFont);
			g2d.drawString("4cm", (int) c28r[0] - 15, (int) c28r[1] + 35);
			g2d.setFont(normalFont);
			g2d.draw(new Line2D.Double(c28r[0] - 5 + 20, c28r[1] + 90,
					c28r[0] - 5 + 20, c28r[1] + 340));
			g2d.draw(new Line2D.Double(c28r[0] - 5 + 20, c28r[1] + 340,
					c34[0] + 89, c28r[1] + 340));
			g2d.draw(new Line2D.Double(c34[0] + 89, c28r[1] + 340, c34[0] + 89,
					c34[1] + 5));

			int[] monC28RF = { (int) c28r[0] + (int) c28r[2] / 2 + 15 + 10,
					(int) c28r[1] + 153 + 5 };
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double(c28r[0] + c28r[2] / 2, c28r[1] + 155,
					c28r[0] + c28r[2] / 2 + 15, c28r[1] + 155));
			drawMonitorPoint(g2d, c28r[0] + c28r[2] / 2 + 15, c28r[1] + 153,
					circlesize);
			g2d.drawString("RF (V)", monC28RF[0], monC28RF[1]);
			drawMonPointBox(g2d, monC28RF[0], monC28RF[1] + 5, true, false);
			drawMonPointBox(g2d, monC28RF[0], monC28RF[1] + 20 + 5, true, false);

			drawValues(g2d, "conversion.C28.RFA", monC28RF[0] + 10,
					monC28RF[1] + 17, 1);
			drawValues(g2d, "conversion.C28.RFB", monC28RF[0] + 10,
					monC28RF[1] + 20 + 17, 2);
			
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double((int) c28r[0] +35, (int) c28r[1] + 70, (int) c28r[0] +50, (int) c28r[1] + 70));
			drawMonitorPoint(g2d, (int) c28r[0] +50, (int) c28r[1] + 70-2, circlesize);
			g2d.drawString("ATTEN (0-15 dB)", (int) c28r[0] +57, (int) c28r[1] + 75);
			drawMonPointBox(g2d, (int) c28r[0] +55, (int) c28r[1] + 80, false, false);
			drawValues(g2d, "receiver.CX.F16.AttenA", (int) c28r[0] +71, (int) c28r[1] + 91, 1);
			drawValues(g2d, "receiver.CX.F16.AttenB", (int) c28r[0] +71, (int) c28r[1] + 111, 2);

			
			// ///////// CC2 ////////////
			int[] cc2d = { 300, 550, 100, 30 };
			drawDottedBox(g2d, Color.black, cc2d[0], cc2d[1], cc2d[2], cc2d[3]);
			g2d.setStroke(semithickStroke);
			g2d.draw(new Line2D.Double(c34[0] + c34[2] / 2, c34[1] + c34[3]
					+ 35, c34[0] + c34[2] / 2, cc2d[1] + 10));
			g2d.draw(new Line2D.Double(c34[0] + c34[2] / 2, cc2d[1] + 10,
					cc2d[0] + 10, cc2d[1] + cc2d[3] - 10));
			g2d.draw(new Line2D.Double(c34[0] + c34[2] / 2, cc2d[1] + 10,
					cc2d[0] + cc2d[2] - 10, cc2d[1] + cc2d[3] - 10));

			g2d.draw(new Line2D.Double(cc2d[0] + 10, cc2d[1] + cc2d[3] - 10,
					cc2d[0] + 10, cc2d[1] + cc2d[3] + 10));
			g2d.draw(new Line2D.Double(cc2d[0] + cc2d[2] - 10, cc2d[1]
					+ cc2d[3] - 10, cc2d[0] + cc2d[2] - 10, cc2d[1] + cc2d[3]
					+ 10));
			g2d.draw(new Line2D.Double(cc2d[0] + 10, cc2d[1] + cc2d[3] + 10,
					c34d[0] - 5 + 25, cc2d[1] + cc2d[3] + 10));
			g2d.draw(new Line2D.Double(cc2d[0] + cc2d[2] - 10, cc2d[1]
					+ cc2d[3] + 10, c34d[0] + 160 - 5 + 25, cc2d[1] + cc2d[3]
					+ 10));

			g2d.draw(new Line2D.Double(c34d[0] - 5 + 25,
					cc2d[1] + cc2d[3] + 10, c34d[0] - 5 + 25, cc2d[1] + cc2d[3]
							+ 20));
			g2d.draw(new Line2D.Double(c34d[0] + 160 - 5 + 25, cc2d[1]
					+ cc2d[3] + 10, c34d[0] + 160 - 5 + 25, cc2d[1] + cc2d[3]
					+ 20));

			drawAttenuator(g2d, (int) c34d[0], cc2d[1] + cc2d[3] + 20, 40, 40);
			drawAttenuator(g2d, (int) c34d[0] + 160, cc2d[1] + cc2d[3] + 20,
					40, 40);

			g2d.draw(new Line2D.Double((int) c34d[0] + 20, cc2d[1] + cc2d[3]
					+ 20 + 40, (int) c34d[0] + 20, cc2d[1] + cc2d[3] + 80));
			g2d.draw(new Line2D.Double((int) c34d[0] + 160 + 20, cc2d[1]
					+ cc2d[3] + 20 + 40, (int) c34d[0] + 160 + 20, cc2d[1]
					+ cc2d[3] + 80));

			drawCrossedCircle(g2d, c34d[0] + 5, cc2d[1] + cc2d[3] + 80, 30);
			drawCurvedThing(g2d, c34d[0] -80, cc2d[1] + cc2d[3] + 85, 20);
			g2d.draw(new Line2D.Double(c34d[0] -60, cc2d[1] + cc2d[3] + 95, c34d[0] + 5, cc2d[1] + cc2d[3] + 95));
			drawCrossedCircle(g2d, c34d[0] + 165, cc2d[1] + cc2d[3] + 80, 30);
			drawCurvedThing(g2d, c34d[0] + 245, cc2d[1] + cc2d[3] + 85, 20);
			g2d.draw(new Line2D.Double(c34d[0] + 195, cc2d[1] + cc2d[3] + 95, c34d[0] + 245, cc2d[1] + cc2d[3] + 95));
			
			g2d.draw(new Line2D.Double((int) c34d[0] + 20, cc2d[1] + cc2d[3]
					+ 80 + 30, (int) c34d[0] + 20, cc2d[1] + cc2d[3] + 130));
			g2d.draw(new Line2D.Double((int) c34d[0] + 160 + 20, cc2d[1]
					+ cc2d[3] + 80 + 30, (int) c34d[0] + 160 + 20, cc2d[1]
					+ cc2d[3] + 130));

			g2d.draw(new Rectangle2D.Double(c34d[0] - 5, cc2d[1] + cc2d[3]
					+ 130, 50, 40));
			g2d.draw(new Rectangle2D.Double(c34d[0] + 160 - 5, cc2d[1]
					+ cc2d[3] + 130, 50, 40));
			g2d.drawString("Digitiser", c34d[0] - 3, cc2d[1] + cc2d[3] + 100
					+ 55);
			g2d.drawString("Digitiser", c34d[0] + 160 - 3, cc2d[1] + cc2d[3]
					+ 100 + 55);

			g2d.setColor(Color.RED);
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 - 15, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 - 15, cc2d[1]
					+ cc2d[3] + 230));
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 - 15 + 160, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 - 15 + 160,
					cc2d[1] + cc2d[3] + 230));
			g2d.setColor(Color.YELLOW);
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 - 5, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 - 5, cc2d[1]
					+ cc2d[3] + 230));
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 - 5 + 160, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 - 5 + 160, cc2d[1]
					+ cc2d[3] + 230));
			g2d.setColor(Color.GREEN);
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 + 5, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 + 5, cc2d[1]
					+ cc2d[3] + 230));
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 + 5 + 160, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 + 5 + 160, cc2d[1]
					+ cc2d[3] + 230));
			g2d.setColor(Color.BLUE);
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 + 15, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 + 15, cc2d[1]
					+ cc2d[3] + 230));
			g2d.draw(new Line2D.Double((int) c34d[0] + 20 + 15 + 160, cc2d[1]
					+ cc2d[3] + 130 + 41, (int) c34d[0] + 20 + 15 + 160,
					cc2d[1] + cc2d[3] + 230));

			g2d.setColor(Color.BLACK);
			g2d.draw(new Rectangle2D.Double(c34d[0] - 10, cc2d[1] + cc2d[3]
					+ 230, 60, 40));
			g2d.draw(new Rectangle2D.Double(c34d[0] + 160 - 10, cc2d[1]
					+ cc2d[3] + 230, 60, 40));
			g2d.drawString("Correlator", c34d[0] - 9, cc2d[1] + cc2d[3] + 200
					+ 55);
			g2d.drawString("Correlator", c34d[0] + 160 - 9, cc2d[1] + cc2d[3]
					+ 200 + 55);

			// Monpoints
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double((int) c34d[0], cc2d[1] + cc2d[3] + 20
					+ 20, (int) c34d[0] - 15, cc2d[1] + cc2d[3] + 40));
			g2d.draw(new Line2D.Double((int) c34d[0] + 200, cc2d[1]
					+ cc2d[3] + 40, (int) c34d[0] + 215, cc2d[1]
					+ cc2d[3] + 40));
			drawMonitorPoint(g2d, (int) c34d[0] - 19, cc2d[1] + cc2d[3]
					+ 38, circlesize);
			drawMonitorPoint(g2d, (int) c34d[0] + 215, cc2d[1]
					+ cc2d[3] + 20 + 20 - 2, circlesize);
			g2d.drawString("ATTEN (0-15 dB)", (int) c34d[0] - 125, cc2d[1]
					+ cc2d[3] + 45);
			g2d.drawString("ATTEN (0-15 dB)", (int) c34d[0] + 225, cc2d[1] + cc2d[3] + 45);
			drawMonPointBox(g2d, (int) c34d[0]- 125, cc2d[1] + cc2d[3]
					+ 50, false, true);
			drawMonPointBox(g2d, (int) c34d[0]+ 225, cc2d[1]
					+ cc2d[3] + 50, false, true);
			drawValues(g2d, "cabb.cl1f1.AttenA", (int) c34d[0]- 110, cc2d[1] + cc2d[3]
					+ 62, 1);
			drawValues(g2d, "cabb.cl1f1.AttenB", (int) c34d[0]- 55, cc2d[1] + cc2d[3]
					+ 62, 2);
			drawValues(g2d, "cabb.cl1f2.AttenA", (int) c34d[0]+240, cc2d[1] + cc2d[3]
					+ 62, 1);
			drawValues(g2d, "cabb.cl1f2.AttenB", (int) c34d[0]+290, cc2d[1] + cc2d[3]
					+ 62, 2);
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.drawString("Optical Power (F1)", (int) c34d[0] - 115,
					cc2d[1] + cc2d[3] + 150);
			g2d.drawString("Optical Power (F2)", (int) c34d[0] + 13 + 200, cc2d[1]
					+ cc2d[3] + 150);
			drawMonitorPoint(g2d, (int) c34d[0] + 20 - 15 - 130, cc2d[1]
					+ cc2d[3] + 130 + 40 + 5 - 20 + 5, circlesize);
			g2d.drawString("R/0", (int) c34d[0] + 20 - 15 - 130 - 25, cc2d[1]
					+ cc2d[3] + 130 + 40 + 5 - 20 + 12);
			drawMonitorPoint(g2d, (int) c34d[0] + 20 - 15 - 130, cc2d[1]
					+ cc2d[3] + 130 + 40 + 17 - 7, circlesize);
			g2d.drawString("Y/1", (int) c34d[0] + 20 - 15 - 130 - 25, cc2d[1]
					+ cc2d[3] + 130 + 40 + 30 - 13);
			drawMonitorPoint(g2d, (int) c34d[0] + 20 - 15 - 130, cc2d[1]
					+ cc2d[3] + 130 + 40 + 36 - 5, circlesize);
			g2d.drawString("G/2", (int) c34d[0] + 20 - 15 - 130 - 25, cc2d[1]
					+ cc2d[3] + 130 + 40 + 43 - 5);
			drawMonitorPoint(g2d, (int) c34d[0] + 20 - 15 - 130, cc2d[1]
					+ cc2d[3] + 130 + 40 + 49 + 2, circlesize);
			g2d.drawString("3/B", (int) c34d[0] + 20 - 15 - 130 - 25, cc2d[1]
					+ cc2d[3] + 130 + 40 + 56 + 2);
			// F2
			drawMonitorPoint(g2d, (int) c34d[0] + 317, cc2d[1] + cc2d[3] + 130
					+ 40 + 5 - 20 + 5, circlesize);
			g2d.drawString("R/0", (int) c34d[0] + 324, cc2d[1] + cc2d[3] + 130
					+ 40 + 5 - 20 + 12);
			drawMonitorPoint(g2d, (int) c34d[0] + 317, cc2d[1] + cc2d[3] + 130
					+ 40 + 17 - 7, circlesize);
			g2d.drawString("Y/1", (int) c34d[0] + 324, cc2d[1] + cc2d[3] + 130
					+ 40 + 30 - 13);
			drawMonitorPoint(g2d, (int) c34d[0] + 317, cc2d[1] + cc2d[3] + 130
					+ 40 + 36 - 5, circlesize);
			g2d.drawString("G/2", (int) c34d[0] + 324, cc2d[1] + cc2d[3] + 130
					+ 40 + 43 - 5);
			drawMonitorPoint(g2d, (int) c34d[0] + 317, cc2d[1] + cc2d[3] + 130
					+ 40 + 49 + 2, circlesize);
			g2d.drawString("3/B", (int) c34d[0] + 324, cc2d[1] + cc2d[3] + 130
					+ 40 + 56 + 2);

			drawMonPointBox(g2d, (int) c34d[0] - 115, cc2d[1]
					+ cc2d[3] + 155, false, true);
			drawMonPointBox(g2d, (int) c34d[0] - 115, cc2d[1]
					+ cc2d[3] + 175, false, true);
			drawMonPointBox(g2d, (int) c34d[0] - 115, cc2d[1]
					+ cc2d[3] + 195, false, true);
			drawMonPointBox(g2d, (int) c34d[0] - 115, cc2d[1]
					+ cc2d[3] + 215, false, true);
			drawValues(g2d, "cabb.data_links.OptPow-1A-0R",
					(int) c34d[0] - 110, cc2d[1] + cc2d[3] + 167, 1);
			drawValues(g2d, "cabb.data_links.OptPow-1B-0R",
					(int) c34d[0] - 60, cc2d[1] + cc2d[3] + 167, 2);
			drawValues(g2d, "cabb.data_links.OptPow-1A-1Y",
					(int) c34d[0] - 110, cc2d[1]+ cc2d[3] + 187, 1);
			drawValues(g2d, "cabb.data_links.OptPow-1B-1Y",
					(int) c34d[0] - 60, cc2d[1] + cc2d[3] + 187, 2);
			drawValues(g2d, "cabb.data_links.OptPow-1A-2G",
					(int) c34d[0] - 110, cc2d[1] + cc2d[3] + 207, 1);
			drawValues(g2d, "cabb.data_links.OptPow-1B-2G",
					(int) c34d[0] - 60, cc2d[1] + cc2d[3] + 207, 2);
			drawValues(g2d, "cabb.data_links.OptPow-1A-3B",
					(int) c34d[0] - 110, cc2d[1] + cc2d[3] + 227, 1);
			drawValues(g2d, "cabb.data_links.OptPow-1B-3B",
					(int) c34d[0] - 60, cc2d[1] + cc2d[3] + 227, 2);
			drawMonPointBox(g2d, (int) c34d[0] + 215, cc2d[1] + cc2d[3]
					+ 155, false, true);
			drawMonPointBox(g2d, (int) c34d[0] + 215, cc2d[1] + cc2d[3]
					+ 175, false, true);
			drawMonPointBox(g2d, (int) c34d[0] + 215, cc2d[1] + cc2d[3]
					+ 195, false, true);
			drawMonPointBox(g2d, (int) c34d[0] + 215, cc2d[1] + cc2d[3]
					+ 215, false, true);
			drawValues(g2d, "cabb.data_links.OptPow-2A-0R",
					(int) c34d[0] +220, cc2d[1] + cc2d[3] + 167, 1);
			drawValues(g2d, "cabb.data_links.OptPow-2B-0R",
					(int) c34d[0] +270, cc2d[1] + cc2d[3] + 167, 2);
			drawValues(g2d, "cabb.data_links.OptPow-2A-1Y",
					(int) c34d[0] +220, cc2d[1]+ cc2d[3] + 187, 1);
			drawValues(g2d, "cabb.data_links.OptPow-2B-1Y",
					(int) c34d[0] +270, cc2d[1] + cc2d[3] + 187, 2);
			drawValues(g2d, "cabb.data_links.OptPow-2A-2G",
					(int) c34d[0] +220, cc2d[1] + cc2d[3] + 207, 1);
			drawValues(g2d, "cabb.data_links.OptPow-2B-2G",
					(int) c34d[0] +270, cc2d[1] + cc2d[3] + 207, 2);
			drawValues(g2d, "cabb.data_links.OptPow-2A-3B",
					(int) c34d[0] +220, cc2d[1] + cc2d[3] + 227, 1);
			drawValues(g2d, "cabb.data_links.OptPow-2B-3B",
					(int) c34d[0] +270, cc2d[1] + cc2d[3] + 227, 2);
			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double((int) c34d[0] - 10, cc2d[1] + cc2d[3]
					+ 230 + 20, (int) c34d[0] - 10 - 15, cc2d[1] + cc2d[3]
					+ 230 + 20));
			g2d.draw(new Line2D.Double((int) c34d[0] + 10 + 160 + 40, cc2d[1]
					+ cc2d[3] + 230 + 20, (int) c34d[0] + 160 + 10 + 40 + 15,
					cc2d[1] + cc2d[3] + 230 + 20));
			drawMonitorPoint(g2d, (int) c34d[0] - 10 - 15 - 4, cc2d[1]
					+ cc2d[3] + 230 + 20 - 2, circlesize);
			drawMonitorPoint(g2d, (int) c34d[0] + 160 + 40 + 10 + 15, cc2d[1]
					+ cc2d[3] + 230 + 20 - 2, circlesize);
			g2d.drawString("RMS-1", (int) c34d[0] - 10 - 15 - 50, cc2d[1]
					+ cc2d[3] + 230 + 20 + 5);
			g2d.drawString("RMS-2", (int) c34d[0] + 160 + 40 + 10 + 15 + 10,
					cc2d[1] + cc2d[3] + 230 + 20 + 5);
			drawMonPointBox(g2d, (int) c34d[0] - 10 - 15 - 100, cc2d[1]
					+ cc2d[3] + 230 + 20 + 5 + 5, false, true);
			drawMonPointBox(g2d, (int) c34d[0] + 160 + 40 + 15 + 20, cc2d[1]
					+ cc2d[3] + 230 + 20 + 5 + 5, false, true);
			drawValues(g2d, "cabb.correlator.RMS-1A", (int) c34d[0] - 10 - 15
					- 100 + 5, cc2d[1] + cc2d[3] + 230 + 20 + 5 + 17, 1);
			drawValues(g2d, "cabb.correlator.RMS-1B", (int) c34d[0] - 10 - 15
					- 100 + 55, cc2d[1] + cc2d[3] + 230 + 20 + 5 + 17, 2);
			drawValues(g2d, "cabb.correlator.RMS-2A", (int) c34d[0] + 160 + 40
					+ 15 + 20 + 5, cc2d[1] + cc2d[3] + 230 + 20 + 5 + 17, 1);
			drawValues(g2d, "cabb.correlator.RMS-2B", (int) c34d[0] + 160 + 40
					+ 15 + 20 + 55, cc2d[1] + cc2d[3] + 230 + 20 + 5 + 17, 2);

			g2d.setColor(thisOrange);
			g2d.setStroke(thickStroke);
			g2d.draw(new Line2D.Double(c34d[0] -70, cc2d[1] + cc2d[3] + 105, c34d[0] -70, cc2d[1] + cc2d[3] + 115));
			g2d.draw(new Line2D.Double(c34d[0] +255, cc2d[1] + cc2d[3] + 105, c34d[0] +255, cc2d[1] + cc2d[3] + 115));
			g2d.draw(new Line2D.Double(c34d[0] -70, cc2d[1] + cc2d[3] + 115, c34d[0] -90, cc2d[1] + cc2d[3] + 115));
			g2d.draw(new Line2D.Double(c34d[0] +255, cc2d[1] + cc2d[3] + 115, c34d[0] +275, cc2d[1] + cc2d[3] + 115));
			drawMonitorPoint(g2d, c34d[0] -94, cc2d[1] + cc2d[3] + 113, circlesize);
			drawMonitorPoint(g2d, c34d[0] +275, cc2d[1] + cc2d[3] + 113, circlesize);
			g2d.drawString("LOCK", c34d[0] -130, cc2d[1] + cc2d[3] + 105);
			g2d.drawString("LOCK", c34d[0] +284, cc2d[1] + cc2d[3] + 105);
			drawMonPointBox(g2d, c34d[0] -145, cc2d[1] + cc2d[3] + 110, true, true);
			drawMonPointBox(g2d, c34d[0] +285, cc2d[1] + cc2d[3] + 110, true, true);
			drawValues(g2d, "cabb.cl1f1.PLLLock", c34d[0] -145, cc2d[1] + cc2d[3] + 122, 3);
			drawValues(g2d, "cabb.cl1f2.PLLLock", c34d[0] +285, cc2d[1] + cc2d[3] + 122, 3);
			
			// ///////////////////////////////////
			// Antenna label
			g2d.setColor(Color.black);
			Font antennaFont = new Font("Helvetica", Font.PLAIN, 28);
			g2d.setFont(antennaFont);
			String antennastring = "Antenna ".concat(focusAntenna);
			g2d.drawString(antennastring, 790, 480);

			// Key
			// g2d.drawRect(800, 520, 100, 47);
			g2d.setFont(new Font("Verdana", Font.PLAIN, 12));
			g2d.drawString("Key :", 790, 535);
			drawMonPointBox(g2d, 825, 525, false, false);
			g2d.setColor(Color.red);
			g2d.drawString("Pol A", 830, 537);
			g2d.setColor(Color.blue);
			g2d.drawString("Pol B", 830, 558);
		}

		void drawF14RFSELA(Graphics2D g, int position) {
			g.setColor(Color.RED);
			double[] f14m = { 280, 125, 200, 50 };
			double[] F14xpos = { f14m[0] + 12, f14m[0] + 12 + 35,
					f14m[0] + 12 + 35 * 2, f14m[0] + 12 + 35 * 3,
					f14m[0] + 12 + 35 * 4, f14m[0] + 12 + 35 * 5 };
			double F14ypos = f14m[1] + 14;

			switch (position) {
			case 0:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[0] + 2, F14ypos));
				drawArrowHead(g, F14xpos[0] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 1:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[1] + 2, F14ypos));
				drawArrowHead(g, F14xpos[1] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 2:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[2] + 2, F14ypos));
				drawArrowHead(g, F14xpos[2] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 3:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[3] + 2, F14ypos));
				drawArrowHead(g, F14xpos[3] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 4:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[4] + 2, F14ypos));
				drawArrowHead(g, F14xpos[4] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 5:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[5] + 2, F14ypos));
				drawArrowHead(g, F14xpos[5] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			}
		}

		void drawF14RFSELB(Graphics2D g, int position) {
			g.setColor(Color.BLUE);
			double[] f14m = { 280, 125, 200, 50 };
			double[] F14xpos = { f14m[0] + 12 + 45, f14m[0] + 12 + 35 + 27.5,
					f14m[0] + 12 + 35 * 2 + 10, f14m[0] + 12 + 35 * 3 - 7.5,
					f14m[0] + 12 + 35 * 4 - 25, f14m[0] + 12 + 35 * 5 - 42.5 };

			double F14ypos = f14m[1] + 32;

			switch (position) {
			case 0:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[0] + 2, F14ypos));
				drawArrowHead(g, F14xpos[0] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 1:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[1] + 2, F14ypos));
				drawArrowHead(g, F14xpos[1] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 2:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[2] + 2, F14ypos));
				drawArrowHead(g, F14xpos[2] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 3:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[3] + 2, F14ypos));
				drawArrowHead(g, F14xpos[3] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 4:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[4] + 2, F14ypos));
				drawArrowHead(g, F14xpos[4] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			case 5:
				g.draw(new Line2D.Double(f14m[0] + f14m[2] / 2 + 2, f14m[1]
						+ f14m[3], F14xpos[5] + 2, F14ypos));
				drawArrowHead(g, F14xpos[5] + 2, F14ypos, f14m[0] + f14m[2] / 2
						+ 2, f14m[1] + f14m[3]);
				break;
			}

			g.setColor(thisPurple);
		}

		void drawC34RFA(Graphics2D g, int position) {
			g.setColor(Color.RED);

			// double[] c34 = { 300, 435, 100, 30 };
			double[] C34RF = { 350, 465, 448, 300 }; // base x, base y, top y

			switch (position) {
			case 0:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RF[3] + 12,
						C34RF[2]));
				drawArrowHead(g, C34RF[3] + 12, C34RF[2], C34RF[0], C34RF[1]);
				break;
			case 1:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RF[3] + 37,
						C34RF[2]));
				drawArrowHead(g, C34RF[3] + 37, C34RF[2], C34RF[0], C34RF[1]);
				break;
			case 2:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RF[3] + 62,
						C34RF[2]));
				drawArrowHead(g, C34RF[3] + 62, C34RF[2], C34RF[0], C34RF[1]);
				break;
			case 3:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RF[3] + 87,
						C34RF[2]));
				drawArrowHead(g, C34RF[3] + 87, C34RF[2], C34RF[0], C34RF[1]);
				break;
			}

			g.setColor(thisPurple);
		}

		void drawC34RFB(Graphics2D g, int position) {
			g.setColor(Color.BLUE);

			double[] C34RF = { 350, 465, 456.5 }; // base x, base y, top y
			double[] C34RFx = { C34RF[0] - 19, C34RF[0] - 6.5, C34RF[0] + 6,
					C34RF[0] + 18.5 }; // X-Pos 0-4

			switch (position) {
			case 0:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RFx[0],
						C34RF[2]));
				drawArrowHead(g, C34RFx[0], C34RF[2], C34RF[0], C34RF[1]);
				break;
			case 1:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RFx[1],
						C34RF[2]));
				drawArrowHead(g, C34RFx[1], C34RF[2], C34RF[0], C34RF[1]);
				break;
			case 2:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RFx[2],
						C34RF[2]));
				drawArrowHead(g, C34RFx[2], C34RF[2], C34RF[0], C34RF[1]);
				break;
			case 3:
				g.draw(new Line2D.Double(C34RF[0], C34RF[1], C34RFx[3],
						C34RF[2]));
				drawArrowHead(g, C34RFx[3], C34RF[2], C34RF[0], C34RF[1]);
				break;
			}

			g.setColor(thisPurple);
		}

		void drawMonPointBox(Graphics2D g, double x, double y, boolean single,
				boolean sidebyside) {
			double width = 45;
			double height = 15;

			g.setColor(thisOrange);
			g.drawRect((int) x, (int) y, (int) width, (int) height);
			g.setColor(thisYellow);
			g.fillRect((int) x, (int) y, (int) width, (int) height);
			if (!single) {
				if (sidebyside) { // Place boxes side by side
					g.setColor(thisOrange);
					g.drawRect((int) x + (int) width + 5, (int) y, (int) width,
							(int) height);
					g.setColor(thisYellow);
					g.fillRect((int) x + (int) width + 5, (int) y, (int) width,
							(int) height);
				} else { // Stack pols
					g.setColor(thisOrange);
					g.drawRect((int) x, (int) y + (int) height + 5,
							(int) width, (int) height);
					g.setColor(thisYellow);
					g.fillRect((int) x, (int) y + (int) height + 5,
							(int) width, (int) height);
				}
			}
		}

		void drawValues(Graphics2D g, String point, int x, int y, int color) {
			switch (color) {
			case 1:
				g.setColor(Color.RED); // Polarisation A
				break;
			case 2:
				g.setColor(Color.BLUE); // Polarisation B
				break;
			case 3:
				g.setColor(Color.MAGENTA); // No polarisation
				break;
			}

			g.drawString(itsMonPointsHash.get(point), x, y);
		}

		void drawCrossedCircle(Graphics2D g, double x1, double y1,
				double diameter) {
			double line1x1 = x1 + 5;
			double line1y1 = y1 + 5;
			double line1x2 = line1x1 + 20;
			double line1y2 = line1y1 + 20;

			g.draw(new Ellipse2D.Double(x1, y1, diameter, diameter)); // Crossed
																		// thing
			g.draw(new Line2D.Double(line1x1, line1y1, line1x2, line1y2));
			g.draw(new Line2D.Double(line1x2, line1y1, line1x1, line1y2));
		}

		void drawMonitorPoint(Graphics2D g, double x1, double y1,
				double diameter) {
			g.setColor(thisOrange);
			g.setStroke(thickStroke);
			g.draw(new Ellipse2D.Double(x1, y1, diameter, diameter));
		}

		void drawDottedBox(Graphics2D g, Color color, int x1, int y1,
				int width, int height) {
			g.setColor(color);
			g.setStroke(dottedStroke);
			g.drawRect(x1, y1, width, height);
		}

		void drawReceiver(Graphics2D g, double x1, double y1, double width,
				double height) {
			double line2x1 = x1 + 30;
			double linex2 = x1 + 15;
			double liney1 = y1 + 5;
			double liney2 = y1 + 35;

			g.draw(new Ellipse2D.Double(x1, y1, width, height)); // Receiver
			g.draw(new Line2D.Double(x1, liney1, linex2, liney2));
			g.draw(new Line2D.Double(line2x1, liney1, linex2, liney2));
		}

		void drawCurvedThing(Graphics2D g, double x1, double y1, double diameter) {
			g.draw(new Ellipse2D.Double(x1, y1, diameter, diameter));
			CubicCurve2D c = new CubicCurve2D.Double();
			c.setCurve(x1, y1 + 10, x1 + 8, y1, x1 + 12, y1 + 20, x1 + 20,
					y1 + 10);
			g.draw(c);

		}

		void drawAttenuator(Graphics2D g, int x, int y, int width, int height) {
			g.drawRect(x, y, width, height);
			double thisX = x + 5;
			double[] theseY = { y + 20, y + 15 };
			double[] zigzag = { thisX + 3, thisX + 3 + 4, thisX + 3 + 4 * 2,
					thisX + 3 + 4 * 3, thisX + 3 + 4 * 4, thisX + 3 + 4 * 5,
					thisX + 3 + 4 * 6 };
			g.draw(new Line2D.Double(zigzag[0], theseY[0], zigzag[1], theseY[1]));
			g.draw(new Line2D.Double(zigzag[1], theseY[1], zigzag[2], theseY[0]));
			g.draw(new Line2D.Double(zigzag[2], theseY[0], zigzag[3], theseY[1]));
			g.draw(new Line2D.Double(zigzag[3], theseY[1], zigzag[4], theseY[0]));
			g.draw(new Line2D.Double(zigzag[4], theseY[0], zigzag[5], theseY[1]));
			g.draw(new Line2D.Double(zigzag[5], theseY[1], zigzag[6], theseY[0]));
		}

		void drawArrowHead(Graphics2D g, double x1, double y1, double x2,
				double y2) {
			double a = 0;
			double b = 0;
			double c = 0;
			double d = 0;
			double length = 10.0;
			double theta = Math.atan2((y2 - y1), (x2 - x1));

			double costheta = Math.cos(theta - Math.PI / 6);
			double sintheta = Math.sin(theta - Math.PI / 6);

			if (y1 < y2) { // Arrow pointing up
				a = x1 + length * costheta;
				b = y1 + length * sintheta;

			} else if (y1 > y2) { // Arrow pointing down
				costheta = costheta * (-1);
				sintheta = sintheta * (-1);
				a = x1 - length * costheta;
				b = y1 - length * sintheta;
			}
			g.draw(new Line2D.Double(a, b, x1, y1));

			costheta = Math.cos(theta + Math.PI / 6);
			sintheta = Math.sin(theta + Math.PI / 6);

			if (y1 < y2) { // Arrow pointing up
				c = x1 + length * costheta;
				d = y1 + length * sintheta;

			} else if (y1 > y2) { // Arrow pointing down
				costheta = costheta * (-1);
				sintheta = sintheta * (-1);
				c = x1 - length * costheta;
				d = y1 - length * sintheta;
			}
			g.draw(new Line2D.Double(c, d, x1, y1));
		}

	}

	// /// END NESTED CLASS /////

	public class DataListener implements PointListener {
		/** The name of the monitor point we are subscribed to. */
		String itsPoint;

		DataListener(String point) {
			itsPoint = point;
			DataMaintainer.subscribe(itsPoint, this);
			// System.out.println("DataListener: Subscribed to " + itsPoint);
		}

		public void onPointEvent(Object source, final PointEvent evt) {
			// System.out.println("ConversionDiagram: onPointEvent" + source);
			Runnable newdata = new Runnable() {
				public void run() {
					try {
						PointData pd = evt.getPointData();

						// System.out.println("New data value for  " +
						// pd.getName() + " is " + pd.getData());

						itsMonPointsHash.put(pd.getNameOnly(), pd.getData()
								.toString());

						validate();
						repaint();
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

		public void unsubscribe() {
			DataMaintainer.unsubscribe(itsPoint, this);
		}
	}

	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;

	/** Constructor. */
	public ConversionDiagram() {
		JPanel jp = new JPanel();
		jp.setSize(new Dimension(800, 600));
		jp.add(new PaintDiagram());

		add(jp);
	}

	/**
	 * Configure this MonPanel to use the specified setup.
	 * 
	 * @param setup
	 *            class-specific setup information.
	 * @return <tt>true</tt> if setup could be parsed or <tt>false</tt> if there
	 *         was a problem and the setup cannot be used.
	 */
	public boolean loadSetup(SavedSetup setup) {
		try {
			resetListeners();

			String thisAntenna = setup.get("focus");
			focusAntenna = thisAntenna;

			for (int i = 0; i < itsMonPoints.length; i++) {
				if (itsMonPoints[i].equals("LO.sitesynth.freq")) {
					DataListener currentListener = new DataListener("site"
							+ "." + itsMonPoints[i]);
					itsDataListeners.add(currentListener);
				} else if (itsMonPoints[i].startsWith("cabb.cl1f")) {
					//System.out.println("Starts with cabb.cl1f");
					DataListener currentListener = new DataListener("cacscc"
							+ focusAntenna + "." + itsMonPoints[i]);
					itsDataListeners.add(currentListener);
				} else {
					DataListener currentListener = new DataListener("ca0"
							+ focusAntenna + "." + itsMonPoints[i]);
					itsDataListeners.add(currentListener);
				}

			}

			validate();
			repaint();

		} catch (Exception e) {
			itsSetup = null;
			System.err.println("ConversionDiagram:loadSetup: "
					+ e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		itsSetup = setup;
		return true;
	}

	public synchronized SavedSetup getSetup() {
		// System.out.println("ConversionDiagram: getSetup");
		return itsSetup;
	}

	public void vaporise() {
	}

	public void export(PrintStream p) {
	}

	public String getLabel() {
		return "Conversion Diagram";
	}

	public MonPanelSetupPanel getControls() {
		return new ConversionDiagramSetupPanel(this, itsFrame);
	}

	public void resetListeners() {
		Iterator<DataListener> i = itsDataListeners.iterator();
		while (i.hasNext()) {
			i.next().unsubscribe();
		}
		itsDataListeners.clear();
	}

}
