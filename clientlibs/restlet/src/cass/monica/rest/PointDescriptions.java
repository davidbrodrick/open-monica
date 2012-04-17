package cass.monica.rest;

import org.restlet.data.MediaType;
import org.restlet.representation.*;
import org.restlet.resource.*;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Vector;

import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.PointDescription;

;

/**
 * Resource for getting point descriptions.
 * 
 * @author Mark Wieringa
 * @author David Brodrick
 */
public class PointDescriptions extends ServerResource {
  /** The point definitions obtained from the server. */
  private Vector<PointDescription> itsPoints;

  /** The list of points requested (if specified). */
  private Vector<String> itsPointNames;

  /** Callback used to allow cross-domain requests (?). */
  private String itsCallback = "";

  @Override
  public void doInit() {
    itsCallback = getQuery().getFirstValue("callback", "");
  }

  @Override
  public MoniCAApplication getApplication() {
    return (MoniCAApplication) super.getApplication();
  }

  public MoniCAClient getClient() {
    return getApplication().getClient();
  }

  public Gson getGson() {
    return MoniCAApplication.getGson();
  }

  /** Handle the GET request, with or without a point name specified. */
  @Get
  public Representation getDescription() {
    if (getRequestAttributes().get("name") != null) {
      itsPointNames = new Vector<String>(1);
      itsPointNames.add((String) getRequestAttributes().get("name"));
    }
    return doQuery();
  }

  /** Trivial class for the point names request structure. */
  private class PointNames {
    String[] points;
  }

  /** Handle the POST request. */
  @Post
  public Representation getDescriptions(String arg) {
    // Parse the request
    PointNames p = getGson().fromJson(arg, PointNames.class);
    if (p.points != null && p.points.length > 0) {
      // Get the requested point names as a vector
      itsPointNames = new Vector<String>(Arrays.asList(p.points));
    }
    return doQuery();
  }

  /** Contact the server to handle the request. */
  private Representation doQuery() {
    try {
      if (itsPointNames == null || itsPointNames.size() == 0) {
        // Get all points
        itsPoints = getClient().getAllPoints();
      } else {
        // Specified point names were provided
        itsPoints = getClient().getPoints(itsPointNames);
      }
    } catch (Exception e) {
      System.err.println(e);
      itsPoints = null;
    }

    String out = "";
    String data;
    if (itsPoints != null) {
      data = "{\"status\":\"ok\", \"descriptions\": " + getGson().toJson(itsPoints) + "}";
    } else {
      data = "{\"status\":\"fail\"}";
    }
    if (itsCallback != "") {
      out += itsCallback + "(" + data + ")";
    } else {
      out += data;
    }
    return new StringRepresentation(out, MediaType.APPLICATION_JAVASCRIPT);
  }
}
