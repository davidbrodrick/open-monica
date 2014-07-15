package cass.monica.rest.json;

import atnf.atoms.mon.PointDescription;


public class MonicaPointDescriptionList {
	
	PointDescription monitoringPointDescriptions[] = new PointDescription[0];
	String status = "";
	String error = "";

	public MonicaPointDescriptionList() {
		
	}

	public MonicaPointDescriptionList(PointDescription points[]) {
		monitoringPointDescriptions = points;
	}

	
	public PointDescription[] getMonitoringPointNames() {
		return monitoringPointDescriptions;
	}

	public void setMonitoringPointNames(PointDescription[] monitoringPointNames) {
		this.monitoringPointDescriptions = monitoringPointNames;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	
}
