package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Hull extends VehicleComponent{
	
	public Hull(ConfigurationSection config) {
		super(Component.HULL, config);
	}
	public Hull(Hull another, ActiveVehicle v, ActiveModel m, IncompleteComponent ic) {
		super(another, v, m, ic);
	}

	@Override
	public void slowTick(List<Player> nearby) {
		super.slowTick(nearby);
	}
	
}
