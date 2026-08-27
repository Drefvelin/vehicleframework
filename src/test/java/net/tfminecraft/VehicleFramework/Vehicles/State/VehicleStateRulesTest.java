package net.tfminecraft.VehicleFramework.Vehicles.State;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleStateRulesTest {

	@Test
	void oneBlockWater_doesNotFloat() {
		assertFalse(VehicleStateRules.shouldSwapToFloating(true, true, false));
		assertFalse(VehicleStateRules.shouldSwapToFloating(false, true, true));
		assertFalse(VehicleStateRules.shouldSwapToFloating(true, false, true));
	}

	@Test
	void twoBlockWater_floatsWhenConfigured() {
		assertTrue(VehicleStateRules.shouldSwapToFloating(true, true, true));
	}

	@Test
	void dummyFlying_doesNotSwap() {
		assertFalse(VehicleStateRules.shouldSwapToFlying(false, true));
		assertFalse(VehicleStateRules.shouldSwapToFlying(true, false));
	}

	@Test
	void configuredFlying_swapsWhenAirBelow() {
		assertTrue(VehicleStateRules.shouldSwapToFlying(true, true));
	}
}
