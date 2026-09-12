package net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThrottleStepTowardTest {

	@Test
	void stepToward_threeStepsFromZero() {
		Throttle throttle = new Throttle("Throttle", 100, 0, null);
		assertEquals(0, throttle.getCurrent());
		throttle.stepToward(3);
		assertEquals(1, throttle.getCurrent());
		throttle.stepToward(3);
		assertEquals(2, throttle.getCurrent());
		throttle.stepToward(3);
		assertEquals(3, throttle.getCurrent());
		throttle.stepToward(3);
		assertEquals(3, throttle.getCurrent());
	}
}
