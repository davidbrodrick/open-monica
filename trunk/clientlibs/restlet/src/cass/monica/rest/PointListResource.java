package cass.monica.rest;

//import org.restlet.Application;
import java.util.Arrays;
import java.util.Vector;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.comms.MoniCAClient;


public class PointListResource extends ServerResource {  
	Vector<String> pointNames;
	// "home.weather.wind_speed"
	
    @Override
    public void doInit() {
    	String name = (String) getRequestAttributes().get("names");
    	this.pointNames = new Vector<String>(Arrays.asList(name.split(",")));
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
    		Vector<PointData> data = getClient().getData(this.pointNames);
    		return getGson().toJson(data);
		} catch (Exception e) {
		      System.err.println(e);			
		}
    	return out;
    }

}
