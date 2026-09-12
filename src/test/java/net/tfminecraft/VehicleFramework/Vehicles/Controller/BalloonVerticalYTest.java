package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BalloonVerticalYTest {

	@Test
	void healthyClimbUsesDelta() {
		assertEquals(0.5, LiftController.balloonVerticalY(0.4, 0.5));
	}

	@Test
	void healthyHoverZerosY() {
		assertEquals(0.0, LiftController.balloonVerticalY(0.4, 0.0));
	}

	@Test
	void healthyDescendUsesDelta() {
		assertEquals(-0.5, LiftController.balloonVerticalY(0.4, -0.5));
	}

	@Test
	void damagedBalloonSinksWithLift() {
		assertEquals(-0.2, LiftController.balloonVerticalY(-0.2, 0.5));
		assertEquals(-0.2, LiftController.balloonVerticalY(-0.2, 0.0));
	}
}
