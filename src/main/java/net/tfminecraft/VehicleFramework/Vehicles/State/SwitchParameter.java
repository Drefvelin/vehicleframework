package net.tfminecraft.VehicleFramework.Vehicles.State;

import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;

public class SwitchParameter {
	private HashMap<String, Parameter> parameters = new HashMap<>();
	
	public SwitchParameter(ConfigurationSection config) {
		if(config.isConfigurationSection("velocity")) {
			ConfigurationSection v = config.getConfigurationSection("velocity");
			if(v.contains("x")) parameters.put("vX", new Parameter(v.getString("x")));
			if(v.contains("y")) parameters.put("vY", new Parameter(v.getString("y")));
			if(v.contains("z")) parameters.put("vZ", new Parameter(v.getString("z")));
		}
		if(config.isConfigurationSection("rotations")) {
			ConfigurationSection r = config.getConfigurationSection("rotations");
			if(r.contains("yaw")) parameters.put("yaw", new Parameter(r.getString("yaw")));
			if(r.contains("pitch")) parameters.put("pitch", new Parameter(r.getString("pitch")));
			if(r.contains("roll")) parameters.put("roll", new Parameter(r.getString("roll")));
		}
	}

	public HashMap<String, Parameter> getParameters() {
		return parameters;
	}
	
}
