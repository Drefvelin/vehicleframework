package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrackGenerateTest {

	@Test
	void densify_tenBlockPlusZ_hasElevenSamplesAndSouthYaw() {
		List<double[]> points = TrackGenerate.densify(0, 64, 0, 0, 64, 10, 1.0);
		assertEquals(11, points.size());
		TrackSpline spline = TrackGenerate.between(UUID.randomUUID(), "world", 0, 64, 0, 0, 64, 10);
		assertEquals(10, spline.length(), 1e-9);
		assertEquals(11, spline.getSamples().size());
		assertEquals(0f, spline.sampleAt(5).yaw, 0.01f);
	}

	@Test
	void densify_rejectsIdenticalAnchors() {
		assertThrows(IllegalArgumentException.class, () -> TrackGenerate.densify(1, 2, 3, 1, 2, 3, 1.0));
	}
}
