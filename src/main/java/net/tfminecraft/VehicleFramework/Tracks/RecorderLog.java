package net.tfminecraft.VehicleFramework.Tracks;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Location;

import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TrainHandler;

/**
 * Append-only debug log at {@code logs/recorder.log} in the plugin data folder.
 * Not wiped on reload. Enable via {@code debug-logging: true} in trains.yml.
 */
public final class RecorderLog {
	private static final Logger LOGGER = Logger.getLogger(RecorderLog.class.getName());
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	static final String LOG_DIRECTORY = "logs";
	static final String LOG_FILE_NAME = "recorder.log";

	private static volatile boolean enabled;
	private static volatile Path logFile;
	private static final Object LOCK = new Object();
	private static final Map<String, Long> quietUntil = new ConcurrentHashMap<>();

	private RecorderLog() {
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

	public static void append(String message) {
		if (!enabled || message == null || logFile == null) {
			return;
		}
		write(TIME.format(Instant.now()) + " " + message);
	}

	public static String train(ActiveVehicle v) {
		if (v == null || !v.isTrain()) {
			return "train=null";
		}
		TrainHandler t = v.getTrainHandler();
		int thr = v.getThrottle() == null ? 0 : v.getThrottle().getCurrent();
		int samples = t.getInstalledTape() == null ? 0 : t.getInstalledTape().getSamples().size();
		int rec = t.recordingSampleCount();
		float eyaw = v.getEntity() == null ? Float.NaN : v.getEntity().getLocation().getYaw();
		float dyaw = Float.NaN;
		if (v.getBehaviourHandler() != null && v.getBehaviourHandler().getRotator() != null) {
			BoneRotator rotator = v.getBehaviourHandler().getRotator();
			dyaw = rotator.getDriveYaw();
		}
		return "uuid=" + v.getUUID()
				+ " id=" + v.getId()
				+ " s=" + fmt(t.getS())
				+ " sign=" + t.getTravelSign()
				+ " thr=" + thr
				+ " eyaw=" + fmtAngle(eyaw)
				+ " dyaw=" + fmtAngle(dyaw)
				+ " bound=" + t.isBound()
				+ " spline=" + t.getSplineId()
				+ " tape=" + samples
				+ " recording=" + t.isRecording()
				+ " recSamples=" + rec
				+ " captain=" + (v.getSeatHandler() != null && v.getSeatHandler().hasCaptain());
	}

	public static void pose(ActiveVehicle v, Location from, Location to, TrackPose pose, ConvertedAngle move) {
		if (v == null) {
			return;
		}
		if (!note("pose:" + v.getUUID(), 250)) {
			return;
		}
		double gap = 0;
		if (from != null && to != null && from.getWorld() != null && from.getWorld().equals(to.getWorld())) {
			gap = from.distance(to);
		}
		append("POSE"
				+ " gap=" + fmt(gap)
				+ " myaw=" + (move == null ? "-" : fmt(move.getYaw()))
				+ " mpitch=" + (move == null ? "-" : fmt(move.getPitch()))
				+ " pyaw=" + (pose == null ? "-" : fmt(pose.yaw))
				+ " ppitch=" + (pose == null ? "-" : fmt(pose.pitch))
				+ " " + train(v));
	}

	public static void sample(ActiveVehicle v, ThrottleTape.AppendResult result, int throttle, int hold) {
		if (result == ThrottleTape.AppendResult.HELD && hold > 1 && hold % 20 != 0) {
			return;
		}
		append("SAMPLE result=" + result + " thr=" + throttle + " hold=" + hold + " " + train(v));
	}

	public static void playback(ActiveVehicle v, String reason, Integer target, ThrottleTape.DwellState dwell) {
		String key = (v == null ? "none" : v.getUUID()) + ":" + reason + ":" + target
				+ ":" + (dwell != null && dwell.left > 0);
		if (!note(key, 750)) {
			return;
		}
		append("PLAY reason=" + reason
				+ " target=" + target
				+ " dwellLeft=" + (dwell == null ? "-" : dwell.left)
				+ " dwellS=" + (dwell == null || dwell.atS == null ? "-" : fmt(dwell.atS))
				+ " " + train(v));
	}

	public static void interact(String action, String extra) {
		append("INTERACT " + action + (extra == null || extra.isBlank() ? "" : " " + extra));
	}

	public static void arm(ActiveVehicle v, String status, TrackJunction.Side hold, TrackJunction frog, double ahead) {
		append("ARM status=" + status
				+ " hold=" + (hold == null ? "-" : hold.name())
				+ " frogSide=" + (frog == null ? "-" : frog.side.name())
				+ " frogS=" + (frog == null ? "-" : fmt(frog.s))
				+ " ahead=" + fmt(ahead)
				+ " facing=" + (frog == null ? "-" : frog.facingSign)
				+ " " + train(v));
	}

	public static void junction(ActiveVehicle v, boolean diverge, UUID junctionId) {
		junction(v, diverge, junctionId, "");
	}

	public static void junction(ActiveVehicle v, boolean diverge, UUID junctionId, String reason) {
		append("JUNCTION take=" + (diverge ? "diverge" : "through")
				+ " id=" + junctionId
				+ (reason == null || reason.isBlank() ? "" : " reason=" + reason)
				+ " " + train(v));
	}

	public static boolean throttle(String key, long minMs) {
		return note(key, minMs);
	}

	private static boolean note(String key, long minMs) {
		long now = System.currentTimeMillis();
		Long last = quietUntil.get(key);
		if (last != null && now - last < minMs) {
			return false;
		}
		quietUntil.put(key, now);
		return true;
	}

	private static String fmt(double value) {
		return String.format(Locale.US, "%.3f", value);
	}

	private static String fmtAngle(float value) {
		if (Float.isNaN(value)) {
			return "-";
		}
		return fmt(value);
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
				LOGGER.warning("Failed to write recorder.log: " + e.getMessage());
			}
		}
	}
}
