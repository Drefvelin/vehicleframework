package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Skins.VehicleSkin;

public class SkinHandler {
	private ActiveVehicle v;
	private VehicleSkin currentSkin;
	
	private HashMap<String, VehicleSkin> skins = new HashMap<>();

	public SkinHandler(String model, ConfigurationSection config) {
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		for(String key : list) {
			skins.put(key, new VehicleSkin(key, config.getConfigurationSection(key)));
		}
		currentSkin = skins.get(model);
	}
	
	public SkinHandler(ActiveVehicle vehicle, String skin, SkinHandler another) {
		v = vehicle;
		skins = another.getSkins();
		currentSkin = skins.get(skin);
	}
	
	public boolean canChangeSkin(String id, boolean override) {
		if(currentSkin.getId().equalsIgnoreCase(id) && !override) return false;
		if(!skins.containsKey(id)) return false;
		if(v.getSeatHandler().hasPassengers() && !override) return false;
		if(v.getStateHandler().getCurrentState().getType().equals(State.FLYING) && !override) return false;
		if(v.hasComponent(Component.ENGINE) && !override) {
			Engine e = (Engine) v.getComponent(Component.ENGINE);
			if(e.requiresStart() && e.isStarted()) return false;
			if(e.getThrottle().getCurrent() != 0) return false;
		}
		return true;
	}
	
	public String changeSkin(String id) {
		if(!skins.containsKey(id)) return null;
		currentSkin = skins.get(id);
		return currentSkin.getModel();
	}

	public VehicleSkin getCurrentSkin() {
		return currentSkin;
	}

	public HashMap<String, VehicleSkin> getSkins() {
		return skins;
	}
	
	
}
