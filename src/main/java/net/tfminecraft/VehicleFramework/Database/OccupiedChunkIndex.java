package net.tfminecraft.VehicleFramework.Database;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory live occupancy by chunk. Avoids SQLite on empty {@code ChunkLoadEvent}s.
 */
final class OccupiedChunkIndex {
	private final Map<String, ChunkKey> byUuid = new HashMap<>();
	private final Map<ChunkKey, Integer> counts = new HashMap<>();

	record ChunkKey(String world, int chunkX, int chunkZ) {}

	record LiveLocation(String uuid, String world, int chunkX, int chunkZ) {}

	void replace(Iterable<LiveLocation> liveRows) {
		byUuid.clear();
		counts.clear();
		if (liveRows == null) {
			return;
		}
		for (LiveLocation row : liveRows) {
			putLive(row);
		}
	}

	void putLive(VehicleSnapshot snapshot) {
		if (snapshot == null || snapshot.isDeleted()) {
			remove(snapshot == null ? null : snapshot.getUuid());
			return;
		}
		putLive(new LiveLocation(
				snapshot.getUuid(),
				snapshot.getWorld(),
				snapshot.getChunkX(),
				snapshot.getChunkZ()));
	}

	void putLive(LiveLocation location) {
		if (location == null) {
			return;
		}
		putLive(location.uuid(), location.world(), location.chunkX(), location.chunkZ());
	}

	void putLive(String uuid, String world, int chunkX, int chunkZ) {
		String key = normalizeUuid(uuid);
		ChunkKey next = chunkKey(world, chunkX, chunkZ);
		if (key == null || next == null) {
			return;
		}
		ChunkKey previous = byUuid.put(key, next);
		if (Objects.equals(previous, next)) {
			return;
		}
		if (previous != null) {
			decrement(previous);
		}
		counts.put(next, counts.getOrDefault(next, 0) + 1);
	}

	void remove(String uuid) {
		String key = normalizeUuid(uuid);
		if (key == null) {
			return;
		}
		ChunkKey previous = byUuid.remove(key);
		if (previous != null) {
			decrement(previous);
		}
	}

	boolean isOccupied(String world, int chunkX, int chunkZ) {
		ChunkKey key = chunkKey(world, chunkX, chunkZ);
		return key != null && counts.getOrDefault(key, 0) > 0;
	}

	int count(String world, int chunkX, int chunkZ) {
		ChunkKey key = chunkKey(world, chunkX, chunkZ);
		if (key == null) {
			return 0;
		}
		return counts.getOrDefault(key, 0);
	}

	private void decrement(ChunkKey key) {
		int next = counts.getOrDefault(key, 0) - 1;
		if (next <= 0) {
			counts.remove(key);
			return;
		}
		counts.put(key, next);
	}

	private static String normalizeUuid(String uuid) {
		if (uuid == null || uuid.isBlank()) {
			return null;
		}
		return uuid.trim().toLowerCase(Locale.ROOT);
	}

	private static ChunkKey chunkKey(String world, int chunkX, int chunkZ) {
		if (world == null || world.isBlank()) {
			return null;
		}
		return new ChunkKey(world, chunkX, chunkZ);
	}
}
