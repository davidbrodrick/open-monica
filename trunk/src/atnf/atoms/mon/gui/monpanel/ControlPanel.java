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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.SimpleTreeSelector;
import atnf.atoms.mon.gui.SimpleTreeSelector.SimpleTreeUtil;

/**
 * Class representing a control panel.
 * 
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
public class ControlPanel extends MonPanel implements ActionListener, ItemListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5900630541567847520L;

	private Integer numControls = 0;
	private ArrayList<ControlPanelDisplayComponent> panelList = new ArrayList<ControlPanelDisplayComponent>();


	static {
		MonPanel.registerMonPanel("Control Panel", ControlPanel.class);
	}

	// //// NESTED CLASS: ControlSetupPanel ///////
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener, ChangeListener{

		/**
		 * 
		 */
		private static final long serialVersionUID = -3585682908515598074L;
		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private JPanel spinnerPanel = new JPanel();
		private JScrollPane viewPane = new JScrollPane();

		private JLabel numberControlsLabel = new JLabel("Number of Controls: ");
		private JSpinner numberControls = new JSpinner();
		private SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);

		private final String[] controlOptions = {"Text Field", "Button", "Checkbox"};
		private final String[] dataOptions = {"Text", "Number", "True/False"};

		private ArrayList<ControlPanelSetupComponent> componentList = new ArrayList<ControlPanelSetupComponent>();

		public ControlSetupPanel(ControlPanel panel, JFrame frame) {
			super(panel, frame);

			spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));			
			itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

			numberControls.setMaximumSize(new Dimension(45, 25));
			numberControls.setModel(spinModel);
			numberControls.addChangeListener(this);

			numberControlsLabel.setAlignmentX(CENTER_ALIGNMENT);
			numberControls.setAlignmentX(CENTER_ALIGNMENT);
			spinnerPanel.add(Box.createHorizontalGlue());
			spinnerPanel.add(numberControlsLabel);
			spinnerPanel.add(numberControls);
			spinnerPanel.add(Box.createHorizontalGlue());

			this.add(spinnerPanel, BorderLayout.NORTH);
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
					if (!cpsc.getLabelText().equals("")) {
						res += cpsc.getLabelText() + ";";
					} else {
						res += "\t" + ";";
					}
				}
			}

			ss.put("points", res);
			ss.put("controls number", Integer.toString(numControls));

			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			//TODO
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
			numberControls.setValue(numControls); //should trigger call to create numControls blank panels

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

				ControlPanelSetupComponent cpsc = componentList.get(n);
				cpsc.setPointString(point);
				cpsc.setControlType(controlType);
				cpsc.setDataType(dataType);
				cpsc.setTextField(labelText);

				n++;
			}

		}

		public void addControlSetup(){

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

			gbc.insets = new Insets(0, 0, 0, 0);
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

			gbc.gridx = 1;
			gbc.gridy = 5;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(displayLabel, gbc);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 2;
			bigPanel.add(displayField, gbc);
			
			gbc.gridx = 3;
			gbc.anchor = GridBagConstraints.EAST;
			bigPanel.add(valueLabel, gbc);
			valueLabel.setVisible(false);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 4;
			bigPanel.add(valueField, gbc);
			valueField.setVisible(false);

			gbc.anchor = GridBagConstraints.NORTHEAST;
			gbc.gridx = 5;
			gbc.gridy = 0;
			bigPanel.add(close, gbc);

			bigPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

			itsMainPanel.add(bigPanel);
			componentList.add(new ControlPanelSetupComponent(bigPanel, itsSimpleTreeSelector, selectedPoint, controlType, dataType, displayField, valueField, valueLabel, close));
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
		itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
		JLabel tempLabel = new JLabel("Setup Not Configured. Hit the \"Control Panel\" tab to set up this panel.");
		itsMainPanel.add(tempLabel);
		itsScrollPane.add(itsMainPanel);
		this.add(itsScrollPane);

	}

	// ///// NESTED CLASS: ControlPanelSetupComponent ///////

	public class ControlPanelSetupComponent{

		private JPanel container;
		private SimpleTreeSelector tree;
		private JLabel point;
		private JComboBox control;
		private JComboBox data;
		private JTextField label;
		private JTextField value;
		private JLabel valueLabel;

		private JButton closeButton;

		public ControlPanelSetupComponent(JPanel cont, SimpleTreeSelector stis, JLabel pt, JComboBox ct, JComboBox dt, JTextField lb, JTextField vf, JLabel vl, JButton close){
			this.container = cont;
			this.tree = stis;
			this.point = pt;
			this.control = ct;
			this.data = dt;
			this.label = lb;
			this.value = vf;
			this.valueLabel = vl;
			this.closeButton = close;
		}

		public JComboBox getDataBox() {
			return data;

		}

		public JComboBox getControlBox() {
			return control;
		}

		public JButton getCloseButton() {
			return closeButton;
		}

		public void setTextField(String labelText) {
			label.setText(labelText);

		}

		public void setDataType(String dataType) {
			data.setSelectedItem(dataType);

		}

		public void setControlType(String controlType) {
			control.setSelectedItem(controlType);

		}

		public void setPointString(String point2) {
			Vector<String> selection = new Vector<String>();
			selection.add(point2);
			tree.setSelections(selection);
			point.setText(point2);

		}

		public String getTreeSelection(){
			Vector<String> selections = this.tree.getSelections();
			if (selections.size() != 1){
				return null;
			} else {
				return selections.get(0);
			}
		}

		public SimpleTreeSelector getTree(){
			return this.tree;
		}

		public void setPointString(){
			String pointString = this.getTreeSelection();
			if (pointString != null){
				point.setText(pointString);
			} else {
				point.setText("No Point Selected");
			}
		}

		public String getControlType(){
			return control.getSelectedItem().toString();
		}

		public String getDataType(){
			return data.getSelectedItem().toString();
		}

		public String getLabelText(){
			return label.getText();
		}
		
		public String getValueText(){
			return value.getText();
		}
		
		public void setValueVis(boolean b){
			value.setVisible(b);
		}
		
		public void setValueLabelVis(boolean b){
			valueLabel.setVisible(b);
		}

		public JPanel getContainer(){
			return this.container;
		}

		public void removeListeners(ControlSetupPanel csp){
			tree.removeChangeListener(csp);
			closeButton.removeActionListener(csp);
			control.removeActionListener(csp);
			data.removeActionListener(csp);
		}
	}

	// ///// END NESTED CLASS ///////

	// ///// NESTED CLASS: ControlPanelDisplayComponent ///////

	public class ControlPanelDisplayComponent{

		public final static int BUTTON_TYPE = 0x00;
		public final static int CHECKBOX_TYPE = 0x01;
		public final static int TEXT_TYPE = 0x02;

		private JPanel container;
		private JLabel label;
		private JButton button;
		private JCheckBox check;
		private JTextField value;
		private JButton confirm;
		private String dataType;
		private int type;

		public ControlPanelDisplayComponent(JPanel cont, JLabel pt, JButton jb, String dt){
			this.container = cont;
			this.label = pt;
			this.button = jb; 
			this.type = BUTTON_TYPE;
			this.dataType = dt;
		}

		public ControlPanelDisplayComponent(JPanel cont, JLabel pt, JCheckBox jc, String dt){
			this.container = cont;
			this.label = pt;
			this.check = jc; 
			this.type = CHECKBOX_TYPE;
			this.dataType = dt;
		}

		public ControlPanelDisplayComponent(JPanel cont, JLabel pt, JTextField v, JButton conf, String dt){
			this.container = cont;
			this.label = pt;
			this.value = v; 
			this.type = TEXT_TYPE;
			this.dataType = dt;
			this.confirm = conf;
		}

		public void setLabel(String labelText) {
			label.setText(labelText);		
		}

		public JButton getButton(){
			if (this.type == BUTTON_TYPE){
				return button;
			} else {
				return null;
			}
		}

		public JCheckBox getCheckBox(){
			if (this.type == CHECKBOX_TYPE){
				return check;
			} else {
				return null;
			}
		}

		public JTextField getTextField(){
			if (this.type == TEXT_TYPE){
				return value;
			} else {
				return null;
			}
		}

		public String getLabelText(){
			return label.getText();
		}

		public JPanel getContainer(){
			return this.container;
		}

		public String getdataType(){
			return dataType;
		}

		public JButton getConfirmButton(){
			if (this.type == TEXT_TYPE){
				return confirm;
			} else {
				return null;
			}
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

			String res = itsInitialSetup.get("points");
			StringTokenizer st = new StringTokenizer(res, ";");
			Vector<String> components = new Vector<String>();
			while (st.hasMoreTokens()){
				components.add(st.nextToken());
			}

			itsMainPanel = new JPanel();
			itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));

			for (String s : components){
				st = new StringTokenizer(s, ",");
				ControlPanelDisplayComponent cpdc;
				String point = st.nextToken();
				String controlType = st.nextToken();
				String dataType = st.nextToken();
				String labelText = st.nextToken();

				JPanel itsPanel = new JPanel();
				JLabel itsPoint = new JLabel(point);

				if (controlType.equals("Button")){
					itsPanel.setLayout(new GridLayout(1,3));
					JButton jb = new JButton(labelText);
					itsPanel.add(itsPoint);
					itsPanel.add(jb);
					cpdc = new ControlPanelDisplayComponent(itsPanel, itsPoint, jb, dataType);
				} else if (controlType.equals("Checkbox")){
					itsPanel.setLayout(new GridLayout(1,3));
					JCheckBox jc = new JCheckBox(labelText);
					itsPanel.add(itsPoint);
					itsPanel.add(jc);
					cpdc = new ControlPanelDisplayComponent(itsPanel, itsPoint, jc, dataType);

				} else {
					itsPanel.setLayout(new GridLayout(1,3));
					JLabel tfl = new JLabel(labelText);
					JTextField tf = new JTextField();
					JButton send = new JButton("Send");
					send.addActionListener(this);
					tf.setEditable(true);
					itsPanel.add(itsPoint);
					itsPanel.add(tfl);
					itsPanel.add(tf);
					itsPanel.add(send);
					cpdc = new ControlPanelDisplayComponent(itsPanel, itsPoint, tf, send, dataType);

				}


				itsMainPanel.add(itsPanel);
				panelList.add(cpdc);

			}



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
		//TODO
	}

	public void export(PrintStream p) {
		//TODO
	}

	public String getLabel() {
		return "Control Panel";
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		// TODO Auto-generated method stub
		
	}


}
