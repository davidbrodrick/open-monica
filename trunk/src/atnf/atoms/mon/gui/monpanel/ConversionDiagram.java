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
import java.awt.geom.*;
import static java.awt.geom.AffineTransform.*;


import javax.swing.*;

import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;

/**
 * Class representing a conversion diagram.
 * 
 * @author Camille Nicodemus
 * @see MonPanel
 */
public class ConversionDiagram extends MonPanel {
	protected String focusAntenna = "";
	
	static {
		MonPanel.registerMonPanel("Conversion Diagram", ConversionDiagram.class);
	}

	///// NESTED CLASS: CONVERSION DIAGRAM SETUP PANEL /////
	public class ConversionDiagramSetupPanel extends MonPanelSetupPanel implements ActionListener {
		/** The main panel which hold our GUI controls. */
		protected JPanel itsSetupPanel = new JPanel();

		public ConversionDiagramSetupPanel(ConversionDiagram panel, JFrame frame) {
			super(panel, frame);
			
			JLabel temp = new JLabel("Select which antenna you wish to use: ");
			String[] antennae = { "1", "2", "3,", "4", "5", "6" };
			JComboBox antennaCombo = new JComboBox(antennae);
			antennaCombo.addActionListener(this);			
			
			itsSetupPanel.add(temp);
			itsSetupPanel.add(antennaCombo);
			add(itsSetupPanel);
		}

		protected SavedSetup getSetup() {
            SavedSetup setup = new SavedSetup("temp", "atnf.atoms.mon.gui.monpanel.ConversionDiagram");
            
            return setup;
		}

		protected void showSetup(SavedSetup setup) {
		}
		
		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox)e.getSource();
			focusAntenna = (String)cb.getSelectedItem();
			System.out.println("ConversionDiagramSetupPanel:actionPerformed: focusAntenna is " + focusAntenna);
		}
	}	
	
	///// END NESTED CLASS /////
	
	///// NESTED CLASS: CONVERSION DIAGRAM /////
	// Class that contains methods for drawing the conversion diagram onscreen
	private class PaintDiagram extends JPanel {
        private Color thisGreen = new Color(85, 187, 102); // Green
        private Color thisOrange = new Color(255, 119, 034); // Orange
        private Color thisPurple = new Color(119, 034, 136); // Purple
        private Color thisBlue = new Color(034, 170, 255); // Blue
        
        private Stroke dottedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        private Stroke plainStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, null, 0);
        private Stroke thickStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, null, 0);
        private Stroke semithickStroke = new BasicStroke((float)1.5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, null, 0);
		private final double circlesize = 4;
        
		public PaintDiagram() {
		}
		
		public Dimension getPreferredSize() {
	        return new Dimension(1000,750);
	    }
		
	    public void paintComponent(Graphics g) {
	        super.paintComponent(g);      
	        Graphics2D g2d = (Graphics2D) g;

	        // BLACK C34
	        int[] c34d = {250, 500, 200, 100}; // x, y, width, height
	        double[] c34 = {300, 535, 100, 30}; // x, y, width, height
	        
	        g2d.setColor(Color.black);
	        drawDottedBox(g2d, Color.black, c34d[0], c34d[1], c34d[2], c34d[3]);
	        g2d.drawString("C34", c34d[0]+5, c34d[1]+c34d[3]-5);
	        g2d.setStroke(semithickStroke);
	        // C34 contents
	        g2d.draw(new Rectangle2D.Double(c34[0], c34[1], c34[2], c34[3]));
	        g2d.draw(new Line2D.Double(c34[0]+c34[2]/2, c34[1]+c34[3], c34[0]+c34[2]/2, c34[1]+c34[3]+35));	// Arrow pointing down
	        g2d.draw(new Ellipse2D.Double(c34[0]+c34[2]/2-2, c34[1]+c34[3], circlesize, circlesize));		// Circle joining dynamic + pointing down arrow
	        g2d.draw(new Line2D.Double(c34[0]+c34[2]/2, c34[1]+c34[3], c34[0]+39, c34[1]+13));// DYNAMIC ARROW
	        
	        drawArrowHead(g2d, c34[0]+c34[2]/2, c34[1]+c34[3]+35, c34[0]+c34[2]/2, c34[1]+c34[3]); // Arrow pointing down
	        drawArrowHead(g2d, c34[0]+39, c34[1]+13, c34[0]+c34[2]/2, c34[1]+c34[3]); // Dynamic arrow
	        
	        // Small circles
	        g2d.setColor(thisPurple);
	        g2d.draw(new Ellipse2D.Double(c34[0]+12, c34[1]+5, circlesize, circlesize));
	        g2d.setColor(thisGreen);
	        g2d.draw(new Ellipse2D.Double(c34[0]+37, c34[1]+5, circlesize, circlesize));
	        g2d.setColor(thisBlue);
	        g2d.draw(new Ellipse2D.Double(c34[0]+62, c34[1]+5, circlesize, circlesize));
	        g2d.draw(new Ellipse2D.Double(c34[0]+87, c34[1]+5, circlesize, circlesize));
	        //TODO: Refactor monitor points
	        g2d.setColor(thisOrange);
	        drawMonitorPoint(g2d, 320, 578, 4);	// LO
	        g2d.draw(new Line2D.Double(350, 580, 323, 580));
	        g2d.drawString("RFA", 295, 585);
	        
	        // PURPLE F14
	        int[] f14d = {25, 50, 580, 250}; // x, y, width, height
	        double[] f14r = {300, 10, 30, 10, 30}; // Receiver x, y, width, height, crossed circle diameter
	        double[] f14dt = {30, 80, 30, 15}; // Leftmost doubler x, y, width, height
	        double[] f14l86 = {0, f14dt[1]+155, 20}; // L86 x, y, diameter
	        double[] f14m = {280, 125, 200, 50}; // Multibox x, y, width, height
	        double[] c26 = {130, 325, 100, 70}; // C26 x, y, width, height
	        
	        g2d.setColor(thisPurple);
	        drawDottedBox(g2d, thisPurple, f14d[0], f14d[1], f14d[2], f14d[3]);
	        g2d.drawString("F14", f14d[0]+5, f14d[1]+f14d[3]-5);
	        g2d.setStroke(semithickStroke);
	        drawReceiver(g2d, f14r[0], f14r[1], f14r[2], f14r[3]); // 1
	        drawReceiver(g2d, f14r[0]+60, f14r[1], f14r[2], f14r[3]); // 2
	        drawReceiver(g2d, f14r[0]+130, f14r[1], f14r[2], f14r[3]); // 3
	        g2d.draw(new Line2D.Double(f14r[0]+f14r[2]/2, f14r[1]+35, f14r[0]+f14r[2]/2, f14r[1]+60));
	        g2d.draw(new Line2D.Double(f14r[0]+f14r[2]/2+60, f14r[1]+35, f14r[0]+f14r[2]/2+60, f14r[1]+60));
	        g2d.draw(new Line2D.Double(f14r[0]+f14r[2]/2+130, f14r[1]+35, f14r[0]+f14r[2]/2+130, f14r[1]+60));
	        drawCrossedCircle(g2d, f14r[0], f14r[1]+60, f14r[4]);
	        drawCrossedCircle(g2d, f14r[0]+60, f14r[1]+60, f14r[4]);
	        drawCrossedCircle(g2d, f14r[0]+130, f14r[1]+60, f14r[4]);
	        // Connecting Receivers to Multibox
	        g2d.draw(new Line2D.Double(f14r[0]+f14r[2]/2, f14r[1]+60+f14r[4], f14r[0]+f14r[2]/2, f14r[1]+60+f14r[4]+10)); // Rcv 1
	        g2d.draw(new Line2D.Double(f14r[0]+f14r[2]/2, f14r[1]+60+f14r[4]+10, f14m[0]+14, f14r[1]+60+f14r[4]+10));
	        g2d.draw(new Line2D.Double(f14m[0]+14, f14r[1]+60+f14r[4]+10, f14m[0]+14, f14m[1]+5));
	        g2d.draw(new Line2D.Double(f14r[0]+60+f14r[2]/2-5, f14r[1]+60+f14r[4], f14r[0]+60+f14r[2]/2-5, f14r[1]+60+f14r[4]+10)); // Rcv 2
	        g2d.draw(new Line2D.Double(f14r[0]+60+f14r[2]/2-5, f14r[1]+60+f14r[4]+10, f14m[0]+49, f14r[1]+60+f14r[4]+10));
	        g2d.draw(new Line2D.Double(f14m[0]+49, f14r[1]+60+f14r[4]+10, f14m[0]+49, f14m[1]+5));
	        g2d.draw(new Line2D.Double(f14r[0]+60+f14r[2]/2+5, f14r[1]+60+f14r[4], f14r[0]+60+f14r[2]/2+5, f14r[1]+60+f14r[4]+20)); // Rcv 2
	        g2d.draw(new Line2D.Double(f14r[0]+60+f14r[2]/2+5, f14r[1]+60+f14r[4]+20, f14m[0]+14+35*2, f14r[1]+60+f14r[4]+20));
	        g2d.draw(new Line2D.Double(f14m[0]+14+35*2, f14r[1]+60+f14r[4]+20, f14m[0]+14+35*2, f14m[1]+5));
	        g2d.draw(new Line2D.Double(f14r[0]+130+f14r[2]/2-5, f14r[1]+60+f14r[4], f14r[0]+130+f14r[2]/2-5, f14r[1]+60+f14r[4]+10)); // Rcv 3
	        g2d.draw(new Line2D.Double(f14r[0]+130+f14r[2]/2-5, f14r[1]+60+f14r[4]+10, f14m[0]+14+35*3, f14r[1]+60+f14r[4]+10));
	        g2d.draw(new Line2D.Double(f14m[0]+14+35*3, f14r[1]+60+f14r[4]+10, f14m[0]+14+35*3, f14m[1]+5));
	        g2d.draw(new Line2D.Double(f14r[0]+130+f14r[2]/2+5, f14r[1]+60+f14r[4], f14r[0]+130+f14r[2]/2+5, f14r[1]+60+f14r[4]+10)); // Rcv 3
	        g2d.draw(new Line2D.Double(f14r[0]+130+f14r[2]/2+5, f14r[1]+60+f14r[4]+10, f14m[0]+14+35*5, f14r[1]+60+f14r[4]+10));
	        g2d.draw(new Line2D.Double(f14m[0]+14+35*5, f14r[1]+60+f14r[4]+10, f14m[0]+14+35*5, f14m[1]+5));
	        g2d.draw(new Line2D.Double(f14m[0]+14+35*4, f14m[1]+5, f14m[0]+14+35*4, f14m[1]-10)); // Random one
	        
	        // Doublers/Triplers
	        g2d.draw(new Rectangle2D.Double(f14dt[0], f14dt[1], f14dt[2], f14dt[3])); // X2
	        g2d.draw(new Rectangle2D.Double(f14dt[0]+30, f14dt[1]+45, f14dt[2], f14dt[3])); // X3
	        g2d.draw(new Rectangle2D.Double(f14dt[0]+80, f14dt[1]+45, f14dt[2], f14dt[3])); // X6/1
	        g2d.draw(new Rectangle2D.Double(f14dt[0]+80, f14dt[1]+75, f14dt[2], f14dt[3])); // X6/2
	        g2d.draw(new Rectangle2D.Double(f14dt[0]+80, f14dt[1]+105, f14dt[2], f14dt[3])); // X6/3
	        g2d.draw(new Rectangle2D.Double(f14dt[0]+10, f14dt[1]+155, f14dt[2]+70, f14dt[3]+7));
	        g2d.drawString("X2", (int)f14dt[0]+5, (int)f14dt[1]+12);
	        g2d.drawString("X3", (int)f14dt[0]+35, (int)f14dt[1]+57);
            g2d.drawString("X2", (int)f14dt[0]+85, (int)f14dt[1]+57);
	        g2d.drawString("X2", (int)f14dt[0]+85, (int)f14dt[1]+87);
	        g2d.drawString("X2", (int)f14dt[0]+85, (int)f14dt[1]+117);
	        // Connecting Lines
	        g2d.draw(new Line2D.Double(f14dt[0]+30+f14dt[2]/2, f14dt[1]+45+f14dt[3], f14dt[0]+30+f14dt[2]/2, f14dt[1]+155));// X3 to Combining Box
	        g2d.draw(new Line2D.Double(f14dt[0]+f14dt[2], f14dt[1]+f14dt[3]/2, f14r[0], f14dt[1]+f14dt[3]/2)); // X2 to Receiver
	        g2d.draw(new Line2D.Double(f14dt[0]+f14dt[2]/2, f14dt[1]+f14dt[3], f14dt[0]+f14dt[2]/2, f14dt[1]+155)); // X2 to Combining box
	        g2d.draw(new Line2D.Double(f14l86[0]+f14l86[2], f14l86[1]+f14l86[2]/2, f14dt[2]+10, f14l86[1]+f14l86[2]/2)); // Combining box to L86
	        g2d.draw(new Line2D.Double(f14dt[0]+80+f14dt[2]/2, f14dt[1]+45+f14dt[3], f14dt[0]+80+f14dt[2]/2, f14dt[1]+75)); // X6/1 to X6/2
	        g2d.draw(new Line2D.Double(f14dt[0]+80+f14dt[2]/2, f14dt[1]+75+f14dt[3], f14dt[0]+80+f14dt[2]/2, f14dt[1]+105)); // X6/2 to X6/3
	        g2d.draw(new Line2D.Double(f14dt[0]+80+f14dt[2]/2, f14dt[1]+105+f14dt[3], f14dt[0]+80+f14dt[2]/2, f14dt[1]+155)); // X6/3 to Combining Box
	        drawCurvedThing(g2d, f14l86[0], f14l86[1], f14l86[2]);
	        g2d.drawString("L86", (int)f14l86[0], (int)f14l86[1]+35);
	        // Multibox
	        g2d.draw(new Rectangle2D.Double(f14m[0], f14m[1], f14m[2], f14m[3]));
	        g2d.draw(new Ellipse2D.Double(f14m[0]+12, f14m[1]+5, circlesize, circlesize));
	        g2d.draw(new Ellipse2D.Double(f14m[0]+12+35, f14m[1]+5, circlesize, circlesize));
	        g2d.draw(new Ellipse2D.Double(f14m[0]+12+35*2, f14m[1]+5, circlesize, circlesize));
	        g2d.draw(new Ellipse2D.Double(f14m[0]+12+35*3, f14m[1]+5, circlesize, circlesize));
	        g2d.draw(new Ellipse2D.Double(f14m[0]+12+35*4, f14m[1]+5, circlesize, circlesize));
	        g2d.draw(new Ellipse2D.Double(f14m[0]+12+35*5, f14m[1]+5, circlesize, circlesize));
	        // Dynamic arrow
	        g2d.draw(new Ellipse2D.Double(f14m[0]+f14m[2]/2, f14m[1]+f14m[3], circlesize, circlesize));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]/2+2, f14m[1]+f14m[3], f14m[0]+12+35+2, f14m[1]+14));
	        drawArrowHead(g2d, f14m[0]+12+35+2, f14m[1]+14, f14m[0]+f14m[2]/2+2, f14m[1]+f14m[3]);
	        // Attenuator
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]/2+2, f14m[1]+f14m[3]+4, f14m[0]+f14m[2]/2+2, f14m[1]+f14m[3]+25));
	        g2d.draw(new Rectangle2D.Double(f14m[0]+f14m[2]/2-25, f14m[1]+f14m[3]+25, 50, 50));
	        // C26
	        g2d.drawString("C26", (int)c26[0]+5, (int)c26[1]+(int)c26[3]-5);
	        drawDottedBox(g2d, thisPurple, (int)c26[0], (int)c26[1], (int)c26[2], (int)c26[3]);
	        g2d.setStroke(semithickStroke);
	        g2d.draw(new Line2D.Double(c26[0]+c26[2]/2, c26[1], c26[0]+c26[2]/2, c26[1]+c26[3]));
	        // Connecting Attenuator and C26
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]/2, f14m[1]+f14m[3]+75, f14m[0]+f14m[2]/2, f14dt[1]+205));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]/2, f14dt[1]+205, c26[0]+c26[2]/2, f14dt[1]+205));
	        g2d.draw(new Line2D.Double(c26[0]+c26[2]/2, f14dt[1]+205, c26[0]+c26[2]/2, c26[1]));
	        // Connect C26 and C34
	        g2d.draw(new Line2D.Double(c26[0]+c26[2]/2, c26[1]+c26[3], c26[0]+c26[2]/2, c26[1]+c26[3]+50));
	        g2d.draw(new Line2D.Double(c26[0]+c26[2]/2, c26[1]+c26[3]+50, c34[0]+14, c26[1]+c26[3]+50));
	        g2d.draw(new Line2D.Double(c34[0]+14, c26[1]+c26[3]+50, c34[0]+14, c34[1]+5));

	        // C26 Monitor points
	        g2d.setColor(thisOrange);
	        g2d.setStroke(thickStroke);
	        g2d.draw(new Line2D.Double(c26[0]+c26[2]/2, c26[1]+10, c26[0]+c26[2]/2+15,  c26[1]+10)); // RFA
	        g2d.draw(new Line2D.Double(c26[0]+c26[2]/2, c26[1]+60, c26[0]+c26[2]/2+15, c26[1]+60)); // RFB
	        drawMonitorPoint(g2d, c26[0]+c26[2]/2+15, c26[1]+8, circlesize);
	        drawMonitorPoint(g2d, c26[0]+c26[2]/2+15, c26[1]+58, circlesize);
	        g2d.drawString("RFA", (int)c26[0]+(int)c26[2]/2+24, (int)c26[1]+13);
	        g2d.drawString("RFB", (int)c26[0]+(int)c26[2]/2+24, (int)c26[1]+63);
	        
	        // F14 Mon points
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]/2-25+50, f14m[1]+f14m[3]+25+25, f14m[0]+f14m[2]/2-25+50+15, f14m[1]+f14m[3]+25+25)); // Atten monpt
	        drawMonitorPoint(g2d, f14m[0]+f14m[2]/2-25+50+15, f14m[1]+f14m[3]+25+23, circlesize);
	        g2d.drawString("ATTENA M", (int)f14m[0]+(int)f14m[2]/2-25+74, (int)f14m[1]+(int)f14m[3]+53);
	        
	        g2d.draw(new Line2D.Double(f14dt[0]+f14dt[3]/2, f14dt[1], f14dt[0]+f14dt[3]/2, f14dt[1]-10)); // X2
	        g2d.draw(new Line2D.Double(f14dt[0]+f14dt[3]/2, f14dt[1]-10, f14dt[0]+f14dt[3]/2+15, f14dt[1]-10));
	        drawMonitorPoint(g2d, f14dt[0]+f14dt[3]/2+15, f14dt[1]-12, circlesize);
	        g2d.drawString("K-LO_A", (int)f14dt[0]+(int)f14dt[3]/2+24, (int)f14dt[1]-7);
	        g2d.draw(new Line2D.Double(f14dt[0]+30+f14dt[3]/2, f14dt[1]+45, f14dt[0]+30+f14dt[3]/2, f14dt[1]+35)); // X3
	        g2d.draw(new Line2D.Double(f14dt[0]+30+f14dt[3]/2, f14dt[1]+35, f14dt[0]+30+f14dt[3]/2+15, f14dt[1]+35));
	        drawMonitorPoint(g2d, f14dt[0]+30+f14dt[3]/2+15, f14dt[1]+33, circlesize);
	        g2d.drawString("W-50TRBL", (int)f14dt[0]+30+(int)f14dt[3]/2+24, (int)f14dt[1]+38);
	        g2d.draw(new Line2D.Double(f14dt[0]+80+f14dt[2], f14dt[1]+45+f14dt[3]/2, f14dt[0]+80+f14dt[2]+15, f14dt[1]+45+f14dt[3]/2)); //X6/1
	        drawMonitorPoint(g2d, f14dt[0]+80+f14dt[2]+15, f14dt[1]+43+f14dt[3]/2, circlesize);
	        g2d.drawString("W-100GDBL", (int)f14dt[0]+80+(int)f14dt[2]+24, (int)f14dt[1]+43+(int)f14dt[3]/2+5);
	        g2d.draw(new Line2D.Double(f14dt[0]+80+f14dt[2], f14dt[1]+75+f14dt[3]/2, f14dt[0]+80+f14dt[2]+15, f14dt[1]+75+f14dt[3]/2)); //X6/2
	        drawMonitorPoint(g2d, f14dt[0]+80+f14dt[2]+15, f14dt[1]+73+f14dt[3]/2, circlesize);
	        g2d.drawString("W-50GDBL", (int)f14dt[0]+80+(int)f14dt[2]+24, (int)f14dt[1]+73+(int)f14dt[3]/2+5);
	        g2d.draw(new Line2D.Double(f14dt[0]+80+f14dt[2], f14dt[1]+105+f14dt[3]/2, f14dt[0]+80+f14dt[2]+15, f14dt[1]+105+f14dt[3]/2)); //X6/3
	        drawMonitorPoint(g2d, f14dt[0]+80+f14dt[2]+15, f14dt[1]+103+f14dt[3]/2, circlesize);
	        g2d.drawString("W-25GDBL", (int)f14dt[0]+80+(int)f14dt[2]+24, (int)f14dt[1]+103+(int)f14dt[3]/2+4);
	        
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]-2, circlesize);
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+10, circlesize);
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+23, circlesize);
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+36, circlesize);
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+49, circlesize);
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+62, circlesize);
	        drawMonitorPoint(g2d, (int)f14m[0]+(int)f14m[2]+20, (int)f14m[1]+75, circlesize);
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1], f14m[0]+f14m[2]+24+15, f14m[1]));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+12, f14m[0]+f14m[2]+24+15, f14m[1]+12));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+25, f14m[0]+f14m[2]+24+15, f14m[1]+25));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+38, f14m[0]+f14m[2]+24+15, f14m[1]+38));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+51, f14m[0]+f14m[2]+24+15, f14m[1]+51));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+64, f14m[0]+f14m[2]+24+15, f14m[1]+64));
	        g2d.draw(new Line2D.Double(f14m[0]+f14m[2]+24, f14m[1]+77, f14m[0]+f14m[2]+24+15, f14m[1]+77));
	        g2d.drawString("K-DCOK", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+5);
	        g2d.drawString("Q-DCOK", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+12+5);
	        g2d.drawString("W-DCOK", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+25+5);
	        g2d.drawString("IF-DCOK", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+38+5);
	        g2d.drawString("K-LOSLEEP", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+51+5);
	        g2d.drawString("Q-LOSLEEP", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+64+5);
	        g2d.drawString("W-LOSLEEP", (int)f14m[0]+(int)f14m[2]+44, (int)f14m[1]+77+5);
	   
	        
	        // GREEN C21
	        int[] c21d = {815, 50, 150, 250}; // Dotted box x, y, width, height
	        double[] c21r = {910, 10, 30, 10, 30}; // Receiver x, y, width, height
	        
	        g2d.setColor(thisGreen);
	        drawDottedBox(g2d, thisGreen, c21d[0], c21d[1], c21d[2], c21d[3]);
	        g2d.drawString("C21", c21d[0]+5, c21d[1]+c21d[3]-5);
	        g2d.setStroke(semithickStroke);
	        drawReceiver(g2d, c21r[0], c21r[1], c21r[2], c21r[3]);
	        g2d.draw(new Line2D.Double(c21r[0]+c21r[2]/2, c21r[1]+35, c21r[0]+c21r[2]/2, c21r[1]+85));
	        drawCrossedCircle(g2d, c21r[0], c21r[1]+85, c21r[4]);
	        g2d.draw(new Line2D.Double(c21r[0], c21r[1]+100, c21r[0]-55, c21r[1]+100));	// Line to Curved thing
	        g2d.draw(new Line2D.Double(c21r[0]+c21r[2]/2, c21r[1]+115, c21r[0]+c21r[2]/2, c21r[1]+390));	// Line(1-3) extending to C34
	        g2d.draw(new Line2D.Double(c21r[0]+c21r[2]/2, c21r[1]+390, c34[0]+39, c21r[1]+390));
	        g2d.draw(new Line2D.Double(c34[0]+39, c21r[1]+390, c34[0]+39, c34[1]+5));
	    	drawCurvedThing(g2d, c21r[0]-75, c21r[1]+90, 20);
	        g2d.setColor(thisOrange);
	        g2d.setStroke(thickStroke);
	        g2d.draw(new Line2D.Double(c21r[0]-65, c21r[1]+110, c21r[0]-65, c21r[1]+120));
	        g2d.draw(new Line2D.Double(c21r[0]-65, c21r[1]+120, c21r[0]-45, c21r[1]+120));
	        drawMonitorPoint(g2d, c21r[0]-45, c21r[1]+118, circlesize);	// LOCK
	        g2d.drawString("LOCK", (int)c21r[0]-35, (int)c21r[1]+122);
	        drawMonitorPoint(g2d, (int)c21r[0]-45, c21r[1]+85, circlesize);	// LO
	        g2d.drawString("LO", (int)c21r[0]-35, (int)c21r[1]+92);
	        
	        
	        // BLUE C28
	        int[] c28d = {635, 50, 150, 250}; // Dotted box x, y, width, height
	        double[] c28r = {695, 10, 30, 10, 30}; // Dotted box x, y, width, height
	        
	        g2d.setColor(thisBlue);
	        drawDottedBox(g2d, thisBlue, c28d[0], c28d[1], c28d[2], c28d[3]);
	        g2d.drawString("C28", c28d[0]+5, c28d[1]+c28d[3]-5);

	        g2d.setStroke(semithickStroke);
	        drawReceiver(g2d, c28r[0], c28r[1], c28r[2], c28r[3]);
	        g2d.draw(new Line2D.Double(c28r[0]+c28r[2]/2, c28r[1]+35, c28r[0]+c28r[2]/2, c28r[1]+340));
	        g2d.draw(new Line2D.Double(c28r[0]+c28r[2]/2, c28r[1]+340, c34[0]+89, c28r[1]+340));
	        g2d.draw(new Line2D.Double(c34[0]+89, c28r[1]+340, c34[0]+89, c34[1]+5));
	        
	        g2d.setColor(thisOrange);
	        g2d.setStroke(thickStroke);
	        g2d.draw(new Line2D.Double(c28r[0]+c28r[2]/2, c28r[1]+105, c28r[0]+c28r[2]/2+15,  c28r[1]+105)); // RFA
	        g2d.draw(new Line2D.Double(c28r[0]+c28r[2]/2, c28r[1]+205, c28r[0]+c28r[2]/2+15, c28r[1]+205)); // RFB
	        drawMonitorPoint(g2d, c28r[0]+c28r[2]/2+15, c28r[1]+103, circlesize);
	        drawMonitorPoint(g2d, c28r[0]+c28r[2]/2+15, c28r[1]+203, circlesize);
	        g2d.drawString("RFA", (int)c28r[0]+(int)c28r[2]/2+24, (int)c28r[1]+110);
	        g2d.drawString("RFB", (int)c28r[0]+(int)c28r[2]/2+24, (int)c28r[1]+210);

	    }
	    
	    void drawCrossedCircle(Graphics2D g, double x1, double y1, double diameter) {
	    	double line1x1 = x1 + 5;
	    	double line1y1 = y1 + 5;
	    	double line1x2 = line1x1 + 20;
	    	double line1y2 = line1y1 + 20;
	    	
	        g.draw(new Ellipse2D.Double(x1, y1, diameter, diameter));	// Crossed thing
	        g.draw(new Line2D.Double(line1x1, line1y1, line1x2, line1y2));
	        g.draw(new Line2D.Double(line1x2, line1y1, line1x1, line1y2));
	    }
	    
	    void drawMonitorPoint(Graphics2D g, double x1, double y1, double diameter) {
	        g.setColor(thisOrange);
	        g.setStroke(thickStroke);
	        g.draw(new Ellipse2D.Double(x1, y1, diameter, diameter));
	    }
	    
	    void drawDottedBox(Graphics2D g, Color color, int x1, int y1, int width, int height) {
	        g.setColor(color);
	    	g.setStroke(dottedStroke);
	        g.drawRect(x1, y1, width, height);
	    }
	    
	    void drawReceiver(Graphics2D g, double x1, double y1, double width, double height) {
	    	double line2x1 = x1 + 30;
	    	double linex2 = x1 + 15;
	    	double liney1 = y1 + 5;
	    	double liney2 = y1 + 35;
	    	
	    	g.draw(new Ellipse2D.Double(x1, y1, width, height));		// Receiver
	        g.draw(new Line2D.Double(x1, liney1, linex2, liney2));
	        g.draw(new Line2D.Double(line2x1, liney1, linex2, liney2));
	    }
	    
	    void drawCurvedThing(Graphics2D g, double x1, double y1, double diameter) {
	    	g.draw(new Ellipse2D.Double(x1, y1, diameter, diameter));
	        CubicCurve2D c = new CubicCurve2D.Double();
	        c.setCurve(x1, y1+10, x1+8, y1, x1+12, y1+20, x1+20, y1+10);
	        g.draw(c);
	    	
	    }
	    
	    void drawArrowHead(Graphics2D g, double x1, double y1, double x2, double y2) {
	    	double a = 0; double b = 0;
	    	double c = 0; double d = 0;
	    	double length = 10.0;
	    	double theta = Math.atan2((y2-y1),(x2-x1));
	    	
	    	double costheta = Math.cos(theta - Math.PI/6);
	    	double sintheta = Math.sin(theta - Math.PI/6);
	    	
	    	if (costheta < 0) {
	    		costheta = costheta * (-1);
	    	}
	    	if (sintheta < 0) {
	    		sintheta = sintheta * (-1);
	    	}

	    	if (y1 < y2) {	// Arrow pointing up
		    	a = x1 + length * costheta;
		    	b = y1 + length * sintheta;
		    	
	    	} else if (y1 > y2) {	// Arrow pointing down
		    	a = x1 - length * costheta;
		    	b = y1 - length * sintheta;
	    	}
	    	g.draw(new Line2D.Double(a, b, x1, y1));
	    	
	    	costheta = Math.cos(theta + Math.PI/6);
	    	sintheta = Math.sin(theta + Math.PI/6);
	    	
	    	if (costheta < 0) {
	    		costheta = costheta * (-1);
	    	}
	    	if (sintheta < 0) {
	    		sintheta = sintheta * (-1);
	    	}

	    	if (y1 < y2) {	// Arrow pointing up
		    	c = x1 + length * costheta;
		    	d = y1 + length * sintheta;
		    	
	    	} else if (y1 > y2) {	// Arrow pointing down
		    	c = x1 + length * costheta;
		    	d = y1 - length * sintheta;
	    	}
	    	g.draw(new Line2D.Double(c, d, x1, y1));
	    }
		
	}
	///// END NESTED CLASS /////
	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;

	/** Constructor. */
	public ConversionDiagram() {
		JPanel jp = new JPanel();
		jp.setSize(new Dimension(800,600));
		jp.add(new PaintDiagram());
		
		add(jp);

	}

	public boolean loadSetup(SavedSetup setup) {
		itsSetup = setup;
		
		return true;
	}

	public synchronized SavedSetup getSetup() {
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
	
}
