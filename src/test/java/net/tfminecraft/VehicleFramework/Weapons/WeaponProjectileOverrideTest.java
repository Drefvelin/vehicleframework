package net.tfminecraft.VehicleFramework.Weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeaponProjectileOverrideTest {

	@Test
	void effectiveYield_noOverride_returnsZeroWhenAmmoIsNull() {
		assertEquals(0f, Weapon.effectiveYield((Float) null, null));
	}

	@Test
	void effectiveYield_withOverride_returnsOverride() {
		assertEquals(3.5f, Weapon.effectiveYield(3.5f, null));
	}

	@Test
	void effectiveRadius_noOverride_returnsZeroWhenAmmoIsNull() {
		assertEquals(0, Weapon.effectiveRadius((Integer) null, null));
	}

	@Test
	void effectiveRadius_withOverride_returnsOverride() {
		assertEquals(12, Weapon.effectiveRadius(12, null));
	}

	@Test
	void effectiveExplosive_noOverride_returnsFalseWhenAmmoIsNull() {
		assertFalse(Weapon.effectiveExplosive((Boolean) null, null));
	}

	@Test
	void effectiveExplosive_overrideTrue_returnsTrue() {
		assertTrue(Weapon.effectiveExplosive(true, null));
	}

	@Test
	void effectiveExplosive_overrideFalse_returnsFalseEvenWhenAmmoWouldBeTrue() {
		assertFalse(Weapon.effectiveExplosive(false, null));
	}

	@Test
	void effectiveClusterAmount_noOverride_returnsZeroWhenClusterIsNull() {
		assertEquals(0, Weapon.effectiveClusterAmount((Integer) null, null));
	}

	@Test
	void effectiveClusterAmount_withOverride_returnsOverride() {
		assertEquals(26, Weapon.effectiveClusterAmount(26, null));
	}
}
