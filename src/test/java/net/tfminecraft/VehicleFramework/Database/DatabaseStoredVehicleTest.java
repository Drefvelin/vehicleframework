package net.tfminecraft.VehicleFramework.Database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Data.StoredVehicleMeta;

class DatabaseStoredVehicleTest {
    private static final String VEHICLE_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private Path vehiclesDir;
    private Database database;

    @BeforeEach
    void setUp() throws IOException {
        database = new Database();
        vehiclesDir = Path.of("plugins", "VehicleFramework", "data", "vehicles");
        Files.createDirectories(vehiclesDir);
        writeVehicle(
                VEHICLE_UUID,
                "{\"id\":\"cloudskimmer\",\"name\":\"Test Plane\",\"owner\":\"player_Alice\",\"whitelisted\":true,\"whitelist\":[\"Bob\"]}");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(vehiclesDir)) {
            Files.walk(vehiclesDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
        }
        Path pluginsDir = Path.of("plugins");
        if (Files.exists(pluginsDir)) {
            try {
                Files.walk(pluginsDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(
                                path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                    } catch (IOException ignored) {
                                    }
                                });
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void readStoredVehicle_returnsMeta() {
        Optional<StoredVehicleMeta> meta = database.readStoredVehicle(VEHICLE_UUID);
        assertTrue(meta.isPresent());
        assertEquals(VEHICLE_UUID, meta.get().getUuid());
        assertEquals("Test Plane", meta.get().getName());
        assertEquals("cloudskimmer", meta.get().getTypeId());
        assertEquals("player_Alice", meta.get().getOwner());
    }

    @Test
    void clearStoredOwnership_resetsOwnerAndWhitelist() throws IOException {
        assertTrue(database.clearStoredOwnership(VEHICLE_UUID));

        String json = Files.readString(vehiclesDir.resolve(VEHICLE_UUID + ".json"));
        assertTrue(json.contains("\"owner\":\"none\""));
        assertTrue(json.contains("\"whitelisted\":false"));
        assertTrue(json.contains("\"whitelist\":[]"));
    }

    @Test
    void listStoredVehiclesByOwner_filtersByOwner() {
        writeVehicle(
                "bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee",
                "{\"id\":\"horse_cart\",\"name\":\"Cart\",\"owner\":\"player_Bob\",\"whitelisted\":false,\"whitelist\":[]}");

        assertEquals(1, database.listStoredVehiclesByOwner("player_Alice").size());
        assertEquals(1, database.listStoredVehiclesByOwner("player_Bob").size());
        assertTrue(database.listStoredVehiclesByOwner("player_None").isEmpty());
    }

    @Test
    void listStoredPlayerOwnedVehicles_skipsNone() {
        writeVehicle(
                "bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee",
                "{\"id\":\"horse_cart\",\"name\":\"Cart\",\"owner\":\"none\",\"whitelisted\":false,\"whitelist\":[]}");
        writeVehicle(
                "cccccccc-bbbb-cccc-dddd-eeeeeeeeeeee",
                "{\"id\":\"ironclad\",\"name\":\"Ship\",\"owner\":\"player_Bob\",\"whitelisted\":false,\"whitelist\":[]}");

        List<StoredVehicleMeta> owned = database.listStoredPlayerOwnedVehicles();
        assertEquals(2, owned.size());
        assertTrue(owned.stream().anyMatch(meta -> meta.getOwner().equals("player_Alice")));
        assertTrue(owned.stream().anyMatch(meta -> meta.getOwner().equals("player_Bob")));
        assertTrue(owned.stream().noneMatch(meta -> "none".equalsIgnoreCase(meta.getOwner())));
    }

    private static void writeVehicle(String uuid, String json) {
        try {
            File file = Path.of("plugins", "VehicleFramework", "data", "vehicles", uuid + ".json")
                    .toFile();
            file.getParentFile().mkdirs();
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.print(json);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
