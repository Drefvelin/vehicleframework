package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class ThrottleTape {
	public static final int MAX_SAMPLES = 2000;
	public static final double HOLD_S = 1.0;

	public static final class Sample {
		public final double s;
		public final int sign;
		public final int throttle;
		public int holdTicks;
		public final String splineId;
		public final String junctionId;

		public Sample(double s, int sign, int throttle) {
			this(s, sign, throttle, 1, null, null);
		}

		public Sample(double s, int sign, int throttle, int holdTicks) {
			this(s, sign, throttle, holdTicks, null, null);
		}

		public Sample(double s, int sign, int throttle, int holdTicks, String splineId, String junctionId) {
			this.s = s;
			this.sign = sign < 0 ? -1 : 1;
			this.throttle = throttle;
			this.holdTicks = Math.max(1, holdTicks);
			this.splineId = blankToNull(splineId);
			this.junctionId = blankToNull(junctionId);
		}

		public String resolvedSpline(String origin) {
			return splineId == null ? origin : splineId;
		}
	}

	public static final class DwellState {
		public int left;
		public Double atS;
	}

	public enum AppendResult {
		HELD, ADDED, CAPPED
	}

	private final String splineId;
	private final List<Sample> samples;

	public ThrottleTape(String splineId) {
		this(splineId, List.of());
	}

	public ThrottleTape(String splineId, List<Sample> samples) {
		this.splineId = splineId == null ? "" : splineId;
		this.samples = new ArrayList<>(samples == null ? List.of() : samples);
	}

	public String getSplineId() {
		return splineId;
	}

	public boolean isEmpty() {
		return samples.isEmpty();
	}

	public boolean matchesSpline(UUID id) {
		return id != null && id.toString().equalsIgnoreCase(splineId);
	}

	public boolean takesJunction(UUID junctionId) {
		if (junctionId == null) {
			return false;
		}
		String want = junctionId.toString();
		for (Sample sample : samples) {
			if (want.equalsIgnoreCase(sample.junctionId)) {
				return true;
			}
		}
		return false;
	}

	public List<Sample> getSamples() {
		return List.copyOf(samples);
	}

	public int lookup(double s, int sign) {
		return lookup(s, sign, splineId);
	}

	public int lookup(double s, int sign, UUID currentSpline) {
		return lookup(s, sign, currentSpline == null ? splineId : currentSpline.toString());
	}

	public int lookup(double s, int sign, String currentSpline) {
		return lookup(samplesFor(currentSpline), s, sign);
	}

	static int lookup(List<Sample> samples, double s, int sign) {
		if (samples == null || samples.isEmpty()) {
			return 0;
		}
		int want = sign < 0 ? -1 : 1;
		List<Sample> matched = new ArrayList<>();
		for (Sample sample : samples) {
			if (sample.sign == want) {
				matched.add(sample);
			}
		}
		if (matched.isEmpty()) {
			return 0;
		}
		matched.sort(Comparator.comparingDouble(a -> a.s));
		if (s <= matched.get(0).s) {
			return matched.get(0).throttle;
		}
		Sample last = matched.get(matched.size() - 1);
		if (s >= last.s) {
			return last.throttle;
		}
		for (int i = 0; i < matched.size() - 1; i++) {
			Sample a = matched.get(i);
			Sample b = matched.get(i + 1);
			if (s >= a.s && s <= b.s) {
				if (b.s == a.s) {
					return a.throttle;
				}
				double t = (s - a.s) / (b.s - a.s);
				return (int) Math.round(a.throttle + t * (b.throttle - a.throttle));
			}
		}
		return 0;
	}

	public int holdAt(double s, int sign) {
		return holdAt(s, sign, splineId);
	}

	public int holdAt(double s, int sign, UUID currentSpline) {
		return holdAt(s, sign, currentSpline == null ? splineId : currentSpline.toString());
	}

	public int holdAt(double s, int sign, String currentSpline) {
		int want = sign < 0 ? -1 : 1;
		Sample best = null;
		double bestD = HOLD_S;
		for (Sample sample : samplesFor(currentSpline)) {
			if (sample.sign != want) {
				continue;
			}
			double d = Math.abs(sample.s - s);
			if (d < bestD) {
				bestD = d;
				best = sample;
			}
		}
		return best == null ? 1 : best.holdTicks;
	}

	public int targetWithDwell(double s, int sign, DwellState dwell) {
		return targetWithDwell(s, sign, dwell, splineId);
	}

	public int targetWithDwell(double s, int sign, DwellState dwell, UUID currentSpline) {
		return targetWithDwell(s, sign, dwell, currentSpline == null ? splineId : currentSpline.toString());
	}

	public int targetWithDwell(double s, int sign, DwellState dwell, String currentSpline) {
		int target = lookup(s, sign, currentSpline);
		int hold = holdAt(s, sign, currentSpline);
		if (dwell == null) {
			return target;
		}
		if (target == 0 && hold > 1) {
			if (dwell.atS == null || Math.abs(s - dwell.atS) >= HOLD_S) {
				dwell.left = hold;
				dwell.atS = s;
			}
			if (dwell.left > 0) {
				dwell.left--;
				return 0;
			}
		} else if (dwell.atS != null && Math.abs(s - dwell.atS) >= HOLD_S) {
			dwell.left = 0;
			dwell.atS = null;
		}
		return target;
	}

	public AppendResult tryAppend(double s, int sign, int throttle) {
		return tryAppend(s, sign, throttle, splineId, null);
	}

	public AppendResult tryAppend(double s, int sign, int throttle, String sampleSpline, String junctionId) {
		int nsign = sign < 0 ? -1 : 1;
		String spline = blankToNull(sampleSpline);
		if (spline == null) {
			spline = blankToNull(this.splineId);
		}
		String junction = blankToNull(junctionId);
		if (!samples.isEmpty()) {
			Sample last = samples.get(samples.size() - 1);
			String lastSpline = last.resolvedSpline(this.splineId);
			boolean splineChanged = !sameId(lastSpline, spline);
			boolean junctionChanged = !sameId(last.junctionId, junction);
			boolean throttleChanged = last.throttle != throttle || last.sign != nsign;
			boolean far = Math.abs(s - last.s) >= HOLD_S;
			if (!splineChanged && !junctionChanged && !throttleChanged && !far) {
				last.holdTicks++;
				return AppendResult.HELD;
			}
		}
		if (samples.size() >= MAX_SAMPLES) {
			return AppendResult.CAPPED;
		}
		samples.add(new Sample(s, nsign, throttle, 1, spline, junction));
		if (samples.size() >= MAX_SAMPLES) {
			return AppendResult.CAPPED;
		}
		return AppendResult.ADDED;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject root = new JSONObject();
		root.put("splineId", splineId);
		JSONArray arr = new JSONArray();
		for (Sample sample : samples) {
			JSONObject o = new JSONObject();
			o.put("s", sample.s);
			o.put("sign", (long) sample.sign);
			o.put("throttle", (long) sample.throttle);
			o.put("hold", (long) sample.holdTicks);
			if (sample.splineId != null && !sample.splineId.equalsIgnoreCase(splineId)) {
				o.put("splineId", sample.splineId);
			}
			if (sample.junctionId != null) {
				o.put("junction", sample.junctionId);
			}
			arr.add(o);
		}
		root.put("samples", arr);
		return root;
	}

	public static ThrottleTape fromJson(JSONObject root) {
		if (root == null) {
			return null;
		}
		Object idRaw = root.get("splineId");
		if (idRaw == null) {
			return null;
		}
		String splineId = String.valueOf(idRaw);
		if (splineId.isBlank()) {
			return null;
		}
		List<Sample> loaded = new ArrayList<>();
		Object arrRaw = root.get("samples");
		if (arrRaw instanceof JSONArray arr) {
			for (Object raw : arr) {
				if (!(raw instanceof JSONObject o)) {
					continue;
				}
				int hold = o.get("hold") == null ? 1 : (int) asDouble(o.get("hold"));
				loaded.add(new Sample(
						asDouble(o.get("s")),
						(int) asDouble(o.get("sign")),
						(int) asDouble(o.get("throttle")),
						hold,
						stringOrNull(o.get("splineId")),
						stringOrNull(o.get("junction"))));
			}
		}
		return new ThrottleTape(splineId, loaded);
	}

	private List<Sample> samplesFor(String currentSpline) {
		String want = blankToNull(currentSpline);
		if (want == null) {
			want = splineId;
		}
		List<Sample> out = new ArrayList<>();
		for (Sample sample : samples) {
			if (sameId(sample.resolvedSpline(splineId), want)) {
				out.add(sample);
			}
		}
		return out;
	}

	private static boolean sameId(String a, String b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equalsIgnoreCase(b);
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
			return null;
		}
		return value;
	}

	private static String stringOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		return blankToNull(String.valueOf(raw));
	}

	private static double asDouble(Object value) {
		if (value instanceof Number n) {
			return n.doubleValue();
		}
		if (value == null) {
			return 0;
		}
		try {
			return Double.parseDouble(String.valueOf(value));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
