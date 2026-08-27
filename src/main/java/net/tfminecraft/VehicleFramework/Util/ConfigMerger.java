package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigMerger {

	public static final String TEMPLATE_KEY = "template";

	private ConfigMerger() {
	}

	public static Map<String, Object> fromSection(ConfigurationSection section) {
		Map<String, Object> result = new LinkedHashMap<>();
		if (section == null) {
			return result;
		}
		for (String key : section.getKeys(false)) {
			if (section.isConfigurationSection(key)) {
				result.put(key, fromSection(section.getConfigurationSection(key)));
			} else {
				result.put(key, copyValue(section.get(key)));
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> overlay(Map<String, Object> base, Map<String, Object> overlay) {
		Map<String, Object> result = copyMap(base);
		if (overlay == null) {
			return result;
		}
		for (Map.Entry<String, Object> entry : overlay.entrySet()) {
			if (TEMPLATE_KEY.equals(entry.getKey())) {
				continue;
			}
			Object overlayValue = entry.getValue();
			Object existing = result.get(entry.getKey());
			if (existing instanceof Map && overlayValue instanceof Map) {
				result.put(
						entry.getKey(),
						overlay((Map<String, Object>) existing, (Map<String, Object>) overlayValue));
			} else {
				result.put(entry.getKey(), copyValue(overlayValue));
			}
		}
		return result;
	}

	public static YamlConfiguration toConfiguration(Map<String, Object> values) {
		YamlConfiguration config = new YamlConfiguration();
		writeDotted(config, "", values);
		return config;
	}

	@SuppressWarnings("unchecked")
	private static void writeDotted(YamlConfiguration config, String prefix, Map<String, Object> values) {
		if (values == null) {
			return;
		}
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				writeDotted(config, path, (Map<String, Object>) value);
			} else {
				config.set(path, value);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> copyMap(Map<String, Object> source) {
		Map<String, Object> copy = new LinkedHashMap<>();
		if (source == null) {
			return copy;
		}
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			copy.put(entry.getKey(), copyValue(entry.getValue()));
		}
		return copy;
	}

	@SuppressWarnings("unchecked")
	private static Object copyValue(Object value) {
		if (value instanceof Map) {
			return copyMap((Map<String, Object>) value);
		}
		if (value instanceof List) {
			List<Object> copy = new ArrayList<>();
			for (Object item : (List<?>) value) {
				copy.add(copyValue(item));
			}
			return copy;
		}
		return value;
	}
}
