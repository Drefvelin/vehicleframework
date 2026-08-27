package net.tfminecraft.VehicleFramework.Weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeaponAimConfigTest {

	@Test
	void fromConfig_manual_returnsManual() {
		assertEquals(WeaponAimMode.MANUAL, WeaponAimMode.fromConfig("manual"));
	}

	@Test
	void fromConfig_cursor_returnsCursor() {
		assertEquals(WeaponAimMode.CURSOR, WeaponAimMode.fromConfig("cursor"));
	}

	@Test
	void fromConfig_nullOrUnknown_defaultsToManual() {
		assertEquals(WeaponAimMode.MANUAL, WeaponAimMode.fromConfig(null));
		assertEquals(WeaponAimMode.MANUAL, WeaponAimMode.fromConfig("wasd"));
	}

	@Test
	void defaultCursorRange_isEighty() {
		assertEquals(80.0, Weapon.DEFAULT_CURSOR_RANGE);
	}
}
