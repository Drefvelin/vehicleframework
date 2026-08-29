package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

class TrackSplineMotionTest {

	private static final float TOLERANCE = 0.5f;

	@Test
	void boneYaw_isInvertedWorldMinusEntity() {
		assertEquals(0f, TrackSplineMotion.boneYaw(90f, 90f), TOLERANCE);
		assertEquals(90f, TrackSplineMotion.boneYaw(0f, 90f), TOLERANCE);
		assertEquals(-20f, TrackSplineMotion.boneYaw(-170f, 170f), TOLERANCE);
	}

	@Test
	void bonePitch_matchesSample() {
		assertEquals(6f, TrackSplineMotion.bonePitch(6f), TOLERANCE);
	}

	@Test
	void worldHeading_followsPlusSSampleNotMoveOrTravelSign() {
		ConvertedAngle east = TrackSplineMotion.worldHeading(
				new Vector(1, 10, 0),
				new TrackPose(0, 0, 0, -90f, 6f),
				-1);
		assertEquals(-90f, east.getYaw(), TOLERANCE);
		assertEquals(6f, east.getPitch(), TOLERANCE);
		ConvertedAngle snapDown = TrackSplineMotion.worldHeading(
				new Vector(0, -0.5, 0),
				new TrackPose(0, 0, 0, -97f, 0f),
				-1);
		assertEquals(-97f, snapDown.getYaw(), TOLERANCE);
		ConvertedAngle oppositeMove = TrackSplineMotion.worldHeading(
				new Vector(0, 0, -1),
				new TrackPose(0, 0, 0, 0f, 0f),
				-1);
		assertEquals(0f, oppositeMove.getYaw(), TOLERANCE);
	}

	@Test
	void tangentFromPose_southThenFlipped() {
		TrackPose south = new TrackPose(0, 0, 0, 0f, 0f);
		Vector plus = TrackSplineMotion.tangentFromPose(south, 1);
		assertEquals(0, plus.getX(), 1e-6);
		assertEquals(1, plus.getZ(), 1e-6);
		Vector minus = TrackSplineMotion.tangentFromPose(south, -1);
		assertEquals(0, minus.getX(), 1e-6);
		assertEquals(-1, minus.getZ(), 1e-6);
	}

	@Test
	void stopped_smallSpeed() {
		assertTrue(TrackSplineMotion.stopped(0));
		assertTrue(TrackSplineMotion.stopped(0.001));
		assertFalse(TrackSplineMotion.stopped(0.3));
	}
}
