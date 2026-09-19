package net.tfminecraft.VehicleFramework.Database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class OccupiedChunkIndexTest {
	@Test
	void emptyIndexIsUnoccupied() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		assertFalse(index.isOccupied("world", 1, 2));
		assertEquals(0, index.count("world", 1, 2));
	}

	@Test
	void twoVehiclesShareAChunkUntilLastIsRemoved() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive("a", "world", 1, 2);
		index.putLive("b", "world", 1, 2);
		assertTrue(index.isOccupied("world", 1, 2));
		assertEquals(2, index.count("world", 1, 2));
		index.remove("a");
		assertTrue(index.isOccupied("world", 1, 2));
		assertEquals(1, index.count("world", 1, 2));
		index.remove("b");
		assertFalse(index.isOccupied("world", 1, 2));
		assertEquals(0, index.count("world", 1, 2));
	}

	@Test
	void sameChunkSaveDoesNotChangeCount() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive("a", "world", 3, 4);
		index.putLive("a", "world", 3, 4);
		assertEquals(1, index.count("world", 3, 4));
	}

	@Test
	void moveVacatesOldChunkAndOccupiesNew() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive("a", "world", 1, 2);
		index.putLive("a", "world", 5, 6);
		assertFalse(index.isOccupied("world", 1, 2));
		assertTrue(index.isOccupied("world", 5, 6));
		assertEquals(1, index.count("world", 5, 6));
	}

	@Test
	void tombstoneIsIdempotent() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive("a", "world", 1, 2);
		index.remove("a");
		index.remove("a");
		assertFalse(index.isOccupied("world", 1, 2));
	}

	@Test
	void restoreAfterTombstoneReoccupies() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive("a", "world", 1, 2);
		index.remove("a");
		index.putLive("a", "world", 1, 2);
		assertTrue(index.isOccupied("world", 1, 2));
		assertEquals(1, index.count("world", 1, 2));
	}

	@Test
	void replaceClearsPreviousState() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive("old", "world", 0, 0);
		index.replace(List.of(new OccupiedChunkIndex.LiveLocation("new", "nether", 8, 9)));
		assertFalse(index.isOccupied("world", 0, 0));
		assertTrue(index.isOccupied("nether", 8, 9));
	}

	@Test
	void deletedSnapshotRemovesUuid() {
		OccupiedChunkIndex index = new OccupiedChunkIndex();
		index.putLive(live("a", "world", 1, 2, false));
		index.putLive(live("a", "world", 1, 2, true));
		assertFalse(index.isOccupied("world", 1, 2));
	}

	private static VehicleSnapshot live(String uuid, String world, int chunkX, int chunkZ, boolean deleted) {
		return new VehicleSnapshot(
				uuid,
				"horse_cart",
				world,
				0,
				64,
				0,
				0f,
				chunkX,
				chunkZ,
				"{}",
				VehicleRepository.SCHEMA_VERSION,
				1,
				deleted,
				1L);
	}
}
