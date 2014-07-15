package cass.monica.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.comms.MoniCAClientIce;
import atnf.atoms.time.AbsTime;
import atnf.atoms.util.Angle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MoniCAApplication extends Application {
	private static Logger logger = Logger.getLogger(MoniCAApplication.class.getName());
	
	private MoniCAClient monicaServer;
	private String serverName;

	private static Gson theirGson;

	public MoniCAApplication() {
		GsonBuilder gsonbuilder = new GsonBuilder();
		gsonbuilder.registerTypeAdapter(Angle.class, new AngleSerializer());
		gsonbuilder.registerTypeAdapter(AbsTime.class, new AbsTimeSerializer(AbsTime.Format.UTC_STRING));
		gsonbuilder.registerTypeAdapter(PointDescription.class, new PointDescriptionSerializer());
		gsonbuilder.registerTypeAdapter(PointData.class, new PointDataSerializer());
		theirGson = gsonbuilder.setPrettyPrinting().create();
	}


	public static Gson getGson() {
		return theirGson;
	}

	public MoniCAClient getClient() {
		if (monicaServer==null) {
			Properties parset = new Properties();
			// get property from property file if not set
			if (serverName==null || serverName.trim().length()==0) {
				try {
					InputStream in = MoniCAClient.class.getClassLoader().getResourceAsStream("monica-restlet.properties");
					parset.load(in);
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Could not load peroperties file: obs.properties", e);
				}

				serverName = parset.getProperty("monics-server-name", "");
				parset.remove("monics-server-name");
			}
			setMonicaServer(serverName, parset);
		}
		
		return monicaServer;
	}

	public void setMonicaServer(String serverName, Properties parset) {
		if (serverName!=null && serverName.trim().length()>0) {
			try {
				this.serverName = serverName;
				
				if (parset.size() > 0) {
					 Ice.Properties props = Ice.Util.createProperties();
					 props.setProperty("Ice.Default.Locator", serverName);
					 for (Enumeration<Object> iter=parset.keys(); iter.hasMoreElements();) {
						 String key = (String) iter.nextElement();
						 String value = parset.getProperty(key);
						 
						 props.setProperty(key, value);
						 
						monicaServer =  new MoniCAClientIce (props);
					 }
				} else {
					monicaServer =  new MoniCAClientIce (serverName);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not connect to " + serverName, e);
			}
		}
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
		MoniCAApplication application = new MoniCAApplication();
		application.setMonicaServer(monicaserver, new Properties());

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
		router.attach("/point/{points}", PointResource.class);
	    router.attach("/points", PointResource.class);
	    router.attach("/names", PointNameResource.class);
	    router.attach("/description/{points}", PointDescriptionResource.class);
	    router.attach("/descriptions", PointDescriptionResource.class);
	    
	    logger.log(Level.INFO, "Resource binding finished");
		// Return the root router
		return router;
	}

}