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

import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.SavedSetup;
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
public class MonitorPointEditor extends MonPanel implements ActionListener{

	private static final long serialVersionUID = 1030968805100512703L;
	/** Array holding the layout values "Horizontal" and "Vertical" for use with the layout combo-box */
	private final String[] layouts = {"Horizontal", "Vertical"};
	private String username = "";
	private String password = "";

	static {
		MonPanel.registerMonPanel("Monitor Point Editor", MonitorPointEditor.class);
	}

	// //// NESTED CLASS: ControlSetupPanel ///////
	protected class MonitorPointEditorSetupPanel extends MonPanelSetupPanel implements ActionListener, ChangeListener{

		private static final long serialVersionUID = -7475845517269885679L;

		private JPanel itsMainPanel = new JPanel(new GridBagLayout());
		private JPanel topPanel = new JPanel(new GridBagLayout());
		private JScrollPane itsMainScroller = new JScrollPane();
		private JButton addPoint = new JButton("Add new Point");
		private JButton editPoint = new JButton("Edit a Point");
		private JLabel addDesc = new JLabel("Added points:");
		private JLabel addTotal = new JLabel("0");
		private JLabel editDesc = new JLabel("Edited points:");
		private JLabel editTotal = new JLabel("0");
		private JComboBox layout = new JComboBox(layouts);

		int numNew = 0;
		int numEdit = 0;
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
			JLabel layoutLabel = new JLabel("Layout: ");
			gbct.gridx = 2;
			layoutLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			gbct.anchor = GridBagConstraints.EAST;
			topPanel.add(layoutLabel, gbct);
			gbct.gridx = 3;
			gbct.anchor = GridBagConstraints.WEST;
			topPanel.add(layout, gbct);
			gbct.anchor = GridBagConstraints.CENTER;
			gbct.fill = GridBagConstraints.HORIZONTAL;
			gbct.gridx = 1;
			gbct.gridwidth = 2;
			gbct.gridy ++;
			topPanel.add(addPoint, gbct);
			gbct.gridx = 3;
			topPanel.add(editPoint, gbct);
			gbct.gridx = 1;
			gbct.gridy ++;
			gbct.gridwidth = 1;
			addDesc.setHorizontalAlignment(SwingConstants.RIGHT);
			topPanel.add(addDesc, gbct);
			gbct.gridx += 1;
			addTotal.setHorizontalAlignment(SwingConstants.LEFT);
			topPanel.add(addTotal, gbct);
			gbct.gridx += 1;
			editDesc.setHorizontalAlignment(SwingConstants.RIGHT);
			topPanel.add(editDesc, gbct);
			gbct.gridx += 1;
			editTotal.setHorizontalAlignment(SwingConstants.LEFT);
			topPanel.add(editTotal, gbct);

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
				} else {
					comp += "edit," + m.getTreeSelection() + ";";
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
					this.addEditPointPanel();
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
					this.addNewPointPanel();
				} else if (source.equals(editPoint)){

					this.addEditPointPanel();
				}
				if (source.getActionCommand().equals("OK")){

					for (MPEditorSetupComponent c : components){
						if (c.getTreeSelection() == null && c.getType() == MPEditorSetupComponent.EDIT_POINT){
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
							} else {
								this.decrementEdit();
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
		 * Small method to subtract 1 to the total of new point panels, and parse it to 
		 * a String for display in a JLabel.
		 */
		public void incrementEdit(){
			numEdit += 1;
			editTotal.setText(Integer.toString(numEdit));
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
		 * Method to add a new "Edit" type setup panel to the main setup panel.<br/>
		 * It adds all the required elements, registers listeners and updates the running
		 * totals.
		 */
		private void addEditPointPanel() {
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
			components.add(new MPEditorSetupComponent(newPanel, tree, selectedPoint, close));

			this.incrementEdit();
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
					if(c.getType() == MPEditorSetupComponent.EDIT_POINT && source.equals(c.getTree().getTreeUtil())){
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
		public MPEditorSetupComponent(JPanel j, SimpleTreeSelector t, JLabel p, JButton c){
			panel = j;
			type = EDIT_POINT;
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
			if (this.getType() == MPEditorSetupComponent.EDIT_POINT) tree.removeChangeListener(m);
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

	/**
	 * Constructor, initialises some of the Document Filters and fonts for JLabels, along
	 * with setting up the layout for some of the panels
	 */
	public MonitorPointEditor() {
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

	public void addEditorPanel(String point){
		JButton wiz = new JButton("Wizard");
		wiz.addActionListener(this);
		JButton writer = new JButton("Write");
		writer.addActionListener(this);
		JLabel l = new JLabel(point);
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
		components.add(new MPEditorComponent(l, ld, sd, u, s, es, it, ot, t, ac, ap, ui, al, n, p, g, wiz, writer));
	}

	@Override
	public void vaporise() {
		// TODO Auto-generated method stub

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

	private void showWizard(MPEditorComponent pointDetails){
		new WizardFrame(pointDetails);
	}

	public boolean noDupes(){
		boolean ret = true;
		HashSet<String[]> set = new HashSet<String[]>(); // Hashsets only allow adding of unique members
		for (MPEditorComponent c : components){
			ret = set.add(c.getNames()); // returns false if it can't add a member
			if (!ret) return ret;
		}
		return ret;
	}
	// ///// NESTED CLASS: WizardFrame //////

	public class WizardFrame extends JFrame implements ActionListener, ChangeListener{

		private static final long serialVersionUID = -7680339339902552985L;

		private MPEditorComponent reference;
		private JPanel itsCardPanel;
		private CardLayout cl;

		final String[] itsCards = {"metadata", "input transactions", "output transactions", "translations", "update data", "notifications", "alarm data"};
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
				"Synch",
				"ThyconAlarm",
				"TimedSubstitution",
				"VapourPressure",
				"Variance",
				"XY2Angle",
				"XY2Mag"
		};
		String[] archPolOpts = {
				"Alarm",
				"All",
				"Change",
				"Counter",
				"OnDecrease",
				"OnIncrease",
				"Timer"
		};
		String[] notifOpts = {
				"EmailOnAlarm",
				"EmailOnAlarmChange"
		};
		String[] alarmOpts = {
				"Boolean",
				"Range",
				"StringMatch",
				"ValueMatch"
		};
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

		private void setupMetaDataPanel(JPanel mdc){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new GridLayout(6,2));
			JScrollPane scroller = new JScrollPane();

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

			desc.add(title);
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

			if (reference.isNewPoint()){
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

			desc.add(title);
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

			desc.add(title);
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

			desc.add(title);
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

		private void populatePanel(ArrayList<JComboBox> boxes, HashMap<JComboBox, JTextField[]> args, String[] inputs){
			for (int i = 0; i < inputs.length; i++){
				String type = inputs[i].substring(0, inputs[i].indexOf('-'));
				boxes.get(i).setSelectedItem(type);
				inputs[i] = inputs[i].substring(inputs[i].indexOf('-')+1); //strip off type
				inputs[i] = inputs[i].trim();
			}
			int i = 0;
			for (String s : inputs){//TODO needs checking
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

		private void populateFields(){//TODO Update as required
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
		 * Constructs a new DataSender thread
		 */
		public DataSender(String p){
			super();
			purpose = p;
		}

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
						if (!m.isNewPoint()){
							needData.add(m.getNames()[0]);
						}
					}
					Vector<PointDescription> pointDescs = mc.getPoints(needData);
					for (PointDescription pd : pointDescs){
						if (pd == null) continue;
						for (MPEditorComponent m : components){//TODO fill in the rest as I go along
							if (!m.newPoint && m.getNames()[0].equals(pd.getFullName())){
								try {
									m.setNameText(pd.getName());
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
			}	
		}
	}
	// ///// END NESTED CLASS ///////

	// NESTED CLASS: MPEditorComponent ///////
	public class MPEditorComponent{

		final String compoundRegexStr = "\\{{0,1}(([a-zA-Z0-9]+\\-(\\\"[\\S]+\\\")*),{0,1})+\\}{0,1}||\\-||";

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
		private boolean newPoint;

		public MPEditorComponent(JTextField npf, JTextField ld, JTextField sd, JTextField u, 
				JTextField s, JComboBox es, JTextField it, JTextField ot, JTextField t,
				JTextField ac, JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz, JButton writer){
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
			newPoint = true;
		}

		public MPEditorComponent(JLabel epl, JTextField ld, JTextField sd, JTextField u, 
				JTextField s, JComboBox es, JTextField it, JTextField ot, JTextField t,
				JTextField ac, JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz, JButton writer){
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
			newPoint = false;
		}

		public boolean isNewPoint(){
			return newPoint;
		}

		public void setNameText(String s){
			if (newPoint){
				newPointField.setText(s);
			} else {
				editPointLabel.setText(s);
			}
		}

		public void setLongDescText(String s){
			longDesc.setText(s);
		}

		public void setShortDescText(String s){
			shortDesc.setText(s);
		}

		public void setUnitsText(String s){
			units.setText(s);
		}

		public void setSourceText(String s){
			source.setText(s);
		}

		public void setEnabledState(String state){
			enabledState.setSelectedItem(state);
		}

		public void setInputTransactionString(String string) {
			inputTransacts.setText(string);
		}

		public void setOutputTransactionString(String string) {
			outputTransacts.setText(string);
		}


		public void setTranslationString(String string) {
			translations.setText(string);
		}

		public void setArchivePolicyString(String string) {
			archivePolicy.setText(string);
		}

		public void setUpdateInterval(String text) {
			updateInterval.setText(text);
		}

		public void setArchiveLongevity(String text) {
			archiveLongevity.setText(text);
		}

		public void setAlarmCriteria(String string) {
			alarmCriteria.setText(string);
		}

		public void setAlarmGuidance(String text) {
			guidance.setText(text);
		}

		public void setAlarmPriority(Object selectedItem) {
			priority.setSelectedItem(selectedItem);			
		}

		public void setNotificationString(String string) {
			notifications.setText(string);
		}

		public String[] getNames(){
			String names = "";
			if (newPoint){
				names =  newPointField.getText();
			} else {
				names =  editPointLabel.getText();
			}

			st = new StringTokenizer(names, ",");
			int numToks = st.countTokens();
			String[] res = new String[numToks];

			for (int i = 0; i < numToks; i++){
				res[i] = st.nextToken();
			}
			return res;
		}

		public String getFullName(){
			String name = getNames()[0];
			name = getSource() + "." + name;
			return name;
		}

		public String getLongDesc(){
			return longDesc.getText();
		}

		public String getShortDesc(){
			return shortDesc.getText();
		}

		public String getUnits(){
			return units.getText();
		}

		public String getSource(){
			return source.getText();
		}

		public boolean getEnabled(){
			Object value = enabledState.getSelectedItem();
			if (value.equals(bools[0])){
				return true;
			} else {
				return false;
			}
		}

		public Object getEnabledState(){
			return enabledState.getSelectedItem();
		}

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

		public String getPeriod(){
			return updateInterval.getText();
		}

		public String getArchiveLongevity(){
			return archiveLongevity.getText();
		}

		public String getPriority(){
			return priority.getSelectedItem().toString();
		}

		public String getGuidance(){
			return guidance.getText();
		}

		public JButton getWizBtn(){
			return wizardBtn;
		}

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
	 * DocumentFilter that only allows a restricted number of characters being added to a Document
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
}
