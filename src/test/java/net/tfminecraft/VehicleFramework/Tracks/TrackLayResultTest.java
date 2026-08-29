package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrackLayResultTest {

	@Test
	void appendKeep_omitsSharedOrigin() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {0, 64, 2},
						new double[] {0, 64, 3},
						new double[] {0, 64, 4}));
		List<double[]> extra = List.of(
				new double[] {0, 64, 2},
				new double[] {0, 64, 3},
				new double[] {0, 64, 4});
		TrackLayResult result = TrackLayResult.of(TrackLayResult.Kind.APPEND, spline, extra, 3);
		List<double[]> keep = result.keepPoints();
		assertEquals(3, keep.size());
		assertEquals(0, keep.get(0)[2], 1e-9);
		assertEquals(2, keep.get(2)[2], 1e-9);
	}

	@Test
	void prependKeep_omitsSharedOrigin() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, -2},
						new double[] {0, 64, -1},
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {0, 64, 2}));
		List<double[]> extra = List.of(
				new double[] {0, 64, 0},
				new double[] {0, 64, -1},
				new double[] {0, 64, -2});
		TrackLayResult result = TrackLayResult.of(TrackLayResult.Kind.PREPEND, spline, extra, 3);
		List<double[]> keep = result.keepPoints();
		assertEquals(3, keep.size());
		assertEquals(0, keep.get(0)[2], 1e-9);
		assertEquals(2, keep.get(2)[2], 1e-9);
	}

	@Test
	void newStroke_hasNoKeep() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {0, 64, 8}));
		TrackLayResult result = TrackLayResult.of(TrackLayResult.Kind.NEW, spline, spline.xyz(), 0);
		assertTrue(result.keepPoints().isEmpty());
		assertTrue(result.sequential());
	}
}
