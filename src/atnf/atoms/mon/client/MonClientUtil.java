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
import javax.swing.Timer;
import atnf.atoms.mon.*;
import atnf.atoms.mon.comms.*;
import atnf.atoms.mon.gui.*;
import atnf.atoms.mon.util.*;

/**
 * MonClientUtil is a client-side class which contains various methods to
 * perform useful client-side operations. This class doesn't really encapsulate
 * a well defined object, but rather it ties together various pieces of
 * functionality so that most of the useful methods are available in one spot.
 * 
 * <P>
 * One of the things it does it control what server the client will connect to.
 * This is normally done by presenting the user with a dialog box, however the
 * server name can also be specified as a property called "server", eg
 * <tt>-Dserver=monhost-nar.atnf.csiro.au</tt>.
 * 
 * @author David Brodrick
 * @version $Id: MonClientUtil.java,v 1.9 2008/03/18 00:52:10 bro764 Exp bro764
 *          $
 */
public class MonClientUtil {
  /**
   * Hashmap contains a Hashtable of SavedSetups for each class, indexed by
   * class name .
   */
  private static Hashtable<String, Hashtable<String, SavedSetup>> theirSetups;

  /** Network connection to the monitor server. */
  private static MoniCAClient theirServer;

  /** Short descriptive name of the user-selected server. */
  private static String theirServerName;

  /** Cached copy of monitor point name list */
  private static String[] theirPointNameCache;

  /** Initialise stuff. */
  static {
    String host = null;
    Vector chosenserver = null;

    String headless = System.getProperty("java.awt.headless", "false");
    String targethost = System.getProperty("MoniCA.server", null);
    if (targethost == null) {
      // Provide support for deprecated property name
      targethost = System.getProperty("server", null);
    }

    if (targethost != null) {
      host = targethost;
    } else {
      if (headless.equals("true")) {
        // We can only run in headless mode if a server was specified
        System.err.println("MonClientUtil: ERROR: Headless mode requested but no server specified!");
        System.exit(1);
      }

      Vector<Vector<String>> serverlist = new Vector<Vector<String>>();
      Vector<String> defaultserver = null;
      try {
        // Get the list of server definitions
        InputStream res = MonClientUtil.class.getClassLoader().getResourceAsStream("monitor-servers.txt");
        if (res == null) {
          throw new Exception();
        }
        BufferedReader serversfile = new BufferedReader(new InputStreamReader(res));
        int linecounter = 0;
        int def = -1;
        while (serversfile.ready()) {
          String thisline = serversfile.readLine();
          linecounter++;

          if (thisline.startsWith("#") || thisline.trim().equals("")) {
            continue;
          }

          if (thisline.startsWith("default")) {
            // This line specifies the default server to use
            String[] tokens = thisline.split("\t");
            if (tokens.length < 2) {
              System.err.println("MonClientUtil: WARNING: monitor-servers.txt parse error line " + linecounter);
              continue;
            }
            try {
              def = Integer.parseInt(tokens[1]) - 1;
            } catch (Exception e) {
              System.err.println("MonClientUtil: WARNING: monitor-servers.txt parse error line " + linecounter);
              continue;
            }
            continue;
          }

          // This line is a server definition
          String[] tokens = thisline.split("\t");
          if (tokens == null || tokens.length < 2 || tokens[1].trim().equals("")) {
            System.err.println("MonClientUtil: WARNING: monitor-servers.txt parse error line " + linecounter);
            continue;
          }
          Vector<String> thisserver = new Vector<String>();
          thisserver.add(tokens[0].trim());
          thisserver.add(tokens[1].trim());
          if (tokens.length > 2) {
            thisserver.add(tokens[2].trim());
          } else {
            thisserver.add(null);
          }
          // Add the new server to the list
          serverlist.add(thisserver);
        }

        // If the file specified a default server then use it.
        if (def >= 0) {
          if (def > serverlist.size() - 1) {
            System.err.println("MonClientUtil: WARNING: Default server " + (def + 1) + " requested but only " + serverlist.size()
                + " servers defined: IGNORING DEFAULT");
          } else {
            defaultserver = serverlist.get(def);
          }
        }
      } catch (Exception e) {
        System.err.println("MonClientUtil: ERROR: Couldn't find list of monitor servers!");
        System.exit(1);
      }

      if (serverlist.size() == 1) {
        // Only one server specified, so connect to that
        chosenserver = serverlist.get(0);
      } else {
        // We can launch a GUI tool to ask the user what site to connect to
        SiteChooser chooser = new SiteChooser(serverlist, defaultserver);
        chosenserver = chooser.getSite();
      }
      host = (String) chosenserver.get(1);
      theirServerName = (String) chosenserver.get(0);
    }

    JFrame frame = null;
    JProgressBar progressBar = null;
    if (headless.equals("false")) {
      frame = new JFrame("MoniCA");
      //frame.setUndecorated(true);
      frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      progressBar = new JProgressBar();
      progressBar.setString("Connecting to Server");
      progressBar.setIndeterminate(true);
      progressBar.setStringPainted(true);
      frame.getContentPane().add(progressBar, BorderLayout.CENTER);
      frame.setSize(new Dimension(400, 80));
      frame.setVisible(true);
    }

    boolean locator = false;
    int port = 0;
    try {
      if (host.indexOf("locator://") != -1) {
        // An Ice Locator service has been specified
        locator = true;
        host = host.substring(host.lastIndexOf("/") + 1);
        // A port MUST be specified for the Locator
        if (host.indexOf(":") == -1) {
          throw new Exception("No port number was specified for the locator!");
        }
        port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
        host = host.substring(0, host.indexOf(":"));
        Ice.Properties props = Ice.Util.createProperties();
        props.setProperty("Ice.Default.Locator", "IceGrid/Locator:tcp -h " + host + " -p " + port);
        System.out.println("MonClientUtil: Connecting to Locator on host \"" + host + "\" on port " + port);
        theirServer = new MoniCAClientIce(props);
      } else {
        port = MoniCAClientIce.getDefaultPort();
        if (host.indexOf(":") != -1) {
          port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
          host = host.substring(0, host.indexOf(":"));
        }
        System.out.println("MonClientUtil: Connecting to host \"" + host + "\" on port " + port);
        if (headless.equals("false")) {
          if (chosenserver.get(2) != null) {
            progressBar.setString("Attempting Direct Connection to Server");
          } else {
            progressBar.setString("Connecting to Server");
          }
        }
        theirServer = new MoniCAClientIce(host, port);
      }
      theirSetups = new Hashtable<String, Hashtable<String, SavedSetup>>();
      if (headless.equals("false")) {
        progressBar.setString("Fetching Saved Setups");
      }
      addServerSetups(); // Get all SavedSetups from server
      addLocalSetups(); // Also load any which have been saved locally
      if (headless.equals("false")) {
        progressBar.setString("Fetching Point List");
      }      
      cachePointNames(); // Cache the list of points available on the server
    } catch (Exception e) {
      if (headless.equals("true")) {
        System.err.println("MonClientUtil: ERROR: Couldn't connect to server. Goodbye.");
        System.exit(1);
      } else if (locator) {
        // Cannot currently connect via locator service through tunnelled
        // connection
        JOptionPane.showMessageDialog(MonFrame.theirWindowManager.getWindow(0), "ERROR CONTACTING LOCATOR\n\n" + "Server: " + host + ":" + port + "\n"
            + "Error: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()) + "\n\n" + "The application will now exit.",
            "Error Contacting Server", JOptionPane.WARNING_MESSAGE);
        System.exit(1);
      } else {
        boolean connected = false;
        if (chosenserver.get(2) != null) {
          try {
            // Choose a random local port
            int localport = 8060 + (new Random()).nextInt() % 2000;
            progressBar.setString("Waiting For SSH Credentials");
            new SecureTunnel((String) chosenserver.get(1), (String) chosenserver.get(2), localport, port);
            System.out.println("MonClientUtil: Connecting to \"localhost\"");
            // theirServer = new MoniCAClientCustom("localhost");
            progressBar.setString("Attempting Tunnelled Connection");
            theirServer = new MoniCAClientIce("localhost", localport);
            theirSetups = new Hashtable<String, Hashtable<String, SavedSetup>>();
            progressBar.setString("Fetching Saved Setups");
            addServerSetups(); // Get all SavedSetups from server            
            addLocalSetups(); // Also load any which have been saved locally
            progressBar.setString("Fetching Point List");
            cachePointNames(); // Cache the list of points available on the server
            connected = true;
          } catch (Exception f) {
          }
        }

        if (!connected) {
          JOptionPane.showMessageDialog(MonFrame.theirWindowManager.getWindow(0),
              "ERROR CONTACTING SERVER\n\n" + "Server: " + host + "\n" + "Error: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage())
                  + "\n\n" + "The application will now exit.", "Error Contacting Server", JOptionPane.WARNING_MESSAGE);
          System.exit(1);
        }
      }
    }
    if (headless.equals("false")) {
      frame.setVisible(false);
    }
  }

  /** Get the short name of the server. */
  public static synchronized String getServerName() {
    return theirServerName;
  }

  /** Get a reference to the network connection to the server. */
  public static synchronized MoniCAClient getServer() {
    return theirServer;
  }

  /** Cache the list of points available from the server. */
  private static void cachePointNames() {
    try {
      theirPointNameCache = theirServer.getAllPointNames();
    } catch (Exception e) {
      System.err.println("MonClientUtil.cachePointNames: " + e.getClass() + ": " + e.getMessage());
      System.exit(1);
    }
  }

  /** Return the cached list of all point names. */
  public static String[] getAllPointNames() {
    return theirPointNameCache;
  }

  /**
   * Return the names of all sources for each of the given points. The return
   * Vector will be of the same length as the argument Vector. Each entry will
   * be an array of Strings or possibly <tt>null</tt>.
   * 
   * @param names
   *          Vector containing String names for the points.
   * @return Vector containing an array of names for each requested point.
   */
  public static Vector<Vector<String>> getSources(Vector points) {
    if (points == null || points.size() == 0) {
      return null;
    }

    Vector<Vector<String>> res = new Vector<Vector<String>>(points.size());
    for (int i = 0; i < points.size(); i++) {
      if (points.get(i) != null && points.get(i) instanceof String) {
        String searchname = (String) points.get(i);
        Vector<String> match = new Vector<String>();
        for (int j = 0; j < theirPointNameCache.length; j++) {
          String thispoint = theirPointNameCache[j];
          int doti = thispoint.indexOf(".");
          String source = thispoint.substring(0, doti);
          String thisname = thispoint.substring(doti + 1, thispoint.length());
          if (thisname.equals(searchname)) {
            match.add(source);
          }
        }
        if (match.size() == 0) {
          res.add(null);
        } else {
          res.add(match);
        }
      } else {
        res.add(null);
      }
    }
    return res;
  }

  /**
   * Download all the SavedSetups which are available on the server and add them
   * to our collection.
   */
  public static void addServerSetups() {
    try {
      Vector<SavedSetup> setups = theirServer.getAllSetups();
      if (setups != null && setups.size() > 0) {
        // System.err.println("MonClientUtil:addServerSetups: Loaded " +
        // setups.size() + " setups from server");
        mergeSetups(setups);
      } else {
        // System.err.println("MonClientUtil:addServerSetups: None available");
      }
    } catch (Exception e) {
      System.err.println("MonClientUtil.addServerSetups: " + e.getClass() + " " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Download all the SavedSetups which available locally and add them to our
   * collection.
   */
  public static void addLocalSetups() {
    // Figure out which platform we are on.
    String osname = System.getProperty("os.name").toLowerCase();
    String monfile = null;
    if (osname.indexOf("win") != -1) {
      // Must be some flavour of winblows
      monfile = "\\Application Data\\MoniCA\\local-setups.txt";
    } else {
      // But what about other platforms?
      monfile = "/.MoniCA/local-setups.txt";
    }

    monfile = System.getProperty("user.home") + monfile;

    Vector<SavedSetup> setups = null;
    try {
      setups = SavedSetup.parseFile(monfile);
    } catch (Exception e) {
      System.err.println(e.getClass() + " while parsing " + monfile);
    }

    if (setups != null && setups.size() > 0) {
      // We found some local setups, print message and add them
      // System.err.print("MonClientUtil:addLocalSetups: Loaded " +
      // setups.size() + " setups from:");
      // System.err.println(monfile);
      mergeSetups(setups);
    } else {
      // System.err.println("MonClientUtil:addLocalSetups: No setups found
      // in:");
      // System.err.println(monfile);
    }
  }

  /** Merge the Vector of SavedSetups with our collection. */
  public static void mergeSetups(Vector<SavedSetup> setups) {
    if (setups == null || setups.size() == 0) {
      return;
    }

    for (int i = 0; i < setups.size(); i++) {
      SavedSetup thissetup = setups.get(i);
      if (thissetup == null) {
        System.err.println("MonClientUtil:mergeSetups: Warning NULL setup");
        continue;
      }
      if (thissetup.getName() == null) {
        System.err.println("MonClientUtil:mergeSetups: Warning NULL name");
        continue;
      }

      if (theirSetups.get(thissetup.getClassName()) == null) {
        // First setup to be added for that class
        theirSetups.put(thissetup.getClassName(), new Hashtable<String, SavedSetup>());
      }

      // Add this setup to the vector for the appropriate class
      Hashtable<String, SavedSetup> classsetups = theirSetups.get(thissetup.getClassName());
      classsetups.put(thissetup.getName(), thissetup);
    }
  }

  /** Merge the given SavedSetup with our collection. */
  public static void mergeSetup(SavedSetup setup) {
    if (setup == null) {
      System.err.println("MonClientUtil:mergeSetup: Warning NULL setup");
      return;
    }
    if (setup.getName() == null) {
      System.err.println("MonClientUtil:mergeSetup: Warning NULL name");
      return;
    }

    if (theirSetups.get(setup.getClassName()) == null) {
      // First setup to be added for that class
      theirSetups.put(setup.getClassName(), new Hashtable<String, SavedSetup>());
    }

    // Add this setup to the vector for the appropriate class
    Hashtable<String, SavedSetup> classsetups = theirSetups.get(setup.getClassName());
    classsetups.put(setup.getName(), setup);
  }

  /**
   * Return a TreeUtil of the SavedSetups available for the specified class.
   * 
   * @return TreeUtil for available SavedSetups.
   */
  protected static TreeUtil getSetupTreeUtil(Class c) {
    return getSetupTreeUtil(c.getName());
  }

  /**
   * Return a TreeUtil of the SavedSetups available for the specified class.
   * 
   * @return TreeUtil for available SavedSetups.
   */
  protected static TreeUtil getSetupTreeUtil(String c) {
    TreeUtil res = new NavigatorTree("Setups");

    // Get the container of all setups for the required class
    Hashtable setups = (Hashtable) theirSetups.get(c);
    if (setups == null) {
      return null;
    }

    Set keyset = setups.keySet();
    Object[] setupnames = keyset.toArray();
    if (setupnames == null || setupnames.length == 0) {
      return null;
    }

    for (int i = 0; i < setupnames.length; i++) {
      String name = (String) setupnames[i];
      res.addNode(name);
    }

    return res;
  }

  /**
   * Get the names of all setups for the specified class.
   * 
   * @param c
   *          Name of the class to obtain the setup names for.
   * @return Array of setup names, never <tt>null</tt>.
   */
  public static String[] getSetupNames(String c) {
    // Get the container of all setups for the required class
    Hashtable setups = (Hashtable) theirSetups.get(c);
    if (setups == null) {
      return new String[0];
    }

    Object[] allnames = setups.keySet().toArray();
    String[] res = new String[setups.size()];
    for (int i = 0; i < allnames.length; i++) {
      res[i] = (String) allnames[i];
    }
    return res;
  }

  /**
   * Get the named setup for the specified class.
   * 
   * @param c
   *          Name of the class to obtain the setup for.
   * @param name
   *          Name of the setup to fetch.
   * @return The requested setup, or <tt>null</tt> if it doesn't exist.
   */
  public static SavedSetup getSetup(String c, String name) {
    // Get the container of all setups for the required class
    Hashtable setups = (Hashtable) theirSetups.get(c);
    if (setups == null) {
      return null;
    }
    return (SavedSetup) setups.get(name);
  }

  /**
   * Get the named setup for the specified class.
   * 
   * @param c
   *          Class to obtain the setup for.
   * @param name
   *          Name of the setup to fetch.
   * @return The requested setup, or <tt>null</tt> if it doesn't exist.
   */
  public static SavedSetup getSetup(Class c, String name) {
    return getSetup(c.getName(), name);
  }

  public static JMenuItem getSetupMenu(Class c) {
    return getSetupMenu(c.getName());
  }

  public static JMenuItem getSetupMenu(String c) {
    TreeUtil res = getSetupTreeUtil(c);
    if (res == null) {
      return null;
    }
    return res.getMenus();
  }

  public static void getSetupMenu(Class c, JMenu menu) {
    getSetupMenu(c.getName(), menu);
  }

  public static void getSetupMenu(String c, JMenu menu) {
    TreeUtil res = getSetupTreeUtil(c);
    if (res == null) {
      return;
    }
    res.getMenus(menu);
  }

  public static JMenuItem getSetupMenu(Class c, ActionListener listener) {
    return getSetupMenu(c.getName(), listener);
  }

  public static JMenuItem getSetupMenu(String c, ActionListener listener) {
    TreeUtil res = getSetupTreeUtil(c);
    if (res == null) {
      return null;
    }
    res.addActionListener(listener);
    return res.getMenus();
  }

  public static void getSetupMenu(Class c, ActionListener listener, JMenu parent) {
    getSetupMenu(c.getName(), listener, parent);
  }

  public static void getSetupMenu(String c, ActionListener listener, JMenu parent) {
    TreeUtil res = getSetupTreeUtil(c);
    if (res == null) {
      return;
    }
    res.addActionListener(listener);
    res.getMenus(parent);
  }

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
    Vector itsServers = null;

    /** The countdown Timer. */
    Timer itsTimer = null;

    public SiteChooser(Vector servers, Vector def) {
      itsServers = servers;
      itsDefault = def;

      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
      JPanel temppanel = new JPanel();
      temppanel.setLayout(new BoxLayout(temppanel, BoxLayout.Y_AXIS));
      temppanel.add(new JLabel("Monitor which site?"));

      for (int i = 0; i < itsServers.size(); i++) {
        Vector thisserver = (Vector) itsServers.get(i);
        JButton tempbutton = new JButton((String) thisserver.get(0));
        tempbutton.addActionListener(this);
        tempbutton.setActionCommand("" + i);
        tempbutton.setMinimumSize(new Dimension(240, 28));
        tempbutton.setPreferredSize(new Dimension(240, 28));
        tempbutton.setMaximumSize(new Dimension(240, 28));
        temppanel.add(tempbutton);
      }

      if (itsDefault != null) {
        itsCounter = new JLabel("Default \"" + itsDefault.get(0) + "\" in " + itsTimeout, JLabel.CENTER);
        itsCounter.setForeground(Color.red);
        itsCounter.setMinimumSize(new Dimension(240, 28));
        itsCounter.setPreferredSize(new Dimension(240, 28));
        itsCounter.setMaximumSize(new Dimension(240, 28));
        temppanel.add(itsCounter);
      }

      getContentPane().add(temppanel);

      // Do this on the AWT threads time to avoid deadlocks
      final SiteChooser realthis = this;
      final Runnable choosenow = new Runnable() {
        public void run() {
          realthis.pack();
          if (itsDefault != null) {
            realthis.setSize(new Dimension(240, 74 + 28 * itsServers.size()));
          } else {
            realthis.setSize(new Dimension(240, 46 + 28 * itsServers.size()));
          }
          realthis.validateTree();
          realthis.setVisible(true);
          if (itsDefault != null) {
            itsTimer = new Timer(1000, realthis);
            itsTimer.start();
          }
        }
      };
      try {
        SwingUtilities.invokeAndWait(choosenow);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == itsTimer) {
        // timer update
        itsTimeout--;
        if (itsTimeout == 0) {
          itsServer = itsDefault;
          setVisible(false);
          synchronized (this) {
            notifyAll();
          }
          ((Timer) e.getSource()).stop();
        } else {
          itsCounter.setText("Default \"" + itsDefault.get(0) + "\" in " + itsTimeout);
        }
      } else {
        // user server selection
        itsServer = (Vector) itsServers.get(Integer.parseInt(e.getActionCommand()));
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
      } catch (Exception e) {
      }
      return itsServer;
    }
  }

  /**
   * Sub-class of TreeUtil for building the Navigator window, which knows to put
   * the "favourites" menu at the top, and to put a separator item beneath it
   * when building a Menu tree.
   */
  public static class NavigatorTree extends TreeUtil {
    public NavigatorTree(String name, Object root) {
      super(name, root);
    }

    public NavigatorTree(String name) {
      super(name);
    }

    public void addNode(String name, Object obj) {
      itsMap.put(name, obj);
      DefaultMutableTreeNode tempNode = itsRootNode;
      StringTokenizer tok = new StringTokenizer(name, ".");
      String currentName = null;
      while (tok.hasMoreTokens()) {
        String myTok = tok.nextToken();
        currentName = (currentName == null) ? myTok : currentName + "." + myTok;
        boolean createNew = true;
        for (int j = 0; j < tempNode.getChildCount(); j++) {
          DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) tempNode.getChildAt(j);
          if (childNode.toString().equals(myTok)) {
            tempNode = childNode;
            createNew = false;
            break;
          }
        }
        if (createNew) {
          DefaultMutableTreeNode aNode = new DefaultMutableTreeNode(myTok);
          itsTreeMap.put(currentName, aNode);
          // Let's give some consideration to where in the tree we place the new
          // node.
          // We want any nodes with children to be listed first, in alphabetical
          // order.
          // Then come nodes with no children, in alphabetical order.
          if (tok.hasMoreTokens()) {
            // This node is not a leaf node
            if (myTok.toLowerCase().equals("favourites")) {
              tempNode.insert(aNode, 0);
            } else {
              int targeti;
              for (targeti = 0; targeti < tempNode.getChildCount(); targeti++) {
                TreeNode bNode = tempNode.getChildAt(targeti);
                if (bNode.isLeaf() || (bNode.toString().compareToIgnoreCase(myTok) > 0 && !bNode.toString().equals("favourites"))) {
                  break;
                }
              }
              tempNode.insert(aNode, targeti);
            }
          } else {
            // This node is a leaf node
            int targeti;
            for (targeti = 0; targeti < tempNode.getChildCount(); targeti++) {
              TreeNode bNode = tempNode.getChildAt(targeti);
              if (bNode.isLeaf() && bNode.toString().compareToIgnoreCase(myTok) > 0) {
                break;
              }
            }
            tempNode.insert(aNode, targeti);
          }
          tempNode = aNode;
        }
      }
    }

    /**
     * Make a Menu structure, without the root node. The children of the root
     * node will be added to the specified menu element.
     */
    public void getMenus(JMenu menu) {
      int numChild = itsRootNode.getChildCount();
      for (int i = 0; i < numChild; i++) {
        DefaultMutableTreeNode thisnode = (DefaultMutableTreeNode) itsRootNode.getChildAt(i);
        JMenuItem thisitem = getMenus(thisnode, menu);
        if (thisnode.toString().equals("favourites")) {
          thisitem.setForeground(Color.blue);
        }
        menu.add(thisitem);
        // menu.addSeparator();
      }
    }
  }
}
