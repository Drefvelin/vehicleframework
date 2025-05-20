package net.tfminecraft.VehicleFramework.Database;

import net.tfminecraft.VehicleFramework.Enums.Component;

public class IncompleteComponent {
	
	private Component type;
	
	private double damage;
	
	private int fireProgress;
	private int sinkProgress;

	public IncompleteComponent(Component type, double damage, int fireProgress, int sinkProgress) {
		this.type = type;
		this.damage = damage;
		this.fireProgress = fireProgress;
		this.sinkProgress = sinkProgress;
	}

	public Component getType() {
		return type;
	}
	
	public boolean hasDamage() {
		return damage > 0;
	}
	public boolean hasFire() {
		return fireProgress > 0;
	}
	public boolean isSinking() {
		return sinkProgress > 0;
	}
	
	public double getDamage() {
		return damage;
	}

	public int getFireProgress() {
		return fireProgress;
	}

	public int getSinkProgress() {
		return sinkProgress;
	}
	
	
}
