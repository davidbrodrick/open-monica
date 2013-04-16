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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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
					comp += "add, null;";
				} else {
					comp += "edit, " + m.getTreeSelection() + ";";
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
			JLabel alCrit = new JLabel("Alarm Criteria");
			JLabel archPol = new JLabel("Archive Policies");
			JLabel updInt = new JLabel("Update Interval");
			JLabel archLong = new JLabel("Archive Longevity");
			JLabel notifs = new JLabel("Notifications");
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
				itsMainPanel.add(alCrit, gbc);
				gbc.gridx ++;
				itsMainPanel.add(archPol, gbc);
				gbc.gridx ++;
				itsMainPanel.add(updInt, gbc);
				gbc.gridx ++;
				itsMainPanel.add(archLong, gbc);
				gbc.gridx ++;
				itsMainPanel.add(notifs, gbc);
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
				itsMainPanel.add(alCrit, gbc);
				gbc.gridy ++;
				itsMainPanel.add(archPol, gbc);
				gbc.gridy ++;
				itsMainPanel.add(updInt, gbc);
				gbc.gridy ++;
				itsMainPanel.add(archLong, gbc);
				gbc.gridy ++;
				itsMainPanel.add(notifs, gbc);
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
			itsMainPanel.add(ac, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridx ++;
			itsMainPanel.add(al, gbc);
			gbc.gridx ++;
			itsMainPanel.add(n, gbc);
			gbc.gridx ++;
			itsMainPanel.add(p, gbc);
			gbc.gridx ++;
			itsMainPanel.add(g, gbc);
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
			itsMainPanel.add(ac, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridy ++;
			itsMainPanel.add(al, gbc);
			gbc.gridy ++;
			itsMainPanel.add(n, gbc);
			gbc.gridy ++;
			itsMainPanel.add(p, gbc);
			gbc.gridy ++;
			itsMainPanel.add(g, gbc);
		}
		components.add(new MPEditorComponent(npf, ld, sd, u, s, es, it, ot, t, ac, ap, ui, al, n, p, g, wiz));
	}

	public void addEditorPanel(String point){
		JButton wiz = new JButton("Wizard");
		wiz.addActionListener(this);
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
			itsMainPanel.add(ac, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridx ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridx ++;
			itsMainPanel.add(al, gbc);
			gbc.gridx ++;
			itsMainPanel.add(n, gbc);
			gbc.gridx ++;
			itsMainPanel.add(p, gbc);
			gbc.gridx ++;
			itsMainPanel.add(g, gbc);
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
			itsMainPanel.add(ac, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ap, gbc);
			gbc.gridy ++;
			itsMainPanel.add(ui, gbc);
			gbc.gridy ++;
			itsMainPanel.add(al, gbc);
			gbc.gridy ++;
			itsMainPanel.add(n, gbc);
			gbc.gridy ++;
			itsMainPanel.add(p, gbc);
			gbc.gridy ++;
			itsMainPanel.add(g, gbc);
		}
		components.add(new MPEditorComponent(l, ld, sd, u, s, es, it, ot, t, ac, ap, ui, al, n, p, g, wiz));
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

					boolean valid = true;
					for (MPEditorComponent m : components){
						boolean v = m.validate();
						if (!v) valid = v;
					}
					if (valid && noDupes()){
						new DataSender().start();
					} else {
						JOptionPane.showMessageDialog(this, "Writing point failed. Please ensure that all data points are uniquely named and filled in correctly.", "Data Validation Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
			} else {
				//wizard button instance
				for (MPEditorComponent m : this.components){
					if (source.equals(m.getWizBtn())){
						this.showWizard(m);
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

	public class WizardFrame extends JFrame implements ActionListener{

		private static final long serialVersionUID = -7680339339902552985L;

		private MPEditorComponent reference;
		private JPanel itsCardPanel;
		private CardLayout cl;
		
		String[] itsCards = {"metadata", "input transactions", "output transactions", "translations", "update data", "alarm data"};
		int curr = 0;
		
		//Nav Panel
		JButton back = new JButton("<< Back <<");
		JButton next = new JButton(">> Next >>");
		JButton finish = new JButton("Finish");

		//Metadata card
		JTextField name;
		JTextField longDesc;
		JTextField shortDesc;
		JTextField units;
		JTextField source;
		JComboBox enabled;

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
			JPanel updateCard = new JPanel(); // update interval, achive policy, archive longevity
			JPanel alarmCard = new JPanel(); //notifications, alarm criteria, alarm priority, guidance message

			this.addNavPanel(navPanel, itsPanel);
			this.addMetaDataPanel(metaDataCard, itsCardPanel);
			
			itsCardPanel.add(metaDataCard, itsCards[0]);
			itsCardPanel.add(inTransCard, itsCards[1]);
			itsCardPanel.add(outTransCard, itsCards[2]);
			itsCardPanel.add(translateCard, itsCards[3]);
			itsCardPanel.add(updateCard, itsCards[4]);
			itsCardPanel.add(alarmCard, itsCards[5]);


			itsPanel.setLayout(new BorderLayout());
			itsPanel.add(itsCardPanel, BorderLayout.CENTER);
			itsPanel.add(navPanel, BorderLayout.SOUTH);
			this.setTitle("Point Setup Wizard");
			this.add(itsPanel);
			this.pack();
			this.setVisible(true);
		}

		private void addNavPanel(JPanel nav, JComponent container){
			back.setActionCommand("back");
			back.addActionListener(this);
			next.setActionCommand("next");
			next.addActionListener(this);
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
			nav.add(finish, gbc);
			gbc.gridx ++;

			container.add(nav);
		}

		private void addMetaDataPanel(JPanel mdc, JComponent container){
			mdc.setLayout(new BorderLayout());
			JPanel desc = new JPanel(new GridLayout(2,1));
			JPanel content = new JPanel(new GridLayout(6,2));
			
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
			JLabel untLb = new JLabel("Units: ");
			units = new JTextField(5);
			JLabel srcLb = new JLabel("Source: ");
			source = new JTextField(5);
			JLabel enbldLb = new JLabel("Enabled State: ");
			enabled = new JComboBox(MonitorPointEditor.this.bools);
			enabled.setEditable(false);

			if (reference.isNewPoint()){
				name.setEditable(true);
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

			mdc.add(desc, BorderLayout.NORTH);
			mdc.add(content, BorderLayout.CENTER);
			container.add(mdc, "metadata");
		}

		private void populateFields(){
			reference.setNameText(name.getText());
			reference.setLongDescText(longDesc.getText());
			reference.setShortDescText(shortDesc.getText());
			reference.setSourceText(source.getText());
			reference.setUnitsText(units.getText());
			reference.setEnabledState(enabled.getSelectedItem().toString());
		}

		public void actionPerformed(ActionEvent e){
			//do stuff
			if (e.getSource() instanceof JButton){
				String cmd = e.getActionCommand();
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
				} else if (cmd.equals("finish")){
					this.populateFields();
					this.dispose();
				}
			}
		}

	}

	// ///// END NESTED CLASS //////

	// ///// NESTED CLASS: DataSender ///////
	/**
	 * Worker class for sending data to the server, so the UI doesn't hang waiting for a server
	 * response.
	 */
	public class DataSender extends Thread implements Runnable{

		/**
		 * Constructs a new DataSender thread
		 */
		public DataSender(){
		}

		@Override
		public void run(){
			try{
				MoniCAClient mc = MonClientUtil.getServer();
				boolean allfine = true;
				if (mc != null){
					//write all points 
					for (MPEditorComponent m : components){
						PointDescription pd = PointDescription.factory(m.getNames(), m.getLongDesc(), m.getShortDesc(), m.getUnits(), m.getSource(), m.getInTransactions(), m.getOutTransactions(), m.getTranslations(), m.getAlarmCriteria(), m.getArchivePolicies(), m.getNotifications(), m.getPeriod(), m.getArchiveLongevity(), m.getGuidance(), m.getPriority(), m.getEnabled());
						String monPointTxt = pd.getStringEquiv();
						boolean result = false;
						//	result = mc.writePoint(monPointTxt, username, password); TODO
						if (!result){
							allfine = false;
						}
					}
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
		}
	}
	// ///// END NESTED CLASS ///////

	// NESTED CLASS: MPEditorComponent ///////
	public class MPEditorComponent{

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

		private StringTokenizer st;
		private boolean newPoint;

		public MPEditorComponent(JTextField npf, JTextField ld, JTextField sd, JTextField u, 
				JTextField s, JComboBox es, JTextField it, JTextField ot, JTextField t,
				JTextField ac, JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz){
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
			newPoint = true;
		}

		public MPEditorComponent(JLabel epl, JTextField ld, JTextField sd, JTextField u, 
				JTextField s, JComboBox es, JTextField it, JTextField ot, JTextField t,
				JTextField ac, JTextField ap, JTextField ui, JTextField al, JTextField n,
				JComboBox p, JTextField g, JButton wiz){
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
			newPoint = false;
		}

		public boolean isNewPoint(){
			return newPoint;
		}

		public void setNameText(String s){
			if (newPoint){
				newPointField.setText(s);
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

		public String[] getNames(){
			String names = "";
			if (newPointField != null){
				names =  newPointField.getText();
			} else if (editPointLabel != null){
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
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			AbstractDocument doc = (AbstractDocument) fb.getDocument();
			String oldText = doc.getText(0, doc.getLength());
			if (oldText.length() + text.length() <= charLimit){
				super.replace(fb, offset, length, text, attrs);
			}
		}
	}
	// END NESTED CLASS ///////

	// NESTED CLASS: LimitFieldDocumentFilter ///////
	/**
	 * DocumentFilter that only allows the insertion of integers into the Document
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
			Pattern pattern = Pattern.compile("[0-9]+");
			Matcher matcher = pattern.matcher(text);
			if (matcher.matches()){
				super.insertString(fb, offset, text, attr);
			} 
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			Pattern pattern = Pattern.compile("[0-9]+");
			Matcher matcher = pattern.matcher(text);
			if (matcher.matches()){
				super.replace(fb, offset, length, text, attrs);
			}
		}
	}
	// END NESTED CLASS ///////
}
