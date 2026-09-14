package net.tfminecraft.VehicleFramework.Database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VehiclePersistenceTest {
	private static final String UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	private static final String PAYLOAD = """
			{
			  "id": "horse_cart",
			  "name": "Cart",
			  "skin": "horse_cart",
			  "owner": "player_Alice",
			  "yaw": 45.0,
			  "components": { "hull": { "damage": 0.0 } },
			  "rotators": {}
			}
			""";
	private static final String PAYLOAD_DAMAGED = """
			{
			  "id": "horse_cart",
			  "name": "Cart",
			  "skin": "horse_cart",
			  "owner": "player_Alice",
			  "yaw": 45.0,
			  "components": { "hull": { "damage": 20.0 } },
			  "rotators": {}
			}
			""";

	@TempDir
	Path tempDir;

	@Test
	void saveLiveStartsAtRevisionOneThenBumps() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 99)));
			assertEquals(1, repository.find(UUID).orElseThrow().getRevision());
			assertTrue(persistence.saveLive(snapshot(PAYLOAD_DAMAGED, 1)));
			VehicleSnapshot second = repository.find(UUID).orElseThrow();
			assertEquals(2, second.getRevision());
			assertTrue(second.getPayloadJson().contains("20.0"));
			assertEquals("player_Alice", second.getOwner());
			assertEquals("Cart", second.getName());
		} finally {
			repository.close();
		}
	}

	@Test
	void loadIncompleteReadsPayloadAndCorruptPayloadLeavesRow() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			Optional<IncompleteVehicle> loaded = persistence.loadIncomplete(UUID);
			assertTrue(loaded.isPresent());
			assertEquals("horse_cart", loaded.get().getId());

			assertTrue(persistence.saveLive(snapshot("{", 1)));
			assertTrue(repository.findLive(UUID).isPresent());
			assertTrue(persistence.loadIncomplete(UUID).isEmpty());
		} finally {
			repository.close();
		}
	}

	@Test
	void tombstoneHidesFromLiveQueriesUntilSaveLiveRestores() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			assertTrue(persistence.tombstone(UUID));
			assertTrue(repository.find(UUID).orElseThrow().isDeleted());
			assertTrue(persistence.findLive(UUID).isEmpty());
			assertTrue(repository.findChunk("world", 1, 2).isEmpty());

			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			assertFalse(repository.find(UUID).orElseThrow().isDeleted());
			assertTrue(persistence.findLive(UUID).isPresent());
		} finally {
			repository.close();
		}
	}

	@Test
	void ownerQueriesUseColumns() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			assertEquals(1, persistence.listByOwner("player_Alice").size());
			assertEquals(1, persistence.countByOwner("player_Alice", List.of()).getOrDefault("horse_cart", 0));
			assertEquals(1, persistence.listPlayerOwned().size());
			assertEquals("Cart", persistence.readMeta(UUID).orElseThrow().getName());
		} finally {
			repository.close();
		}
	}

	@Test
	void payloadMutationBumpsRevision() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			VehicleSnapshot first = repository.findLive(UUID).orElseThrow();
			VehicleSnapshot damaged = first.withPayload(PAYLOAD_DAMAGED, first.getName(), first.getOwner(), 99L);
			assertTrue(persistence.saveLive(damaged));
			VehicleSnapshot second = repository.findLive(UUID).orElseThrow();
			assertEquals(first.getRevision() + 1, second.getRevision());
			assertTrue(second.getPayloadJson().contains("20.0"));
		} finally {
			repository.close();
		}
	}

	@Test
	void saveLiveReturnsFalseWhenRepositoryClosed() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		VehiclePersistence persistence = new VehiclePersistence(repository);
		repository.close();
		assertFalse(persistence.saveLive(snapshot(PAYLOAD, 1)));
	}

	@Test
	void tombstoneMissingUuidIsFalseAndRepeatIsIdempotent() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertFalse(persistence.tombstone("missing-uuid"));
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			assertTrue(persistence.tombstone(UUID));
			assertTrue(persistence.tombstone(UUID));
			assertTrue(repository.find(UUID).orElseThrow().isDeleted());
			assertTrue(persistence.findLive(UUID).isEmpty());
		} finally {
			repository.close();
		}
	}

	@Test
	void tombstoneWithoutRemoveLeavesDeletedRow() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot(PAYLOAD, 1)));
			assertTrue(persistence.tombstone(UUID));
			assertTrue(persistence.findLive(UUID).isEmpty());
			assertTrue(repository.find(UUID).orElseThrow().isDeleted());
		} finally {
			repository.close();
		}
	}

	@Test
	void failedDecodeDoesNotTombstone() {
		VehicleRepository repository = VehicleRepository.open(tempDir.resolve("vehicles.db").toFile());
		try {
			VehiclePersistence persistence = new VehiclePersistence(repository);
			assertTrue(persistence.saveLive(snapshot("{", 1)));
			assertTrue(persistence.loadIncomplete(UUID).isEmpty());
			assertTrue(persistence.findLive(UUID).isPresent());
			assertFalse(repository.find(UUID).orElseThrow().isDeleted());
		} finally {
			repository.close();
		}
	}

	private static VehicleSnapshot snapshot(String payload, int ignoredRevision) {
		return new VehicleSnapshot(
				UUID,
				"horse_cart",
				"Cart",
				"player_Alice",
				"world",
				20.0,
				64.0,
				40.0,
				45f,
				1,
				2,
				payload,
				VehicleRepository.SCHEMA_VERSION,
				ignoredRevision,
				false,
				1L);
	}
}
