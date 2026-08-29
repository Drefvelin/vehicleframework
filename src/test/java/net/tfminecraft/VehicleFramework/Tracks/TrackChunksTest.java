package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrackChunksTest {

	@Test
	void chunkCoord_floorsNegatives() {
		assertEquals(0, TrackChunks.chunkCoord(0));
		assertEquals(0, TrackChunks.chunkCoord(15.9));
		assertEquals(1, TrackChunks.chunkCoord(16));
		assertEquals(-1, TrackChunks.chunkCoord(-0.1));
		assertEquals(-1, TrackChunks.chunkCoord(-16));
		assertEquals(-2, TrackChunks.chunkCoord(-16.1));
	}

	@Test
	void inChunk_usesBlockXZ() {
		assertEquals(true, TrackChunks.inChunk(16.2, 32.9, 1, 2));
		assertEquals(false, TrackChunks.inChunk(15.9, 32.9, 1, 2));
	}

	@Test
	void edgeIndexForSample_openSplineLastMapsToLastEdge() {
		assertEquals(0, TrackChunks.edgeIndexForSample(0, 3, false));
		assertEquals(1, TrackChunks.edgeIndexForSample(1, 3, false));
		assertEquals(1, TrackChunks.edgeIndexForSample(2, 3, false));
	}

	@Test
	void edgeIndexForSample_loopUsesAllSamples() {
		assertEquals(0, TrackChunks.edgeIndexForSample(0, 3, true));
		assertEquals(1, TrackChunks.edgeIndexForSample(1, 3, true));
		assertEquals(2, TrackChunks.edgeIndexForSample(2, 3, true));
	}
}
