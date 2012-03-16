package cass.monica.rest;

//import org.restlet.Application;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Options;

import com.google.gson.Gson;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.comms.MoniCAClient;


public class PointResource extends ServerResource {  
	String pointName;
	// "home.weather.wind_speed"
	
    @Override
    public void doInit() {
	// getQuery gets '?key=value' parameters
	System.err.println(getQuery());
    	this.pointName = (String) getRequestAttributes().get("name");
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
    	return new StringRepresentation(generateJSON(), 
    			                        MediaType.APPLICATION_JSON);
    }

    private String generateJSON() {
    	String out = "{}";

    	try {
    		PointData data = getClient().getData(this.pointName);
    		if (data.isValid()) {
    			return getGson().toJson(data);
    		}
		} catch (Exception e) {
		      System.err.println(e);			
		}
    	return out;
    }

}
