package net.tfminecraft.VehicleFramework.Database;

import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeMap;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Tracks.ThrottleTape;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container.Container;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;

public final class ActiveVehicleSnapshotFactory {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ActiveVehicleSnapshotFactory() {
	}

	public static Optional<VehicleSnapshot> fromLive(ActiveVehicle vehicle) {
		if (vehicle == null || vehicle.getUUID() == null) {
			return Optional.empty();
		}
		Entity entity = vehicle.getEntity();
		if (entity == null || entity.isDead() || !entity.isValid()) {
			return Optional.empty();
		}
		Location loc;
		try {
			loc = entity.getLocation();
		} catch (Exception ex) {
			return Optional.empty();
		}
		if (loc == null || loc.getWorld() == null) {
			return Optional.empty();
		}
		String payload = encodePayload(vehicle);
		if (payload == null || payload.isBlank()) {
			return Optional.empty();
		}
		String owner = vehicle.getOwnerData() == null ? "none" : vehicle.getOwnerData().getOwner();
		double x = loc.getX();
		double z = loc.getZ();
		return Optional.of(new VehicleSnapshot(
				vehicle.getUUID().toString(),
				vehicle.getId(),
				vehicle.getName(),
				owner,
				loc.getWorld().getName(),
				x,
				loc.getY(),
				z,
				loc.getYaw(),
				chunkCoord(x),
				chunkCoord(z),
				payload,
				VehicleRepository.SCHEMA_VERSION,
				1,
				false,
				System.currentTimeMillis()));
	}

	static int chunkCoord(double block) {
		return (int) Math.floor(block / 16.0);
	}

	@SuppressWarnings("unchecked")
	private static String encodePayload(ActiveVehicle v) {
		JSONObject vehicleData = new JSONObject();
		vehicleData.put("id", v.getId());
		vehicleData.put("name", v.getName());
		vehicleData.put("yaw", (double) v.getEntity().getLocation().getYaw());
		vehicleData.put("skin", v.getSkinHandler().getCurrentSkin().getId());
		vehicleData.put("owner", v.getOwnerData().getOwner());
		vehicleData.put("whitelisted", v.getOwnerData().isWhiteListed());
		vehicleData.put("ticketsEnabled", v.getOwnerData().isTicketsEnabled());
		if (v.getOwnerData().getTicketId() != null) {
			vehicleData.put("ticketId", v.getOwnerData().getTicketId());
		}
		JSONArray whitelistArray = new JSONArray();
		for (String entry : v.getOwnerData().getWhiteList()) {
			whitelistArray.add(entry);
		}
		vehicleData.put("whitelist", whitelistArray);
		if (v.hasContainers()) {
			saveContainers(new ArrayList<>(v.getContainerHandler().getContainers().values()), vehicleData);
		}

		JSONObject passengersObject = new JSONObject();
		for (Entity e : v.getSeatHandler().getPassengers()) {
			Seat seat = v.getSeat(e);
			if (seat == null) {
				continue;
			}
			JSONObject passengerData = new JSONObject();
			if (e instanceof Player player) {
				passengerData.put("player", player.getName());
			} else {
				passengerData.put("entity", e.getUniqueId().toString());
			}
			passengersObject.put(seat.getBone(), passengerData);
		}
		vehicleData.put("passengers", passengersObject);

		JSONObject componentsObject = new JSONObject();
		for (VehicleComponent component : v.getComponents()) {
			JSONObject componentData = new JSONObject();
			componentData.put("damage", component.getHealthData().getDamage());
			if (component.isOnFire()) {
				componentData.put("fire", component.getFire().getProgress());
			}
			if (component instanceof SinkableHull hull && hull.isSinking()) {
				componentData.put("sinkprogress", hull.getSinkProgress());
			}
			Component type = component.getType();
			switch (type) {
				case ENGINE:
					componentData.put("throttle", ((Engine) component).getThrottle().getCurrent());
					componentData.put("fuel", ((Engine) component).getFuelTank().getCurrent());
					break;
				case GEARED_ENGINE:
					componentData.put("gear", ((GearedEngine) component).getCurrentGear());
					componentData.put("throttle", ((GearedEngine) component).getGear().getThrottle().getCurrent());
					componentData.put("fuel", ((GearedEngine) component).getFuelTank().getCurrent());
					break;
				default:
					break;
			}
			componentsObject.put(type.toString().toLowerCase(), componentData);
		}
		vehicleData.put("components", componentsObject);

		JSONObject rotatorsObject = new JSONObject();
		for (BoneRotator rotator : v.getAccessPanel().getRotators()) {
			JSONObject rotation = new JSONObject();
			Quaternionf q = rotator.getAnimator().getRotation();
			rotation.put("x", q.x());
			rotation.put("y", q.y());
			rotation.put("z", q.z());
			rotation.put("w", q.w());
			rotatorsObject.put(rotator.getId(), rotation);
		}
		vehicleData.put("rotators", rotatorsObject);

		if (!v.getWeaponHandler().getWeapons().isEmpty()) {
			JSONObject weaponsObject = new JSONObject();
			for (ActiveWeapon w : v.getWeaponHandler().getWeapons()) {
				JSONObject weaponData = new JSONObject();
				weaponData.put("damage", w.getHealthData().getDamage());
				if (w.getAmmunitionHandler().hasAmmo()) {
					weaponData.put("ammo", w.getAmmunitionHandler().getAmmo().getId());
					weaponData.put("count", w.getAmmunitionHandler().getCount());
				}
				weaponsObject.put(w.getId(), weaponData);
			}
			vehicleData.put("weapons", weaponsObject);
		}

		if (v.isTrain()) {
			v.getTrainHandler().toConsistData().put(vehicleData);
			if (!v.hasParent()) {
				ThrottleTape tape = v.getTrainHandler().getInstalledTape();
				if (tape != null && !tape.isEmpty()) {
					vehicleData.put("throttleTape", tape.toJson());
				}
			}
		}

		TreeMap<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		treeMap.putAll(vehicleData);
		return GSON.toJson(treeMap);
	}

	@SuppressWarnings("unchecked")
	static JSONObject saveContainers(java.util.List<Container> containers, JSONObject vehicleJson) {
		JSONObject containersSection = new JSONObject();
		for (Container container : containers) {
			com.google.gson.JsonObject gsonJson = container.getAsJson();
			Object parsed = org.json.simple.JSONValue.parse(gsonJson.toString());
			if (parsed instanceof JSONObject simpleJson) {
				containersSection.put(container.getId(), simpleJson);
			}
		}
		vehicleJson.put("containers", containersSection);
		return vehicleJson;
	}
}
