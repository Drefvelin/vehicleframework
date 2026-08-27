package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.TerrainFollowConfig;

class TerrainFollowMathTest {

	@Test
	void disabledConfig_isNotEnabled() {
		assertFalse(TerrainFollowConfig.disabled().isEnabled());
		assertEquals(1.0, TerrainFollowConfig.disabled().getStepHeight());
		assertEquals(0.25, TerrainFollowConfig.disabled().getSnapSpeed());
		assertEquals(3.0, TerrainFollowConfig.disabled().getClimbLeadTicks());
		assertEquals(1.0, TerrainFollowConfig.disabled().getClimbLeadFactor());
		assertEquals(0.08, TerrainFollowConfig.disabled().getAirGravity());
		assertEquals(0.98, TerrainFollowConfig.disabled().getAirDrag());
		assertTrue(TerrainFollowConfig.disabled().getGroundProbes().isEmpty());
	}

	@Test
	void emptyProbes_useBodyFallback() {
		assertTrue(TerrainFollowMath.usesBodyFallback(List.of()));
		assertTrue(TerrainFollowMath.usesBodyFallback(null));
		assertFalse(TerrainFollowMath.usesBodyFallback(List.of("ground_fl")));
	}

	@Test
	void classifyForward_stepVsWall() {
		assertEquals(TerrainFollowMath.ForwardObstacle.NONE, TerrainFollowMath.classifyForward(64.0, 64.0, 1.0));
		assertEquals(TerrainFollowMath.ForwardObstacle.STEP, TerrainFollowMath.classifyForward(64.0, 65.0, 1.0));
		assertEquals(TerrainFollowMath.ForwardObstacle.WALL, TerrainFollowMath.classifyForward(64.0, 66.0, 1.0));
	}

	@Test
	void destinationOverlaps_detectsIntersection() {
		assertTrue(TerrainFollowMath.destinationOverlaps(
				0, 1, 0, 2, 0, 1,
				0.5, 1.5, 0, 1, 0.5, 1.5));
		assertFalse(TerrainFollowMath.destinationOverlaps(
				0, 1, 0, 2, 0, 1,
				2, 3, 0, 1, 2, 3));
	}

	@Test
	void tilt_southFacing_frontHigher_matchesFromDirectionPitch() {
		Vector forward = new Vector(0, 0.4, 2);
		Vector right = new Vector(1, 0, 0);
		TerrainFollowMath.Tilt tilt = TerrainFollowMath.tiltFromWorldAxes(forward, right);
		assertEquals(ConvertedAngle.fromDirection(forward).getPitch(), tilt.pitchDeg, 0.01);
		assertEquals(0.0, tilt.rollDeg, 0.2);
		assertTrue(tilt.pitchDeg < 0);
	}

	@Test
	void tilt_eastFacing_frontHigher_isStillPitch() {
		Vector forward = new Vector(2, 0.4, 0);
		Vector right = new Vector(0, 0, -1);
		TerrainFollowMath.Tilt tilt = TerrainFollowMath.tiltFromWorldAxes(forward, right);
		assertEquals(ConvertedAngle.fromDirection(forward).getPitch(), tilt.pitchDeg, 0.01);
		assertEquals(0.0, tilt.rollDeg, 0.2);
		assertTrue(Math.abs(tilt.pitchDeg) > 1.0);
	}

	@Test
	void tilt_rightHigher_matchesFromDirectionRoll() {
		Vector forward = new Vector(0, 0, 2);
		Vector right = new Vector(1, 0.4, 0);
		TerrainFollowMath.Tilt tilt = TerrainFollowMath.tiltFromWorldAxes(forward, right);
		assertEquals(ConvertedAngle.fromDirection(right).getPitch(), tilt.rollDeg, 0.01);
		assertEquals(0.0, tilt.pitchDeg, 0.2);
	}

	@Test
	void tilt_clampsAboveTwentyFive() {
		Vector forward = new Vector(0, 10, 1);
		TerrainFollowMath.Tilt tilt = TerrainFollowMath.tiltFromWorldAxes(forward, new Vector(1, 0, 0));
		assertEquals(-TerrainFollowMath.TILT_CLAMP, tilt.pitchDeg, 0.01);
	}

	@Test
	void approachY_doesNotExceedMaxDelta() {
		assertEquals(10.25, TerrainFollowMath.approachY(10.0, 11.0, 0.25), 1e-9);
		assertEquals(9.75, TerrainFollowMath.approachY(10.0, 9.0, 0.25), 1e-9);
		assertEquals(10.1, TerrainFollowMath.approachY(10.0, 10.1, 0.25), 1e-9);
	}

	@Test
	void approachY_isNotUsedForFreeFall() {
		double gravityDrop = 64.0 - 0.08;
		double ifCappedTowardDistantFloor = TerrainFollowMath.approachY(64.0, 50.0, 0.25);
		assertEquals(63.92, gravityDrop, 1e-9);
		assertEquals(63.75, ifCappedTowardDistantFloor, 1e-9);
		assertTrue(Math.abs(gravityDrop - ifCappedTowardDistantFloor) > 1e-6);
	}

	@Test
	void farthestUnblocked_wallAtPointThree_stopsBefore() {
		double far = TerrainFollowMath.farthestUnblocked(0.65, TerrainFollowMath.SLIDE_STEP, d -> d >= 0.3);
		assertEquals(0.28, far, 1e-9);
	}

	@Test
	void farthestUnblocked_clearPath_takesFullDistance() {
		assertEquals(0.65, TerrainFollowMath.farthestUnblocked(0.65, TerrainFollowMath.SLIDE_STEP, d -> false), 1e-9);
	}

	@Test
	void farthestUnblocked_slowMaxDist_findsClearBelowMax() {
		double far = TerrainFollowMath.farthestUnblocked(0.07, TerrainFollowMath.SLIDE_STEP, d -> d >= 0.07 - 1e-12);
		assertEquals(0.06, far, 1e-9);
	}

	@Test
	void farthestUnblocked_firstSampleBlocked_returnsZero() {
		assertEquals(0.0, TerrainFollowMath.farthestUnblocked(0.07, TerrainFollowMath.SLIDE_STEP, d -> d >= 0.02 - 1e-12), 1e-9);
	}

	@Test
	void headingFromYaw_minecraftCardinals() {
		Vector south = TerrainFollowMath.headingFromYaw(0f);
		assertEquals(0.0, south.getX(), 1e-9);
		assertEquals(0.0, south.getY(), 1e-9);
		assertEquals(1.0, south.getZ(), 1e-9);
		Vector west = TerrainFollowMath.headingFromYaw(90f);
		assertEquals(-1.0, west.getX(), 1e-9);
		assertEquals(0.0, west.getY(), 1e-9);
		assertEquals(0.0, west.getZ(), 1e-9);
	}

	@Test
	void supportY_mixedHeights_usesMax() {
		double[] hitY = {161.0, 160.5, 161.0, 160.5};
		boolean[] hit = {true, true, true, true};
		assertEquals(161.0, TerrainFollowMath.supportY(hitY, hit), 1e-9);
	}

	@Test
	void supportY_ignoresMisses() {
		double[] hitY = {160.5, 0.0};
		boolean[] hit = {true, false};
		assertEquals(160.5, TerrainFollowMath.supportY(hitY, hit), 1e-9);
		assertTrue(Double.isNaN(TerrainFollowMath.supportY(hitY, new boolean[] {false, false})));
	}

	@Test
	void raiseThenSlide_curbBlockedAtCurrentY_raisesInPlace() {
		TerrainFollowMath.OffsetYBlocked curb = (ox, oz, y) -> Math.hypot(ox, oz) > 1e-6 && y < 161.0 - 1e-6;
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 160.0, 160.0, 160.0, 161.0, 0.25, curb);
		assertEquals(160.25, move.y, 1e-9);
		assertEquals(0.0, move.slideDist(), 1e-9);
		assertEquals("up", move.path);
	}

	@Test
	void raiseThenSlide_atStepTop_slidesFullDistance() {
		TerrainFollowMath.OffsetYBlocked curb = (ox, oz, y) -> Math.hypot(ox, oz) > 1e-6 && y < 161.0 - 1e-6;
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 161.0, 161.0, 160.0, 161.0, 0.25, curb);
		assertEquals(161.0, move.y, 1e-9);
		assertEquals(0.65, move.slideDist(), 1e-9);
		assertEquals("slide", move.path);
	}

	@Test
	void raiseThenSlide_noGround_fallsAtCurrentXz() {
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.0, 0.0, 64.0, 63.92, Double.NaN, Double.NaN, 0.25, (ox, oz, y) -> false);
		assertEquals(63.92, move.y, 1e-9);
		assertEquals("fall", move.path);
		assertEquals(0.0, move.slideDist(), 1e-9);
	}

	@Test
	void raiseThenSlide_downhill_slidesThenDrops() {
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 161.0, 160.5, 160.5, Double.NaN, 0.25, (ox, oz, y) -> false);
		assertEquals(160.5, move.y, 1e-9);
		assertEquals(0.65, move.slideDist(), 1e-9);
		assertEquals("slide+down", move.path);
	}

	@Test
	void raiseThenSlide_downhillBlocked_keepsSlideY() {
		TerrainFollowMath.OffsetYBlocked lowCeiling = (ox, oz, y) -> y < 160.75;
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 161.0, 160.5, 160.5, Double.NaN, 0.25, lowCeiling);
		assertEquals(161.0, move.y, 1e-9);
		assertEquals(0.65, move.slideDist(), 1e-9);
		assertEquals("slide", move.path);
	}

	@Test
	void raiseThenSlide_ceilingAndWall_stays() {
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 160.0, 160.0, 160.0, 161.0, 0.25, (ox, oz, y) -> true);
		assertEquals(160.0, move.y, 1e-9);
		assertEquals(0.0, move.slideDist(), 1e-9);
		assertEquals("stay", move.path);
	}

	@Test
	void raiseThenSlide_blockedFace_doesNotDropAfterRaise() {
		TerrainFollowMath.OffsetYBlocked wall = (ox, oz, y) -> Math.hypot(ox, oz) > 1e-6;
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 160.25, 160.0, 160.0, Double.NaN, 0.25, wall);
		assertEquals(160.25, move.y, 1e-9);
		assertEquals("stay", move.path);
		assertEquals(0.0, move.slideDist(), 1e-9);
	}

	@Test
	void raiseThenSlide_belowStepTop_doesNotSlideEvenIfXzClear() {
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 160.0, 160.0, 160.0, 161.0, 0.25, (ox, oz, y) -> false);
		assertEquals(160.25, move.y, 1e-9);
		assertEquals(0.0, move.slideDist(), 1e-9);
		assertEquals("up", move.path);
	}

	@Test
	void raiseThenSlide_headingBlocked_slidesAlongZ() {
		TerrainFollowMath.OffsetYBlocked plusXWall = (ox, oz, y) -> ox > 1e-6;
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.30, 161.0, 161.0, 161.0, Double.NaN, 0.25, plusXWall);
		assertEquals(0.0, move.offsetX, 1e-9);
		assertEquals(0.30, move.offsetZ, 1e-9);
		assertEquals("slideZ", move.path);
		assertEquals(161.0, move.y, 1e-9);
	}

	@Test
	void effectiveSnap_addsSpeedTimesFactorCappedByStepHeight() {
		assertEquals(0.65, TerrainFollowMath.effectiveSnap(0.25, 0.4, 1.0, 1.0), 1e-9);
		assertEquals(1.0, TerrainFollowMath.effectiveSnap(0.25, 2.0, 1.0, 1.0), 1e-9);
		assertEquals(0.25, TerrainFollowMath.effectiveSnap(0.25, 0.0, 1.0, 1.0), 1e-9);
	}

	@Test
	void mergeClimbSupport_prefersHigherLookahead() {
		assertEquals(161.0, TerrainFollowMath.mergeClimbSupport(160.0, 161.0), 1e-9);
		assertEquals(160.0, TerrainFollowMath.mergeClimbSupport(160.0, 159.0), 1e-9);
		assertEquals(160.0, TerrainFollowMath.mergeClimbSupport(160.0, Double.NaN), 1e-9);
		assertTrue(Double.isNaN(TerrainFollowMath.mergeClimbSupport(Double.NaN, 161.0)));
	}

	@Test
	void airborneTick_appliesDragAndGravityAccel() {
		TerrainFollowMath.AirborneTick first = TerrainFollowMath.airborneTick(0.4, 0.0, 0.0, 0.98, 0.08);
		assertEquals(0.4 * 0.98, first.vx, 1e-9);
		assertEquals(-0.08, first.vy, 1e-9);
		assertEquals(0.0, first.vz, 1e-9);
		TerrainFollowMath.AirborneTick second = TerrainFollowMath.airborneTick(first.vx, first.vy, first.vz, 0.98, 0.08);
		assertEquals(-0.16, second.vy, 1e-9);
		assertEquals(0.4 * 0.98 * 0.98, second.vx, 1e-9);
	}

	@Test
	void raiseThenSlide_raiseBlocked_backsThenRaises() {
		TerrainFollowMath.OffsetYBlocked lip = (ox, oz, y) -> {
			if (Math.abs(ox) < 1e-6) {
				return y > 160.0 + 1e-6;
			}
			return ox > -0.09;
		};
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				0.65, 0.0, 160.0, 160.0, 160.0, 161.0, 0.25, lip, false);
		assertEquals(160.25, move.y, 1e-9);
		assertEquals(-0.1, move.offsetX, 1e-9);
		assertEquals(0.0, move.offsetZ, 1e-9);
		assertEquals("back+up", move.path);
	}

	@Test
	void raiseThenSlide_reverse_skipsBackThenRaise() {
		TerrainFollowMath.OffsetYBlocked lip = (ox, oz, y) -> {
			if (Math.abs(ox) < 1e-6) {
				return y > 160.0 + 1e-6;
			}
			return ox > -0.09;
		};
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				-0.07, 0.0, 160.0, 160.0, 160.0, 161.0, 0.25, lip, true);
		assertEquals(160.0, move.y, 1e-9);
		assertEquals(0.0, move.slideDist(), 1e-9);
		assertEquals("stay", move.path);
	}
}
