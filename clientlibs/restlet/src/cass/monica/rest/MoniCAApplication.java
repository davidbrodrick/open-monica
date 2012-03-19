package cass.monica.rest;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.comms.MoniCAClientIce;
import atnf.atoms.time.AbsTime;

public class MoniCAApplication extends Application {

    /**
     * Run  as a standalone component.
     * 
     * @param args
     *            The optional arguments.
     * @throws Exception
     */
    private  MoniCAClient client;
    private Gson gson;
	
    public MoniCAApplication(String monicaserver) {
	GsonBuilder gsonbuilder = new GsonBuilder();
	gsonbuilder.registerTypeAdapter(AbsTime.class, 
					new AbsTimeSerializer(AbsTime.Format.UTC_STRING));
	gson = gsonbuilder.create();
	
	try	{
	    client = new MoniCAClientIce(monicaserver);
	} catch (Exception e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }	
    public Gson getGson() {
    	return this.gson;
    }
    
    public MoniCAClient getClient() {
    	return this.client;
    }
   
    public static void main(String[] args) throws Exception {
        // Create a component
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, 8111);
	String monicaserver = "narrabri.ozforecast.com.au";
	if (args.length == 1) {
	    monicaserver = args[0];
	}
        // Create an application
        Application application = new MoniCAApplication(monicaserver);
	
        // Attach the application to the component and start it
        component.getDefaultHost().attachDefault(application);
        component.start();
    }

    @Override
    public Restlet createInboundRoot() {
        // Create a router
        Router router = new Router(getContext());

        // Attach the resources to the router
        // redundant - use vector version
        router.attach("/point/{name}", PointResource.class);
        // Return the root router
        return router;
    }

}