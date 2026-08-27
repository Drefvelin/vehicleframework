package net.tfminecraft.VehicleFramework.Weapons.Ammunition;

import org.bukkit.configuration.ConfigurationSection;

public class Bullet extends Ammunition{

	public static final double DEFAULT_RANGE = 80.0;
	public static final double DEFAULT_SPEED = 7.0;
	public static final double DEFAULT_GRAVITY = -0.05;

	private double range;
	private double speed;
	private double gravity;
	
	public Bullet (String key, ConfigurationSection config) {
		super(key, config);
		range = config.getDouble("range", DEFAULT_RANGE);
		speed = config.getDouble("speed", DEFAULT_SPEED);
		gravity = config.getDouble("gravity", DEFAULT_GRAVITY);
	}
	
	public double getRange() {
		return range;
	}

	public double getSpeed() {
		return speed;
	}

	public double getGravity() {
		return gravity;
	}
	
}
