package net.tfminecraft.VehicleFramework.Weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

class WeaponAimAlignerTest {

	private static final float TOLERANCE = 0.5f;

	@Test
	void fromDirection_south_yawNearZero() {
		ConvertedAngle angles = ConvertedAngle.fromDirection(new Vector(0, 0, 1));
		assertNear(0f, angles.getYaw());
		assertNear(0f, angles.getPitch());
	}

	@Test
	void fromDirection_east_yawNearNegativeNinety() {
		ConvertedAngle angles = ConvertedAngle.fromDirection(new Vector(1, 0, 0));
		assertNear(-90f, angles.getYaw());
	}

	@Test
	void shortestDelta_wrapsAcrossOneEighty() {
		assertEquals(20f, ConvertedAngle.shortestDelta(170f, -170f), TOLERANCE);
		assertEquals(-20f, ConvertedAngle.shortestDelta(-170f, 170f), TOLERANCE);
	}

	@Test
	void yawError_targetAhead_returnsNearZero() {
		Vector current = new Vector(0, 0, 1);
		Vector desired = new Vector(0, 0, 1);
		assertNear(0f, WeaponAimAligner.yawError(current, desired));
	}

	@Test
	void yawError_targetEast_returnsNegativeNinety() {
		Vector current = new Vector(0, 0, 1);
		Vector desired = new Vector(1, 0, 0);
		assertNear(-90f, WeaponAimAligner.yawError(current, desired));
	}

	@Test
	void yawError_targetWest_returnsNinety() {
		Vector current = new Vector(0, 0, 1);
		Vector desired = new Vector(-1, 0, 0);
		assertNear(90f, WeaponAimAligner.yawError(current, desired));
	}

	@Test
	void yawError_oppositeDirection_isOneEightyWithoutPitchCorruption() {
		Vector current = new Vector(0, 0, 1);
		Vector desired = new Vector(0, 0, -1);
		float yaw = WeaponAimAligner.yawError(current, desired);
		assertEquals(180f, Math.abs(yaw), TOLERANCE);
		assertNear(0f, WeaponAimAligner.elevationError(current, desired, "x"));
	}

	@Test
	void elevationError_elevatedTarget_negativeMinecraftPitch() {
		Vector current = new Vector(0, 0, 1);
		Vector desired = new Vector(0, 1, 1);
		float error = WeaponAimAligner.elevationError(current, desired, "x");
		assertTrue(error < 0f);
		assertNear(-45f, error);
	}

	@Test
	void elevationError_elevatedTarget_headAxisZ_sameWorldPitch() {
		Vector current = new Vector(0, 0, 1);
		Vector desired = new Vector(0, 1, 1);
		float error = WeaponAimAligner.elevationError(current, desired, "z");
		assertTrue(error < 0f);
		assertNear(-45f, error);
	}

	@Test
	void followStep_whileTracking_easesSmallErrorInsteadOfSnapping() {
		float step = WeaponAimAligner.followStep(3f, 0.5f, false);
		assertTrue(step > 0f);
		assertTrue(step < 3f);
		assertTrue(step <= 0.5f);
	}

	@Test
	void followStep_nearTarget_isMuchSlowerThanFarTarget() {
		float near = Math.abs(WeaponAimAligner.followStep(4f, 0.5f, false));
		float far = Math.abs(WeaponAimAligner.followStep(40f, 0.5f, false));
		assertTrue(near < far * 0.6f);
	}

	@Test
	void followStep_whileSettled_ignoresErrorInsideStartLeeway() {
		assertEquals(0f, WeaponAimAligner.followStep(4f, 0.5f, true));
	}

	@Test
	void followStep_whileSettled_startsAgainPastStartLeeway() {
		assertEquals(-0.5f, WeaponAimAligner.followStep(-90f, 0.5f, true), 0.02f);
	}

	@Test
	void followStep_largeError_capsAtRate() {
		assertEquals(0.5f, WeaponAimAligner.followStep(90f, 0.5f, false), 0.02f);
		assertEquals(-0.5f, WeaponAimAligner.followStep(-90f, 0.5f, false), 0.02f);
	}

	@Test
	void updateSettled_stopsNearTarget_andNeedsLargerLeewayToRestart() {
		assertTrue(WeaponAimAligner.updateSettled(0.5f, false));
		assertTrue(WeaponAimAligner.updateSettled(4f, true));
		assertTrue(!WeaponAimAligner.updateSettled(7f, true));
	}

	@Test
	void toBoneElevationStep_negatesMinecraftPitchTowardWasdUp() {
		assertEquals(60f, WeaponAimAligner.toBoneElevationStep(-60f));
	}

	@Test
	void toBoneYawStep_negatesMinecraftYawTowardWasd() {
		assertEquals(142f, WeaponAimAligner.toBoneYawStep(-142f));
	}

	@Test
	void applyToAngles_bodyYawOffsetsWorldYaw() {
		WeaponAimOffset offset = new WeaponAimOffset(90f, 0f, 0f, 0f);
		ConvertedAngle desired = ConvertedAngle.fromDirection(new Vector(0, 0, 1));
		ConvertedAngle adjusted = offset.applyToAngles(desired, "x");
		assertNear(90f, adjusted.getYaw());
	}

	private static void assertNear(float expected, float actual) {
		assertEquals(expected, actual, TOLERANCE);
	}
}
