package net.tfminecraft.VehicleFramework.Vehicles.Component.Gear;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Data.TimedSound;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;

public class Gear {
	private String name;
	private SoundData engineSound;
	private TimedSound accelerationSound;
	
	private Throttle throttle;
	
	private int acceleration;
	
	private double speed;
	
	public Gear(VehicleComponent c, ConfigurationSection config) {
		name = config.getString("name", "Gear");
		engineSound = new SoundData(config.getConfigurationSection("engine-sound"));
		accelerationSound = new TimedSound(config.getConfigurationSection("accelerate-sound"));
		throttle = new Throttle(config.getString("throttle-alias", "Throttle"), config.getInt("max", 100), config.getInt("min", 0), c);
		acceleration = config.getInt("acceleration", 1);
		speed = config.getDouble("speed", 0.3);
	}
	
	public Gear(VehicleComponent c, Gear another) {
		name = another.getName();
		engineSound = another.getEngineSound();
		accelerationSound = new TimedSound(another.getAccelerationSound());
		throttle = new Throttle(another.getThrottle().getName(), another.getThrottle().getMax(), another.getThrottle().getMin(), c);
		acceleration = another.getAcceleration();
		speed = another.getBaseSpeed();
	}

	public String getName() {
		return name;
	}

	public SoundData getEngineSound() {
		return engineSound;
	}

	public TimedSound getAccelerationSound() {
		return accelerationSound;
	}

	public Throttle getThrottle() {
		return throttle;
	}

	public int getAcceleration() {
		return acceleration;
	}

	public double getBaseSpeed() {
		return speed;
	}
	
	public double getSpeed() {
		return speed*(throttle.getCurrent()/100.0);
	}
}
