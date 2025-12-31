package net.aahso.homehausen.wallbox_service;

public class WallBoxState {

	private long timeStamp;
	private String wallboxState;
	
	public WallBoxState(long timeStamp, String state){
					
		this.timeStamp = timeStamp;
		this.wallboxState = state;
	}

	public long getTimeStamp() {
		return timeStamp;
	}	

	public String getState() {
		return wallboxState;
	}	

}
