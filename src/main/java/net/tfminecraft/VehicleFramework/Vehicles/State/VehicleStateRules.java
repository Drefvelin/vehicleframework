package net.tfminecraft.VehicleFramework.Vehicles.State;

public final class VehicleStateRules {

	private VehicleStateRules() {
	}

	/**
	 * FLOATING only when the vehicle has a configured floating state and water is
	 * at least two blocks deep (feet and one block above).
	 */
	public static boolean shouldSwapToFloating(
			boolean floatingConfigured,
			boolean waterAtFeet,
			boolean waterOneBlockAbove) {
		return floatingConfigured && waterAtFeet && waterOneBlockAbove;
	}

	public static boolean shouldSwapToFlying(boolean flyingConfigured, boolean airBelow) {
		return flyingConfigured && airBelow;
	}
}
