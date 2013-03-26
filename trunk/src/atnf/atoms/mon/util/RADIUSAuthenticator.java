//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.util;

import org.apache.log4j.Logger;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusClient;

/**
 * Wrapper for using the tinyradius library to authenticate against a RADIUS server.
 * 
 * <P>
 * For authentication to work this required three properties to be defined:
 * <ul>
 * <li>RADIUSHost (MoniCA.RADIUSHost): The host name of the machine running the RADIUS server.
 * <li>RADIUSPort (MoniCA.RADIUSPort): The port number for the RADIUS server.
 * <li>RADIUSSecret (MoniCA.RADIUSSecret): The shared secret for communication with the server.
 * </ul>
 * 
 * <P>
 * If any of these properties are undefined or invalid then we will not attempt to communicate with the RADIUS server and
 * authentication will fallover to approve-all or deny-all, depending on the argument given to <tt>setDefaultAuthMode</tt>.
 * 
 * <P>
 * When RADIUS Access Requests are sent to the server, the IP address of the requesting client will be given in the
 * <tt>NAS-IP-Address</tt> attribute.
 * 
 * @author David Brodrick
 */
public class RADIUSAuthenticator {
  /** The RADIUS server to authenticate against. */
  private static String theirServer;

  /** The port to contact the RADIUS server. */
  private static int theirPort = -1;

  /** The shared secret for the RADIUS server. */
  private static String theirSharedSecret;

  /** The RADIUS client. */
  private static RadiusClient theirRadiusClient;

  /**
   * Should we authorise all requests if no server is defined (for backwards compatibility), if True, or deny all requests for
   * security (if False).
   */
  private static boolean theirDefaultAuthMode = true;

  /** Logger. */
  private static Logger theirLogger = Logger.getLogger(RADIUSAuthenticator.class.getName());

  /**
   * Static block, try to read the details for the RADIUS server from properties. Set theirServer to null if we do not have
   * sufficient details to connect to a server.
   */
  static {
    theirServer = System.getProperty("MoniCA.RADIUSHost");
    if (theirServer == null) {
      theirServer = MonitorConfig.getProperty("RADIUSHost");
    }
    if (theirServer != null) {
      String port;
      try {
        port = System.getProperty("MoniCA.RADIUSPort");
        if (port == null) {
          port = MonitorConfig.getProperty("RADIUSPort");
        }
        if (port == null) {
          theirLogger.error("RADIUS server port number not defined");
          theirServer = null;
        } else {
          theirPort = Integer.parseInt(port);
        }
      } catch (NumberFormatException e) {
        theirLogger.error("Error parsing RADIUS server port number");
        theirServer = null;
      }
    }
    if (theirServer != null) {
      theirSharedSecret = System.getProperty("MoniCA.RADIUSSecret");
      if (theirSharedSecret == null) {
        theirSharedSecret = MonitorConfig.getProperty("RADIUSSecret");
      }
      if (theirSharedSecret == null) {
        theirLogger.error("RADIUS server shared secret not defined");
        theirServer = null;
      }
    }
  }

  /**
   * Should we authorise all requests if no server is defined (for backwards compatibility), if True, or deny all requests (for
   * better security), if False.
   */
  public synchronized static void setDefaultAuthMode(boolean auth) {
    theirDefaultAuthMode = auth;
  }

  /** Return true if the details for the RADIUS server have been defined. */
  private synchronized static boolean isServerDefined() {
    return (theirServer != null);
  }

  /** Connect to the RADIUS server. */
  private synchronized static void connect() {
    if (isServerDefined()) {
      try {
        theirRadiusClient = new RadiusClient(theirServer, theirSharedSecret);
        theirRadiusClient.setAuthPort(theirPort);
      } catch (Exception e) {
        theirLogger.warn("While connecting to RADIUS server: " + e);
        theirRadiusClient = null;
      }
    }
  }

  /**
   * Check the users credentials.
   * 
   * @param username
   *          The username to be authenticated.
   * @param password
   *          The password associated with this user.
   * @param host
   *          The users IP address.
   * @return True is successful, False otherwise.
   */
  public synchronized static boolean authenticate(String username, String password, String host) {
    // If no server defined then fallback to default response
    if (!isServerDefined()) {
      return theirDefaultAuthMode;
    }

    if (theirRadiusClient == null) {
      // Instanciate classes for communication with the server
      connect();
    }

    boolean res = false;
    if (theirRadiusClient != null) {
      try {
        AccessRequest ar = new AccessRequest(username, password);
        ar.setAuthProtocol(AccessRequest.AUTH_CHAP); // or AUTH_PAP
        ar.addAttribute("NAS-IP-Address", host);
        RadiusPacket response = theirRadiusClient.authenticate(ar);
        if (response.getPacketType() == RadiusPacket.ACCESS_ACCEPT) {
          // Server verified the authentication request
          res = true;
        }
      } catch (Exception e) {
        theirLogger.warn("While attempting to authenticate user: " + e);
      }
    } else {
      theirLogger.warn("Unable to connect to RADIUS server");
    }
    return res;
  }

  /** Simple test method - needs server properties defined separately. */
  public static final void main(String[] args) {
    if (args.length < 3) {
      System.err.println("USAGE: RADIUSAuthenticator <username> <password> <IP address>");
      System.exit(1);
    }
    boolean res = RADIUSAuthenticator.authenticate(args[0], args[1], args[2]);
    theirLogger.info("Authentication result = " + res);
  }
}
