package net.tfminecraft.VehicleFramework.Weapons;

import net.tfminecraft.VehicleFramework.Data.HealthData;

public final class WeaponPerformance {

	private WeaponPerformance() {
	}

	public static double effectiveTurnRate(double baseTurnRate, HealthData health) {
		if (health == null) {
			return baseTurnRate;
		}
		return baseTurnRate * (health.getHealthPercentage() / 100.0);
	}

	public static int effectiveReloadSeconds(int baseReloadTime, HealthData health, double maxMultiplierAtZeroHealth) {
		if (baseReloadTime <= 0) {
			return 1;
		}
		double maxMultiplier = Math.max(1.0, maxMultiplierAtZeroHealth);
		double healthRatio = health == null ? 1.0 : health.getHealthPercentage() / 100.0;
		double reloadMultiplier = 1.0 + (1.0 - healthRatio) * (maxMultiplier - 1.0);
		return Math.max(1, (int) Math.ceil(baseReloadTime * reloadMultiplier));
	}
}
