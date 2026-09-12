package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TrackCurve {
	public static final double STRAIGHT_EPS = 0.02;
	public static final double DEFAULT_DESIRED_GRADE = 10;
	public static final double DEFAULT_MAX_GRADE = 15;

	private TrackCurve() {
	}

	public static List<double[]> between(
			double ax, double ay, double az,
			double bx, double by, double bz,
			double minDistance,
			double desiredGradeDegrees,
			double maxGradeDegrees,
			double step) throws TrackLayException {
		ensureMinDistance(ax, ay, az, bx, by, bz, minDistance);
		double horiz = Math.hypot(bx - ax, bz - az);
		List<double[]> points;
		if (horiz < 1e-6) {
			points = new ArrayList<>();
			points.add(new double[] {ax, ay, az});
			points.add(new double[] {bx, ay, bz});
		} else {
			points = TrackGenerate.densify(ax, ay, az, bx, ay, bz, step);
		}
		TrackGrade.apply(points, ay, by, desiredGradeDegrees, maxGradeDegrees);
		return points;
	}

	public static List<double[]> lay(
			double ax, double ay, double az,
			float startYaw,
			double bx, double by, double bz,
			double minDistance,
			double maxTurnDegrees,
			double step) throws TrackLayException {
		return lay(ax, ay, az, startYaw, bx, by, bz,
				minDistance, maxTurnDegrees, DEFAULT_DESIRED_GRADE, DEFAULT_MAX_GRADE, step);
	}

	public static List<double[]> lay(
			double ax, double ay, double az,
			float startYaw,
			double bx, double by, double bz,
			double minDistance,
			double maxTurnDegrees,
			double desiredGradeDegrees,
			double maxGradeDegrees,
			double step) throws TrackLayException {
		ensureMinDistance(ax, ay, az, bx, by, bz, minDistance);
		double dx = bx - ax;
		double dz = bz - az;
		double yawRad = Math.toRadians(startYaw);
		double tx = -Math.sin(yawRad);
		double tz = Math.cos(yawRad);
		double horiz = Math.hypot(dx, dz);
		if (horiz < 1e-6) {
			List<double[]> vertical = new ArrayList<>();
			vertical.add(new double[] {ax, ay, az});
			vertical.add(new double[] {bx, ay, bz});
			TrackGrade.apply(vertical, ay, by, desiredGradeDegrees, maxGradeDegrees);
			return vertical;
		}
		List<double[]> points;
		double along = dx * tx + dz * tz;
		double cross = tx * dz - tz * dx;
		if (Math.abs(cross) <= STRAIGHT_EPS * horiz) {
			if (along < 0) {
				throw new TrackLayException(
						"Turn is too sharp: the end is behind the track heading. Pick an end ahead of the current track.");
			}
			points = TrackGenerate.densify(ax, ay, az, bx, ay, bz, step);
		} else {
			points = arcXz(ax, ay, az, bx, bz, tx, tz, cross, horiz, maxTurnDegrees, step);
		}
		TrackGrade.apply(points, ay, by, desiredGradeDegrees, maxGradeDegrees);
		return points;
	}

	private static List<double[]> arcXz(
			double ax, double ay, double az,
			double bx, double bz,
			double tx, double tz,
			double cross, double horiz,
			double maxTurnDegrees,
			double step) throws TrackLayException {
		double vDotN = cross;
		double radius = (horiz * horiz) / (2.0 * vDotN);
		double nx = -tz;
		double nz = tx;
		if (radius < 0) {
			radius = -radius;
			nx = -nx;
			nz = -nz;
		}
		double cx = ax + nx * radius;
		double cz = az + nz * radius;
		double pox = ax - cx;
		double poz = az - cz;
		double qox = bx - cx;
		double qoz = bz - cz;
		double angle = Math.atan2(pox * qoz - poz * qox, pox * qox + poz * qoz);
		double absDeg = Math.abs(Math.toDegrees(angle));
		if (absDeg > maxTurnDegrees + 1e-6) {
			throw new TrackLayException("Turn is too sharp (heading change "
					+ format(absDeg)
					+ " degrees, max "
					+ format(maxTurnDegrees)
					+ "). Lay a longer or gentler curve.");
		}
		double arcLen = radius * Math.abs(angle);
		double increment = Math.max(1e-6, step);
		int n = Math.max(1, (int) Math.floor(arcLen / increment));
		List<double[]> points = new ArrayList<>();
		for (int i = 0; i <= n; i++) {
			double t = (i * increment) / arcLen;
			if (t > 1) {
				t = 1;
			}
			double a = angle * t;
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			double rx = pox * cos - poz * sin;
			double rz = pox * sin + poz * cos;
			points.add(new double[] {cx + rx, ay, cz + rz});
		}
		double[] last = points.get(points.size() - 1);
		if (Math.hypot(last[0] - bx, last[2] - bz) > 1e-4) {
			points.add(new double[] {bx, ay, bz});
		}
		return points;
	}

	private static void ensureMinDistance(
			double ax, double ay, double az,
			double bx, double by, double bz,
			double minDistance) throws TrackLayException {
		double dist = Math.sqrt(
				(bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
		if (dist < minDistance) {
			double remain = minDistance - dist;
			throw new TrackLayException("Need at least "
					+ format(minDistance)
					+ " blocks (you are "
					+ format(dist)
					+ ", "
					+ format(remain)
					+ " more).");
		}
	}

	private static String format(double value) {
		return String.format(Locale.US, "%.1f", value);
	}
}
