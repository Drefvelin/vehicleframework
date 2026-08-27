package net.tfminecraft.VehicleFramework.Weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Data.HealthData;

class WeaponPerformanceTest {

	@Test
	void effectiveTurnRate_atFullHealth_returnsBaseRate() {
		HealthData health = healthAtPercent(100);
		assertEquals(0.5, WeaponPerformance.effectiveTurnRate(0.5, health));
	}

	@Test
	void effectiveTurnRate_atHalfHealth_returnsHalfRate() {
		HealthData health = healthAtPercent(50);
		assertEquals(0.25, WeaponPerformance.effectiveTurnRate(0.5, health));
	}

	@Test
	void effectiveTurnRate_atZeroHealth_returnsZero() {
		HealthData health = healthAtPercent(0);
		assertEquals(0.0, WeaponPerformance.effectiveTurnRate(0.5, health));
	}

	@Test
	void effectiveReloadSeconds_atFullHealth_returnsBaseTime() {
		HealthData health = healthAtPercent(100);
		assertEquals(5, WeaponPerformance.effectiveReloadSeconds(5, health, 2.0));
	}

	@Test
	void effectiveReloadSeconds_atZeroHealth_returnsDoubledTime() {
		HealthData health = healthAtPercent(0);
		assertEquals(10, WeaponPerformance.effectiveReloadSeconds(5, health, 2.0));
	}

	@Test
	void effectiveReloadSeconds_atHalfHealth_returnsScaledTime() {
		HealthData health = healthAtPercent(50);
		assertEquals(8, WeaponPerformance.effectiveReloadSeconds(5, health, 2.0));
	}

	@Test
	void effectiveReloadSeconds_customMultiplier_scalesToConfiguredMax() {
		HealthData health = healthAtPercent(0);
		assertEquals(15, WeaponPerformance.effectiveReloadSeconds(5, health, 3.0));
	}

	@Test
	void effectiveReloadSeconds_zeroBaseTime_clampsToMinimumOneSecond() {
		HealthData health = healthAtPercent(100);
		assertEquals(1, WeaponPerformance.effectiveReloadSeconds(0, health, 2.0));
	}

	private static HealthData healthAtPercent(int percent) {
		HealthData health = new HealthData(100.0, 0, 5);
		health.setDamage(100.0 - percent);
		return health;
	}
}
