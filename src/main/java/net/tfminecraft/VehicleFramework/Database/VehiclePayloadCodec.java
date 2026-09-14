package net.tfminecraft.VehicleFramework.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Tracks.ThrottleTape;

/**
 * Shared JSON payload stored in SQLite {@code payload_json}.
 * Does not touch Bukkit entities.
 */
public final class VehiclePayloadCodec {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private VehiclePayloadCodec() {
	}

	public static Optional<IncompleteVehicle> decode(String json, String uuid) {
		if (json == null || json.isBlank()) {
			return Optional.empty();
		}
		try {
			JSONObject parsed = (JSONObject) new JSONParser().parse(json);
			return decode(parsed, uuid);
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	public static Optional<IncompleteVehicle> decode(JSONObject json, String uuid) {
		if (json == null) {
			return Optional.empty();
		}
		try {
			String id = stringOrNull(json.get("id"));
			if (id == null || id.isBlank()) {
				return Optional.empty();
			}
			String name = stringOrDefault(json.get("name"), id);
			String skin = stringOrDefault(json.get("skin"), id);
			String owner = json.containsKey("owner") ? stringOrDefault(json.get("owner"), "none") : "none";
			boolean whitelisted = bool(json.get("whitelisted"));
			boolean ticketsEnabled = bool(json.get("ticketsEnabled"));
			String ticketId = json.containsKey("ticketId") && json.get("ticketId") != null
					? String.valueOf(json.get("ticketId"))
					: null;
			List<String> whitelist = new ArrayList<>();
			if (json.get("whitelist") instanceof JSONArray whitelistArray) {
				for (Object entry : whitelistArray) {
					if (entry != null) {
						whitelist.add(String.valueOf(entry));
					}
				}
			}

			int throttle = 0;
			int gear = 1;
			double fuel = 0;
			float yaw = json.containsKey("yaw") ? asFloat(json.get("yaw")) : 0f;

			List<IncompleteComponent> componentsList = new ArrayList<>();
			if (json.get("components") instanceof JSONObject componentsObject) {
				for (Object componentKey : componentsObject.keySet()) {
					if (!(componentKey instanceof String componentType)) {
						continue;
					}
					if (!(componentsObject.get(componentType) instanceof JSONObject componentData)) {
						continue;
					}
					double damage = asDouble(componentData.get("damage"));
					int fireProgress = componentData.containsKey("fire") ? asInt(componentData.get("fire")) : 0;
					int sinkProgress = componentData.containsKey("sinkprogress")
							? asInt(componentData.get("sinkprogress"))
							: 0;
					try {
						Component c = Component.valueOf(componentType.toUpperCase());
						switch (c) {
							case ENGINE:
								if (componentData.containsKey("throttle")) {
									throttle = asInt(componentData.get("throttle"));
								}
								if (componentData.containsKey("fuel")) {
									fuel = asDouble(componentData.get("fuel"));
								}
								break;
							case GEARED_ENGINE:
								if (componentData.containsKey("gear")) {
									gear = asInt(componentData.get("gear"));
								}
								if (componentData.containsKey("throttle")) {
									throttle = asInt(componentData.get("throttle"));
								}
								if (componentData.containsKey("fuel")) {
									fuel = asDouble(componentData.get("fuel"));
								}
								break;
							default:
								break;
						}
						componentsList.add(new IncompleteComponent(c, damage, fireProgress, sinkProgress));
					} catch (Exception ignored) {
					}
				}
			}

			List<RotationData> rotations = new ArrayList<>();
			if (json.get("rotators") instanceof JSONObject rotatorsObject) {
				for (Object key : rotatorsObject.keySet()) {
					if (!(key instanceof String rotatorId)) {
						continue;
					}
					if (rotatorsObject.get(rotatorId) instanceof JSONObject rotationValues) {
						rotations.add(new RotationData(rotatorId, rotationValues));
					}
				}
			}

			List<IncompleteWeapon> weapons = new ArrayList<>();
			if (json.get("weapons") instanceof JSONObject weaponsObject) {
				for (Object weaponKey : weaponsObject.keySet()) {
					if (!(weaponKey instanceof String weaponId)) {
						continue;
					}
					if (!(weaponsObject.get(weaponId) instanceof JSONObject weaponData)) {
						continue;
					}
					double damage = asDouble(weaponData.get("damage"));
					String ammo = weaponData.containsKey("ammo") ? stringOrNull(weaponData.get("ammo")) : null;
					int count = weaponData.containsKey("count") ? asInt(weaponData.get("count")) : 0;
					weapons.add(new IncompleteWeapon(weaponId, damage, ammo, count));
				}
			}

			List<PassengerData> passengers = new ArrayList<>();
			if (json.get("passengers") instanceof JSONObject passengersObject) {
				for (Object passengerKey : passengersObject.keySet()) {
					if (!(passengerKey instanceof String seatId)) {
						continue;
					}
					if (!(passengersObject.get(seatId) instanceof JSONObject passengerData)) {
						continue;
					}
					if (passengerData.containsKey("entity")) {
						try {
							UUID entityUUID = UUID.fromString(String.valueOf(passengerData.get("entity")));
							passengers.add(new PassengerData(entityUUID, seatId));
						} catch (IllegalArgumentException ignored) {
						}
					} else if (passengerData.get("player") != null) {
						passengers.add(new PassengerData(String.valueOf(passengerData.get("player")), seatId));
					}
				}
			}

			List<JsonObject> containers = loadContainers(json);
			ConsistData consist = ConsistData.fromJson(json);
			ThrottleTape tape = json.get("throttleTape") instanceof JSONObject tapeJson
					? ThrottleTape.fromJson(tapeJson)
					: null;
			String resolvedUuid = uuid == null || uuid.isBlank()
					? stringOrDefault(json.get("uuid"), "")
					: uuid;
			IncompleteVehicle incomplete = new IncompleteVehicle(
					resolvedUuid,
					id,
					name,
					skin,
					componentsList,
					weapons,
					rotations,
					passengers,
					containers,
					throttle,
					gear,
					yaw,
					fuel,
					owner,
					whitelisted,
					whitelist,
					consist);
			incomplete.setThrottleTape(tape);
			incomplete.setTicketId(ticketId);
			incomplete.setTicketsEnabled(ticketsEnabled);
			return Optional.of(incomplete);
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	@SuppressWarnings("unchecked")
	public static String encode(IncompleteVehicle vehicle) {
		if (vehicle == null) {
			return "{}";
		}
		JSONObject json = new JSONObject();
		json.put("id", vehicle.getId());
		json.put("name", vehicle.getName());
		json.put("yaw", (double) vehicle.getYaw());
		json.put("skin", vehicle.getSkin());
		json.put("owner", vehicle.getOwner());
		json.put("whitelisted", vehicle.isWhitelisted());
		json.put("ticketsEnabled", vehicle.isTicketsEnabled());
		if (vehicle.getTicketId() != null) {
			json.put("ticketId", vehicle.getTicketId());
		}
		JSONArray whitelistArray = new JSONArray();
		if (vehicle.getWhitelist() != null) {
			whitelistArray.addAll(vehicle.getWhitelist());
		}
		json.put("whitelist", whitelistArray);

		JSONObject componentsObject = new JSONObject();
		if (vehicle.getComponents() != null) {
			for (IncompleteComponent component : vehicle.getComponents()) {
				if (component == null || component.getType() == null) {
					continue;
				}
				JSONObject componentData = new JSONObject();
				componentData.put("damage", component.getDamage());
				if (component.hasFire()) {
					componentData.put("fire", (long) component.getFireProgress());
				}
				if (component.isSinking()) {
					componentData.put("sinkprogress", (long) component.getSinkProgress());
				}
				Component type = component.getType();
				if (type == Component.ENGINE || type == Component.GEARED_ENGINE) {
					componentData.put("throttle", (long) vehicle.getThrottle());
					componentData.put("fuel", vehicle.getFuel());
				}
				if (type == Component.GEARED_ENGINE) {
					componentData.put("gear", (long) vehicle.getGear());
				}
				componentsObject.put(type.toString().toLowerCase(), componentData);
			}
		}
		json.put("components", componentsObject);

		JSONObject rotatorsObject = new JSONObject();
		if (vehicle.getRotations() != null) {
			for (RotationData rotation : vehicle.getRotations()) {
				JSONObject values = new JSONObject();
				values.put("x", (double) rotation.getX());
				values.put("y", (double) rotation.getY());
				values.put("z", (double) rotation.getZ());
				values.put("w", (double) rotation.getW());
				rotatorsObject.put(rotation.getRotator(), values);
			}
		}
		json.put("rotators", rotatorsObject);

		if (vehicle.getWeapons() != null && !vehicle.getWeapons().isEmpty()) {
			JSONObject weaponsObject = new JSONObject();
			for (IncompleteWeapon weapon : vehicle.getWeapons()) {
				JSONObject weaponData = new JSONObject();
				weaponData.put("damage", weapon.getDamage());
				if (weapon.hasAmmo()) {
					weaponData.put("ammo", weapon.getAmmo().getId());
					weaponData.put("count", (long) weapon.getCount());
				}
				weaponsObject.put(weapon.getId(), weaponData);
			}
			json.put("weapons", weaponsObject);
		}

		JSONObject passengersObject = new JSONObject();
		if (vehicle.getPassengers() != null) {
			for (PassengerData passenger : vehicle.getPassengers()) {
				JSONObject passengerData = new JSONObject();
				if (passenger.isEntity()) {
					passengerData.put("entity", passenger.getEntityUUID().toString());
				} else {
					passengerData.put("player", passenger.getPassenger());
				}
				passengersObject.put(passenger.getSeat(), passengerData);
			}
		}
		json.put("passengers", passengersObject);

		if (vehicle.getContainers() != null && !vehicle.getContainers().isEmpty()) {
			JSONObject containersSection = new JSONObject();
			for (JsonObject container : vehicle.getContainers()) {
				if (container == null || !container.has("id")) {
					continue;
				}
				Object parsed = org.json.simple.JSONValue.parse(container.toString());
				if (parsed instanceof JSONObject simpleJson) {
					containersSection.put(container.get("id").getAsString(), simpleJson);
				}
			}
			json.put("containers", containersSection);
		}

		if (vehicle.getConsist() != null) {
			vehicle.getConsist().put(json);
		}
		if (vehicle.getThrottleTape() != null && !vehicle.getThrottleTape().isEmpty()) {
			json.put("throttleTape", vehicle.getThrottleTape().toJson());
		}

		TreeMap<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		treeMap.putAll(json);
		return GSON.toJson(treeMap);
	}

	static List<JsonObject> loadContainers(JSONObject vehicleJson) {
		List<JsonObject> containerList = new ArrayList<>();
		if (vehicleJson == null || !(vehicleJson.get("containers") instanceof JSONObject containersSection)) {
			return containerList;
		}
		for (Object key : containersSection.keySet()) {
			Object rawContainer = containersSection.get(key);
			if (rawContainer == null) {
				continue;
			}
			JsonObject containerJson = JsonParser.parseString(rawContainer.toString()).getAsJsonObject();
			containerList.add(containerJson);
		}
		return containerList;
	}

	private static String stringOrNull(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static String stringOrDefault(Object value, String fallback) {
		String text = stringOrNull(value);
		return text == null || text.isBlank() ? fallback : text;
	}

	private static boolean bool(Object value) {
		return value instanceof Boolean b && b;
	}

	private static float asFloat(Object value) {
		if (value instanceof Number number) {
			return number.floatValue();
		}
		if (value == null) {
			return 0f;
		}
		try {
			return Float.parseFloat(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return 0f;
		}
	}

	private static double asDouble(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value == null) {
			return 0d;
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return 0d;
		}
	}

	private static int asInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return 0;
		}
	}
}
