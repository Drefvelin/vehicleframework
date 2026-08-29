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
	void rewind_mergedStemPutsLeftoverOnBranch() {
		UUID stem = UUID.randomUUID();
		UUID branch = UUID.randomUUID();
		TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
				stem, 19, -1, 10, true, stem, branch, 20, 1, 40, false, 30);
		assertEquals(branch, pose.splineId);
		assertEquals(9, pose.s, 1e-9);
	}

	@Test
	void facing_matchesTravel() {
		assertTrue(TrackJunctionTravel.facing(1, 1));
		assertTrue(TrackJunctionTravel.facing(-1, -1));
		assertFalse(TrackJunctionTravel.facing(1, -1));
	}
}
