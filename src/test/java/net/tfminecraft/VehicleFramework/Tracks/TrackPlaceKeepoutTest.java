package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrackPlaceKeepoutTest {

	@Test
	void besideAndAboveDenied_belowAndFarAllowed() {
		double x = 0.5;
		double y = 64.2;
		double z = 0.5;
		double r = 1.5;
		assertTrue(TrackPlaceKeepout.blocked(x, y, z, 0, 64, 0, r));
		assertTrue(TrackPlaceKeepout.blocked(x, y, z, 1, 64, 0, r));
		assertTrue(TrackPlaceKeepout.blocked(x, y, z, 0, 65, 0, r));
		assertFalse(TrackPlaceKeepout.blocked(x, y, z, 0, 63, 0, r));
		assertFalse(TrackPlaceKeepout.blocked(x, y, z, 3, 64, 0, r));
	}

	@Test
	void splineList_usesNearestSampleHeight() {
		List<TrackSample> samples = List.of(
				new TrackSample(0.5, 64.1, 0.5, 0f, 0f, 0),
				new TrackSample(8.5, 70.1, 0.5, 0f, 0f, 8));
		assertFalse(TrackPlaceKeepout.blocked(samples, 0, 63, 0, 1.5));
		assertTrue(TrackPlaceKeepout.blocked(samples, 0, 64, 0, 1.5));
		assertTrue(TrackPlaceKeepout.blocked(samples, 8, 70, 0, 1.5));
		assertFalse(TrackPlaceKeepout.blocked(samples, 8, 69, 0, 1.5));
	}
}
