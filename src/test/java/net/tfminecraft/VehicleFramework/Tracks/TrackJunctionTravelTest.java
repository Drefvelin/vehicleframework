package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrackJunctionTravelTest {

	@Test
	void choice_defaultAndWrongSideAreThrough() {
		assertEquals(TrackJunctionTravel.Choice.THROUGH, TrackJunctionTravel.choice(TrackJunction.Side.LEFT, 0));
		assertEquals(TrackJunctionTravel.Choice.THROUGH, TrackJunctionTravel.choice(TrackJunction.Side.LEFT, -1));
		assertEquals(TrackJunctionTravel.Choice.THROUGH, TrackJunctionTravel.choice(TrackJunction.Side.RIGHT, 1));
	}

	@Test
	void choice_matchingHoldDiverges() {
		assertEquals(TrackJunctionTravel.Choice.DIVERGE, TrackJunctionTravel.choice(TrackJunction.Side.LEFT, TrackJunction.Side.LEFT));
		assertEquals(TrackJunctionTravel.Choice.DIVERGE, TrackJunctionTravel.choice(TrackJunction.Side.RIGHT, TrackJunction.Side.RIGHT));
		assertEquals(TrackJunctionTravel.Choice.THROUGH, TrackJunctionTravel.choice(TrackJunction.Side.LEFT, TrackJunction.Side.RIGHT));
	}

	@Test
	void armWindow_inRangeVsTooFar() {
		assertTrue(TrackJunctionTravel.inArmWindow(12, 20, 1, false, 40));
		assertFalse(TrackJunctionTravel.inArmWindow(1, 20, 1, false, 40));
		assertFalse(TrackJunctionTravel.inArmWindow(10, 20, 1, false, 40, 8));
		assertEquals(8, TrackJunctionTravel.ahead(12, 20, 1, false, 40), 1e-9);
		assertTrue(TrackJunctionTravel.inArmWindow(20, 12, -1, false, 40));
		assertFalse(TrackJunctionTravel.inArmWindow(10, 20, -1, false, 40));
	}

	@Test
	void crosses_openTrack() {
		assertTrue(TrackJunctionTravel.crosses(10, 10.4, 10.2, 1, false, 40));
		assertFalse(TrackJunctionTravel.crosses(10.2, 10.4, 10.2, 1, false, 40));
		assertTrue(TrackJunctionTravel.crosses(10.4, 10, 10.2, -1, false, 40));
		assertFalse(TrackJunctionTravel.crosses(5, 10, 20, 1, false, 40));
	}

	@Test
	void crosses_loopWrap() {
		assertTrue(TrackJunctionTravel.crosses(78, 2, 79, 1, true, 80));
		assertFalse(TrackJunctionTravel.crosses(78, 2, 50, 1, true, 80));
	}

	@Test
	void rewind_branchShortOfFrogGoesToStem() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				branch, 3, 1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, pose.splineId);
		assertEquals(13, pose.s, 1e-9);
	}

	@Test
	void rewind_branchFarStaysOnBranch() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				branch, 12, 1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, pose.splineId);
		assertEquals(2, pose.s, 1e-9);
	}

	@Test
	void rewind_branchReverse_staysOnBranch() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				branch, 12, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, pose.splineId);
		assertEquals(22, pose.s, 1e-9);
	}

	@Test
	void rewind_branchReverse_nearFrog_staysOnBranch() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				branch, 3, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, pose.splineId);
		assertEquals(13, pose.s, 1e-9);
	}

	@Test
	void rewind_branchReverse_pastTip_spillsToStem() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				branch, 27, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, pose.splineId);
		assertEquals(27, pose.s, 1e-9);
	}

	@Test
	void rewind_stemReverse_beforeFrog_staysOnStem() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 19, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, pose.splineId);
		assertEquals(29, pose.s, 1e-9);
	}

	@Test
	void rewind_stemForward_spansFrog_spillsToBranch() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 25, 1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, pose.splineId);
		assertEquals(5, pose.s, 1e-9);
	}

	@Test
	void rewind_stemForward_beforeFrog_staysOnStem() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 15, 1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, pose.splineId);
		assertEquals(5, pose.s, 1e-9);
	}

	@Test
	void rewind_stemForward_pastFrog_staysOnStem() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 30, 1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, pose.splineId);
		assertEquals(20, pose.s, 1e-9);
	}

	@Test
	void rewind_stemReverse_pastFrog_staysOnStem() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 25, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, pose.splineId);
		assertEquals(35, pose.s, 1e-9);
	}

	@Test
	void rewind_consistChain_branchUsesParentTravelSign() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose car1 = TrackJunctionTravel.rewind(
				branch, 12, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, car1.splineId);
		assertEquals(22, car1.s, 1e-9);
		int parentTravelSign = 1;
		TrackJunctionTravel.Pose car2 = TrackJunctionTravel.rewind(
				branch, car1.s, parentTravelSign, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, car2.splineId);
		assertEquals(12, car2.s, 1e-9);
	}

	@Test
	void rewind_consistChain_stemForwardSpansFrog() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose car1 = TrackJunctionTravel.rewind(
				stem, 25, 1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, car1.splineId);
		assertEquals(5, car1.s, 1e-9);
		int parentTravelSign = 1;
		TrackJunctionTravel.Pose car2 = TrackJunctionTravel.rewind(
				branch, car1.s, parentTravelSign, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, car2.splineId);
		assertEquals(15, car2.s, 1e-9);
		TrackJunctionTravel.Pose car3 = TrackJunctionTravel.rewind(
				stem, car2.s, parentTravelSign, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(stem, car3.splineId);
		assertEquals(5, car3.s, 1e-9);
	}

	@Test
	void rewind_openReverse_trailsBehind() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 60, -1, 20, false, stem, null, 0, 1, 100, false, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(80, pose.s, 1e-9);
	}

	@Test
	void rewind_openForward_trailsBehind() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 60, 1, 20, false, stem, null, 0, 1, 100, false, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(40, pose.s, 1e-9);
	}

	@Test
	void rewind_loopReverse_wrapsPastEnd() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 80, -1, 40, false, stem, null, 0, 1, 100, true, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(20, pose.s, 1e-9);
	}

	@Test
	void rewind_loopReverse_clampsWhenNotLoop() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 80, -1, 40, false, stem, null, 0, 1, 100, false, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(100, pose.s, 1e-9);
	}

	@Test
	void rewind_loopReverse_nearSeam() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 2, -1, 50, false, stem, null, 0, 1, 100, true, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(52, pose.s, 1e-9);
	}

	@Test
	void rewind_loopReverse_crossesSeam() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 90, -1, 30, false, stem, null, 0, 1, 100, true, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(20, pose.s, 1e-9);
	}

	@Test
	void rewind_loopForward_wrapsPastStart() {
		UUID stem = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 10, 1, 50, false, stem, null, 0, 1, 100, true, 0);
		assertEquals(stem, pose.splineId);
		assertEquals(60, pose.s, 1e-9);
	}

	@Test
	void facing_matchesTravel() {
		assertTrue(TrackJunctionTravel.facing(1, 1));
		assertTrue(TrackJunctionTravel.facing(-1, -1));
		assertFalse(TrackJunctionTravel.facing(1, -1));
	}
}
