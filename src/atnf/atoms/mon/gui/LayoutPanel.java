//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import atnf.atoms.mon.SavedSetup;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

/**
 * This is a tab that appears in each MonFrame window that allows the user to
 * control the size and layout of the different MonPanels that make up the
 * display.
 * 
 * @author David Brodrick
 * @author Camille Nicodemus
 * @version $Id: $
 * @see MonFrame
 */
public class LayoutPanel extends JPanel implements ActionListener, ItemListener {
	// ///////////////NESTED TABLE MODEL CLASS//////////////////
	private class LayoutTableModel extends AbstractTableModel {
		/** X positions for each panel. */
		protected ArrayList<Double> itsXPos = new ArrayList<Double>();
		/** Y positions for each panel. */
		protected ArrayList<Double> itsYPos = new ArrayList<Double>();
		/** Pixel widths for each panel. */
		protected ArrayList<Double> itsWidth = new ArrayList<Double>();
		/** Pixel heights for each panel. */
		protected ArrayList<Double> itsHeight = new ArrayList<Double>();

		public LayoutTableModel() {
		}

		public String getColumnName(int col) {
			switch (col) {
			case 0:
				return "Panel";
			case 1:
				return "X";
			case 2:
				return "Y";
			case 3:
				return "Width";
			case 4:
				return "Height";
			}
			return "ERROR";
		}

		/** Resize arrays to accommodate the change in the number of panels. */
		private void accomodateChanges() {
			/*
			 * Vector v = itsParent.getPanels(); synchronized (v) { int newsize
			 * = v.size(); int oldsize = itsLeftPos.length;
			 * 
			 * int[] newleft = new int[newsize]; int[] newright = new
			 * int[newsize]; int[] newtop = new int[newsize]; int[] newbottom =
			 * new int[newsize];
			 * 
			 * if (itsAutoControl.isSelected()) { //We just use the numbers
			 * derived from panels preferred sizes int toty = 0; for (int i=0;
			 * i<v.size(); i++) { toty += ((MonPanel)v.get(i)).getSize().height;
			 * }
			 * 
			 * int lastbot = 0; for (int i=0; i<v.size(); i++) { newleft[i] = 0;
			 * newright[i] = 100; newtop[i] = lastbot; newbottom[i] =
			 * ((MonPanel)v.get(i)).getSize().height; //newtop[i] =
			 * (int)(((float)lastbot)/toty); //newbottom[i] =
			 * (int)(((float)(((MonPanel)v.get(i)).getSize().height))/toty);
			 * //newbottom[i] =
			 * (int)(((float)(lastbot+((MonPanel)v.get(i)).getSize
			 * ().height))/toty); lastbot = newbottom[i]+1; } } else { //We need
			 * to let user specify the new numbers }
			 * 
			 * itsLeftPos = newleft; itsRightPos = newright; itsTopPos = newtop;
			 * itsBottomPos = newbottom; } //synchronized
			 */
		}

		public int getRowCount() {
			return itsParent.getPanels().size();
		}

		public int getColumnCount() {
			return 5;
		}

		public Object getValueAt(int row, int col) {
			itsAllPanels = itsParent.getPanels();

			if (col == 0) { // Panel name
				Class c = itsAllPanels.get(row).getClass();
				return MonPanel.getName(c);
			}

			if (itsManualControl.isSelected()) {
				if (col == 1) { // X-coordinate
					return itsXPos.get(row) * 100;
				} else if (col == 2) { // Y-coordinate
					return itsYPos.get(row) * 100;
				} else if (col == 3) { // Panel width
					return itsWidth.get(row) * 100;
				} else if (col == 4) { // Panel height
					return itsHeight.get(row) * 100;
				}
			} else if (itsAutoControl.isSelected()) {
				if (col == 1) { // X-coordinate
					return "N/A";
				} else if (col == 2) { // Y-coordinate
					return "N/A";
				} else if (col == 3) { // Panel width
					return "N/A";
				} else if (col == 4) { // Panel height
					return "N/A";
				}
			}
			return "LayoutPanel:LayoutTableModel:getValueAt:Unknown";
		}

		public boolean isCellEditable(int row, int col) {
			if (col > 0) {
				return true;
			} else {
				return false;
			}
		}

		public void setValueAt(Object value, int row, int col) {
			System.out.println("LayoutPanel:setValueAt");
			if (col == 1) { // X-coordinate
				String thisCell = value.toString();
				itsXPos.set(row, Double.parseDouble(thisCell) / 100);
			} else if (col == 2) { // Y-coordinate
				String thisCell = value.toString();
				itsYPos.set(row, Double.parseDouble(thisCell) / 100);
			} else if (col == 3) { // Panel width
				String thisCell = value.toString();
				itsWidth.set(row, Double.parseDouble(thisCell) / 100);
			} else if (col == 4) { // Panel height
				String thisCell = value.toString();
				itsHeight.set(row, Double.parseDouble(thisCell) / 100);
			}

			repaint();
			fireTableCellUpdated(row, col);
		}

		public void getCoordinates() {
			// Only execute the below code if a user converts from Automatic to
			// Manual layout control

			itsAllPanels = itsParent.getPanels();
			for (int i = 0; i < itsAllPanels.size(); i++) { // For each panel
				MonPanel thisPanel = (MonPanel) itsAllPanels.get(i);

				double pixelX = (double) thisPanel.getX();
				double pixelY = (double) thisPanel.getY();
				double pixelWidth = (double) thisPanel.getWidth();
				double pixelHeight = (double) thisPanel.getHeight();

				double pcX = pixelX
						/ (double) Integer.parseInt(itsWindowWidth.getText());
				double pcY = pixelY
						/ (double) Integer.parseInt(itsWindowHeight.getText());
				double pcWidth = pixelWidth
						/ (double) Integer.parseInt(itsWindowWidth.getText());
				double pcHeight = pixelHeight
						/ (double) Integer.parseInt(itsWindowHeight.getText());

				itsXPos.add(i, pcX); // X-coordinate
				itsYPos.add(i, pcY); // Y-coordinate
				itsWidth.add(i, pcWidth); // Width
				itsHeight.add(i, pcHeight); // Height in pixels
			}
		}

		public void tableChanged(TableModelEvent evt) {
			System.out.println("Table has been changed!!!");
		}

	}

	// //////////////////END NESTED CLASS///////////////////////

	// PreviewPanel provides a basic preview of how panels are placed on the
	// Display pane.
	class PreviewPanel extends JPanel {
		public PreviewPanel() {
			// setBorder(BorderFactory.createLineBorder(Color.black));
		}

		public Dimension getPreferredSize() {
			return new Dimension(250, 300);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (itsAutoControl.isSelected()) {
				g.drawString("Automatic control is selected.", 10, 20);
				g.drawString("Panels will be stacked vertically.", 10, 30);
				g.drawString("Switch to Manual layout control to rearrange panels.", 10, 40);
			} else if (itsManualControl.isSelected()) {
				if (!itsBackwards) {
					Vector currentPanels = itsParent.getPanels();
					int numPanels = currentPanels.size();
					int previewWidth = this.getPreferredSize().width-10;
					int previewHeight = this.getPreferredSize().height-10;

					for (int i=0; i<numPanels; i++) {
						MonPanel thisPan = (MonPanel)currentPanels.get(i);
						Class c = thisPan.getClass();
						String panelname = MonPanel.getName(c);
						double pX = itsTableModel.itsXPos.get(i);
						double pY = itsTableModel.itsYPos.get(i);
						double pW = itsTableModel.itsWidth.get(i);
						double pH = itsTableModel.itsHeight.get(i);
						
						int drawX = (int)(pX * previewWidth);
						int drawY = (int)(pY * previewHeight);
						int drawW = (int)(pW * previewWidth);
						int drawH = (int)(pH * previewHeight);

						g.drawRect(drawX, drawY, drawW, drawH);
						
						// Draw the class name inside the box
						int midX = drawW / 2;
						int midY = drawH / 2;
						Font cd = new Font("Helvetica", Font.PLAIN, 10);
						g.setFont(cd);
						g.drawString(panelname, drawX+10, drawY+10);
					}
				}
			}
		}
	}

	/** The MonFrame to which this panel belongs. */
	protected MonFrame itsParent = null;
	/** Check box for selecting automatic layout control. */
	protected JRadioButton itsAutoControl = new JRadioButton(
			"Automatic layout control");
	/** Check box for enabling manual layout control. */
	protected JRadioButton itsManualControl = new JRadioButton(
			"Manual layout control");
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
	/** Button for setting panel sizes. */
	protected JButton itsSetPanels = new JButton("Set Panel Sizes");
	/** Combo box for selecting what kind of MonPanel to add. */
	protected JComboBox itsPanelType = null;
	/** Reference to the TableModel. */
	protected LayoutTableModel itsTableModel = new LayoutTableModel();
	/** Reference to the Table display itself. */
	protected JTable itsTable = null;
	/** Reference to the Table ScrollPane. */
	protected JScrollPane itsTableScrollPane = null;
	/** Button for updating window size. */
	protected JButton itsUpdateWindowSize = new JButton("Update window size");
	/** Vector of panels currently displayed. */
	protected Vector itsAllPanels = null;
	/** Used for backwards compatibility - prevent PreviewPanel from showing if older saved setup */
	protected static boolean itsBackwards = true;
	
	protected static int itsPreferredHeight = 0;
	protected static int itsPreferredWidth = 0;

	/** Constructor. */
	public LayoutPanel(MonFrame parent) {
		itsParent = parent;
		JPanel contentPane = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		GridBagConstraints cremovep = new GridBagConstraints();
		GridBagConstraints ccontp = new GridBagConstraints();
		GridBagConstraints cprevp = new GridBagConstraints();

		// Temporary layout panels
		JPanel contpanel = new JPanel(new GridBagLayout());
		JPanel removepanel = new JPanel(new GridBagLayout());
		JPanel prevpanel = new JPanel(new GridBagLayout());

		// Panel table
		itsTable = new JTable(itsTableModel);
		itsTableScrollPane = new JScrollPane(itsTable);
		itsTableScrollPane.setPreferredSize(new Dimension(270, 500));

		// Auto/Manual selection
		itsAutoControl.addActionListener(this);
		itsAutoControl.setActionCommand("auto");
		itsAutoControl.addItemListener(this);
		itsManualControl.addActionListener(this);
		itsManualControl.setActionCommand("manual");
		ButtonGroup group = new ButtonGroup();
		group.add(itsAutoControl);
		group.add(itsManualControl);
		itsAutoControl.setSelected(true);

		itsUpdateWindowSize.addActionListener(this);
		itsUpdateWindowSize.setActionCommand("update");

		contpanel.setPreferredSize(new Dimension(240, 180));

		ccontp.gridx = 0;
		ccontp.gridy = 0;
		ccontp.gridwidth = 4;
		ccontp.anchor = GridBagConstraints.LINE_START;
		contpanel.add(itsAutoControl, ccontp);
		ccontp.gridy = 1;
		contpanel.add(itsManualControl, ccontp);

		// User can specify window width/height with Manual control
		ccontp.gridy = 2;
		ccontp.insets = new Insets(10, 0, 0, 0);
		contpanel.add(new JLabel("Default size of window (pixels): "), ccontp);
		ccontp.gridwidth = 1; // Reset span to 1-column
		ccontp.gridy = 3;
		ccontp.insets = new Insets(0, 0, 10, 10);
		contpanel.add(new JLabel("Width:"), ccontp);
		ccontp.gridx = 1;
		ccontp.gridy = 3;
		contpanel.add(itsWindowWidth, ccontp);
		ccontp.gridx = 2;
		ccontp.gridy = 3;
		contpanel.add(new JLabel("Height:"), ccontp);
		ccontp.gridx = 3;
		ccontp.gridy = 3;
		contpanel.add(itsWindowHeight, ccontp);
		ccontp.gridwidth = 4;
		ccontp.gridx = 1;
		ccontp.gridy = 4;
		contpanel.add(itsUpdateWindowSize, ccontp);

		ccontp.anchor = GridBagConstraints.CENTER;

		// Preview pane
		TitledBorder previewtitle;
		previewtitle = BorderFactory.createTitledBorder("Preview Pane");
		prevpanel.setBorder(previewtitle);
		// prevpanel.setPreferredSize(new Dimension(250,250));
		cprevp.gridx = 0;
		cprevp.gridy = 0;
		cprevp.anchor = GridBagConstraints.SOUTH;
		cprevp.insets = new Insets(0, 0, 0, 5);
		prevpanel.add(new PreviewPanel(), c);

		itsSetPanels.addActionListener(this);
		itsSetPanels.setActionCommand("setpanels");

		// Remove selected/all buttons
		itsRemoveAll.addActionListener(this);
		itsRemoveAll.setActionCommand("removeall");
		itsRemoveSelected.addActionListener(this);
		itsRemoveSelected.setActionCommand("removeselected");

		cremovep.insets = new Insets(5, 0, 0, 20); // Top, left, bottom, right
		cremovep.gridx = 0;
		cremovep.gridy = 0;
		removepanel.add(itsSetPanels, cremovep);
		cremovep.insets = new Insets(5, 0, 0, 5);
		cremovep.gridx = 1;
		removepanel.add(itsRemoveSelected, cremovep);
		cremovep.gridx = 2;
		removepanel.add(itsRemoveAll, cremovep);

		// Layout of panes on main control panel
		c.gridheight = 2;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(25, 0, 0, 0);
		contentPane.add(itsTableScrollPane, c);
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 0;
		contentPane.add(contpanel, c);
		c.gridx = 1;
		c.gridy = 1;
		contentPane.add(prevpanel, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 2;
		contentPane.add(removepanel, c);

		add(contentPane);

		itsAutoControl.doClick(); // Enable auto layout control
	}

	/**
	 * Called by the MonFrame when a panel is added or removed. This provides an
	 * opportunity for us to update the table display.
	 */
	public void update() {
		itsTableModel.accomodateChanges();
		itsTableModel.fireTableStructureChanged();
		if (itsAutoControl.isSelected()) {
			// Copy the current window size to the fields
			itsWindowWidth.setText("" + itsParent.getWidth());
			itsWindowHeight.setText("" + itsParent.getHeight());
		}
	}

	/** Populate dimension ArrayLists with default coordinates in Manual mode */
	public void defineCoordinates(int i) {
		double zero = 0.0;
		double hundred = 1.0;
		itsTableModel.itsXPos.add(i, zero);
		itsTableModel.itsYPos.add(i, zero);
		itsTableModel.itsWidth.add(i, hundred);
		itsTableModel.itsHeight.add(i, hundred);

		return;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null) {
			System.err
					.println("LayoutPanel:actionPerformed: Got null command!");
		} else if (cmd == "auto") {
			itsWindowWidth.setEnabled(false);
			itsWindowHeight.setEnabled(false);
			itsTable.setEnabled(true);
		} else if (cmd == "manual") {
			itsWindowWidth.setEnabled(true);
			itsWindowHeight.setEnabled(true);
			itsTable.setEnabled(true);

		} else if (cmd == "removeselected") {
			MonPanel thisPanel = (MonPanel) itsAllPanels.get(itsTable
					.getSelectedRow());
			itsParent.removePanel(thisPanel);
		} else if (cmd == "removeall") {
			itsParent.blankSetup();
		} else if (cmd == "update") { // Update window size
			int newWidth = Integer.parseInt(itsWindowWidth.getText());
			int newHeight = Integer.parseInt(itsWindowHeight.getText());

			itsParent.setSize(new Dimension(newWidth, newHeight));
		} else if (cmd == "setpanels") {
			setPanels();
		}
		repaint();
	}

	public void setPanels() {
		if (itsManualControl.isSelected()) {
			itsAllPanels = itsParent.getPanels();
			itsParent.clearPanels();
			for (int i = 0; i < itsAllPanels.size(); i++) {
				MonPanel panel = (MonPanel) itsAllPanels.get(i);
				double x = itsTableModel.itsXPos.get(i);
				double y = itsTableModel.itsYPos.get(i);
				double width = itsTableModel.itsWidth.get(i);
				double height = itsTableModel.itsHeight.get(i);

				itsParent.redrawPanels(panel, x, y, width, height);
				
				validate();
				repaint();
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			// Automatic to Manual transition
			itsTable.setVisible(true);
			itsTableModel.getCoordinates();
			itsBackwards = false;
			System.out.println("NOT itsBackwards!");
		} else if (e.getStateChange() == ItemEvent.SELECTED) {
			// Manual to Automatic transition
			System.out.println("Manual has been changed to auto mode");
			itsAllPanels = itsParent.getPanels();
			itsParent.clearPanels();
			for (int i = 0; i < itsAllPanels.size(); i++) {
				MonPanel panel = (MonPanel) itsAllPanels.get(i);
				itsParent.addPanelToDisplay(panel);
			}

		}
	}

	/** Save a setup describing the selected layout control options. */
	public void getSetup(SavedSetup setup) {
		itsAllPanels = itsParent.getPanels();

		// Save Automatic/Manual layout preference
		if (itsManualControl.isSelected()) {
			setup.put("control", "manual");
			// Save individual panel dimensions
			for (int i = 0; i < itsAllPanels.size(); i++) {
				setup.put("panelx" + i, itsTableModel.itsXPos.get(i).toString());
				setup.put("panely" + i, itsTableModel.itsYPos.get(i).toString());
				setup.put("panelwidth" + i, itsTableModel.itsWidth.get(i)
						.toString());
				setup.put("panelheight" + i, itsTableModel.itsHeight.get(i)
						.toString());
			}
		} else if (itsAutoControl.isSelected()) {
			setup.put("control", "auto");
		}

		// Save preferred window dimensions
		setup.put("windowwidth", itsWindowWidth.getText());
		setup.put("windowheight", itsWindowHeight.getText());
		
		return;
	}

	/**
	 * Configure the GUI to display the setup options described in the specified
	 * saved setup.
	 */
	public void loadSetup(SavedSetup setup) {
		int preferredWidth = 600; // Default values for window size
		int preferredHeight = 700;
		int numpanels = Integer.parseInt((String) setup.get("numpanels"));

		if (setup.containsKey("control")) {
			itsBackwards = false;
			// Set window to preferred dimensions
			String savedWidth = setup.get("windowwidth");
			String savedHeight = setup.get("windowheight");
			itsPreferredWidth = Integer.parseInt(savedWidth);
			itsPreferredHeight = Integer.parseInt(savedHeight);

			if (setup.get("control").equals("manual")) {
				itsManualControl.doClick();
				for (int i = 0; i < numpanels; i++) {
					itsTableModel.itsXPos.add(i,
							Double.parseDouble(setup.get("panelx" + i)));
					itsTableModel.itsYPos.add(i,
							Double.parseDouble(setup.get("panely" + i)));
					itsTableModel.itsWidth.add(i,
							Double.parseDouble(setup.get("panelwidth" + i)));
					itsTableModel.itsHeight.add(i,
							Double.parseDouble(setup.get("panelheight" + i)));
				}

				itsTableModel.fireTableStructureChanged();
				itsTableModel.fireTableDataChanged();
				
				//setPanels();

			} else {
				itsAutoControl.doClick();
			}
			itsWindowWidth.setText(savedWidth);
			itsWindowHeight.setText(savedHeight);
		} else {
			System.out
					.println("LayoutPanel:loadSetup: Does not contain panel layout information");
		}

		return;
	}

	public void resizeWindow() {
		System.out.printf("%d %d", itsPreferredWidth, itsPreferredHeight);
		itsParent.setSize(new Dimension(itsPreferredWidth, itsPreferredHeight));
		validate();
		repaint();
	}
}
