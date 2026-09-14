package net.tfminecraft.VehicleFramework.Database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import me.Plugins.TLibs.database.SqliteDatabase;
import me.Plugins.TLibs.database.SqliteDatabaseException;
import net.tfminecraft.VehicleFramework.Data.StoredVehicleMeta;

public final class VehicleRepository {
	public static final int SCHEMA_VERSION = 2;

	private static final String CREATE_METADATA = """
			CREATE TABLE IF NOT EXISTS schema_metadata (
			    key TEXT PRIMARY KEY,
			    value TEXT NOT NULL
			)
			""";

	private static final String CREATE_VEHICLES = """
			CREATE TABLE IF NOT EXISTS vehicles (
			    uuid           TEXT PRIMARY KEY,
			    type_id        TEXT NOT NULL,
			    name           TEXT NOT NULL DEFAULT '',
			    owner          TEXT NOT NULL DEFAULT 'none',
			    world          TEXT NOT NULL,
			    x              REAL NOT NULL,
			    y              REAL NOT NULL,
			    z              REAL NOT NULL,
			    yaw            REAL NOT NULL,
			    chunk_x        INTEGER NOT NULL,
			    chunk_z        INTEGER NOT NULL,
			    payload_json   TEXT NOT NULL,
			    schema_version INTEGER NOT NULL,
			    revision       INTEGER NOT NULL,
			    deleted        INTEGER NOT NULL DEFAULT 0,
			    updated_at     INTEGER NOT NULL
			)
			""";

	private static final String CREATE_CHUNK_INDEX = """
			CREATE INDEX IF NOT EXISTS vehicles_by_chunk
			ON vehicles(world, chunk_x, chunk_z, deleted)
			""";

	private static final String CREATE_OWNER_INDEX = """
			CREATE INDEX IF NOT EXISTS vehicles_by_owner
			ON vehicles(owner, deleted)
			""";

	private static final String UPSERT = """
			INSERT INTO vehicles (
			    uuid, type_id, name, owner, world, x, y, z, yaw, chunk_x, chunk_z,
			    payload_json, schema_version, revision, deleted, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT(uuid) DO UPDATE SET
			    type_id = excluded.type_id,
			    name = excluded.name,
			    owner = excluded.owner,
			    world = excluded.world,
			    x = excluded.x,
			    y = excluded.y,
			    z = excluded.z,
			    yaw = excluded.yaw,
			    chunk_x = excluded.chunk_x,
			    chunk_z = excluded.chunk_z,
			    payload_json = excluded.payload_json,
			    schema_version = excluded.schema_version,
			    revision = excluded.revision,
			    deleted = excluded.deleted,
			    updated_at = excluded.updated_at
			WHERE excluded.revision >= vehicles.revision
			""";

	private static final String SAVE_LIVE = """
			INSERT INTO vehicles (
			    uuid, type_id, name, owner, world, x, y, z, yaw, chunk_x, chunk_z,
			    payload_json, schema_version, revision, deleted, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)
			ON CONFLICT(uuid) DO UPDATE SET
			    type_id = excluded.type_id,
			    name = excluded.name,
			    owner = excluded.owner,
			    world = excluded.world,
			    x = excluded.x,
			    y = excluded.y,
			    z = excluded.z,
			    yaw = excluded.yaw,
			    chunk_x = excluded.chunk_x,
			    chunk_z = excluded.chunk_z,
			    payload_json = excluded.payload_json,
			    schema_version = excluded.schema_version,
			    revision = vehicles.revision + 1,
			    deleted = 0,
			    updated_at = excluded.updated_at
			""";

	private static final String SELECT_COLUMNS = """
			SELECT uuid, type_id, name, owner, world, x, y, z, yaw, chunk_x, chunk_z,
			       payload_json, schema_version, revision, deleted, updated_at
			FROM vehicles
			""";

	private static final String SELECT_BY_UUID = SELECT_COLUMNS + " WHERE uuid = ?";

	private static final String SELECT_LIVE_BY_UUID = SELECT_BY_UUID + " AND deleted = 0";

	private static final String SELECT_CHUNK = SELECT_COLUMNS + """
			WHERE world = ? AND chunk_x = ? AND chunk_z = ? AND deleted = 0
			""";

	private static final String SELECT_NEAR = SELECT_COLUMNS + """
			WHERE world = ? AND deleted = 0
			  AND chunk_x BETWEEN ? AND ?
			  AND chunk_z BETWEEN ? AND ?
			""";

	private static final String SELECT_ALL_LIVE = SELECT_COLUMNS + " WHERE deleted = 0";

	private static final String SELECT_LIVE_BY_OWNER = SELECT_COLUMNS + """
			WHERE deleted = 0 AND owner = ? COLLATE NOCASE
			""";

	private static final String SELECT_LIVE_PLAYER_OWNED = SELECT_COLUMNS + """
			WHERE deleted = 0
			  AND owner LIKE 'player_%' COLLATE NOCASE
			  AND owner != 'player_none' COLLATE NOCASE
			  AND length(owner) > 7
			""";

	private static final String TOMBSTONE_REVISION = """
			UPDATE vehicles
			SET deleted = 1, revision = ?, updated_at = ?
			WHERE uuid = ? AND ? >= revision
			""";

	private static final String TOMBSTONE_LIVE = """
			UPDATE vehicles
			SET deleted = 1, revision = revision + 1, updated_at = ?
			WHERE uuid = ? AND deleted = 0
			""";

	private final SqliteDatabase database;

	private VehicleRepository(SqliteDatabase database) {
		this.database = database;
	}

	public static VehicleRepository open(File dbFile) {
		SqliteDatabase sqlite = new SqliteDatabase(dbFile);
		VehicleRepository repository = new VehicleRepository(sqlite);
		try {
			repository.initSchema();
		} catch (RuntimeException ex) {
			try {
				sqlite.close();
			} catch (Exception closeError) {
				ex.addSuppressed(closeError);
			}
			throw ex;
		}
		return repository;
	}

	public static VehicleRepository openWithRestore(File liveFile, File backupDir) {
		try {
			return open(liveFile);
		} catch (RuntimeException first) {
			File backup = VehicleSqliteBackup.findNewestValid(backupDir);
			if (backup == null) {
				throw first;
			}
			try {
				VehicleSqliteBackup.replaceLive(liveFile, backup);
			} catch (Exception replaceError) {
				first.addSuppressed(replaceError);
				throw first;
			}
			VehicleRepository restored = open(liveFile);
			try {
				net.tfminecraft.VehicleFramework.VFLogger.log(
						"Restored vehicles.db from backup " + backup.getName());
			} catch (Exception ignored) {
			}
			return restored;
		}
	}

	public boolean vacuumIntoBackup(File backupDir) {
		if (backupDir == null) {
			return false;
		}
		backupDir.mkdirs();
		File dest = VehicleSqliteBackup.nextBackupFile(backupDir);
		database.execute(VehicleSqliteBackup.vacuumIntoSql(dest));
		VehicleSqliteBackup.rotate(backupDir);
		return dest.isFile();
	}

	private void initSchema() {
		database.execute("PRAGMA journal_mode=WAL");
		database.execute("PRAGMA foreign_keys=ON");
		database.execute("PRAGMA busy_timeout=5000");
		database.execute(CREATE_METADATA);
		database.execute(CREATE_VEHICLES);
		addColumnIfMissing("name", "TEXT NOT NULL DEFAULT ''");
		addColumnIfMissing("owner", "TEXT NOT NULL DEFAULT 'none'");
		database.execute(CREATE_CHUNK_INDEX);
		database.execute(CREATE_OWNER_INDEX);
		database.executeUpdate(
				"INSERT OR IGNORE INTO schema_metadata (key, value) VALUES (?, ?)",
				"schema_version",
				String.valueOf(SCHEMA_VERSION));
		database.executeUpdate(
				"UPDATE schema_metadata SET value = ? WHERE key = ?",
				String.valueOf(SCHEMA_VERSION),
				"schema_version");
		assertQuickCheck();
	}

	private void assertQuickCheck() {
		List<String> results = queryList("PRAGMA quick_check", result -> result.getString(1));
		if (results.size() != 1 || !"ok".equalsIgnoreCase(results.get(0))) {
			throw new SqliteDatabaseException("SQLite integrity check failed: " + results);
		}
	}

	public void checkpointWal(boolean truncate) {
		String mode = truncate ? "TRUNCATE" : "PASSIVE";
		database.execute("PRAGMA wal_checkpoint(" + mode + ")");
	}

	public String journalMode() {
		return queryOne("PRAGMA journal_mode", result -> result.getString(1)).orElse("");
	}

	public int busyTimeoutMillis() {
		return queryOne("PRAGMA busy_timeout", result -> result.getInt(1)).orElse(0);
	}

	private void addColumnIfMissing(String column, String typeSql) {
		try {
			database.execute("ALTER TABLE vehicles ADD COLUMN " + column + " " + typeSql);
		} catch (Exception ignored) {
			// already present
		}
	}

	public void upsert(VehicleSnapshot snapshot) {
		if (snapshot == null || snapshot.getUuid() == null || snapshot.getUuid().isBlank()) {
			return;
		}
		database.executeUpdate(
				UPSERT,
				snapshot.getUuid(),
				snapshot.getTypeId(),
				snapshot.getName(),
				snapshot.getOwner(),
				snapshot.getWorld(),
				snapshot.getX(),
				snapshot.getY(),
				snapshot.getZ(),
				snapshot.getYaw(),
				snapshot.getChunkX(),
				snapshot.getChunkZ(),
				snapshot.getPayloadJson(),
				snapshot.getSchemaVersion(),
				snapshot.getRevision(),
				snapshot.isDeleted() ? 1 : 0,
				snapshot.getUpdatedAt());
	}

	public boolean saveLive(VehicleSnapshot snapshot) {
		if (snapshot == null || snapshot.getUuid() == null || snapshot.getUuid().isBlank()) {
			return false;
		}
		int updated = database.executeUpdate(
				SAVE_LIVE,
				snapshot.getUuid(),
				snapshot.getTypeId(),
				snapshot.getName(),
				snapshot.getOwner(),
				snapshot.getWorld(),
				snapshot.getX(),
				snapshot.getY(),
				snapshot.getZ(),
				snapshot.getYaw(),
				snapshot.getChunkX(),
				snapshot.getChunkZ(),
				snapshot.getPayloadJson(),
				snapshot.getSchemaVersion(),
				snapshot.getUpdatedAt());
		return updated > 0;
	}

	public Optional<VehicleSnapshot> find(String uuid) {
		if (uuid == null || uuid.isBlank()) {
			return Optional.empty();
		}
		return queryOne(SELECT_BY_UUID, VehicleRepository::mapSnapshot, uuid);
	}

	public Optional<VehicleSnapshot> findLive(String uuid) {
		if (uuid == null || uuid.isBlank()) {
			return Optional.empty();
		}
		return queryOne(SELECT_LIVE_BY_UUID, VehicleRepository::mapSnapshot, uuid);
	}

	public List<VehicleSnapshot> findChunk(String world, int chunkX, int chunkZ) {
		if (world == null || world.isBlank()) {
			return List.of();
		}
		return queryList(SELECT_CHUNK, VehicleRepository::mapSnapshot, world, chunkX, chunkZ);
	}

	public List<VehicleSnapshot> findNear(String world, int chunkX, int chunkZ, int radiusChunks) {
		if (world == null || world.isBlank()) {
			return List.of();
		}
		int radius = Math.max(0, radiusChunks);
		return queryList(
				SELECT_NEAR,
				VehicleRepository::mapSnapshot,
				world,
				chunkX - radius,
				chunkX + radius,
				chunkZ - radius,
				chunkZ + radius);
	}

	public int tombstone(String uuid, int revision, long updatedAt) {
		if (uuid == null || uuid.isBlank()) {
			return 0;
		}
		return database.executeUpdate(TOMBSTONE_REVISION, revision, updatedAt, uuid, revision);
	}

	public int tombstone(String uuid) {
		if (uuid == null || uuid.isBlank()) {
			return 0;
		}
		return database.executeUpdate(TOMBSTONE_LIVE, System.currentTimeMillis(), uuid);
	}

	public List<VehicleSnapshot> listAllLive() {
		return queryList(SELECT_ALL_LIVE, VehicleRepository::mapSnapshot);
	}

	public List<VehicleSnapshot> listLiveByOwner(String owner) {
		if (owner == null || owner.isBlank()) {
			return List.of();
		}
		return queryList(SELECT_LIVE_BY_OWNER, VehicleRepository::mapSnapshot, owner);
	}

	public List<VehicleSnapshot> listLivePlayerOwned() {
		return queryList(SELECT_LIVE_PLAYER_OWNED, VehicleRepository::mapSnapshot);
	}

	public Map<String, Integer> countLiveByOwner(String owner, java.util.Collection<String> skipUuids) {
		Map<String, Integer> counts = new HashMap<>();
		if (owner == null || owner.isBlank()) {
			return counts;
		}
		java.util.Set<String> skip = new java.util.HashSet<>();
		if (skipUuids != null) {
			for (String uuid : skipUuids) {
				if (uuid != null && !uuid.isBlank()) {
					skip.add(uuid.toLowerCase(Locale.ROOT));
				}
			}
		}
		for (VehicleSnapshot snapshot : listLiveByOwner(owner)) {
			if (skip.contains(snapshot.getUuid().toLowerCase(Locale.ROOT))) {
				continue;
			}
			String typeId = snapshot.getTypeId();
			if (typeId == null || typeId.isBlank()) {
				continue;
			}
			counts.put(typeId, counts.getOrDefault(typeId, 0) + 1);
		}
		return counts;
	}

	public List<StoredVehicleMeta> listMetaByOwner(String owner) {
		List<StoredVehicleMeta> meta = new ArrayList<>();
		for (VehicleSnapshot snapshot : listLiveByOwner(owner)) {
			meta.add(toMeta(snapshot));
		}
		return meta;
	}

	public List<StoredVehicleMeta> listPlayerOwnedMeta() {
		List<StoredVehicleMeta> meta = new ArrayList<>();
		for (VehicleSnapshot snapshot : listLivePlayerOwned()) {
			meta.add(toMeta(snapshot));
		}
		return meta;
	}

	public Optional<StoredVehicleMeta> readMeta(String uuid) {
		return find(uuid).map(VehicleRepository::toMeta);
	}

	public void close() {
		database.close();
	}

	public void runInTransaction(Runnable work) {
		if (work == null) {
			return;
		}
		database.runTransaction(connection -> work.run());
	}

	private static StoredVehicleMeta toMeta(VehicleSnapshot snapshot) {
		return new StoredVehicleMeta(
				snapshot.getUuid(),
				snapshot.getName(),
				snapshot.getTypeId(),
				snapshot.getOwner());
	}

	private static VehicleSnapshot mapSnapshot(ResultSet result) throws SQLException {
		return new VehicleSnapshot(
				result.getString("uuid"),
				result.getString("type_id"),
				nullable(result.getString("name")),
				nullable(result.getString("owner")),
				result.getString("world"),
				result.getDouble("x"),
				result.getDouble("y"),
				result.getDouble("z"),
				result.getFloat("yaw"),
				result.getInt("chunk_x"),
				result.getInt("chunk_z"),
				result.getString("payload_json"),
				result.getInt("schema_version"),
				result.getInt("revision"),
				result.getInt("deleted") != 0,
				result.getLong("updated_at"));
	}

	private static String nullable(String value) {
		return value == null ? "" : value;
	}

	private <T> Optional<T> queryOne(String sql, ResultSetFunction<T> mapper, Object... params) {
		List<T> rows = queryList(sql, mapper, params);
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(rows.get(0));
	}

	private <T> List<T> queryList(String sql, ResultSetFunction<T> mapper, Object... params) {
		List<T> rows = new ArrayList<>();
		synchronized (database) {
			try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
				bindParams(statement, params);
				try (ResultSet result = statement.executeQuery()) {
					while (result.next()) {
						rows.add(mapper.apply(result));
					}
				}
			} catch (SQLException e) {
				throw new SqliteDatabaseException("Failed to query: " + sql, e);
			}
		}
		return rows;
	}

	private static void bindParams(PreparedStatement statement, Object... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			statement.setObject(i + 1, params[i]);
		}
	}

	@FunctionalInterface
	private interface ResultSetFunction<T> {
		T apply(ResultSet result) throws SQLException;
	}
}
