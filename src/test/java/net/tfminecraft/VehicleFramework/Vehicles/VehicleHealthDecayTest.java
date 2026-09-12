package net.tfminecraft.VehicleFramework.Vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class VehicleHealthDecayTest {
	private static final String VEHICLE_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	private Path vehiclesDir;

	@BeforeEach
	void setUp() throws IOException {
		vehiclesDir = Path.of("plugins", "VehicleFramework", "data", "vehicles");
		Files.createDirectories(vehiclesDir);
	}

	@AfterEach
	void tearDown() throws IOException {
		Path pluginsDir = Path.of("plugins");
		if (Files.exists(pluginsDir)) {
			Files.walk(pluginsDir)
					.sorted(Comparator.reverseOrder())
					.forEach(
							path -> {
								try {
									Files.deleteIfExists(path);
								} catch (IOException ignored) {
								}
							});
		}
	}

	@Test
	void nextDamage_addsTwentyPercentOfMax() {
		assertEquals(20.0, VehicleHealthDecay.nextDamage(0.0, 100.0, 0.20, 0.03));
	}

	@Test
	void nextDamage_clampsToMinHealthFloor() {
		assertEquals(97.0, VehicleHealthDecay.nextDamage(90.0, 100.0, 0.20, 0.03));
	}

	@Test
	void nextDamage_alreadyAtFloorStays() {
		assertEquals(97.0, VehicleHealthDecay.nextDamage(97.0, 100.0, 0.20, 0.03));
	}

	@Test
	void applyToJson_updatesDamageWithoutTouchingFire() throws IOException {
		JsonObject root = JsonParser.parseString("""
				{
				  "id": "cloudskimmer",
				  "components": {
				    "hull": { "damage": 0.0, "fire": 12.0, "sinkprogress": 5.0 }
				  },
				  "weapons": {
				    "cannon": { "damage": 0.0 }
				  }
				}
				""").getAsJsonObject();

		VehicleHealthDecay.MaxHealthLookup lookup = new VehicleHealthDecay.MaxHealthLookup() {
			@Override
			public double componentMaxHealth(String componentTypeKey) {
				return "hull".equalsIgnoreCase(componentTypeKey) ? 100.0 : 0.0;
			}

			@Override
			public double weaponMaxHealth(String weaponId) {
				return "cannon".equalsIgnoreCase(weaponId) ? 50.0 : 0.0;
			}
		};

		assertTrue(VehicleHealthDecay.applyToJson(root, lookup, 0.20, 0.03));

		JsonObject hull = root.getAsJsonObject("components").getAsJsonObject("hull");
		assertEquals(20.0, hull.get("damage").getAsDouble());
		assertEquals(12.0, hull.get("fire").getAsDouble());
		assertEquals(5.0, hull.get("sinkprogress").getAsDouble());
		assertEquals(10.0, root.getAsJsonObject("weapons").getAsJsonObject("cannon").get("damage").getAsDouble());
	}

	@Test
	void applyToStoredFile_returnsFalseWhenMissing() {
		File missing = VehicleHealthDecay.storedVehicleFile("does-not-exist");
		assertFalse(VehicleHealthDecay.applyToStoredFile(missing, 0.20, 0.03));
	}

	@Test
	void applyToJson_writesFixtureFileDamageFields() throws IOException {
		File file = vehiclesDir.resolve(VEHICLE_UUID + ".json").toFile();
		try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
			writer.print("""
					{
					  "id": "cloudskimmer",
					  "components": {
					    "hull": { "damage": 0.0, "fire": 12.0 }
					  }
					}
					""");
		}

		JsonObject root = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
		VehicleHealthDecay.applyToJson(root, new VehicleHealthDecay.MaxHealthLookup() {
			@Override
			public double componentMaxHealth(String componentTypeKey) {
				return 100.0;
			}

			@Override
			public double weaponMaxHealth(String weaponId) {
				return 0.0;
			}
		}, 0.20, 0.03);
		Files.writeString(file.toPath(), root.toString());

		String json = Files.readString(file.toPath());
		assertTrue(json.contains("\"damage\":20.0") || json.contains("\"damage\":20"));
		assertTrue(json.contains("\"fire\":12.0") || json.contains("\"fire\":12"));
	}
}
