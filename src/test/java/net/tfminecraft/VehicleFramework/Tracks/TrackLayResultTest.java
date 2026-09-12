package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrackLayResultTest {

	@Test
	void shouldAnnounce_newNeedsTwoSamples() {
		assertFalse(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.NEW, 0, 0));
		assertFalse(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.NEW, 0, 1));
		assertTrue(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.NEW, 0, 5));
	}

	@Test
	void shouldAnnounce_appendOnlyIfExtraKept() {
		assertFalse(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.APPEND, 10, 10));
		assertTrue(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.APPEND, 10, 12));
		assertFalse(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.PREPEND, 8, 8));
		assertTrue(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.PREPEND, 8, 9));
	}

	@Test
	void shouldAnnounce_connectIfJoined() {
		assertTrue(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.CONNECT, 0, 20));
		assertFalse(TrackLayResult.shouldAnnounce(TrackLayResult.Kind.CONNECT, 0, 1));
	}
}
