package net.tfminecraft.VehicleFramework.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Util.ConfigMerger;

public class WeaponTemplateLoader {

	public static HashMap<String, Map<String, Object>> map = new HashMap<>();

	public void clear() {
		map.clear();
	}

	public void loadFolder(File folder) {
		if (folder == null || !folder.isDirectory()) {
			return;
		}
		File[] files = folder.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				continue;
			}
			String name = file.getName().toLowerCase();
			if (!name.endsWith(".yml") && !name.endsWith(".yaml")) {
				continue;
			}
			load(file);
		}
	}

	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			return;
		}
		for (String key : config.getKeys(false)) {
			if (map.containsKey(key)) {
				VFLogger.log("Duplicate weapon template id '" + key + "' in " + configFile.getName() + ", keeping the first");
				continue;
			}
			if (!config.isConfigurationSection(key)) {
				VFLogger.log("Weapon template '" + key + "' in " + configFile.getName() + " is not a section, skipping");
				continue;
			}
			map.put(key, ConfigMerger.fromSection(config.getConfigurationSection(key)));
		}
	}

	public static Map<String, Object> getByString(String id) {
		if (id == null) {
			return null;
		}
		return map.get(id);
	}

	public static ConfigurationSection resolve(String weaponId, ConfigurationSection instance) {
		if (instance == null) {
			return null;
		}
		String templateId = instance.getString(ConfigMerger.TEMPLATE_KEY);
		if (templateId == null || templateId.isBlank()) {
			return instance;
		}
		Map<String, Object> template = getByString(templateId);
		if (template == null) {
			VFLogger.log("Weapon " + weaponId + " references unknown template '" + templateId + "', skipping");
			return null;
		}
		Map<String, Object> overlay = ConfigMerger.fromSection(instance);
		return ConfigMerger.toConfiguration(ConfigMerger.overlay(template, overlay));
	}
}
