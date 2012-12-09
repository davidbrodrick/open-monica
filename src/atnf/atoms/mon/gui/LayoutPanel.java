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
@SuppressWarnings("serial")
public class LayoutPanel extends JPanel implements ActionListener, ItemListener, ComponentListener {
	// /////////////// NESTED TABLE MODEL CLASS //////////////////
	private class LayoutTableModel extends AbstractTableModel {
		/** X positions for each panel. */
		protected ArrayList<Integer> itsXPos = new ArrayList<Integer>();
		/** Y positions for each panel. */
		protected ArrayList<Integer> itsYPos = new ArrayList<Integer>();
		/** Pixel widths for each panel. */
		protected ArrayList<Integer> itsWidth = new ArrayList<Integer>();
		/** Pixel heights for each panel. */
		protected ArrayList<Integer> itsHeight = new ArrayList<Integer>();
		/** Vector of panels currently displayed. */
		protected Vector<MonPanel> itsCurrentPanels = null;
		/** Current size of the window */
		protected int[] itsWindowSize = new int[2];
		protected double[] itsWindowSizeDouble = new double[2];
		/** Double-type array of panel dimensions */
		protected double[] itsPanelDouble = new double[4];

		// Returns column names for the table
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

		public int getColumnCount() {
			return 5;
		}

		// Returns the current number of panels being displayed
		public int getRowCount() {
			return getCurrentPanels().size();
		}

/*		public Class getColumnClass(int c) {
			return (getValueAt(0, c).getClass());
		}*/
		
		// Returns value for the selected cell
		public Object getValueAt(int row, int col) {
			getCurrentPanels();
			// Return panel type
			if (col == 0) {
				Class<? extends MonPanel> c = itsCurrentPanels.get(row).getClass();
				return MonPanel.getName(c);
			}

			if (itsManualControl.isSelected()) {
				if (col == 1) { // X-coordinate
					return itsXPos.get(row);
				} else if (col == 2) { // Y-coordinate
					return itsYPos.get(row);
				} else if (col == 3) { // Panel width
					return itsWidth.get(row);
				} else if (col == 4) { // Panel height
					return itsHeight.get(row);
				}
			} else if (itsAutoControl.isSelected()) {
				if (col == 1) {
					return "N/A";
				} else if (col == 2) {
					return "N/A";
				} else if (col == 3) {
					return "N/A";
				} else if (col == 4) {
					return "N/A";
				}
			}
			return "LayoutPanel:LayoutTableModel:getValueAt:Unknown";
		}

		public boolean isCellEditable(int row, int col) {
			if (col > 0) {
				return true;
			} else {
				return false; // Panel names not editable
			}
		}

		public Vector<MonPanel> getCurrentPanels() {
			itsCurrentPanels = itsParent.getPanels();
			return itsCurrentPanels;
		}

		// Called when a user edits a cell containing panel information
		public void setValueAt(Object value, int row, int col) {
			if (col == 1) { // X-coordinate
				int setvalue = Integer.parseInt((String) value);
				//System.out.printf("setValueAt: setvalue is %d\n", setvalue);
				itsXPos.set(row, setvalue);
			} else if (col == 2) { // Y-coordinate
				int setvalue = Integer.parseInt((String) value);
				//System.out.printf("setValueAt: setvalue is %d\n", setvalue);
				itsYPos.set(row, setvalue);
			} else if (col == 3) { // Panel width
				int setvalue = Integer.parseInt((String) value);
				//System.out.printf("setValueAt: setvalue is %d\n", setvalue);
				itsWidth.set(row, setvalue);
			} else if (col == 4) { // Panel height
				int setvalue = Integer.parseInt((String) value);
				//System.out.printf("setValueAt: setvalue is %d\n", setvalue);
				itsHeight.set(row,  setvalue);
			}
			repaint();
			fireTableCellUpdated(row, col);
		}

		public int[] getWindowSize() {
			itsWindowSize[0] = LayoutPanel.this.getWidth();
			itsWindowSize[1] = LayoutPanel.this.getHeight();
			//System.out.printf("getWindowSize %d %d", itsWindowSize[0], itsWindowSize[1]);
			return itsWindowSize;
		}
		
		// Offsets/dimensions for all current panels retrieved when a user
		// switches from Automatic to Manual mode
		public void getCoordinates() {
			// First get the current panels and window size
			getCurrentPanels();
			getWindowSize();
			
			for (int i = 0; i < itsCurrentPanels.size(); i++) { // For each panel
				MonPanel thisPanel = (MonPanel) itsCurrentPanels.get(i);
				double ten = 10.0;
				
				// Get panel dimensions in pixels
				int pixelX = thisPanel.getX();
				int pixelY = thisPanel.getY();
				int pixelWidth = thisPanel.getWidth();
				int pixelHeight = thisPanel.getHeight();
				
				// Convert to a percentage of the window size
				castToDouble(pixelX, pixelY, pixelWidth, pixelHeight);
				castToDouble(itsWindowSize[0],itsWindowSize[1]);
				
				double pcX = itsPanelDouble[0] / itsWindowSizeDouble[0];
				double pcY = itsPanelDouble[1] / itsWindowSizeDouble[1];
				double pcWidth = itsPanelDouble[2] / itsWindowSizeDouble[0];
				double pcHeight = itsPanelDouble[3] / itsWindowSizeDouble[1];
				
				// Convert to an integer value between 0-10 for placement on grid
				int gridX = 		(int) (pcX * ten);
				int gridY = 		(int) (pcY * ten);
				int gridWidth = 	(int) (pcWidth * ten);
				int gridHeight =	(int) (pcHeight * ten);

				itsXPos.add(i, gridX); // X-coordinate
				itsYPos.add(i, gridY); // Y-coordinate
				itsWidth.add(i, gridWidth); // Width
				itsHeight.add(i, gridHeight); // Height in pixels
			}
		}
		
		// Casts panel offsets/dimensions from integer to double
		public double[] castToDouble(int x, int y, int width, int height) {
			itsPanelDouble[0] = (double) x;
			itsPanelDouble[1] = (double) y;
			itsPanelDouble[2] = (double) width;
			itsPanelDouble[3] = (double) height;
			
			//System.out.printf("castToDouble %f %f %f %f\n", itsPanelDouble[0], itsPanelDouble[1], itsPanelDouble[2], itsPanelDouble[3]);

			return itsPanelDouble;
		}
		
		public double[] castToDouble(int width, int height) {
			itsWindowSizeDouble[0] = (double) width;
			itsWindowSizeDouble[1] = (double) height;
			
			return itsWindowSizeDouble;
		}
	}
	// ////////////////// END NESTED CLASS ///////////////////////

	// PreviewPanel provides a basic preview of how panels are placed on the
	// Display pane.
	class PreviewPanel extends JPanel {
		public PreviewPanel() {
		}

		public Dimension getPreferredSize() {
			return new Dimension(250, 300);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			double ten = 10.0;

			if (itsAutoControl.isSelected()) {
				g.drawString("Automatic control is selected.", 10, 20);
				g.drawString("Panels will be stacked vertically.", 10, 40);
				g.drawString("Switch to Manual layout control to", 10, 60);
				g.drawString("rearrange panels.", 10, 75);
			} else if (itsManualControl.isSelected()) {
				if (!itsBackwards) {
					itsTableModel.getCurrentPanels();
					
					int numPanels = itsTableModel.itsCurrentPanels.size();
					int previewWidth = this.getPreferredSize().width - 10;
					int previewHeight = this.getPreferredSize().height - 10;

					for (int i=0; i<numPanels; i++) {
						MonPanel thisPan = (MonPanel) itsTableModel.itsCurrentPanels.get(i);
						Class<? extends MonPanel> c = thisPan.getClass();
						String panelname = MonPanel.getName(c);
						
						double pX = (double)itsTableModel.itsXPos.get(i) / ten;
						double pY = (double)itsTableModel.itsYPos.get(i) / ten;
						double pW = (double)itsTableModel.itsWidth.get(i) / ten;
						double pH = (double)itsTableModel.itsHeight.get(i) / ten;

						int drawX = (int) (pX * previewWidth);
						int drawY = (int) (pY * previewHeight);
						int drawW = (int) (pW * previewWidth);
						int drawH = (int) (pH * previewHeight);

						g.drawRect(drawX, drawY, drawW, drawH);

						// Draw the class name inside the box
						Font cd = new Font("Helvetica", Font.PLAIN, 10);
						g.setFont(cd);
						g.drawString(panelname, drawX + 10, drawY + 10);
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
	/** Button for setting panel sizes. */
	protected JButton itsSetPanels = new JButton("OK");
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
	/**
	 * Used for backwards compatibility - prevent PreviewPanel from showing if
	 * older saved setup
	 */
	protected static boolean itsBackwards = true;

	protected static int itsPreferredHeight = 0;
	protected static int itsPreferredWidth = 0;

	/** Constructor. */
	public LayoutPanel(MonFrame parent) {
		itsParent = parent;
		setLayout(new GridBagLayout());
		JPanel contentPane = new JPanel(new GridBagLayout());
		int[] windowsize = getWindowSize();
		contentPane.setMinimumSize(new Dimension(windowsize[0], windowsize[1]));

		GridBagConstraints c = new GridBagConstraints();
		GridBagConstraints ccontp = new GridBagConstraints();
		GridBagConstraints cprevp = new GridBagConstraints();

		// Temporary layout panels
		JPanel contpanel = new JPanel(new GridBagLayout());
		JPanel prevpanel = new JPanel(new GridBagLayout());

		// Panel table
		itsTable = new JTable(itsTableModel);
		itsTableScrollPane = new JScrollPane(itsTable);
		itsTableScrollPane.setPreferredSize(new Dimension(270, 500));
		itsTableScrollPane.setMinimumSize(new Dimension(270, 500));

		/*
		// COMBO BOX
		JComboBox percentCombo = new JComboBox();
		percentCombo.addItem(50);
		percentCombo.addItem(100);
		DefaultCellEditor editor = new DefaultCellEditor(percentCombo);
		TableColumnModel tcm = itsTable.getColumnModel();
		tcm.getColumn(3).setCellEditor(editor);
		*/
		
		
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
		contpanel.setMinimumSize(new Dimension(240, 180));

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
		contpanel.add(new JLabel("Size of window (pixels): "), ccontp);
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
		prevpanel.setMinimumSize(new Dimension(250,300));
		cprevp.gridx = 0;
		cprevp.gridy = 0;
		cprevp.anchor = GridBagConstraints.SOUTH;
		cprevp.insets = new Insets(0, 0, 0, 5);
		prevpanel.add(new PreviewPanel(), c);

		itsSetPanels.addActionListener(this);
		itsSetPanels.setActionCommand("setpanels");

		// Layout of panes on main control panel
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		JLabel temp = new JLabel(
				"<html>Panel offsets and dimensions are represented in the table as a fraction<br> out of 10, of the specified window pixel dimensions.");
		temp.setMinimumSize(new Dimension(windowsize[0],50));
		c.insets = new Insets(0, 0, 15, 0);
		contentPane.add(temp, c);
		c.insets = new Insets(0, 0, 0, 0);
		c.gridwidth = 1;
		c.gridheight = 2;
		c.gridx = 0;
		c.gridy = 1;
		contentPane.add(itsTableScrollPane, c);
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 1;
		contentPane.add(contpanel, c);
		c.gridx = 1;
		c.gridy = 2;
		contentPane.add(prevpanel, c);
		c.gridx = 1;
		c.gridy = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LAST_LINE_END;
		c.insets = new Insets(20, 0, 0, 0);
		contentPane.add(itsSetPanels, c);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.fill = GridBagConstraints.BOTH;
		add(contentPane, c);

		itsParent.addComponentListener(this);
		
		itsAutoControl.doClick(); // Enable auto layout control
	}

	/**
	 * Called by the MonFrame when a panel is added or removed. This provides an
	 * opportunity for us to update the table display.
	 */
	public void update() {
		//System.out.println("LayoutPanel:update");
		itsTableModel.fireTableStructureChanged();
		if (itsAutoControl.isSelected()) {
			// Copy the current window size to the fields
			itsWindowWidth.setText("" + itsParent.getWidth());
			itsWindowHeight.setText("" + itsParent.getHeight());
		}
	}

	/** Populate dimension ArrayLists with default coordinates in Manual mode */
	public void defineCoordinates(int i) {
		int zero = 0;
		int ten = 10;
		itsTableModel.itsXPos.add(i, zero);
		itsTableModel.itsYPos.add(i, zero);
		itsTableModel.itsWidth.add(i, ten);
		itsTableModel.itsHeight.add(i, ten);

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
		} else if (cmd == "update") { // Update window size
			int newWidth = Integer.parseInt(itsWindowWidth.getText());
			int newHeight = Integer.parseInt(itsWindowHeight.getText());

			itsParent.setSize(new Dimension(newWidth, newHeight));
		} else if (cmd == "setpanels") {
			if (itsManualControl.isSelected()) {
				setPanels();
			} else {
				itsParent.showDisplayForced();
			}
		}
		repaint();
	}

	public void setPanels() {
		//System.out.println("LayoutPanel:setPanels");
		if (!itsBackwards) { // 
			if (itsManualControl.isSelected()) {
				itsTableModel.getCurrentPanels();
				itsParent.clearPanels();
				
				itsParent.setUpML();
				
				for (int i = 0; i < itsTableModel.itsCurrentPanels.size(); i++) {
					MonPanel panel = (MonPanel) itsTableModel.itsCurrentPanels.get(i);
					int x = itsTableModel.itsXPos.get(i);
					int y = itsTableModel.itsYPos.get(i);
					int width = itsTableModel.itsWidth.get(i);
					int height = itsTableModel.itsHeight.get(i);

					//System.out.printf("setPanels: %d %d %d %d\n", x, y, width, height);

					itsParent.redrawPanels(panel, x, y, width, height);

					validate();
					repaint();
				}
			}
		}

		itsParent.showDisplayForced();

	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			// Automatic to Manual transition
			itsTable.setVisible(true);
			itsTableModel.getCoordinates();
			itsBackwards = false;
			//System.out.println("NOT itsBackwards!");
		} else if (e.getStateChange() == ItemEvent.SELECTED) {
			// Manual to Automatic transition
			//System.out.println("Manual has been changed to auto mode");
			itsTableModel.getCurrentPanels();
			itsParent.clearPanels();
			
			itsParent.setUpMLAuto();
			
			for (int i = 0; i < itsTableModel.itsCurrentPanels.size(); i++) {
				MonPanel panel = (MonPanel) itsTableModel.itsCurrentPanels.get(i);
				itsParent.redrawPanelsAuto(panel);
			}

		}
	}

	/** Save a setup describing the selected layout control options. */
	public void getSetup(SavedSetup setup) {
		itsTableModel.getCurrentPanels();

		// Save Automatic/Manual layout preference
		if (itsManualControl.isSelected()) {
			setup.put("control", "manual");
			// Save individual panel dimensions
			for (int i = 0; i < itsTableModel.itsCurrentPanels.size(); i++) {
				setup.put("panelx" + i, itsTableModel.itsXPos.get(i).toString());
				setup.put("panely" + i, itsTableModel.itsYPos.get(i).toString());
				setup.put("panelwidth" + i, itsTableModel.itsWidth.get(i)
						.toString());
				setup.put("panelheight" + i, itsTableModel.itsHeight.get(i)
						.toString());
			}    
			// Save preferred window dimensions
	    setup.put("windowwidth", itsWindowWidth.getText());
	    setup.put("windowheight", itsWindowHeight.getText());
		} else if (itsAutoControl.isSelected()) {
		  //Commented as auto is implied if 'manual' not specified
			//setup.put("control", "auto");
		}

		return;
	}

	/**
	 * Configure the GUI to display the setup options described in the specified
	 * saved setup.
	 */
	public void loadSetup(SavedSetup setup) {
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
							Integer.parseInt(setup.get("panelx" + i)));
					itsTableModel.itsYPos.add(i,
							Integer.parseInt(setup.get("panely" + i)));
					itsTableModel.itsWidth.add(i,
							Integer.parseInt(setup.get("panelwidth" + i)));
					itsTableModel.itsHeight.add(i,
							Integer.parseInt(setup.get("panelheight" + i)));
				}

				itsTableModel.fireTableStructureChanged();
				itsTableModel.fireTableDataChanged();
			} else {
				itsAutoControl.doClick();
			}
			itsWindowWidth.setText(savedWidth);
			itsWindowHeight.setText(savedHeight);
		} else {
			itsBackwards = true; // Setup was saved previous to LayoutPanel
									// implementation; no layout information
									// saved
		}
		return;
	}

	public void resizeWindow() {
		if (!itsBackwards) {
			//System.out.printf("%d %d", itsPreferredWidth, itsPreferredHeight);
			itsParent.setSize(new Dimension(itsPreferredWidth,
					itsPreferredHeight));
			validate();
			repaint();
		}
	}

	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent arg0) {
		if (itsManualControl.isSelected()) {
			String width = Integer.toString(itsParent.getWidth());
			String height = Integer.toString(itsParent.getHeight());
			
			itsWindowWidth.setText(width);
			itsWindowHeight.setText(height);
		}
	}

	public void componentShown(ComponentEvent arg0) {
	}
	
	public int[] getWindowSize() {
		int[] size = new int[2];
		size[0] = Integer.parseInt(itsWindowWidth.getText());
		size[1] = Integer.parseInt(itsWindowHeight.getText());
		
		return size;
	}
}
