package net.tfminecraft.VehicleFramework.Data;

public class HealthData {
	private double health;
	private double damage;
	
	private int repairTime;
	private int activeRepairTime;
	
	public HealthData(double h, double d, int r) {
		health = h;
		repairTime = r;
		activeRepairTime = -1;
		damage = d;
	}
	
	public void tick() {
		if(isUnderRepair()) {
			activeRepairTime--;
			if(activeRepairTime == 0) {
				activeRepairTime = -1;
				repair();
			}
		}
	}

	public void setDamage(double d) {
		damage = d;
		if(damage > health) damage = health;
	}
	
	public double getHealth() {
		return health;
	}

	public double getDamage() {
		return damage;
	}
	
	public void damage(double a) {
		damage += a;
		if(damage > health) damage = health;
	}
	
	public void startRepair() {
		activeRepairTime = repairTime;
	}
	public void stopRepair() {
		activeRepairTime = -1;
	}
	private void repair() {
		int a = (int) Math.round(Math.random()*7)+7;
		damage -= a;
		if(damage < 0) damage = 0;
	}
	
	public int getBaseRepairTime() {
		return repairTime;
	}
	public int getRepairTime() {
		return activeRepairTime;
	}
	public boolean isUnderRepair() {
		return activeRepairTime != -1;
	}
	public String getRepairString() {
		double a = Math.round((activeRepairTime/20.0)*10.0)/10.0;
		return a+"s";
	}
	
	public int getHealthPercentage() {
		return (int) Math.round((1-damage/health)*100);
	}
	
	public String getHealthPercentageString() {
		int a = getHealthPercentage();
		String colour = "§4";
		if(a > 80) colour = "§2";
		else if(a > 60) colour = "§a";
		else if(a > 40) colour = "§e";
		else if(a > 20) colour = "§c";
		return "§fHealth: "+colour+a+"%";
	}
}
