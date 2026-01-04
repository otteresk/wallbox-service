package net.aahso.homehausen.wallbox_service;

public class StatusLine {

	private long timeStamp;
	private String statusMsg;

	
	public StatusLine(long timeStamp, String status){
		this.timeStamp = timeStamp;
		this.statusMsg = status;
	}


	public long getTimeStamp() {
		return timeStamp;
	}	


	public String getStatusMsg() {
		return statusMsg;
	}

}
