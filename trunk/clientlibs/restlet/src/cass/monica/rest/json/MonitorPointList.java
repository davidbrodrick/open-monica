package cass.monica.rest.json;

import java.util.Vector;

import atnf.atoms.mon.PointData;
import atnf.atoms.mon.PointDescription;

public class MonitorPointList {
	
	Vector<PointData> pointData = new Vector<PointData>();
	String status = "";
	String error = "";
	
	public MonitorPointList() {
		
	}
	
	public Vector<PointData> getPointData() {
		return pointData;
	}
	public void setPointData(Vector<PointData> pointData) {
		this.pointData = pointData;
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
