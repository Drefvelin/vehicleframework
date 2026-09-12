package net.tfminecraft.VehicleFramework.Database;

import org.json.simple.JSONObject;

public final class ConsistData {
	private final String parent;
	private final String child;
	private final String splineId;
	private final Double s;
	private final Integer travelSign;
	private final String junctionId;
	private final Boolean diverge;

	public ConsistData(String parent, String child, String splineId, Double s) {
		this(parent, child, splineId, s, null, null, null);
	}

	public ConsistData(String parent, String child, String splineId, Double s, Integer travelSign) {
		this(parent, child, splineId, s, travelSign, null, null);
	}

	public ConsistData(
			String parent,
			String child,
			String splineId,
			Double s,
			Integer travelSign,
			String junctionId,
			Boolean diverge) {
		this.parent = blankToNull(parent);
		this.child = blankToNull(child);
		this.splineId = blankToNull(splineId);
		this.s = this.splineId == null ? null : s;
		this.travelSign = this.splineId == null ? null : normalizeSign(travelSign);
		this.junctionId = this.splineId == null ? null : blankToNull(junctionId);
		this.diverge = this.junctionId == null ? null : diverge;
	}

	public static ConsistData unbound() {
		return new ConsistData(null, null, null, null);
	}

	public static ConsistData fromJson(JSONObject json) {
		if (json == null) {
			return unbound();
		}
		return new ConsistData(
				stringOrNull(json, "parent"),
				stringOrNull(json, "child"),
				stringOrNull(json, "splineId"),
				numberOrNull(json, "s"),
				intOrNull(json, "travelSign"),
				stringOrNull(json, "junction"),
				boolOrNull(json, "diverge"));
	}

	@SuppressWarnings("unchecked")
	public void put(JSONObject json) {
		if (json == null) {
			return;
		}
		if (parent != null) {
			json.put("parent", parent);
		}
		if (child != null) {
			json.put("child", child);
		}
		if (splineId != null) {
			json.put("splineId", splineId);
			if (s != null) {
				json.put("s", s);
			}
			json.put("travelSign", (long) (travelSign == null ? 1 : travelSign));
			if (junctionId != null) {
				json.put("junction", junctionId);
				if (Boolean.TRUE.equals(diverge)) {
					json.put("diverge", true);
				}
			}
		}
	}

	public boolean isUnbound() {
		return parent == null && child == null && splineId == null;
	}

	public String getParent() {
		return parent;
	}

	public String getChild() {
		return child;
	}

	public String getSplineId() {
		return splineId;
	}

	public Double getS() {
		return s;
	}

	public int getTravelSign() {
		return travelSign == null ? 1 : travelSign;
	}

	public String getJunctionId() {
		return junctionId;
	}

	public boolean isDiverge() {
		return Boolean.TRUE.equals(diverge);
	}

	private static Integer intOrNull(JSONObject json, String key) {
		Double n = numberOrNull(json, key);
		if (n == null) {
			return null;
		}
		return n.intValue();
	}

	private static Boolean boolOrNull(JSONObject json, String key) {
		if (!json.containsKey(key) || json.get(key) == null) {
			return null;
		}
		Object raw = json.get(key);
		if (raw instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(raw));
	}

	private static int normalizeSign(Integer travelSign) {
		if (travelSign != null && travelSign < 0) {
			return -1;
		}
		return 1;
	}

	private static String stringOrNull(JSONObject json, String key) {
		if (!json.containsKey(key) || json.get(key) == null) {
			return null;
		}
		String value = String.valueOf(json.get(key));
		return blankToNull(value);
	}

	private static Double numberOrNull(JSONObject json, String key) {
		if (!json.containsKey(key) || json.get(key) == null) {
			return null;
		}
		Object raw = json.get(key);
		if (raw instanceof Number number) {
			return number.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(raw));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank() || value.equals("null")) {
			return null;
		}
		return value;
	}
}
