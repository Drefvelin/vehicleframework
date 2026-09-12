package net.tfminecraft.VehicleFramework.Tracks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

public final class TrackJunctionSession {
	public static final class Pending {
		public final UUID stemId;
		public final double s;
		public final int facingSign;

		public Pending(UUID stemId, double s, int facingSign) {
			this.stemId = stemId;
			this.s = s;
			this.facingSign = facingSign;
		}
	}

	private static final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

	private TrackJunctionSession() {
	}

	public static void set(Player player, Pending value) {
		pending.put(player.getUniqueId(), value);
	}

	public static Pending get(Player player) {
		return pending.get(player.getUniqueId());
	}

	public static void clear(Player player) {
		pending.remove(player.getUniqueId());
	}
}
