package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Effects.CustomEffect;
import net.tfminecraft.VehicleFramework.Enums.CustomAction;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class EffectHandler {
	private HashMap<CustomAction, CustomEffect> effects = new HashMap<>();
	
	public EffectHandler(ConfigurationSection config) {
		for (CustomAction action : CustomAction.values()) {
	        String enumString = action.name().toLowerCase();
	        if (config.contains(enumString)) {
	        	effects.put(action, new CustomEffect(config.getStringList(enumString)));
	        } else {
	        	effects.put(action, new CustomEffect(new ArrayList<String>()));
	        }
	    }
	}
	
	public EffectHandler(EffectHandler another) {
		for (CustomAction action : CustomAction.values()) {
			effects.put(action, new CustomEffect(another.getEffect(action).getCommands()));
	    }
	}
	
	public EffectHandler() {
		for (CustomAction action : CustomAction.values()) {
			effects.put(action, new CustomEffect(new ArrayList<String>()));
	    }
	}
	
	public boolean hasEffect(CustomAction a) {
		return !effects.get(a).isEmpty();
	}
	
	public CustomEffect playEffect(List<Player> players, ActiveVehicle v, CustomAction a) {
		CustomEffect e = effects.get(a);
		if(!e.isEmpty()) e.play(players, v);
		return e;
	}
	
	public CustomEffect getEffect(CustomAction a) {
		return effects.get(a);
	}
}
