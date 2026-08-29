package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrainCollisionTest {

	@Test
	void consistHead_liveParentWins() {
		assertEquals("loco", TrainCollision.consistHead("car", "loco", null));
	}

	@Test
	void consistHead_pendingParentWhenUnlinked() {
		assertEquals("loco", TrainCollision.consistHead("car", null, "loco"));
	}

	@Test
	void consistHead_selfWhenAlone() {
		assertEquals("loco", TrainCollision.consistHead("loco", null, null));
	}

	@Test
	void sameConsist_linkedCarAndLoco() {
		assertTrue(TrainCollision.sameConsistIds("car", "loco", null, "loco", null, null));
	}

	@Test
	void sameConsist_pendingAfterUnload() {
		assertTrue(TrainCollision.sameConsistIds("car", null, "loco", "loco", null, null));
	}

	@Test
	void differentConsists() {
		assertFalse(TrainCollision.sameConsistIds("loco-a", null, null, "loco-b", null, null));
		assertFalse(TrainCollision.sameConsistIds("car-a", "loco-a", null, "car-b", "loco-b", null));
	}
}
