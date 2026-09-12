package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

class TrackJunctionTest {

	@Test
	void jsonRoundtrip_preservesBranch() {
		UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		UUID stem = UUID.fromString("11111111-2222-3333-4444-555555555555");
		UUID branch = UUID.fromString("99999999-8888-7777-6666-555555555555");
		TrackJunction junction = new TrackJunction(
				id, stem, 12.5, -1, TrackJunction.Side.LEFT, branch);
		TrackJunction loaded = TrackJunction.fromJson(junction.toJson());
		assertEquals(id, loaded.id);
		assertEquals(stem, loaded.stemSplineId);
		assertEquals(12.5, loaded.s, 1e-9);
		assertEquals(-1, loaded.facingSign);
		assertEquals(TrackJunction.Side.LEFT, loaded.side);
		assertEquals(branch, loaded.branchSplineId().orElseThrow());
		assertFalse(loaded.thrown);
		assertEquals(0, loaded.turnoutEndS, 1e-9);
	}

	@Test
	void jsonRoundtrip_preservesThrown() {
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 4, 1, TrackJunction.Side.RIGHT, UUID.randomUUID(), true);
		TrackJunction loaded = TrackJunction.fromJson(junction.toJson());
		assertTrue(loaded.thrown);
		JSONObject missing = junction.withThrown(false).toJson();
		missing.remove("thrown");
		assertFalse(TrackJunction.fromJson(missing).thrown);
	}

	@Test
	void jsonRoundtrip_omitsMissingBranch() {
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 3, 1, TrackJunction.Side.RIGHT, null);
		JSONObject json = junction.toJson();
		assertFalse(json.containsKey("branch"));
		TrackJunction loaded = TrackJunction.fromJson(json);
		assertTrue(loaded.branchSplineId().isEmpty());
		assertEquals(1, loaded.facingSign);
	}

	@Test
	void jsonRoundtrip_preservesTurnoutS() {
		UUID id = UUID.randomUUID();
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunction junction = new TrackJunction(
				id, stem, 8, 1, TrackJunction.Side.RIGHT, branch, false, 23.5);
		JSONObject json = junction.toJson();
		assertTrue(json.containsKey("turnoutS"));
		assertEquals(23.5, (Double) json.get("turnoutS"), 1e-9);
		TrackJunction loaded = TrackJunction.fromJson(json);
		assertEquals(23.5, loaded.turnoutEndS, 1e-9);
		assertEquals(branch, loaded.branchSplineId().orElseThrow());
	}

	@Test
	void jsonRoundtrip_omitsTurnoutSWhenZero() {
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 3, 1, TrackJunction.Side.RIGHT, null);
		JSONObject json = junction.toJson();
		assertFalse(json.containsKey("turnoutS"));
		TrackJunction loaded = TrackJunction.fromJson(json);
		assertEquals(0, loaded.turnoutEndS, 1e-9);
	}

	@Test
	void jsonRoundtrip_missingTurnoutSDefaultsToZero() {
		UUID branch = UUID.randomUUID();
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 5, 1, TrackJunction.Side.LEFT, branch);
		JSONObject json = junction.toJson();
		assertFalse(json.containsKey("turnoutS"));
		TrackJunction loaded = TrackJunction.fromJson(json);
		assertEquals(0, loaded.turnoutEndS, 1e-9);
		assertEquals(branch, loaded.branchSplineId().orElseThrow());
	}

	@Test
	void withTurnoutEndS_preservesOtherFields() {
		UUID id = UUID.randomUUID();
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunction base = new TrackJunction(
				id, stem, 10, -1, TrackJunction.Side.LEFT, branch, true);
		TrackJunction updated = base.withTurnoutEndS(18);
		assertEquals(18, updated.turnoutEndS, 1e-9);
		assertEquals(id, updated.id);
		assertEquals(stem, updated.stemSplineId);
		assertEquals(10, updated.s, 1e-9);
		assertEquals(-1, updated.facingSign);
		assertEquals(TrackJunction.Side.LEFT, updated.side);
		assertEquals(branch, updated.branchSplineId().orElseThrow());
		assertTrue(updated.thrown);
		assertEquals(0, base.turnoutEndS, 1e-9);
	}

	@Test
	void facingSign_withinNinetyIsPlus() {
		assertEquals(1, TrackJunction.facingSign(0, 0));
		assertEquals(1, TrackJunction.facingSign(80, 0));
		assertEquals(-1, TrackJunction.facingSign(180, 0));
	}

	@Test
	void arcDistance_loopWraps() {
		assertEquals(2.0, TrackJunction.arcDistance(1, 79, 80, true), 1e-9);
		assertEquals(16.0, TrackJunction.arcDistance(0, 16, 80, false), 1e-9);
	}
}
