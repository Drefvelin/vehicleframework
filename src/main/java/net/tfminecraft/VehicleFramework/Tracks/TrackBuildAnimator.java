package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.VehicleFramework;

public final class TrackBuildAnimator {
	private static final Map<UUID, Job> jobs = new ConcurrentHashMap<>();

	private TrackBuildAnimator() {
	}

	public static boolean isBuilding(UUID splineId) {
		return splineId != null && jobs.containsKey(splineId);
	}

	public static boolean sequential(Player player) {
		if (player == null) {
			return false;
		}
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return false;
		}
		return Cache.trackBuildIntervalTicks > 0;
	}

	public static void start(Player player, TrackSpline spline, List<double[]> keep, List<double[]> stroke) {
		if (spline == null) {
			return;
		}
		cancel(spline.getId());
		List<double[]> strokeCopy = copy(stroke);
		if (strokeCopy.size() < 2) {
			TrackDisplayManager displays = VehicleFramework.getTrackDisplayManager();
			if (displays != null) {
				displays.rebakeSpline(spline.getId());
			}
			return;
		}
		Job job = new Job(
				spline.getId(),
				player == null ? null : player.getUniqueId(),
				copy(keep),
				strokeCopy);
		jobs.put(spline.getId(), job);
		job.seedKeep();
		TrackDisplayManager displays = VehicleFramework.getTrackDisplayManager();
		if (displays != null) {
			job.sync(displays, null);
			job.fx();
		}
	}

	public static void tick() {
		if (jobs.isEmpty()) {
			return;
		}
		TrackDisplayManager displays = VehicleFramework.getTrackDisplayManager();
		Iterator<Job> it = jobs.values().iterator();
		while (it.hasNext()) {
			Job job = it.next();
			job.ticksLeft--;
			if (job.ticksLeft > 0) {
				continue;
			}
			job.ticksLeft = Math.max(1, Cache.trackBuildIntervalTicks);
			if (job.revealed >= job.stroke.size()) {
				it.remove();
				continue;
			}
			job.revealed = nextReveal(job);
			if (displays != null) {
				job.sync(displays, null);
			}
			job.fx();
			if (job.revealed >= job.stroke.size()) {
				it.remove();
			}
		}
	}

	public static void spawnIntoChunk(TrackSpline spline, Chunk chunk) {
		Job job = jobs.get(spline.getId());
		if (job == null) {
			return;
		}
		TrackDisplayManager displays = VehicleFramework.getTrackDisplayManager();
		if (displays != null) {
			job.sync(displays, chunk);
		}
	}

	public static void cancel(UUID splineId) {
		if (splineId != null) {
			jobs.remove(splineId);
		}
	}

	public static void finish(UUID splineId) {
		cancel(splineId);
	}

	public static void finishAll() {
		for (UUID id : List.copyOf(jobs.keySet())) {
			finish(id);
		}
	}

	private static int nextReveal(Job job) {
		int next = job.revealed + 1;
		return Math.min(job.stroke.size(), next);
	}

	private static List<double[]> copy(List<double[]> points) {
		List<double[]> out = new ArrayList<>();
		if (points == null) {
			return out;
		}
		for (double[] p : points) {
			out.add(new double[] {p[0], p[1], p[2]});
		}
		return out;
	}

	private static final class Job {
		private final UUID splineId;
		private final UUID playerId;
		private final List<double[]> keep;
		private final List<double[]> stroke;
		private List<TrackVisual> baked = List.of();
		private int revealed;
		private int ticksLeft;

		private Job(UUID splineId, UUID playerId, List<double[]> keep, List<double[]> stroke) {
			this.splineId = splineId;
			this.playerId = playerId;
			this.keep = keep;
			this.stroke = stroke;
			this.revealed = 2;
			this.ticksLeft = Math.max(1, Cache.trackBuildIntervalTicks);
		}

		private void seedKeep() {
			if (joinsAtKeepEnd() && keep.size() >= 2) {
				baked = bakePoints(keep);
				return;
			}
			baked = List.of();
		}

		private void sync(TrackDisplayManager displays, Chunk chunk) {
			World world = chunk != null ? chunk.getWorld() : world();
			if (world == null) {
				return;
			}
			List<TrackVisual> next = bakePoints(visible());
			if (chunk != null) {
				displays.spawnVisuals(splineId, next, world, chunk);
				baked = next;
				return;
			}
			displays.replaceFrom(splineId, baked, next, world, null);
			baked = next;
		}

		private List<double[]> visible() {
			int shown = Math.min(stroke.size(), Math.max(2, revealed));
			if (keep.isEmpty()) {
				return stroke.subList(0, shown);
			}
			if (joinsAtKeepEnd()) {
				return concat(keep, stroke.subList(1, shown));
			}
			return stroke.subList(0, shown);
		}

		private static List<TrackVisual> bakePoints(List<double[]> points) {
			if (points == null || points.size() < 2) {
				return List.of();
			}
			return TrackSpline.fromPoints(UUID.randomUUID(), "world", false, points).visuals();
		}

		private boolean joinsAtKeepEnd() {
			if (keep.isEmpty() || stroke.size() < 2) {
				return false;
			}
			double[] a = keep.get(keep.size() - 1);
			double[] b = stroke.get(0);
			double dx = a[0] - b[0];
			double dy = a[1] - b[1];
			double dz = a[2] - b[2];
			return dx * dx + dy * dy + dz * dz < 0.01;
		}

		private static List<double[]> concat(List<double[]> a, List<double[]> b) {
			List<double[]> out = new ArrayList<>(a);
			out.addAll(b);
			return out;
		}

		private void fx() {
			double[] p = stroke.get(Math.min(revealed, stroke.size()) - 1);
			World world = world();
			if (world == null) {
				return;
			}
			float yaw = 0;
			int at = Math.min(revealed, stroke.size()) - 1;
			if (at >= 1) {
				double[] a = stroke.get(at - 1);
				yaw = (float) Math.toDegrees(Math.atan2(-(p[0] - a[0]), p[2] - a[2]));
			}
			TrackFx.place(world, new TrackPose(p[0], p[1], p[2], yaw, 0));
			if (!Cache.trackBuildSwing) {
				return;
			}
			Player player = playerId == null ? null : Bukkit.getPlayer(playerId);
			if (player != null && player.isOnline()) {
				player.swingMainHand();
			}
		}

		private World world() {
			TrackRegistry registry = VehicleFramework.getTrackRegistry();
			if (registry == null) {
				return null;
			}
			TrackSpline spline = registry.get(splineId).orElse(null);
			if (spline == null) {
				return null;
			}
			return Bukkit.getWorld(spline.getWorld());
		}
	}
}
