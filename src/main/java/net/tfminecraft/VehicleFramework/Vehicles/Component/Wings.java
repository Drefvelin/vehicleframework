package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Wings extends VehicleComponent{
	private double lift;
	private float turnRate;
	
	public Wings(ConfigurationSection config) {
		super(Component.WINGS, config);
		lift = config.getDouble("lift", 0.5);
		turnRate = (float) config.getDouble("turn-rate", 0.2);
	}
	public Wings(ActiveVehicle v, Wings another, ActiveModel m, IncompleteComponent ic) {
		super(another, v, m, ic);
		this.v = v;
		lift = another.getBaseLift();
		turnRate = another.getBaseTurnRate();
	}

	@Override
	public void slowTick(List<Player> nearby) {
		super.slowTick(nearby);
	}
	
	public float getBaseTurnRate() {
		return turnRate;
	}
	
	public float getTurnRate() {
		return (float) (turnRate*v.getSpeed());
	}
	
	public double getBaseLift() {
		return lift;
	}
	
	public double getLift() {
		return lift*(healthData.getHealthPercentage()/100.0);
	}
}
