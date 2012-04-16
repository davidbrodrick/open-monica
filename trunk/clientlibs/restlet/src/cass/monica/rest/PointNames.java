package cass.monica.rest;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;

import atnf.atoms.mon.comms.MoniCAClient;

/**
 * Resource for getting the list of point names.
 * 
 * @author Mark Wieringa
 */
public class PointNames extends ServerResource {
  String[] itsPointNames;
  String itsCallback = "";

  @Override
  public void doInit() {
    itsCallback = getQuery().getFirstValue("callback", "");
    try {
      itsPointNames = getClient().getAllPointNames();
    } catch (Exception e) {
      System.err.println(e);
      itsPointNames = null;
    }
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

  @Get
  @Post
  public Representation getPoint() {
    String out = "";
    String data;
    if (itsPointNames != null) {
      data = "{\"status\":\"ok\", \"names\":" + getGson().toJson(itsPointNames) + "}";
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
