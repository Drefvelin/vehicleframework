package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

class ThrottleTapeTest {

	@Test
	void emptyTape_lookupZero() {
		assertEquals(0, ThrottleTape.lookup(List.of(), 10.0, 1));
		assertTrue(new ThrottleTape("spline").isEmpty());
	}

	@Test
	void wrongSign_lookupZero() {
		List<ThrottleTape.Sample> samples = List.of(
				new ThrottleTape.Sample(0, 1, 40),
				new ThrottleTape.Sample(10, 1, 80));
		assertEquals(0, ThrottleTape.lookup(samples, 5.0, -1));
	}

	@Test
	void matchingS_interpolates() {
		List<ThrottleTape.Sample> samples = List.of(
				new ThrottleTape.Sample(0, 1, 0),
				new ThrottleTape.Sample(10, 1, 100));
		assertEquals(50, ThrottleTape.lookup(samples, 5.0, 1));
		assertEquals(0, ThrottleTape.lookup(samples, -1.0, 1));
		assertEquals(100, ThrottleTape.lookup(samples, 20.0, 1));
	}

	@Test
	void tryAppend_sameSAndThrottle_incrementsHold() {
		ThrottleTape tape = new ThrottleTape("spline");
		assertEquals(ThrottleTape.AppendResult.ADDED, tape.tryAppend(5, 1, 0));
		assertEquals(ThrottleTape.AppendResult.HELD, tape.tryAppend(5.2, 1, 0));
		assertEquals(ThrottleTape.AppendResult.HELD, tape.tryAppend(5.4, 1, 0));
		assertEquals(1, tape.getSamples().size());
		assertEquals(3, tape.getSamples().get(0).holdTicks);
	}

	@Test
	void jsonRoundtrip_preservesHold() {
		ThrottleTape tape = new ThrottleTape("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		tape.tryAppend(0, 1, 20);
		tape.tryAppend(4, 1, 40);
		JSONObject json = tape.toJson();
		ThrottleTape loaded = ThrottleTape.fromJson(json);
		assertEquals(40, loaded.lookup(4.0, 1));
		assertEquals(30, loaded.lookup(2.0, 1));
		assertEquals(1, loaded.getSamples().get(0).holdTicks);
		assertFalse(loaded.isEmpty());
	}

	@Test
	void fromJson_missingHold_defaultsToOne() {
		JSONObject root = new JSONObject();
		root.put("splineId", "spline");
		org.json.simple.JSONArray arr = new org.json.simple.JSONArray();
		JSONObject o = new JSONObject();
		o.put("s", 0d);
		o.put("sign", 1L);
		o.put("throttle", 0L);
		arr.add(o);
		root.put("samples", arr);
		ThrottleTape loaded = ThrottleTape.fromJson(root);
		assertEquals(1, loaded.getSamples().get(0).holdTicks);
	}

	@Test
	void targetWithDwell_holdsZeroThenReleases() {
		ThrottleTape tape = new ThrottleTape("spline");
		tape.tryAppend(10, 1, 0);
		for (int i = 0; i < 39; i++) {
			tape.tryAppend(10, 1, 0);
		}
		assertEquals(40, tape.holdAt(10, 1));
		ThrottleTape.DwellState dwell = new ThrottleTape.DwellState();
		for (int i = 0; i < 40; i++) {
			assertEquals(0, tape.targetWithDwell(10, 1, dwell));
		}
		tape.tryAppend(12, 1, 50);
		assertEquals(50, tape.targetWithDwell(12, 1, dwell));
	}

	@Test
	void tryAppend_splineChange_addsSample() {
		ThrottleTape tape = new ThrottleTape("stem");
		assertEquals(ThrottleTape.AppendResult.ADDED, tape.tryAppend(5, 1, 40, "stem", null));
		assertEquals(ThrottleTape.AppendResult.HELD, tape.tryAppend(5.2, 1, 40, "stem", null));
		assertEquals(ThrottleTape.AppendResult.ADDED, tape.tryAppend(0, 1, 40, "branch", "junc-1"));
		assertEquals(2, tape.getSamples().size());
		assertEquals("branch", tape.getSamples().get(1).splineId);
		assertEquals("junc-1", tape.getSamples().get(1).junctionId);
	}

	@Test
	void lookup_usesCurrentSplineOnly() {
		ThrottleTape tape = new ThrottleTape("stem");
		tape.tryAppend(0, 1, 10, "stem", null);
		tape.tryAppend(10, 1, 50, "stem", null);
		tape.tryAppend(0, 1, 80, "branch", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		tape.tryAppend(8, 1, 20, "branch", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		assertEquals(30, tape.lookup(5, 1, "stem"));
		assertEquals(50, tape.lookup(4, 1, "branch"));
		assertTrue(tape.takesJunction(java.util.UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")));
		assertFalse(tape.takesJunction(java.util.UUID.fromString("11111111-2222-3333-4444-555555555555")));
	}

	@Test
	void jsonRoundtrip_sampleSplineAndJunction() {
		ThrottleTape tape = new ThrottleTape("stem-id");
		tape.tryAppend(1, 1, 20, "stem-id", null);
		tape.tryAppend(0, 1, 40, "branch-id", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		ThrottleTape loaded = ThrottleTape.fromJson(tape.toJson());
		assertEquals(20, loaded.lookup(1, 1, "stem-id"));
		assertEquals(40, loaded.lookup(0, 1, "branch-id"));
		assertTrue(loaded.takesJunction(java.util.UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")));
		assertEquals("branch-id", loaded.getSamples().get(1).splineId);
	}

	@Test
	void fromJson_missingSampleSpline_inheritsOrigin() {
		JSONObject root = new JSONObject();
		root.put("splineId", "origin");
		org.json.simple.JSONArray arr = new org.json.simple.JSONArray();
		JSONObject o = new JSONObject();
		o.put("s", 4d);
		o.put("sign", 1L);
		o.put("throttle", 70L);
		arr.add(o);
		root.put("samples", arr);
		ThrottleTape loaded = ThrottleTape.fromJson(root);
		assertEquals(70, loaded.lookup(4, 1, "origin"));
		assertFalse(loaded.takesJunction(java.util.UUID.randomUUID()));
	}
}
