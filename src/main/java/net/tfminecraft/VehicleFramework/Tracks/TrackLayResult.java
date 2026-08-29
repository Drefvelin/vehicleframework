package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.List;

public final class TrackLayResult {
	public enum Kind {
		NEW,
		APPEND,
		PREPEND,
		CONNECT
	}

	public final Kind kind;
	public final TrackSpline spline;
	public final List<double[]> stroke;
	public final int previousCount;

	private TrackLayResult(Kind kind, TrackSpline spline, List<double[]> stroke, int previousCount) {
		this.kind = kind;
		this.spline = spline;
		this.stroke = stroke == null ? List.of() : copyPoints(stroke);
		this.previousCount = Math.max(0, previousCount);
	}

	public static TrackLayResult of(Kind kind, TrackSpline spline) {
		return of(kind, spline, List.of(), 0);
	}

	public static TrackLayResult of(Kind kind, TrackSpline spline, List<double[]> stroke, int previousCount) {
		return new TrackLayResult(kind, spline, stroke, previousCount);
	}

	public TrackSpline spline() {
		return spline;
	}

	public boolean sequential() {
		return kind == Kind.NEW || kind == Kind.APPEND || kind == Kind.PREPEND;
	}

	public List<double[]> keepPoints() {
		if (spline == null || previousCount <= 0) {
			return List.of();
		}
		List<double[]> xyz = spline.xyz();
		if (kind == Kind.APPEND) {
			int end = Math.min(previousCount, xyz.size());
			if (end < 2) {
				return List.of();
			}
			return copyPoints(xyz.subList(0, end));
		}
		if (kind == Kind.PREPEND) {
			int origin = Math.max(0, stroke.size() - 1);
			if (origin + 1 >= xyz.size()) {
				return List.of();
			}
			return copyPoints(xyz.subList(origin, xyz.size()));
		}
		return List.of();
	}

	private static List<double[]> copyPoints(List<double[]> points) {
		List<double[]> out = new ArrayList<>();
		for (double[] p : points) {
			out.add(new double[] {p[0], p[1], p[2]});
		}
		return out;
	}
}
