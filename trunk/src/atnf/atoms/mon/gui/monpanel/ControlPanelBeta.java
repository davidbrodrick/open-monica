// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.PointEvent;
import atnf.atoms.mon.PointListener;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.DataMaintainer;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.PointSourceSelector;
import atnf.atoms.mon.util.RSA;

/**
 * Class representing a control panel.
 * 
 * @author Camille Nicodemus
 * @author Kalinga Hulugalle
 * @see MonPanel
 */
public class ControlPanelBeta extends MonPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5900630541567847520L;

	static {
		MonPanel.registerMonPanel("Control Panel Beta", ControlPanelBeta.class);
	}

	private String username = "";
	private RSA passwordEncryptor;
	private String encryptedPassword = "";

	// //// NESTED CLASS: ControlSetupPanel ///////
	protected class ControlSetupPanel extends MonPanelSetupPanel implements ActionListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3194244600789453530L;

		/** Main panel for our setup components. */
		private JPanel itsMainPanel = new JPanel();
		private PointSourceSelector itsPointSelector = new PointSourceSelector();
		private JButton login;
		private JLabel loginState;

		public ControlSetupPanel(ControlPanelBeta panel, JFrame frame) {
			super(panel, frame);

			// Authentication
			login = new JButton("Login");
			login.addActionListener(this);
			JPanel authenticate = new JPanel();
			authenticate.setLayout(new BoxLayout(authenticate, BoxLayout.Y_AXIS));
			login.setAlignmentX(JButton.CENTER_ALIGNMENT);

			authenticate.add(login);
			loginState = new JLabel();
			loginState.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			if (username.equals("")){
				loginState.setText("Not Logged In");
			} else {
				loginState.setText("Logged in OK as " + username);
				loginState.setForeground(new Color(0x6E8B3D));
			}
			authenticate.add(loginState);

			//itsMainPanel.setLayout(new GridLayout(0, 1));
			itsMainPanel.setLayout(new BorderLayout());

			itsMainPanel.add(authenticate, BorderLayout.NORTH);
			itsMainPanel.add(itsPointSelector, BorderLayout.CENTER);

			this.add(new JScrollPane(itsMainPanel), BorderLayout.CENTER);
		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.ControlPanelBeta");
			ss.setName("controlBetaSetup");

			Vector<?> points = itsPointSelector.getSelections();
			String p = "";
			if (points.size() > 0) {
				p += points.get(0);
				// Then add rest of point names with a delimiter
				for (int i = 1; i < points.size(); i++) {
					p += ":" + points.get(i);
				}
			}
			ss.put("points", p);

			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			// data validation and verification
			itsInitialSetup = setup;
			if (setup == null) {
				System.err.println("ControlBetaSetupPanel:showSetup: Setup is NULL");
				return;
			}
			if (!setup.getClassName().equals("atnf.atoms.mon.gui.monpanel.ControlPanelBeta")) {
				System.err.println("ControlPanelBeta:showSetup: Setup is for wrong class");
				return;
			}

			// get the stored points from the saved setup
			String p = setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			Vector<String> points = new Vector<String>(stp.countTokens());
			while (stp.hasMoreTokens()) {
				points.add(stp.nextToken());
			}
			itsPointSelector.setSelections(points);

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);

			Object source = e.getSource();
			if (source.equals(login)){
				
				JPanel inputs = new JPanel();
				inputs.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 0.5;
				gbc.gridx = 0;
				gbc.gridy = 0;
				JLabel usernameLabel = new JLabel("Username: ");
				JTextField usernameField = new JTextField(20);
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
					try {
					username = usernameField.getText();
					encryptedPassword = passwordEncryptor.encrypt(new String(passwordField.getPassword()));
					} catch (NumberFormatException nfe){
						username = "";
						encryptedPassword = "";
						JOptionPane.showMessageDialog(this, "Incorrect Username or Password", "ERROR LOGGING IN", JOptionPane.ERROR_MESSAGE);
					}
				}

				if (username.equals("")){
					loginState.setText("Not Logged In");
				} else {
					loginState.setText("Logged in OK as " + username);
					loginState.setForeground(new Color(0x6E8B3D));
				}
			}
		}
	}

	// ///// END NESTED CLASS ///////

	/** Copy of the setup we are currently using. */
	private SavedSetup itsSetup = null;
	private Vector<String> itsPoints = new Vector<String>();
	private ControlDisplayPanel contents;
	private JScrollPane scroll;

	/** Constructor. */
	public ControlPanelBeta() {
		 try {
			 passwordEncryptor = MonClientUtil.getServer().getEncryptor();
		 } catch (Exception e){
			 
		 }
		this.setLayout(new BorderLayout());
		contents = new ControlDisplayPanel();
		contents.setSize(5000, 5000);
		scroll =  new JScrollPane(contents);
		this.add(scroll, BorderLayout.CENTER);

	}

	// ///// NESTED CLASS: ControlDisplayPanel ///////

	public class ControlDisplayPanel extends JPanel implements ActionListener, PointListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5755259256750234800L;

		public HashMap<JLabel, CPComponents> components = new HashMap<JLabel, CPComponents>();
		public ControlDisplayPanel(){
			super();
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			JPanel contents = new JPanel();
			contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
			contents.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());

			//Initial Settings
			GridBagConstraints gbc = new GridBagConstraints();

			Insets big = new Insets(0,0,0,30);
			Insets regular = new Insets(0,0,0,5);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.5;

			int y = 0;

			for (String str : itsPoints){

				JLabel pointLabel = new JLabel("Point: ");
				JLabel point = new JLabel(str);
				JLabel pointvalLabel = new JLabel("Value: ");
				JLabel currValue = new JLabel("#NODATA");
				JLabel valueLabel = new JLabel("Update Value: ");
				JTextField value = new JTextField(10);
				JButton confirm = new JButton("Confirm");
				confirm.setActionCommand(str);
				confirm.addActionListener(this);

				gbc.gridx = 0;
				gbc.gridy = y;

				gbc.insets = regular;
				panel.add(pointLabel, gbc);

				gbc.insets = big;
				gbc.gridx = 1;
				gbc.gridwidth = 2;
				panel.add(point, gbc);

				gbc.insets = regular;
				gbc.gridx = 3;
				gbc.gridwidth = 1;
				panel.add(pointvalLabel, gbc);

				gbc.insets = big;
				gbc.gridx = 4;
				panel.add(currValue, gbc);

				gbc.insets = regular;
				gbc.gridx = 5;
				gbc.gridwidth = 2;
				panel.add(valueLabel, gbc);

				gbc.insets = big;
				gbc.gridx = 7;
				gbc.gridwidth = 1;
				panel.add(value, gbc);

				gbc.insets = regular;
				gbc.gridx = 8;
				panel.add(confirm, gbc);

				CPComponents cpc = new CPComponents(currValue, value);
				components.put(point, cpc);

				y++;
			}

			contents.add(panel);
			this.add(contents);

		}

		public void register(){
			DataMaintainer.subscribe(itsPoints, this);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String cmd = arg0.getActionCommand();
			Pattern pattern = Pattern.compile("[[a-zA-Z0-9]+\\.]+[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(cmd);
			if (matcher.matches()){
				// Valid pointname format
				for (JLabel j : components.keySet()){
					if (j.getText().equals(cmd)){
						String data = components.get(j).getValue();
						System.out.println(data);
						if (!PointDescription.getPoint(cmd).getUnits().equals("")){
							//check if it is a valid floating point number
							Pattern patt = Pattern.compile("[0-9]+|[0-9]*\\.[0-9]+");
							Matcher mat = patt.matcher(data);
							if (mat.matches()){
								//valid float, send off data
								//JOptionPane.showMessageDialog(this, "Data is being sent. Wait a moment to ensure that it has had an effect.", "SENDING DATA", JOptionPane.INFORMATION_MESSAGE);
							} else {
								// Badly formatted, possibly multiple floating points
								JOptionPane.showMessageDialog(this, "Incorrectly Formatted Input. Please check and try again", "BAD DATA ERROR", JOptionPane.ERROR_MESSAGE);
							}
						} else {
							//send off data since it's not a number anyway
							//JOptionPane.showMessageDialog(this, "Data is being sent. Wait a moment to ensure that it has had an effect.", "SENDING DATA", JOptionPane.INFORMATION_MESSAGE);
						}						
						break;
					}
				}

			}
		}

		@Override
		public void onPointEvent(Object source, PointEvent evt) {
			PointData data = evt.getPointData();
			for (JLabel j : components.keySet()){
				if (j.getText().equals(data.getName())){
					components.get(j).getLabel().setText(data.getData().toString());
					AbstractDocument doc = (AbstractDocument) components.get(j).getValueField().getDocument();
					if (doc.getDocumentFilter() == null && !PointDescription.getPoint(j.getText()).getUnits().equals("")){
						System.out.println("Units for point " + j.getText() + " are <" + PointDescription.getPoint(j.getText()).getUnits() + ">");
						CPDocumentFilter cpdf = new CPDocumentFilter();
						doc.setDocumentFilter(cpdf);
					}
					break;
				}
			}
		}
	}

	// ///// END NESTED CLASS ///////


	private void updateContents(){
		contents = new ControlDisplayPanel();
		scroll.setViewportView(contents);

		this.revalidate();
		this.repaint();
	}

	public MonPanelSetupPanel getControls() {
		return new ControlSetupPanel(this, itsFrame);
	}

	public boolean loadSetup(SavedSetup setup) {
		try {
			// check if the setup is suitable for our class
			if (!setup.checkClass(this)) {
				System.err.println("ControlPanelBeta:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}

			String p = (String) setup.get("points");
			StringTokenizer stp = new StringTokenizer(p, ":");
			while (stp.hasMoreTokens()) {
				itsPoints.add(stp.nextToken());
			}


			this.updateContents();
			contents.register();

		} catch (final Exception e) {
			e.printStackTrace();
			if (itsFrame != null) {
				JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
						+ "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
						JOptionPane.WARNING_MESSAGE);
			} else {
				System.err.println("AlarmManagerPanel:loadData: " + e.getClass().getName());
			}
			blankSetup();
			return false;
		}
		return true;
	}

	private void blankSetup() {
		itsPoints = new Vector<String>();

	}

	public synchronized SavedSetup getSetup() {
		return itsSetup;
	}

	public void vaporise() {
	}

	public void export(PrintStream p) {
	}

	public String getLabel() {
		return "Control Panel";
	}

	public class CPDocumentFilter extends DocumentFilter{
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
				Pattern pattern = Pattern.compile("[0-9]*|\\.|[0-9]*\\.[0-9]*");
				Matcher matcher = pattern.matcher(text);
				if (matcher.matches()){
					super.insertString(fb, offset, text, attr);
				} 
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (filter) {
				Pattern pattern = Pattern.compile("[0-9]*|\\.|[0-9]*\\.[0-9]*");
				Matcher matcher = pattern.matcher(text);
				if (matcher.matches()){
					super.replace(fb, offset, length, text, attrs);
				}
			}
		}
	}

	public class CPComponents{
		private JLabel valueLabel;
		private JTextField newValueField;

		public CPComponents(JLabel v, JTextField nv){
			this.valueLabel = v;
			this.newValueField = nv;
		}

		public JLabel getLabel(){
			return valueLabel;
		}

		public String getValue(){
			return newValueField.getText();
		}

		public JTextField getValueField(){
			return newValueField;
		}
	}
}
