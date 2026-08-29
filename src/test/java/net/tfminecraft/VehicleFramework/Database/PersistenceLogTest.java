package net.tfminecraft.VehicleFramework.Database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenceLogTest {

	@TempDir
	Path tempDir;

	@Test
	void append_writesToLogsFolder() throws Exception {
		PersistenceLog.configure(true, tempDir.toFile());
		PersistenceLog.append("reload-probe");

		String content = Files.readString(tempDir.resolve("logs").resolve("persistence.log"));
		assertTrue(content.contains("SESSION"));
		assertTrue(content.contains("reload-probe"));
	}

	@Test
	void append_noOpWhenDisabled() throws Exception {
		PersistenceLog.configure(false, tempDir.toFile());
		PersistenceLog.append("hello");

		assertEquals(false, Files.exists(tempDir.resolve("logs").resolve("persistence.log")));
	}

	@Test
	void configure_doesNotWipeExistingFile() throws Exception {
		Path logFile = tempDir.resolve("logs").resolve("persistence.log");
		Files.createDirectories(logFile.getParent());
		Files.writeString(logFile, "first-reload\n");
		PersistenceLog.configure(true, tempDir.toFile());

		String content = Files.readString(logFile);
		assertTrue(content.contains("first-reload"));
		assertTrue(content.contains("SESSION"));
	}
}
