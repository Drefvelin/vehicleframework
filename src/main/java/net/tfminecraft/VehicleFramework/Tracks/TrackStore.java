package net.tfminecraft.VehicleFramework.Tracks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.tfminecraft.VehicleFramework.VFLogger;

public final class TrackStore {
	private final File root;

	public TrackStore(File dataFolder) {
		this.root = new File(dataFolder, "data/tracks");
	}

	public static String sanitizeWorld(String world) {
		if (world == null || world.isBlank()) {
			return "unknown";
		}
		return world.replace('\\', '_').replace('/', '_').replace(':', '_');
	}

	public File fileFor(TrackSpline spline) {
		return fileFor(spline.getWorld(), spline.getId());
	}

	public File fileFor(String world, UUID id) {
		return new File(new File(root, sanitizeWorld(world)), id.toString() + ".json");
	}

	public void save(TrackSpline spline) {
		File file = fileFor(spline);
		file.getParentFile().mkdirs();
		JSONObject json = spline.toJson();
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write(json.toJSONString());
		} catch (Exception e) {
			VFLogger.log("Failed to save track " + spline.getId());
			e.printStackTrace();
		}
	}

	public void delete(TrackSpline spline) {
		File file = fileFor(spline);
		if (file.exists() && !file.delete()) {
			VFLogger.log("Failed to delete track file " + file.getName());
		}
	}

	public File junctionFileFor(String world, UUID id) {
		return new File(new File(new File(root, sanitizeWorld(world)), "junctions"), id.toString() + ".json");
	}

	public void saveJunction(String world, TrackJunction junction) {
		File file = junctionFileFor(world, junction.id);
		file.getParentFile().mkdirs();
		JSONObject json = junction.toJson();
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write(json.toJSONString());
		} catch (Exception e) {
			VFLogger.log("Failed to save junction " + junction.id);
			e.printStackTrace();
		}
	}

	public void deleteJunction(String world, UUID id) {
		File file = junctionFileFor(world, id);
		if (file.exists() && !file.delete()) {
			VFLogger.log("Failed to delete junction file " + file.getName());
		}
	}

	public static final class LoadedJunction {
		public final String world;
		public final TrackJunction junction;

		public LoadedJunction(String world, TrackJunction junction) {
			this.world = world;
			this.junction = junction;
		}
	}

	public List<LoadedJunction> loadAllJunctions() {
		List<LoadedJunction> loaded = new ArrayList<>();
		if (!root.exists() || !root.isDirectory()) {
			return loaded;
		}
		File[] worlds = root.listFiles();
		if (worlds == null) {
			return loaded;
		}
		JSONParser parser = new JSONParser();
		for (File worldDir : worlds) {
			if (!worldDir.isDirectory()) {
				continue;
			}
			File junctionDir = new File(worldDir, "junctions");
			if (!junctionDir.isDirectory()) {
				continue;
			}
			File[] files = junctionDir.listFiles((dir, name) -> name.endsWith(".json"));
			if (files == null) {
				continue;
			}
			for (File file : files) {
				try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
					Object parsed = parser.parse(reader);
					if (parsed instanceof JSONObject json) {
						loaded.add(new LoadedJunction(worldDir.getName(), TrackJunction.fromJson(json)));
					}
				} catch (Exception e) {
					VFLogger.log("Failed to load junction " + file.getName());
					e.printStackTrace();
				}
			}
		}
		return loaded;
	}

	public List<TrackSpline> loadAll() {
		List<TrackSpline> loaded = new ArrayList<>();
		if (!root.exists() || !root.isDirectory()) {
			return loaded;
		}
		File[] worlds = root.listFiles();
		if (worlds == null) {
			return loaded;
		}
		JSONParser parser = new JSONParser();
		for (File worldDir : worlds) {
			if (!worldDir.isDirectory()) {
				continue;
			}
			File[] files = worldDir.listFiles((dir, name) -> name.endsWith(".json"));
			if (files == null) {
				continue;
			}
			for (File file : files) {
				try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
					Object parsed = parser.parse(reader);
					if (parsed instanceof JSONObject json) {
						loaded.add(TrackSpline.fromJson(json));
					}
				} catch (Exception e) {
					VFLogger.log("Failed to load track " + file.getName());
					e.printStackTrace();
				}
			}
		}
		return loaded;
	}
}
