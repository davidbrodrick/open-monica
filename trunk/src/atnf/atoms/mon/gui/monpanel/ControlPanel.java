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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.SimpleTreeSelector;
import atnf.atoms.mon.gui.SimpleTreeSelector.SimpleTreeUtil;
import atnf.atoms.time.AbsTime;

/**
 * Class representing a control panel.
 * 
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
public class ControlPanel extends MonPanel implements ActionListener{
	/**
	 * 
	 */
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

	/** Array holding the values "Horizontal" and "Vertical".
	 *  Used for constant references to these Strings, and also
	 *	for the values in the "Layout" type JComboBox.
	 */
	private final String[] layoutOptions = {"Horizontal", "Vertical"};

	private String username = "";
	private String passwd = "";


	static {
		MonPanel.registerMonPanel("Control Panel", ControlPanel.class);
	}

	// //// NESTED CLASS: ControlSetupPanel ///////
	/**
	 * Internal class that allows a user to set up and customise a ControlPanel as they would like
	 */
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener, ChangeListener{

		/**
		 * 
		 */
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
		private JTextField title = new JTextField(10);

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
			viewPane.setViewportView(itsMainPanel);
			this.add(viewPane, BorderLayout.CENTER);

			if (itsInitialSetup != null) {
				showSetup(itsInitialSetup);
			}
		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.ControlPanel");
			ss.setName("controlSetup");

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

			ss.put("points", res);
			ss.put("title", title.getText());
			ss.put("layout", layout.getSelectedItem().toString());
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
				String labelText = st.nextToken();
				String valueText = st.nextToken();

				ControlPanelSetupComponent cpsc = componentList.get(n);
				cpsc.setPointString(point);
				cpsc.setControlType(controlType);
				cpsc.setDataType(dataType);
				cpsc.setTextField(labelText);
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
			selectedPointLabel.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			JLabel selectedPoint = new JLabel("No Point Selected");
			selectedPoint.setFont(new Font("Sans Serif", Font.ITALIC | Font.BOLD, 24));
			JLabel controlTypeLabel = new JLabel("Control Type: ");
			JComboBox controlType = new JComboBox(controlOptions);
			controlType.addActionListener(this);
			controlType.setEditable(false);
			JLabel dataTypeLabel = new JLabel("Data Type: ");
			JComboBox dataType = new JComboBox(dataOptions);
			dataType.setEditable(false);
			dataType.addActionListener(this);
			JLabel pointLabel = new JLabel("Control Name: ");
			JTextField pointField = new JTextField(10);
			JLabel displayLabel = new JLabel("Label: ");
			JTextField displayField = new JTextField(10);
			JLabel valueLabel = new JLabel("Value: ");
			JTextField valueField = new JTextField(10);
			JButton close = new JButton("X");
			close.setFont(new Font("Monospaced", Font.BOLD, 12));
			close.setBackground(new Color(0xCD0000));
			close.setForeground(Color.WHITE);
			close.addActionListener(this);

			gbc.gridheight = 4;
			gbc.gridwidth = 3;
			bigPanel.add(itsSimpleTreeSelector, gbc);

			gbc.insets = new Insets(30, 0, 0, 0);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridheight = 1;
			gbc.gridwidth = 2;
			gbc.gridx = 3;
			gbc.gridy = 1;
			bigPanel.add(selectedPointLabel, gbc);

			gbc.gridy = 2;
			gbc.insets = new Insets(0, 0, 30, 0);
			bigPanel.add(selectedPoint, gbc);

			gbc.insets = new Insets(5,0,0,0);
			gbc.gridwidth = 1;
			gbc.gridx = 1;
			gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(controlTypeLabel, gbc);

			gbc.anchor = GridBagConstraints.CENTER;
			gbc.gridx = 2;
			bigPanel.add(controlType, gbc);

			gbc.gridx = 3;
			bigPanel.add(dataTypeLabel, gbc);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 4;
			bigPanel.add(dataType, gbc);

			gbc.gridx = 0;
			gbc.gridy = 5;
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
			gbc.gridx = 5;
			gbc.gridy = 0;
			bigPanel.add(close, gbc);

			bigPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			itsMainPanel.add(bigPanel);
			componentList.add(new ControlPanelSetupComponent(bigPanel, itsSimpleTreeSelector, selectedPoint, controlType, dataType, pointField, displayField, valueField, valueLabel, close));
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
						JOptionPane.showMessageDialog(this, 
								"You do not have an input panel allocated.\n" +
								"Please set the spinner at the top of the Setup Panel to at least 1.",
								"No Input Panels Error", JOptionPane.ERROR_MESSAGE);
						return;
					} else {
						for (ControlPanelSetupComponent c : componentList){
							if (c.getTreeSelection() == null){
								JOptionPane.showMessageDialog(this, 
										"One or more of your Controls does not have a point selected.\n" +
										"Please select a data point to control, or remove a control input panel.", 
										"Missing Point Selection", JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
					}
					for (ControlPanelSetupComponent c : componentList){
						if (c.getDataType().equals(dataOptions[1])){
							Pattern pattern = Pattern.compile("[0-9]*|\\.|[0-9]*\\.[0-9]*");
							Matcher matcher = pattern.matcher(c.getValueText());
							if (!matcher.matches() || c.getValueText().equals("")){
								System.out.println("Error in Number");
								JOptionPane.showMessageDialog(this, 
										"The data value set in the \"Value\" field is incompatible with the Data type you have selected.\n" +
										"Please set a valid value for the selected data type.",
										"Invalid Data Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
						} else if (c.getDataType().equals(dataOptions[2])){
							Pattern pattern = Pattern.compile("[true|false\\.*]*");
							Matcher matcher = pattern.matcher(c.getValueText().toLowerCase());
							if (!matcher.matches()){
								System.out.println("Error in True/False");
								JOptionPane.showMessageDialog(this, 
										"The data value set in the \"Value\" field is incompatible with the Data type you have selected.\n" +
										"Please set a valid value for the selected data type.",
										"Invalid Data Error", JOptionPane.ERROR_MESSAGE);
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
						if (c.getControlType().equals(controlOptions[2])){ // true/false
							c.getDataBox().setEnabled(false);
							c.setDataType(dataOptions[2]);
							c.setValueVis(false);
							c.setValueLabelVis(false);
						} else {
							if (c.getControlType().equals(controlOptions[0])){ // text field
								c.setDataType(dataOptions[0]);
								c.setValueVis(false);
								c.setValueLabelVis(false);
							} else if (c.getControlType().equals(controlOptions[1])){// button
								c.setDataType(dataOptions[1]);
								c.setValueVis(true);
								c.setValueLabelVis(true);
							}
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
	/** Constructor. */
	public ControlPanel() {
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
		 * @param lb The JTextField that allows users to enter a label for the control mechanism (JButton, JCheckbox, JTextField)
		 * @param vf The JTextField that allow users to enter a value that the button-type control will push to the server
		 * @param vl The JLabel that is paired with the value JTextField
		 * @param close The JButton used to close a specific instance of a control setup
		 */
		public ControlPanelSetupComponent(JPanel cont, SimpleTreeSelector sts, JLabel pt, JComboBox ct, JComboBox dt, JTextField pf, JTextField lb, JTextField vf, JLabel vl, JButton close){
			this.container = cont;
			this.tree = sts;
			this.point = pt;
			this.control = ct;
			this.data = dt;
			this.controlName = pf;
			this.label = lb;
			this.value = vf;
			this.valueLabel = vl;
			this.closeButton = close;
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
		 * Sets the label text field to the given parameter value
		 * @param labelText The value the label JTextField should be set to
		 */
		public void setTextField(String labelText) {
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
		private String buttonValue;
		private int controlType;

		/**
		 * Constructor for a Button type Display Component
		 * @param pt The point this component refers to, as a String
		 * @param jb The JButton reference this component incorporates
		 * @param bv The value that should be committed on each button press
		 * @param dt The data type of this component
		 */
		public ControlPanelDisplayComponent(String pt, JButton jb, String bv, String dt){
			this.point = pt;
			this.button = jb; 
			this.setButtonValue(bv);
			this.controlType = BUTTON_TYPE;
			this.dataType = dt;
		}

		/**
		 * Constructor for a Checkbox type Display Component
		 * @param pt The point this component refers to, as a String
		 * @param jc The JCheckbox reference this component incorporates
		 * @param dt The data type of this component
		 * @param conf The button used to confirm the checkbox selection
		 */
		public ControlPanelDisplayComponent(String pt, JCheckBox jc, String dt, JButton conf){
			this.point = pt;
			this.check = jc; 
			this.controlType = CHECKBOX_TYPE;
			this.dataType = dt;
			this.confirm = conf;
		}

		/**
		 * Constructor for a Text Field type Display Component
		 * @param pt The point this component refers to, as a String
		 * @param v The JTextField reference this component incorporates
		 * @param conf The button used to confirm the text field entry
		 * @param dt The data type of this component
		 */
		public ControlPanelDisplayComponent(String pt, JTextField v, JButton conf, String dt){
			this.point = pt;
			this.value = v; 
			this.controlType = TEXT_TYPE;
			this.dataType = dt;
			this.confirm = conf;
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
		public void setButtonValue(String bv) {
			this.buttonValue = bv;
		}

		/**
		 * Returns the string contents for the value associated with the button in the relevant control type. If
		 * this isn't a button type control, then it returns a null value.
		 * @return The text associated with the button of this control if it is a button control, otherwise null.
		 */
		public String getButtonValue() {
			if (this.controlType == BUTTON_TYPE){
				return buttonValue;
			} else {
				return null;
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

	public MonPanelSetupPanel getControls() {
		return new ControlSetupPanel(this, itsFrame);
	}

	public boolean loadSetup(SavedSetup setup) {
		try {
			// check if the setup is suitable for our class
			if (!setup.checkClass(this)) {
				System.err.println("ControlPanel:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}

			SavedSetup itsInitialSetup = setup;

			numControls = Integer.parseInt(itsInitialSetup.get("controls number"));
			String layout = itsInitialSetup.get("layout");
			String title = itsInitialSetup.get("title");


			String res = itsInitialSetup.get("points");
			StringTokenizer st = new StringTokenizer(res, ";");
			Vector<String> components = new Vector<String>();
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

			gbc.gridx = 0;
			gbc.gridy = 0;
			if (layout.equals(layoutOptions[0])){ // how to appear if horizontal layout is selected

				for (String s : components){ // fixes alignment for last line of grid, so it isn't massively spread out
					if (gbc.gridy == components.size()-1){
						gbc.weighty = 1.0;
						gbc.anchor = GridBagConstraints.NORTH;
					}
					gbc.gridx = 0;
					gbc.gridheight = 1;
					gbc.gridwidth = 1;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.weightx = 0.001;
					gbc.insets = new Insets(5,30,0,0);

					st = new StringTokenizer(s, ","); // pick apart the string into its individual components
					ControlPanelDisplayComponent cpdc;
					String point = st.nextToken();
					String controlType = st.nextToken();
					String dataType = st.nextToken();
					String controlName = st.nextToken();
					String labelText = st.nextToken();
					String bValue = st.nextToken();

					// rest of this is just formatting and alignment

					JLabel itsName = new JLabel(controlName);
					fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
					itsName.setFont(new Font("Sans Serif", Font.BOLD | Font.ITALIC, 14).deriveFont(fontAttributes));


					itsPanel.add(itsName, gbc);
					gbc.insets = new Insets(5,10,0,0);

					if (controlType.equals("Button")){
						JButton jb = new JButton(labelText);
						jb.addActionListener(this);
						gbc.gridx = 3;
						gbc.insets = new Insets(5,10,0,30);
						itsPanel.add(jb, gbc);
						cpdc = new ControlPanelDisplayComponent(point, jb, bValue, dataType);
					} else if (controlType.equals("Checkbox")){
						JCheckBox jc = new JCheckBox(labelText);
						JButton send = new JButton("Send");
						send.addActionListener(this);
						gbc.gridx = 2;
						itsPanel.add(jc, gbc);
						gbc.gridx = 3;
						gbc.insets = new Insets(5,10,0,30);
						itsPanel.add(send, gbc);
						cpdc = new ControlPanelDisplayComponent(point, jc, dataType, send);
					} else {
						JLabel tfl;
						if (labelText.equals("\t")){
							tfl = new JLabel(labelText);
						} else {
							tfl = new JLabel(labelText + ": ");
						}
						tfl.setHorizontalAlignment(SwingConstants.RIGHT);
						JTextField tf = new JTextField(10);
						JButton send = new JButton("Send");
						send.addActionListener(this);
						tf.setEditable(true);
						gbc.gridx = 1;
						itsPanel.add(tfl, gbc);
						gbc.gridx = 2;
						itsPanel.add(tf, gbc);
						gbc.gridx = 3;
						gbc.insets = new Insets(5,10,0,30);
						itsPanel.add(send, gbc);
						cpdc = new ControlPanelDisplayComponent(point, tf, send, dataType);

					}
					panelList.add(cpdc);

					gbc.gridy += 1;
				}
			} else { // how to appear if vertical alignment selected
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

					//rest of this is mostly just formatting and alignment

					JLabel itsName = new JLabel(controlName);
					fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
					itsName.setFont(new Font("Sans Serif", Font.BOLD, 14).deriveFont(fontAttributes));

					itsPanel.add(itsName, gbc);

					if (controlType.equals("Button")){
						JButton jb = new JButton(labelText);
						jb.addActionListener(this);
						gbc.gridy = 3;
						gbc.weighty = 1.0;
						gbc.anchor = GridBagConstraints.NORTH;
						itsPanel.add(jb, gbc);
						cpdc = new ControlPanelDisplayComponent(point, jb, bValue, dataType);
					} else if (controlType.equals("Checkbox")){
						JCheckBox jc = new JCheckBox(labelText);
						JButton send = new JButton("Send");
						send.addActionListener(this);
						gbc.gridy = 2;
						itsPanel.add(jc, gbc);
						gbc.weighty = 1.0;
						gbc.gridy = 3;
						gbc.anchor = GridBagConstraints.NORTH;
						itsPanel.add(send, gbc);
						cpdc = new ControlPanelDisplayComponent(point, jc, dataType, send);
					} else {
						JLabel tfl;
						if (labelText.equals("\t")){
							tfl = new JLabel(labelText);
						} else {
							tfl = new JLabel(labelText + ": ");
						}
						tfl.setVerticalAlignment(SwingConstants.BOTTOM);
						JTextField tf = new JTextField(10);
						JButton send = new JButton("Send");
						send.addActionListener(this);
						tf.setEditable(true);
						gbc.gridy = 1;
						itsPanel.add(tfl, gbc);
						gbc.gridy = 2;
						itsPanel.add(tf, gbc);
						gbc.gridy = 3;
						gbc.weighty = 1.0;
						gbc.anchor = GridBagConstraints.NORTH;
						itsPanel.add(send, gbc);
						cpdc = new ControlPanelDisplayComponent(point, tf, send, dataType);

					}

					panelList.add(cpdc);

					gbc.gridx += 1;
				}
			}

			itsMainPanel.add(titleLabel, BorderLayout.NORTH);
			itsMainPanel.add(itsPanel, BorderLayout.CENTER);


			itsMainPanel.revalidate();
			itsMainPanel.repaint();
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

	private void blankSetup() {
		//TODO
	}

	public synchronized SavedSetup getSetup() {
		return itsSetup;
	}

	public void vaporise() {
		for (ControlPanelDisplayComponent c : panelList){
			c.removeListeners(this);
		}

		panelList = new ArrayList<ControlPanelDisplayComponent>();
		itsMainPanel.removeAll();
		itsMainPanel.revalidate();
		itsMainPanel.repaint();
	}

	public void export(PrintStream p) {
		//TODO
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

					if (username.equals("") || passwd.equals("")){
						JPanel inputs = new JPanel();
						inputs.setLayout(new GridBagLayout());
						GridBagConstraints gbc = new GridBagConstraints();
						gbc.fill = GridBagConstraints.HORIZONTAL;
						gbc.weightx = 0.5;
						gbc.gridx = 0;
						gbc.gridy = 0;
						JLabel usernameLabel = new JLabel("Username: ");
						JTextField usernameField = new JTextField(20);
						usernameField.setText(username);
						inputs.add(usernameLabel, gbc);
						gbc.gridx = 1;
						gbc.gridwidth = 3;
						inputs.add(usernameField, gbc);
						JLabel passwordLabel = new JLabel("Password: ");
						JPasswordField passwordField = new JPasswordField(20);
						gbc.gridx = 0;
						gbc.gridy = 1;
						gbc.gridwidth = 1;
						inputs.add(passwordLabel, gbc);
						gbc.gridwidth = 3;
						gbc.gridx = 1;
						inputs.add(passwordField, gbc);

						int result = JOptionPane.showConfirmDialog(this, inputs, "Authentication", JOptionPane.OK_CANCEL_OPTION);

						if (result == JOptionPane.OK_OPTION){
							username = usernameField.getText();
							passwd = new String(passwordField.getPassword());

						}
					}


					try{
						MoniCAClient server = MonClientUtil.getServer();
						PointData data = null;
						if (c.getControlType() == ControlPanelDisplayComponent.BUTTON_TYPE){
							String value = c.getButtonValue();
							if (value != null){
								if (c.getDataType().equals(dataOptions[1])){ //increment the current value with the stored number
									int intValue = Integer.parseInt(value);
									Integer currValue = (Integer) server.getData(c.getPoint()).getData();
									intValue = currValue + intValue;
									value = Integer.toString(intValue);
								}
								data = new PointData(c.getPoint(), AbsTime.factory(), value);
							}
						} else if (c.getControlType() == ControlPanelDisplayComponent.CHECKBOX_TYPE){

							JCheckBox cb = c.getCheckBox();
							if (cb != null){
								boolean state = cb.isSelected();
								String value;
								if (state){
									value = "true";
								} else {
									value = "false";
								}
								data = new PointData(c.getPoint(), AbsTime.factory(), value);
							}
						} else {
							String value = c.getTextFieldContents().toLowerCase(Locale.ENGLISH);
							if (value != null){
								if (c.getDataType().equals(dataOptions[1])){ //make sure we're actually sending a number
									Pattern pattern = Pattern.compile("[-|+]{0,1}[0-9]*|\\.|[-|+]{0,1}[0-9]*\\.[0-9]*");
									Matcher matcher = pattern.matcher(value);
									if (matcher.matches()){
										data = new PointData(c.getPoint(), AbsTime.factory(), value);
									} else {
										throw (new Exception());
									}
								} else if (c.getDataType().equals(dataOptions[2])){
									Pattern pattern = Pattern.compile("true|false");
									Matcher matcher = pattern.matcher(value);
									if (matcher.matches()){
										data = new PointData(c.getPoint(), AbsTime.factory(), value);
									} else {
										throw (new Exception());
									}
								} else {
									data = new PointData(c.getPoint(), AbsTime.factory(), value);
								}
							}
						}


						boolean success = server.setData(c.getPoint(), data, username, passwd);
						// does it block here? if so, following line is fine
						if (success == false) throw (new Exception());
					} catch (Exception e){
						passwd = "";
						JOptionPane.showMessageDialog(this, "Something went wrong with the sending of data. " +
								"\nPlease ensure that you're properly connected to the network, you are attempting to write to a valid point" +
								"\n and your username and password are correct.", "Data Sending Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					break;

				}
			}

		}

	}

	// ///// NESTED CLASS: NumberDocumentFilter ///////
	/**
	 * Document Filter designed to only allow the entry of characters that adhere to the 
	 * regex <strong><code>([0-9]*|\.|[0-9]*\.[0-9]*)</code></strong>, i.e. only characters that can be used to form 
	 * valid floating point numbers.
	 */
	public class NumberDocumentFilter extends DocumentFilter{
		private boolean filter = true;

		public boolean getFilterState(){
			return filter;
		}

		public void setFilterState(boolean state){
			this.filter = state;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
			System.out.println(text);
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

}
