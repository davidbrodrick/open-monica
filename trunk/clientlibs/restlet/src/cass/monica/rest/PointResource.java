package cass.monica.rest;

//import org.restlet.Application;
import org.restlet.data.*;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import org.restlet.representation.Representation;
import org.restlet.resource.Options;

import com.google.gson.*;

import java.util.*;

import atnf.atoms.time.AbsTime;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.util.MonitorUtils;

public class PointResource extends ServerResource {
  private String callback = "";

  @Override
  public void doInit() {
    callback = this.getQuery().getFirstValue("callback", "");
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
  public Representation processRequest() {
    // Parse the request parameters
    MoniCARequest req = new MoniCARequest();
    if (getQuery().getFirst("start") != null && getQuery().getFirst("end") != null) {
      req.itsRequestType = MoniCARequest.BETWEEN;
      req.itsStartTime = AbsTime.factory(getQuery().getFirst("start").getValue());
      req.itsEndTime = AbsTime.factory(getQuery().getFirst("end").getValue());
    } else if (getQuery().getFirst("before") != null) {
      req.itsRequestType = MoniCARequest.BEFORE;
      req.itsTime = AbsTime.factory(getQuery().getFirst("before").getValue());
    } else if (getQuery().getFirst("after") != null) {
      req.itsRequestType = MoniCARequest.AFTER;
      req.itsTime = AbsTime.factory(getQuery().getFirst("after").getValue());
    } else {
      // Default get operation
      req.itsRequestType = MoniCARequest.GET;
    }
    // Get the point name
    if (getRequestAttributes().get("name") != null) {
      req.itsPointNames = new String[] { (String) getRequestAttributes().get("name") };
    }
    System.err.println(req);
    return req.completeRequest(getClient());
  }

  @Post
  public Representation acceptJson(String arg) {
    try {
      System.err.println("Got JSON request: " + arg);
      JsonParser parser = new JsonParser();
      MoniCARequest req = getGson().fromJson(arg, MoniCARequest.class);
      System.err.println(req);
      return req.completeRequest(getClient());
    } catch (Exception e) {
      System.err.println("acceptJson: Got exception: " + e);
      e.printStackTrace();
    }
    return null;
  }
}
