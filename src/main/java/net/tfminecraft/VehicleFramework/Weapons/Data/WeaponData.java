package net.tfminecraft.VehicleFramework.Weapons.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Data.ParticleData;
import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Enums.SoundArg;
import net.tfminecraft.VehicleFramework.Util.SoundLoader;

public class WeaponData {
	
	private HashMap<SoundArg, List<SoundData>> sounds = new HashMap<>();
	private String reloadAnimation;
	private String shootAnimation;
	
	private List<ParticleData> particles = new ArrayList<>();
	
	private double velocity;
	
	public WeaponData(ConfigurationSection config) {
		if(config.isConfigurationSection("reload-sounds")) {
			ConfigurationSection soundConfig = config.getConfigurationSection("reload-sounds");
			sounds.put(SoundArg.RELOAD, SoundLoader.getSoundsFromConfig(soundConfig));
		} else {
			sounds.put(SoundArg.RELOAD, new ArrayList<SoundData>());
		}
		if(config.isConfigurationSection("reload-start-sounds")) {
			ConfigurationSection soundConfig = config.getConfigurationSection("reload-start-sounds");
			sounds.put(SoundArg.RELOAD_START, SoundLoader.getSoundsFromConfig(soundConfig));
		} else {
			sounds.put(SoundArg.RELOAD_START, new ArrayList<SoundData>());
		}
		if(config.isConfigurationSection("shoot-sounds")) {
			ConfigurationSection soundConfig = config.getConfigurationSection("shoot-sounds");
			sounds.put(SoundArg.SHOOT, SoundLoader.getSoundsFromConfig(soundConfig));
		} else {
			sounds.put(SoundArg.SHOOT, new ArrayList<SoundData>());
		}
		
		reloadAnimation = config.getString("reload-animation", "none");
		shootAnimation = config.getString("shoot-animation", "none");
		
		if(config.isConfigurationSection("particles")) {
			ConfigurationSection particleConfig = config.getConfigurationSection("particles");
			Set<String> set = particleConfig.getKeys(false);

			List<String> list = new ArrayList<String>(set);
			
			for(String key : list) {
				particles.add(new ParticleData(particleConfig.getConfigurationSection(key)));
			}
		}
		velocity = config.getDouble("velocity", 3.0);		
	}

	public List<SoundData> getSounds(SoundArg arg) {
		return sounds.get(arg);
	}

	public String getReloadAnimation() {
		return reloadAnimation;
	}

	public String getShootAnimation() {
		return shootAnimation;
	}

	public List<ParticleData> getParticles() {
		return particles;
	}
	
	public double getVelocity() {
		return velocity;
	}
	
	
}
