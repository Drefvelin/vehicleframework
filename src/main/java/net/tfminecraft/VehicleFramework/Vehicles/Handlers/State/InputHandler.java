package net.tfminecraft.VehicleFramework.Vehicles.Handlers.State;

import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.Keybind;

public class InputHandler {
	private HashMap<Keybind, Input> keybinds = new HashMap<>();
	
	public InputHandler(ConfigurationSection config) {
		for (Keybind keybind : Keybind.values()) {
	        String enumString = keybind.name();
	        if (config.contains(enumString)) {
	        	if(Input.valueOf(config.getString(enumString).toUpperCase()) != null) {
	        		keybinds.put(keybind, Input.valueOf(config.getString(enumString).toUpperCase()));
	        	}
	        } else {
	        	keybinds.put(keybind, Input.NONE);
	        }
	    }
	}
	
	public InputHandler() {
		for (Keybind keybind : Keybind.values()) {
			keybinds.put(keybind, Input.NONE);
	    }
		keybinds.put(Keybind.SHIFT_LEFT_CLICK, Input.SEAT_SELECTION);
	}
	
	public InputHandler(InputHandler another) {
		for (Keybind keybind : another.keybinds.keySet()) {
	        keybinds.put(keybind, another.keybinds.get(keybind));
	    }
	}
	
	public Input getInput(Keybind key) {
		return keybinds.get(key);
	}
	
	public boolean containsInput(Input i) {
		return keybinds.containsValue(i);
	}
}
