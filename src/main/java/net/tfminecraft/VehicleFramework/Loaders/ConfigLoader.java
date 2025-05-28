package net.tfminecraft.VehicleFramework.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Cache.Cache;

public class ConfigLoader {

	public void load(File configFile) {
		VFLogger.info("Loading config...");
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        if(config.contains("ignore-explosion")) {
        	for(String s : config.getStringList("ignore-explosion")) {
				try {
					Cache.ignoreExplode.add(Material.valueOf(s.toUpperCase()));
				} catch (Exception e) {
					VFLogger.log(s+" is not a material");
				}
    		}
        }
        if(config.contains("ignore-landing")) {
			for(String s : config.getStringList("ignore-landing")) {
				try {
					Cache.ignoreLands.add(Material.valueOf(s.toUpperCase()));
				} catch (Exception e) {
					VFLogger.log(s+" is not a material");
				}
			}
        }
        
        if(config.contains("convert-explosion")) {
			for(String s : config.getStringList("convert-explosion")) {
				String from = s.split("\\.")[0];
				String to = s.split("\\.")[1];
				try {
					Cache.convertExplode.put(Material.valueOf(from.toUpperCase()), Material.valueOf(to.toUpperCase()));
				} catch (Exception e) {
					VFLogger.log("either "+from+" or "+s+" is not a material");
				}
			}
        }

		Cache.blockDamage = config.getBoolean("block-damage", true);
		
		Cache.despawnDistance = (int) Math.round(Math.pow(config.getInt("despawn-distance", 64), 2));

		Cache.skinItem = config.getString("skin-item", "v.bucket");
		Cache.repairItem = config.getString("repair-item", "v.iron_shovel");
	}
}
