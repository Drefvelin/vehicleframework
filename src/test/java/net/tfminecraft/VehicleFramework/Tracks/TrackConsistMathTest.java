package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrackConsistMathTest {

	@Test
	void connectorSpacing_sumsBoneLengths() {
		assertEquals(5.5, TrackConsistMath.connectorSpacing(2.0, 3.5), 1e-9);
	}

	@Test
	void carS_subtractsCumulativeSpacing() {
		double[] cars = TrackConsistMath.carS(40, new double[] {10, 8});
		assertArrayEquals(new double[] {30, 22}, cars, 1e-9);
	}

	@Test
	void carS_emptyWhenNoSpacings() {
		assertEquals(0, TrackConsistMath.carS(10, new double[0]).length);
	}
}
