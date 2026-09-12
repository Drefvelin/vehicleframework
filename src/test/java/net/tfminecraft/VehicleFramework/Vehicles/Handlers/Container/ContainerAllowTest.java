package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContainerAllowTest {

	@Test
	void emptyList_allowsAnyItem() {
		assertTrue(Container.decideAllow(true, false, false));
	}

	@Test
	void emptyItem_alwaysAllowed() {
		assertTrue(Container.decideAllow(false, true, false));
	}

	@Test
	void listedItem_requiresPathHit() {
		assertTrue(Container.decideAllow(false, false, true));
		assertFalse(Container.decideAllow(false, false, false));
	}
}
