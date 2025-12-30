package net.aahso.homehausen.wallbox_service;

public class DataPoint {

	private long timeStamp;
	private int fromPV;
	private int fromGrid;
	private int fromBat;
	private int useHome;
	private int levelBat;
	
	public DataPoint (long timeStamp,
					int PV,
					int grid,
					int power_bat,
					int home,
					int batSoC){
		this.timeStamp = timeStamp;
		this.fromPV = PV;
		this.fromGrid = grid;
		this.fromBat = power_bat;
		this.useHome = home;
		this.levelBat = batSoC;
	}

	public long getTimeStamp() {
		return timeStamp;
	}	

	public int getFromPV() {
		return fromPV;
	}	

	public int getFromGrid() {
		return fromGrid;
	}	

	public int getFromBat() {
		return fromBat;
	}	

	public int getUseHome() {
		return useHome;
	}

	public int getLevelBat() {
		return levelBat;
	}

}
