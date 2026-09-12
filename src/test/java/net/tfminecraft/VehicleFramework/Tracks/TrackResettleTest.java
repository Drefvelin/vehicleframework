package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TrackResettleTest {

	@Test
	void withoutWorld_rampsOnlyNearLoweredLast() {
		List<double[]> points = new ArrayList<>();
		for (int i = 0; i <= 20; i++) {
			points.add(new double[] {0, 66, i});
		}
		points.get(points.size() - 1)[1] = 64;
		TrackResettle.resettle(null, points, false, true);
		assertEquals(66, points.get(0)[1], 1e-6);
		assertEquals(64, points.get(points.size() - 1)[1], 1e-6);
		assertEquals(66, points.get(4)[1], 1e-6);
		assertTrue(points.get(points.size() - 2)[1] < 65.0);
	}

	@Test
	void sameEndY_doesNotFlattenRaisedMiddle() {
		List<double[]> points = new ArrayList<>();
		for (int i = 0; i <= 30; i++) {
			double y = (i == 0 || i == 30) ? 161 : 163;
			points.add(new double[] {i, y, 0});
		}
		TrackResettle.resettle(null, points, false, true);
		assertEquals(161, points.get(0)[1], 1e-6);
		assertEquals(161, points.get(points.size() - 1)[1], 1e-6);
		assertEquals(163, points.get(15)[1], 1e-6);
	}

	@Test
	void floorFirst_rampsOnlyNearStart() {
		List<double[]> points = new ArrayList<>();
		for (int i = 0; i <= 20; i++) {
			points.add(new double[] {0, 66, i});
		}
		points.get(0)[1] = 64;
		TrackResettle.resettle(null, points, true, false);
		assertEquals(64, points.get(0)[1], 1e-6);
		assertEquals(66, points.get(points.size() - 1)[1], 1e-6);
		assertEquals(66, points.get(16)[1], 1e-6);
		assertTrue(points.get(1)[1] < 65.0);
	}

	@Test
	void smooth_stopsWhenSitYWouldEnterBlock() {
		List<double[]> points = new ArrayList<>();
		for (int i = 0; i <= 20; i++) {
			points.add(new double[] {0, 162, i});
		}
		points.get(points.size() - 1)[1] = 161;
		TrackResettle.smoothInward(points, points.size() - 1, -1, 0, "last", i -> 162.0);
		assertEquals(161, points.get(points.size() - 1)[1], 1e-6);
		assertEquals(162, points.get(10)[1], 1e-6);
		assertEquals(162, points.get(points.size() - 2)[1], 1e-6);
	}
}
