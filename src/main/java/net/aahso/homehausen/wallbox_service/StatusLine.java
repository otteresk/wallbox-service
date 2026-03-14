package net.aahso.homehausen.wallbox_service;


public class StatusLine {

	public static final String TYPE_INFO = "INFO";
	public static final String TYPE_FAILED = "FAILED";
	public static final String TYPE_SUCCESS = "SUCCESS";

	private long timeStamp;
	private String statusType;
	private String statusMsg;

	
	public StatusLine(long timeStamp, String statusType, String status){
		this.timeStamp = timeStamp;
		this.statusType = statusType;
		this.statusMsg = status;
	}

	public long getTimeStamp() {
		return timeStamp;
	}	

	public String getStatusType() {
		return statusType;
	}

	public String getStatusMsg() {
		return statusMsg;
	}

}
