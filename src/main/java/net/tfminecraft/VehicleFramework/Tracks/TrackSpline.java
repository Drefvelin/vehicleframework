package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Cache.Cache;

public final class TrackSpline {
	private final UUID id;
	private final String world;
	private final boolean loop;
	private final List<TrackSample> samples;
	private final List<TrackSegment> segments;
	private List<TrackVisual> visualCache;

	public TrackSpline(UUID id, String world, boolean loop, List<TrackSample> samples, List<TrackSegment> segments) {
		if (samples == null || samples.size() < 2) {
			throw new IllegalArgumentException("spline needs at least 2 samples");
		}
		this.id = id == null ? UUID.randomUUID() : id;
		this.world = world == null ? "" : world;
		this.loop = loop;
		this.samples = List.copyOf(samples);
		int expected = edgeCount(this.samples.size(), loop);
		List<TrackSegment> segs = segments == null ? defaultSegments(expected) : new ArrayList<>(segments);
		while (segs.size() < expected) {
			segs.add(new TrackSegment(segs.size(), false, 1.0));
		}
		if (segs.size() > expected) {
			segs = segs.subList(0, expected);
		}
		this.segments = List.copyOf(segs);
	}

	public static TrackSpline fromPoints(UUID id, String world, boolean loop, List<double[]> xyz) {
		if (xyz == null || xyz.size() < 2) {
			throw new IllegalArgumentException("spline needs at least 2 samples");
		}
		List<TrackSample> samples = new ArrayList<>();
		double s = 0;
		for (int i = 0; i < xyz.size(); i++) {
			double[] p = xyz.get(i);
			double[] dir;
			if (i + 1 < xyz.size()) {
				dir = delta(p, xyz.get(i + 1));
			} else if (loop) {
				dir = delta(p, xyz.get(0));
			} else {
				dir = delta(xyz.get(i - 1), p);
			}
			float yaw = yawFromDelta(dir[0], dir[2]);
			float pitch = pitchFromDelta(dir[0], dir[1], dir[2]);
			samples.add(new TrackSample(p[0], p[1], p[2], yaw, pitch, s));
			if (i + 1 < xyz.size()) {
				s += length(dir);
			}
		}
		return new TrackSpline(id, world, loop, samples, defaultSegments(edgeCount(samples.size(), loop)));
	}

	public static boolean shouldLoop(List<double[]> xyz, double joinDistance) {
		if (xyz == null || xyz.size() < 3 || joinDistance < 0) {
			return false;
		}
		double[] a = xyz.get(0);
		double[] b = xyz.get(xyz.size() - 1);
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz) <= joinDistance;
	}

	public TrackSpline promotedLoop(double joinDistance) {
		if (loop || samples.size() < 3) {
			return this;
		}
		if (dist(first(), last()) > joinDistance) {
			return this;
		}
		return new TrackSpline(id, world, true, samples, segments);
	}

	public List<TrackVisual> visuals() {
		if (visualCache == null) {
			visualCache = TrackVisualBake.bake(this);
		}
		return visualCache;
	}

	public List<double[]> xyz() {
		List<double[]> out = new ArrayList<>();
		for (TrackSample sample : samples) {
			out.add(new double[] {sample.x, sample.y, sample.z});
		}
		return out;
	}

	public TrackSample first() {
		return samples.get(0);
	}

	public TrackSample last() {
		return samples.get(samples.size() - 1);
	}

	public UUID getId() {
		return id;
	}

	public String getWorld() {
		return world;
	}

	public boolean isLoop() {
		return loop;
	}

	public List<TrackSample> getSamples() {
		return samples;
	}

	public List<TrackSegment> getSegments() {
		return segments;
	}

	public double length() {
		if (!loop) {
			return samples.get(samples.size() - 1).s;
		}
		TrackSample last = samples.get(samples.size() - 1);
		TrackSample first = samples.get(0);
		return last.s + dist(last, first);
	}

	public TrackPose sampleAt(double s) {
		double len = length();
		if (len <= 1e-12) {
			TrackSample a = samples.get(0);
			return new TrackPose(a.x, a.y, a.z, a.yaw, a.pitch);
		}
		s = normalizeS(s, len);
		int edges = edgeCount();
		for (int i = 0; i < edges; i++) {
			double a = edgeStartS(i);
			double b = edgeEndS(i);
			if (s + 1e-12 >= a && s <= b + 1e-12) {
				double span = b - a;
				double t = span < 1e-12 ? 0 : (s - a) / span;
				return lerpEdge(i, t);
			}
		}
		return lerpEdge(edges - 1, 1);
	}

	public TrackAdvance advance(double s, double ds) {
		double len = length();
		if (len <= 1e-12 || Math.abs(ds) <= 1e-12) {
			return new TrackAdvance(normalizeS(s, len), false);
		}
		double from = normalizeS(s, len);
		double unconstrained = from + ds;
		double to;
		if (loop) {
			to = wrapS(unconstrained, len);
		} else {
			to = clamp(unconstrained, 0, len);
		}

		if (ds > 0) {
			Double stop = firstBrokenAhead(from, to);
			if (stop != null) {
				return new TrackAdvance(stop, true);
			}
		} else {
			Double stop = firstBrokenBehind(from, to);
			if (stop != null) {
				return new TrackAdvance(stop, true);
			}
		}
		return new TrackAdvance(to, false);
	}

	public double nearestS(double x, double y, double z) {
		double bestS = 0;
		double bestD = Double.POSITIVE_INFINITY;
		int edges = edgeCount();
		for (int i = 0; i < edges; i++) {
			TrackSample a = edgeStart(i);
			TrackSample b = edgeEndSample(i);
			double[] proj = project(x, y, z, a, b);
			double d = distSq(x, y, z, proj[0], proj[1], proj[2]);
			if (d < bestD) {
				bestD = d;
				double span = edgeEndS(i) - edgeStartS(i);
				bestS = edgeStartS(i) + span * proj[3];
			}
		}
		return bestS;
	}

	public TrackSegment segment(int fromIndex) {
		for (TrackSegment segment : segments) {
			if (segment.fromIndex == fromIndex) {
				return segment;
			}
		}
		return new TrackSegment(fromIndex, false, 1.0);
	}

	public int edgeCount() {
		return edgeCount(samples.size(), loop);
	}

	public static int edgeCount(int sampleCount, boolean loop) {
		return loop ? sampleCount : sampleCount - 1;
	}

	public TrackSpline withSegment(int fromIndex, TrackSegment segment) {
		List<TrackSegment> next = new ArrayList<>(segments);
		for (int i = 0; i < next.size(); i++) {
			if (next.get(i).fromIndex == fromIndex) {
				next.set(i, segment);
				break;
			}
		}
		return new TrackSpline(id, world, loop, samples, next);
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject root = new JSONObject();
		root.put("id", id.toString());
		root.put("world", world);
		root.put("loop", loop);
		JSONArray sampleArr = new JSONArray();
		for (TrackSample sample : samples) {
			JSONObject o = new JSONObject();
			o.put("x", sample.x);
			o.put("y", sample.y);
			o.put("z", sample.z);
			o.put("yaw", (double) sample.yaw);
			o.put("pitch", (double) sample.pitch);
			o.put("s", sample.s);
			sampleArr.add(o);
		}
		root.put("samples", sampleArr);
		JSONArray segArr = new JSONArray();
		for (TrackSegment segment : segments) {
			JSONObject o = new JSONObject();
			o.put("fromIndex", (long) segment.fromIndex);
			o.put("broken", segment.broken);
			o.put("health", segment.health);
			segArr.add(o);
		}
		root.put("segments", segArr);
		return root;
	}

	public static TrackSpline fromJson(JSONObject root) {
		if (root == null) {
			throw new IllegalArgumentException("json is null");
		}
		UUID id = parseUuid(root.get("id"));
		String world = root.get("world") == null ? "" : String.valueOf(root.get("world"));
		boolean loop = root.get("loop") instanceof Boolean b && b;
		JSONArray sampleArr = (JSONArray) root.get("samples");
		if (sampleArr == null || sampleArr.size() < 2) {
			throw new IllegalArgumentException("spline needs at least 2 samples");
		}
		List<TrackSample> loaded = new ArrayList<>();
		boolean missingS = false;
		for (Object raw : sampleArr) {
			JSONObject o = (JSONObject) raw;
			double x = asDouble(o.get("x"));
			double y = asDouble(o.get("y"));
			double z = asDouble(o.get("z"));
			float yaw = (float) asDouble(o.get("yaw"));
			float pitch = (float) asDouble(o.get("pitch"));
			double s = 0;
			if (o.containsKey("s") && o.get("s") != null) {
				s = asDouble(o.get("s"));
			} else {
				missingS = true;
			}
			loaded.add(new TrackSample(x, y, z, yaw, pitch, s));
		}
		if (missingS) {
			loaded = recomputeS(loaded);
		}
		JSONArray segArr = (JSONArray) root.get("segments");
		List<TrackSegment> segs = new ArrayList<>();
		if (segArr != null) {
			for (Object raw : segArr) {
				JSONObject o = (JSONObject) raw;
				int from = (int) asDouble(o.get("fromIndex"));
				boolean broken = o.get("broken") instanceof Boolean b && b;
				double health = o.get("health") == null ? 1.0 : asDouble(o.get("health"));
				segs.add(new TrackSegment(from, broken, health));
			}
		}
		return new TrackSpline(id, world, loop, loaded, segs).promotedLoop(Cache.trackJoinDistance);
	}

	public void invalidateVisuals() {
		visualCache = null;
	}

	private Double firstBrokenAhead(double from, double to) {
		int edges = edgeCount();
		boolean wrap = loop && to + 1e-12 < from - 1e-9;
		Double stop = null;
		for (int i = 0; i < edges; i++) {
			if (!segmentBroken(i)) {
				continue;
			}
			double start = edgeStartS(i);
			boolean crosses;
			if (wrap) {
				crosses = start + 1e-12 > from || start <= to + 1e-12;
			} else {
				crosses = start > from + 1e-12 && start <= to + 1e-12;
			}
			if (!crosses) {
				continue;
			}
			if (stop == null) {
				stop = start;
			} else if (wrap) {
				if (start > from && (stop <= to || start < stop)) {
					stop = start;
				}
			} else if (start < stop) {
				stop = start;
			}
		}
		return stop;
	}

	private Double firstBrokenBehind(double from, double to) {
		int edges = edgeCount();
		boolean wrap = loop && to > from + 1e-9;
		double best = Double.NaN;
		for (int i = 0; i < edges; i++) {
			if (!segmentBroken(i)) {
				continue;
			}
			double end = edgeEndS(i);
			if (wrap) {
				if (end < from - 1e-12 || end + 1e-12 >= to) {
					if (Double.isNaN(best) || end > best) {
						best = end;
					}
				}
			} else if (end < from - 1e-12 && end + 1e-12 >= to) {
				if (Double.isNaN(best) || end > best) {
					best = end;
				}
			}
		}
		if (!Double.isNaN(best)) {
			return best;
		}
		return null;
	}

	private boolean segmentBroken(int edgeIndex) {
		for (TrackSegment segment : segments) {
			if (segment.fromIndex == edgeIndex) {
				return segment.broken;
			}
		}
		return false;
	}

	private double edgeStartS(int edgeIndex) {
		if (edgeIndex < samples.size() - 1 || !loop) {
			return samples.get(edgeIndex).s;
		}
		return samples.get(samples.size() - 1).s;
	}

	private double edgeEndS(int edgeIndex) {
		if (edgeIndex < samples.size() - 1) {
			return samples.get(edgeIndex + 1).s;
		}
		if (loop) {
			return length();
		}
		return samples.get(samples.size() - 1).s;
	}

	private TrackSample edgeStart(int edgeIndex) {
		if (edgeIndex < samples.size()) {
			return samples.get(Math.min(edgeIndex, samples.size() - 1));
		}
		return samples.get(samples.size() - 1);
	}

	private TrackSample edgeEndSample(int edgeIndex) {
		if (edgeIndex < samples.size() - 1) {
			return samples.get(edgeIndex + 1);
		}
		if (loop) {
			return samples.get(0);
		}
		return samples.get(samples.size() - 1);
	}

	private TrackPose lerpEdge(int edgeIndex, double t) {
		t = clamp(t, 0, 1);
		TrackSample a = edgeStart(edgeIndex);
		TrackSample b = edgeEndSample(edgeIndex);
		float yaw = ConvertedAngle.wrapDegrees(a.yaw + ConvertedAngle.shortestDelta(a.yaw, b.yaw) * (float) t);
		float pitch = a.pitch + (b.pitch - a.pitch) * (float) t;
		return new TrackPose(
				a.x + (b.x - a.x) * t,
				a.y + (b.y - a.y) * t,
				a.z + (b.z - a.z) * t,
				yaw,
				pitch);
	}

	private double normalizeS(double s, double len) {
		if (len <= 1e-12) {
			return 0;
		}
		if (loop) {
			return wrapS(s, len);
		}
		return clamp(s, 0, len);
	}

	private static double wrapS(double s, double len) {
		s = s % len;
		if (s < 0) {
			s += len;
		}
		return s;
	}

	private static List<TrackSegment> defaultSegments(int count) {
		List<TrackSegment> segs = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			segs.add(new TrackSegment(i, false, 1.0));
		}
		return segs;
	}

	private static List<TrackSample> recomputeS(List<TrackSample> loaded) {
		List<TrackSample> out = new ArrayList<>();
		double s = 0;
		for (int i = 0; i < loaded.size(); i++) {
			TrackSample p = loaded.get(i);
			out.add(new TrackSample(p.x, p.y, p.z, p.yaw, p.pitch, s));
			if (i + 1 < loaded.size()) {
				TrackSample n = loaded.get(i + 1);
				s += dist(p, n);
			}
		}
		return out;
	}

	private static float yawFromDelta(double dx, double dz) {
		return (float) Math.toDegrees(Math.atan2(-dx, dz));
	}

	private static float pitchFromDelta(double dx, double dy, double dz) {
		double horiz = Math.hypot(dx, dz);
		return (float) Math.toDegrees(Math.atan2(-dy, horiz));
	}

	private static double[] delta(double[] a, double[] b) {
		return new double[] {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
	}

	private static double length(double[] d) {
		return Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
	}

	private static double dist(TrackSample a, TrackSample b) {
		return Math.sqrt(distSq(a.x, a.y, a.z, b.x, b.y, b.z));
	}

	private static double distSq(double ax, double ay, double az, double bx, double by, double bz) {
		double dx = ax - bx;
		double dy = ay - by;
		double dz = az - bz;
		return dx * dx + dy * dy + dz * dz;
	}

	private static double[] project(double x, double y, double z, TrackSample a, TrackSample b) {
		double abx = b.x - a.x;
		double aby = b.y - a.y;
		double abz = b.z - a.z;
		double len2 = abx * abx + aby * aby + abz * abz;
		double t = 0;
		if (len2 > 1e-12) {
			t = ((x - a.x) * abx + (y - a.y) * aby + (z - a.z) * abz) / len2;
			t = clamp(t, 0, 1);
		}
		return new double[] {a.x + abx * t, a.y + aby * t, a.z + abz * t, t};
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	private static UUID parseUuid(Object raw) {
		if (raw == null) {
			return UUID.randomUUID();
		}
		try {
			return UUID.fromString(String.valueOf(raw));
		} catch (IllegalArgumentException e) {
			return UUID.randomUUID();
		}
	}

	private static double asDouble(Object raw) {
		if (raw instanceof Number n) {
			return n.doubleValue();
		}
		if (raw == null) {
			return 0;
		}
		return Double.parseDouble(String.valueOf(raw));
	}
}
