package net.tfminecraft.VehicleFramework.Managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;

class SpawnManagerPersistenceTest {
	@Test
	void enqueueUsesUuidNotJsonFilename() {
		assertEquals("u1", SpawnManager.stripJson("u1.json"));
		IncompleteVehicle incomplete = typedVehicle("horse_cart");
		assertTrue(SpawnManager.isComplete(incomplete));
	}

	@Test
	void isComplete_nullIncompleteDoesNotThrow() {
		assertDoesNotThrow(() -> SpawnManager.isComplete(null));
		assertFalse(SpawnManager.isComplete(null));
		assertFalse(SpawnManager.isComplete(blankVehicle()));
		assertTrue(SpawnManager.isComplete(typedVehicle("horse_cart")));
	}

	@Test
	void stripJson_usesUuidNotFilename() {
		assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", SpawnManager.stripJson("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.json"));
		assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", SpawnManager.stripJson("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
	}

	private static IncompleteVehicle blankVehicle() {
		return new IncompleteVehicle(
				"uuid",
				null,
				"name",
				"skin",
				java.util.List.of(),
				java.util.List.of(),
				java.util.List.of(),
				java.util.List.of(),
				java.util.List.of(),
				0,
				1,
				0f,
				0d,
				"none",
				false,
				java.util.List.of());
	}

	private static IncompleteVehicle typedVehicle(String id) {
		return new IncompleteVehicle(
				"uuid",
				id,
				"name",
				"skin",
				java.util.List.of(),
				java.util.List.of(),
				java.util.List.of(),
				java.util.List.of(),
				java.util.List.of(),
				0,
				1,
				0f,
				0d,
				"none",
				false,
				java.util.List.of());
	}
}
