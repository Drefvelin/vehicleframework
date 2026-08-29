package net.tfminecraft.VehicleFramework.Tracks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TrackAnchorSession {
	private static final Map<UUID, Location> start = new ConcurrentHashMap<>();

	private TrackAnchorSession() {
	}

	public static void setStart(Player player, Location location) {
		start.put(player.getUniqueId(), location.clone());
	}

	public static Location getStart(Player player) {
		return start.get(player.getUniqueId());
	}

	public static void clear(Player player) {
		start.remove(player.getUniqueId());
	}
}
