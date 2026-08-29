package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrackLapTest {

	@Test
	void wrapDelta_forwardWrap() {
		assertEquals(3, TrackLap.wrapDelta(18, 1, 20), 1e-9);
	}

	@Test
	void wrapDelta_noWrap() {
		assertEquals(4, TrackLap.wrapDelta(2, 6, 20), 1e-9);
	}

	@Test
	void complete_afterOneLength() {
		assertFalse(TrackLap.complete(19.9, 20));
		assertTrue(TrackLap.complete(20, 20));
		assertFalse(TrackLap.complete(5, 0.5));
	}
}
