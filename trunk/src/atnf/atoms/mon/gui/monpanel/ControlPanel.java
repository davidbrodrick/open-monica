// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.TextAttribute;
import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.AuthenticationException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.DataMaintainer;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.SimpleTreeSelector;
import atnf.atoms.mon.gui.SimpleTreeSelector.SimpleTreeUtil;
import atnf.atoms.time.AbsTime;

/**
 * Class representing a control panel. Dynamically adds and removes controls in the setup phase
 * with a lot of customisability as to the layout and appearance.
 * Primarily used for pushing data out to control points.
 * 
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
public class ControlPanel extends MonPanel implements ActionListener, KeyListener, PointListener{

	private static final long serialVersionUID = -5900630541567847520L;

	private Integer numControls = 0;

	/** ArrayList holding references to the JComponents that make up the display in the ControlPanel*/
	private ArrayList<ControlPanelDisplayComponent> panelList = new ArrayList<ControlPanelDisplayComponent>();

	/** Array holding the values "Text Field", "Button" and "Checkbox".
	 *  Used for constant references to these Strings, and also
	 *	for the values in the "Control Type" JComboBoxes
	 */
	private final String[] controlOptions = {"Text Field", "Button", "Checkbox"};

	/** Array holding the values "Text", "Number" and "True/False".
	 *  Used for constant references to these Strings, and also
	 *	for the values in the "Control Type" JComboBoxes
	 */
	private final String[] dataOptions = {"Text", "Number", "True/False"};

	/** Array holding the values "Text" and "Number".
	 *  Used for constant references to these Strings, and also
	 *	for the values in the "Control Type" JComboBoxes when "Text Field"
	 *	is selected.
	 */
	private final String[] dataOptions2 = {"Text", "Number"};

	/** Array holding the values "Horizontal", "Vertical", "H-Readback" and "V-Readback".
	 *  Used for constant references to these Strings, and also
	 *	for the values in the "Layout" type JComboBox.
	 */
	private final String[] layoutOptions = {"Horizontal", "Vertical", "H-Readback", "V-Readback"};

	private String username = "";
	private String passwd = "";
	private String titleStr = "";
	private String layoutStr = "";


	static {
		MonPanel.registerMonPanel("Control Panel", ControlPanel.class);
	}

	// //// NESTED CLASS: ControlSetupPanel ///////
	/**
	 * Internal class that allows a user to set up and customise a ControlPanel as they would like
	 */
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener, ChangeListener{

		private static final long serialVersionUID = -3585682908515598074L;
		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		/** Panel for our persistent components. */
		private JPanel topPanel = new JPanel();
		/** JScrollPane for our main setup components. */
		private JScrollPane viewPane = new JScrollPane();

		private JLabel numberControlsLabel = new JLabel("Number of Controls: ");
		private JSpinner numberControls = new JSpinner();
		private SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);

		private JLabel titleLabel = new JLabel("Title: ");
		private JTextField title = new JTextField(15);

		private JLabel layoutLabel = new JLabel("Layout: ");
		private JComboBox layout;

		/** DocumentFilter used to ensure invalid values do not get entered into a number-only text field*/
		private NumberDocumentFilter ndoc = new NumberDocumentFilter();
		/** ArrayList holding all references to the dynamically created JComponents that make up this ControlSetupPanel*/
		private ArrayList<ControlPanelSetupComponent> componentList = new ArrayList<ControlPanelSetupComponent>();

		/**
		 * Constructor for a ControlSetupPanel
		 * @param panel The parent ControlPanel
		 * @param frame The JFrame that holds this MonPanel
		 */
		public ControlSetupPanel(ControlPanel panel, JFrame frame) {
			super(panel, frame);
			/*			if (panel.getPreferredSize().equals(frame.getPreferredSize())){
				this.setPreferredSize(frame.getPreferredSize());
			} else {
				this.setPreferredSize(panel.getPreferredSize());
			}*/

			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));			
			itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

			title.setText("Controls");
			title.setMaximumSize(new Dimension(50, 25));

			numberControls.setMaximumSize(new Dimension(45, 25));
			numberControls.setModel(spinModel);
			numberControls.addChangeListener(this);

			layout = new JComboBox(layoutOptions);
			layout.setEditable(false);
			layout.setMaximumSize(new Dimension(50, 25));

			numberControlsLabel.setAlignmentX(CENTER_ALIGNMENT);
			numberControls.setAlignmentX(CENTER_ALIGNMENT);
			topPanel.add(Box.createHorizontalGlue());
			topPanel.add(titleLabel);
			topPanel.add(title);
			topPanel.add(Box.createHorizontalGlue());
			topPanel.add(numberControlsLabel);
			topPanel.add(numberControls);
			topPanel.add(Box.createHorizontalGlue());
			topPanel.add(layoutLabel);
			topPanel.add(layout);
			topPanel.add(Box.createHorizontalGlue());

			this.add(topPanel, BorderLayout.NORTH);
			viewPane.getVerticalScrollBar().setUnitIncrement(16);
			viewPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			viewPane.setViewportView(itsMainPanel);
			this.add(viewPane, BorderLayout.CENTER);
			numberControls.setValue(1);
		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.ControlPanel");
			ss.setName("Control Panel");

			@SuppressWarnings ("unchecked")
			ArrayList<ControlPanelSetupComponent> pointInfo = (ArrayList<ControlPanelSetupComponent>) componentList.clone();

			String res = "";
			for (ControlPanelSetupComponent cpsc : pointInfo){
				if (cpsc.getTreeSelection() != null){
					res += cpsc.getTreeSelection() + ",";
					res += cpsc.getControlType() + ",";
					res += cpsc.getDataType() + ",";
					if (!cpsc.getControlName().equals("")) {
						res += cpsc.getControlName() + ",";
					} else {
						res += cpsc.getTreeSelection() + ",";
					}
					if (!cpsc.getLabelText().equals("")) {
						res += cpsc.getLabelText() + ",";
					} else {
						res += "\t" + ",";
					}
					if (!cpsc.getValueText().equals("")) {
						res += cpsc.getValueText() + ";";
					} else {
						res += "\t" + ";";
					}
				}
			}
			titleStr = title.getText();
			layoutStr = layout.getSelectedItem().toString();

			ss.put("points", res);
			ss.put("title", titleStr);
			ss.put("layout", layoutStr);
			ss.put("controls number", Integer.toString(numControls));
			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			// data validation and verification
			itsInitialSetup = setup;
			if (setup == null) {
				System.err.println("ControlSetupPanel:showSetup: Setup is NULL");
				return;
			}
			if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.ControlPanel")) {
				System.err.println("ControlSetupPanel:showSetup: Setup is for wrong class");
				return;
			}

			numControls = Integer.parseInt(itsInitialSetup.get("controls number"));
			numberControls.setValue(numControls);

			for (int i = 0; i < numControls; i++){
				addControlSetup();
			}

			layout.setSelectedItem(itsInitialSetup.get("layout"));
			if (layout.getSelectedItem().equals(layoutOptions[0])){
				this.setPreferredSize(new Dimension(numControls * 50, 180));
			} else {
				this.setPreferredSize(new Dimension(180, numControls * 30));
			}
			title.setText(itsInitialSetup.get("title"));

			String res = itsInitialSetup.get("points");
			StringTokenizer st = new StringTokenizer(res, ";");
			Vector<String> components = new Vector<String>();
			while (st.hasMoreTokens()){
				components.add(st.nextToken());
			}

			int n = 0;
			for (String s : components){
				st = new StringTokenizer(s, ",");

				String point = st.nextToken();
				String controlType = st.nextToken();
				String dataType = st.nextToken();
				String controlText = st.nextToken();
				String labelText = st.nextToken();
				String valueText = st.nextToken();

				ControlPanelSetupComponent cpsc = componentList.get(n);
				cpsc.setPointString(point);
				cpsc.setControlType(controlType);
				cpsc.setDataType(dataType);
				cpsc.setControlField(controlText);
				cpsc.setLabelField(labelText);
				cpsc.setValueText(valueText);

				n++;
			}
		}

		/**
		 * Method used to easily add new Control Setups to the ControlSetupPanel in the 
		 * correct configuration and with associated Listeners for the relevant JComponents
		 */
		private void addControlSetup(){
			JPanel bigPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;

			SimpleTreeSelector itsSimpleTreeSelector = new SimpleTreeSelector();
			itsSimpleTreeSelector.addChangeListener(this);
			JLabel selectedPointLabel = new JLabel("Selected Point: ");
			selectedPointLabel.setFont(new Font("Sans Serif", Font.ITALIC, 14));
			JScrollPane pointPane = new JScrollPane();
			pointPane.setBorder(null);
			JLabel selectedPoint = new JLabel("No Point Selected");
			selectedPoint.setFont(new Font("Sans Serif", Font.ITALIC | Font.BOLD, 14));
			JLabel controlTypeLabel = new JLabel("Control Type: ");
			JComboBox controlType = new JComboBox(controlOptions);
			controlType.addActionListener(this);
			controlType.setEditable(false);
			JLabel dataTypeLabel = new JLabel("Data Type: ");
			JComboBox dataType = new JComboBox(dataOptions2);
			dataType.setEditable(false);
			dataType.addActionListener(this);
			JLabel pointLabel = new JLabel("Control Name: ");
			JTextField pointField = new JTextField(10);
			JLabel displayLabel;
			if (controlType.getSelectedItem().equals(controlOptions[2])){
				displayLabel = new JLabel("Checkbox Text: ");
			} else {
				displayLabel = new JLabel("Button Text: ");
			}
			JTextField displayField = new JTextField(6);
			JLabel valueLabel = new JLabel("Value: ");
			JTextField valueField = new JTextField(4);
			JButton close = new JButton("X");
			close.setFont(new Font("Monospaced", Font.BOLD, 12));
			close.setBackground(new Color(0xCD0000));
			close.setForeground(Color.WHITE);
			close.addActionListener(this);

			gbc.gridheight = 4;
			gbc.gridwidth = 2;
			bigPanel.add(itsSimpleTreeSelector, gbc);

			gbc.insets = new Insets(30, 0, 0, 0);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridheight = 1;
			gbc.gridwidth = 4;
			gbc.gridx = 2;
			gbc.gridy = 1;
			selectedPointLabel.setHorizontalTextPosition(SwingConstants.CENTER);
			selectedPointLabel.setHorizontalAlignment(SwingConstants.CENTER);
			bigPanel.add(selectedPointLabel, gbc);

			gbc.gridy = 2;
			gbc.gridheight = 2;
			gbc.gridwidth = 4;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(0, 0, 30, 0);
			selectedPoint.setHorizontalTextPosition(SwingConstants.CENTER);
			selectedPoint.setHorizontalAlignment(SwingConstants.CENTER);
			pointPane.setViewportView(selectedPoint);
			bigPanel.add(pointPane, gbc);

			gbc.insets = new Insets(5,5,0,5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.gridx = 1;
			gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(controlTypeLabel, gbc);

			gbc.anchor = GridBagConstraints.CENTER;
			gbc.gridx = 2;
			bigPanel.add(controlType, gbc);

			gbc.gridx = 3;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(dataTypeLabel, gbc);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 4;
			gbc.gridwidth = 2;
			bigPanel.add(dataType, gbc);

			gbc.gridx = 0;
			gbc.gridy = 5;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(pointLabel, gbc);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 1;
			bigPanel.add(pointField, gbc);

			gbc.gridx = 2;
			gbc.gridy = 5;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(displayLabel, gbc);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 3;
			bigPanel.add(displayField, gbc);

			gbc.gridx = 4;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(valueLabel, gbc);
			valueLabel.setVisible(false);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 5;
			bigPanel.add(valueField, gbc);
			valueField.setVisible(false);

			gbc.insets = new Insets(0,0,0,0);
			gbc.anchor = GridBagConstraints.NORTHEAST;
			gbc.gridx = 6;
			gbc.gridy = 0;
			bigPanel.add(close, gbc);

			bigPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			itsMainPanel.add(bigPanel);
			componentList.add(new ControlPanelSetupComponent(bigPanel, itsSimpleTreeSelector, selectedPoint, controlType, dataType, pointField, displayLabel, displayField, valueField, valueLabel, close));
		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			if (arg0.getSource() instanceof JSpinner){
				JSpinner source = (JSpinner) arg0.getSource();
				if (source.getValue() instanceof Integer){
					if (source.getValue().equals(numControls)){
						return;
					} else if ((Integer)source.getValue() > numControls && ((Integer) source.getValue() - numControls) == 1){
						addControlSetup();
					} else if ((Integer) source.getValue() < numControls && ((Integer) source.getValue() - numControls) == -1){
						if (componentList.size() > 0){
							itsMainPanel.remove(componentList.get(componentList.size()-1).getContainer());
							componentList.get(componentList.size()-1).removeListeners(this);
							componentList.remove(componentList.size()-1);
						}
					} else {
						itsMainPanel = new JPanel();
						itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
						for (int i = 0; i < (Integer)source.getValue(); i++){
							addControlSetup();
						}
					}
					numControls = (Integer)source.getValue();
					itsMainPanel.revalidate();
					itsMainPanel.repaint();
					viewPane.setViewportView(itsMainPanel);
				}
			} else if (arg0.getSource() instanceof SimpleTreeUtil){
				SimpleTreeUtil source = (SimpleTreeUtil) arg0.getSource();
				for (ControlPanelSetupComponent c: componentList){
					if(source.equals(c.getTree().getTreeUtil())){
						c.setPointString();
						break;
					}
				}
			}
		}

		@Override
		public void actionPerformed(ActionEvent e){
			ControlPanelSetupComponent cspc = null;

			if (e.getSource() instanceof JButton){
				JButton source = (JButton) e.getSource();
				for (ControlPanelSetupComponent c: componentList){
					if(source.equals(c.getCloseButton())){
						itsMainPanel.remove(c.getContainer());
						c.removeListeners(this);
						cspc = c;
						numControls -=1;
						numberControls.setValue(numControls);
						break;
					}
				}
				if (cspc != null){
					componentList.remove(cspc);
					itsMainPanel.revalidate();
					itsMainPanel.repaint();
				} else {
					if (componentList.size() == 0){
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								JOptionPane.showMessageDialog(ControlSetupPanel.this, 
										"You do not have an input panel allocated.\n" +
										"Please set the spinner at the top of the Setup Panel to at least 1.",
										"No Input Panels Error", JOptionPane.ERROR_MESSAGE);
							}
						});
						return;
					} else {
						for (ControlPanelSetupComponent c : componentList){
							if (c.getTreeSelection() == null){
								SwingUtilities.invokeLater(new Runnable(){
									public void run(){
										JOptionPane.showMessageDialog(ControlSetupPanel.this, 
												"One or more of your Controls does not have a point selected.\n" +
												"Please select a data point to control, or remove a control input panel.", 
												"Missing Point Selection", JOptionPane.ERROR_MESSAGE);
									}
								});
								return;
							}
						}
					}
					for (ControlPanelSetupComponent c : componentList){
						if (c.getDataType().equals(dataOptions[1]) && c.getControlType().equals(controlOptions[1])){
							Pattern pattern = Pattern.compile("[0-9]*|\\.|[0-9]*\\.[0-9]*");
							Matcher matcher = pattern.matcher(c.getValueText());
							if (!matcher.matches() || c.getValueText().equals("")){
								SwingUtilities.invokeLater(new Runnable(){
									public void run(){
										JOptionPane.showMessageDialog(ControlSetupPanel.this, 
												"The data value set in the \"Value\" field is incompatible with the Data type you have selected.\n" +
												"Please set a valid value for the selected data type.",
												"Invalid Data Error", JOptionPane.ERROR_MESSAGE);
									}
								});
								return;
							}
						}
					}
					super.actionPerformed(e);
				}
			}

			if (e.getSource() instanceof JComboBox){
				JComboBox source = (JComboBox) e.getSource();
				for (ControlPanelSetupComponent c: componentList){
					if(source.equals(c.getControlBox())){
						if (c.getControlType().equals(controlOptions[2])){ // checkbox
							c.getDataBox().setModel(new DefaultComboBoxModel(dataOptions));
							c.getDataBox().setEnabled(false);
							c.setDataType(dataOptions[2]);
							c.setValueVis(false);
							c.setValueLabelVis(false);
							c.setLabel("Checkbox Text: ");
						} else {
							if (c.getControlType().equals(controlOptions[0])){ // text field
								c.getDataBox().setModel(new DefaultComboBoxModel(dataOptions2));
								c.setDataType(dataOptions2[0]);
								c.setValueVis(false);
								c.setValueLabelVis(false);
							} else if (c.getControlType().equals(controlOptions[1])){// button
								c.getDataBox().setModel(new DefaultComboBoxModel(dataOptions));
								c.setDataType(dataOptions[1]);
								c.setValueVis(true);
								c.setValueLabelVis(true);
							}
							c.setLabel("Button Text: ");
							c.getDataBox().setEnabled(true);
						}
						break;
					} else if (source.equals(c.getDataBox())){
						c.getValueField().setText("");
						AbstractDocument thisDoc = (AbstractDocument) c.getValueField().getDocument();
						if (source.getSelectedItem().equals(dataOptions[1])){
							thisDoc.setDocumentFilter(ndoc);
						} else {
							thisDoc.setDocumentFilter(null);
						}
					}
				}
			}
		}
	}

	// ///// END NESTED CLASS ///////

	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;

	JPanel itsMainPanel = new JPanel();
	JScrollPane itsScrollPane = new JScrollPane();
	Vector<String> components = new Vector<String>();
	boolean readback = false;
	/** Constructor. */
	public ControlPanel() {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
		JLabel tempLabel = new JLabel("Setup Not Configured. Hit the \"Control Panel\" tab to set up this panel.");
		itsMainPanel.add(tempLabel);
		itsScrollPane.setViewportView(itsMainPanel);
		this.add(itsScrollPane);
	}

	// ///// NESTED CLASS: ControlPanelSetupComponent ///////

	/**
	 * Class used to internally keep track of various elements used in setting up, interacting with and displaying the ControlSetupPanel
	 */
	public class ControlPanelSetupComponent{

		private JPanel container;
		private SimpleTreeSelector tree;
		private JLabel point;
		private JComboBox control;
		private JComboBox data;
		private JTextField controlName;
		private JLabel labelLabel;
		private JTextField label;
		private JTextField value;
		private JLabel valueLabel;
		private JButton closeButton;

		/**
		 * Constructor for a ControlPanelSetupComponent
		 * @param cont The JPanel container holding all the other elements
		 * @param sts The SimpleTreeSelector in this control setup
		 * @param pt The JLabel that displays which point is selected
		 * @param ct The JComboBox that displays the control type options
		 * @param dt The JComboBox that displays the data type options
		 * @param pf The JTextField that allows users to enter a name for the displayed Control
		 * @param lbl The JLabel for describing the location of the control mechanism label
		 * @param lb The JTextField that allows users to enter a label for the control mechanism (JButton, JCheckbox, JTextField)
		 * @param vf The JTextField that allow users to enter a value that the button-type control will push to the server
		 * @param vl The JLabel that is paired with the value JTextField
		 * @param close The JButton used to close a specific instance of a control setup
		 */
		public ControlPanelSetupComponent(JPanel cont, SimpleTreeSelector sts, JLabel pt, JComboBox ct, JComboBox dt, JTextField pf, JLabel lbl, JTextField lb, JTextField vf, JLabel vl, JButton close){
			this.container = cont;
			this.tree = sts;
			this.point = pt;
			this.control = ct;
			this.data = dt;
			this.controlName = pf;
			this.labelLabel = lbl;
			this.label = lb;
			this.value = vf;
			this.valueLabel = vl;
			this.closeButton = close;
		}

		/**
		 * Sets the JLabel text for the label descriptor
		 * @param string The String that the JLabel should be set to
		 */
		public void setLabel(String string) {
			this.labelLabel.setText(string);
		}

		/**
		 * Sets the text in the Value JTextField to the given parameter value
		 * @param valueText The value the JTextField should be set to
		 */
		public void setValueText(String valueText) {
			this.value.setText(valueText);
		}

		/**
		 * Returns a reference to the data type JComboBox
		 * @return the JComboBox reference
		 */
		public JComboBox getDataBox() {
			return data;
		}

		/**
		 * Returns a reference to the control type JComboBox
		 * @return the JComboBox reference
		 */
		public JComboBox getControlBox() {
			return control;
		}

		/**
		 * Returns a reference to the "X" JButton
		 * @return The "X" JButton reference
		 */
		public JButton getCloseButton() {
			return closeButton;
		}

		/**
		 * Returns the value stored in the Control name JTextField
		 * @return The String value in the JTextField
		 */
		public String getControlName(){
			return this.controlName.getText();
		}

		/**
		 * Sets the control name text field to the given parameter value
		 * @param controlText The value the control name JTextField should be set to
		 */
		public void setControlField(String controlText) {
			controlName.setText(controlText);
		}

		/**
		 * Sets the label text field to the given parameter value
		 * @param labelText The value the label JTextField should be set to
		 */
		public void setLabelField(String labelText) {
			label.setText(labelText);
		}

		/**
		 * Sets the selected item in the Data type JComboBox to the given parameter
		 * @param dataType The value the JComboBox should be set to
		 */
		public void setDataType(String dataType) {
			data.setSelectedItem(dataType);
		}

		/**
		 * Sets the selected item in the Control type JComboBox to the given parameter
		 * @param controlType The value the JComboBox should be set to
		 */
		public void setControlType(String controlType) {
			control.setSelectedItem(controlType);
		}

		/**
		 * Sets the selection on the SimpleTreeSelector and the internally selected point
		 * to the String parameter given
		 * @param point2 The full point name the selection should be set to
		 */
		public void setPointString(String point2) {
			Vector<String> selection = new Vector<String>();
			selection.add(point2);
			tree.setSelections(selection);
			point.setText(point2);
		}

		/**
		 * Returns a String representation of the selected point on
		 * the SimpleTreeSelector if one is selected, otherwise returns null.
		 */
		public String getTreeSelection(){
			Vector<String> selections = this.tree.getSelections();
			if (selections.size() != 1){
				return null;
			} else {
				return selections.get(0);
			}
		}
		/**
		 * Returns the reference to the SimpleTreeSelector of this control setup
		 * @return The SimpleTreeSelector reference
		 */
		public SimpleTreeSelector getTree(){
			return this.tree;
		}

		/**
		 * Sets the JLabel display of the selected point from the associated SimpleTreeSelector
		 */
		public void setPointString(){
			String pointString = this.getTreeSelection();
			if (pointString != null){
				point.setText(pointString);
			} else {
				point.setText("No Point Selected");
			}
		}

		/**
		 * Returns the value of the selected item in the control-type JComboBox
		 * @return String representation of the selected item
		 */
		public String getControlType(){
			return control.getSelectedItem().toString();
		}

		/**
		 * Returns the value of the selected item in the data-type JComboBox
		 * @return String representation of the selected item
		 */
		public String getDataType(){
			return data.getSelectedItem().toString();
		}

		/**
		 * Returns the contents of the label text field for this control
		 * @return The String representation of the JTextField contents
		 */
		public String getLabelText(){
			return label.getText();
		}

		/**
		 * Simple getter for the value held in the 
		 * value text field used with the button-type control
		 * @return the String representation of the value
		 */
		public String getValueText(){
			return value.getText();
		}

		/**
		 * Sets the Value text field visible or invisible
		 * @param b the visibility of the JTextField
		 */
		public void setValueVis(boolean b){
			value.setVisible(b);
		}

		/**
		 * Sets the Value label visible or invisible
		 * @param b the visibility of the JLabel
		 */
		public void setValueLabelVis(boolean b){
			valueLabel.setVisible(b);
		}

		/**
		 * Returns the referene to the JTextField for the button-type control.
		 * @return the JTextField object
		 */
		public JTextField getValueField(){
			return value;
		}
		/**
		 * Returns a reference to the JPanel container for this setup component
		 * @return The reference to the JPanel
		 */
		public JPanel getContainer(){
			return this.container;
		}

		/**
		 * Removes the various listeners from the components in this ControlPanelSetupComponent
		 * @param csp The parent ControlSetupPanel that implements ChangeListener and ActionListener
		 * @see ActionListener
		 * @see ChangeListener
		 */
		public void removeListeners(ControlSetupPanel csp){
			tree.removeChangeListener(csp);
			closeButton.removeActionListener(csp);
			control.removeActionListener(csp);
			data.removeActionListener(csp);
		}
	}
	// ///// END NESTED CLASS ///////

	// ///// NESTED CLASS: ControlPanelDisplayComponent ///////

	/**
	 * A class that encapsulates much of the data associated with displaying a control 
	 * in the "Display" tab of the MoniCA panel.
	 * 
	 */
	public class ControlPanelDisplayComponent{ //should probably have made this an abstract superclass, but for the moment this is fine - may be worth a refactor later.

		public final static int BUTTON_TYPE = 0x00;
		public final static int CHECKBOX_TYPE = 0x01;
		public final static int TEXT_TYPE = 0x02;

		private String point;
		private JButton button;
		private JCheckBox check;
		private JTextField value;
		private JButton confirm;
		private String dataType;
		private Object buttonValue;
		private int controlType;
		private JLabel readbackLb;

		private String name;
		private String label;

		/**
		 * Constructor for a Button type Display Component
		 * @param pt The point this component refers to, as a String
		 * @param jb The JButton reference this component incorporates
		 * @param bv The value that should be committed on each button press
		 * @param dt The data type of this component
		 */
		public ControlPanelDisplayComponent(String n, String pt, JButton jb, String bv, String dt, JLabel rb){
			this.point = pt;
			this.button = jb; 
			this.dataType = dt;
			if (dt.equals(dataOptions[0])) {
				// Text
				this.setButtonValue(bv);
			} else if (dt.equals(dataOptions[1])) {
				//Number
				try {
					this.setButtonValue(new Double(bv));
				} catch (NumberFormatException e) {
					System.err.println("ControlPanel: Error parsing number " + bv);
				}
			} else if (dt.equals(dataOptions[2])) {
				// True/False
				this.setButtonValue(new Boolean(bv));
			} else {
				System.err.println("ControlPanel: Unknown data type " + dt);
			}
			this.controlType = BUTTON_TYPE;
			this.dataType = dt;

			this.name = n;
			this.label = button.getText();

			if (rb != null){
				readbackLb = rb;
			}
		}

		/**
		 * Constructor for a Checkbox type Display Component
		 * @param pt The point this component refers to, as a String
		 * @param jc The JCheckbox reference this component incorporates
		 * @param dt The data type of this component
		 * @param conf The button used to confirm the checkbox selection
		 */
		public ControlPanelDisplayComponent(String n, String pt, String lb, JCheckBox jc, String dt, JButton conf, JLabel rb){
			this.point = pt;
			this.check = jc; 
			this.controlType = CHECKBOX_TYPE;
			this.dataType = dt;
			this.confirm = conf;

			this.name = n;
			this.label = lb;

			if (rb != null){
				readbackLb = rb;
			}
		}

		/**
		 * Constructor for a Text Field type Display Component
		 * @param pt The point this component refers to, as a String
		 * @param v The JTextField reference this component incorporates
		 * @param conf The button used to confirm the text field entry
		 * @param dt The data type of this component
		 */
		public ControlPanelDisplayComponent(String n, String pt, JTextField v, JButton conf, String dt, JLabel rb){
			this.point = pt;
			this.value = v; 
			this.controlType = TEXT_TYPE;
			this.dataType = dt;
			this.confirm = conf;

			this.name = n;
			this.label = confirm.getText();

			if (rb != null){
				readbackLb = rb;
			}
		}

		/**
		 * Method to return a reference to the Checkbox if this component includes one
		 * @return The JCheckBox reference
		 */
		public JCheckBox getCheckBox(){
			if (this.controlType == CHECKBOX_TYPE){
				return check;
			} else {
				return null;
			}
		}

		/**
		 * Method to return a formatted String of the type of data of this component;
		 * Text, Number or True/False
		 * @return the String containing the datatype
		 */
		public String getDataType(){
			return dataType;
		}

		/**
		 * Returns a formatted String containing the point name of the point associated with this control
		 * @return The name of the point
		 */
		public String getPoint(){
			return this.point;
		}

		/**
		 * Returns the JButton that sends data for this control. For a button-type control, this
		 * is the button itself. Otherwise, it is the "send" button.
		 * @return The JButton reference used for pushing data on press
		 */
		public JButton getConfirmButton(){
			if (this.controlType == BUTTON_TYPE){
				return button;
			} else {
				return confirm;
			}
		}

		/**
		 * Sets the associated value of the JButton in a button-type control
		 * @param buttonValue the value that should be pushed out when the JButton is pressed
		 */
		public void setButtonValue(Object bv) {
			this.buttonValue = bv;
		}

		/**
		 * Returns the contents for the value associated with the button in the relevant control type. If
		 * this isn't a button type control, then it returns a String containing a single tab character.
		 * @return The text associated with the button of this control if it is a button control, otherwise a String containing a single tab character.
		 */
		public Object getButtonValue() {
			if (this.controlType == BUTTON_TYPE){
				return buttonValue;
			} else {
				return "\t";
			}
		}

		/**
		 * Returns the int mask of this control type; either BUTTON_TYPE, TEXT_TYPE or CHECKBOX_TYPE
		 * @return the corresponding int mask
		 */
		public int getControlType(){
			return controlType;
		}

		/**
		 * Returns a string representation of the control type for this panel
		 * @return A String holding the value of the current control type, or null if it doesn't
		 * correspond to any of these.
		 */
		public String getControlTypeString(){
			if (getControlType() == ControlPanelDisplayComponent.BUTTON_TYPE){
				return "button";
			} else if (getControlType() == ControlPanelDisplayComponent.CHECKBOX_TYPE){
				return "checkbox";
			} else if (getControlType() == ControlPanelDisplayComponent.TEXT_TYPE){
				return "text";
			} else {
				return null;
			}
		}
		/**
		 * Returns the string contents for the text field in the relevant control type. If
		 * this isn't a text-field type control, then it returns a null value.
		 * @return The text contained in the text field of this control if it is a text-field control, otherwise null.
		 */
		public String getTextFieldContents(){
			if (controlType == TEXT_TYPE){
				return this.value.getText();
			} else {
				return null; //if this ever gets returned, we need to do a null check befor using it.
			}
		}

		/**
		 * Returns the actual JTextField used in TEXT_TYPE components
		 * @return The JTextField associated with this ControlPanelDisplayComponent
		 */
		public JTextField getTextField() {
			if (controlType == TEXT_TYPE){
				return this.value;
			} else {
				return null; //if this ever gets returned, we need to do a null check befor using it.
			}
		}

		/**
		 * Returns the name of this Control
		 * @return String name of this control
		 */
		public String getName(){
			return name;
		}

		/**
		 * Returns the label for either the Button or the checkbox
		 * @return A String of the label text
		 */
		public String getLabel(){
			return label;
		}

		/**
		 * if there is a readback JLabel assigned, set the text to the specified value
		 * @param text The String that the JLabel should say
		 */
		public void setReadbackText(final String text){
			if (readbackLb != null){
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						readbackLb.setText(text);
					}
				});
			}
		}

		/**
		 * Removes the listeners associated with the buttons in this ControlPanelDisplayComponent
		 * @param cp the parent ControlPanel that implements the ActionListener interface
		 * @see ActionListener
		 */
		public void removeListeners(ControlPanel cp){
			if (controlType == BUTTON_TYPE) button.removeActionListener(cp);
			if (controlType == TEXT_TYPE || controlType == CHECKBOX_TYPE) confirm.removeActionListener(cp);
		}
	}

	// ///// END NESTED CLASS ///////

	@Override
	public MonPanelSetupPanel getControls() {
		return new ControlSetupPanel(this, itsFrame);
	}

	@Override
	public boolean loadSetup(SavedSetup setup) {
		try {
			// check if the setup is suitable for our class
			if (!setup.checkClass(this)) {
				System.err.println("ControlPanel:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}
			itsSetup = setup;

			numControls = Integer.parseInt(itsSetup.get("controls number"));
			String layout = itsSetup.get("layout");
			String title = itsSetup.get("title");

			if (layout.equals(layoutOptions[0]) || layout.equals(layoutOptions[2])){
				itsMainPanel.setPreferredSize(new Dimension(numControls * 50, 180));
				//System.err.println("ControlPanel.loadSetup: Preferred size is " + (numControls * 50) + ", " + 180);
			} else {
				itsMainPanel.setPreferredSize(new Dimension(180, 50 + numControls * 30));
				//System.err.println("ControlPanel.loadSetup: Preferred size is " + 180 + ", " + numControls*30);
			}

			String res = itsSetup.get("points");
			StringTokenizer st = new StringTokenizer(res, ";");
			components = new Vector<String>();
			while (st.hasMoreTokens()){
				components.add(st.nextToken());
			}

			GridBagConstraints gbc = new GridBagConstraints();
			itsMainPanel = new JPanel(new BorderLayout());
			JPanel itsPanel = new JPanel(new GridBagLayout());
			JLabel titleLabel = new JLabel(title);
			titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
			Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
			fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_TWO_PIXEL);
			titleLabel.setFont(new Font("Sans Serif", Font.BOLD | Font.ITALIC, 24).deriveFont(fontAttributes));
			readback = false;
			if (layout.equals(layoutOptions[2]) || layout.equals(layoutOptions[3])){
				readback = true;
			}

			gbc.gridx = 0;
			gbc.gridy = 0;
			if (layout.equals(layoutOptions[1]) || layout.equals(layoutOptions[3])){ // how to appear if vertical layout is selected
				for (String s : components){ 
					if (gbc.gridy == components.size()-1){// fixes alignment for last line of grid, so it isn't massively spread out
						gbc.weighty = 1.0;
						gbc.anchor = GridBagConstraints.NORTH;
					} else {
						gbc.anchor = GridBagConstraints.CENTER;
					}
					gbc.insets = new Insets(10,30,0,0);
					gbc.gridx = 0;
					gbc.gridheight = 1;
					gbc.gridwidth = 1;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 0.001;

					st = new StringTokenizer(s, ","); // pick apart the string into its individual components
					ControlPanelDisplayComponent cpdc;
					String point = st.nextToken();
					String controlType = st.nextToken();
					String dataType = st.nextToken();
					String controlName = st.nextToken();
					String labelText = st.nextToken();
					String bValue = st.nextToken();
					String inputValue = null;

					if (readback){
						DataMaintainer.subscribe(point, this);
					}

					try {
						inputValue = st.nextToken();
					} catch (Exception e){
						// Button type control, so no original input
						// Alternatively, there is no data here since it isn't a "saved" setup
					} finally {
						// rest of this is just formatting and alignment
						JLabel itsName = new JLabel(controlName);
						fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
						itsName.setFont(new Font("Sans Serif", Font.BOLD | Font.ITALIC, 14).deriveFont(fontAttributes));
						itsPanel.add(itsName, gbc);

						JLabel readbackValue = null;
						if (readback){
							gbc.insets = new Insets(13,10,0,0);
							gbc.gridx = 1;
							JPanel readbackPanel = new JPanel(new GridLayout(1,2));
							JLabel readbackLabel = new JLabel("Value: ");
							readbackValue = new JLabel("N/D");
							readbackPanel.add(readbackLabel);
							readbackPanel.add(readbackValue);
							itsPanel.add(readbackPanel, gbc);
						}
						gbc.insets = new Insets(5,10,0,0);

						if (controlType.equals("Button")){
							JButton jb = new JButton(labelText);
							jb.addActionListener(this);
							gbc.gridx += 3;
							gbc.insets = new Insets(5,10,0,30);
							itsPanel.add(jb, gbc);
							cpdc = new ControlPanelDisplayComponent(itsName.getText(), point, jb, bValue, dataType, readbackValue);
						} else if (controlType.equals("Checkbox")){
							JCheckBox jc = new JCheckBox(labelText);
							JButton send = new JButton("Send");
							jc.setSelected(Boolean.parseBoolean(inputValue));
							send.addActionListener(this);
							gbc.gridx += 2;
							itsPanel.add(jc, gbc);
							gbc.gridx += 1;;
							gbc.insets = new Insets(5,10,0,30);
							itsPanel.add(send, gbc);
							cpdc = new ControlPanelDisplayComponent(itsName.getText(), point, labelText, jc, dataType, send, readbackValue);
						} else {
							JButton send = new JButton("Send");
							send.addActionListener(this);
							if (!labelText.equals("\t")){
								send.setText(labelText);
							}
							if (gbc.gridy == components.size()-1) gbc.insets = new Insets(11,10,0,0); //odd alignment issue for text field in last component
							JTextField tf = new JTextField(10);
							tf.setEditable(true);
							tf.setText(inputValue);
							tf.addKeyListener(this);
							gbc.gridx += 2;
							itsPanel.add(tf, gbc);
							gbc.gridx += 1;
							gbc.insets = new Insets(5,10,0,30);
							itsPanel.add(send, gbc);
							cpdc = new ControlPanelDisplayComponent(itsName.getText(), point, tf, send, dataType, readbackValue);
						}
						panelList.add(cpdc);
						gbc.gridy += 1;
					}
				}
			} else { // how to appear if horizontal alignment selected
				for (String s : components){
					gbc.anchor = GridBagConstraints.CENTER;
					gbc.gridy = 0;
					gbc.gridheight = 1;
					gbc.gridwidth = 1;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weighty = 0.001;
					gbc.insets = new Insets(5,5,5,5);

					st = new StringTokenizer(s, ","); // pick apart the string into its individual components
					ControlPanelDisplayComponent cpdc;
					String point = st.nextToken();
					String controlType = st.nextToken();
					String dataType = st.nextToken();
					String controlName = st.nextToken();
					String labelText = st.nextToken();
					String bValue = st.nextToken();
					String inputValue = null;

					if (readback){
						DataMaintainer.subscribe(point, this);
					}

					try {
						inputValue = st.nextToken();
					} catch (Exception e){
						// Button type control, so no original input
						// Alternatively, there is no data here since it isn't a "saved" setup
					} finally {

						//rest of this is mostly just formatting and alignment
						JLabel itsName = new JLabel(controlName);
						fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
						itsName.setFont(new Font("Sans Serif", Font.BOLD, 14).deriveFont(fontAttributes));
						itsPanel.add(itsName, gbc);
						JLabel readbackValue = null;
						if (readback){
							gbc.gridy = 1;
							JPanel readbackPanel = new JPanel(new GridLayout(1,2));
							JLabel readbackLabel = new JLabel("Value: ");
							readbackValue = new JLabel("N/D");
							readbackPanel.add(readbackLabel);
							readbackPanel.add(readbackValue);
							itsPanel.add(readbackPanel, gbc);
						}

						if (controlType.equals("Button")){
							JButton jb = new JButton(labelText);
							jb.addActionListener(this);
							gbc.gridy += 3;
							gbc.weighty = 1.0;
							gbc.anchor = GridBagConstraints.NORTH;
							itsPanel.add(jb, gbc);
							cpdc = new ControlPanelDisplayComponent(itsName.getText(), point, jb, bValue, dataType, readbackValue);
						} else if (controlType.equals("Checkbox")){
							JCheckBox jc = new JCheckBox(labelText);
							JButton send = new JButton("Send");
							jc.setSelected(Boolean.parseBoolean(inputValue));
							send.addActionListener(this);
							gbc.gridy += 2;
							itsPanel.add(jc, gbc);
							gbc.weighty = 1.0;
							gbc.gridy += 1;
							gbc.anchor = GridBagConstraints.NORTH;
							itsPanel.add(send, gbc);
							cpdc = new ControlPanelDisplayComponent(itsName.getText(), point, labelText, jc, dataType, send, readbackValue);
						} else {
							JButton send = new JButton("Send");
							if (!labelText.equals("\t")){
								send.setText(labelText);
							}
							JTextField tf = new JTextField(10);
							send.addActionListener(this);
							tf.setEditable(true);
							tf.setText(inputValue);
							tf.addKeyListener(this);
							gbc.gridy += 2;
							itsPanel.add(tf, gbc);
							gbc.gridy += 1;
							gbc.weighty = 1.0;
							gbc.anchor = GridBagConstraints.NORTH;
							itsPanel.add(send, gbc);
							cpdc = new ControlPanelDisplayComponent(itsName.getText(), point, tf, send, dataType, readbackValue);
						}
						panelList.add(cpdc);
						gbc.gridx += 1;
					}
				}
			}
			itsMainPanel.add(titleLabel, BorderLayout.NORTH);
			itsMainPanel.add(itsPanel, BorderLayout.CENTER);

			itsMainPanel.revalidate();
			itsMainPanel.repaint();
			itsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
			itsScrollPane.setViewportView(itsMainPanel);

		} catch (final Exception e) {
			e.printStackTrace();
			if (itsFrame != null) {
				JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
						+ "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
						JOptionPane.WARNING_MESSAGE);
			} else {
				System.err.println("ControlPanel:loadData: " + e.getClass().getName());
			}
			blankSetup();
			return false;
		}
		return true;
	}

	/**
	 * Resets the display panel to be a blank panel with no contents.
	 */
	private void blankSetup() {
		numControls = 0;
		itsMainPanel = new JPanel();
		itsScrollPane.setViewportView(itsMainPanel);
		itsSetup = null;
		for (ControlPanelDisplayComponent c : panelList){
			c.removeListeners(this);
		}
	}

	/** 
	 * @see atnf.atoms.mon.gui.MonPanel#getSetup()
	 */
	@Override 
	public synchronized SavedSetup getSetup() {
		if (itsSetup != null){
			SavedSetup newSetup = (SavedSetup) itsSetup.clone();
			String res = newSetup.get("points");
			StringTokenizer st = new StringTokenizer(res, ";");
			Vector<String> components = new Vector<String>();
			while (st.hasMoreTokens()){
				components.add(st.nextToken());
			}
			res = "";
			for (String s : components){
				String old = s;
				st = new StringTokenizer(s, ","); // pick apart the string into its individual components
				String point = st.nextToken();
				ControlPanelDisplayComponent thisComponent = null;
				for (ControlPanelDisplayComponent c : panelList){
					if (c.getPoint().equals(point)){
						thisComponent = c;
						break;
					}
				}
				if (thisComponent == null) throw new IllegalArgumentException();
				old += ",";
				if (thisComponent.getControlType() == ControlPanelDisplayComponent.CHECKBOX_TYPE){
					String bool = Boolean.toString(thisComponent.getCheckBox().isSelected());
					old += bool + ";";
				} else if (thisComponent.getControlType() == ControlPanelDisplayComponent.TEXT_TYPE){
					old += thisComponent.getTextFieldContents() + ";";
				} else {
					old += ";";
				}
				res += old;
			}
			newSetup.put("points", res);
			itsSetup = newSetup;
		}
		return itsSetup;
	}

	public void vaporise() {
		for (ControlPanelDisplayComponent c : panelList){
			c.removeListeners(this);
		}
		DataMaintainer.unsubscribe(components, this);
		panelList = new ArrayList<ControlPanelDisplayComponent>();
		itsMainPanel.removeAll();
		itsMainPanel.revalidate();
		itsMainPanel.repaint();
	}

	public void export(PrintStream p) {
		String out = "";
		for (ControlPanelDisplayComponent c : panelList){
			out += "Point: " + c.getPoint() + ", \n";
			out += "Name: " + c.getName() + ", \n";
			out += "Data Type: " + c.getDataType() + ", \n";
			out += "Control Type: " + c.getControlTypeString() + ", \n";
			out += "Button Value: " + c.getButtonValue() + ", \n";
			out += "\n\n";
		}
		p.println(out);
	}

	public String getLabel() {
		return "Control Panel";
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() instanceof JButton){
			JButton source = (JButton) arg0.getSource();

			for (ControlPanelDisplayComponent c : panelList){
				if (source.equals(c.getConfirmButton())){
					String[] creds = MonClientUtil.showLogin(this, username, passwd);
					if (creds != null){
						username = creds[0];
						passwd = creds[1];
						if (username.isEmpty() || username == null || passwd.isEmpty() || passwd == null){
							passwd = ""; 
							return;
						}
						new DataSender(c).start(); // Start sending thread
						return;
					}
				}
			}
		}
	}

	// ///// NESTED CLASS: DataSender ///////
	/**
	 * Worker class for sending data to the server, so the UI doesn't hang waiting for a server
	 * response.
	 */
	public class DataSender extends Thread implements Runnable{

		ControlPanelDisplayComponent c;

		/**
		 * Constructs a new DataSender thread
		 * @param cpdc The ControlPanelDisplayComponent that contains the data needing to be sent
		 */
		public DataSender(ControlPanelDisplayComponent cpdc){
			c = cpdc;
		}

		@Override
		public void run(){
			try{
				MoniCAClient server = MonClientUtil.getServer();
				PointData data = null;
				if (c.getControlType() == ControlPanelDisplayComponent.BUTTON_TYPE){
					Object value = c.getButtonValue();
					if (!value.equals("\t")){
						data = new PointData(c.getPoint(), AbsTime.factory(), value);
					}
				} else if (c.getControlType() == ControlPanelDisplayComponent.CHECKBOX_TYPE){
					JCheckBox cb = c.getCheckBox();
					if (cb != null){
						boolean state = cb.isSelected();
						Boolean value;
						if (state){
							value = true;
						} else {
							value = false;
						}
						data = new PointData(c.getPoint(), AbsTime.factory(), value);
					}
				} else {
					String value = c.getTextFieldContents();
					if (value != null){
						if (c.getDataType().equals(dataOptions[1])){ //make sure we're actually sending a number
							Pattern pattern = Pattern.compile("[-|+]{0,1}[0-9]*|\\.|[-|+]{0,1}[0-9]*\\.[0-9]*");
							Matcher matcher = pattern.matcher(value);
							if (matcher.matches()){
								data = new PointData(c.getPoint(), AbsTime.factory(), new Double(value));
							} else {
								throw (new InvalidParameterException());
							}
						} else if (c.getDataType().equals(dataOptions[2])){
							value = value.toLowerCase(Locale.ENGLISH);
							Pattern pattern = Pattern.compile("true|false");
							Matcher matcher = pattern.matcher(value);
							if (matcher.matches()){
								data = new PointData(c.getPoint(), AbsTime.factory(), new Boolean(value));
							} else {
								throw (new InvalidParameterException());
							}
						} else {
							data = new PointData(c.getPoint(), AbsTime.factory(), value);
						}
					}
				}
				boolean res = server.setData(c.getPoint(), data, username, passwd);
				if (!res) throw (new AuthenticationException());
			} catch (Exception e){
				passwd = "";
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						JOptionPane.showMessageDialog(ControlPanel.this, "Something went wrong with the sending of data. " +
								"\nPlease ensure that you're properly connected to the network, you are attempting to \n" +
								"write to a valid data-type to the point and your username and password are correct.", 
								"Data Sending Error", JOptionPane.ERROR_MESSAGE);
					}
				});
				return;
			}
		}
	}
	// ///// END NESTED CLASS ///////

	// ///// NESTED CLASS: NumberDocumentFilter ///////
	/**
	 * Document Filter designed to only allow the entry of characters that adhere to the 
	 * regex <strong><code>([0-9]*|\.|[0-9]*\.[0-9]*)</code></strong>, i.e. only characters that can be used to form 
	 * valid floating point numbers.
	 */
	public class NumberDocumentFilter extends DocumentFilter{
		private boolean filter = true;

		/**
		 * Method to set this NumberDocumentFilter to actively filter or not.
		 * @param state Boolean indicating whether to enable the NumberDocumentFilter
		 */
		public void setFilterState(boolean state){
			this.filter = state;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
			if (filter) {
				Pattern pattern = Pattern.compile("[-|+]{0,1}[0-9]*|\\.|[-|+]{0,1}[0-9]*\\.[0-9]*");
				Matcher matcher = pattern.matcher(text);
				if (matcher.matches()){
					super.insertString(fb, offset, text, attr);
				} 
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (filter) {
				Pattern pattern = Pattern.compile("[-|+]{0,1}[0-9]*|\\.|[-|+]{0,1}[0-9]*\\.[0-9]*");
				Matcher matcher = pattern.matcher(text);
				if (matcher.matches()){
					super.replace(fb, offset, length, text, attrs);
				}
			}
		}
	}
	// ///// END NESTED CLASS ///////

	@Override
	public void onPointEvent(Object source, PointEvent evt) {
		if (readback){
			PointData pd = evt.getPointData();
			for (ControlPanelDisplayComponent c: panelList){
				if (pd.getName().equals(c.getPoint())){
					c.setReadbackText(pd.getData().toString());
				}
			}
		}

	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		if (arg0.getKeyCode() == KeyEvent.VK_ENTER){
			if (arg0.getSource() instanceof JTextField){
				JTextField source = (JTextField) arg0.getSource();
				for (ControlPanelDisplayComponent c : panelList){
					if (source.equals(c.getTextField())){
						c.getConfirmButton().doClick();
						return;
					}
				}
			}
		}

	}

	@Override
	public void keyReleased(KeyEvent arg0) {}
	@Override
	public void keyTyped(KeyEvent arg0) {}
}