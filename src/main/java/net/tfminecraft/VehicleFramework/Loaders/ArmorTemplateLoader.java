package net.tfminecraft.VehicleFramework.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Data.DamageData;
import net.tfminecraft.VehicleFramework.Util.ConfigMerger;

public class ArmorTemplateLoader {

	public static HashMap<String, Map<String, Object>> armor = new HashMap<>();
	public static HashMap<String, Map<String, Object>> roles = new HashMap<>();

	public void clear() {
		armor.clear();
		roles.clear();
	}

	public void loadArmorFolder(File folder) {
		loadFolder(folder, armor, "armor");
	}

	public void loadRoleFolder(File folder) {
		loadFolder(folder, roles, "role");
	}

	public void loadFolder(File folder, HashMap<String, Map<String, Object>> target, String kind) {
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
			load(file, target, kind);
		}
	}

	public void load(File configFile, HashMap<String, Map<String, Object>> target, String kind) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			return;
		}
		for (String key : config.getKeys(false)) {
			if (target.containsKey(key)) {
				VFLogger.log("Duplicate " + kind + " template id '" + key + "' in " + configFile.getName() + ", keeping the first");
				continue;
			}
			if (!config.isConfigurationSection(key)) {
				VFLogger.log(kind + " template '" + key + "' in " + configFile.getName() + " is not a section, skipping");
				continue;
			}
			target.put(key, ConfigMerger.fromSection(config.getConfigurationSection(key)));
		}
	}

	public static Map<String, Object> mergeLayers(
			Map<String, Object> armorMap,
			Map<String, Object> roleMap,
			Map<String, Object> overlay) {
		Map<String, Object> merged = overlayOrEmpty(null, armorMap);
		merged = overlayOrEmpty(merged, roleMap);
		return overlayOrEmpty(merged, overlay);
	}

	public static DamageData resolve(String componentId, ConfigurationSection config) {
		if (config == null) {
			return new DamageData(List.of());
		}
		String armorId = config.getString("armor");
		String roleId = config.getString("role");
		boolean useTemplates = (armorId != null && !armorId.isBlank()) || (roleId != null && !roleId.isBlank());
		if (useTemplates) {
			Map<String, Object> armorMap = lookup(armor, armorId, componentId, "armor");
			Map<String, Object> roleMap = lookup(roles, roleId, componentId, "role");
			Map<String, Object> overlay = damageOverlay(config);
			return new DamageData(mergeLayers(armorMap, roleMap, overlay));
		}
		if (config.isConfigurationSection("damage")) {
			return new DamageData(ConfigMerger.fromSection(config.getConfigurationSection("damage")));
		}
		return new DamageData(config.getStringList("damage"));
	}

	public static Map<String, Object> damageOverlay(ConfigurationSection config) {
		if (config == null) {
			return null;
		}
		if (config.isConfigurationSection("damage")) {
			return ConfigMerger.fromSection(config.getConfigurationSection("damage"));
		}
		if (!config.isList("damage")) {
			return null;
		}
		return listToMap(config.getStringList("damage"));
	}

	public static Map<String, Object> listToMap(List<String> list) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (list == null) {
			return map;
		}
		for (String s : list) {
			String type = s.split("\\(")[0];
			Double damage = Double.parseDouble(s.split("\\(")[1].replace(")", ""));
			map.put(type, damage);
		}
		return map;
	}

	private static Map<String, Object> lookup(
			HashMap<String, Map<String, Object>> source,
			String id,
			String componentId,
			String kind) {
		if (id == null || id.isBlank()) {
			return null;
		}
		Map<String, Object> found = source.get(id);
		if (found == null) {
			VFLogger.log("Component " + componentId + " references unknown " + kind + " '" + id + "', skipping");
		}
		return found;
	}

	private static Map<String, Object> overlayOrEmpty(Map<String, Object> base, Map<String, Object> overlay) {
		if (base == null && overlay == null) {
			return new LinkedHashMap<>();
		}
		if (base == null) {
			return ConfigMerger.overlay(new LinkedHashMap<>(), overlay);
		}
		if (overlay == null) {
			return ConfigMerger.overlay(base, new LinkedHashMap<>());
		}
		return ConfigMerger.overlay(base, overlay);
	}
}
