package net.tfminecraft.VehicleFramework.Weapons.Ammunition;

import org.bukkit.configuration.ConfigurationSection;

public class Bullet extends Ammunition{
	
	private double range;
	
	private boolean explosive;
	
	public Bullet (String key, ConfigurationSection config) {
		super(key, config);
		range = config.getDouble("range", 80.0);
		explosive = config.getBoolean("explosive", false);
	}
	
	public double getRange() {
		return range;
	}

	public boolean isExplosive() {
		return explosive;
	}
	
}
