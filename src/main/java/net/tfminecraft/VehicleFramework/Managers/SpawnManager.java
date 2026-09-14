package net.tfminecraft.VehicleFramework.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Database.VehiclePersistence;
import net.tfminecraft.VehicleFramework.Database.VehicleSnapshot;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Util.SpawnLocation;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class SpawnManager implements Listener {
	private static List<SpawnLocation> spawns = new ArrayList<>();

	private VehicleManager vehicleManager;
	private BukkitTask spawnTickTask;
	private final Set<String> spawning = new HashSet<>();

	public SpawnManager(VehicleManager m) {
		vehicleManager = m;
	}

	public static boolean exists(SpawnLocation s) {
		for (SpawnLocation e : spawns) {
			if (e.getFile().equalsIgnoreCase(s.getFile())) {
				return true;
			}
		}
		return false;
	}

	public static void add(SpawnLocation s) {
		if (exists(s)) {
			return;
		}
		spawns.add(s);
	}

	public static void remove(SpawnLocation s) {
		if (!exists(s)) {
			return;
		}
		spawns.remove(s);
	}

	public static Optional<Location> findSpawnLocation(String vehicleUuid) {
		if (vehicleUuid == null || vehicleUuid.isBlank()) {
			return Optional.empty();
		}
		String uuid = stripJson(vehicleUuid);
		for (SpawnLocation spawn : spawns) {
			if (stripJson(spawn.getFile()).equalsIgnoreCase(uuid)) {
				return Optional.ofNullable(spawn.getLoc());
			}
		}
		return Optional.empty();
	}

	public void start() {
		PersistenceLog.spawnManagerStart(spawns.size());
		enqueueLoadedChunks();
		startTickCycle();
	}

	public void reload() {
		spawns.clear();
		spawning.clear();
		enqueueLoadedChunks();
		startTickCycle();
	}

	public void save() {
		// SQLite is authoritative; queued rows stay in vehicles.db.
	}

	private void enqueueLoadedChunks() {
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				enqueueChunk(chunk);
			}
		}
	}

	private void enqueueChunk(Chunk chunk) {
		if (chunk == null || chunk.getWorld() == null) {
			return;
		}
		VehiclePersistence persistence = VehiclePersistence.current();
		if (persistence == null) {
			return;
		}
		for (VehicleSnapshot snapshot : persistence.findChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())) {
			enqueueSnapshot(snapshot);
		}
	}

	void enqueueSnapshot(VehicleSnapshot snapshot) {
		if (snapshot == null || snapshot.getUuid() == null || snapshot.isDeleted()) {
			return;
		}
		String uuid = snapshot.getUuid();
		if (vehicleManager.getByUUID(uuid) != null) {
			return;
		}
		World world = Bukkit.getWorld(snapshot.getWorld());
		if (world == null) {
			return;
		}
		Location loc = new Location(
				world,
				snapshot.getX(),
				snapshot.getY(),
				snapshot.getZ(),
				snapshot.getYaw(),
				0f);
		SpawnLocation queued = new SpawnLocation(world.getChunkAt(snapshot.getChunkX(), snapshot.getChunkZ()), loc, uuid);
		add(queued);
		PersistenceLog.spawnQueued(uuid, loc, snapshot.getWorld() + " " + snapshot.getChunkX() + "," + snapshot.getChunkZ());
	}

	private void startTickCycle() {
		if (spawnTickTask != null) {
			spawnTickTask.cancel();
			spawnTickTask = null;
		}
		spawnTickTask = new BukkitRunnable() {
			@Override
			public void run() {
				List<SpawnLocation> snapshot = new ArrayList<>(spawns);
				for (SpawnLocation loc : snapshot) {
					if (!loc.hasNearby()) {
						continue;
					}
					String uuid = stripJson(loc.getFile());
					if (uuid == null || !spawning.add(uuid)) {
						continue;
					}
					try {
						trySpawn(loc);
					} finally {
						spawning.remove(uuid);
					}
				}
			}
		}.runTaskTimer(VehicleFramework.plugin, 0L, 20L);
	}

	/**
	 * Returns false when the queue entry must stay for a later retry.
	 */
	boolean trySpawn(SpawnLocation loc) {
		String uuid = stripJson(loc.getFile());
		if (vehicleManager.getByUUID(uuid) != null) {
			remove(loc);
			return true;
		}
		VehiclePersistence persistence = VehiclePersistence.current();
		if (persistence == null) {
			VFLogger.log("Failed to load queued vehicle " + uuid + ": SQLite is not open");
			return false;
		}
		Optional<IncompleteVehicle> loaded = persistence.loadIncomplete(uuid);
		IncompleteVehicle incomplete = loaded.orElse(null);
		PersistenceLog.spawnLoad(uuid, loc.getLoc());
		if (!isComplete(incomplete)) {
			VFLogger.log("Failed to load queued vehicle " + uuid);
			return false;
		}
		Vehicle v = VehicleLoader.getByString(incomplete.getId());
		if (v == null) {
			VFLogger.log("Unknown vehicle type " + incomplete.getId() + " for " + uuid);
			return false;
		}
		if (loc.getLoc() == null || loc.getLoc().getWorld() == null) {
			VFLogger.log("Missing world for queued vehicle " + uuid);
			return false;
		}
		ActiveVehicle spawned = vehicleManager.spawn(loc.getLoc(), v, incomplete);
		if (spawned == null) {
			return false;
		}
		remove(loc);
		return true;
	}

	static boolean isComplete(IncompleteVehicle incomplete) {
		return incomplete != null && incomplete.getId() != null && !incomplete.getId().isBlank();
	}

	static String stripJson(String name) {
		if (name == null) {
			return "";
		}
		if (name.toLowerCase().endsWith(".json")) {
			return name.substring(0, name.length() - 5);
		}
		return name;
	}

	@EventHandler
	public void chunkLoad(ChunkLoadEvent e) {
		Chunk c = e.getChunk();
		PersistenceLog.chunkLoad(c.getWorld().getName(), c.getX(), c.getZ());
		enqueueChunk(c);
	}

	@SuppressWarnings("unchecked")
	@EventHandler
	public void chunkUnload(ChunkUnloadEvent e) {
		Chunk c = e.getChunk();
		PersistenceLog.chunkUnload(c.getWorld().getName(), c.getX(), c.getZ());
		HashMap<Entity, ActiveVehicle> vc = (HashMap<Entity, ActiveVehicle>) vehicleManager.get().clone();
		for (Map.Entry<Entity, ActiveVehicle> entry : vc.entrySet()) {
			ActiveVehicle v = entry.getValue();
			Entity entity = entry.getKey();
			if (!entityInChunk(entity, c)) {
				continue;
			}
			if (!vehicleManager.unload(v)) {
				VFLogger.log("Failed to persist vehicle " + v.getUUID() + " on chunk unload");
			}
		}
	}

	private static boolean entityInChunk(Entity entity, Chunk c) {
		if (entity == null || c == null || entity.isDead() || !entity.isValid()) {
			return false;
		}
		try {
			return entity.getLocation().getChunk().equals(c);
		} catch (Exception ex) {
			return false;
		}
	}
}
