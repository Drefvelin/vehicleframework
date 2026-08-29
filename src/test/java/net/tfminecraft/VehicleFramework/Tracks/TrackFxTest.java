package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrackFxTest {

	@Test
	void laneOffsets_threeWide() {
		assertArrayEquals(new int[] {-1, 0, 1}, TrackFx.laneOffsets(3));
		assertArrayEquals(new int[] {0}, TrackFx.laneOffsets(1));
	}

	@Test
	void rightOf_yawZeroIsEast() {
		double[] right = TrackFx.rightOf(0);
		assertEquals(1, right[0], 1e-6);
		assertEquals(0, right[1], 1e-6);
	}
}
