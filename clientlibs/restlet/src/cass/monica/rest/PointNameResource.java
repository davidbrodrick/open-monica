package cass.monica.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import cass.monica.rest.json.MonicaPointNameList;

import com.google.gson.Gson;

/**
 * Resource for getting the list of point names.
 * 
 * @author Mark Wieringa
 */
public class PointNameResource extends ServerResource {
	private static Logger logger = Logger.getLogger(PointNameResource.class.getName());

	public Gson getGson() {
		return MoniCAApplication.getGson();
	}

	@Get
	public String getPointNames() {
		return getAllNames();
	}

	@Post
	public String servicePost(String entity) {
		return getAllNames();
	}

	private String getAllNames() {
		MonicaPointNameList nameList = new MonicaPointNameList();
		String itsPointNames[] = new String[0];
		try {
			itsPointNames = ((MoniCAApplication)getApplication()).getClient().getAllPointNames();
			nameList.setStatus("ok");
			nameList.setMonitoringPointNames(itsPointNames);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not get names", e);
			nameList.setStatus("fail");
			nameList.setErrorMsg(e.getMessage());
		}
		
		return getGson().toJson(nameList);
	}

}
