package cass.monica.rest;

//import org.restlet.Application;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import org.restlet.representation.Representation;
import org.restlet.resource.Options;

import com.google.gson.Gson;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.comms.MoniCAClient;


public class PointResource extends ServerResource {  
    String pointName;
    PointData pointData;
    String callback = "";
	
    @Override
    public void doInit() {
	// getQuery gets '?key=value' parameters
	//	System.err.println(getQuery());
	callback = this.getQuery().getFirstValue("callback", "");
    	this.pointName = (String) getRequestAttributes().get("name");
    	try {
	    this.pointData = getClient().getData(this.pointName);
	} catch (Exception e) {
	    System.err.println(e);			
	    this.pointData = null;
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
    	return getApplication().getGson();
    }

    @Get   
    public Representation getPoint() {
	String out  = "";
	if (this.pointData != null && this.pointData.isValid()) {
	    String data = getGson().toJson(this.pointData);
	    if (callback != "") {
		out += callback+"("+data+")";
	    } else {
		out += data;
	    }
	} else {
	    out = "{'error': 'not a valid point.'}";
	    if (callback != "") {
		out = callback+"("+out+")";
	    }
	} 	
	return new StringRepresentation(out, 
					MediaType.APPLICATION_JAVASCRIPT);
    }
}
