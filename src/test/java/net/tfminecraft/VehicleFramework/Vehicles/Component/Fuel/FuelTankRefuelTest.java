package net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelTankRefuelTest {

	@Test
	void engineBlocksRefuel_whenThrottleOnAndFlagFalse() {
		assertTrue(FuelTank.engineBlocksRefuel(false, 10));
	}

	@Test
	void engineBlocksRefuel_allowsWhenFlagTrue() {
		assertFalse(FuelTank.engineBlocksRefuel(true, 10));
	}

	@Test
	void engineBlocksRefuel_allowsWhenThrottleIdle() {
		assertFalse(FuelTank.engineBlocksRefuel(false, 0));
	}
}
