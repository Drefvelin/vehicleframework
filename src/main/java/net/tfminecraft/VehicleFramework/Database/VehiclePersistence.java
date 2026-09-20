package net.tfminecraft.VehicleFramework.Database;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Data.StoredVehicleMeta;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;
import net.tfminecraft.VehicleFramework.Vehicles.VehicleHealthDecay;

public final class VehiclePersistence {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final VehicleRepository repository;

	public VehiclePersistence(VehicleRepository repository) {
		this.repository = repository;
	}

	public static VehiclePersistence current() {
		VehicleRepository repository = VehicleFramework.getVehicleRepository();
		if (repository == null) {
			return null;
		}
		return new VehiclePersistence(repository);
	}

	public VehicleRepository repository() {
		return repository;
	}

	public boolean saveLive(ActiveVehicle vehicle) {
		return saveLiveResult(vehicle).isSaved();
	}

	public VehiclePersistResult saveLiveResult(ActiveVehicle vehicle) {
		if (repository == null) {
			return VehiclePersistResult.failed("SQLite is not open");
		}
		try {
			ActiveVehicleSnapshotFactory.SnapshotAttempt attempt = ActiveVehicleSnapshotFactory.tryFromLive(vehicle);
			if (attempt.snapshot().isEmpty()) {
				return resolveFailedLiveSave(uuidOf(vehicle), attempt.failureReason());
			}
			if (repository.saveLive(attempt.snapshot().get())) {
				return VehiclePersistResult.saved();
			}
			return resolveFailedLiveSave(uuidOf(vehicle), "SQL wrote 0 rows");
		} catch (Exception ex) {
			String message = ex.getMessage();
			return resolveFailedLiveSave(
					uuidOf(vehicle),
					message == null || message.isBlank() ? "SQLite save failed" : "SQLite save failed: " + message);
		}
	}

	public VehiclePersistResult resolveFailedLiveSave(String uuid, String reason) {
		String why = reason == null || reason.isBlank() ? "unknown" : reason;
		if (uuid != null && !uuid.isBlank() && repository != null) {
			try {
				if (repository.findLive(uuid).isPresent()) {
					return VehiclePersistResult.alreadyStored(why);
				}
			} catch (Exception ignored) {
				// fall through to failed
			}
		}
		return VehiclePersistResult.failed(why + ", no SQLite row");
	}

	private static String uuidOf(ActiveVehicle vehicle) {
		return vehicle == null ? null : vehicle.getUUID();
	}

	public boolean saveLive(VehicleSnapshot snapshot) {
		try {
			if (repository == null) {
				return false;
			}
			return repository.saveLive(snapshot);
		} catch (Exception ex) {
			log("SQLite save failed: " + ex.getMessage());
			return false;
		}
	}

	public Optional<IncompleteVehicle> loadIncomplete(String uuid) {
		try {
			Optional<VehicleSnapshot> snapshot = repository.findLive(uuid);
			if (snapshot.isEmpty()) {
				return Optional.empty();
			}
			return VehiclePayloadCodec.decode(snapshot.get().getPayloadJson(), snapshot.get().getUuid());
		} catch (Exception ex) {
			log("SQLite load failed for " + uuid + ": " + ex.getMessage());
			return Optional.empty();
		}
	}

	public boolean tombstone(String uuid) {
		try {
			if (repository == null || uuid == null || uuid.isBlank()) {
				return false;
			}
			if (repository.tombstone(uuid) > 0) {
				return true;
			}
			Optional<VehicleSnapshot> existing = repository.find(uuid);
			return existing.isPresent() && existing.get().isDeleted();
		} catch (Exception ex) {
			log("SQLite tombstone failed for " + uuid + ": " + ex.getMessage());
			return false;
		}
	}

	public boolean checkpointWal(boolean truncate) {
		try {
			if (repository == null) {
				return false;
			}
			repository.checkpointWal(truncate);
			return true;
		} catch (Exception ex) {
			log("SQLite WAL checkpoint failed: " + ex.getMessage());
			return false;
		}
	}

	public boolean vacuumIntoBackup() {
		try {
			if (repository == null) {
				return false;
			}
			File dataFolder = VehicleFramework.plugin == null ? null : VehicleFramework.plugin.getDataFolder();
			if (dataFolder == null) {
				return false;
			}
			return repository.vacuumIntoBackup(VehicleSqliteBackup.backupDir(dataFolder));
		} catch (Exception ex) {
			log("SQLite VACUUM INTO backup failed: " + ex.getMessage());
			return false;
		}
	}

	public List<VehicleSnapshot> findChunk(String world, int chunkX, int chunkZ) {
		try {
			return repository.findChunk(world, chunkX, chunkZ);
		} catch (Exception ex) {
			log("SQLite chunk query failed: " + ex.getMessage());
			return List.of();
		}
	}

	public boolean hasLiveInChunk(String world, int chunkX, int chunkZ) {
		try {
			return repository.hasLiveInChunk(world, chunkX, chunkZ);
		} catch (Exception ex) {
			log("SQLite chunk occupancy check failed: " + ex.getMessage());
			return false;
		}
	}

	public Optional<VehicleSnapshot> findLive(String uuid) {
		try {
			return repository.findLive(uuid);
		} catch (Exception ex) {
			log("SQLite find failed for " + uuid + ": " + ex.getMessage());
			return Optional.empty();
		}
	}

	public Map<String, Integer> countByOwner(String owner, java.util.Collection<String> skipUuids) {
		try {
			return repository.countLiveByOwner(owner, skipUuids);
		} catch (Exception ex) {
			log("SQLite owner count failed: " + ex.getMessage());
			return Map.of();
		}
	}

	public List<StoredVehicleMeta> listByOwner(String owner) {
		try {
			return repository.listMetaByOwner(owner);
		} catch (Exception ex) {
			log("SQLite owner list failed: " + ex.getMessage());
			return List.of();
		}
	}

	public List<StoredVehicleMeta> listPlayerOwned() {
		try {
			return repository.listPlayerOwnedMeta();
		} catch (Exception ex) {
			log("SQLite player-owned list failed: " + ex.getMessage());
			return List.of();
		}
	}

	public Optional<StoredVehicleMeta> readMeta(String uuid) {
		try {
			return repository.readMeta(uuid);
		} catch (Exception ex) {
			log("SQLite meta read failed: " + ex.getMessage());
			return Optional.empty();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean clearOwnership(String uuid) {
		try {
			boolean[] saved = { false };
			repository.runInTransaction(() -> {
				Optional<VehicleSnapshot> existing = repository.findLive(uuid);
				if (existing.isEmpty()) {
					return;
				}
				VehicleSnapshot row = existing.get();
				JSONParser parser = new JSONParser();
				JSONObject json;
				try {
					json = (JSONObject) parser.parse(row.getPayloadJson());
				} catch (Exception parseError) {
					throw new RuntimeException(parseError);
				}
				json.put("owner", "none");
				json.put("whitelisted", false);
				json.put("whitelist", new JSONArray());
				VehicleSnapshot next = row.withPayload(
						json.toJSONString(),
						row.getName(),
						"none",
						System.currentTimeMillis());
				saved[0] = repository.saveLive(next);
			});
			return saved[0];
		} catch (Exception ex) {
			log("SQLite clear ownership failed for " + uuid + ": " + ex.getMessage());
			return false;
		}
	}

	public boolean applyStoredDecay(String uuid, double fractionOfMax, double minHealthFraction) {
		try {
			boolean[] saved = { false };
			repository.runInTransaction(() -> {
				Optional<VehicleSnapshot> existing = repository.findLive(uuid);
				if (existing.isEmpty()) {
					return;
				}
				VehicleSnapshot row = existing.get();
				JsonObject root = JsonParser.parseString(row.getPayloadJson()).getAsJsonObject();
				String typeId = row.getTypeId();
				Vehicle template = VehicleLoader.getByString(typeId);
				if (template == null) {
					return;
				}
				boolean changed = VehicleHealthDecay.applyToJson(
						root,
						VehicleHealthDecay.lookupFromTemplate(template),
						fractionOfMax,
						minHealthFraction);
				if (!changed) {
					saved[0] = true;
					return;
				}
				VehicleSnapshot next = row.withPayload(
						GSON.toJson(root),
						row.getName(),
						row.getOwner(),
						System.currentTimeMillis());
				saved[0] = repository.saveLive(next);
			});
			return saved[0];
		} catch (Exception ex) {
			log("SQLite decay failed for " + uuid + ": " + ex.getMessage());
			return false;
		}
	}

	private static void log(String message) {
		try {
			VFLogger.log(message);
		} catch (Exception ignored) {
			System.out.println("[VehicleFramework] Error! " + message);
		}
	}
}
