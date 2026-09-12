package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrackSupportTest {

	@Test
	void sitTop_ignoreThin() {
		assertNull(TrackSupport.sitTop(64, 0));
		assertNull(TrackSupport.sitTop(64, 0.04));
	}

	@Test
	void sitTop_bottomSlabHalf() {
		assertEquals(64.5, TrackSupport.sitTop(64, 0.5), 1e-9);
	}

	@Test
	void sitTop_fullBlock() {
		assertEquals(65.0, TrackSupport.sitTop(64, 1.0), 1e-9);
	}

	@Test
	void sitTop_topSlabFull() {
		assertEquals(65.0, TrackSupport.sitTop(64, 1.0), 1e-9);
	}

	@Test
	void snapY_insideFullBlock_liftsToSit() {
		double y = TrackSupport.firstSitY(161.176, by -> by == 161 ? 162.0 : null);
		assertEquals(162.0, y, 1e-9);
	}

	@Test
	void snapY_onGrassAboveDirt_dropsToDirt() {
		double y = TrackSupport.firstSitY(163.0, by -> by == 161 ? 162.0 : null);
		assertEquals(162.0, y, 1e-9);
	}

	@Test
	void snapY_bridge_keepsNoSit() {
		assertNull(TrackSupport.firstSitY(70.0, by -> null));
	}

	@Test
	void isPlant_tallGrassNotGrassBlock() {
		assertTrue(TrackSupport.isPlantName("TALL_GRASS"));
		assertTrue(TrackSupport.isPlantName("SHORT_GRASS"));
		assertTrue(TrackSupport.isPlantName("GRASS"));
		assertFalse(TrackSupport.isPlantName("GRASS_BLOCK"));
	}
}
