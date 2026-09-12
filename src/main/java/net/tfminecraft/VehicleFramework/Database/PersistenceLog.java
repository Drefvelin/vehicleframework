package net.tfminecraft.VehicleFramework.Database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TrainHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

/**
 * Append-only debug log at {@code logs/persistence.log} in the plugin data folder.
 * Not wiped on reload so a first-reload failure is still there after a second reload.
 * Enable via {@code debug-logging: true} in trains.yml.
 */
public final class PersistenceLog {
	private static final Logger LOGGER = Logger.getLogger(PersistenceLog.class.getName());
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	static final String LOG_DIRECTORY = "logs";
	static final String LOG_FILE_NAME = "persistence.log";

	private static volatile boolean enabled;
	private static volatile Path logFile;
	private static final Object LOCK = new Object();
	private static final Map<String, Integer> poseCounts = new ConcurrentHashMap<>();
	private static final Map<String, Long> poseTimes = new ConcurrentHashMap<>();
	private static final Map<String, Integer> placeCounts = new ConcurrentHashMap<>();
	private static final Map<String, Long> particleTimes = new ConcurrentHashMap<>();
	private static int spawnCycle;

	private PersistenceLog() {
	}

	public static void configure(boolean loggingEnabled, File dataFolder) {
		enabled = loggingEnabled;
		if (dataFolder != null) {
			logFile = dataFolder.toPath().resolve(LOG_DIRECTORY).resolve(LOG_FILE_NAME);
		}
		if (enabled) {
			append("SESSION plugin-folder=" + (dataFolder == null ? "null" : dataFolder.getAbsolutePath()));
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static int nextSpawnCycle() {
		return ++spawnCycle;
	}

	public static int spawnCycle() {
		return spawnCycle;
	}

	public static void append(String message) {
		if (!enabled || message == null || logFile == null) {
			return;
		}
		write(TIME.format(Instant.now()) + " " + message);
	}

	public static String xyz(Location loc) {
		if (loc == null || loc.getWorld() == null) {
			return "null";
		}
		return loc.getWorld().getName() + " " + xyz(loc.getX(), loc.getY(), loc.getZ())
				+ " yaw=" + fmt(loc.getYaw()) + " pitch=" + fmt(loc.getPitch())
				+ " chunk=" + loc.getChunk().getX() + "," + loc.getChunk().getZ()
				+ " loaded=" + loc.getChunk().isLoaded();
	}

	public static String xyz(double x, double y, double z) {
		return fmt(x) + "," + fmt(y) + "," + fmt(z);
	}

	public static String vehicle(ActiveVehicle v) {
		if (v == null) {
			return "vehicle=null";
		}
		StringBuilder line = new StringBuilder();
		line.append("uuid=").append(v.getUUID());
		line.append(" id=").append(v.getId());
		line.append(" name=").append(v.getName());
		line.append(" ageMs=").append(System.currentTimeMillis() - v.getSpawnTime());
		line.append(" entity=").append(xyz(v.getEntity() == null ? null : v.getEntity().getLocation()));
		line.append(" dead=").append(v.getEntity() == null || v.getEntity().isDead());
		if (v.isTrain()) {
			TrainHandler train = v.getTrainHandler();
			line.append(" train=true");
			line.append(" bound=").append(train.isBound());
			line.append(" spline=").append(train.getSplineId());
			line.append(" s=").append(fmt(train.getS()));
			line.append(" sign=").append(train.getTravelSign());
			line.append(" parent=").append(v.hasParent() ? v.getParent().getUUID() : train.getPendingParent());
			line.append(" child=").append(train.hasChild() ? train.getChild().getUUID() : train.getPendingChild());
			line.append(" liveParent=").append(v.hasParent());
			line.append(" liveChild=").append(train.hasChild());
		} else {
			line.append(" train=false");
		}
		line.append(" vfPassengers=").append(v.getSeatHandler() == null ? 0 : v.getSeatHandler().getPassengers().size());
		if (v.getEntity() != null) {
			line.append(" bukkitPassengers=").append(v.getEntity().getPassengers().size());
		}
		return line.toString();
	}

	public static String player(Player p) {
		if (p == null) {
			return "player=null";
		}
		Entity ride = p.getVehicle();
		return "player=" + p.getName()
				+ " loc=" + xyz(p.getLocation())
				+ " inside=" + p.isInsideVehicle()
				+ " bukkitVehicle=" + (ride == null ? "none" : ride.getUniqueId());
	}

	public static void spawnManagerStart(int pending) {
		append("SPAWN_MANAGER_START cycle=" + nextSpawnCycle() + " pending=" + pending);
	}

	public static void spawnLoad(String file, Location loc) {
		append("SPAWN_LOAD file=" + file + " loc=" + xyz(loc) + " cycle=" + spawnCycle);
	}

	public static void spawnQueued(String file, Location loc, String worldChunk) {
		append("SPAWN_QUEUED file=" + file + " loc=" + xyz(loc) + " from=" + worldChunk);
	}

	public static void spawned(ActiveVehicle v, Location requested) {
		append("SPAWNED requested=" + xyz(requested) + " " + vehicle(v));
	}

	public static void unload(ActiveVehicle v, String reason) {
		append("UNLOAD reason=" + reason + " " + vehicle(v));
		if (v != null && v.getUUID() != null) {
			poseCounts.remove(v.getUUID());
			poseTimes.remove(v.getUUID());
			placeCounts.remove(v.getUUID());
			particleTimes.remove(v.getUUID());
		}
	}

	public static void saveVehicle(ActiveVehicle v, Location loc) {
		append("SAVE_VEHICLE " + vehicle(v) + " saveLoc=" + xyz(loc));
	}

	public static void saveSpawn(String file, Location loc) {
		append("SAVE_SPAWN file=" + file + " loc=" + xyz(loc));
	}

	public static void loadJson(String file, String uuid, String id, ConsistData consist) {
		append("LOAD_JSON file=" + file
				+ " uuid=" + uuid
				+ " id=" + id
				+ " consist=" + consistText(consist));
	}

	public static void tryLink(String phase, ActiveVehicle v) {
		append("TRY_LINK " + phase + " " + vehicle(v));
	}

	public static void placeCars(ActiveVehicle loco) {
		if (loco == null) {
			return;
		}
		String uuid = loco.getUUID();
		int n = placeCounts.merge(uuid, 1, Integer::sum);
		long age = System.currentTimeMillis() - loco.getSpawnTime();
		if (n > 20 && age > 15000) {
			return;
		}
		append("PLACE_CARS n=" + n + " " + vehicle(loco));
	}

	public static void applyPose(ActiveVehicle vehicle, String reason, Location before, Location target) {
		if (vehicle == null) {
			return;
		}
		String uuid = vehicle.getUUID();
		int n = poseCounts.merge(uuid, 1, Integer::sum);
		double gap = dist(before, target);
		long now = System.currentTimeMillis();
		if (n > 20 && gap < 0.25) {
			Long last = poseTimes.get(uuid);
			if (last != null && now - last < 1000) {
				return;
			}
		}
		poseTimes.put(uuid, now);
		Location after = vehicle.getEntity() == null ? null : vehicle.getEntity().getLocation();
		append("APPLY_POSE n=" + n
				+ " reason=" + reason
				+ " gap=" + fmt(gap)
				+ " before=" + xyz(before)
				+ " target=" + xyz(target)
				+ " after=" + xyz(after)
				+ " " + vehicle(vehicle));
	}

	public static void mount(Player p, ActiveVehicle v, Seat seat, boolean managerOk, boolean onSeatMap) {
		append("MOUNT seat=" + (seat == null ? "null" : seat.getBone())
				+ " type=" + (seat == null ? "null" : seat.getType())
				+ " manager=" + managerOk
				+ " meSeatMap=" + onSeatMap
				+ " " + player(p)
				+ " " + vehicle(v));
	}

	public static void remount(Entity e, ActiveVehicle v, String bone) {
		append("REMOUNT bone=" + bone
				+ " entity=" + (e instanceof Player p ? p.getName() : String.valueOf(e.getUniqueId()))
				+ " " + (e instanceof Player p ? player(p) : "eloc=" + xyz(e.getLocation()))
				+ " " + vehicle(v));
	}

	public static void dismount(Entity e, ActiveVehicle v, boolean change) {
		append("DISMOUNT change=" + change
				+ " " + (e instanceof Player p ? player(p) : "entity=" + e)
				+ " " + vehicle(v));
	}

	public static void particleOffset(ActiveVehicle v, Location bone, Location entity) {
		if (v == null || v.getUUID() == null) {
			return;
		}
		double gap = dist(bone, entity);
		if (gap < 0.5) {
			return;
		}
		long now = System.currentTimeMillis();
		Long last = particleTimes.get(v.getUUID());
		if (last != null && now - last < 1000) {
			return;
		}
		particleTimes.put(v.getUUID(), now);
		append("SMOKE_OFFSET gap=" + fmt(gap)
				+ " bone=" + xyz(bone)
				+ " entity=" + xyz(entity)
				+ " " + vehicle(v));
	}

	public static void chunkLoad(String world, int x, int z) {
		append("CHUNK_LOAD " + world + " " + x + "," + z);
	}

	public static void chunkUnload(String world, int x, int z) {
		append("CHUNK_UNLOAD " + world + " " + x + "," + z);
	}

	private static String consistText(ConsistData consist) {
		if (consist == null || consist.isUnbound()) {
			return "none";
		}
		return "parent=" + consist.getParent()
				+ " child=" + consist.getChild()
				+ " spline=" + consist.getSplineId()
				+ " s=" + consist.getS()
				+ " sign=" + consist.getTravelSign();
	}

	private static double dist(Location a, Location b) {
		if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
			return -1;
		}
		if (!a.getWorld().equals(b.getWorld())) {
			return -1;
		}
		return a.distance(b);
	}

	private static String fmt(double value) {
		return String.format(Locale.US, "%.3f", value);
	}

	private static void write(String line) {
		synchronized (LOCK) {
			try {
				Files.createDirectories(logFile.getParent());
				try (BufferedWriter writer = Files.newBufferedWriter(
						logFile,
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE,
						StandardOpenOption.APPEND)) {
					writer.write(line);
					writer.newLine();
				}
			} catch (IOException e) {
				LOGGER.warning("Failed to write persistence.log: " + e.getMessage());
			}
		}
	}
}
