package net.tfminecraft.VehicleFramework.Vehicles.Util;

public class Fire {
	private int progress;
	
	public Fire() {
		progress = (int) Math.round(Math.random()*7);
		if(progress < 1) progress = 1;
	}
	
	public void setProgress(int a) {
		progress = a;
		if(progress > 100) progress = 100;
		if(progress < 0) progress = 0;
	}
	
	public boolean engulfed() {
		if(progress == 100) return true;
		return false;
	}
	
	public int getProgress() {
		return progress;
	}
	
	public void tick() {
		progress += (int) Math.round(Math.random()*11);
		if(progress > 100) progress = 100;
	}
	
	public void fight() {
		progress -= (int) Math.round(Math.random()*3);
		if(progress < 0) progress = 0;
	}
	
	public String getFireString() {
		String info = "§eSmall Fire: §6";
		if(progress > 70) info = "§4Massive Fire!!: §6";
		else if(progress > 40) info = "§cWidespread Fire!: §6";

		return info+progress+"%";
	}
}
