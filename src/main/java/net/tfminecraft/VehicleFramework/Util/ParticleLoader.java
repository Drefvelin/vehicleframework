package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Data.ParticleData;

public class ParticleLoader {
	public static List<ParticleData> getParticlesFromConfig(ConfigurationSection config){
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		List<ParticleData> temp = new ArrayList<ParticleData>();
		for(String key : list) {
			temp.add(new ParticleData(config.getConfigurationSection(key)));
		}
		return temp;
	}
}
