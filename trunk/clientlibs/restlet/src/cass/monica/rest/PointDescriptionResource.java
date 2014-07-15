package cass.monica.rest;

import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.Form;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import atnf.atoms.mon.PointDescription;
import cass.monica.rest.json.MonicaPointDescriptionList;

import com.google.gson.Gson;

/**
 * Resource for getting point descriptions.
 * 
 * @author Mark Wieringa
 * @author David Brodrick
 * @author Xinyu Wu
 */
public class PointDescriptionResource extends ServerResource {
	private static Logger logger = Logger
			.getLogger(PointDescriptionResource.class.getName());

	@Override
	public MoniCAApplication getApplication() {
		return (MoniCAApplication) super.getApplication();
	}

	public Gson getGson() {
		return MoniCAApplication.getGson();
	}

	/** Handle the GET request, with or without a point name specified. */
	@Get
	public String getDescription() {
		Vector<String> pointNames = new Vector<String>(1);
		if (getRequestAttributes().get("points") != null) {
			pointNames.add((String) getRequestAttributes().get("points"));
		}

		return doQuery(pointNames);
	}

	/** Trivial class for the point names request structure. */
	private class PointNames {
		String[] points;
	}

	/** Handle the POST request. */
	@Post("json")
	public String getDescriptions(String arg) {
		// Parse the request
		Vector<String> pointNames = new Vector<String>(1);
		PointNames p = getGson().fromJson(arg, PointNames.class);
		if (p.points != null && p.points.length > 0) {
			// Get the requested point names as a vector
			pointNames = new Vector<String>(Arrays.asList(p.points));
		}
		return doQuery(pointNames);
	}

	@Post("form")
	public String servicePost(String entity) {
		Form form = new Form(entity);

		Map<String, String> valuesMap = form.getValuesMap();

		String names = valuesMap.get("points");

		Vector<String> pointNames = new Vector<String>(1);

		// semi colon seperated list of points
		if (names != null && names.trim().length() > 0) {
			StringTokenizer tokenizer = new StringTokenizer(names, ";");
			while (tokenizer.hasMoreTokens()) {
				pointNames.add(tokenizer.nextToken());
			}

		}
		return doQuery(pointNames);
	}

	/** Contact the server to handle the request. */
	private String doQuery(Vector<String> itsPointNames) {
		Vector<PointDescription> itsPoints;
		MonicaPointDescriptionList descriptionList = new MonicaPointDescriptionList();
		try {
			if (itsPointNames == null || itsPointNames.size() == 0) {
				// Get all points
				itsPoints = getApplication().getClient().getAllPoints();
			} else {
				// Specified point names were provided
				itsPoints = getApplication().getClient().getPoints(itsPointNames);
			}
			descriptionList.setStatus("ok");
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could get point descriptions", e);
			itsPoints = null;
			descriptionList.setStatus("fail");
			descriptionList.setError(e.getMessage());
			
		}

		descriptionList.setMonitoringPointNames(itsPoints.toArray(new PointDescription[]{}));		
		return MoniCAApplication.getGson().toJson(descriptionList);		
	}
}
