package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TrackGenerate {
	public static final double STEP = 1.0;

	private TrackGenerate() {
	}

	public static TrackSpline between(
			UUID id,
			String world,
			double ax, double ay, double az,
			double bx, double by, double bz) {
		return TrackSpline.fromPoints(id, world, false, densify(ax, ay, az, bx, by, bz, STEP));
	}

	public static List<double[]> densify(
			double ax, double ay, double az,
			double bx, double by, double bz,
			double step) {
		double dx = bx - ax;
		double dy = by - ay;
		double dz = bz - az;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist < 1e-6) {
			throw new IllegalArgumentException("anchors are too close");
		}
		double increment = Math.max(1e-6, step);
		List<double[]> points = new ArrayList<>();
		int n = (int) Math.floor(dist / increment);
		for (int i = 0; i <= n; i++) {
			double t = (i * increment) / dist;
			if (t > 1) {
				t = 1;
			}
			points.add(new double[] {ax + dx * t, ay + dy * t, az + dz * t});
		}
		double[] last = points.get(points.size() - 1);
		if (Math.hypot(Math.hypot(last[0] - bx, last[1] - by), last[2] - bz) > 1e-6) {
			points.add(new double[] {bx, by, bz});
		}
		return points;
	}
}
