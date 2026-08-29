package net.tfminecraft.VehicleFramework.Database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

class ConsistDataTest {

	@Test
	void put_omitsKeysWhenUnbound() {
		JSONObject json = new JSONObject();
		ConsistData.unbound().put(json);
		assertTrue(json.isEmpty());
	}

	@Test
	void roundtrip_parentChildSplineAndS() {
		ConsistData data = new ConsistData(
				"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
				"11111111-2222-3333-4444-555555555555",
				"99999999-8888-7777-6666-555555555555",
				12.5);
		JSONObject json = new JSONObject();
		data.put(json);
		assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", json.get("parent"));
		assertEquals("11111111-2222-3333-4444-555555555555", json.get("child"));
		assertEquals("99999999-8888-7777-6666-555555555555", json.get("splineId"));
		assertEquals(12.5, (Double) json.get("s"), 1e-9);

		ConsistData loaded = ConsistData.fromJson(json);
		assertEquals(data.getParent(), loaded.getParent());
		assertEquals(data.getChild(), loaded.getChild());
		assertEquals(data.getSplineId(), loaded.getSplineId());
		assertEquals(12.5, loaded.getS(), 1e-9);
		assertEquals(1, loaded.getTravelSign());
	}

	@Test
	void roundtrip_negativeTravelSign() {
		ConsistData data = new ConsistData(null, null,
				"99999999-8888-7777-6666-555555555555", 1.0, -1);
		JSONObject json = new JSONObject();
		data.put(json);
		assertEquals(-1L, json.get("travelSign"));
		assertEquals(-1, ConsistData.fromJson(json).getTravelSign());
	}

	@Test
	void fromJson_readsLongS() {
		JSONObject json = new JSONObject();
		json.put("s", Long.valueOf(8));
		json.put("splineId", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		ConsistData loaded = ConsistData.fromJson(json);
		assertEquals(8.0, loaded.getS(), 1e-9);
	}

	@Test
	void fromJson_missingKeysAreUnbound() {
		ConsistData loaded = ConsistData.fromJson(new JSONObject());
		assertTrue(loaded.isUnbound());
		assertNull(loaded.getParent());
		assertNull(loaded.getChild());
		assertNull(loaded.getSplineId());
		assertNull(loaded.getS());
	}

	@Test
	void put_omitsSWithoutSpline() {
		JSONObject json = new JSONObject();
		new ConsistData(null, null, null, 4.0).put(json);
		assertTrue(json.isEmpty());
	}

	@Test
	void roundtrip_junctionAndDiverge() {
		ConsistData data = new ConsistData(
				null, null,
				"99999999-8888-7777-6666-555555555555",
				4.0, 1,
				"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
				true);
		JSONObject json = new JSONObject();
		data.put(json);
		assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", json.get("junction"));
		assertEquals(true, json.get("diverge"));
		ConsistData loaded = ConsistData.fromJson(json);
		assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", loaded.getJunctionId());
		assertTrue(loaded.isDiverge());
	}

	@Test
	void put_omitsJunctionWhenNull() {
		JSONObject json = new JSONObject();
		new ConsistData(null, null, "spline", 1.0, 1, null, true).put(json);
		assertFalse(json.containsKey("junction"));
		assertFalse(json.containsKey("diverge"));
	}
}
