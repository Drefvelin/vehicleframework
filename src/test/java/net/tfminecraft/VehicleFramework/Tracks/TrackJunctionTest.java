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
