package net.tfminecraft.VehicleFramework.Vehicles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.WeaponHandler;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;

/**
 * Generic health decay for spawned and file-backed vehicles. Callers supply
 * fraction-of-max and remaining-health floor; this class does not know about upkeep.
 */
public final class VehicleHealthDecay {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

	public static File storedVehicleFile(String vehicleUuid) {
		if (vehicleUuid == null || vehicleUuid.isBlank()) {
			return new File("plugins/VehicleFramework/data/vehicles", "invalid.json");
		}
		String id = vehicleUuid.trim();
		if (id.toLowerCase(Locale.ROOT).endsWith(".json")) {
			id = id.substring(0, id.length() - 5);
		}
		return new File("plugins/VehicleFramework/data/vehicles", id + ".json");
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

	public static boolean applyToStoredFile(
			File file,
			double fractionOfMax,
			double minHealthFraction) {
		if (file == null || !file.isFile()) {
			return false;
		}
		JsonObject root;
		try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject()) {
				return false;
			}
			root = parsed.getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			return false;
		}
		String typeId = jsonString(root, "id");
		if (typeId == null || typeId.isBlank()) {
			return false;
		}
		Vehicle template = VehicleLoader.getByString(typeId);
		if (template == null) {
			return false;
		}
		boolean changed = applyToJson(root, lookupFromTemplate(template), fractionOfMax, minHealthFraction);
		if (!changed) {
			return true;
		}
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			GSON.toJson(root, writer);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	static boolean applyToJson(
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

	static MaxHealthLookup lookupFromTemplate(Vehicle template) {
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

	private static String jsonString(JsonObject object, String key) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return null;
		}
		try {
			return object.get(key).getAsString();
		} catch (RuntimeException e) {
			return null;
		}
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
