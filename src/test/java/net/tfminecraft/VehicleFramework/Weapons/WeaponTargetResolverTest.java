package net.tfminecraft.VehicleFramework.Weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class WeaponTargetResolverTest {

	@Test
	void fallbackPoint_extendsEyeAlongDirectionByRange() {
		Location eye = new Location(null, 0, 64, 0);
		Vector direction = new Vector(0, 0, 1);

		Location result = WeaponTargetResolver.fallbackPoint(eye, direction, 80);

		assertEquals(0, result.getX(), 0.001);
		assertEquals(64, result.getY(), 0.001);
		assertEquals(80, result.getZ(), 0.001);
	}
}
