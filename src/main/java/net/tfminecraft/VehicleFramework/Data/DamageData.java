package net.tfminecraft.VehicleFramework.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageData {
	private HashMap<String, Double> modifiers = new HashMap<>();
	
	public DamageData(List<String> list) {
		if (list == null) {
			return;
		}
		for(String s : list) {
			String type = s.split("\\(")[0].toUpperCase();
			Double damage = Double.parseDouble(s.split("\\(")[1].replace(")", ""));
			modifiers.put(type, damage);
		}
	}

	public DamageData(Map<String, Object> map) {
		if (map == null) {
			return;
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			modifiers.put(entry.getKey().toUpperCase(), toDouble(entry.getValue()));
		}
	}

	public HashMap<String, Double> getModifiers() {
		return modifiers;
	}
	
	public boolean hasModifier(String cause) {
		return modifiers.containsKey(cause);
	}
	
	public double getModifier(String cause) {
		return modifiers.get(cause);
	}

	private static double toDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		return Double.parseDouble(value.toString());
	}
}
