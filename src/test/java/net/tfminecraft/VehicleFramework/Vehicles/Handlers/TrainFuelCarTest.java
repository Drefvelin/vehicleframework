package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrainFuelCarTest {

	@Test
	void emptyList_noDrain() {
		assertFalse(TrainHandler.childIdAllowed(List.of(), "coal_car"));
		assertFalse(TrainHandler.shouldDrain(List.of(), "coal_car", true, true));
	}

	@Test
	void wrongChildId_noDrain() {
		assertFalse(TrainHandler.childIdAllowed(List.of("coal_car"), "passenger_car"));
		assertFalse(TrainHandler.shouldDrain(List.of("coal_car"), "passenger_car", true, true));
	}

	@Test
	void matchingId_allowed() {
		assertTrue(TrainHandler.childIdAllowed(List.of("coal_car"), "coal_car"));
		assertTrue(TrainHandler.childIdAllowed(List.of("Coal_Car"), "coal_car"));
		assertTrue(TrainHandler.shouldDrain(List.of("coal_car"), "coal_car", true, true));
	}

	@Test
	void matchingId_butNoSpaceOrItem_noDrain() {
		assertFalse(TrainHandler.shouldDrain(List.of("coal_car"), "coal_car", false, true));
		assertFalse(TrainHandler.shouldDrain(List.of("coal_car"), "coal_car", true, false));
	}
}
