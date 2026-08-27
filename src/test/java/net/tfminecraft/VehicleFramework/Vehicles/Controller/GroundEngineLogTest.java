package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GroundEngineLogTest {

	@TempDir
	Path tempDir;

	@Test
	void append_writesImmediatelyWhenEnabled() throws Exception {
		GroundEngineLog.configure(true, false, tempDir.toFile());
		GroundEngineLog.append("one-off");

		String content = Files.readString(tempDir.resolve("logs").resolve("ground_engine.log"));
		assertTrue(content.contains("one-off"));
	}

	@Test
	void append_noOpWhenDisabled() throws Exception {
		GroundEngineLog.configure(false, false, tempDir.toFile());
		GroundEngineLog.append("hello");

		assertEquals(false, Files.exists(tempDir.resolve("logs").resolve("ground_engine.log")));
	}

	@Test
	void configure_wipeLog_deletesExistingFile() throws Exception {
		Path logFile = tempDir.resolve("logs").resolve("ground_engine.log");
		Files.createDirectories(logFile.getParent());
		Files.writeString(logFile, "old content");
		GroundEngineLog.configure(false, true, tempDir.toFile());

		assertEquals(false, Files.exists(logFile));
	}

	@Test
	void formatThrottle_includesCurrentMinMax() {
		net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle throttle =
				new net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle(
						"Throttle", 100, 0, null);
		throttle.setThrottle(42);
		assertEquals("thr=42/0/100", GroundEngineLog.formatThrottle(throttle));
	}

	@Test
	void formatStateSwap_includesReasonAndDefault() {
		assertEquals(
				"id=small_car state=GROUND->FLOATING reason=water isDefault=true",
				GroundEngineLog.formatStateSwap("small_car", "GROUND", "FLOATING", "water", true));
	}
}
