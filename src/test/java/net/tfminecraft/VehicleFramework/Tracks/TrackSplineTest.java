package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

class TrackSplineTest {

	@Test
	void fromPoints_rejectsSingleSample() {
		assertThrows(IllegalArgumentException.class, () -> TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false, List.of(new double[] {0, 0, 0})));
	}

	@Test
	void sampleAt_southAlongPlusZ_hasYawZero() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {0, 64, 10}));
		TrackPose mid = spline.sampleAt(5);
		assertEquals(0, mid.x, 1e-9);
		assertEquals(64, mid.y, 1e-9);
		assertEquals(5, mid.z, 1e-9);
		assertEquals(0f, mid.yaw, 0.01f);
	}

	@Test
	void sampleAt_westAlongMinusX_hasYawNinety() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {-10, 64, 0}));
		TrackPose mid = spline.sampleAt(5);
		assertEquals(-5, mid.x, 1e-9);
		assertEquals(90f, mid.yaw, 0.01f);
	}

	@Test
	void advance_stopsAtBrokenSegment() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 0, 0}, new double[] {0, 0, 10}, new double[] {0, 0, 20}));
		spline = spline.withSegment(1, new TrackSegment(1, true, 1.0));
		TrackAdvance move = spline.advance(5, 8);
		assertTrue(move.stoppedAtBreak);
		assertEquals(10, move.s, 1e-9);
	}

	@Test
	void advance_openClampsAtEnd() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 0, 0}, new double[] {0, 0, 10}));
		TrackAdvance move = spline.advance(9, 5);
		assertFalse(move.stoppedAtBreak);
		assertEquals(10, move.s, 1e-9);
	}

	@Test
	void advance_loopWraps() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", true,
				List.of(new double[] {0, 0, 0}, new double[] {0, 0, 10}));
		double len = spline.length();
		assertEquals(20, len, 1e-9);
		TrackAdvance move = spline.advance(len - 1, 3);
		assertFalse(move.stoppedAtBreak);
		assertEquals(2, move.s, 1e-9);
	}

	@Test
	void promotedLoop_wrapsPastJoin() {
		TrackSpline open = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 0, 0},
						new double[] {8, 0, 0},
						new double[] {8, 0, 8},
						new double[] {0.5, 0, 0}));
		assertFalse(open.isLoop());
		TrackSpline loop = open.promotedLoop(1.5);
		assertTrue(loop.isLoop());
		double len = loop.length();
		TrackAdvance move = loop.advance(len - 0.2, 1.0);
		assertFalse(move.stoppedAtBreak);
		assertTrue(move.s < 2.0);
	}

	@Test
	void fromJson_promotesLoopWhenEndsMeet() {
		TrackSpline open = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 0, 0},
						new double[] {8, 0, 0},
						new double[] {8, 0, 8},
						new double[] {0.4, 0, 0}));
		assertFalse(open.isLoop());
		TrackSpline loaded = TrackSpline.fromJson(open.toJson());
		assertTrue(loaded.isLoop());
	}

	@Test
	void json_roundtripPreservesBrokenAndWorld() {
		UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		TrackSpline spline = TrackSpline.fromPoints(
				id, "tracks", false,
				List.of(new double[] {1, 2, 3}, new double[] {1, 2, 13}));
		spline = spline.withSegment(0, new TrackSegment(0, true, 0.5));
		JSONObject json = spline.toJson();
		json.put("visuals", "ignored");
		TrackSpline loaded = TrackSpline.fromJson(json);
		assertEquals(id, loaded.getId());
		assertEquals("tracks", loaded.getWorld());
		assertFalse(loaded.isLoop());
		assertTrue(loaded.getSegments().get(0).broken);
		assertEquals(0.5, loaded.getSegments().get(0).health, 1e-9);
		assertEquals(10, loaded.length(), 1e-9);
	}

	@Test
	void nearestS_onStraightSegment() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 0, 0}, new double[] {0, 0, 10}));
		assertEquals(4, spline.nearestS(0, 1, 4), 1e-6);
		assertEquals(0, spline.nearestS(0, 0, -5), 1e-6);
		assertEquals(10, spline.nearestS(0, 0, 50), 1e-6);
	}
}
