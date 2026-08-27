package net.tfminecraft.VehicleFramework.Weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeaponProjectileDamageTest {

	@Test
	void effectiveDamage_usesOverrideWhenPresent() {
		assertEquals(12, Weapon.effectiveDamage(12, null));
	}

	@Test
	void effectiveDamage_fallsBackWhenOverrideMissing() {
		assertEquals(0, Weapon.effectiveDamage((Integer) null, null));
		assertEquals(0, Weapon.effectiveDamage((ActiveWeapon) null, null));
	}

	@Test
	void effectiveDamageType_usesOverrideWhenPresent() {
		assertEquals("AA", Weapon.effectiveDamageType("AA", null));
	}

	@Test
	void effectiveDamageType_fallsBackWhenOverrideMissingOrBlank() {
		assertEquals("PROJECTILE", Weapon.effectiveDamageType((String) null, null));
		assertEquals("PROJECTILE", Weapon.effectiveDamageType("  ", null));
		assertEquals("PROJECTILE", Weapon.effectiveDamageType((ActiveWeapon) null, null));
	}
}
