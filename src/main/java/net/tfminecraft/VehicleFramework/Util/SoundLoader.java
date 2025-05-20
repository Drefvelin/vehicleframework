package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Data.SoundData;

public class SoundLoader {
	public static List<SoundData> getSoundsFromConfig(ConfigurationSection config){
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		List<SoundData> temp = new ArrayList<SoundData>();
		for(String key : list) {
			temp.add(new SoundData(config.getConfigurationSection(key)));
		}
		return temp;
	}
}
