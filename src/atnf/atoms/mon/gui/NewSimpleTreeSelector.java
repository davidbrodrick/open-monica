package atnf.atoms.mon.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import atnf.atoms.mon.util.PointBinner;
import atnf.atoms.mon.util.PointBinner.PointHierarchy;

public class NewSimpleTreeSelector extends JPanel implements TreeExpansionListener, TreeSelectionListener{
	private static final long serialVersionUID = 4957372795206902915L;
	/** Graphical JTree widget*/
	private JTree itsTree;
	/** Base node that all other branches stem from */
	private DefaultMutableTreeNode rootNode;
	/** Local cached version of the PointHierarchy, shared between all instances*/
	private static PointHierarchy cachedTree;
	/** Locally stored copy of the string representation of the point selected*/
	private String selected = "";
	/** Mapping of points from fully qualified names to TreeNodes*/
	public static HashMap<String, DefaultMutableTreeNode> itsTreeMap;

	static {
		cachedTree = new PointHierarchy();
		itsTreeMap = new HashMap<String, DefaultMutableTreeNode>();
	}

	/** Vector of ChangeListeners that are listening to updates form this TreeSelector*/
	Vector<ChangeListener> itsListeners = new Vector<ChangeListener>();


	/**
	 * Creates a NewSimpleTreeSelector with "Points" as the border title by default
	 * @throws NullPointerException thrown if the TreeSelector can't get the list of points
	 */
	public NewSimpleTreeSelector() throws NullPointerException{
		this("Points:");
	}

	/**
	 * Creates a NewSimpleTreeSelector with borderTitle as the border title. Populates two levels
	 * of the tree since with only one, the "+" simples to expand the tree wouldn't appear.
	 * @param borderTitle The String that should be set as the border title
	 * @throws NullPointerException Thrown if this can't get the list of points
	 */
	public NewSimpleTreeSelector(String borderTitle) throws NullPointerException{
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		TitledBorder title = BorderFactory.createTitledBorder(borderTitle);
		setBorder(title);

		rootNode = new DefaultMutableTreeNode("Points");

		//Adds two levels of nodes initially, so the "+" buttons are properly in place
		// If there was a way around this, it would make things a lot simpler, but would probably
		// require extending JTree
		synchronized (cachedTree){
			if (!cachedTree.hasChildren()){
				Vector<String> topNodeNames;
				/*long currentTime = System.currentTimeMillis();
				while (!PointBinner.getPointsBinnedStatus()){
					if (currentTime + 5000 < System.currentTimeMillis()){
						throw new NullPointerException();
					}
				}*/
				topNodeNames = PointBinner.getDirectChildren("");
				//will throw NullPointerException if getDirectChildren() won't work
				for (String s : topNodeNames){
					cachedTree.addLeaf(s);
					DefaultMutableTreeNode thisNode = new DefaultMutableTreeNode(s);
					addNode(s, thisNode);
					Vector<String> subChildren = PointBinner.getDirectChildren(s);
					for (String str : subChildren){
						thisNode.add(new DefaultMutableTreeNode(str));
						cachedTree.addLeaf(s + "." + str);
					}
				}
			} else {
				Vector<String> topNodeNames = cachedTree.getDirectChildren("");
				for (String s : topNodeNames){
					DefaultMutableTreeNode thisNode = new DefaultMutableTreeNode(s);
					rootNode.add(thisNode);
					Vector<String> subChildren = PointBinner.getDirectChildren(s);
					for (String str : subChildren){
						thisNode.add(new DefaultMutableTreeNode(str));
					}
				}
			}
		}
		itsTree = new JTree(rootNode);
		itsTree.addTreeExpansionListener(this);
		itsTree.addTreeSelectionListener(this);
		itsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		JScrollPane sp = new JScrollPane(itsTree);
		sp.setPreferredSize(new Dimension(170, 200));
		sp.setMinimumSize(new Dimension(140, 100));
		add(sp);
		sp.setPreferredSize(new Dimension(200, 180));
		sp.setMinimumSize(new Dimension(80, 100));
		JPanel temppan = new JPanel();
		temppan.setLayout(new BorderLayout());
		temppan.add(sp, BorderLayout.CENTER);

		add(temppan);
	}


	@Override
	public void treeCollapsed(TreeExpansionEvent arg0) {}

	@Override
	public void treeExpanded(TreeExpansionEvent arg0) {
		TreePath path = arg0.getPath();
		Object[] pathObjects = path.getPath();
		String[] pathNames = Arrays.copyOf(pathObjects, pathObjects.length, String[].class);
		String dottedPath = "";
		for (int i = 0; i < pathNames.length; i ++){
			dottedPath += pathNames[i];
			if (i < pathNames.length - 1){
				dottedPath += ".";
			}
		}
		//Get Next two levels
		synchronized (cachedTree){
			Vector<String> nodeNames = cachedTree.getDirectChildren(dottedPath);
			if (nodeNames.isEmpty() || nodeNames == null) return;
			DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
			for (String s : nodeNames){
				DefaultMutableTreeNode parent = null;
				for (int j = 0; j < lastPathComponent.getChildCount(); j++){
					if (((DefaultMutableTreeNode)lastPathComponent.getChildAt(j)).getUserObject().equals(s)){
						parent = (DefaultMutableTreeNode) lastPathComponent.getChildAt(j);
						break;
					}
				}
				if (parent != null){
					Vector<String> children = cachedTree.getDirectChildren(dottedPath + "." + s);
					boolean ok = true;
					if (children == null || children.isEmpty()){
						children = PointBinner.getDirectChildren(dottedPath + "." + s);
						ok = false;
					}
					if (!ok){
						for (String str : children){
							parent.add(new DefaultMutableTreeNode(str));
							cachedTree.addLeaf(dottedPath + "." + str);
						}
					}
				}
			}
		}
	}

	private void addNode(String name, DefaultMutableTreeNode thisNode){
		cachedTree.addLeaf(name);
		DefaultMutableTreeNode tempNode = rootNode;
		StringTokenizer tok = new StringTokenizer(name, ".");
		String currentName = null;
		while (tok.hasMoreTokens()) {
			String myTok = tok.nextToken();
			currentName = (currentName == null) ? myTok : currentName + "." + myTok;
			boolean createNew = true;
			for (int j = 0; j < tempNode.getChildCount(); j++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)tempNode.getChildAt(j);
				if (childNode.toString().equals(myTok)) {
					tempNode = childNode;
					createNew = false;
					break;
				}
			}
			if (createNew) {
				DefaultMutableTreeNode aNode = new DefaultMutableTreeNode(myTok);
				itsTreeMap.put(currentName, aNode);
				//Let's give some consideration to where in the tree we place the new node.
				//We want any nodes with children to be listed first, in alphabetical order.
				//Then come nodes with no children, in alphabetical order.
				if (tok.hasMoreTokens()) {
					//This node is not a leaf node
					int targeti;
					for (targeti=0; targeti<tempNode.getChildCount(); targeti++) {
						TreeNode bNode = tempNode.getChildAt(targeti);
						if (bNode.isLeaf() || bNode.toString().compareToIgnoreCase(myTok)>0) {
							break;
						}
					}
					tempNode.insert(aNode, targeti);
				} else {
					//This node is a leaf node
					int targeti;
					for (targeti=0; targeti<tempNode.getChildCount(); targeti++) {
						TreeNode bNode = tempNode.getChildAt(targeti);
						if (bNode.isLeaf() && bNode.toString().compareToIgnoreCase(myTok)>0) {
							break;
						}
					}
					tempNode.insert(aNode, targeti);
				}
				tempNode = aNode;
			}
		}
	}

	private DefaultMutableTreeNode getNode(String name){
		return (DefaultMutableTreeNode)itsTreeMap.get(name);
	}

	public void addChangeListener(ChangeListener cl){
		itsListeners.add(cl);
	}

	public void removeChangeListener(ChangeListener cl){
		itsListeners.remove(cl);
	}

	public Vector<String> getSelections(){
		Vector<String> res = new Vector<String>();
		res.add(selected);
		return res;
	}

	private void fireChangeEvent(ChangeEvent ce){
		for (ChangeListener cl : itsListeners){
			cl.stateChanged(ce);
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		selected = "";
		TreePath selectedPath = arg0.getNewLeadSelectionPath();
		DefaultMutableTreeNode[] pathComponents = (DefaultMutableTreeNode[])selectedPath.getPath();
		for (int i = 1; i < pathComponents.length; i++){//skip the root node, which is ""
			selected += pathComponents[i].getUserObject();
			if (i != pathComponents.length-1) selected += ".";
		}
		System.out.println("Selected Point: " + selected);
		fireChangeEvent(new ChangeEvent(this));
	}

	public static void main(String[] args){
		JFrame frame = new JFrame();
		JPanel itsPanel = new JPanel();
		itsPanel.add(new NewSimpleTreeSelector());
		frame.add(itsPanel);
		frame.pack();
		frame.setVisible(true);
	}
}
