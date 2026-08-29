package net.tfminecraft.VehicleFramework.Tracks;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

import org.bukkit.World;

import net.tfminecraft.VehicleFramework.Cache.Cache;

public final class TrackResettle {
	private TrackResettle() {
	}

	public static void resettle(World world, List<double[]> points, boolean floorFirst, boolean floorLast) {
		if (points == null || points.size() < 2) {
			return;
		}
		if (world != null) {
			if (floorFirst) {
				snapTip(world, points.get(0), "first");
			}
			if (floorLast) {
				snapTip(world, points.get(points.size() - 1), "last");
			}
		}
		int n = points.size();
		if (floorFirst) {
			int stopAt = floorLast ? n / 2 - 1 : n - 1;
			smoothInward(points, 0, 1, stopAt, "first", minSitFn(world, points));
		}
		if (floorLast) {
			int stopAt = floorFirst ? n / 2 : 0;
			smoothInward(points, n - 1, -1, stopAt, "last", minSitFn(world, points));
		}
	}

	private static IntFunction<Double> minSitFn(World world, List<double[]> points) {
		if (world == null) {
			return i -> null;
		}
		return i -> TrackSupport.firstSitY(world, points.get(i)[0], points.get(i)[1], points.get(i)[2]);
	}

	private static void snapTip(World world, double[] p, String which) {
		double oldY = p[1];
		double next = TrackSupport.snapY(world, p[0], p[1], p[2]);
		if (Math.abs(next - oldY) < 1e-4) {
			return;
		}
		p[1] = next;
		TrackLog.append("RESETTLE floored=" + which
				+ " oldY=" + String.format(Locale.US, "%.3f", oldY)
				+ " newY=" + String.format(Locale.US, "%.3f", next)
				+ " at=" + String.format(Locale.US, "%.1f,%.1f,%.1f", p[0], next, p[2]));
	}

	static void smoothInward(
			List<double[]> points,
			int tipIndex,
			int step,
			int stopAt,
			String end,
			IntFunction<Double> minSit) {
		int n = points.size();
		double tanMax = Math.tan(Math.toRadians(Math.max(1.0, Cache.trackMaxGradeDegrees)));
		int changed = 0;
		int j = tipIndex;
		int i = tipIndex + step;
		while (i >= 0 && i < n && towardStop(i, stopAt, step)) {
			double[] from = points.get(j);
			double[] to = points.get(i);
			double h = Math.hypot(to[0] - from[0], to[2] - from[2]);
			double allowed = h < 1e-9 ? 0.0 : tanMax * h;
			double lo = from[1] - allowed;
			double hi = from[1] + allowed;
			Double sit = minSit == null ? null : minSit.apply(i);
			boolean inBand = to[1] >= lo - 1e-4 && to[1] <= hi + 1e-4;
			if (inBand && (sit == null || to[1] >= sit - 1e-4)) {
				break;
			}
			double next;
			if (inBand) {
				next = sit;
			} else if (to[1] < lo) {
				next = lo;
			} else {
				next = hi;
			}
			if (sit != null && next < sit - 1e-4) {
				if (sit > hi + 1e-4) {
					break;
				}
				next = sit;
			}
			if (Math.abs(next - to[1]) < 1e-4) {
				break;
			}
			to[1] = next;
			changed++;
			j = i;
			i += step;
		}
		if (changed > 0) {
			TrackLog.append("RESETTLE smooth end=" + end + " changed=" + changed);
		}
	}

	private static boolean towardStop(int i, int stopAt, int step) {
		if (step > 0) {
			return i <= stopAt;
		}
		return i >= stopAt;
	}
}
