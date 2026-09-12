package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FloatBobStepTest {

	private static final double MIN = FloatController.MIN_DEPTH;
	private static final double MAX = FloatController.MAX_DEPTH;
	private static final double SPEED = FloatController.BOB_SPEED;

	@Test
	void tooDeepRisesAndFlipsUp() {
		FloatController.BobStep step = FloatController.bobStep(1.4, true, MIN, MAX, SPEED);
		assertEquals(SPEED, step.vy);
		assertFalse(step.goingDown);
	}

	@Test
	void maxDepthEndpointRises() {
		FloatController.BobStep step = FloatController.bobStep(1.0, true, MIN, MAX, SPEED);
		assertEquals(SPEED, step.vy);
		assertFalse(step.goingDown);
	}

	@Test
	void tooHighSinksAndFlipsDown() {
		FloatController.BobStep step = FloatController.bobStep(0.2, false, MIN, MAX, SPEED);
		assertEquals(-SPEED, step.vy);
		assertTrue(step.goingDown);
	}

	@Test
	void minDepthEndpointSinks() {
		FloatController.BobStep step = FloatController.bobStep(0.6, false, MIN, MAX, SPEED);
		assertEquals(-SPEED, step.vy);
		assertTrue(step.goingDown);
	}

	@Test
	void midBandKeepsRising() {
		FloatController.BobStep step = FloatController.bobStep(0.8, false, MIN, MAX, SPEED);
		assertEquals(SPEED, step.vy);
		assertFalse(step.goingDown);
	}

	@Test
	void midBandKeepsSinking() {
		FloatController.BobStep step = FloatController.bobStep(0.8, true, MIN, MAX, SPEED);
		assertEquals(-SPEED, step.vy);
		assertTrue(step.goingDown);
	}
}
