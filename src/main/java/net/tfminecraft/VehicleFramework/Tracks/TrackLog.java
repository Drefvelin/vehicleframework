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
import java.util.UUID;
import java.util.logging.Logger;

import net.tfminecraft.VehicleFramework.Cache.Cache;

/**
 * Debug log at {@code logs/track.log} in the plugin data folder.
 * Enable via {@code debug-logging: true} in trains.yml.
 */
public final class TrackLog {
	private static final Logger LOGGER = Logger.getLogger(TrackLog.class.getName());
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	static final String LOG_DIRECTORY = "logs";
	static final String LOG_FILE_NAME = "track.log";

	private static volatile boolean enabled;
	private static volatile Path logFile;
	private static final Object LOCK = new Object();

	private TrackLog() {
	}

	public static void configure(boolean loggingEnabled, boolean wipeLog, File dataFolder) {
		enabled = loggingEnabled;
		if (dataFolder != null) {
			logFile = dataFolder.toPath().resolve(LOG_DIRECTORY).resolve(LOG_FILE_NAME);
		}
		if (wipeLog) {
			wipe();
		}
	}

	private static void wipe() {
		if (logFile == null) {
			return;
		}
		synchronized (LOCK) {
			try {
				Files.deleteIfExists(logFile);
			} catch (IOException e) {
				LOGGER.warning("Failed to wipe track.log: " + e.getMessage());
			}
		}
	}

	public static void append(String message) {
		if (!enabled || message == null || logFile == null) {
			return;
		}
		write(TIME.format(Instant.now()) + " " + message);
	}

	public static void start(String player, double x, double y, double z) {
		append("START player=" + player + " " + xyz(x, y, z));
	}

	public static void layAttempt(
			String world,
			double ax, double ay, double az,
			double bx, double by, double bz,
			boolean joinStart, boolean joinEnd) {
		append("LAY world=" + world
				+ " from=" + xyz(ax, ay, az)
				+ " to=" + xyz(bx, by, bz)
				+ " joinStart=" + joinStart
				+ " joinEnd=" + joinEnd
				+ " minDist=" + fmt(Cache.trackMinLayDistance)
				+ " maxTurn=" + fmt(Cache.trackMaxTurnDegrees)
				+ " desiredGrade=" + fmt(Cache.trackDesiredGradeDegrees)
				+ " maxGrade=" + fmt(Cache.trackMaxGradeDegrees));
	}

	public static void layOk(TrackSpline spline, String how) {
		if (spline == null) {
			return;
		}
		TrackSample first = spline.first();
		TrackSample last = spline.last();
		append("LAY_OK how=" + how
				+ " id=" + spline.getId()
				+ " samples=" + spline.getSamples().size()
				+ " length=" + fmt(spline.length())
				+ " first=" + xyz(first.x, first.y, first.z)
				+ " last=" + xyz(last.x, last.y, last.z));
	}

	public static void layFail(String reason, TrackLayException error) {
		String extra = "";
		if (error != null && error.hasBlock()) {
			extra = " block=" + error.blockX + "," + error.blockY + "," + error.blockZ;
		}
		append("LAY_FAIL " + reason + extra);
	}

	public static void dig(String player, DigResult result) {
		if (result == null) {
			return;
		}
		StringBuilder line = new StringBuilder("DIG player=").append(player)
				.append(" kind=").append(result.kind);
		if (result.deletedId != null) {
			line.append(" deleted=").append(result.deletedId);
		}
		if (result.kept != null) {
			line.append(" kept=").append(result.kept.getId());
		}
		if (result.tail != null) {
			line.append(" tail=").append(result.tail.getId());
		}
		append(line.toString());
	}

	public static void delete(String player, UUID id, boolean ok) {
		append("DELETE player=" + player + " id=" + id + " ok=" + ok);
	}

	public static void spline(
			UUID id,
			String world,
			boolean loop,
			double length,
			int samples,
			double fx, double fy, double fz,
			double lx, double ly, double lz) {
		append("SPLINE id=" + id
				+ " world=" + world
				+ " loop=" + loop
				+ " length=" + fmt(length)
				+ " n=" + samples
				+ " first=" + xyz(fx, fy, fz)
				+ " last=" + xyz(lx, ly, lz));
	}

	public static void pt(UUID splineId, double s, double x, double y, double z) {
		append("PT spline=" + splineId + " s=" + fmt(s) + " " + xyz(x, y, z));
	}

	public static void junction(
			UUID id,
			UUID stem,
			double s,
			double x, double y, double z,
			boolean hasFrog,
			String side,
			int facing,
			UUID branch,
			double branchLen,
			double bx, double bz,
			boolean hasBranch,
			boolean thrown) {
		append("JUNCTION id=" + id
				+ " stem=" + stem
				+ " s=" + fmt(s)
				+ " frog=" + (hasFrog ? xyz(x, y, z) : "-")
				+ " side=" + side
				+ " facing=" + facing
				+ " thrown=" + thrown
				+ " branch=" + branch
				+ " branchLen=" + (hasBranch ? fmt(branchLen) : "-")
				+ " branchTip=" + (hasBranch ? fmt(bx) + "," + fmt(bz) : "-"));
	}

	public static void junctionDrop(UUID id, UUID stem, String reason) {
		append("JUNCTION_DROP id=" + id + " stem=" + stem + " reason=" + reason);
	}

	public static void junctionStart(String player, UUID stemId, double s) {
		append("JUNCTION_START player=" + player
				+ " stem=" + stemId
				+ " s=" + fmt(s));
	}

	public static void junctionBranch(TrackSpline branch, UUID stemId, UUID junctionId) {
		if (branch == null) {
			return;
		}
		append("JUNCTION_BRANCH how=branch id=" + branch.getId()
				+ " stem=" + stemId
				+ " junction=" + junctionId
				+ " length=" + fmt(branch.length()));
	}

	private static String xyz(double x, double y, double z) {
		return fmt(x) + "," + fmt(y) + "," + fmt(z);
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
				LOGGER.warning("Failed to write track.log: " + e.getMessage());
			}
		}
	}
}
