package net.tfminecraft.VehicleFramework.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.VehicleFramework.Vehicles.Fuel.Fuel;

public class FuelLoader implements LoaderInterface{
	public static HashMap<String, Fuel> map = new HashMap<>();
	
	public static HashMap<String, Fuel> get(){
		return map;
	}
	
	@Override
	public void load(File configFile) {
		
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			Fuel o = new Fuel(key, config.getConfigurationSection(key));
			map.put(key, o);
		}
	}

	public static Fuel getByString(String id) {
		if(map.containsKey(id)) return map.get(id);
		return null;
	}

    public static Fuel getByInput(String item) {
        for(Map.Entry<String, Fuel> entry : map.entrySet()) {
			if(entry.getValue().getItem().equalsIgnoreCase(item)) return entry.getValue();
		}
		return null;
    }

    public static boolean itemIsFuel(String item) {
        for(Map.Entry<String, Fuel> entry : map.entrySet()) {
			if(entry.getValue().getItem().equalsIgnoreCase(item)) return true;
		}
		return false;
    } 
}
