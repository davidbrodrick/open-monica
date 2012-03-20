package cass.monica.rest;

//import org.restlet.Application;
import org.restlet.data.*;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import org.restlet.representation.Representation;
import org.restlet.resource.Options;

import com.google.gson.Gson;

import java.util.*;

import atnf.atoms.time.AbsTime;
import atnf.atoms.mon.PointData;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.mon.util.MonitorUtils;

public class PointResource extends ServerResource {
	private abstract class PointAction {
		public abstract Representation doAction();
	}

	private class GetAction extends PointAction {
		public Representation doAction() {
			PointData pointData;
			try {
				pointData = getClient().getData(pointName);
			} catch (Exception e) {
				System.err.println(e);
				pointData = null;
			}
			String out = "";
			if (pointData != null && pointData.isValid()) {
				String data = getGson().toJson(pointData);
				if (callback != "") {
					out += callback + "(" + data + ")";
				} else {
					out += data;
				}
			} else {
				out = "{'error': 'not a valid point.'}";
				if (callback != "") {
					out = callback + "(" + out + ")";
				}
			}
			return new StringRepresentation(out,
					MediaType.APPLICATION_JAVASCRIPT);
		}
	}

	private class BeforeAction extends PointAction {
		public Representation doAction() {
			AbsTime t = AbsTime.factory(getQuery().getFirst("before").getValue());
			PointData pointData;
			try {
				pointData = getClient().getBefore(pointName, t);
			} catch (Exception e) {
				System.err.println(e);
				pointData = null;
			}
			String out = "";
			if (pointData != null && pointData.isValid()) {
				String data = getGson().toJson(pointData);
				if (callback != "") {
					out += callback + "(" + data + ")";
				} else {
					out += data;
				}
			} else {
				out = "{'error': 'not a valid point.'}";
				if (callback != "") {
					out = callback + "(" + out + ")";
				}
			}
			return new StringRepresentation(out,
					MediaType.APPLICATION_JAVASCRIPT);
		}
	}	
	
	private class AfterAction extends PointAction {
		public Representation doAction() {
			AbsTime t = AbsTime.factory(getQuery().getFirst("after").getValue());
			PointData pointData;
			try {
				pointData = getClient().getBefore(pointName, t);
			} catch (Exception e) {
				System.err.println(e);
				pointData = null;
			}
			String out = "";
			if (pointData != null && pointData.isValid()) {
				String data = getGson().toJson(pointData);
				if (callback != "") {
					out += callback + "(" + data + ")";
				} else {
					out += data;
				}
			} else {
				out = "{'error': 'not a valid point.'}";
				if (callback != "") {
					out = callback + "(" + out + ")";
				}
			}
			return new StringRepresentation(out,
					MediaType.APPLICATION_JAVASCRIPT);
		}
	}
	
	private class SetAction extends PointAction {
		public Representation doAction() {
			String typecode = getQuery().getFirst("type").getValue();
			String valcode = getQuery().getFirst("val").getValue();
			Object newval = MonitorUtils.parseFixedValue(typecode, valcode);

			PointData pointData = new PointData(pointName, newval);
			Boolean setres = new Boolean(false);
			try {
				setres = getClient().setData(pointName, pointData, "user",
						"pass");
			} catch (Exception e) {
				System.err.println(e);
			}

			String out = "";
			String data = getGson().toJson(setres);
			if (callback != "") {
				out += callback + "(" + data + ")";
			} else {
				out += data;
			}
			return new StringRepresentation(out,
					MediaType.APPLICATION_JAVASCRIPT);
		}
	}

	private class ArchiveAction extends PointAction {
		public Representation doAction() {
			AbsTime start = AbsTime.factory(getQuery().getFirst("start")
					.getValue());
			AbsTime end = AbsTime
					.factory(getQuery().getFirst("end").getValue());

			Vector<PointData> resdata = null;
			try {
				resdata = getClient().getArchiveData(pointName, start, end);
			} catch (Exception e) {
				System.err.println(e);
			}
			String out = "";
			if (resdata != null) {
				HashMap<String, Vector<PointData>> tempres = new HashMap<String, Vector<PointData>>(1);
				tempres.put("data", resdata);
				String data = getGson().toJson(tempres);
				if (callback != "") {
					out += callback + "(" + data + ")";
				} else {
					out += data;
				}
			} else {
				out = "{'error': 'not a valid point?'}";
				if (callback != "") {
					out = callback + "(" + out + ")";
				}
			}
			return new StringRepresentation(out,
					MediaType.APPLICATION_JAVASCRIPT);
		}
	}

	private String pointName;

	private String callback = "";

	private PointAction itsAction;

	@Override
	public void doInit() {
		callback = this.getQuery().getFirstValue("callback", "");
		pointName = (String) getRequestAttributes().get("name");
		itsAction = actionFactory();
	}

	private PointAction actionFactory() {
		PointAction res;

		if (getQuery().getFirst("type") != null
				&& getQuery().getFirst("val") != null) {
			// Set operation
			res = new SetAction();
		} else if (getQuery().getFirst("start") != null
				&& getQuery().getFirst("end") != null) {
			// Archive query operation
			res = new ArchiveAction();
		} else if (getQuery().getFirst("before") != null) {
			// Preceding value query operation
			res = new BeforeAction();	
		} else if (getQuery().getFirst("after") != null) {
			// Following value query operation
			res = new AfterAction();	
		} else {
			// Default get operation
			res = new GetAction();
		}
		return res;
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
	public Representation processRequest() {
		return itsAction.doAction();
	}
}
