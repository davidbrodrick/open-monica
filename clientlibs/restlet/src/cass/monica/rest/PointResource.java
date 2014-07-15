package cass.monica.rest;

//import org.restlet.Application;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import atnf.atoms.time.AbsTime;

public class PointResource extends ServerResource {
	private static Logger logger = Logger.getLogger(PointResource.class
			.getName());

	@Override
	public MoniCAApplication getApplication() {
		return (MoniCAApplication) super.getApplication();
	}

	@Get
	public Representation processRequest() {
		// Parse the request parameters
		MoniCARequest req = formToMonicaRequest(getQuery());
		// Get the point name
		if (getRequestAttributes().get("name") != null) {
			req.itsPointNames = new String[] { (String) getRequestAttributes()
					.get("name") };
		} else if (getQuery().getFirst("points") != null) {

			String names = getQuery().getFirst("points").getValue();
			Vector<String> pointNames = new Vector<String>(1);
			// semi colon seperated list of points
			if (names != null && names.trim().length() > 0) {
				StringTokenizer tokenizer = new StringTokenizer(names, ";");
				while (tokenizer.hasMoreTokens()) {
					pointNames.add(tokenizer.nextToken());
				}

			}
			req.itsPointNames = pointNames.toArray(new String[] {});
		}
		return req.completeRequest(getApplication().getClient());
	}

	@Post("json")
	public Representation acceptJson(String arg) {
		try {
			MoniCARequest req = MoniCAApplication.getGson().fromJson(arg,
					MoniCARequest.class);
			return req.completeRequest(getApplication().getClient());
		} catch (Exception e) {
			logger.log(Level.WARNING, "acceptJson: Got exception: " + e);
		}
		return null;
	}

	@Post("form")
	public Representation servicePost(String entity) {
		Form form = new Form(entity);

		// Parse the request parameters
		MoniCARequest req = formToMonicaRequest(form);
		// Get the point name
		if (form.getFirst("points") != null) {

			String names = form.getFirst("points").getValue();
			Vector<String> pointNames = new Vector<String>(1);
			// semi colon seperated list of points
			if (names != null && names.trim().length() > 0) {
				StringTokenizer tokenizer = new StringTokenizer(names, ";");
				while (tokenizer.hasMoreTokens()) {
					pointNames.add(tokenizer.nextToken());
				}

			}
			req.itsPointNames = pointNames.toArray(new String[] {});
		}

		return req.completeRequest(getApplication().getClient());
	}

	private MoniCARequest formToMonicaRequest(Form form) {
		MoniCARequest req = new MoniCARequest();
		if (form.getFirst("start") != null && form.getFirst("end") != null) {
			req.itsRequestType = MoniCARequest.BETWEEN;
			req.itsStartTime = AbsTime.factory(form.getFirst("start")
					.getValue());
			req.itsEndTime = AbsTime.factory(form.getFirst("end").getValue());
		} else if (form.getFirst("before") != null) {
			req.itsRequestType = MoniCARequest.BEFORE;
			req.itsTime = AbsTime.factory(form.getFirst("before").getValue());
		} else if (form.getFirst("after") != null) {
			req.itsRequestType = MoniCARequest.AFTER;
			req.itsTime = AbsTime.factory(form.getFirst("after").getValue());
		} else {
			// Default get operation
			req.itsRequestType = MoniCARequest.GET;
		}
		return req;
	}

}
