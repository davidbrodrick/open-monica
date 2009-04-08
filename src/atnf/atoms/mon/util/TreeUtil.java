/**
 * Class: TreeUtil
 * Description: A simple utility class for dealing with Trees
 * @author Le Cuong Nguyen
**/
package atnf.atoms.mon.util;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.util.*;
import java.awt.event.*;
//import org.apache.regexp.*;

public class TreeUtil implements ActionListener, TreeSelectionListener
{
   protected DefaultMutableTreeNode itsRootNode = null;
   protected JMenuItem itsRootMenu = null;
   
   // Should really do this with an object encompassing the nodes and data
   // with only on map. But this is easier...
   protected Hashtable itsMap = new Hashtable();
   protected Hashtable itsTreeMap = new Hashtable();

   public static final int TREE = 7654;
   public static final int MENU = 7655;
      
   // Want to do stuff with ActionListeners
   private EventListenerList itsListeners = new EventListenerList();

   public TreeUtil(String name, Object root)
   {
      itsMap.put(name, root);
      itsRootNode = new DefaultMutableTreeNode(name);
      itsTreeMap.put(name, itsRootNode);
   }

   public TreeUtil(String name)
   {
      itsMap.put(name, new Object());
      itsRootNode = new DefaultMutableTreeNode(name);
      itsTreeMap.put(name, itsRootNode);
   }

   public void addNode(String name)
   {
      addNode(name, new Object());
   }
   
   public void addNode(String name, Object obj)
   {
      itsMap.put(name, obj);
      DefaultMutableTreeNode tempNode = itsRootNode;
      JMenuItem tempMenu = itsRootMenu;
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

   public void addChildNode(DefaultMutableTreeNode parent, String name, Object obj)
   {
      String realName = parent.getUserObject().toString() + "." + name;
   }
 
   public void addChildNode(DefaultMutableTreeNode parent, String name)
   {
      String realName = parent.getUserObject().toString() + "." + name;
      addNode(realName);   
   }
   
   public Object getNodeObject(String name)
   {
      return itsMap.get(name);
   }

   public void setNodeObject(String node, Object data)
   {
      itsMap.put(node, data);
   }

   public String[] getAllNodes()
   {
      return MonitorUtils.toStringArray(itsMap.keySet().toArray());
   }
      
   public DefaultMutableTreeNode getNode(String name)
   {
      return (DefaultMutableTreeNode)itsTreeMap.get(name);
   }
         
   public DefaultMutableTreeNode getRootNode()
   {
      return itsRootNode;
   }


   /** Make a Menu structure, without the root node. The children of the
    * root node will be added to the specified menu element. */
   public
   void
   getMenus(JMenu menu)
   {
     int numChild = itsRootNode.getChildCount();
     for (int i=0; i<numChild; i++) {
      menu.add(getMenus((DefaultMutableTreeNode)itsRootNode.getChildAt(i), menu));
    }
   }

   /**
    * Makes menus from the root node
    */
   public JMenuItem getMenus()
   {
      JMenu rootMenu = new JMenu(itsRootNode.getUserObject().toString());
      rootMenu.setActionCommand("TreeMenu");
      rootMenu.addActionListener(this);
      return getMenus(itsRootNode, rootMenu);
   }

   /** Creates the menus by using recursion */
   public JMenuItem getMenus(DefaultMutableTreeNode node, JMenu parentMenu)
   {
      String name = node.getUserObject().toString();
      int numChild = node.getChildCount();
      if (numChild < 1) {
	 JMenuItem tempMenu = new JMenuItem(name);
	 tempMenu.setActionCommand(parentMenu.getActionCommand()+"."+name);
	 tempMenu.addActionListener(this);
	 return tempMenu;
      }
      JMenu tempMenu = new JMenu(name);
      tempMenu.setActionCommand(parentMenu.getActionCommand()+"."+name);
      tempMenu.addActionListener(this);
      for (int i = 0; i < numChild; i++) {
        tempMenu.add(getMenus((DefaultMutableTreeNode)node.getChildAt(i), tempMenu));
      }
      return tempMenu;
   }

   public JTree getTree()
   {
      return getTree(itsRootNode);
   }
         
   public JTree getTree(DefaultMutableTreeNode node)
   {
      JTree tree = new JTree(node);
//      tree.addTreeSelectionListener(this);
      return tree;
   }
   
   public void addActionListener(ActionListener listener)
   {
      itsListeners.add(ActionListener.class, listener);
   }

   public void removeActionListener(ActionListener listener)
   {
      itsListeners.remove(ActionListener.class, listener);
   }

   public void fireActionEvent(ActionEvent ae)
   {
      Object[] listeners = itsListeners.getListenerList();
      for (int i = 0; i < listeners.length; i +=2) {
        if (listeners[i] == ActionListener.class) {
          ((ActionListener)listeners[i+1]).actionPerformed(ae);
        }
      }
   }
   
   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();
      int idx = cmd.indexOf('.');
      cmd = cmd.substring(++idx, cmd.length());
      fireActionEvent(new ActionEvent(this, MENU, cmd));
   }

   public void valueChanged(TreeSelectionEvent e)
   {
      TreePath path = e.getPath();
      Object[] items = path.getPath();
      if (items.length < 1) {
        return;
      }
      String cmd = "";
      for (int i = 0; i < items.length; i++) {
        cmd = cmd + "." + items[i].toString();
      }
      if (cmd.length() > 0) {
        cmd = cmd.substring(1);
      }
      fireActionEvent(new ActionEvent(this, TREE, cmd));
   }
   
/*   public ArrayList search(String pattern)
   {
      ArrayList res = new ArrayList();
      try {
	 RE re = new RE(pattern, RE.MATCH_CASEINDEPENDENT);
         Iterator it = itsTreeMap.keySet().iterator();
	 while (it.hasNext()) {
	    String key = (String)it.next();
	    if (re.match(key)) res.add(key);
	 }
      } catch (Exception e) {e.printStackTrace();}
      return res;
   }*/
   
   public TreePath makeTreePath(String path)
   {
      return makeTreePath(path, itsRootNode);
   }
   
   public static TreePath makeTreePath(String path, DefaultMutableTreeNode parentNode)
   {
      DefaultMutableTreeNode tempNode = parentNode;
      TreePath res = new TreePath(parentNode);
      StringTokenizer tok = new StringTokenizer(path, ".");
      String currentPath = null;
      while (tok.hasMoreTokens()) {
	 String myTok = tok.nextToken();
	 currentPath = (currentPath == null) ? myTok : currentPath + "." + myTok;
	 for (int j = 0; j < tempNode.getChildCount(); j++) {
	    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)tempNode.getChildAt(j);
	    if (childNode.toString().equals(myTok)) {
	       tempNode = childNode;
	       res = res.pathByAddingChild(tempNode);
	       break;
	    }
	 }
      }
      return res;
   }
   
   public static String pathToString(TreePath path)
   {
      if (path == null) {
        return null;
      }
      Object[] obj = path.getPath();
      String res = "";
      for (int i = 0; i < obj.length; i++) {
        res = res + "." + obj[i].toString();
      }
      if (res.length() > 0) {
        res = res.substring(1);
      }
      return res;
   }
}
