package net.tfminecraft.VehicleFramework.Database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VehicleRepositoryTest {
	private static final String PAYLOAD = """
			{
			  "id": "horse_cart",
			  "name": "Cart",
			  "skin": "horse_cart",
			  "owner": "player_Alice",
			  "yaw": 12.5,
			  "components": {},
			  "rotators": {}
			}
			""";

	@TempDir
	Path tempDir;

	@Test
	void codec_decodeEncodeRoundTrip() {
		Optional<IncompleteVehicle> decoded = VehiclePayloadCodec.decode(PAYLOAD, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		assertTrue(decoded.isPresent());
		IncompleteVehicle vehicle = decoded.get();
		assertEquals("horse_cart", vehicle.getId());
		assertEquals("player_Alice", vehicle.getOwner());
		assertEquals(12.5f, vehicle.getYaw(), 0.001f);

		Optional<IncompleteVehicle> again = VehiclePayloadCodec.decode(
				VehiclePayloadCodec.encode(vehicle),
				vehicle.getUUID());
		assertTrue(again.isPresent());
		assertEquals("horse_cart", again.get().getId());
		assertEquals("player_Alice", again.get().getOwner());
		assertEquals(12.5f, again.get().getYaw(), 0.001f);
	}

	@Test
	void codec_corruptPayloadReturnsEmpty() {
		assertTrue(VehiclePayloadCodec.decode("{", "uuid").isEmpty());
		assertTrue(VehiclePayloadCodec.decode("{\"name\":\"no-id\"}", "uuid").isEmpty());
	}

	@Test
	void saveLiveBumpsRevisionAtomically() {
		File dbFile = tempDir.resolve("vehicles-live.db").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			VehicleSnapshot first = snapshot("u1", "world", 10, 20, 99);
			assertTrue(repository.saveLive(first));
			assertEquals(1, repository.find("u1").orElseThrow().getRevision());
			assertTrue(repository.saveLive(first));
			assertEquals(2, repository.find("u1").orElseThrow().getRevision());
			assertEquals(1, repository.tombstone("u1"));
			assertTrue(repository.findLive("u1").isEmpty());
		} finally {
			repository.close();
		}
	}

	@Test
	void upsertFindRoundTrip() {
		File dbFile = tempDir.resolve("vehicles.db").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			VehicleSnapshot snapshot = snapshot("u1", "world", 10, 20, 1);
			repository.upsert(snapshot);
			Optional<VehicleSnapshot> found = repository.find("u1");
			assertTrue(found.isPresent());
			assertEquals("horse_cart", found.get().getTypeId());
			assertEquals("world", found.get().getWorld());
			assertEquals(1.5, found.get().getX(), 0.0001);
			assertEquals(64, found.get().getY(), 0.0001);
			assertEquals(2.5, found.get().getZ(), 0.0001);
			assertEquals(12.5f, found.get().getYaw(), 0.001f);
			assertEquals(10, found.get().getChunkX());
			assertEquals(20, found.get().getChunkZ());
			assertTrue(found.get().getPayloadJson().contains("horse_cart"));
			assertEquals(1, found.get().getRevision());
			assertFalse(found.get().isDeleted());
		} finally {
			repository.close();
		}
	}

	@Test
	void olderRevisionDoesNotOverwrite() {
		File dbFile = tempDir.resolve("vehicles.db").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			repository.upsert(snapshot("u1", "world", 0, 0, 5));
			VehicleSnapshot older = new VehicleSnapshot(
					"u1",
					"sloop",
					"world",
					0,
					64,
					0,
					0f,
					0,
					0,
					PAYLOAD,
					VehicleRepository.SCHEMA_VERSION,
					3,
					false,
					1L);
			repository.upsert(older);
			assertEquals("horse_cart", repository.find("u1").orElseThrow().getTypeId());
			assertEquals(5, repository.find("u1").orElseThrow().getRevision());
		} finally {
			repository.close();
		}
	}

	@Test
	void tombstoneHiddenFromLiveQueries() {
		File dbFile = tempDir.resolve("vehicles.db").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			repository.upsert(snapshot("u1", "world", 3, 4, 1));
			assertEquals(1, repository.tombstone("u1", 2, 99L));
			assertTrue(repository.find("u1").isPresent());
			assertTrue(repository.find("u1").orElseThrow().isDeleted());
			assertTrue(repository.findLive("u1").isEmpty());
			assertTrue(repository.findChunk("world", 3, 4).isEmpty());
			assertTrue(repository.listAllLive().isEmpty());
		} finally {
			repository.close();
		}
	}

	@Test
	void findNearFiltersWorldAndRadius() {
		File dbFile = tempDir.resolve("vehicles.db").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			repository.upsert(snapshot("center", "overworld", 10, 10, 1));
			repository.upsert(snapshot("near", "overworld", 11, 10, 1));
			repository.upsert(snapshot("far", "overworld", 20, 10, 1));
			repository.upsert(snapshot("other", "nether", 10, 10, 1));
			List<VehicleSnapshot> near = repository.findNear("overworld", 10, 10, 1);
			assertEquals(2, near.size());
			assertTrue(near.stream().anyMatch(row -> row.getUuid().equals("center")));
			assertTrue(near.stream().anyMatch(row -> row.getUuid().equals("near")));
			assertTrue(near.stream().noneMatch(row -> row.getUuid().equals("far")));
			assertTrue(near.stream().noneMatch(row -> row.getUuid().equals("other")));
		} finally {
			repository.close();
		}
	}

	@Test
	void openFailsOnCorruptFile() throws Exception {
		File dbFile = tempDir.resolve("corrupt.db").toFile();
		Files.writeString(dbFile.toPath(), "this is not a sqlite database");
		assertThrows(RuntimeException.class, () -> VehicleRepository.open(dbFile));
	}

	@Test
	void walPragmasAndCheckpoint() {
		File dbFile = tempDir.resolve("pragma.db").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			assertEquals("wal", repository.journalMode().toLowerCase());
			assertEquals(5000, repository.busyTimeoutMillis());
			repository.checkpointWal(false);
			repository.checkpointWal(true);
		} finally {
			repository.close();
		}
	}

	@Test
	void vacuumIntoPreservesLiveRow() {
		File dbFile = tempDir.resolve("live.db").toFile();
		File backups = tempDir.resolve("backups").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			assertTrue(repository.saveLive(snapshot("u1", "world", 1, 2, 1)));
			assertTrue(repository.vacuumIntoBackup(backups));
			List<File> snapshots = VehicleSqliteBackup.listSnapshots(backups);
			assertEquals(1, snapshots.size());
			VehicleRepository copy = VehicleRepository.open(snapshots.get(0));
			try {
				VehicleSnapshot row = copy.findLive("u1").orElseThrow();
				assertEquals("horse_cart", row.getTypeId());
				assertTrue(row.getPayloadJson().contains("horse_cart"));
			} finally {
				copy.close();
			}
		} finally {
			repository.close();
		}
	}

	@Test
	void vacuumIntoKeepsThreeNewest() throws Exception {
		File dbFile = tempDir.resolve("live.db").toFile();
		File backups = tempDir.resolve("backups").toFile();
		VehicleRepository repository = VehicleRepository.open(dbFile);
		try {
			assertTrue(repository.saveLive(snapshot("u1", "world", 1, 2, 1)));
			long time = System.currentTimeMillis();
			for (int i = 0; i < 4; i++) {
				assertTrue(repository.vacuumIntoBackup(backups));
				List<File> snapshots = VehicleSqliteBackup.listSnapshots(backups);
				snapshots.sort(java.util.Comparator.comparing(File::getName));
				for (int n = 0; n < snapshots.size(); n++) {
					snapshots.get(n).setLastModified(time + n * 1000L);
				}
			}
			assertEquals(VehicleSqliteBackup.KEEP, VehicleSqliteBackup.listSnapshots(backups).size());
		} finally {
			repository.close();
		}
	}

	@Test
	void openWithRestoreRecoversFromBackup() throws Exception {
		File live = tempDir.resolve("vehicles.db").toFile();
		File backups = tempDir.resolve("backups").toFile();
		VehicleRepository repository = VehicleRepository.open(live);
		try {
			assertTrue(repository.saveLive(snapshot("u1", "world", 1, 2, 1)));
			assertTrue(repository.vacuumIntoBackup(backups));
		} finally {
			repository.close();
		}
		Files.writeString(live.toPath(), "this is not a sqlite database");
		VehicleRepository restored = VehicleRepository.openWithRestore(live, backups);
		try {
			assertTrue(restored.findLive("u1").isPresent());
			assertTrue(restored.findLive("u1").orElseThrow().getPayloadJson().contains("horse_cart"));
		} finally {
			restored.close();
		}
	}

	@Test
	void openWithRestoreWithoutBackupStillFails() throws Exception {
		File live = tempDir.resolve("vehicles.db").toFile();
		Files.writeString(live.toPath(), "this is not a sqlite database");
		assertThrows(
				RuntimeException.class,
				() -> VehicleRepository.openWithRestore(live, tempDir.resolve("empty-backups").toFile()));
	}

	private static VehicleSnapshot snapshot(String uuid, String world, int chunkX, int chunkZ, int revision) {
		return new VehicleSnapshot(
				uuid,
				"horse_cart",
				world,
				1.5,
				64,
				2.5,
				12.5f,
				chunkX,
				chunkZ,
				PAYLOAD,
				VehicleRepository.SCHEMA_VERSION,
				revision,
				false,
				50L);
	}
}
