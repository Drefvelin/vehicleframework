package net.tfminecraft.VehicleFramework.Tracks;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.block.Block;

import net.tfminecraft.VehicleFramework.Cache.Cache;

public final class TrackClearance {
	public static final double OVERLAP_HORIZ = 1.0;
	public static final double OVERLAP_VERT = 1.5;
	public static final double FROG_S = 4.0;

	public static final class FrogIgnore {
		public final UUID splineId;
		public final double s;

		public FrogIgnore(UUID splineId, double s) {
			this.splineId = splineId;
			this.s = s;
		}
	}

	private TrackClearance() {
	}

	public static void check(
			World world,
			List<double[]> points,
			TrackRegistry registry,
			Set<UUID> ignoreSplineIds) throws TrackLayException {
		check(world, points, registry, ignoreSplineIds, null);
	}

	public static void check(
			World world,
			List<double[]> points,
			TrackRegistry registry,
			Set<UUID> ignoreSplineIds,
			FrogIgnore frog) throws TrackLayException {
		if (world == null || points == null || points.size() < 2) {
			return;
		}
		Set<UUID> ignore = ignoreSplineIds == null ? Set.of() : ignoreSplineIds;
		liftOneBlockSteps(world, points);
		for (int i = 0; i < points.size(); i++) {
			double[] p = points.get(i);
			double[] dir = step(points, i);
			double len = Math.hypot(dir[0], dir[1]);
			double rx = 1;
			double rz = 0;
			if (len > 1e-6) {
				double fx = dir[0] / len;
				double fz = dir[1] / len;
				rx = fz;
				rz = -fx;
			}
			int y0 = (int) Math.floor(p[1]);
			for (int side = -1; side <= 1; side++) {
				int x = (int) Math.floor(p[0] + rx * side);
				int z = (int) Math.floor(p[2] + rz * side);
				for (int h = 0; h <= 2; h++) {
					int y = y0 + h;
					Block block = world.getBlockAt(x, y, z);
					if (TrackSupport.blocksRail(block, p[1])) {
						String name = block.getType().name().toLowerCase(Locale.US);
						throw new TrackLayException(
								"Cannot lay track: " + name + " in the way at " + x + ", " + y + ", " + z + ".",
								x, y, z);
					}
				}
			}
		}
		if (registry == null) {
			return;
		}
		checkOverlap(world.getName(), points, registry, ignore, frog);
	}

	static void checkOverlap(
			String worldName,
			List<double[]> points,
			TrackRegistry registry,
			Set<UUID> ignoreSplineIds,
			FrogIgnore frog) throws TrackLayException {
		if (worldName == null || points == null || registry == null) {
			return;
		}
		Set<UUID> ignore = ignoreSplineIds == null ? Set.of() : ignoreSplineIds;
		for (double[] p : points) {
			for (TrackSpline spline : registry.inWorld(worldName)) {
				if (ignore.contains(spline.getId())) {
					continue;
				}
				for (TrackSample sample : spline.getSamples()) {
					if (inFrog(spline, sample, frog)) {
						continue;
					}
					double dx = sample.x - p[0];
					double dz = sample.z - p[2];
					if (Math.hypot(dx, dz) > OVERLAP_HORIZ) {
						continue;
					}
					if (Math.abs(sample.y - p[1]) > OVERLAP_VERT) {
						continue;
					}
					int x = (int) Math.floor(p[0]);
					int y = (int) Math.floor(p[1]);
					int z = (int) Math.floor(p[2]);
					throw new TrackLayException(
							"Cannot lay track: another track in the way at " + x + ", " + y + ", " + z + ".",
							x, y, z);
				}
			}
		}
	}

	private static boolean inFrog(TrackSpline spline, TrackSample sample, FrogIgnore frog) {
		if (frog == null || frog.splineId == null || !frog.splineId.equals(spline.getId())) {
			return false;
		}
		return TrackJunction.arcDistance(sample.s, frog.s, spline.length(), spline.isLoop()) <= FROG_S;
	}

	static void liftOneBlockSteps(World world, List<double[]> points) throws TrackLayException {
		if (world == null || points == null || points.size() < 2) {
			return;
		}
		int n = points.size();
		double[] minY = new double[n];
		boolean any = false;
		for (int i = 0; i < n; i++) {
			double[] p = points.get(i);
			minY[i] = p[1];
			double[] dir = step(points, i);
			double len = Math.hypot(dir[0], dir[1]);
			double rx = len > 1e-6 ? dir[1] / len : 1;
			double rz = len > 1e-6 ? -dir[0] / len : 0;
			int y0 = (int) Math.floor(p[1]);
			boolean stepUp = false;
			Double liftTo = null;
			for (int side = -1; side <= 1; side++) {
				int x = (int) Math.floor(p[0] + rx * side);
				int z = (int) Math.floor(p[2] + rz * side);
				Block at = world.getBlockAt(x, y0, z);
				Double sit = TrackSupport.sitY(at);
				if (sit == null || sit <= p[1] + 1e-4) {
					continue;
				}
				Block above = world.getBlockAt(x, y0 + 1, z);
				if (TrackSupport.sitY(above) != null && TrackSupport.sitY(above) > sit + 1e-4) {
					String name = at.getType().name().toLowerCase(Locale.US);
					throw new TrackLayException(
							"Cannot lay track: " + name + " in the way at " + x + ", " + y0 + ", " + z + ".",
							x, y0, z);
				}
				stepUp = true;
				if (liftTo == null || sit > liftTo) {
					liftTo = sit;
				}
			}
			if (stepUp && liftTo != null) {
				minY[i] = liftTo;
				any = true;
			}
		}
		if (!any) {
			return;
		}
		TrackLog.append("STEP_LIFT applying support steps, maxGrade="
				+ String.format(Locale.US, "%.1f", Cache.trackMaxGradeDegrees));
		TrackGrade.applyRequiredHeights(points, minY, Cache.trackMaxGradeDegrees);
	}

	private static double[] step(List<double[]> points, int i) {
		if (i + 1 < points.size()) {
			double[] a = points.get(i);
			double[] b = points.get(i + 1);
			return new double[] {b[0] - a[0], b[2] - a[2]};
		}
		double[] a = points.get(i - 1);
		double[] b = points.get(i);
		return new double[] {b[0] - a[0], b[2] - a[2]};
	}
}
