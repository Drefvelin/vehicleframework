package net.tfminecraft.VehicleFramework.Projectiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BulletRaycastTest {

	@Test
	void isBulletPassable_allowsLeavesAndGlass() {
		assertTrue(BulletRaycast.isBulletPassableName("OAK_LEAVES"));
		assertTrue(BulletRaycast.isBulletPassableName("GLASS"));
		assertTrue(BulletRaycast.isBulletPassableName("GLASS_PANE"));
	}

	@Test
	void isBulletPassable_blocksSolidMaterials() {
		assertFalse(BulletRaycast.isBulletPassableName("STONE"));
		assertFalse(BulletRaycast.isBulletPassableName("OAK_PLANKS"));
	}
}
