package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Skins;

import org.bukkit.configuration.ConfigurationSection;

public class VehicleSkin {
	private String id;
	private String name;
	private String model;
	
	public VehicleSkin(String key, ConfigurationSection config) {
		id = key;
		name = config.getString("name", key);
		model = config.getString("model");
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getModel() {
		return model;
	}
	
	
}
