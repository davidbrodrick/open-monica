package cass.monica.rest.json;


public class MonicaPointNameList {
	
	String monitoringPointNames[] = new String[0];
	String status = "";
	String errorMsg = "";

	public MonicaPointNameList() {
		
	}

	public MonicaPointNameList(String points[]) {
		monitoringPointNames = points;
	}

	
	public String[] getMonitoringPointNames() {
		return monitoringPointNames;
	}

	public void setMonitoringPointNames(String[] monitoringPointNames) {
		this.monitoringPointNames = monitoringPointNames;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String error) {
		this.errorMsg = error;
	}
	
	
}
