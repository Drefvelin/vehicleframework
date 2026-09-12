package net.tfminecraft.VehicleFramework.Vehicles.State;

public final class VehicleStateRules {

	private VehicleStateRules() {
	}

	/**
	 * FLOATING when the vehicle has a configured floating state, water is at the feet,
	 * and the water is deep (not shallow 1-block wadable water over solid ground).
	 */
	public static boolean shouldSwapToFloating(
			boolean floatingConfigured,
			boolean waterAtFeet,
			boolean shallowWadableWater) {
		return floatingConfigured && waterAtFeet && !shallowWadableWater;
	}

	public static boolean shouldSwapToFlying(boolean flyingConfigured, boolean airBelow) {
		return flyingConfigured && airBelow;
	}
}
