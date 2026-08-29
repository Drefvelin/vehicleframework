package net.tfminecraft.VehicleFramework.Tracks;

import java.util.List;
import java.util.Locale;

public final class TrackGrade {
	private TrackGrade() {
	}

	public static void apply(
			List<double[]> points,
			double startY,
			double endY,
			double desiredDegrees,
			double maxDegrees) throws TrackLayException {
		if (points == null || points.size() < 2) {
			throw new TrackLayException("Need at least 2 samples to lay grade.");
		}
		double max = Math.max(1.0, maxDegrees);
		double desired = Math.min(max, Math.max(1.0, desiredDegrees));
		double h = horizontalLength(points);
		double dh = endY - startY;
		if (Math.abs(dh) < 1e-6) {
			setY(points, startY);
			points.get(points.size() - 1)[1] = endY;
			return;
		}
		if (h < 1e-6) {
			throw tooSteep(90.0, max, rampLength(dh, max));
		}
		double rampDesired = rampLength(dh, desired);
		double rampMax = rampLength(dh, max);
		double grade;
		double ramp;
		if (h + 1e-6 >= rampDesired) {
			grade = desired;
			ramp = rampDesired;
		} else if (h + 1e-6 >= rampMax) {
			grade = max;
			ramp = rampMax;
		} else {
			double actual = Math.toDegrees(Math.atan(Math.abs(dh) / h));
			throw tooSteep(actual, max, rampMax - h);
		}
		double sign = dh < 0 ? -1.0 : 1.0;
		double slope = Math.tan(Math.toRadians(grade));
		boolean downhill = dh < 0;
		double s = 0;
		points.get(0)[1] = startY;
		for (int i = 1; i < points.size(); i++) {
			double[] prev = points.get(i - 1);
			double[] cur = points.get(i);
			s += Math.hypot(cur[0] - prev[0], cur[2] - prev[2]);
			if (downhill) {
				if (s <= ramp + 1e-9) {
					cur[1] = startY + sign * s * slope;
				} else {
					cur[1] = endY;
				}
			} else {
				double flatEnd = Math.max(0.0, h - ramp);
				if (s <= flatEnd + 1e-9) {
					cur[1] = startY;
				} else {
					cur[1] = startY + sign * (s - flatEnd) * slope;
				}
			}
		}
		points.get(points.size() - 1)[1] = endY;
	}

	/**
	 * Raises the polyline so it sits on top of required heights without exceeding
	 * {@code maxDegrees}. Used when a 1-block step is in the way of a postponed ramp.
	 */
	public static void applyRequiredHeights(
			List<double[]> points,
			double[] minY,
			double maxDegrees) throws TrackLayException {
		if (points == null || minY == null || points.size() != minY.length || points.size() < 2) {
			return;
		}
		int n = points.size();
		double tanMax = Math.tan(Math.toRadians(Math.max(1.0, maxDegrees)));
		double startY = points.get(0)[1];
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			y[i] = Math.max(points.get(i)[1], minY[i]);
		}
		for (int i = n - 2; i >= 0; i--) {
			double ds = Math.hypot(
					points.get(i + 1)[0] - points.get(i)[0],
					points.get(i + 1)[2] - points.get(i)[2]);
			y[i] = Math.max(y[i], y[i + 1] - tanMax * ds);
		}
		if (y[0] > startY + 1e-3) {
			double remain = (y[0] - startY) / tanMax;
			throw tooSteep(
					Math.toDegrees(Math.atan((y[0] - startY) / Math.max(1e-6, remain))),
					maxDegrees,
					remain);
		}
		y[0] = startY;
		for (int i = 1; i < n; i++) {
			double ds = Math.hypot(
					points.get(i)[0] - points.get(i - 1)[0],
					points.get(i)[2] - points.get(i - 1)[2]);
			double maxReach = y[i - 1] + tanMax * ds;
			if (y[i] > maxReach + 1e-6) {
				throw tooSteep(
						Math.toDegrees(Math.atan((y[i] - y[i - 1]) / Math.max(1e-6, ds))),
						maxDegrees,
						(y[i] - y[i - 1]) / tanMax - ds);
			}
			double minReach = y[i - 1] - tanMax * ds;
			if (y[i] < minReach) {
				y[i] = minReach;
			}
		}
		for (int i = 0; i < n; i++) {
			points.get(i)[1] = y[i];
		}
	}

	public static double horizontalLength(List<double[]> points) {
		double h = 0;
		for (int i = 1; i < points.size(); i++) {
			double[] a = points.get(i - 1);
			double[] b = points.get(i);
			h += Math.hypot(b[0] - a[0], b[2] - a[2]);
		}
		return h;
	}

	private static void setY(List<double[]> points, double y) {
		for (double[] p : points) {
			p[1] = y;
		}
	}

	private static double rampLength(double dh, double degrees) {
		return Math.abs(dh) / Math.tan(Math.toRadians(degrees));
	}

	private static TrackLayException tooSteep(double actual, double max, double remain) {
		double run = 1.0 / Math.tan(Math.toRadians(max));
		return new TrackLayException("Slope is too steep (incline "
				+ format(actual)
				+ " degrees, max "
				+ format(max)
				+ ", about 1 in "
				+ format(run)
				+ "). Need about "
				+ format(Math.max(0, remain))
				+ " more blocks of run, or a lower end.");
	}

	private static String format(double value) {
		return String.format(Locale.US, "%.1f", value);
	}
}
