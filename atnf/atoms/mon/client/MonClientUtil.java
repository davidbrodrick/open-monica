// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.client;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.tree.*;
import atnf.atoms.mon.*;
import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.util.*;

/**
 * MonClientUtil is a client-side class which contains various methods to
 * perform useful client-side operations. This class doesn't really
 * encapsulate a well defined object, but rather it ties together various
 * pieces of functionality so that most of the useful methods are available
 * in one spot.
 *
 * <P>One of the things it does it control what server the client will
 * connect to. This is normally done by presenting the user with a dialog
 * box. If the program is in headless mode (ie, <tt>-Djava.awt.headless=true</tt>)
 * then it will automatically select the site that the client's IP address
 * corresponds to. This behaviour can be modified (in headed or headless mode)
 * by specifying the server name as a property called "server", eg <tt>
 * -Dserver=monhost-nar.atnf.csiro.au</tt>.
 *
 * @author David Brodrick
 * @version $Id: MonClientUtil.java,v 1.9 2008/03/18 00:52:10 bro764 Exp bro764 $
 */
public class MonClientUtil
{
  /** Hashmap contains a Hashtable of SavedSetups for each class, indexed by
   * class name . */
  private static Hashtable theirSetups;

  /** Network connection to the monitor server. */
  private static MonitorClientCustom theirServer;

  /** Records if we have downloaded the list of available SavedSetups
   * from the network server. Normally this will be done at class load time
   * but if the network server is unavailable at that time then we have
   * no option but to delay the operation until the server is available. */
  private static boolean theirHasGotServerSetups;


  /** Initialise stuff. */
  static {
    theirHasGotServerSetups = false;
    String host = null;
    Vector chosenserver = null;

    String headless = System.getProperty("java.awt.headless", "false");
    String targethost = System.getProperty("server", null);

    if (targethost!=null) {
      host = targethost;
    } else {
      if (headless.equals("true")) {
	//We can only run in headless mode if a server was specified
	System.err.println("MonClientUtil: ERROR: Headless mode requested but no server specified!");
	System.exit(1);
      }

      Vector serverlist=new Vector();
      Vector defaultserver=null;
      try {
        //Get the list of server definitions
        InputStream res = MonClientUtil.class.getClassLoader().getResourceAsStream("monitor-servers.txt");
        if (res==null) throw new Exception();
        BufferedReader serversfile = new BufferedReader(new InputStreamReader(res));
        int linecounter=0;
        int def=-1;
        while (serversfile.ready()) {
          String thisline=serversfile.readLine();
          linecounter++;

          if (thisline.startsWith("#") || thisline.trim().equals(""))
          continue;

          if (thisline.startsWith("default")) {
            //This line specifies the default server to use
            String[] tokens=thisline.split("\t");
            if (tokens.length<2) {
              System.err.println("MonClientUtil: WARNING: monitor-servers.txt parse error line " + linecounter);
              continue;
            }
            try {
              def=Integer.parseInt(tokens[1])-1;
            } catch (Exception e) {
              System.err.println("MonClientUtil: WARNING: monitor-servers.txt parse error line " + linecounter);
              continue;
            }
            continue;
          }

          //This line is a server definition
          String[] tokens=thisline.split("\t");
          if (tokens==null || tokens.length<2) {
            System.err.println("MonClientUtil: WARNING: monitor-servers.txt parse error line " + linecounter);
            continue;
          }
          Vector thisserver=new Vector();
          thisserver.add(tokens[0].trim());
          thisserver.add(tokens[1].trim());
          if (tokens.length>2) {
            thisserver.add(tokens[2].trim());
          } else {
            thisserver.add(null);
          }
          //Add the new server to the list
          serverlist.add(thisserver);
        }

        //If the file specified a default server then use it.
        if (def>=0) {
          if (def>serverlist.size()-1) {
            System.err.println("MonClientUtil: WARNING: Default server " + (def+1) + " requested but only " + serverlist.size() + " servers defined: IGNORING DEFAULT");
          } else {
            defaultserver=(Vector)serverlist.get(def);
          }
        }
      } catch (Exception e) {
        System.err.println("MonClientUtil: ERROR: Couldn't find list of monitor servers!");
        System.exit(1);
      }

      //We can launch a GUI tool to ask the user what site to connect to
      SiteChooser chooser = new SiteChooser(serverlist, defaultserver);
      chosenserver = chooser.getSite();
      host=(String)chosenserver.get(1);
      System.err.println("User selected server " + host);
    }

    try {
      System.out.print("MonClientUtil: Connecting to host \"" + host + "\"");
      theirServer = new MonitorClientCustom(host);
      System.out.println("\tOK");
      theirSetups = new Hashtable();
      addServerSetups(); //Get all SavedSetups from server
      addLocalSetups();  //Also load any which have been saved locally
    } catch (Exception e) {
      if (headless.equals("true")) {
        System.err.println("MonClientUtil: ERROR: Couldn't connect to server. Goodbye.");
        System.exit(1);
      } else {
        boolean connected=false;
        if (chosenserver.get(2)!=null) {
          try {
            SecureTunnel st = new SecureTunnel((String)chosenserver.get(1),
                                  (String)chosenserver.get(2),
                                  8050, 8050);

	    System.out.print("MonClientUtil: Connecting to \"localhost\"");
	    theirServer = new MonitorClientCustom("localhost");
	    System.out.println("\tOK");
	    theirSetups = new Hashtable();
	    addServerSetups(); //Get all SavedSetups from server
	    addLocalSetups();  //Also load any which have been saved locally

            connected=true;
	  } catch (Exception f) {

	  }
	}

	if (!connected) {
	  JOptionPane.showMessageDialog(MonFrame.theirWindowManager.getWindow(0),
					"ERROR CONTACTING SERVER\n\n" +
					"Server: " + host + "\n" +
					"Error: \"" + e.getMessage() + "\"\n\n" +
					"The application will now exit.",
					"Error Contacting Server",
					JOptionPane.WARNING_MESSAGE);
	  System.exit(1);
	}
      }
    }
  }


  /** Get a reference to the network connection to the server. */
  public static synchronized
  MonitorClientCustom
  getServer()
  {
    if (!theirServer.isConnected())
      try { theirServer.connect(); } catch (Exception e) { }

    if (theirHasGotServerSetups==false) {
      //Still need to load setups from server
      addServerSetups();
    }

    return theirServer;
  }

  /** Get a reference to the network connection to the server. */
  public static synchronized
  void
  setServer(String newserver)
  throws Exception
  {
    theirServer = new MonitorClientCustom(newserver);

    if (!theirServer.isConnected())
      try { theirServer.connect(); } catch (Exception e) { }

    theirHasGotServerSetups=false;
    theirSetups = new Hashtable();
    //Need to load setups from server
    addServerSetups();
    //reload any which have been saved locally
    addLocalSetups();  
  }


  /** Download all the SavedSetups which are available on the server
   * and add them to our collection. */
  public static
  void
  addServerSetups()
  {
    if (!theirServer.isConnected())
      try { theirServer.connect(); } catch (Exception e) { }

    if (theirServer.isConnected()) {
      SavedSetup[] setups = theirServer.getAllSetups();
      theirHasGotServerSetups = true;
      if (setups!=null && setups.length>0) {
	System.err.println("MonClientUtil:addServerSetups: Loaded "
			   + setups.length + " setups from server");
	//Convert to Vector form
	Vector v = new Vector(setups.length);
	for (int i=0; i<setups.length; i++) v.add(setups[i]);
	mergeSetups(v);
      } else {
	System.err.println("MonClientUtil:addServerSetups: None available");
      }
    }
  }


  /** Download all the SavedSetups which available locally and add them
   * to our collection. */
  public static
  void
  addLocalSetups()
  {
    //Figure out which platform we are on.
    String osname = System.getProperty("os.name").toLowerCase();
    String monfile = null;
    if (osname.indexOf("win")!=-1) {
      //Must be some flavour of winblows
      monfile = "\\Application Data\\MoniCA\\local-setups.txt";
    } else {
      //But what about other platforms?
      monfile = "/.MoniCA/local-setups.txt";
    }

    monfile = System.getProperty("user.home") + monfile;

    Vector setups = null;
    try {
      setups = SavedSetup.parseFile(monfile);
    } catch (Exception e) {
      System.err.println(e.getClass() + " while parsing " + monfile);
    }

    if (setups!=null && setups.size()>0) {
      //We found some local setups, print message and add them
      System.err.print("MonClientUtil:addLocalSetups: Loaded "
			 + setups.size() + " setups from:");
      System.err.println(monfile);
      mergeSetups(setups);
    } else {
      System.err.println("MonClientUtil:addLocalSetups: No setups found in:");
      System.err.println(monfile);
    }
  }


  /** Merge the Vector of SavedSetups with our collection. */
  public static
  void
  mergeSetups(Vector setups)
  {
    if (setups==null || setups.size()==0) return;

    for (int i=0; i<setups.size(); i++) {
      SavedSetup thissetup = (SavedSetup)setups.get(i);
      if (thissetup==null) {
	System.err.println("MonClientUtil:mergeSetups: Warning NULL setup");
	continue;
      }
      if (thissetup.getName()==null) {
	System.err.println("MonClientUtil:mergeSetups: Warning NULL name");
	continue;
      }

      if (theirSetups.get(thissetup.getClassName())==null) {
	//First setup to be added for that class
	theirSetups.put(thissetup.getClassName(), new Hashtable());
      }

      //Add this setup to the vector for the appropriate class
      Hashtable classsetups = (Hashtable)theirSetups.get(thissetup.getClassName());
      classsetups.put(thissetup.getName(), thissetup);
    }
  }


  /** Merge the given SavedSetup with our collection. */
  public static
  void
  mergeSetup(SavedSetup setup)
  {
    if (setup==null) {
      System.err.println("MonClientUtil:mergeSetup: Warning NULL setup");
      return;
    }
    if (setup.getName()==null) {
      System.err.println("MonClientUtil:mergeSetup: Warning NULL name");
      return;
    }

    if (theirSetups.get(setup.getClassName())==null) {
      //First setup to be added for that class
      theirSetups.put(setup.getClassName(), new Hashtable());
    }

    //Add this setup to the vector for the appropriate class
    Hashtable classsetups = (Hashtable)theirSetups.get(setup.getClassName());
    classsetups.put(setup.getName(), setup);
  }


  /** Return a TreeUtil of the SavedSetups available for the specified class.
   * @return TreeUtil for available SavedSetups. */
  protected static
  TreeUtil
  getSetupTreeUtil(Class c)
  {
    return getSetupTreeUtil(c.getName());
  }


  /** Return a TreeUtil of the SavedSetups available for the specified class.
   * @return TreeUtil for available SavedSetups. */
  protected static
  TreeUtil
  getSetupTreeUtil(String c)
  {
    TreeUtil res = new NavigatorTree("Setups");

    //Get the container of all setups for the required class
    Hashtable setups = (Hashtable)theirSetups.get(c);
    if (setups==null) return null;

    Set keyset = setups.keySet();
    Object[] setupnames = keyset.toArray();
    if (setupnames==null || setupnames.length==0) return null;

    for (int i=0; i<setupnames.length; i++) {
      String name = (String)setupnames[i];
      res.addNode(name);
    }

    return res;
  }

  /** Get the names of all setups for the specified class.
   * @param c Name of the class to obtain the setup names for.
   * @return Array of setup names, never <tt>null</tt>. */
  public static
  String[]
  getSetupNames(String c)
  {
    //Get the container of all setups for the required class
    Hashtable setups = (Hashtable)theirSetups.get(c);
    if (setups==null) return new String[0];

    Object[] allnames = setups.keySet().toArray();
    String[] res = new String[setups.size()];
    for (int i=0; i<allnames.length; i++) {
      res[i] = (String)allnames[i];
    }
    return res;
  }


  /** Get the named setup for the specified class.
   * @param c Name of the class to obtain the setup for.
   * @param name Name of the setup to fetch.
   * @return The requested setup, or <tt>null</tt> if it doesn't exist. */
  public static
  SavedSetup
  getSetup(String c, String name)
  {
    //Get the container of all setups for the required class
    Hashtable setups = (Hashtable)theirSetups.get(c);
    if (setups==null) return null;
    return (SavedSetup)setups.get(name);
  }


  /** Get the named setup for the specified class.
   * @param c Class to obtain the setup for.
   * @param name Name of the setup to fetch.
   * @return The requested setup, or <tt>null</tt> if it doesn't exist. */
  public static
  SavedSetup
  getSetup(Class c, String name)
  {
    return getSetup(c.getName(), name);
  }


  public static
  JMenuItem
  getSetupMenu(Class c)
  {
    return getSetupMenu(c.getName());
  }


  public static
  JMenuItem
  getSetupMenu(String c)
  {
    TreeUtil res = getSetupTreeUtil(c);
    if (res==null) return null;
    return res.getMenus();
  }

  public static
  void
  getSetupMenu(Class c, JMenu menu)
  {
    getSetupMenu(c.getName(), menu);
  }


  public static
  void
  getSetupMenu(String c, JMenu menu)
  {
    TreeUtil res = getSetupTreeUtil(c);
    if (res==null) return;
    res.getMenus(menu);
  }


  public static
  JMenuItem
  getSetupMenu(Class c, ActionListener listener)
  {
    return getSetupMenu(c.getName(), listener);
  }


  public static
  JMenuItem
  getSetupMenu(String c, ActionListener listener)
  {
    TreeUtil res = getSetupTreeUtil(c);
    if (res==null) return null;
    res.addActionListener(listener);
    return res.getMenus();
  }


  public static
  void
  getSetupMenu(Class c, ActionListener listener, JMenu parent)
  {
    getSetupMenu(c.getName(), listener, parent);
  }


  public static
  void
  getSetupMenu(String c, ActionListener listener, JMenu parent)
  {
    TreeUtil res = getSetupTreeUtil(c);
    if (res==null) return;
    res.addActionListener(listener);
    res.getMenus(parent);
  }




  /** Sort the names of the available sources. This is to ensure that the
   * user always gets the same set of sources in the same order.
   * @param sources Container of source names to be sorted.
   * @return Vector containing source names in sorted order. */
/*  public static
  Vector
  sortSources(Vector sources)
  {
    Vector res = new Vector();
    //Simple sort since we expect size to always be "small"
    for (int i=0; i<sources.size(); i++) {
      String thissource = (String)sources.get(i);
      int j = 0;
      for (; j<res.size(); j++) {
	String comp = (String)res.get(j);
        if (comp.compareToIgnoreCase(thissource)>0) break;
      }
      res.insertElementAt(thissource, j);
    }
    return res;
  }
*/

  /** Return the sources of data for the specified point name. This uses
   * locally cached information if it is available and asks the network
   * server otherwise.
   * @param name Name of the point to get the sources for.
   * @return Vector containing the names of the sources. If no sources are
   *         available for the point a non-null Vector with size zero will
   *         be returned. */
/*  public static
  Vector
  getPointSources(String name)
  {
    return null;
  }
*/

  /** Return all data sources for the points named in the PointPage. The
   * result is a Vector containing the String names of all unique sources.
   * If there are no sources for the given page then a Vector will still
   * be returned but it's size will be zero.
   * @param pname Name of the PointPage to list the sources for.
   * @return Vector containing the source names. */
/*  public static
  Vector
  getAvailableSources(String pname)
  {
    //Do a lookup to get the page with the given name
    PointPage page = getPage(pname);
    return getAvailableSources(page);
  }
*/

  /** Return all data sources for the points named in the PointPage. The
   * result is a Vector containing the String names of all unique sources.
   * If there are no sources for the given page then a Vector will still
   * be returned but it's size will be zero.
   * @param pname The PointPage to list the sources for.
   * @return Vector containing the source names. */
/*  public static
  Vector
  getAvailableSources(PointPage page)
  {
    Vector res = new Vector();
    if (page==null || page.size()==0) return res; //Pathological cases

    for (int p=0; p<page.size(); p++) {
      if (page.get(p)==null) continue; //Empty name = empty row
      String pname = (String)page.get(p);
      if (pname.equals("")) continue;    //Empty name = empty row

      //Get the available sources for this point
      String[] s = getServer().getSources(pname);
      if (s==null || s.length==0) continue; //No sources found

      //Add any new sources to our result
      for (int i=0; i<s.length; i++) {
        if (!res.contains(s[i])) res.add(s[i]);
      }
    }

    return sortSources(res);
  }
*/

  /** Attach a checklist of available sources to the specified Menu. This
   * works out all of the sources that are available for the given PointPage
   * and then builds a menu containing a check box for each source. If the
   * <i>selected</i> argument is not <code>null</code> it will be used to
   * specify which sources should initially be in a selected state. If the
   * <i>selected</i> argument is <code>null</code> then all sources will
   * initially be selected. The nominated ItemListener will receive events
   * whenever the user changes the source selection. 
   * @param page The PointPage to get available sources for.
   * @param selected Collection of Strings naming currently selected sources.
   * @param menu The JMenu to attach the new menu to.
   * @param listener Listener to receive events when selection changes. */
/*  public static
  void
  setSourcesMenu(PointPage page, Collection selected,
		 JMenu menu, ItemListener listener)
  {
    //Remove the old contents of the menu
    menu.removeAll();

    //Get the available sources
    Vector avail = getAvailableSources(page);

    for (int i=0; i<avail.size(); i++) {
      JMenuItem thissource = null;
      if (selected==null || selected.contains(avail.get(i))) {
	//This source is currently selected
        thissource = new JCheckBoxMenuItem((String)avail.get(i), true);
      } else {
	//This source is currently deselected
	thissource = new JCheckBoxMenuItem((String)avail.get(i), false);
      }
      thissource.addItemListener(listener);
      menu.add(thissource);
    }
  }
*/


  /**
   * Prompt the user to choose a site, and return the hostname.
   */
  public static class SiteChooser extends JFrame implements ActionListener {
    /** The server host name that will be returned. */
    Vector itsServer = null;
    /** The default server to return. */
    Vector itsDefault = null;
    /** Number of seconds for timeout. */
    int itsTimeout = 7;
    /** Label for our counter. */
    JLabel itsCounter = null;
    /** The available server. */
    Vector itsServers=null;

    public SiteChooser(Vector servers, Vector def) {
      itsServers=servers;
      itsDefault=def;

      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      getContentPane().setLayout(new BoxLayout(getContentPane(),
					       BoxLayout.Y_AXIS));
      JPanel temppanel = new JPanel();
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
      temppanel.add(new JLabel("Monitor which site?"));

      for (int i=0; i<itsServers.size(); i++) {
        Vector thisserver=(Vector)itsServers.get(i);
	JButton tempbutton = new JButton((String)thisserver.get(0));
	tempbutton.addActionListener(this);
	tempbutton.setActionCommand(""+i);
	tempbutton.setMinimumSize(new Dimension(240, 28));
	tempbutton.setPreferredSize(new Dimension(240, 28));
	tempbutton.setMaximumSize(new Dimension(240, 28));
	temppanel.add(tempbutton);
      }

      if (itsDefault!=null) {
	itsCounter = new JLabel("Default \"" + itsDefault.get(0) + "\" in " +
				itsTimeout, JLabel.CENTER);
	itsCounter.setForeground(Color.red);
	itsCounter.setMinimumSize(new Dimension(240, 28));
	itsCounter.setPreferredSize(new Dimension(240, 28));
	itsCounter.setMaximumSize(new Dimension(240, 28));
	temppanel.add(itsCounter);
      }

      getContentPane().add(temppanel);
      //getContentPane().add(temppanel2);

      //Do this on the AWT threads time to avoid deadlocks
      final SiteChooser realthis = this;
      final Runnable choosenow = new Runnable() {
	public void run() {
	  realthis.pack();
	  if (itsDefault!=null) {
	    realthis.setSize(new Dimension(240, 74+28*itsServers.size()));
	  } else {
            realthis.setSize(new Dimension(240, 46+28*itsServers.size()));
	  }
	  realthis.validateTree();
	  realthis.setVisible(true);
	  if (itsDefault!=null) {
	    MonitorTimer timer = new MonitorTimer(1000, realthis, true);
	    timer.start();
	  }
	}
      };
      try {
	SwingUtilities.invokeAndWait(choosenow);
      } catch (Exception e) {e.printStackTrace();}
    }

    public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().equals("timer")) {
	//timer update
	itsTimeout--;
	if (itsTimeout==0) {
	  itsServer = itsDefault;
	  setVisible(false);
	  synchronized (this) {
	    notifyAll();
	  }
	  ((MonitorTimer)e.getSource()).stop();
	} else {
	  itsCounter.setText("Default \"" + itsDefault.get(0) + "\" in " +
			     itsTimeout);
	}
      } else {
	//user server selection
	itsServer = (Vector)itsServers.get(Integer.parseInt(e.getActionCommand()));
	setVisible(false);
	synchronized (this) {
	  notifyAll();
	}
      }
    }

    public Vector getSite() {
      try {
	synchronized (this) {
	  wait();
	}
      } catch (Exception e) {}
      return itsServer;
    }
  }

  /** Sub-class of TreeUtil for building the Navigator window, which knows
   * to put the "favourites" menu at the top, and to put a separator item
   * beneath it when building a Menu tree. */
  public static class NavigatorTree
  extends TreeUtil
  {
   public NavigatorTree(String name, Object root)
   {
     super(name,root);
   }

   public NavigatorTree(String name)
   {
     super(name);
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
	      if (myTok.toLowerCase().equals("favourites")) {
		tempNode.insert(aNode, 0);
	      } else {
		int targeti;
		for (targeti=0; targeti<tempNode.getChildCount(); targeti++) {
		  TreeNode bNode = tempNode.getChildAt(targeti);
		  if (bNode.isLeaf() || (bNode.toString().compareToIgnoreCase(myTok)>0 && !bNode.toString().equals("favourites"))) break;
		}
		tempNode.insert(aNode, targeti);
	      }
	    } else {
	      //This node is a leaf node
	      int targeti;
	      for (targeti=0; targeti<tempNode.getChildCount(); targeti++) {
		TreeNode bNode = tempNode.getChildAt(targeti);
		if (bNode.isLeaf() && bNode.toString().compareToIgnoreCase(myTok)>0) break;
	      }
	      tempNode.insert(aNode, targeti);
	    }
	    tempNode = aNode;
	 }
      }
   }

   /** Make a Menu structure, without the root node. The children of the
    * root node will be added to the specified menu element. */
   public
   void
   getMenus(JMenu menu)
   {
     int numChild = itsRootNode.getChildCount();
     for (int i=0; i<numChild; i++) {
       DefaultMutableTreeNode thisnode = (DefaultMutableTreeNode)itsRootNode.getChildAt(i);
       JMenuItem thisitem = getMenus(thisnode, menu);
       if (thisnode.toString().equals("favourites")) {
         thisitem.setForeground(Color.blue);
       }
       menu.add(thisitem);
       //menu.addSeparator();
     }
   }
  }

  static final
  void
  main(String[] argv)
  {
    
  }
}
