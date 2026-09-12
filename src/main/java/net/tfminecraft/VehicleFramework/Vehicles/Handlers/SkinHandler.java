package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import com.ticxo.modelengine.api.ModelEngineAPI;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Skins.VehicleSkin;

public class SkinHandler {
	private ActiveVehicle v;
	private VehicleSkin currentSkin;
	
	private HashMap<String, VehicleSkin> skins = new HashMap<>();

	public static boolean isModelAvailable(String modelId) {
		if (modelId == null || modelId.isBlank()) {
			return false;
		}
		return ModelEngineAPI.getBlueprint(modelId) != null;
	}

	public static boolean isModelAvailable(VehicleSkin skin) {
		return skin != null && isModelAvailable(skin.getModel());
	}

	public SkinHandler(String model, ConfigurationSection config) {
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		for(String key : list) {
			VehicleSkin skin = new VehicleSkin(key, config.getConfigurationSection(key));
			if (!isModelAvailable(skin)) {
				VFLogger.log("Skin '" + key + "' references missing ModelEngine model '" + skin.getModel() + "'");
			}
			skins.put(key, skin);
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
		if (!override && !isModelAvailable(skins.get(id))) return false;
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
		VehicleSkin skin = skins.get(id);
		if (!isModelAvailable(skin)) return null;
		currentSkin = skin;
		return currentSkin.getModel();
	}

	public VehicleSkin getCurrentSkin() {
		return currentSkin;
	}

	public HashMap<String, VehicleSkin> getSkins() {
		return skins;
	}
	
	
}
