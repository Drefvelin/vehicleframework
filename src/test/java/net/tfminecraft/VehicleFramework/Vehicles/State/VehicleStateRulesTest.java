package net.tfminecraft.VehicleFramework.Vehicles.State;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleStateRulesTest {

	@Test
	void noWater_doesNotFloat() {
		assertFalse(VehicleStateRules.shouldSwapToFloating(true, false, false));
		assertFalse(VehicleStateRules.shouldSwapToFloating(true, false, true));
		assertFalse(VehicleStateRules.shouldSwapToFloating(false, false, false));
	}

	@Test
	void unconfiguredFloating_doesNotSwap() {
		assertFalse(VehicleStateRules.shouldSwapToFloating(false, true, false));
		assertFalse(VehicleStateRules.shouldSwapToFloating(false, true, true));
	}

	@Test
	void shallowWadableWater_staysGround() {
		assertFalse(VehicleStateRules.shouldSwapToFloating(true, true, true));
	}

	@Test
	void deepWater_floatsWhenConfigured() {
		assertTrue(VehicleStateRules.shouldSwapToFloating(true, true, false));
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
