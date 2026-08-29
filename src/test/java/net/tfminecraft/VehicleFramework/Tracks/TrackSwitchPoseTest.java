package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrackSwitchPoseTest {

	@Test
	void leftBranch_standsOnRightAndThrowsNegative() {
		TrackPose frog = new TrackPose(0, 64, 0, 0, 0);
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 10, 1, TrackJunction.Side.LEFT, UUID.randomUUID(), false);
		TrackSwitchPose pose = TrackSwitchPose.of(frog, junction, 0, 2, 0.5f, 0, 40);
		assertEquals(-2, pose.x, 1e-6);
		assertEquals(0, pose.z, 1e-6);
		assertEquals(64.5, pose.y, 1e-6);
		assertEquals(0, pose.throughYaw, 1e-3);
		assertEquals(-40, pose.divergeYaw, 1e-3);
		assertEquals(pose.throughYaw, pose.targetYaw, 1e-3);
	}

	@Test
	void thrown_usesDivergeYaw() {
		TrackPose frog = new TrackPose(0, 64, 0, 0, 0);
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 10, 1, TrackJunction.Side.RIGHT, UUID.randomUUID(), true);
		TrackSwitchPose pose = TrackSwitchPose.of(frog, junction, 0, 2, 0, 0, 40);
		assertEquals(2, pose.x, 1e-6);
		assertEquals(40, pose.divergeYaw, 1e-3);
		assertEquals(pose.divergeYaw, pose.targetYaw, 1e-3);
	}

	@Test
	void reverseFacing_adds180() {
		TrackPose frog = new TrackPose(10, 64, 10, 0, 0);
		TrackJunction junction = new TrackJunction(
				UUID.randomUUID(), UUID.randomUUID(), 10, -1, TrackJunction.Side.LEFT, UUID.randomUUID(), false);
		TrackSwitchPose pose = TrackSwitchPose.of(frog, junction, 1, 0, 0, 0, 40);
		assertEquals(10, pose.x, 1e-6);
		assertEquals(9, pose.z, 1e-6);
		assertEquals(180, pose.throughYaw, 1e-3);
	}

	@Test
	void stepYaw_snapsWhenClose() {
		assertEquals(10, TrackSwitchPose.stepYaw(0, 10, 12), 1e-3);
		float stepped = TrackSwitchPose.stepYaw(0, 40, 10);
		assertTrue(stepped > 0 && stepped < 40);
	}
}
