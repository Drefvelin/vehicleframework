package net.tfminecraft.VehicleFramework.Vehicles.Controller;

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
import java.util.List;
import java.util.logging.Logger;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Gear.Gear;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;

/**
 * Immediate debug log at {@code logs/ground_engine.log} in the plugin data folder.
 * Enable via {@code ground-engine-logging: true} in config.yml.
 */
public final class GroundEngineLog {
	private static final Logger LOGGER = Logger.getLogger(GroundEngineLog.class.getName());
	private static final DateTimeFormatter SESSION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	static final String LOG_DIRECTORY = "logs";
	static final String LOG_FILE_NAME = "ground_engine.log";

	private static volatile boolean enabled;
	private static volatile Path logFile;
	private static final Object LOCK = new Object();

	private GroundEngineLog() {
	}

	public static void configure(boolean loggingEnabled, boolean wipeLog, File dataFolder) {
		enabled = loggingEnabled;
		if (dataFolder != null) {
			logFile = dataFolder.toPath().resolve(LOG_DIRECTORY).resolve(LOG_FILE_NAME);
		}
		if (wipeLog) {
			wipeLogFile();
		}
	}

	private static void wipeLogFile() {
		if (logFile == null) {
			return;
		}
		synchronized (LOCK) {
			try {
				Files.deleteIfExists(logFile);
			} catch (IOException exception) {
				LOGGER.warning("Failed to wipe ground_engine.log: " + exception.getMessage());
			}
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static String formatThrottle(Throttle throttle) {
		if (throttle == null) {
			return "";
		}
		return String.format(
				"thr=%d/%d/%d",
				throttle.getCurrent(),
				throttle.getMin(),
				throttle.getMax());
	}

	public static String formatGearedEngine(GearedEngine engine) {
		if (engine == null) {
			return "";
		}
		Gear gear = engine.getGear();
		Throttle throttle = gear == null ? null : gear.getThrottle();
		String thr = formatThrottle(throttle);
		if (thr.isEmpty()) {
			return "";
		}
		return String.format(
				"gear=%d gearName=%s %s shifting=%s started=%s",
				engine.getCurrentGear(),
				gear == null ? "null" : gear.getName(),
				thr,
				engine.isShifting(),
				engine.isStarted());
	}

	public static String formatEngineFragment(ActiveVehicle vehicle) {
		if (vehicle == null) {
			return "";
		}
		if (vehicle.hasComponent(Component.GEARED_ENGINE)) {
			return formatGearedEngine((GearedEngine) vehicle.getComponent(Component.GEARED_ENGINE));
		}
		if (vehicle.hasComponent(Component.ENGINE)) {
			Engine engine = (Engine) vehicle.getComponent(Component.ENGINE);
			return formatThrottle(engine == null ? null : engine.getThrottle());
		}
		return "";
	}

	public static String formatStateSwap(String id, String from, String to, String reason, boolean toIsDefault) {
		return String.format(
				"id=%s state=%s->%s reason=%s isDefault=%s",
				id == null ? "null" : id,
				from == null ? "none" : from,
				to == null ? "null" : to,
				reason == null ? "unknown" : reason,
				toIsDefault);
	}

	public static void append(String message) {
		if (!enabled || message == null || logFile == null) {
			return;
		}
		writeLines(List.of(SESSION_TIME.format(Instant.now()) + " " + message));
	}

	private static void writeLines(List<String> lines) {
		synchronized (LOCK) {
			try {
				Files.createDirectories(logFile.getParent());
				try (BufferedWriter writer = Files.newBufferedWriter(
						logFile,
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE,
						StandardOpenOption.APPEND)) {
					for (String line : lines) {
						writer.write(line);
						writer.newLine();
					}
				}
			} catch (IOException exception) {
				LOGGER.warning("Failed to write ground_engine.log: " + exception.getMessage());
			}
		}
	}
}
