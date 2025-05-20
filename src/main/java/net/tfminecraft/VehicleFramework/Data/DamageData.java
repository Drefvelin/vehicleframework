package net.tfminecraft.VehicleFramework.Data;

import java.util.HashMap;
import java.util.List;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class DamageData {
	private HashMap<String, Double> modifiers = new HashMap<>();
	
	public DamageData(List<String> list) {
		for(String s : list) {
			String type = s.split("\\(")[0].toUpperCase();
			Double damage = Double.parseDouble(s.split("\\(")[1].replace(")", ""));
			modifiers.put(type, damage);
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
}
