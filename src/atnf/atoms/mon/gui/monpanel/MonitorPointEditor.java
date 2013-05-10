// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui.monpanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.PrintStream;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
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
import javax.swing.text.DefaultCaret;
import javax.swing.text.DocumentFilter;

import atnf.atoms.mon.Alarm;
import atnf.atoms.mon.AlarmEvent;
import atnf.atoms.mon.AlarmEventListener;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.SavedSetup;
import atnf.atoms.mon.client.AlarmMaintainer;
import atnf.atoms.mon.client.MonClientUtil;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.gui.MonPanel;
import atnf.atoms.mon.gui.MonPanelSetupPanel;
import atnf.atoms.mon.gui.SimpleTreeSelector;
import atnf.atoms.mon.gui.SimpleTreeSelector.SimpleTreeUtil;

/**
 * MonPanel subclass designed to be a nice means of adding monitor-points to the monitor-points.txt
 * file without having to mess around with using a text editor. Also aiming to have some small
 * amount of help to users when adding points.
 * @author Kalinga Hulugalle
 *
 */
public class MonitorPointEditor extends MonPanel implements ActionListener, AlarmEventListener{

	private static final long serialVersionUID = 1030968805100512703L;
	/** Array holding the layout values "Horizontal" and "Vertical" for use with the layout combo-box */
	private final String[] layouts = {"Horizontal", "Vertical"};
	private String username = "";
	private String password = "";

	static {
		MonPanel.registerMonPanel("Monitor Point Editor", MonitorPointEditor.class);
	}

	// //// NESTED CLASS: MonitorPointEditorSetupPanel ///////
	protected class MonitorPointEditorSetupPanel extends MonPanelSetupPanel implements ActionListener, ChangeListener{

		private static final long serialVersionUID = -7475845517269885679L;

		private JPanel itsMainPanel = new JPanel(new GridBagLayout());
		private JPanel topPanel = new JPanel(new GridBagLayout());
		private JScrollPane itsMainScroller = new JScrollPane();
		private JButton addPoint = new JButton("Add new Point");
		private JButton editPoint = new JButton("Edit a Point");
		private JButton clonePoint = new JButton("Clone a Point");
		private JLabel addDesc = new JLabel("Added points:");
		private JLabel addTotal = new JLabel("0");
		private JLabel editDesc = new JLabel("Edited points:");
		private JLabel editTotal = new JLabel("0");
		private JLabel cloneDesc = new JLabel("Cloned points:");
		private JLabel cloneTotal = new JLabel("0");
		private JComboBox layout = new JComboBox(layouts);

		int numNew = 0;
		int numEdit = 0;
		int numClone = 0;
		/** ArrayList holding references to all the small panels so they can be easily modified or notify this panel of changes */
		private ArrayList<MPEditorSetupComponent> components = new ArrayList<MPEditorSetupComponent>();

		/**
		 * Constructor for a MonitorPointEditorSetupPanel.
		 * @param mpe The main display monpanel
		 * @param frame The JFrame that holds this monpanel
		 */
		protected MonitorPointEditorSetupPanel(MonitorPointEditor mpe, JFrame frame) {
			super(mpe, frame);

			itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
			itsMainScroller.getVerticalScrollBar().setUnitIncrement(15);
			addTotal.setText(Integer.toString(numNew));
			editTotal.setText(Integer.toString(numEdit));

			// Create new GridBagConstraints for the Top panel
			GridBagConstraints gbct = new GridBagConstraints();
			gbct.weightx = 0.5;
			gbct.weighty = 0.5;
			gbct.gridx = 0;
			gbct.gridy = 0;
			gbct.gridheight = 1;
			gbct.gridwidth = 1;
			gbct.insets = new Insets(5,10,5,10);

			// Add stuff to the Top panel (not used at the moment, but maybe later)
			addPoint.addActionListener(this);
			editPoint.addActionListener(this);
			clonePoint.addActionListener(this);
			JLabel layoutLabel = new JLabel("Layout: ");
			gbct.gridx = 2;
			topPanel.add(layoutLabel, gbct);
			gbct.gridx = 3;
			topPanel.add(layout, gbct);
			gbct.fill = GridBagConstraints.HORIZONTAL;
			gbct.gridx = 0;
			gbct.gridwidth = 2;
			gbct.gridy ++;
			topPanel.add(addPoint, gbct);
			gbct.gridx = 2;
			topPanel.add(editPoint, gbct);
			gbct.gridx = 4;
			topPanel.add(clonePoint, gbct);
			gbct.gridx = 0;
			gbct.gridy ++;
			gbct.gridwidth = 1;
			topPanel.add(addDesc, gbct);
			gbct.gridx += 1;
			topPanel.add(addTotal, gbct);
			gbct.gridx += 1;
			topPanel.add(editDesc, gbct);
			gbct.gridx += 1;
			topPanel.add(editTotal, gbct);
			gbct.gridx += 1;
			topPanel.add(cloneDesc, gbct);
			gbct.gridx += 1;
			topPanel.add(cloneTotal, gbct);


			//Add stuff to this SetupPanel
			this.add(topPanel, BorderLayout.NORTH);
			itsMainScroller.setViewportView(itsMainPanel);
			this.add(itsMainScroller, BorderLayout.CENTER);
		}

		@Override
		protected SavedSetup getSetup() {
			SavedSetup ss = new SavedSetup();
			ss.setClass("atnf.atoms.mon.gui.monpanel.MonitorPointEditor");
			ss.setName("monitorPointEditor");

			String comp = "";
			for (MPEditorSetupComponent m : components){
				if (m.getType() == MPEditorSetupComponent.ADD_POINT){
					comp += "add,null;";
				} else if (m.getType() == MPEditorSetupComponent.EDIT_POINT){
					comp += "edit," + m.getTreeSelection() + ";";
				} else if (m.getType() == MPEditorSetupComponent.CLONE_POINT){
					comp += "clone," + m.getTreeSelection() + ";";
				}
			}
			ss.put("values", comp);
			ss.put("layout", layout.getSelectedItem().toString());
			return ss;
		}

		@Override
		protected void showSetup(SavedSetup setup) {
			String values = setup.get("values");
			int n = 0;
			StringTokenizer st = new StringTokenizer(values,";");
			while (st.hasMoreTokens()){
				StringTokenizer s = new StringTokenizer(st.nextToken(), ",");			
				String type = s.nextToken();
				if (type.equals("add")){
					this.addNewPointPanel();
					continue;
				} else if (type.equals("edit")){
					this.addEditPointPanel(MPEditorSetupComponent.EDIT_POINT);
					Vector<String> selection = new Vector<String>();
					selection.add(s.nextToken());
					components.get(n).getTree().setSelections(selection);		
				} else if (type.equals("clone")){
					this.addEditPointPanel(MPEditorSetupComponent.CLONE_POINT);
					Vector<String> selection = new Vector<String>();
					selection.add(s.nextToken());
					components.get(n).getTree().setSelections(selection);		
				}
				n++;
			}
			layout.setSelectedItem(setup.get("layout"));
		}

		@Override
		public void actionPerformed(ActionEvent e){
			if (e.getSource() instanceof JButton){
				JButton source = (JButton) e.getSource();
				if (source.equals(addPoint)){
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							MonitorPointEditorSetupPanel.this.addNewPointPanel();
						}
					});
				} else if (source.equals(editPoint)){
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							MonitorPointEditorSetupPanel.this.addEditPointPanel(MPEditorSetupComponent.EDIT_POINT);
						}
					});
				} else if (source.equals(clonePoint)){
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							MonitorPointEditorSetupPanel.this.addEditPointPanel(MPEditorSetupComponent.CLONE_POINT);
						}
					});
				}
				if (source.getActionCommand().equals("OK")){
					for (MPEditorSetupComponent c : components){
						if (c.getTreeSelection() == null && c.getType() != MPEditorSetupComponent.ADD_POINT){
							JOptionPane.showMessageDialog(this, 
									"One or more of your Controls does not have a point selected.\n" +
									"Please select a data point to control, or remove a control input panel.", 
									"Missing Point Selection", JOptionPane.ERROR_MESSAGE);
							return;
						}
					}

					if (!this.noDuplicates()){
						JOptionPane.showMessageDialog(this, 
								"There are one or more duplicate points selected.\n" +
								"Please ensure all selected points are unique.", 
								"Duplicate Points Error ", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (numClone + numEdit + numNew == 0){
						JOptionPane.showMessageDialog(this, 
								"Please select at least one type of point.", 
								"No Points Selected Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					super.actionPerformed(e);
				} else {// must be a "close" button
					MPEditorSetupComponent removed = null;
					for (MPEditorSetupComponent m : components){
						if (m.getButton().equals(source)){
							m.removeListeners(this);
							itsMainPanel.remove(m.getPanel());
							removed = m;
							if (m.getType() == MPEditorSetupComponent.ADD_POINT) {
								this.decrementAdd();
							} else if (m.getType() == MPEditorSetupComponent.EDIT_POINT){
								this.decrementEdit();
							} else {
								this.decrementClone();
							}
							break;
						}
					}		
					if (removed != null) components.remove(removed);
					itsMainPanel.revalidate();
					itsMainPanel.repaint();
				}
			}
		}

		/**
		 * Private method that checks that there are no duplicate selections among the "edit" type panels.
		 * @return A boolean indicating whether there are duplicates or not
		 */
		private boolean noDuplicates() {
			boolean ret = true;
			HashSet<String> set = new HashSet<String>(); // Hashsets only allow adding of unique members
			for (MPEditorSetupComponent c : components){
				if (c.getType() == MPEditorSetupComponent.EDIT_POINT){
					ret = set.add(c.getTreeSelection()); // returns false if it can't add a member
					if (!ret) return ret;
				}
			}
			return ret;
		}

		/**
		 * Small method to add 1 to the total of new point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void incrementAdd(){
			numNew += 1;
			addTotal.setText(Integer.toString(numNew));
		}

		/**
		 * Small method to subtract 1 to the total of edit point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void incrementEdit(){
			numEdit += 1;
			editTotal.setText(Integer.toString(numEdit));
		}

		/**
		 * Small method to subtract 1 to the total of clone point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void incrementClone(){
			numClone+= 1;
			cloneTotal.setText(Integer.toString(numClone));
		}

		/**
		 * Small method to add 1 to the total of edit point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void decrementAdd(){
			numNew -= 1;
			addTotal.setText(Integer.toString(numNew));
		}

		/**
		 * Small method to subtract 1 to the total of edit point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void decrementEdit(){
			numEdit -= 1;
			editTotal.setText(Integer.toString(numEdit));
		}

		/**
		 * Small method to subtract 1 to the total of clone point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void decrementClone(){
			numClone -= 1;
			cloneTotal.setText(Integer.toString(numClone));
		}

		/**
		 * Method to add a new "Edit" or "Clone" type setup panel to the main setup panel.<br/>
		 * It adds all the required elements, registers listeners and updates the running
		 * totals.
		 */
		private void addEditPointPanel(int type) {
			JPanel newPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			SimpleTreeSelector tree = new SimpleTreeSelector();
			tree.addChangeListener(this);
			JLabel selectedPointLabel = new JLabel("Selected Point:");
			JLabel selectedPoint = new JLabel("No Point Selected");
			JButton close = new JButton("X");
			gbc.gridheight = 4;
			gbc.gridwidth = 3;
			newPanel.add(tree, gbc);

			gbc.insets = new Insets(30, 0, 0, 0);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridheight = 1;
			gbc.gridwidth = 2;
			gbc.gridx = 3;
			gbc.gridy = 0;
			selectedPointLabel.setFont(new Font("Sans Serif", Font.ITALIC, 18));
			newPanel.add(selectedPointLabel, gbc);

			gbc.gridy = 2;
			gbc.insets = new Insets(0, 0, 30, 0);
			selectedPoint.setFont(new Font("Sans Serif", Font.ITALIC | Font.BOLD, 24));
			newPanel.add(selectedPoint, gbc);
			gbc.weightx = 0.001;
			gbc.weighty = 0.001;
			gbc.gridx = 4;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.NORTHEAST;
			close.setFont(new Font("Monospaced", Font.BOLD, 12));
			close.setBackground(new Color(0xCD0000));
			close.setForeground(Color.WHITE);
			close.addActionListener(this);
			newPanel.add(close, gbc);
			newPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

			itsMainPanel.add(newPanel);
			components.add(new MPEditorSetupComponent(newPanel, tree, selectedPoint, close, type));

			if (type == MPEditorSetupComponent.EDIT_POINT){
				this.incrementEdit();
			} else {
				this.incrementClone();
			}
		}

		/**
		 * Method to add a new "New" type setup panel to the main setup panel
		 * It adds all the required elements, registers listeners and updates the running
		 * totals.
		 */
		private void addNewPointPanel() {
			JPanel newPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			JLabel label = new JLabel("New Point will be configured in  the Display tab");
			JButton close = new JButton("X");
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			newPanel.add(label, gbc);
			gbc.weightx = 0.001;
			gbc.weighty = 0.001;
			gbc.anchor = GridBagConstraints.NORTHEAST;
			close.setFont(new Font("Monospaced", Font.BOLD, 12));
			close.setBackground(new Color(0xCD0000));
			close.setForeground(Color.WHITE);
			close.addActionListener(this);
			newPanel.add(close, gbc);
			newPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

			itsMainPanel.add(newPanel);
			components.add(new MPEditorSetupComponent(newPanel, close));

			this.incrementAdd();
		}

		/**
		 * Simple method that updates whenever the point tree has its selections changed,
		 * so the corresponding JLabel is similarly updated.
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() instanceof SimpleTreeUtil){
				SimpleTreeUtil source = (SimpleTreeUtil) e.getSource();
				for (MPEditorSetupComponent c: components){
					if(c.getType() != MPEditorSetupComponent.ADD_POINT && source.equals(c.getTree().getTreeUtil())){
						c.setPointString();
						break;
					}
				}
			}
		}
	}

	// ///// NESTED NESTED CLASS: MPEditorSetupComponent ///////

	/**
	 * Class used to internally keep track of various elements used in setting up, interacting with and displaying the ControlSetupPanel
	 */
	public class MPEditorSetupComponent{

		public final static int ADD_POINT = 0x01;
		public final static int EDIT_POINT = 0x02;
		public final static int CLONE_POINT = 0x03;

		private JPanel panel = null;
		private int type;
		private JButton close = null;
		private SimpleTreeSelector tree = null;
		private JLabel point = null;

		/**
		 * Constructor for "Add" type panels. Only requires references to the JPanel and the close button
		 * @param j The JPanel these components are added into
		 * @param c The button that closes this panel
		 */
		public MPEditorSetupComponent(JPanel j, JButton c){
			panel = j;
			type = ADD_POINT;
			close = c;
		}

		/**
		 * Constructor for the "Edit" type panels. Has references to the JPanel, the selection tree, the point label and the close button
		 * @param j The JPanel these components are added into
		 * @param t The SimpleTreeSelector that this panel uses to choose points
		 * @param p The JLabel that gets updated when the tree's selection changes
		 * @param c The button that closes this panel
		 */
		public MPEditorSetupComponent(JPanel j, SimpleTreeSelector t, JLabel p, JButton c, int ty){
			panel = j;
			type = ty;
			tree = t;
			point = p;
			close = c;
		}

		/**
		 * Updates the point JLabel to the selected point from the JTree if applicable
		 */
		public void setPointString() {
			if (this.getType() == MPEditorSetupComponent.ADD_POINT) return;
			String pointString = this.getTreeSelection();
			if (pointString != null){
				point.setText(pointString);
			} else {
				point.setText("No Point Selected");
			}
		}

		/**
		 * Gets the selected item from the tree - only returns a valid value when the 
		 * tree has a single unique point selected
		 * @return A String holding the name of the selected point if valid, otherwise {@code null}.
		 */
		public String getTreeSelection(){
			if (this.getType() == MPEditorSetupComponent.ADD_POINT) return null;
			Vector<String> selections = this.tree.getSelections();
			if (selections.size() != 1){
				return null;
			} else {
				return selections.get(0);
			}
		}

		/**
		 * Returns a reference to the SimpleTreeSelector in the "Edit" type panels, if applicable
		 * @return The SimpleTreeSelector, otherwise null
		 */
		public SimpleTreeSelector getTree() {
			if (this.getType() == MPEditorSetupComponent.ADD_POINT) return null;
			return tree;
		}

		/**
		 * Returns an int mask for the type of panel
		 * @return The relevant int mask, either {@code MPEditorSetupComponent.ADD_POINT} or {@code MPEditorComponent.EDIT_POINT}
		 */
		public int getType(){
			return type;
		}

		/**
		 * Returns a reference to the JPanel that these components reside in
		 * @return The JPanel reference
		 */
		public JPanel getPanel(){
			return panel;
		}

		/**
		 * Returns a reference to the JButton that closes this panel
		 * @return The JButton reference
		 */
		public JButton getButton(){
			return close;
		}

		/**
		 * Removes listeners from the close button and the SimpleTreeSelector if applicable
		 * @param m
		 */
		public void removeListeners(MonitorPointEditorSetupPanel m){
			close.removeActionListener(m);
			if (this.getType() != MPEditorSetupComponent.ADD_POINT) tree.removeChangeListener(m);
		}

	}

	// ///// END NESTED NESTED CLASS ///////
	// //// END NESTED CLASS ///////

	private SavedSetup itsSetup = null;
	private JPanel itsMainPanel = new JPanel(new GridBagLayout());
	private JScrollPane  itsScrollPane = new JScrollPane();
	private ArrayList<MPEditorComponent>  components = new ArrayList<MPEditorComponent>();
	private GridBagConstraints gbc = new  GridBagConstraints();
	/** Array holding the values "True" and "False" for use with boolean combo-boxes */
	private String[] bools = {"True", "False"};
	/** Array holding the different categories of Alarms; "Information", "Warning", "Danger" and "Severe"*/
	private String[] priorities = {"Information (0)", "Warning (1)", "Danger (2)", "Severe (3)"};

	private String layout = "";
	private LimitFieldDocumentFilter lfdf;
	private NumDocumentFilter ndf;

	JButton help = new JButton("?");
	JButton write = new JButton("Write all points");
	/** Private boolean indicating whether the <tt>loadSetup()</tt> method has completed yet.*/
	private static boolean loaded = false;
	/** Private boolean indicating whether the text fields have been populated yet.*/
	private static boolean updated = false;

	/**
	 * Constructor, initialises some of the Document Filters and fonts for JLabels, along
	 * with setting up the layout for some of the panels
	 */
	public MonitorPointEditor() {
		AlarmMaintainer.addListener(this);
		lfdf = new LimitFieldDocumentFilter(10);
		ndf = new NumDocumentFilter();
		help.setToolTipText("Help");
		help.setBackground(new Color(0x0066CC));
		help.setForeground(Color.WHITE);
		write.setFont(new Font("Sans Serif", Font.BOLD, write.getFont().getSize()));

		help.addActionListener(this);
		write.addActionListener(this);

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		itsMainPanel.setLayout(new BoxLayout(itsMainPanel, BoxLayout.Y_AXIS));
		JLabel tempLabel = new JLabel("Setup Not Configured. Hit the \"Monitor Point Editor\" tab to set up this panel.");
		itsMainPanel.add(tempLabel);
		itsScrollPane.getVerticalScrollBar().setUnitIncrement(15);
		itsScrollPane.setViewportView(itsMainPanel);
		this.add(itsScrollPane);
	}

	@Override
	public void export(PrintStream p) {
		String out = "";
		for (MPEditorComponent m : components){
			PointDescription pd = PointDescription.factory(m.getNames(), m.getLongDesc(), m.getShortDesc(), m.getUnits(), m.getSource(), m.getInTransactions(), m.getOutTransactions(), m.getTranslations(), m.getAlarmCriteria(), m.getArchivePolicies(), m.getNotifications(), m.getPeriod(), m.getArchiveLongevity(), m.getGuidance(), m.getPriority(), m.getEnabled());
			String monPointTxt = pd.getStringEquiv();
			out += monPointTxt + "\n";
		}
		p.print(out);
	}

	@Override
	public MonPanelSetupPanel getControls() {
		return new MonitorPointEditorSetupPanel(this, itsFrame);
	}

	@Override
	public String getLabel() {
		return "Monitor Point Editor";
	}

	@Override
	public SavedSetup getSetup() {
		return itsSetup;
	}

	@Override
	public boolean loadSetup(SavedSetup setup) {
		try {
			if (!setup.checkClass(this)) {
				System.err.println("MonitorPointEditor:loadSetup: setup not for " + this.getClass().getName());
				return false;
			}
			AlarmMaintainer.addListener(this);
			layout = setup.get("layout");

			itsSetup = setup;
			itsMainPanel = new JPanel();
			itsMainPanel.setLayout(new GridBagLayout());
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0.00001;
			gbc.weighty = 0.00001;
			gbc.insets = new Insets(5,5,5,5);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			help.setFont(new Font("Monospaced", Font.BOLD, 12));
			itsMainPanel.add(help, gbc);
			gbc.gridx ++;
			itsMainPanel.add(write, gbc);
			gbc.gridy ++;
			gbc.gridx = 0;

			JLabel point = new JLabel("Point Name");
			JLabel longDesc = new JLabel("Long Description");
			JLabel shortDesc = new JLabel("Short Desc");
			JLabel units = new JLabel("Units");
			JLabel sources = new JLabel("Sources");
			JLabel state = new JLabel("Enabled State");
			JLabel inTrans = new JLabel("Input Transactions");
			JLabel outTrans = new JLabel("Output Transactions");
			JLabel translats = new JLabel("Translations");
			JLabel archPol = new JLabel("Archive Policies");
			JLabel updInt = new JLabel("Update Interval");
			JLabel archLong = new JLabel("Archive Longevity");
			JLabel notifs = new JLabel("Notifications");
			JLabel alCrit = new JLabel("Alarm Criteria");
			JLabel priority = new JLabel("Priority");
			JLabel guidance = new JLabel("Guidance");

			Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
			fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);

			point.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			longDesc.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			shortDesc.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			units.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			sources.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			state.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			inTrans.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			outTrans.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			translats.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			alCrit.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			archPol.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			updInt.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			archLong.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			notifs.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			priority.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));
			guidance.setFont(new Font("Sans Serif", Font.BOLD, point.getFont().getSize()).deriveFont(fontAttributes));

			if (layout.equals(layouts[0])){
				gbc.gridx ++;
				itsMainPanel.add(point, gbc);
				gbc.gridx ++;
				itsMainPanel.add(longDesc, gbc);
				gbc.gridx ++;
				itsMainPanel.add(shortDesc, gbc);
				gbc.gridx ++;
				itsMainPanel.add(units, gbc);
				gbc.gridx ++;
				itsMainPanel.add(sources, gbc);
				gbc.gridx ++;
				itsMainPanel.add(state, gbc);
				gbc.gridx ++;
				itsMainPanel.add(inTrans, gbc);
				gbc.gridx ++;
				itsMainPanel.add(outTrans, gbc);
				gbc.gridx ++;
				itsMainPanel.add(translats, gbc);
				gbc.gridx ++;
				itsMainPanel.add(archPol, gbc);
				gbc.gridx ++;
				itsMainPanel.add(updInt, gbc);
				gbc.gridx ++;
				itsMainPanel.add(archLong, gbc);
				gbc.gridx ++;
				itsMainPanel.add(notifs, gbc);
				gbc.gridx ++;
				itsMainPanel.add(alCrit, gbc);
				gbc.gridx ++;
				itsMainPanel.add(priority, gbc);
				gbc.gridx ++;
				itsMainPanel.add(guidance, gbc);
				gbc.gridy ++;
			} else if (layout.equals(layouts[1])){
				itsMainPanel.add(point, gbc);
				gbc.gridy ++;
				itsMainPanel.add(longDesc, gbc);
				gbc.gridy ++;
				itsMainPanel.add(shortDesc, gbc);
				gbc.gridy ++;
				itsMainPanel.add(units, gbc);
				gbc.gridy ++;
				itsMainPanel.add(sources, gbc);
				gbc.gridy ++;
				itsMainPanel.add(state, gbc);
				gbc.gridy ++;
				itsMainPanel.add(inTrans, gbc);
				gbc.gridy ++;
				itsMainPanel.add(outTrans, gbc);
				gbc.gridy ++;
				itsMainPanel.add(translats, gbc);
				gbc.gridy ++;
				itsMainPanel.add(archPol, gbc);
				gbc.gridy ++;
				itsMainPanel.add(updInt, gbc);
				gbc.gridy ++;
				itsMainPanel.add(archLong, gbc);
				gbc.gridy ++;
				itsMainPanel.add(notifs, gbc);
				gbc.gridy ++;
				itsMainPanel.add(alCrit, gbc);
				gbc.gridy ++;
				itsMainPanel.add(priority, gbc);
				gbc.gridy ++;
				itsMainPanel.add(guidance, gbc);
				gbc.gridx ++;
			}

			ArrayList<String> tokens = new ArrayList<String>();
			String values = itsSetup.get("values");
			StringTokenizer st = new StringTokenizer(values,";");
			while (st.hasMoreTokens()){
				tokens.add(st.nextToken());
			}
			int n = 0;
			for (String str: tokens){
				if (n == tokens.size()-1){
					if (layout.equals(layouts[0])) {
						gbc.weighty = 1.0;
						gbc.anchor = GridBagConstraints.NORTH;
					} else if (layout.equals(layouts[1])) {
						gbc.weightx = 1.0;
					}
				}
				StringTokenizer s = new StringTokenizer(str, ",");			
				String type = s.nextToken();
				if (type.equals("add")){
					this.addEditorPanel();
				} else if (type.equals("edit")){
					this.addEditorPanel(s.nextToken());
				} else {
					this.addClonePanel(s.nextToken());
				}
				if (layout.equals(layouts[0])){
					gbc.gridy ++;
				} else if (layout.equals(layouts[1])){
					gbc.gridx ++;
				}
				n++;
			}
			new DataSender("update").start();
			itsScrollPane.setViewportView(itsMainPanel);
			itsScrollPane.revalidate();
			itsScrollPane.repaint();
			loaded = true;
			return true;
		} catch (Exception e){
			e.printStackTrace();
			if (itsFrame != null) {
				JOptionPane.showMessageDialog(itsFrame, "The setup called \"" + setup.getName() + "\"\n" + "for class \"" + setup.getClassName() + "\"\n"
						+ "could not be parsed.\n\n" + "The type of exception was:\n\"" + e.getClass().getName() + "\"\n\n", "Error Loading Setup",
						JOptionPane.WARNING_MESSAGE);
			} else {
				System.err.println("MonitorPointEditor:loadData: " + e.getClass().getName());
			}
			return false;
		}
	}

	/**
	 * Adds an Editor row or column to the main panel, with blank entries in the JTextFields.
	 * Primarily used for new points rather than existing ones.
	 * @see #addEditorPanel(String)
	 * @see #addClonePanel(String)
	 */
	public void addEditorPanel(){
		JButton wiz = new JButton("Wizard");
		wiz.addActionListener(this);
		JButton writer = new JButton("Write");
		writer.addActionListener(this);
		JTextField npf = new JTextField(10);
		JTextField ld = new JTextField(7);
		JTextField sd = new JTextField(5);
		JTextField u = new JTextField(2);
		JTextField s = new JTextField(5);
		JComboBox es = new JComboBox(bools);
		JTextField it = new JTextField(10);
		JTextField ot = new JTextField(10);
		JTextField t = new JTextField(10);
		JTextField ac = new JTextField(10);
		JTextField ap = new JTextField(5);
		JTextField ui = new JTextField(3);
		JTextField al = new JTextField(3);
		JTextField n = new JTextField(4);
		JComboBox p = new JComboBox(priorities);
		JTextField g = new JTextField(15);

		AbstractDocument adsd = (AbstractDocument) sd.getDocument();
		adsd.setDocumentFilter(lfdf);
		AbstractDocument adui = (AbstractDocument) ui.getDocument();
		adui.setDocumentFilter(ndf);
		AbstractDocument adal = (AbstractDocument) al.getDocument();
		adal.setDocumentFilter(ndf);
		es.setEditable(false);
		p.setEditable(false);

		if (layout.equals(layouts[0])){
			gbc.gridx = 0;
			itsMainPanel.add(wiz, gbc);
			gbc.gridx ++;
			itsMainPanel.add(npf, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ld, gbc);
			gbc.gridx ++;
			itsMainPanel.add(sd, gbc);
			gbc.gridx ++;
			itsMainPanel.add(u, gbc);
			gbc.gridx ++;
			itsMainPanel.add(s, gbc);
			gbc.gridx ++;
			itsMainPanel.add(es, gbc);
			gbc.gridx ++;
			itsMainPanel.add(it, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ot, gbc);
			gbc.gridx ++;
			itsMainPanel.add(t, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridx ++;
			itsMainPanel.add(al, gbc);
			gbc.gridx ++;
			itsMainPanel.add(n, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ac, gbc);
			gbc.gridx ++;
			itsMainPanel.add(p, gbc);
			gbc.gridx ++;
			itsMainPanel.add(g, gbc);
			gbc.gridx ++;
			itsMainPanel.add(writer, gbc);
		} else  if (layout.equals(layouts[1])){
			gbc.gridy = 1;
			itsMainPanel.add(npf, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ld, gbc);
			gbc.gridy ++;
			itsMainPanel.add(sd, gbc);
			gbc.gridy ++;
			itsMainPanel.add(u, gbc);
			gbc.gridy ++;
			itsMainPanel.add(s, gbc);
			gbc.gridy ++;
			itsMainPanel.add(es, gbc);
			gbc.gridy ++;
			itsMainPanel.add(it, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ot, gbc);
			gbc.gridy ++;
			itsMainPanel.add(t, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridy ++;
			itsMainPanel.add(al, gbc);
			gbc.gridy ++;
			itsMainPanel.add(n, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ac, gbc);
			gbc.gridy ++;
			itsMainPanel.add(p, gbc);
			gbc.gridy ++;
			itsMainPanel.add(g, gbc);
			gbc.gridy ++;
			itsMainPanel.add(wiz, gbc);
			gbc.gridy ++;
			itsMainPanel.add(writer, gbc);
		}
		components.add(new MPEditorComponent(npf, ld, sd, u, s, es, it, ot, t, ac, ap, ui, al, n, p, g, wiz, writer));
	}

	/**
	 * Adds a new Editor row or column to the main panel, for the given point. Used for
	 * editing existing points, rather than adding new ones, hence the inability to alter the
	 * point name.
	 * @param point The name of the point to edit using these input fields
	 * @see #addEditorPanel()
	 * @see #addClonePanel(String)
	 */
	public void addEditorPanel(String point){
		JButton wiz = new JButton("Wizard");
		wiz.addActionListener(this);
		JButton writer = new JButton("Write");
		writer.addActionListener(this);
		JLabel l = new JLabel(point.substring(point.indexOf('.')+1));
		JTextField ld = new JTextField(5);
		JTextField sd = new JTextField(10);
		JTextField u = new JTextField(2);
		JTextField s = new JTextField(5);
		JComboBox es = new JComboBox(bools);
		JTextField it = new JTextField(10);
		JTextField ot = new JTextField(10);
		JTextField t = new JTextField(10);
		JTextField ac = new JTextField(10);
		JTextField ap = new JTextField(5);
		JTextField ui = new JTextField(3);
		JTextField al = new JTextField(3);
		JTextField n = new JTextField(4);
		JComboBox p = new JComboBox(priorities);
		JTextField g = new JTextField(15);

		AbstractDocument adsd = (AbstractDocument) sd.getDocument();
		adsd.setDocumentFilter(lfdf);
		AbstractDocument adui = (AbstractDocument) ui.getDocument();
		adui.setDocumentFilter(ndf);
		AbstractDocument adal = (AbstractDocument) al.getDocument();
		adal.setDocumentFilter(ndf);
		es.setEditable(false);
		p.setEditable(false);

		if (layout.equals(layouts[0])){
			gbc.gridx = 0;
			itsMainPanel.add(wiz, gbc);
			gbc.gridx ++;
			itsMainPanel.add(l, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ld, gbc);
			gbc.gridx ++;
			itsMainPanel.add(sd, gbc);
			gbc.gridx ++;
			itsMainPanel.add(u, gbc);
			gbc.gridx ++;
			itsMainPanel.add(s, gbc);
			gbc.gridx ++;
			itsMainPanel.add(es, gbc);
			gbc.gridx ++;
			itsMainPanel.add(it, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ot, gbc);
			gbc.gridx ++;
			itsMainPanel.add(t, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridx ++;
			itsMainPanel.add(al, gbc);
			gbc.gridx ++;
			itsMainPanel.add(n, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ac, gbc);
			gbc.gridx ++;
			itsMainPanel.add(p, gbc);
			gbc.gridx ++;
			itsMainPanel.add(g, gbc);
			gbc.gridx ++;
			itsMainPanel.add(writer, gbc);
		} else if (layout.equals(layouts[1])){
			gbc.gridy = 1;
			itsMainPanel.add(l, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ld, gbc);
			gbc.gridy ++;
			itsMainPanel.add(sd, gbc);
			gbc.gridy ++;
			itsMainPanel.add(u, gbc);
			gbc.gridy ++;
			itsMainPanel.add(s, gbc);
			gbc.gridy ++;
			itsMainPanel.add(es, gbc);
			gbc.gridy ++;
			itsMainPanel.add(it, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ot, gbc);
			gbc.gridy ++;
			itsMainPanel.add(t, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridy ++;
			itsMainPanel.add(al, gbc);
			gbc.gridy ++;
			itsMainPanel.add(n, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ac, gbc);
			gbc.gridy ++;
			itsMainPanel.add(p, gbc);
			gbc.gridy ++;
			itsMainPanel.add(g, gbc);
			gbc.gridy ++;
			itsMainPanel.add(wiz, gbc);
			gbc.gridy ++;
			itsMainPanel.add(writer, gbc);
		}
		components.add(new MPEditorComponent(point, l, ld, sd, u, s, es, it, ot, t, ac, ap, ui, al, n, p, g, wiz, writer));
	}

	/**
	 * Adds an Editor row or column to the main point panel, with the contents of the fields
	 * populated by the values of the point specified. However, this is a convenience for new
	 * points, hence the ability for the point name to be changed with this Editor row or column
	 * @param point The point name of the point to be copied
	 * @see #addEditorPanel()
	 * @see #addEditorPanel(String)
	 */
	public void addClonePanel(String point){
		JButton wiz = new JButton("Wizard");
		wiz.addActionListener(this);
		JButton writer = new JButton("Write");
		writer.addActionListener(this);
		JTextField npf = new JTextField(10);
		JTextField ld = new JTextField(7);
		JTextField sd = new JTextField(5);
		JTextField u = new JTextField(2);
		JTextField s = new JTextField(5);
		JComboBox es = new JComboBox(bools);
		JTextField it = new JTextField(10);
		JTextField ot = new JTextField(10);
		JTextField t = new JTextField(10);
		JTextField ac = new JTextField(10);
		JTextField ap = new JTextField(5);
		JTextField ui = new JTextField(3);
		JTextField al = new JTextField(3);
		JTextField n = new JTextField(4);
		JComboBox p = new JComboBox(priorities);
		JTextField g = new JTextField(15);

		AbstractDocument adsd = (AbstractDocument) sd.getDocument();
		adsd.setDocumentFilter(lfdf);
		AbstractDocument adui = (AbstractDocument) ui.getDocument();
		adui.setDocumentFilter(ndf);
		AbstractDocument adal = (AbstractDocument) al.getDocument();
		adal.setDocumentFilter(ndf);
		es.setEditable(false);
		p.setEditable(false);

		if (layout.equals(layouts[0])){
			gbc.gridx = 0;
			itsMainPanel.add(wiz, gbc);
			gbc.gridx ++;
			itsMainPanel.add(npf, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ld, gbc);
			gbc.gridx ++;
			itsMainPanel.add(sd, gbc);
			gbc.gridx ++;
			itsMainPanel.add(u, gbc);
			gbc.gridx ++;
			itsMainPanel.add(s, gbc);
			gbc.gridx ++;
			itsMainPanel.add(es, gbc);
			gbc.gridx ++;
			itsMainPanel.add(it, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ot, gbc);
			gbc.gridx ++;
			itsMainPanel.add(t, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridx ++;
			itsMainPanel.add(al, gbc);
			gbc.gridx ++;
			itsMainPanel.add(n, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ac, gbc);
			gbc.gridx ++;
			itsMainPanel.add(p, gbc);
			gbc.gridx ++;
			itsMainPanel.add(g, gbc);
			gbc.gridx ++;
			itsMainPanel.add(writer, gbc);
		} else  if (layout.equals(layouts[1])){
			gbc.gridy = 1;
			itsMainPanel.add(npf, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ld, gbc);
			gbc.gridy ++;
			itsMainPanel.add(sd, gbc);
			gbc.gridy ++;
			itsMainPanel.add(u, gbc);
			gbc.gridy ++;
			itsMainPanel.add(s, gbc);
			gbc.gridy ++;
			itsMainPanel.add(es, gbc);
			gbc.gridy ++;
			itsMainPanel.add(it, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ot, gbc);
			gbc.gridy ++;
			itsMainPanel.add(t, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridy ++;
			itsMainPanel.add(al, gbc);
			gbc.gridy ++;
			itsMainPanel.add(n, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ac, gbc);
			gbc.gridy ++;
			itsMainPanel.add(p, gbc);
			gbc.gridy ++;
			itsMainPanel.add(g, gbc);
			gbc.gridy ++;
			itsMainPanel.add(wiz, gbc);
			gbc.gridy ++;
			itsMainPanel.add(writer, gbc);
		}
		components.add(new MPEditorComponent(point, npf, ld, sd, u, s, es, it, ot, t, ac, ap, ui, al, n, p, g, wiz, writer));
	}

	@Override
	public void vaporise() {
		AlarmMaintainer.removeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() instanceof JButton){
			JButton source = (JButton) arg0.getSource();
			if (source.equals(help)){
				JPanel pan = new JPanel();
				JLabel text = new JLabel("<html>Enter the relevant values for the control point in the text fields and drop-down lists.<br/>You can enter multiple values into fields that support it, but separating the differing values with a comma ( , ) <br/>For more detailed information, visit: </p></html>");
				JButton link = new JButton("<html><font color=\"#000099\">http://code.google.com/p/open-monica/wiki/MonitorPointsFileFormat</font></html>");
				link.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						if (Desktop.isDesktopSupported()) {
							try {
								URI uri = new URI("http://code.google.com/p/open-monica/wiki/MonitorPointsFileFormat");				
								Desktop.getDesktop().browse(uri);
							} catch (Exception e1) {}
						} else {
							System.err.println("Desktop API not supported.");
						}
					}
				});
				link.setHorizontalAlignment(SwingConstants.LEFT);
				link.setOpaque(false);
				link.setContentAreaFilled(false);
				link.setBackground(null);
				link.setBorderPainted(false);
				pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
				pan.add(text);
				pan.add(link);
				JOptionPane.showMessageDialog(this, pan
						, "Help", JOptionPane.QUESTION_MESSAGE);
			} else if (source.equals(write)){
				String[] res = MonClientUtil.showLogin(this, username, password);
				if (res != null){
					username = res[0];
					password = res[1];

					if (password.isEmpty()) return; //user hit cancel

					boolean valid = true;
					for (MPEditorComponent m : components){
						boolean v = m.validate();
						if (!v) valid = v;
					}
					if (valid && noDupes()){
						new DataSender("commitAll").start();
					} else {
						JOptionPane.showMessageDialog(this, "Writing point failed. Please ensure that all data points are uniquely named and filled in correctly.", "Data Validation Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
			} else {
				//wizard or writer button instance
				for (MPEditorComponent m : this.components){
					if (source.equals(m.getWizBtn())){
						this.showWizard(m);
					} else if (source.equals(m.getWriterBtn())){
						new DataSender("commit", m);
					}
				}
			}
		}
	}

	/**
	 * Creates a WizardFrame and displays it for a step-by-step wizard of adding and formatting 
	 * monitor point entries
	 * @param pointDetails A MPEditorComponent that holds all the references to
	 * input fields for a given editor row or column
	 * @see WizardFrame
	 */
	private void showWizard(MPEditorComponent pointDetails){
		new WizardFrame(pointDetails);
	}

	/**
	 * Method to detect whether the names of the points that are being edited are duplicated or
	 * not, to ensure that there won't be multiple additions of the same point name.
	 * @return boolean returns true if there are no duplicates, otherwise false
	 */
	public boolean noDupes(){
		boolean ret = true;
		HashSet<String> set = new HashSet<String>(); // Hashsets only allow adding of unique members
		for (MPEditorComponent c : components){
			ret = set.add(c.getFullName()); // returns false if it can't add a member
			if (!ret) return ret;
		}
		return ret;
	}
	// ///// NESTED CLASS: WizardFrame //////

	/**
	 * JFrame that shows a Step-by-step wizard to create a new monitor-point definition, which
	 * automatically handles all formatting
	 */
	public class WizardFrame extends JFrame implements ActionListener, ChangeListener{

		private static final long serialVersionUID = -7680339339902552985L;

		private MPEditorComponent reference;
		private JPanel itsCardPanel;
		private CardLayout cl;

		/** String array holding the names of each of the cards used in the CardLayout:<br/>
		 *	"metadata", "input transactions", "output transactions", "translations", "update data", "notifications", "alarm data" 
		 */
		final String[] itsCards = {"metadata", "input transactions", "output transactions", "translations", "update data", "notifications", "alarm data"};
		/** String array holding the possible options for the Transaction class:<br/>
		 * "EPICS",	"EPICSMonitor", "Generic", "InitialValue", "LimitCheck", "Listen", "Strings", "Timer"
		 */
		final String[] transactionOpts = {
				"EPICS",
				"EPICSMonitor",
				"Generic",
				"InitialValue",
				"LimitCheck",
				"Listen",
				"Strings",
				"Timer"
		};
		/** String array holding the possible options for the Translation class:<br/>
		 * "Add16","AngleToNumber","Array","AvailabilityMask","BCDToInteger","BitShift","BoolMap",
		 * "Calculation","CalculationTimed","CopyTimestamp","CronPulse","DailyIntegrator",
		 * "DailyIntegratorPosOn","DailyPulse","DailyWindow","Delta","DewPoint","EmailOnChange",
		 * "EmailOnFallingEdge","EmailOnRisingEdge","EnumMap","EQ","Failover","HexString","HighTimer",
		 * "LimitCheck","Listener","LowTimer","Mean","MonthlyPulse","None","NumberToAngle","NumberToBool",
		 * "NumDecimals","NV","PeakDetect","Polar2X","Polar2Y","Preceding","PrecipitableWater",
		 * "PrecipitableWaterMMA","Pulse","RelTimeToSeconds","RessetableIntegrator",
		 * "ResettablePeakDetect","ResettablePulse","RetriggerablePulse","RoundToInt","RunCmd","Shorts2Double","Shorts2Float",
		 * "SinceHighTimer","SpecificHumidity","Squelch","StopIfNoChange","StopIfNull","StringMap",
		 * "StringReplace","StringToArray","StringToNumber","StringTrim","StuckValue","Substring",
		 * "ThyconAlarm","TimedSubstitution","VapourPressure","Variance","XY2Angle","XY2Mag"
		 */
		final String[] translationOpts = {
				"Add16",
				"AngleToNumber",
				"Array",
				"AvailabilityMask",
				"BCDToInteger",
				"BitShift",
				"BoolMap",
				"Calculation",
				"CalculationTimed",
				"CopyTimestamp",
				"CronPulse",
				"DailyIntegrator",
				"DailyIntegratorPosOn",
				"DailyPulse",
				"DailyWindow",
				"Delta",
				"DewPoint",
				"EmailOnChange",
				"EmailOnFallingEdge",
				"EmailOnRisingEdge",
				"EnumMap",
				"EQ",
				"Failover",
				"HexString",
				"HighTimer",
				"LimitCheck",
				"Listener",
				"LowTimer",
				"Mean",
				"MonthlyPulse",
				"None",
				"NumberToAngle",
				"NumberToBool",
				"NumDecimals",
				"NV",
				"PeakDetect",
				"Polar2X",
				"Polar2Y",
				"Preceding",
				"PrecipitableWater",
				"PrecipitableWaterMMA",
				"Pulse",
				"RelTimeToSeconds",
				"RessetableIntegrator",
				"ResettablePeakDetect",
				"ResettablePulse",
				"RetriggerablePulse",
				"RoundToInt",
				"RunCmd",
				"Shorts2Double",
				"Shorts2Float",
				"SinceHighTimer",
				"SpecificHumidity",
				"Squelch",
				"StopIfNoChange",
				"StopIfNull",
				"StringMap",
				"StringReplace",
				"StringToArray",
				"StringToNumber",
				"StringTrim",
				"StuckValue",
				"Substring",
				"ThyconAlarm",
				"TimedSubstitution",
				"VapourPressure",
				"Variance",
				"XY2Angle",
				"XY2Mag"
		};

		/** String array holding the possible options for the ArchivePolicy class:<br/>
		 * "Alarm","All","Change","Counter","OnDecrease","OnIncrease","Timer"
		 */
		String[] archPolOpts = {
				"Alarm",
				"All",
				"Change",
				"Counter",
				"OnDecrease",
				"OnIncrease",
				"Timer"
		};

		/** String array holding the possible options for the Notification class:<br/>
		 * "EmailOnAlarm", "EmailOnAlarmChange"
		 */
		String[] notifOpts = {
				"EmailOnAlarm",
				"EmailOnAlarmChange"
		};
		/** String array holding the possible options for the AlarmCheck class:<br/>
		 * "Boolean","Range","StringMatch","ValueMatch"
		 */
		String[] alarmOpts = {
				"Boolean",
				"Range",
				"StringMatch",
				"ValueMatch"
		};

		/** int indicating the index of the card the CardLayout is currently displaying*/
		int curr = 0;

		//Nav Panel
		JButton back = new JButton("<< Back <<");
		JButton next = new JButton(">> Next >>");
		JButton cancel = new JButton("Cancel");
		JButton finish = new JButton("Finish");

		//Metadata card
		JTextField name;
		JTextField longDesc;
		JTextField shortDesc;
		JTextField units;
		JTextField source;
		JComboBox enabled;

		//Input Transactions Card
		HashMap<JComboBox, JTextField[]> inFieldRefs;
		ArrayList<JComboBox> inFieldBoxes;
		JPanel inTransMainPanel;
		JSpinner inSpinner;
		int inSpinnerVal = 0;
		GridBagConstraints itgbc;

		//Output Transactions Card
		HashMap<JComboBox, JTextField[]> outFieldRefs;
		ArrayList<JComboBox> outFieldBoxes;
		JPanel outTransMainPanel;
		JSpinner outSpinner;
		int outSpinnerVal = 0;
		GridBagConstraints otgbc;

		//Translations Card
		HashMap<JComboBox, JTextField[]> transFieldRefs;
		ArrayList<JComboBox> transFieldBoxes;
		JPanel translateMainPanel;
		JSpinner transSpinner;
		int transSpinnerVal = 0;
		GridBagConstraints trgbc;

		//Update Data Card
		HashMap<JComboBox, JTextField[]> updFieldRefs;
		ArrayList<JComboBox> updFieldBoxes;
		JPanel updMainPanel;
		JSpinner updSpinner;
		int updSpinnerVal = 0;
		GridBagConstraints updgbc;
		JTextField updIntFld;
		JTextField archLongFld;

		//Notification Card
		HashMap<JComboBox, JTextField[]> notifFieldRefs;
		ArrayList<JComboBox> notifFieldBoxes;
		JPanel notifMainPanel;
		JSpinner notifSpinner;
		int notifSpinnerVal = 0;
		GridBagConstraints ntgbc;

		//Alarm Policies Card
		HashMap<JComboBox, JTextField[]> almFieldRefs;
		ArrayList<JComboBox> almFieldBoxes;
		JPanel almMainPanel;
		JSpinner almSpinner;
		int almSpinnerVal = 0;
		GridBagConstraints almgbc;
		JComboBox almPriority;
		JTextArea almGuidance;

		/**
		 * Constructor for a new WizardFrame. It preloads all the panels and calls their relevant
		 * setup methods, then adds them to the CardLayout 
		 * @param m The MPEditorComponent that is used as a reference for what point is being
		 * edited and what fields it needs
		 */
		public WizardFrame(MPEditorComponent m){
			super();
			reference = m;
			JPanel itsPanel = new JPanel();
			cl = new CardLayout();
			itsCardPanel = new JPanel(cl);
			JPanel navPanel = new JPanel();
			JPanel metaDataCard = new JPanel();//name, long desc, short desc, units, source
			JPanel inTransCard = new JPanel(); //input transactions
			JPanel outTransCard = new JPanel(); //output transactions
			JPanel translateCard = new JPanel(); // translations
			JPanel updateCard = new JPanel(); // update interval, archive policy, archive longevity
			JPanel notifCard = new JPanel(); // notifications
			JPanel alarmCard = new JPanel(); //alarm criteria, alarm priority, guidance message

			this.addNavPanel(navPanel, itsPanel);

			this.setupMetaDataPanel(metaDataCard);
			this.setupInputTransactionPanel(inTransCard);
			this.setupOutputTransactionPanel(outTransCard);
			this.setupTranslationPanel(translateCard);
			this.setupUpdateDataPanel(updateCard);
			this.setupNotificationPanel(notifCard);
			this.setupAlarmDetailsPanel(alarmCard);

			itsCardPanel.add(metaDataCard, itsCards[0]);
			itsCardPanel.add(inTransCard, itsCards[1]);
			itsCardPanel.add(outTransCard, itsCards[2]);
			itsCardPanel.add(translateCard, itsCards[3]);
			itsCardPanel.add(updateCard, itsCards[4]);
			itsCardPanel.add(notifCard, itsCards[5]);
			itsCardPanel.add(alarmCard, itsCards[6]);

			itsPanel.setLayout(new BorderLayout());
			itsPanel.add(itsCardPanel, BorderLayout.CENTER);
			itsPanel.add(navPanel, BorderLayout.SOUTH);

			this.setMinimumSize(new Dimension(400, 260));
			this.setPreferredSize(new Dimension(600, 400));
			this.setLocationRelativeTo(MonitorPointEditor.this);
			this.setTitle("Point Setup Wizard");
			this.add(itsPanel);
			this.pack();
			this.setVisible(true);
		}

		/**
		 * Alters the specified JPanel to contain navigational buttons, and adds it to the specified
		 * JComponent container.
		 * @param nav The JPanel that will contain the navigational components
		 * @param container The JComponent that will hold the nav JPanel
		 */
		private void addNavPanel(JPanel nav, JComponent container){
			back.setActionCommand("back");
			back.addActionListener(this);
			back.setEnabled(false);
			next.setActionCommand("next");
			next.addActionListener(this);
			cancel.setActionCommand("cancel");
			cancel.addActionListener(this);
			finish.setActionCommand("finish");
			finish.addActionListener(this);

			GridBagConstraints gbc = new GridBagConstraints();
			nav.setLayout(new GridBagLayout());
			gbc.anchor = GridBagConstraints.SOUTHEAST;
			gbc.gridx = 2;
			gbc.gridy = 0;
			gbc.insets = new Insets(5, 5, 15, 5);
			nav.add(back, gbc);
			gbc.gridx ++;
			nav.add(next, gbc);
			gbc.gridx ++;
			nav.add(cancel, gbc);
			gbc.gridx ++;
			nav.add(finish, gbc);
			gbc.gridx ++;

			container.add(nav);
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate textual input
		 * of the required metadata for this JPanel, e.g. JTextFields for entry of textual input
		 * @param mdc The JPanel to add the components to
		 */
		private void setupMetaDataPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new GridLayout(6,2));
			JScrollPane scroller = new JScrollPane();

			JButton minihelp = new JButton("?");
			minihelp.addActionListener(this);
			minihelp.setActionCommand("help-metadata");
			minihelp.setBackground(Color.BLUE);
			minihelp.setOpaque(true);
			minihelp.setForeground(Color.WHITE);
			minihelp.setPreferredSize(new Dimension(40,40));
			minihelp.setToolTipText("Information about the metadata fields");

			JLabel title = new JLabel("MetaData");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("Basic details about this data point. Edit information such as the data point's name, " +
			"description of the point and other metadata.");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);

			JPanel titlePanel = new JPanel();
			titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(title);
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(minihelp);

			desc.add(titlePanel);
			desc.add(description);

			JLabel nameLb = new JLabel("Point Name: ");
			name = new JTextField(10);
			JLabel lDescLb = new JLabel("Long Description: ");
			longDesc = new JTextField(10);
			JLabel sDescLb = new JLabel("Short Description: ");
			shortDesc = new JTextField(5);
			AbstractDocument ad = (AbstractDocument)shortDesc.getDocument();
			ad.setDocumentFilter(lfdf);
			JLabel untLb = new JLabel("Units: ");
			units = new JTextField(5);
			JLabel srcLb = new JLabel("Source: ");
			source = new JTextField(5);
			JLabel enbldLb = new JLabel("Enabled State: ");
			enabled = new JComboBox(MonitorPointEditor.this.bools);
			enabled.setEditable(false);

			if (reference.isAddPoint() || reference.isClonePoint()){
				name.setEditable(true);
				if (reference.getNames().length > 0){
					name.setText(reference.getNames()[0]);//prepopulate with first entry
				}
			} else {
				name.setText(reference.getNames()[0]);//prepopulate with first entry
				name.setEditable(false);
			}
			longDesc.setText(reference.getLongDesc());
			shortDesc.setText(reference.getShortDesc());
			units.setText(reference.getUnits());
			source.setText(reference.getSource());
			enabled.setSelectedItem(reference.getEnabledState());

			content.add(nameLb);
			content.add(name);
			content.add(lDescLb);
			content.add(longDesc);
			content.add(sDescLb);
			content.add(shortDesc);
			content.add(untLb);
			content.add(units);
			content.add(srcLb);
			content.add(source);
			content.add(enbldLb);
			content.add(enabled);
			scroller.setViewportView(content);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(scroller, BorderLayout.CENTER);
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate input of Input
		 * Transactions in a simple manner, using JTextFields, JComboBoxes etc.
		 * @param mdc The JPanel to add the components to
		 */
		private void setupInputTransactionPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new BorderLayout());
			JScrollPane scroller = new JScrollPane();
			inTransMainPanel = new JPanel(new GridBagLayout());
			inFieldRefs = new HashMap<JComboBox, JTextField[]>();
			inFieldBoxes = new ArrayList<JComboBox>();
			itgbc = new GridBagConstraints();
			itgbc.fill = GridBagConstraints.HORIZONTAL;
			itgbc.anchor = GridBagConstraints.NORTH;
			itgbc.weightx = 0.5;
			itgbc.weighty = 0.5;
			itgbc.gridheight = 1;
			itgbc.gridwidth = 1;
			itgbc.gridx = 0;
			itgbc.gridy = 0;
			itgbc.insets = new Insets(5, 5, 0, 0);

			JButton minihelp = new JButton("?");
			minihelp.addActionListener(this);
			minihelp.setActionCommand("help-transaction");
			minihelp.setBackground(Color.BLUE);
			minihelp.setOpaque(true);
			minihelp.setForeground(Color.WHITE);
			minihelp.setPreferredSize(new Dimension(40,40));
			minihelp.setToolTipText("Information about the Transaction types");

			JLabel title = new JLabel("Input Transactions");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("The types of input transactions that will occur when this point's " +
			"data is added into MoniCA. This can take the form of 8 different types.");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);
			JLabel type = new JLabel("Type");
			JLabel arg0 = new JLabel("Arg 1");;
			JLabel arg1 = new JLabel("Arg 2");
			JLabel arg2 = new JLabel("Arg 3");
			JLabel arg3 = new JLabel("Arg 4");
			JLabel arg4 = new JLabel("Arg 5");

			type.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg0.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg1.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg2.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg3.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg4.setFont(new Font("Sans Serif", Font.BOLD, 14));

			inTransMainPanel.add(type, itgbc);
			itgbc.gridx++;
			inTransMainPanel.add(arg0, itgbc);
			itgbc.gridx++;
			inTransMainPanel.add(arg1, itgbc);
			itgbc.gridx++;
			inTransMainPanel.add(arg2, itgbc);
			itgbc.gridx++;
			inTransMainPanel.add(arg3, itgbc);
			itgbc.gridx++;
			inTransMainPanel.add(arg4, itgbc);

			JPanel titlePanel = new JPanel();
			titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(title);
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(minihelp);

			desc.add(titlePanel);
			desc.add(description);

			JPanel counter =  new JPanel();
			counter.setLayout(new BoxLayout(counter, BoxLayout.X_AXIS));
			JLabel counterLabel = new JLabel("Number of Input Transactions");
			inSpinner = new JSpinner();
			SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
			inSpinner.setMaximumSize(new Dimension(45, 25));
			inSpinner.setModel(spinModel);
			inSpinner.addChangeListener(this);

			counter.add(Box.createHorizontalGlue());
			counter.add(counterLabel);
			counter.add(inSpinner);
			counter.add(Box.createHorizontalGlue());
			content.add(counter, BorderLayout.NORTH);
			content.add(inTransMainPanel, BorderLayout.CENTER);
			scroller.setViewportView(content);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(scroller, BorderLayout.CENTER);

			try {
				final String[] inTrans = reference.getInTransactions();
				int numTrans = inTrans.length;
				inSpinner.setValue(numTrans);
				SwingUtilities.invokeLater(new Runnable(){// ensure that this section is called after the boxes are created
					public void run(){
						populatePanel(inFieldBoxes, inFieldRefs, inTrans);
					}
				});
			} catch (InvalidParameterException ipe){
				System.err.println("Input Transactions could not be parsed.");
			}
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate input of Output
		 * Transactions in a simple manner, using JTextFields, JComboBoxes etc.
		 * @param mdc The JPanel to add the components to
		 */
		private void setupOutputTransactionPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new BorderLayout());
			JScrollPane scroller = new JScrollPane();
			outTransMainPanel = new JPanel(new GridBagLayout());
			outFieldRefs = new HashMap<JComboBox, JTextField[]>();
			outFieldBoxes = new ArrayList<JComboBox>();
			otgbc = new GridBagConstraints();
			otgbc.fill = GridBagConstraints.HORIZONTAL;
			otgbc.anchor = GridBagConstraints.NORTH;
			otgbc.weightx = 0.5;
			otgbc.weighty = 0.5;
			otgbc.gridheight = 1;
			otgbc.gridwidth = 1;
			otgbc.gridx = 0;
			otgbc.gridy = 0;
			otgbc.insets = new Insets(5, 5, 0, 0);

			JButton minihelp = new JButton("?");
			minihelp.addActionListener(this);
			minihelp.setActionCommand("help-transaction");
			minihelp.setBackground(Color.BLUE);
			minihelp.setOpaque(true);
			minihelp.setForeground(Color.WHITE);
			minihelp.setPreferredSize(new Dimension(40,40));
			minihelp.setToolTipText("Information about the Transaction types");

			JLabel title = new JLabel("Output Transactions");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("The types of output transactions that will occur when this point's " +
			"data is exported from MoniCA. This can take the form of 8 different types.");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);
			JLabel type = new JLabel("Type");
			JLabel arg0 = new JLabel("Arg 1");;
			JLabel arg1 = new JLabel("Arg 2");
			JLabel arg2 = new JLabel("Arg 3");
			JLabel arg3 = new JLabel("Arg 4");
			JLabel arg4 = new JLabel("Arg 5");

			type.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg0.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg1.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg2.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg3.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg4.setFont(new Font("Sans Serif", Font.BOLD, 14));

			outTransMainPanel.add(type, otgbc);
			otgbc.gridx++;
			outTransMainPanel.add(arg0, otgbc);
			otgbc.gridx++;
			outTransMainPanel.add(arg1, otgbc);
			otgbc.gridx++;
			outTransMainPanel.add(arg2, otgbc);
			otgbc.gridx++;
			outTransMainPanel.add(arg3, otgbc);
			otgbc.gridx++;
			outTransMainPanel.add(arg4, otgbc);

			JPanel titlePanel = new JPanel();
			titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(title);
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(minihelp);

			desc.add(titlePanel);
			desc.add(description);

			JPanel counter =  new JPanel();
			counter.setLayout(new BoxLayout(counter, BoxLayout.X_AXIS));
			JLabel counterLabel = new JLabel("Number of Output Transactions");
			outSpinner = new JSpinner();
			SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
			outSpinner.setMaximumSize(new Dimension(45, 25));
			outSpinner.setModel(spinModel);
			outSpinner.addChangeListener(this);

			counter.add(Box.createHorizontalGlue());
			counter.add(counterLabel);
			counter.add(outSpinner);
			counter.add(Box.createHorizontalGlue());
			content.add(counter, BorderLayout.NORTH);
			content.add(outTransMainPanel, BorderLayout.CENTER);
			scroller.setViewportView(content);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(scroller, BorderLayout.CENTER);
			try {
				final String[] outTrans = reference.getOutTransactions();
				int numTrans = outTrans.length;
				outSpinner.setValue(numTrans);
				SwingUtilities.invokeLater(new Runnable(){// ensure that this section is called after the boxes are created
					public void run(){
						populatePanel(outFieldBoxes, outFieldRefs, outTrans);
					}
				});
			} catch (InvalidParameterException ipe){
				System.err.println("Output Transactions could not be parsed.");
			}
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate input of Translations
		 *  in a simple manner, using JTextFields, JComboBoxes etc.
		 * @param mdc The JPanel to add the components to
		 */
		private void setupTranslationPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new BorderLayout());
			JScrollPane scroller = new JScrollPane();
			translateMainPanel = new JPanel(new GridBagLayout());
			transFieldRefs = new HashMap<JComboBox, JTextField[]>();
			transFieldBoxes = new ArrayList<JComboBox>();
			trgbc = new GridBagConstraints();
			trgbc.fill = GridBagConstraints.HORIZONTAL;
			trgbc.anchor = GridBagConstraints.NORTH;
			trgbc.weightx = 0.5;
			trgbc.weighty = 0.5;
			trgbc.gridheight = 1;
			trgbc.gridwidth = 1;
			trgbc.gridx = 0;
			trgbc.gridy = 0;
			trgbc.insets = new Insets(5, 5, 0, 0);

			JButton minihelp = new JButton("?");
			minihelp.addActionListener(this);
			minihelp.setActionCommand("help-translation");
			minihelp.setBackground(Color.BLUE);
			minihelp.setOpaque(true);
			minihelp.setForeground(Color.WHITE);
			minihelp.setPreferredSize(new Dimension(40,40));
			minihelp.setToolTipText("Information about the Translation types");

			JLabel title = new JLabel("Translations");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("Translations are used to convert data into a higher-level or more " +
			"meaningful form. More information can be found on the wiki.");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);
			JLabel type = new JLabel("Type");
			JLabel arg0 = new JLabel("Arg 1");;
			JLabel arg1 = new JLabel("Arg 2");
			JLabel arg2 = new JLabel("Arg 3");
			JLabel arg3 = new JLabel("Arg 4");
			JLabel arg4 = new JLabel("Arg 5");

			type.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg0.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg1.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg2.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg3.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg4.setFont(new Font("Sans Serif", Font.BOLD, 14));

			translateMainPanel.add(type, trgbc);
			trgbc.gridx++;
			translateMainPanel.add(arg0, trgbc);
			trgbc.gridx++;
			translateMainPanel.add(arg1, trgbc);
			trgbc.gridx++;
			translateMainPanel.add(arg2, trgbc);
			trgbc.gridx++;
			translateMainPanel.add(arg3, trgbc);
			trgbc.gridx++;
			translateMainPanel.add(arg4, trgbc);

			JPanel titlePanel = new JPanel();
			titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(title);
			titlePanel.add(Box.createHorizontalGlue());
			titlePanel.add(minihelp);

			desc.add(titlePanel);
			desc.add(description);

			JPanel counter =  new JPanel();
			counter.setLayout(new BoxLayout(counter, BoxLayout.X_AXIS));
			JLabel counterLabel = new JLabel("Number of Translations");
			transSpinner = new JSpinner();
			SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
			transSpinner.setMaximumSize(new Dimension(45, 25));
			transSpinner.setModel(spinModel);
			transSpinner.addChangeListener(this);

			counter.add(Box.createHorizontalGlue());
			counter.add(counterLabel);
			counter.add(transSpinner);
			counter.add(Box.createHorizontalGlue());
			content.add(counter, BorderLayout.NORTH);
			content.add(translateMainPanel, BorderLayout.CENTER);
			scroller.setViewportView(content);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(scroller, BorderLayout.CENTER);

			try {
				final String[] translations = reference.getTranslations();
				int numTrans = translations.length;
				transSpinner.setValue(numTrans);
				SwingUtilities.invokeLater(new Runnable(){// ensure that this section is called after the boxes are created
					public void run(){
						populatePanel(transFieldBoxes, transFieldRefs, translations);
					}
				});
			} catch (InvalidParameterException ipe){
				System.err.println("Translations could not be parsed.");
			}
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate input of Archive Policies
		 * , and other miscellaneous update data metadata in a simple manner, using 
		 * JTextFields, JComboBoxes etc.
		 * @param mdc The JPanel to add the components to
		 */
		private void setupUpdateDataPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new BorderLayout());
			JPanel archivePolicyPanel = new JPanel(new BorderLayout());
			JScrollPane archivePolicyScroller = new JScrollPane();
			JPanel updateDataMisc = new JPanel(new GridLayout(2,2));
			updMainPanel = new JPanel(new GridBagLayout());
			updFieldRefs = new HashMap<JComboBox, JTextField[]>();
			updFieldBoxes = new ArrayList<JComboBox>();
			updgbc = new GridBagConstraints();
			updgbc.fill = GridBagConstraints.HORIZONTAL;
			updgbc.anchor = GridBagConstraints.NORTH;
			updgbc.weightx = 0.5;
			updgbc.weighty = 0.5;
			updgbc.gridheight = 1;
			updgbc.gridwidth = 1;
			updgbc.gridx = 0;
			updgbc.gridy = 0;
			updgbc.insets = new Insets(5, 5, 0, 0);

			JLabel title = new JLabel("Archive Policies");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("These aspects define when and how often the monitor point should" +
			"be archived, and under what other conditions this occurs.");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);

			JLabel updIntLb = new JLabel("Update Interval: ");
			updIntFld = new JTextField(10);
			AbstractDocument updIntFldDoc = (AbstractDocument) updIntFld.getDocument();
			updIntFldDoc.setDocumentFilter(ndf);
			JLabel archLongLb = new JLabel("Archive Longevity: ");
			archLongFld = new JTextField(10);
			AbstractDocument archLongFldDoc = (AbstractDocument) archLongFld.getDocument();
			archLongFldDoc.setDocumentFilter(ndf);

			updateDataMisc.add(updIntLb);
			updateDataMisc.add(updIntFld);
			updateDataMisc.add(archLongLb);
			updateDataMisc.add(archLongFld);

			JLabel type = new JLabel("Type");
			JLabel arg0 = new JLabel("Arg 1");;
			JLabel arg1 = new JLabel("Arg 2");
			JLabel arg2 = new JLabel("Arg 3");
			JLabel arg3 = new JLabel("Arg 4");
			JLabel arg4 = new JLabel("Arg 5");

			type.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg0.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg1.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg2.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg3.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg4.setFont(new Font("Sans Serif", Font.BOLD, 14));

			updMainPanel.add(type, updgbc);
			updgbc.gridx++;
			updMainPanel.add(arg0, updgbc);
			updgbc.gridx++;
			updMainPanel.add(arg1, updgbc);
			updgbc.gridx++;
			updMainPanel.add(arg2, updgbc);
			updgbc.gridx++;
			updMainPanel.add(arg3, updgbc);
			updgbc.gridx++;
			updMainPanel.add(arg4, updgbc);

			desc.add(title);
			desc.add(description);

			JPanel counter =  new JPanel();
			counter.setLayout(new BoxLayout(counter, BoxLayout.X_AXIS));
			JLabel counterLabel = new JLabel("Number of Archive Policies");
			updSpinner = new JSpinner();
			SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
			updSpinner.setMaximumSize(new Dimension(45, 25));
			updSpinner.setModel(spinModel);
			updSpinner.addChangeListener(this);

			counter.add(Box.createHorizontalGlue());
			counter.add(counterLabel);
			counter.add(updSpinner);
			counter.add(Box.createHorizontalGlue());
			archivePolicyPanel.add(counter, BorderLayout.NORTH);
			archivePolicyPanel.add(updMainPanel, BorderLayout.CENTER);
			archivePolicyScroller.setViewportView(archivePolicyPanel);

			content.add(updateDataMisc, BorderLayout.NORTH);
			content.add(archivePolicyScroller, BorderLayout.CENTER);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(content, BorderLayout.CENTER);

			try {
				final String[] archPols = reference.getArchivePolicies();
				int numPols = archPols.length;
				updSpinner.setValue(numPols);
				SwingUtilities.invokeLater(new Runnable(){// ensure that this section is called after the boxes are created
					public void run(){
						if (reference.getPeriod().isEmpty()){
							updIntFld.setText("-");
						} else {
							updIntFld.setText(reference.getPeriod());
						}
						if (reference.getArchiveLongevity().isEmpty()){
							archLongFld.setText("-");
						} else {
							archLongFld.setText(reference.getArchiveLongevity());
						}
						populatePanel(updFieldBoxes, updFieldRefs, archPols);
					}
				});
			} catch (InvalidParameterException ipe){
				System.err.println("Archive Policies could not be parsed.");
			}
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate input of Notifications
		 * in a simple manner, using JTextFields, JComboBoxes etc.
		 * @param mdc The JPanel to add the components to
		 */
		private void setupNotificationPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new BorderLayout());
			JScrollPane scroller = new JScrollPane();
			notifMainPanel = new JPanel(new GridBagLayout());
			notifFieldRefs = new HashMap<JComboBox, JTextField[]>();
			notifFieldBoxes = new ArrayList<JComboBox>();
			ntgbc = new GridBagConstraints();
			ntgbc.fill = GridBagConstraints.HORIZONTAL;
			ntgbc.anchor = GridBagConstraints.NORTH;
			ntgbc.weightx = 0.5;
			ntgbc.weighty = 0.5;
			ntgbc.gridheight = 1;
			ntgbc.gridwidth = 1;
			ntgbc.gridx = 0;
			ntgbc.gridy = 0;
			ntgbc.insets = new Insets(5, 5, 0, 0);

			JLabel title = new JLabel("Notifications");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("Notifications that will trigger under the set conditions.");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);
			JLabel type = new JLabel("Type");
			JLabel arg0 = new JLabel("Arg 1");;
			JLabel arg1 = new JLabel("Arg 2");
			JLabel arg2 = new JLabel("Arg 3");
			JLabel arg3 = new JLabel("Arg 4");
			JLabel arg4 = new JLabel("Arg 5");

			type.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg0.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg1.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg2.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg3.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg4.setFont(new Font("Sans Serif", Font.BOLD, 14));

			notifMainPanel.add(type, ntgbc);
			ntgbc.gridx++;
			notifMainPanel.add(arg0, ntgbc);
			ntgbc.gridx++;
			notifMainPanel.add(arg1, ntgbc);
			ntgbc.gridx++;
			notifMainPanel.add(arg2, ntgbc);
			ntgbc.gridx++;
			notifMainPanel.add(arg3, ntgbc);
			ntgbc.gridx++;
			notifMainPanel.add(arg4, ntgbc);

			desc.add(title);
			desc.add(description);

			JPanel counter =  new JPanel();
			counter.setLayout(new BoxLayout(counter, BoxLayout.X_AXIS));
			JLabel counterLabel = new JLabel("Number of Notifications");
			notifSpinner = new JSpinner();
			SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
			notifSpinner.setMaximumSize(new Dimension(45, 25));
			notifSpinner.setModel(spinModel);
			notifSpinner.addChangeListener(this);

			counter.add(Box.createHorizontalGlue());
			counter.add(counterLabel);
			counter.add(notifSpinner);
			counter.add(Box.createHorizontalGlue());
			content.add(counter, BorderLayout.NORTH);
			content.add(notifMainPanel, BorderLayout.CENTER);
			scroller.setViewportView(content);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(scroller, BorderLayout.CENTER);

			try {
				final String[] notifs = reference.getNotifications();
				int numNotifs = notifs.length;
				notifSpinner.setValue(numNotifs);
				SwingUtilities.invokeLater(new Runnable(){// ensure that this section is called after the boxes are created
					public void run(){
						populatePanel(notifFieldBoxes, notifFieldRefs, notifs);
					}
				});
			} catch (InvalidParameterException ipe){
				System.err.println("Notifications could not be parsed.");
			}
		}

		/**
		 * Sets up the given panel with various JComponents that facilitate input of Alarm 
		 * Criteria, as well as the Alarm Priority and guidance text in a simple manner, 
		 * using JTextFields, JComboBoxes etc.
		 * @param mdc The JPanel to add the components to
		 */
		private void setupAlarmDetailsPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new BorderLayout());
			JPanel alarmCriteriaPanel = new JPanel(new BorderLayout());
			JScrollPane alarmCriteriaScroller = new JScrollPane();
			JPanel updateDataMisc = new JPanel(new GridLayout(2,2));
			almMainPanel = new JPanel(new GridBagLayout());
			almFieldRefs = new HashMap<JComboBox, JTextField[]>();
			almFieldBoxes = new ArrayList<JComboBox>();
			almgbc = new GridBagConstraints();
			almgbc.fill = GridBagConstraints.HORIZONTAL;
			almgbc.anchor = GridBagConstraints.NORTH;
			almgbc.weightx = 0.5;
			almgbc.weighty = 0.5;
			almgbc.gridheight = 1;
			almgbc.gridwidth = 1;
			almgbc.gridx = 0;
			almgbc.gridy = 0;
			almgbc.insets = new Insets(5, 5, 0, 0);

			JLabel title = new JLabel("Alarm Details");
			title.setFont(new Font("Sans Serif", Font.BOLD, 18));
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setHorizontalTextPosition(SwingConstants.CENTER);
			JTextArea description = new JTextArea(2,5);
			DefaultCaret caret = (DefaultCaret)description.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			description.setText("These policies determine when alarms should be triggered for this point");
			description.setBackground(null);
			description.setWrapStyleWord(true);
			description.setLineWrap(true);
			description.setEditable(false);

			JLabel almPriorityLb = new JLabel("Alarm Priority: ");
			almPriority = new JComboBox(priorities);
			JLabel almGuidanceLb = new JLabel("Alarm Guidance: ");
			almGuidance = new JTextArea(2,5);
			almGuidance.setBorder(BorderFactory.createLineBorder(Color.BLACK));

			updateDataMisc.add(almPriorityLb);
			updateDataMisc.add(almPriority);
			updateDataMisc.add(almGuidanceLb);
			updateDataMisc.add(almGuidance);

			JLabel type = new JLabel("Type");
			JLabel arg0 = new JLabel("Arg 1");;
			JLabel arg1 = new JLabel("Arg 2");
			JLabel arg2 = new JLabel("Arg 3");
			JLabel arg3 = new JLabel("Arg 4");
			JLabel arg4 = new JLabel("Arg 5");

			type.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg0.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg1.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg2.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg3.setFont(new Font("Sans Serif", Font.BOLD, 14));
			arg4.setFont(new Font("Sans Serif", Font.BOLD, 14));

			almMainPanel.add(type, almgbc);
			almgbc.gridx++;
			almMainPanel.add(arg0, almgbc);
			almgbc.gridx++;
			almMainPanel.add(arg1, almgbc);
			almgbc.gridx++;
			almMainPanel.add(arg2, almgbc);
			almgbc.gridx++;
			almMainPanel.add(arg3, almgbc);
			almgbc.gridx++;
			almMainPanel.add(arg4, almgbc);

			desc.add(title);
			desc.add(description);

			JPanel counter =  new JPanel();
			counter.setLayout(new BoxLayout(counter, BoxLayout.X_AXIS));
			JLabel counterLabel = new JLabel("Number of Alarm Checks");
			almSpinner = new JSpinner();
			SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, null, 1);
			almSpinner.setMaximumSize(new Dimension(45, 25));
			almSpinner.setModel(spinModel);
			almSpinner.addChangeListener(this);

			counter.add(Box.createHorizontalGlue());
			counter.add(counterLabel);
			counter.add(almSpinner);
			counter.add(Box.createHorizontalGlue());
			alarmCriteriaPanel.add(counter, BorderLayout.NORTH);
			alarmCriteriaPanel.add(almMainPanel, BorderLayout.CENTER);
			alarmCriteriaScroller.setViewportView(alarmCriteriaPanel);

			content.add(updateDataMisc, BorderLayout.NORTH);
			content.add(alarmCriteriaScroller, BorderLayout.CENTER);

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(content, BorderLayout.CENTER);

			try {
				final String[] alarms = reference.getAlarmCriteria();
				int numTrans = alarms.length;
				almSpinner.setValue(numTrans);
				SwingUtilities.invokeLater(new Runnable(){// ensure that this section is called after the boxes are created
					public void run(){
						almPriority.setSelectedItem(reference.getPriority());
						if (reference.getGuidance().isEmpty()){
							almGuidance.setText("-");
						} else {
							almGuidance.setText(reference.getGuidance());
						}
						populatePanel(almFieldBoxes, almFieldRefs, alarms);
					}
				});
			} catch (InvalidParameterException ipe){
				System.err.println("Alarm Details could not be parsed.");
			}
		}

		/**
		 * Convenience method used by many of the Cards to increase the number of input options
		 * in the compound input capable fields.
		 * @param options The String array defining what the options in the JComboBox are
		 * @param al ArrayList of JComboBoxes that stores references to the JComboBoxes used 
		 * in this Card
		 * @param hm HashMap mapping the JComboBox to a JTextField[] where the arguments for
		 * the type argument are supplied by the user
		 * @param pan The JPanel to add this row to
		 * @param g The GridBagConstraints reference to use for the layout of this panel
		 * @param actionCommand A String defining the ActionCommand used to assign to the JComboBox for
		 * use with ActionListeners
		 */
		private void addRow(String[] options, ArrayList<JComboBox> al, HashMap<JComboBox, JTextField[]> hm, JPanel pan, GridBagConstraints g, String actionCommand){
			JComboBox optBox = new JComboBox(options);
			optBox.setEditable(false);

			JTextField arg0 = new JTextField(3);
			JTextField arg1 = new JTextField(3);
			JTextField arg2 = new JTextField(3);
			JTextField arg3 = new JTextField(3);
			JTextField arg4 = new JTextField(3);

			JTextField[] fields = {arg0, arg1, arg2, arg3, arg4};

			optBox.setSelectedIndex(0);
			if (actionCommand.equals("archPol") || actionCommand.equals("notification")){
				arg0.setEnabled(false);
				arg0.setText(null);
				arg1.setEnabled(false);
				arg1.setText(null);
				arg2.setEnabled(false);
				arg2.setText(null);
				arg3.setEnabled(false);
				arg3.setText(null);
				arg4.setEnabled(false);
				arg4.setText(null);
			} else if (actionCommand.equals("alarm")){
				arg0.setEnabled(true);
				arg1.setEnabled(true);
				arg2.setEnabled(false);
				arg2.setText(null);
				arg3.setEnabled(false);
				arg3.setText(null);
				arg4.setEnabled(false);
				arg4.setText(null);
			} else {
				arg0.setEnabled(true);
				arg1.setEnabled(false);
				arg1.setText(null);
				arg2.setEnabled(false);
				arg2.setText(null);
				arg3.setEnabled(false);
				arg3.setText(null);
				arg4.setEnabled(false);
				arg4.setText(null);
			}

			g.gridx = 0;
			g.gridy ++;
			pan.add(optBox, g);
			g.gridx++;
			pan.add(arg0, g);
			g.gridx++;
			pan.add(arg1, g);
			g.gridx++;
			pan.add(arg2, g);
			g.gridx++;
			pan.add(arg3, g);
			g.gridx++;
			pan.add(arg4, g);

			pan.revalidate();
			pan.repaint();

			optBox.addActionListener(this);
			optBox.setActionCommand(actionCommand);
			al.add(optBox);
			hm.put(optBox, fields);
		}

		/**
		 * Utility method used to remove a row from the given JPanel
		 * @param al The ArrayList with references to the JComboBoxes this panel shows
		 * @param hm The HashMap that maps the JComboBox to a JTextField[] where the arguments
		 * for the type argument are supplied 
		 * @param pan The JPanel the row is being removed from
		 * @param g The GridBagConstraints reference that is being used on this JPanel
		 */
		private void remRow(ArrayList<JComboBox> al, HashMap<JComboBox, JTextField[]> hm, JPanel pan, GridBagConstraints g){
			pan.remove(al.get(al.size()-1));
			for (JTextField j : hm.get(al.get(al.size()-1))){
				pan.remove(j);
			}
			hm.remove(al.get(al.size()-1));
			al.remove(al.size()-1);

			g.gridx = 0;
			g.gridy--;

			pan.revalidate();
			pan.repaint();
		}

		/**
		 * Utility method to format input from a number of JComboBoxes and JTextFields into a
		 * single String mirroring that used in the monitor-points.txt config file
		 * @param fieldBoxes ArrayList of JComboBoxes storing references to the selected type
		 * argument for the panel
		 * @param refs HashMap mapping a JComboBox to a JTextField[] that can be used to extract
		 * the extra arguments for the selected type argument.
		 * @return The formatted String
		 */
		private String formatCompoundString(ArrayList<JComboBox> fieldBoxes, HashMap<JComboBox, JTextField[]> refs){
			ArrayList<String> separateTypes = new ArrayList<String>();
			String compoundString = "";
			for (JComboBox jc : fieldBoxes){
				ArrayList<String> fields = new ArrayList<String>();
				for (JTextField j : refs.get(jc)){
					fields.add(j.getText());
				}
				String res = jc.getSelectedItem().toString();
				res += "-";
				for (String arg : fields){
					if (arg != null && !arg.isEmpty()){
						res += "\"";
						res += arg;
						res += "\"";
					}
				}
				separateTypes.add(res);
			}
			int i = 1;
			int size = separateTypes.size();
			if (size > 1){
				compoundString += "{";
				for (String in : separateTypes){
					compoundString += in;
					if (i != size){
						compoundString += ","; 
						i++;
					}
				}
				compoundString += "}";
			} else if (size == 1){
				compoundString = separateTypes.get(0);
			} else {
				compoundString = "-";
			}
			return compoundString;
		}

		/**
		 * Simple utility method to create a useful title from the action command given.
		 * Has specific cases for known options, and a catchall case for others that don't match
		 * @param cmd The actioncommand used to figure out what the title should be
		 * @return A String holding the useful title
		 */
		private String helpTitle(String cmd) {
			String res = "";
			if (cmd.endsWith("metadata")){
				res = "Metadata Field Help";
			} else if (cmd.endsWith("transaction")){ 
				res = "Transaction Type Help";
			} else {
				res = cmd.substring(cmd.indexOf('-')+1).toUpperCase();
			}
			return res;
		}

		private final String[] metadataInfo = new String[]{
				"The name of the point. Typically this is in a format such as \"branch.descriptor\" where \"branch\" refers to a larger subcategory and \"descriptor\" refers to some detail about the point.",
				"A description of the point. This should be a short summary of what data this point represents, though not more than a few words.",
				"A short description of the point. This cannot exceed 10 characters, therefore must be succinct",
				"The source of this point's data. Ensure that this is properly located in the monitor-source.txt file on the server",
				"The units that this point's data is read in, such as degrees celsius (for temperature) etc. May not be applicable, depending on the indidvidual point."
		};

		private final String[] transactionInfo = new String[]{
				"Holds a process variable name for polling or pushing values over EPICS Channel access. It requires one argument, the process variable. It may also have an optional extra variable where the DBRType is specified",
				"Subscribes to updates via the EPICS Channel access monitor mechanism. It requires one argument, the process variable to be monitored. It may also have an optional extra variable where the DBRType is specified",
				"This is for generic transactions that don't have ExternalSystem specific fields. It requires one argument, the channel.",
				"Provides an initial value to the point before it is assigned other values by external mechanisms. It requires two arguments; the MoniCA type that this value should be parsed as (int, flt, dbl, str or bool), and the value to be used.",
				"Checks that a set of points are within specified limits. It requires at least 5 arguments; first is the check frequency specified in microseconds, and should match the overall update frequency specified for this point. Second is the number of sequential updates that must occur before an alert is raised. Third is an argument string indicating when the point is within its limits, and the fourth is the argument string used when it is not. All following points are the names of points to be monitored.",
				"Listens to a number of points. Requires at least one argument, which is the name of the point(s) to be listened to.",
				"Generic Transaction used for ExternalSystems that require String inputs. The first argument is the name of channel/protocol that corresponds to an ExternalSystem. The extra arguments are those that are used by the specified ExternalSystem.",
				"Provides a periodic fixed-value update to a parent point. It requires three arguments - the period (int seconds) that it should update at, the MoniCA type to parse the value as (int, flt, dbl, str or bool) and finally the actual value to be used."
		};

		private final String[] translationInfo = new String[]{//TODO
				"Adds a constant offset to a 16-bit number, which wraps around at 0 and 65535. It has one argument, the value of the number to offset the point's value by.",
				"Converts an angle data type to a double. Has one argument, which is the format of the initial value - \"r\" if it is in radians (the default), or \"d\" if it is in degrees.",
				"Returns a single value from an array. Has one argument, the index of the array entry that should be returned.",
				"Blocks updates if the listened-to point(s) is unavailable, or if it is in an alarm state. Has multiple arguments - the first is a number with the number of points to be listened to. The others are the names of the points to listen to.",
				"Converts BCD nibbles to equivalent decimal value integers.",
				"Masks a bit field, and rotates it to the right. Requires two arguments - the mask to perform an \"AND\" operation on (may be given in hexadecimal if prefixed with \"0x\"), and secondly the number of bits to shift right.",
				"Maps a boolean input to one of two strings. The first argument is the map from a \"true\" value, and the second argument is the map from a \"false\" value.",
				"Arbitrary mathematical function of any number of listened-to points. Uses JEP for expression parsing. Uses multiple arguments - the first is the \"N\" number of points to listen to. The next N arguments are the names of the points to be used in the calculation. The final argument is the calculation to perfom, where \"a\" is the variable for the first point, \"b\" is for the second, and so on until \"z\" for the twenty-sixth.",
				"Same as for calculation, however this performs the calculation at the period of the parent point, rather than on each update. The arguments are as for Calculation, however two optional arguments can be given to specify a default value if any of the other points are unavailable - a MoniCA type argument (int, flt, dbl, str or bool) and a value.",
				"Sets the timestamp of this point to match that of another. Has one argument, the name of the other point's timestamp to copy.",
				"Generates a single \"true\" pulse at a consistent time. Has two arguments - a time/date In cron format, but must use x instead of asterisk. Example: 0,30 x/2 x x Mon-Fri will trigger at 0 and 30 minutes past every other hour Mondays through Fridays. The cron format is minutes, hours, day of month, month, day of week. All times as specified in 24 hour format and day of week can be either literal Mon, Tue etc or 1, 2 etc with 0 being Sunday, and a timezone that should be interpreted (e.g. \"Australia/Sydney\".",
				"Accumulate the input values and reset each day. Has three arguments - first, the time of day to reset, in the format \"HH:MM\" in 24-hour format. Secondly, the timezone to use (e.g. \"Australia/Sydney\") and third, a boolean to indicate whether to use the value from the previously archived integration value as the initial value each day.",
				"Exactly the same as DailyIntegrator, however this will only integrate the positive values over the day.",
				"Will generate a single \"true\" value once per day. Has two arguments - a time to generate the pulse in the format \"HH:MM\" in 24-hour format, and also a timezone to be interpreted.",
				"Generates a \"true\" value between a certain time window during the day. Has three arguments - the first two are the start time and end time, in the format \"HH:MM\" in 24-hour format. The third arguement is the timezone to be interpreted.",
				"Calculates the difference between successive values for this point.",
				"Calculates the dew point in degrees Celsius from the water vapour pressure. Takes a single argument, the water vapour pressure measured in hectopascals.",
				"Sends an email, using the hosts default mail transport, if the input changes. Supports three or four arguments. The first argument is the destination email address. The next is an optional argument, the sender's email address. The final two arguments are the email subject, and the email body contents. These fields support using MoniCA substitutions.",
				"Sends an email, using the hosts default mail transport, when the input changes from true to false. Arguments are the same as for EmailOnChange.",
				"Sends an email, using the hosts default mail transport, when the input changes from false to true. Arguments are the same as for EmailOnChange.",
				"Maps numbers to strings. Can have multiple arguments - each a map of a number to a string in the format \"Num:Str\". Any input not formatted as such is treated as the default value to be used when there is no explicit map.",
				"Apply an arbitrary equation to numerical input. Uses JEP for expression parsing. Has one input, the equation, using \"x\" to represent the input number.",
				"Assume the value of the highest precedence listened-to point which has valid data. Has multiple arguments, where each is the name of a point in order of priority.",
				"Map an integer input to a hex string output.",
				"Reports the amount of time (as a RelTime) the (numeric or boolean) input has been in a high/mark state. Output will be zero while input is in a low/space state.",
				"Output depends on whether other monitor points are in an alarm state. Used together with TransactionListen. Has three arguments - first is the String to output when all points are within their limits, second is the string to output when they are not, and finally are the names of the points that are being listened to.",
				"Listen to updates from one or more other points. Has at least two arguments; first is the number of points to listen to, and all subsequent arguments are the names of the points to listen to.",
				"Reports the amount of time (as a RelTime) the (numeric or boolean) input has been in a low/space state. Output will be zero while input is in a high/mark state.",
				"Calculates the moving average of the input value. Has two arguments - the first is the buffer time, where the amount of time to collect values for is given in seconds. The second (optional) argument is the minimum number of samples that must be collected before an output value is produced.",
				"Generates a single \"true\" pulse at a specified time on the given day of month. Takes three arguments - the day of the month between 0 and 31, the time in the format \"HH:MM\" in 24-hour time, and finally the timezone to be interpreted.",
				"Just returns the input argument.",
				"Convert a number to an Angle type. Takes one optional argument, the format - either \"r\" for radians, or \"d\" for degrees. The default is radians.",
				"If Integer cast of input is zero, output will be False, otherwise output will be True. Takes an optional argument to invert the output - setting this to \"true\" will invert the normal behaviour (i.e. 0 = true, 1 = false).",
				"Limit the number of decimals in a floating point number. Takes one argument, the number of non-zero decimal places to round the number to.",
				"Retrieve one named element from a HashMap or NameValueList input object. Has one argument, the key for the value to be extracted.",
				"Find the peak of the input over a time period. Takes one argument, the period to perform the peak detections over, in seconds.",
				"Listen to two numeric inputs which represent a polar vector, and output the X cartesian component of the vector. Takes three arguments - the name of the vector magnitude point, the name of the vector angle point, and an optional argument to specify the format of the angle, \"d\" for degrees or \"r\" for radians. The default value is radians.",
				"Listen to two numeric inputs which represent a polar vector, and output the Y cartesian component of the vector. Takes three arguments - the name of the vector magnitude point, the name of the vector angle point, and an optional argument to specify the format of the angle, \"d\" for degrees or \"r\" for radians. The default value is radians.",
				"Use the timestamp of the input value to find the value of a different point at that time and assume that value/timestamp for ourself. Has one argument, the name of the point whose historical values are to be looked up.",
				"Estimate the precipitable water from surface temperature and relative humidity. Takes two point name arguments - the first is the point that has the temperature in degrees celsius, and the second is the point that has the relative humidity as a percentage.",
				"Estimate the precipitable water from surface temperature and relative humidity using the MMA method. Takes two point name arguments - the first is the point that has the temperature in degrees celsius, and the second is the point that has the relative humidity as a percentage.",
				"Produce a mark/space pulse sequence of specified durations when the numeric/boolean input triggers. Takes two arguments - the mark period in seconds, and the space period in seconds",
				"Get the value of a RelTime object as it's number of elapsed seconds expressed as a Double.",
				"Integrates the normal input but resets the integral when a specified listened-to point is \"true\". Takes two arguments. The first is the name of the point to listen to which determines when to reset the integral. Integral will be reset when the value of this point is \"true\". Value must either be a boolean or number, which will be interpreted as a boolean. The second argument is whether or not to use archived data when the server starts for the first time. Set to \"true\" to enable this behaviour, \"false\" otherwise.",
				"Reports the peak detected value from the input but forgets old data when a reset control point is high. Takes one argument, the name of the point that controls the reset behaviour.",
				"Outputs a pulse once triggered, but the pulse can be reset to a low state by a listened-to point. Takes two arguments, the mark period in seconds, and the name of the point which can reset the point.",
				"Basic pulse extender. Timer can be reset. Has one argument, the pulse period, in seconds.",
				"Round the Number input to the nearest integer value.",
				"Runs an external programme and persists the return value in the point. Has at least three arguments, firstly, the number of values that are being passed, then the remaining arguments bar two are the names of the points that are being used as arguments. Then is the actual command to be used, and finally is the arguments you're passing on the command line to the external programme. Similar to the Calculation translation, $a substitutes to the first point value you've passed, $b to the second etc. Note the difference though: Dollar ($) sign is required to mark the letter as a variable. Example passing two parameters: test.ExternalCall \"Ext Call\" \"\" \"\" mysrc T - - {RunCmd-\"2\"\"mysrc.param.x\"\"mysrc.param.y\"\"/full/path/runme\"\"--paramx $a --paramy $b\"} - {Change-} 5000000 - In order to retrieve output from an external programme that does not take any input, you still need to feed one parameter in order for updates to trigger, but you can ignore the parameter field.",
				"Merges two 16 bit integers to reassemble a 32 bit double integer. Has two arguments, first is most significant bits, and second is least significant bits of a 32-bit double.",
				"Merges two 16 bit integers to reassemble a IEEE754 32 bit float.Has two arguments, first is most significant bits, and second is least significant bits of a 32-bit float.",
				"Measures the interval since the input was last 'high'. Input must be a Boolean or Number. Output is a RelTime.",
				"Calculates specific humidity, in grams of water vapour per kilogram of air, from water vapour pressure in hPa and surface pressure in hPa. Takes two arguments - the name of the point that has the value of the water vapour pressure, and the name of the point that has the pressure",
				"Applies thresholding function. Takes two arguments - the threshold, where values below this number are squelched, and also a value to output when the value is squelched.",
				"Stops the point update process if the input value hasn't changed.",
				"Stops the point update process if the input has a null data value.",
				"Mapw input strings to corresponding output strings. Has multiple arguments, all the maps of strings to other strings in the format \"str1:str2\"",
				"Replaces any instances of the first string with the text of the second string. Takes two argument Strings, the target string to be replaced, and the replacement string to replace the target with.",
				"Breaks the input String into an array of tokens. Takes a string delimiter to use to split the string using the Java String.split() method. If no delimiter is given, a space character is used.",
				"Maps the string representation of a number to an actual numeric class type. Takes two arguments - the type of number to output, i.e. Integer, Float, Double or Long. The second optional argument is a radix for integers. If it is omitted, base 10 is used by default.",
				"Removes leading/trailing whitespace from a string.",
				"Flag data with an alarm and empty value when it seems the value has become \"stuck\". Takes one argument, the number of successive updates that a point must have before it is deemed \"stuck\".",
				"Returns a substring of the input. Takes two arguments - the start index, and an optional second argument, the end index.",
				"Returns the alarm string corresponding to the alarm number for a Thycon UPS. Has one argument, the alarm number.",
				"Periodically performs value substitution on the provided template string. The time is taken from the parent points given update interval. Has one argument, the template string to use. See the Substitutions wiki page for further details on using substitutions.",
				"Calculates water vapour pressure in hPa from temperature and relative humidity. Takes two arguments, the name of the point that contains the temperature in degrees celsius, and the name of the point that contains the relative humidty as a percentage.",
				"Calculates the variance of the input over a specified time range. Takes one argument, the interval in seconds over which to calculate the variance of the input.",
				"Determines the wet bulb temperature given the inputs provided. Has four arguments - first is the number of points, which must be 3. Next is the name of the point that reads the pressure in hPa, next is the name of the point with the temperature in degrees celsius, and finally is the name of the point which contains the observed mixing ratio in g/g.",
				"Listen to two numeric inputs which represent the X and Y cartesian components of a vector and output the vector angle. Takes three arguements, first is the name of the point representing the X dimension, second is the name of the point representing the Y dimension, and finally is an optional argument specifying whether the output angle is in  degrees (\"d\") or in radians (\"r\"). The default value is radians.",
				"Listen to two numeric inputs which represent the X and Y cartesian components of a vector and output the magnitude of the vector. Has two arguments, the name of the point representing the X dimension, and the name of the point representing the Y dimension."
		};

		/**
		 * Returns a JPanel that can be used for a help type panel. This is determined by the
		 * cmd String given as a parameter.
		 * @param cmd The actioncommand String given that determines what the panel will look like.
		 * @return A JPanel that can be used in other JComponents, contents determined by cmd
		 */
		private Object createHelpPanel(String cmd) {
			JPanel itsPanel = new JPanel();
			itsPanel.setLayout(new GridBagLayout());
			GridBagConstraints g = new GridBagConstraints();
			g.fill = GridBagConstraints.NONE;
			g.gridheight = 1;
			g.gridwidth = 1;
			g.gridx = 0;
			g.gridy = 0;
			g.weightx = 0.5;
			g.weighty = 0.5;
			if (cmd.endsWith("metadata")){
				final JComboBox metaBox = new JComboBox(new String[]{
						"Name",
						"Long Description",
						"Short Description",
						"Source",
						"Units"
				});
				final JTextArea summary = new JTextArea(4, 30);
				DefaultCaret ct = (DefaultCaret) summary.getCaret();
				ct.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
				summary.setWrapStyleWord(true);
				summary.setLineWrap(true);
				summary.setEditable(false);
				summary.setOpaque(false);
				summary.setText(metadataInfo[0]);
				metaBox.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								summary.setText(metadataInfo[metaBox.getSelectedIndex()]);
							}
						});
					}
				});
				itsPanel.add(metaBox, g);
				g.weighty = 0.1;
				g.gridy++;
				g.insets = new Insets(5,0,15,0);
				g.weighty = 1.0;
				g.fill = GridBagConstraints.HORIZONTAL;
				itsPanel.add(summary, g);
			} else if (cmd.endsWith("transaction")){
				final JComboBox transactionBox = new JComboBox(transactionOpts);
				final JTextArea summary = new JTextArea(4, 30);
				DefaultCaret ct = (DefaultCaret) summary.getCaret();
				ct.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
				summary.setWrapStyleWord(true);
				summary.setLineWrap(true);
				summary.setEditable(false);
				summary.setOpaque(false);
				summary.setText(transactionInfo[0]);
				transactionBox.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								summary.setText(transactionInfo[transactionBox.getSelectedIndex()]);
							}
						});
					}
				});
				itsPanel.add(transactionBox, g);
				g.weighty = 0.1;
				g.gridy++;
				g.insets = new Insets(5,0,15,0);
				g.weighty = 1.0;
				g.fill = GridBagConstraints.HORIZONTAL;
				itsPanel.add(summary, g);
			} else if (cmd.endsWith("translation")){
				final JComboBox translationBox = new JComboBox(translationOpts);
				final JTextArea summary = new JTextArea(4, 30);
				DefaultCaret ct = (DefaultCaret) summary.getCaret();
				ct.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
				summary.setWrapStyleWord(true);
				summary.setLineWrap(true);
				summary.setEditable(false);
				summary.setOpaque(false);
				summary.setText(translationInfo[0]);
				translationBox.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								summary.setText(translationInfo[translationBox.getSelectedIndex()]);
							}
						});
					}
				});
				itsPanel.add(translationBox, g);
				g.weighty = 0.1;
				g.gridy++;
				g.insets = new Insets(5,0,15,0);
				g.weighty = 1.0;
				g.fill = GridBagConstraints.HORIZONTAL;
				itsPanel.add(summary, g);
			} else {
				return cmd;
			}
			itsPanel.setPreferredSize(new Dimension(400, 200));
			return itsPanel;
		}

		/**
		 * Utility method to populate the JComboBoxes and JTextFields used in the compound field
		 * capable fields. Does the opposite task of the {@link #formatCompoundString(ArrayList, HashMap)} method.
		 * @param boxes Reference to the ArrayList of JComboBoxes to populate
		 * @param args Reference to the HashMap&lt;JComboBox, JTextField[]&gt; to populate
		 * @param inputs String[] that contains a String identifying each separate "type" of argument
		 */
		private void populatePanel(ArrayList<JComboBox> boxes, HashMap<JComboBox, JTextField[]> args, String[] inputs){
			for (int i = 0; i < inputs.length; i++){
				String type = inputs[i].substring(0, inputs[i].indexOf('-'));
				boxes.get(i).setSelectedItem(type);
				inputs[i] = inputs[i].substring(inputs[i].indexOf('-')+1); //strip off type
				inputs[i] = inputs[i].trim();
			}
			int i = 0;
			for (String s : inputs){
				JTextField [] refs = args.get(boxes.get(i));
				StringTokenizer st = new StringTokenizer(s, "\"\"");
				int j = 0;
				while (st.hasMoreTokens()){
					String opts = st.nextToken();
					opts = opts.replace("\"", ""); //get rid of any orphaned quote marks
					refs[j].setText(opts);
					j++;
				}
				j = 0;
				i++;
			}
		}

		/**
		 * Utility method to populate the fields of the referenced {@link MPEditorComponent}
		 */
		private void populateFields(){
			//Metadata 
			reference.setNameText(name.getText());
			reference.setLongDescText(longDesc.getText());
			reference.setShortDescText(shortDesc.getText());
			reference.setSourceText(source.getText());
			reference.setUnitsText(units.getText());
			reference.setEnabledState(enabled.getSelectedItem().toString());
			//Transactions
			reference.setInputTransactionString(this.formatCompoundString(inFieldBoxes, inFieldRefs));
			reference.setOutputTransactionString(this.formatCompoundString(outFieldBoxes, outFieldRefs));
			//Translations
			reference.setTranslationString(this.formatCompoundString(transFieldBoxes, transFieldRefs));
			//Archive Policies
			reference.setArchiveLongevity(archLongFld.getText());
			reference.setUpdateInterval(updIntFld.getText());
			reference.setArchivePolicyString(this.formatCompoundString(updFieldBoxes, updFieldRefs));
			//Notifications
			reference.setNotificationString(this.formatCompoundString(notifFieldBoxes, notifFieldRefs));
			//Alarms
			reference.setAlarmPriority(almPriority.getSelectedItem());
			reference.setAlarmGuidance(almGuidance.getText());
			reference.setAlarmCriteria(this.formatCompoundString(almFieldBoxes, almFieldRefs));
		}

		public void actionPerformed(ActionEvent e){
			String cmd = e.getActionCommand();
			if (e.getSource() instanceof JButton){
				if (cmd.equals("next")){
					cl.show(itsCardPanel, itsCards[++curr]);
					if (curr == itsCards.length-1){
						next.setEnabled(false);
					}
					back.setEnabled(true);
				} else if (cmd.equals("back")){
					cl.show(itsCardPanel, itsCards[--curr]);
					if (curr == 0){
						back.setEnabled(false);
					}
					next.setEnabled(true);
				} else if (cmd.equals("cancel")){
					this.dispose();
				} else if (cmd.equals("finish")){
					this.populateFields();
					this.dispose();
				} else if (cmd.startsWith("help-")){//card help box
					JOptionPane.showMessageDialog(this, this.createHelpPanel(cmd), this.helpTitle(cmd), JOptionPane.INFORMATION_MESSAGE);
				}
			} else if (e.getSource() instanceof JComboBox){
				JComboBox src = (JComboBox)e.getSource();
				if (cmd.endsWith("Trans")){
					final JTextField[] refs;
					if (cmd.equals("inTrans")){
						refs = inFieldRefs.get(src);
					} else {
						refs = outFieldRefs.get(src);
					}
					String type = src.getSelectedItem().toString();
					if (type.equals(transactionOpts[0])){//EPICS
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(transactionOpts[1])){//EPICS Monitor
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(transactionOpts[2])){//Generic
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(transactionOpts[3])){//Initial Value
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(transactionOpts[4])){//Limit Check
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(true);
							}
						});
					} else if (type.equals(transactionOpts[5])){//Listen
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(true);
							}
						});
					} else if (type.equals(transactionOpts[6])){//Strings
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(transactionOpts[7])){//Timer
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					}
				} else if (cmd.equals("translate")){
					final JTextField[] refs = transFieldRefs.get(src);
					String type = src.getSelectedItem().toString();
					if (type.equals(translationOpts[0])){//Add16
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[1])){//AngleToNumber
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[2])){//Array
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[3])){//AvailabilityMask
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[4])){//BCDToInteger
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[5])){//BitShift
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[6])){//BoolMap
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[7])){//Calculation
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[8])){//CalculationTimed
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(true);
							}
						});
					} else if (type.equals(translationOpts[9])){//CopyTimestamp
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[10])){//CronPulse
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[11])){//DailyIntegrator
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[12])){//DailyIntegratorPosOnly
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[13])){//DailyPulse
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[14])){//DailyWindow TODO: Needs wiki entry
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[15])){//Delta
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[16])){//DewPoint
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[17])){//EmailOnChange
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[18])){//EmailOnFallingEdge
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[19])){//EmailOnRisingEdge
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[20])){//EnumMap
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[21])){//EQ
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[22])){//Failover
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(true);
							}
						});
					} else if (type.equals(translationOpts[23])){//HexString
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[24])){//HighTimer
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[25])){//LimitCheck
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[26])){//Listener
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[27])){//LowTimer
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[28])){//Mean
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[29])){//MonthlyPulse
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[30])){//None
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[31])){//NumberToAngle
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[32])){//NumberToBool
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[33])){//NumDecimals
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[34])){//NV
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[35])){//PeakDetect
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[36])){//Polar2X
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[37])){//Polar2Y
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[38])){//Preceding
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[39])){//PrecipitableWater
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[40])){//PreciptableWaterMMA TODO: Needs Wiki entry
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[41])){//Pulse
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[42])){//RelTimeToSeconds
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[43])){//ResettableIntegrator
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[44])){//ResettablePeakDetector TODO: Needs Wiki Entry
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[45])){//ResettablePulse
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[46])){//RetriggerablePulse
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[47])){//RoundToInt
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[48])){//RunCmd
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[49])){//Shorts2Double TODO: Needs wiki entry
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[50])){//Shorts2Float TODO: Needs wiki entry
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[51])){//SinceHighTimer
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[52])){//SpecificHumidity
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[53])){//Squelch
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[54])){//StopIfNoChange
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[55])){//StopIfNull
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[1].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[56])){//StringMap
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[57])){//StringReplace
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[58])){//StringToArray
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[59])){//StringToNumber
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[60])){//StringTrim
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[61])){//StuckValue
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[62])){//Substring
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[63])){//Synch
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[64])){//ThyconAlarm
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[65])){//TimedSubstitution
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[66])){//VapourPressure
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[67])){//Variance
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[68])){//XY2Angle
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(translationOpts[69])){//XY2Mag
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					}
				} else if (cmd.equals("archPol")){
					final JTextField[] refs = updFieldRefs.get(src);
					String type = src.getSelectedItem().toString();
					if (type.equals(archPolOpts[0])){//Alarm
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(archPolOpts[1])){//All
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(archPolOpts[2])){//Change
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(archPolOpts[3])){//Counter
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(archPolOpts[4])){//OnDecrease
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(archPolOpts[5])){//OnIncrease
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(false);
								refs[0].setText(null);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(archPolOpts[6])){//Timer
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(false);
								refs[1].setText(null);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					}
				} else if (cmd.equals("notification")){
					final JTextField[] refs = notifFieldRefs.get(src);
					SwingUtilities.invokeLater(new Runnable(){
						@Override
						public void run(){
							refs[0].setEnabled(false);
							refs[0].setText(null);
							refs[1].setEnabled(false);
							refs[1].setText(null);
							refs[2].setEnabled(false);
							refs[2].setText(null);
							refs[3].setEnabled(false);
							refs[3].setText(null);
							refs[4].setEnabled(false);
							refs[4].setText(null);
						}
					});
				} else if (cmd.equals("alarm")){
					final JTextField[] refs = almFieldRefs.get(src);
					String type = src.getSelectedItem().toString();
					if (type.equals(alarmOpts[0])){//Boolean
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(false);
								refs[2].setText(null);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(alarmOpts[1])){//Range
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					} else if (type.equals(alarmOpts[2])){//StringMatch
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(true);
								refs[4].setEnabled(true);
							}
						});
					} else if (type.equals(alarmOpts[3])){//ValueMatch
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								refs[0].setEnabled(true);
								refs[1].setEnabled(true);
								refs[2].setEnabled(true);
								refs[3].setEnabled(false);
								refs[3].setText(null);
								refs[4].setEnabled(false);
								refs[4].setText(null);
							}
						});
					}
				}
			} 
		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			if (arg0.getSource() instanceof JSpinner){
				if (arg0.getSource().equals(inSpinner)){
					if ((Integer)inSpinner.getValue() > inSpinnerVal){
						for (int i = inSpinnerVal; i < (Integer)inSpinner.getValue(); i++){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									addRow(transactionOpts, inFieldBoxes, inFieldRefs, inTransMainPanel, itgbc, "inTrans");
								}
							});
						}
						inSpinnerVal = (Integer)inSpinner.getValue();
					} else if ((Integer)inSpinner.getValue() < inSpinnerVal){
						for (int i = inSpinnerVal; i > (Integer)inSpinner.getValue(); i--){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									remRow(inFieldBoxes, inFieldRefs, inTransMainPanel, itgbc);
								}
							});
						}
						inSpinnerVal = (Integer)inSpinner.getValue();
					}
				} else if (arg0.getSource().equals(outSpinner)){
					if ((Integer)outSpinner.getValue() > outSpinnerVal){
						for (int i = outSpinnerVal; i < (Integer)outSpinner.getValue(); i++){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									addRow(transactionOpts, outFieldBoxes, outFieldRefs, outTransMainPanel, otgbc, "outTrans");
								}
							});
						}
						outSpinnerVal = (Integer)outSpinner.getValue();
					} else if ((Integer)outSpinner.getValue() < outSpinnerVal){
						for (int i = outSpinnerVal; i > (Integer)outSpinner.getValue(); i--){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									remRow(outFieldBoxes, outFieldRefs, outTransMainPanel, otgbc);
								}
							});
						}
						outSpinnerVal = (Integer)outSpinner.getValue();
					}
				} else if (arg0.getSource().equals(transSpinner)){
					if ((Integer)transSpinner.getValue() > transSpinnerVal){
						for (int i = transSpinnerVal; i < (Integer)transSpinner.getValue(); i++){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									addRow(translationOpts, transFieldBoxes, transFieldRefs, translateMainPanel, trgbc, "translate");
								}
							});
						}
						transSpinnerVal = (Integer)transSpinner.getValue();
					} else if ((Integer)transSpinner.getValue() < transSpinnerVal){
						for (int i = transSpinnerVal; i > (Integer)transSpinner.getValue(); i--){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									remRow(transFieldBoxes, transFieldRefs, translateMainPanel, trgbc);
								}
							});
						}
						transSpinnerVal = (Integer)transSpinner.getValue();
					}
				} else if (arg0.getSource().equals(updSpinner)){
					if ((Integer)updSpinner.getValue() > updSpinnerVal){
						for (int i = updSpinnerVal; i < (Integer)updSpinner.getValue(); i++){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									addRow(archPolOpts, updFieldBoxes, updFieldRefs, updMainPanel, updgbc, "archPol");
								}
							});
						}
						updSpinnerVal = (Integer)updSpinner.getValue();
					} else if ((Integer)updSpinner.getValue() < updSpinnerVal){
						for (int i = updSpinnerVal; i > (Integer)updSpinner.getValue(); i--){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									remRow(updFieldBoxes, updFieldRefs, updMainPanel, updgbc);
								}
							});
						}
						updSpinnerVal = (Integer)updSpinner.getValue();
					}
				} else if (arg0.getSource().equals(notifSpinner)){
					if ((Integer)notifSpinner.getValue() > notifSpinnerVal){
						for (int i = notifSpinnerVal; i < (Integer)notifSpinner.getValue(); i++){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									addRow(notifOpts, notifFieldBoxes, notifFieldRefs, notifMainPanel, ntgbc, "notification");
								}
							});
						}
						notifSpinnerVal = (Integer)notifSpinner.getValue();
					} else if ((Integer)notifSpinner.getValue() < notifSpinnerVal){
						for (int i = notifSpinnerVal; i > (Integer)notifSpinner.getValue(); i--){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									remRow(notifFieldBoxes, notifFieldRefs, notifMainPanel, ntgbc);
								}
							});
						}
						notifSpinnerVal = (Integer)notifSpinner.getValue();
					}
				} else if (arg0.getSource().equals(almSpinner)){
					if ((Integer)almSpinner.getValue() > almSpinnerVal){
						for (int i = almSpinnerVal; i < (Integer)almSpinner.getValue(); i++){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									addRow(alarmOpts, almFieldBoxes, almFieldRefs, almMainPanel, almgbc, "alarm");
								}
							});
						}
						almSpinnerVal = (Integer)almSpinner.getValue();
					} else if ((Integer)almSpinner.getValue() < almSpinnerVal){
						for (int i = almSpinnerVal; i > (Integer)almSpinner.getValue(); i--){
							SwingUtilities.invokeLater(new Runnable(){
								@Override
								public void run(){
									remRow(almFieldBoxes, almFieldRefs, almMainPanel, almgbc);
								}
							});
						}
						almSpinnerVal = (Integer)almSpinner.getValue();
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
	public class DataSender extends Thread{

		String purpose;
		MPEditorComponent reference;

		/**
		 * Constructs a new DataSender thread with the given purpose
		 * @param p The command String
		 */
		public DataSender(String p){
			super();
			purpose = p;
		}

		/**
		 * Constructs a new DataSender thread for a given component
		 * @param p The command String
		 * @param ref The reference to send to the server
		 */
		public DataSender(String p, MPEditorComponent ref){
			super();
			purpose = p;
			reference = ref;
		}

		@Override
		public void run(){
			if (purpose.equals("commitAll")){
				try{
					MoniCAClient mc = MonClientUtil.getServer();
					boolean allfine = true;
					if (mc != null){
						//write all points 
						Vector<PointDescription> pointsToWrite = new Vector<PointDescription>();
						for (MPEditorComponent m : components){
							PointDescription pd = PointDescription.factory(m.getNames(), m.getLongDesc(), m.getShortDesc(), m.getUnits(), m.getSource(), m.getInTransactions(), m.getOutTransactions(), m.getTranslations(), m.getAlarmCriteria(), m.getArchivePolicies(), m.getNotifications(), m.getPeriod(), m.getArchiveLongevity(), m.getGuidance(), m.getPriority(), m.getEnabled());
							pointsToWrite.add(pd);
						}
						allfine = mc.addPoints(pointsToWrite, username, password);
					}
					if (!allfine){
						password = "";
						JOptionPane.showMessageDialog(MonitorPointEditor.this, "Writing points failed. Please ensure your login details are correct and try writing again.", "Authentication Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				} catch (Exception e){
					password = "";
					JOptionPane.showMessageDialog(MonitorPointEditor.this, "Writing points failed. Please ensure your login details are correct and try writing again.", "Authentication Error", JOptionPane.ERROR_MESSAGE);
				}
			} else if (purpose.equals("commit")){
				try{
					MoniCAClient mc = MonClientUtil.getServer();
					boolean succ = false;
					if (mc != null){
						PointDescription pd = PointDescription.factory(reference.getNames(), reference.getLongDesc(), reference.getShortDesc(), reference.getUnits(), reference.getSource(), reference.getInTransactions(), reference.getOutTransactions(), reference.getTranslations(), reference.getAlarmCriteria(), reference.getArchivePolicies(), reference.getNotifications(), reference.getPeriod(), reference.getArchiveLongevity(), reference.getGuidance(), reference.getPriority(), reference.getEnabled());
						succ = mc.addPoint(pd, username, password);
					}
					if (!succ){
						password = "";
						JOptionPane.showMessageDialog(MonitorPointEditor.this, "Writing point failed. Please ensure your login details are correct and try writing again.", "Authentication Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				} catch (Exception e){
					password = "";
					JOptionPane.showMessageDialog(MonitorPointEditor.this, "Writing point failed. Please ensure your login details are correct and try writing again.", "Authentication Error", JOptionPane.ERROR_MESSAGE);
				}
			} else if (purpose.equals("update")){
				try {
					MoniCAClient mc = MonClientUtil.getServer();
					Vector<String> needData = new Vector<String>();
					for (MPEditorComponent m : components){
						if (!m.isAddPoint()){
							needData.add(m.getOrigPointName());
						}
					}
					Vector<PointDescription> pointDescs = mc.getPoints(needData);
					for (PointDescription pd : pointDescs){
						if (pd == null) continue;
						for (MPEditorComponent m : components){
							if (!m.isAddPoint() && m.getOrigPointName().equals(pd.getFullName())){
								try {
									if (m.isEditPoint()) m.setNameText(pd.getName());
									m.setLongDescText(pd.getLongDesc());
									m.setShortDescText(pd.getShortDesc());
									m.setUnitsText(pd.getUnits());
									m.setSourceText(pd.getSource());
									m.setEnabledState(Boolean.toString(pd.getEnabled()));
									m.setInputTransactionString(pd.getInputTransactionString());
									m.setOutputTransactionString(pd.getOutputTransactionString());
									m.setTranslationString(pd.getTranslationString());
									m.setArchiveLongevity(String.valueOf(pd.getArchiveLongevity()));
									m.setArchivePolicyString(pd.getArchivePolicyString());
									m.setUpdateInterval(String.valueOf(pd.getPeriod()));	
									m.setNotificationString(pd.getNotificationString());
									m.setAlarmCriteria(pd.getAlarmCheckString());
									m.setAlarmGuidance(pd.getGuidance());
									try {
										m.setAlarmPriority(priorities[pd.getPriority()]);
									} catch (IndexOutOfBoundsException e){
										m.setAlarmPriority(priorities[0]);
									}
								} catch (NullPointerException npe){
									System.err.println("NullPointerException when populating"
											+ "fields for point " + m.getSource() + "." + m.getNames()[0]);
								}
							}
						}
					}
				} catch (NullPointerException n){
					// Add-type point
				} catch (Exception e){
					System.err.println("Unable to prepopulate editor fields");
				}
				updated = true;
			}	
		}
	}
	// ///// END NESTED CLASS ///////

	// NESTED CLASS: MPEditorComponent ///////
	/**
	 * Inner class that is used exclusively for storing references to a collection of JTextFields,
	 * JComboBoxes and other components used for inputting data to create a new Monitor Point
	 * definition
	 */
	public class MPEditorComponent{

		/** String used for a Regular Expression to ensure that fields used with compound syntax
		 * are syntactically valid<br/>
		 * The Java regex string is:<br/>
		 * <tt>"\\{{0,1}(([a-zA-Z0-9]+\\-(\\\"[\\S]+\\\")*),{0,1})+\\}{0,1}||\\-||"</tt>
		 */
		final String compoundRegexStr = "\\{{0,1}(([a-zA-Z0-9]+\\-(\\\"[\\S]+\\\")*),{0,1})+\\}{0,1}||\\-||";

		private String pointName = "";
		private JTextField newPointField = null;
		private JLabel editPointLabel = null;
		private JTextField longDesc = null;
		private JTextField shortDesc = null; //less than 10 chars
		private JTextField units = null;
		private JTextField source = null;
		private JComboBox enabledState  = null; // True or False
		private JTextField inputTransacts = null;
		private JTextField outputTransacts = null;
		private JTextField translations = null;
		private JTextField alarmCriteria = null;
		private JTextField archivePolicy = null;
		private JTextField updateInterval = null;
		private JTextField archiveLongevity = null;
		private JTextField notifications = null;
		private JComboBox priority = null;
		private JTextField guidance = null;
		private JButton wizardBtn = null;
		private JButton writerBtn = null;

		private StringTokenizer st;
		private boolean addPoint = false;
		private boolean clonePoint = false;
		private boolean editPoint = false;

		/**
		 * Constructor for an "add" point to be added to the system
		 * @param npf JTextField with the name of the point
		 * @param ld JTextField with the long description of the point
		 * @param sd JTextField with the short description of the point
		 * @param u JTextField with the units of the point
		 * @param s JTextField with the source of the point
		 * @param es JComboBox with the enabled state of the point
		 * @param it JTextField with the input transactions of the point
		 * @param ot JTextField with the output transactions of the point
		 * @param t JTextField with the translations of the point
		 * @param ac JTextField with the archive longevity of the point
		 * @param ap JTextField with the archive policies of the point
		 * @param ui JTextField with the update interval of the point
		 * @param al JTextField with the alarm criteria of the point
		 * @param n JTextField with the notifications of the point
		 * @param p JComboBox with the alarm priority of the point
		 * @param g JTextField with the alarm guidance text of the point
		 * @param wiz JButton to start the Wizard
		 * @param writer JButton to send this point description to the server
		 */
		public MPEditorComponent(JTextField npf, JTextField ld, JTextField sd, JTextField u, 
				JTextField s, JComboBox es, JTextField it, JTextField ot, JTextField t,
				JTextField ac, JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz, JButton writer){
			pointName = null;
			newPointField = npf;
			longDesc = ld;
			shortDesc = sd;
			units = u;
			source = s;
			enabledState = es;
			inputTransacts = it;
			outputTransacts = ot;
			translations = t;
			alarmCriteria = ac;
			archivePolicy = ap;
			updateInterval = ui;
			archiveLongevity = al;
			notifications = n;
			priority = p;
			guidance = g;	
			wizardBtn = wiz;
			writerBtn = writer;
			addPoint = true;
		}

		/**
		 * Constructor for an "edit" point to be added to the system
		 * @param epl JLabel with the name of the point
		 * @param ld JTextField with the long description of the point
		 * @param sd JTextField with the short description of the point
		 * @param u JTextField with the units of the point
		 * @param s JTextField with the source of the point
		 * @param es JComboBox with the enabled state of the point
		 * @param it JTextField with the input transactions of the point
		 * @param ot JTextField with the output transactions of the point
		 * @param t JTextField with the translations of the point
		 * @param ac JTextField with the archive longevity of the point
		 * @param ap JTextField with the archive policies of the point
		 * @param ui JTextField with the update interval of the point
		 * @param al JTextField with the alarm criteria of the point
		 * @param n JTextField with the notifications of the point
		 * @param p JComboBox with the alarm priority of the point
		 * @param g JTextField with the alarm guidance text of the point
		 * @param wiz JButton to start the Wizard
		 * @param writer JButton to send this point description to the server
		 */
		public MPEditorComponent(String pt, JLabel epl, JTextField ld, JTextField sd, JTextField u, 
				JTextField s, JComboBox es, JTextField it, JTextField ot, JTextField t,
				JTextField ac, JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz, JButton writer){
			pointName = pt;
			editPointLabel = epl;
			longDesc = ld;
			shortDesc = sd;
			units = u;
			source = s;
			enabledState = es;
			inputTransacts = it;
			outputTransacts = ot;
			translations = t;
			alarmCriteria = ac;
			archivePolicy = ap;
			updateInterval = ui;
			archiveLongevity = al;
			notifications = n;
			priority = p;
			guidance = g;
			wizardBtn = wiz;
			writerBtn = writer;
			editPoint = true;
		}

		/**
		 * Constructor for a "clone" point to be added to the system
		 * @param point String of the full name of the point
		 * @param npf JTextField with the name of the point
		 * @param ld JTextField with the long description of the point
		 * @param sd JTextField with the short description of the point
		 * @param u JTextField with the units of the point
		 * @param s JTextField with the source of the point
		 * @param es JComboBox with the enabled state of the point
		 * @param it JTextField with the input transactions of the point
		 * @param ot JTextField with the output transactions of the point
		 * @param t JTextField with the translations of the point
		 * @param ac JTextField with the archive longevity of the point
		 * @param ap JTextField with the archive policies of the point
		 * @param ui JTextField with the update interval of the point
		 * @param al JTextField with the alarm criteria of the point
		 * @param n JTextField with the notifications of the point
		 * @param p JComboBox with the alarm priority of the point
		 * @param g JTextField with the alarm guidance text of the point
		 * @param wiz JButton to start the Wizard
		 * @param writer JButton to send this point description to the server
		 */
		public MPEditorComponent(String point, JTextField npf, JTextField ld,
				JTextField sd, JTextField u, JTextField s, JComboBox es,
				JTextField it, JTextField ot, JTextField t, JTextField ac,
				JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz, JButton writer) {
			pointName = point;
			newPointField = npf;
			longDesc = ld;
			shortDesc = sd;
			units = u;
			source = s;
			enabledState = es;
			inputTransacts = it;
			outputTransacts = ot;
			translations = t;
			alarmCriteria = ac;
			archivePolicy = ap;
			updateInterval = ui;
			archiveLongevity = al;
			notifications = n;
			priority = p;
			guidance = g;	
			wizardBtn = wiz;
			writerBtn = writer;
			clonePoint = true;
		}

		/**
		 * Indicates if this is an "add" type point, i.e. it is a new point to be added to
		 * the system
		 * @return boolean indicating if this is an "add" type point
		 */
		public boolean isAddPoint(){
			return addPoint;
		}

		/**
		 * Indicates if this is an "edit" type point, i.e. it is an existing point that needs
		 * to be altered within the system
		 * @return boolean indicating if this is an "edit" type point
		 */
		public boolean isEditPoint(){
			return editPoint;
		}

		/**
		 * Indicates if this is a "clone" type point, i.e. it is a new point to be added to
		 * the system, but should be populated with data based on an existing one
		 * @return boolean indicating if this is a "clone" type point
		 */
		public boolean isClonePoint(){
			return clonePoint;
		}

		/**
		 * Sets the contents of the name JTextField to s if it is an add point or clone point, 
		 * otherwise alters the text of the JLabel in an edit point
		 * @param s The new name to be set
		 */
		public void setNameText(String s){
			if (isAddPoint() || isClonePoint()){
				newPointField.setText(s);
			} else {
				editPointLabel.setText(s);
			}
		}

		/**
		 * Sets the contents of the long description JTextField to s
		 * @param s The String to change the Long Description to
		 */
		public void setLongDescText(String s){
			longDesc.setText(s);
		}
		/**
		 * Sets the contents of the short description JTextField to s
		 * @param s The String to change the Short Description to
		 */
		public void setShortDescText(String s){
			shortDesc.setText(s);
		}
		/**
		 * Sets the contents of the units JTextField to s
		 * @param s The String to change the units to
		 */
		public void setUnitsText(String s){
			units.setText(s);
		}
		/**
		 * Sets the contents of the source JTextField to s
		 * @param s The String to change the source to
		 */
		public void setSourceText(String s){
			source.setText(s);
		}
		/**
		 * Sets the contents of the enabled state JComboBox to state
		 * @param s The boolean to change the enabled state to
		 */
		public void setEnabledState(String state){
			enabledState.setSelectedItem(state);
		}
		/**
		 * Sets the contents of the input transactions JTextField to s
		 * @param s The String to change the input transactions to
		 */
		public void setInputTransactionString(String s) {
			inputTransacts.setText(s);
		}
		/**
		 * Sets the contents of the output transactions JTextField to s
		 * @param s The String to change the output transactions to
		 */
		public void setOutputTransactionString(String s) {
			outputTransacts.setText(s);
		}
		/**
		 * Sets the contents of the translations JTextField to s
		 * @param s The String to change the translations to
		 */
		public void setTranslationString(String s) {
			translations.setText(s);
		}
		/**
		 * Sets the contents of the archive policies JTextField to s
		 * @param s The String to change the archive policies to
		 */
		public void setArchivePolicyString(String s) {
			archivePolicy.setText(s);
		}
		/**
		 * Sets the contents of the update interval JTextField to s
		 * @param s The String to change the update interval to
		 */
		public void setUpdateInterval(String s) {
			updateInterval.setText(s);
		}
		/**
		 * Sets the contents of the archive longevity JTextField to s
		 * @param s The String to change the archive longevity to
		 */
		public void setArchiveLongevity(String s) {
			archiveLongevity.setText(s);
		}
		/**
		 * Sets the contents of the alarm criteria JTextField to s
		 * @param s The String to change the alarm criteria to
		 */
		public void setAlarmCriteria(String s) {
			alarmCriteria.setText(s);
		}
		/**
		 * Sets the contents of the alarm guidance JTextField to s
		 * @param s The String to change the alarm guidance to
		 */
		public void setAlarmGuidance(String s) {
			guidance.setText(s);
		}
		/**
		 * Sets the contents of the alarm priority JComboBox to selectedItem
		 * @param s The Object to change the JComboBox selection to
		 */
		public void setAlarmPriority(Object selectedItem) {
			priority.setSelectedItem(selectedItem);			
		}
		/**
		 * Sets the contents of the notifications JTextField to s
		 * @param s The String to change the notifications to
		 */
		public void setNotificationString(String s) {
			notifications.setText(s);
		}
		/**
		 * Returns a String[] with all the names assigned to this point, properly parsing
		 * correctly inputted compound syntax into each array entry.
		 * @return A String[] with all the point's names
		 */
		public String[] getNames(){
			String names = "";
			if (!isEditPoint()){
				names =  newPointField.getText();
			} else {
				names =  editPointLabel.getText();
			}
			if (names.equals("-")) return new String[0];
			names = names.replace("{", "");
			names = names.replace("}", "");
			names = names.trim();
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];
			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns the first name of this point, with the source prepended to it. Utilises the
		 * first entry in the array returned from the {@link #getNames()} method.
		 * @return The point's full point name.
		 * @see #getNames()
		 */
		public String getFullName(){
			String name = getNames()[0];
			name = getSource() + "." + name;
			return name;
		}
		/**
		 * Returns the text in the long description JTextField
		 * @return String that is the long description
		 */
		public String getLongDesc(){
			return longDesc.getText();
		}
		/**
		 * Returns the text in the short description JTextField
		 * @return String that is the short description
		 */
		public String getShortDesc(){
			return shortDesc.getText();
		}
		/**
		 * Returns the text in the units JTextField
		 * @return String that is the units
		 */
		public String getUnits(){
			return units.getText();
		}
		/**
		 * Returns the text in the source JTextField
		 * @return String that is the source
		 */
		public String getSource(){
			return source.getText();
		}
		/**
		 * Returns a boolean indicating the enabled state of this point
		 * @return Returns true if it is enabled, otherwise false.
		 */
		public boolean getEnabled(){
			Object value = enabledState.getSelectedItem();
			if (value.equals(bools[0])){
				return true;
			} else {
				return false;
			}
		}
		/**
		 * Returns the object in the enabled state JComboBox
		 * @return Object that is the selected item in the enabled state JComboBox
		 */
		public Object getEnabledState(){
			return enabledState.getSelectedItem();
		}
		/**
		 * Returns a String[] with each type of input transaction plus its arguments in a separate 
		 * entry. Will parse both compound and singular inputs properly into the array.
		 * @return A String[] with the input transactions as separate entries
		 */
		public String[] getInTransactions(){
			String names = inputTransacts.getText();
			if (names.equals("-")) return new String[0];
			Pattern pat = Pattern.compile(compoundRegexStr);
			Matcher mat = pat.matcher(names);
			if (!mat.matches()){
				System.err.println("Pattern didn't match");
				throw (new InvalidParameterException());
			}
			names = names.replace("{", "");
			names = names.replace("}", "");
			names = names.trim();
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns a String[] with each type of output transaction plus its arguments in a separate 
		 * entry. Will parse both compound and singular inputs properly into the array.
		 * @return A String[] with the output transactions as separate entries
		 */
		public String[] getOutTransactions(){
			String names = outputTransacts.getText();
			if (names.equals("-")) return new String[0];
			Pattern pat = Pattern.compile(compoundRegexStr);
			Matcher mat = pat.matcher(names);
			if (!mat.matches()) throw (new InvalidParameterException());
			names = names.replace("{", "");
			names = names.replace("}", "");
			names = names.trim();
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns a String[] with each type of translation plus its arguments in a separate 
		 * entry. Will parse both compound and singular inputs properly into the array.
		 * @return A String[] with the translations as separate entries
		 */
		public String[] getTranslations(){
			String names = translations.getText();
			if (names.equals("-")) return new String[0];
			Pattern pat = Pattern.compile(compoundRegexStr);
			Matcher mat = pat.matcher(names);
			if (!mat.matches()) throw (new InvalidParameterException());
			names = names.replace("{", "");
			names = names.replace("}", "");
			names = names.trim();
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns a String[] with each type of alarm criterion plus its arguments in a separate 
		 * entry. Will parse both compound and singular inputs properly into the array.
		 * @return A String[] with the alarm criteria as separate entries
		 */
		public String[] getAlarmCriteria(){
			String names = alarmCriteria.getText();	
			if (names.equals("-")) return new String[0];
			Pattern pat = Pattern.compile(compoundRegexStr);
			Matcher mat = pat.matcher(names);
			if (!mat.matches()) throw (new InvalidParameterException());
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns a String[] with each type of archive policy plus its arguments in a separate 
		 * entry. Will parse both compound and singular inputs properly into the array.
		 * @return A String[] with the archive policies as separate entries
		 */
		public String[] getArchivePolicies(){
			String names = archivePolicy.getText();
			if (names.equals("-")) return new String[0];
			Pattern pat = Pattern.compile(compoundRegexStr);
			Matcher mat = pat.matcher(names);
			if (!mat.matches()) throw (new InvalidParameterException());
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns a String[] with each type of notification plus its arguments in a separate 
		 * entry. Will parse both compound and singular inputs properly into the array.
		 * @return A String[] with the notifications as separate entries
		 */
		public String[] getNotifications(){
			String names = notifications.getText();	
			if (names.equals("-")) return new String[0];
			Pattern pat = Pattern.compile(compoundRegexStr);
			Matcher mat = pat.matcher(names);
			if (!mat.matches()) throw (new InvalidParameterException());
			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}
		/**
		 * Returns the text in the update interval JTextField
		 * @return String that is the update interval
		 */
		public String getPeriod(){
			return updateInterval.getText();
		}
		/**
		 * Returns the text in the archive longevity JTextField
		 * @return String that is the archive longevity
		 */
		public String getArchiveLongevity(){
			return archiveLongevity.getText();
		}
		/**
		 * Returns a formatted String of the selected item in the priority JComboBox
		 * @return String that is the String form of the selected item in the priority JComboBox
		 */
		public String getPriority(){
			return priority.getSelectedItem().toString();
		}
		/**
		 * Returns the text in the alarm guidance JTextField
		 * @return String that is the alarm guidance
		 */
		public String getGuidance(){
			return guidance.getText();
		}
		/**
		 * Returns the original pointname used for this point, if present
		 * @return The original point name for this point
		 */
		public String getOrigPointName(){
			return pointName;
		}
		/**
		 * Returns a reference to the JButton used to start the Wizard
		 * @return A reference to the "Wizard" JButton
		 */
		public JButton getWizBtn(){
			return wizardBtn;
		}
		/**
		 * Returns a reference to the JButton used to write this point to the server
		 * @return A reference to the "Write" JButton
		 */
		public JButton getWriterBtn(){
			return writerBtn;
		}

		/**
		 * Validates all fields in this panel, and changes the text to placeholders in the
		 * event that it is unfilled.
		 * @return A booean indicating whether this panel is valid
		 */
		public boolean validate(){
			boolean res = true;
			if (newPointField != null){
				res = (!newPointField.getText().equals(""));
			}
			res = (!source.getText().equals("")); 
			if (inputTransacts.getText().equals(""))inputTransacts.setText("-");
			if (outputTransacts.getText().equals(""))outputTransacts.setText("-");
			if (translations.getText().equals(""))inputTransacts.setText("-");
			if (alarmCriteria.getText().equals(""))translations.setText("-");
			if (archivePolicy.getText().equals(""))archivePolicy.setText("-");
			if (updateInterval.getText().equals(""))updateInterval.setText("-");
			if (archiveLongevity.getText().equals(""))archiveLongevity.setText("-");
			if (notifications.getText().equals(""))notifications.setText("-");
			if (guidance.getText().equals(""))guidance.setText("-");

			return res;
		}
	}
	// END NESTED CLASS ///////

	// NESTED CLASS: LimitFieldDocumentFilter ///////
	/**
	 * DocumentFilter that only allows a restricted number of characters being added to a Document, 
	 * and will automatically crop pasted text to the maximum number of characters if applicable
	 */
	public class LimitFieldDocumentFilter extends DocumentFilter{
		private int charLimit;

		/**
		 * Constructor
		 * @param limit The maximum number of allowed characters
		 */
		public LimitFieldDocumentFilter(int limit){
			super();
			charLimit = limit;
		}

		/**
		 * Sets the maximum number of characters allowed by this DocumentFilter
		 * @param lim The maximum number of characters allowed in this Document
		 */
		public void setMaxChars(int lim){
			charLimit = lim;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
			AbstractDocument doc = (AbstractDocument) fb.getDocument();
			String oldText = doc.getText(0, doc.getLength());
			if (oldText.length() + text.length() <= charLimit){
				super.insertString(fb, offset, text, attr);
			} else {
				super.insertString(fb, offset, text.substring(0, charLimit - oldText.length()), attr);
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			AbstractDocument doc = (AbstractDocument) fb.getDocument();
			String oldText = doc.getText(0, doc.getLength());
			if (oldText.length() + text.length() - length <= charLimit){
				super.replace(fb, offset, length, text, attrs);
			} else {
				super.replace(fb, offset, length, text.substring(0, charLimit + length - oldText.length()), attrs);
			}
		}
	}
	// END NESTED CLASS ///////

	// NESTED CLASS: LimitFieldDocumentFilter ///////
	/**
	 * DocumentFilter that only allows the insertion of numbers, floats or a single "-" into the Document
	 */
	public class NumDocumentFilter extends DocumentFilter{

		/**
		 * Constructor
		 */
		public NumDocumentFilter(){
			super();
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
			Pattern pattern = Pattern.compile("\\-{0,1}[0-9]*\\.{0,1}[0-9]*");
			Matcher matcher = pattern.matcher(text);
			if (matcher.matches()){
				super.insertString(fb, offset, text, attr);
			} 
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			Pattern pattern = Pattern.compile("\\-{0,1}[0-9]*\\.{0,1}[0-9]*");
			Matcher matcher = pattern.matcher(text);
			if (matcher.matches()){
				super.replace(fb, offset, length, text, attrs);
			}
		}
	}
	// END NESTED CLASS ///////

	@Override
	public void onAlarmEvent(AlarmEvent event) {}

	@Override
	public void onAlarmEvent(Collection<AlarmEvent> events) {
		AlarmMaintainer.removeListener(this);
		new Thread(){
			@Override
			public void run(){
				while (!loaded || !updated){
					Thread.yield();
				}//loop until loadSetup finishes, and run just once
				for (MPEditorComponent m : components){
					try {
						Alarm a = AlarmMaintainer.getAlarm(m.getOrigPointName());
						m.setAlarmGuidance(a.getGuidance());
						try {
							m.setAlarmPriority(priorities[a.getPriority()]);
						} catch (IndexOutOfBoundsException iobe){
							m.setAlarmPriority(priorities[0]);
						}
					} catch (NullPointerException npe){
						continue;
					}
				}
				// Needs to reset these in case we use loadSetup() again to change appearance
				loaded = false;
				updated = false;
			}
		}.start();
	}
}
