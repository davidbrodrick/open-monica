/*
 * Copyright (c) 2012 CSIRO Astronomy and Space Science (CASS), Commonwealth
 * Scientific and Industrial Research Organisation (CSIRO) PO Box 76, Epping NSW 1710,
 * Australia atnf-enquiries@csiro.au
 * 
 * MoniCA is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either 
 * version 2 of the License, or (at your option) any later version.
 */

package cass.monica.rest;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.comms.MoniCAClient;
import atnf.atoms.time.AbsTime;

import cass.monica.rest.json.MonitorPointList;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Container class for a MoniCA request, including the logic to talk to server
 * and format the results as JSON.
 * 
 * Only some of the fields need be populated, depending on what 'type' of
 * request is made.
 * 
 * @author David Brodrick
 */
public class MoniCARequest {
	private static Logger logger = Logger.getLogger(MoniCARequest.class.getName());

	
	public static final String GET = "get";
	public static final String SET = "set";
	public static final String BETWEEN = "between";
	public static final String BEFORE = "before";
	public static final String AFTER = "after";

	@SerializedName("type")
	public String itsRequestType;

	@SerializedName("start")
	public AbsTime itsStartTime;

	@SerializedName("end")
	public AbsTime itsEndTime;

	@SerializedName("time")
	public AbsTime itsTime;

	@SerializedName("points")
	public String[] itsPointNames;

	public volatile String itsCallback;

	/** Return the JSON representation of this request. */
	public String toString() {
		return MoniCAApplication.getGson().toJson(this);
	}

	/** Complete this request on the given server and return the JSON result. */
	public Representation completeRequest(MoniCAClient client) {
		Vector<PointData> resdata = null;
		try {
			if (itsRequestType.equalsIgnoreCase(GET)) {
				if (itsPointNames != null && itsPointNames.length > 0) {
					resdata = client.getData(new Vector<String>(Arrays
							.asList(itsPointNames)));
				}
			} else if (itsRequestType.equalsIgnoreCase(BETWEEN)) {
				if (itsPointNames != null && itsPointNames.length == 1
						&& itsStartTime != null && itsEndTime != null) {
					resdata = client.getArchiveData(itsPointNames[0],
							itsStartTime, itsEndTime);
					if (resdata != null) {
						for (PointData pd : resdata) {
							pd.setName(null);
						}
					}
				}
			} else if (itsRequestType.equalsIgnoreCase(AFTER)) {
				if (itsPointNames != null && itsPointNames.length > 0
						&& itsTime != null) {
					resdata = client.getAfter(
							new Vector<String>(Arrays.asList(itsPointNames)),
							itsTime);
				}
			} else if (itsRequestType.equalsIgnoreCase(BEFORE)) {
				if (itsPointNames != null && itsPointNames.length > 0
						&& itsTime != null) {
					resdata = client.getBefore(
							new Vector<String>(Arrays.asList(itsPointNames)),
							itsTime);
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not get point value", e);
		}

		return new StringRepresentation(asJSON(resdata),
				MediaType.APPLICATION_JAVASCRIPT);
	}

	/** Return a JSON representation of the data, including status: ok. */
	private String asJSON(Vector<PointData> data) {
		
		MonitorPointList pointList = new MonitorPointList();
		
		if (data == null) {
			pointList.setStatus("fail");
		}

		Gson gson = MoniCAApplication.getGson();
		String datastr = "";
		int numvalid = 0;
		for (int i = 0; i < data.size(); i++) {
			PointData pd = data.get(i);
			if (pd.isValid()) {
				if (numvalid > 0) {
					datastr += ", ";
				}
				datastr += gson.toJson(pd);
				numvalid++;
			}
		}

		if (numvalid > 0) {
			pointList.setStatus("ok");
			pointList.setPointData(data);
		} else {
			pointList.setStatus("fail");
		}

		return MoniCAApplication.getGson().toJson(pointList);
	}
}
