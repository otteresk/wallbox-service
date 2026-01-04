package net.aahso.homehausen.wallbox_service;

public class WallBoxState {

	private long timeStamp;
	private String wallboxState;
	private long sinceTimeStamp;

	
	public WallBoxState(long timeStamp, String state){
		this.timeStamp = timeStamp;
		this.wallboxState = state;
		this.sinceTimeStamp = timeStamp;
	}

	public WallBoxState(long timeStamp, String state, long since){
		this.timeStamp = timeStamp;
		this.wallboxState = state;
		this.sinceTimeStamp = since;
	}

	public long getTimeStamp() {
		return timeStamp;
	}	

	public void setTimeStamp(long newTimeStamp) {
		this.timeStamp = newTimeStamp;
	}

	public String getState() {
		return wallboxState;
	}	

	public long getSinceTimeStamp() {
		return sinceTimeStamp;
	}	

}
