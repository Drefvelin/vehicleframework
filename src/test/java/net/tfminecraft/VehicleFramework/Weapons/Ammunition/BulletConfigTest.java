package net.tfminecraft.VehicleFramework.Weapons.Ammunition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BulletConfigTest {

	@Test
	void defaults_useGgEquivalentSpeedAndGravity() {
		assertEquals(80.0, Bullet.DEFAULT_RANGE);
		assertEquals(7.0, Bullet.DEFAULT_SPEED);
		assertEquals(-0.05, Bullet.DEFAULT_GRAVITY);
	}
}
