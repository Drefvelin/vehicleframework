package net.tfminecraft.VehicleFramework.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Bones.RotationTarget;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Util.SoundLoader;

public class DeathData {
	private VehicleDeath type;
	private int duration;
	
	private int fragments;
	
	private List<SoundData> sfx = new ArrayList<>();
	
	private List<DeathOverride> overrides = new ArrayList<>();
	
	public DeathData(VehicleDeath type, ConfigurationSection config) {
		this.type = type;
		duration = config.getInt("duration", 0);
		if(config.isConfigurationSection("sounds")) {
			ConfigurationSection soundConfig = config.getConfigurationSection("sounds");
			sfx = SoundLoader.getSoundsFromConfig(soundConfig);
		}
		fragments = config.getInt("fragments", 0);
		if(config.isConfigurationSection("overrides")) {
			Set<String> set = config.getConfigurationSection("overrides").getKeys(false);

			List<String> list = new ArrayList<String>(set);
			for(String key : list) {
				overrides.add(new DeathOverride(config.getConfigurationSection("overrides."+key)));
			}
		}
	}

	public VehicleDeath getType() {
		return type;
	}

	public int getDuration() {
		return duration;
	}
	
	public int getFragments() {
		return fragments;
	}

	public List<SoundData> getSfx() {
		return sfx;
	}
	
	public boolean hasOverrides() {
		return overrides.size() != 0;
	}
	
	public List<DeathOverride> getOverrides() {
		return overrides;
	}
}
