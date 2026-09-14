package net.tfminecraft.VehicleFramework.Vehicles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.WeaponHandler;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;

/**
 * Generic health decay for spawned and stored vehicles. Callers supply
 * fraction-of-max and remaining-health floor; this class does not know about upkeep.
 */
public final class VehicleHealthDecay {
	private VehicleHealthDecay() {}

	public interface MaxHealthLookup {
		double componentMaxHealth(String componentTypeKey);

		double weaponMaxHealth(String weaponId);
	}

	public static double nextDamage(
			double currentDamage,
			double maxHealth,
			double fractionOfMax,
			double minHealthFraction) {
		if (maxHealth <= 0.0) {
			return currentDamage;
		}
		double fraction = Math.max(0.0, fractionOfMax);
		double minRemaining = Math.max(0.0, Math.min(1.0, minHealthFraction));
		double maxAllowedDamage = maxHealth * (1.0 - minRemaining);
		double next = currentDamage + maxHealth * fraction;
		if (next > maxAllowedDamage) {
			next = maxAllowedDamage;
		}
		if (next < 0.0) {
			next = 0.0;
		}
		return next;
	}

	public static void applyToLive(
			ActiveVehicle vehicle,
			double fractionOfMax,
			double minHealthFraction) {
		if (vehicle == null) {
			return;
		}
		if (vehicle.getComponents() != null) {
			for (VehicleComponent component : vehicle.getComponents()) {
				if (component == null || component.getHealthData() == null) {
					continue;
				}
				var health = component.getHealthData();
				health.setDamage(nextDamage(
						health.getDamage(),
						health.getHealth(),
						fractionOfMax,
						minHealthFraction));
			}
		}
		WeaponHandler weapons = vehicle.getWeaponHandler();
		if (weapons != null && weapons.getWeapons() != null) {
			for (ActiveWeapon weapon : weapons.getWeapons()) {
				if (weapon == null || weapon.getHealthData() == null) {
					continue;
				}
				var health = weapon.getHealthData();
				health.setDamage(nextDamage(
						health.getDamage(),
						health.getHealth(),
						fractionOfMax,
						minHealthFraction));
			}
		}
	}

	public static boolean applyToJson(
			JsonObject root,
			MaxHealthLookup lookup,
			double fractionOfMax,
			double minHealthFraction) {
		if (root == null || lookup == null) {
			return false;
		}
		boolean changed = false;
		changed |= applySection(root.get("components"), true, lookup, fractionOfMax, minHealthFraction);
		changed |= applySection(root.get("weapons"), false, lookup, fractionOfMax, minHealthFraction);
		return changed;
	}

	public static MaxHealthLookup lookupFromTemplate(Vehicle template) {
		Map<String, Double> components = new HashMap<>();
		Map<String, Double> weapons = new HashMap<>();
		if (template != null && template.getComponentHandler() != null) {
			for (VehicleComponent component : template.getComponentHandler().getComponents()) {
				if (component == null || component.getType() == null || component.getHealthData() == null) {
					continue;
				}
				components.put(
						component.getType().toString().toLowerCase(Locale.ROOT),
						component.getHealthData().getHealth());
			}
		}
		if (template != null && template.getWeapons() != null) {
			for (Weapon weapon : template.getWeapons()) {
				if (weapon == null || weapon.getId() == null || weapon.getHealthData() == null) {
					continue;
				}
				weapons.put(weapon.getId(), weapon.getHealthData().getHealth());
			}
		}
		return new MaxHealthLookup() {
			@Override
			public double componentMaxHealth(String componentTypeKey) {
				if (componentTypeKey == null) {
					return 0.0;
				}
				Double value = components.get(componentTypeKey.toLowerCase(Locale.ROOT));
				return value == null ? 0.0 : value;
			}

			@Override
			public double weaponMaxHealth(String weaponId) {
				if (weaponId == null) {
					return 0.0;
				}
				Double value = weapons.get(weaponId);
				if (value != null) {
					return value;
				}
				for (Map.Entry<String, Double> entry : weapons.entrySet()) {
					if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(weaponId)) {
						return entry.getValue();
					}
				}
				return 0.0;
			}
		};
	}

	private static boolean applySection(
			JsonElement sectionElement,
			boolean components,
			MaxHealthLookup lookup,
			double fractionOfMax,
			double minHealthFraction) {
		if (sectionElement == null || !sectionElement.isJsonObject()) {
			return false;
		}
		JsonObject section = sectionElement.getAsJsonObject();
		boolean changed = false;
		for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
			JsonElement partElement = entry.getValue();
			if (partElement == null || !partElement.isJsonObject()) {
				continue;
			}
			JsonObject part = partElement.getAsJsonObject();
			double maxHealth = components
					? lookup.componentMaxHealth(entry.getKey())
					: lookup.weaponMaxHealth(entry.getKey());
			if (maxHealth <= 0.0) {
				continue;
			}
			double current = jsonDouble(part, "damage");
			double next = nextDamage(current, maxHealth, fractionOfMax, minHealthFraction);
			if (Double.compare(current, next) == 0) {
				continue;
			}
			part.addProperty("damage", next);
			changed = true;
		}
		return changed;
	}

	private static double jsonDouble(JsonObject object, String key) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return 0.0;
		}
		try {
			return object.get(key).getAsDouble();
		} catch (RuntimeException e) {
			return 0.0;
		}
	}
}
