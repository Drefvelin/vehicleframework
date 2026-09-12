package net.tfminecraft.VehicleFramework.Cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CacheTrackDisplayStyleTest {

	@AfterEach
	void reset() {
		Cache.trackItemSmall = null;
		Cache.trackItemMedium = null;
		Cache.trackItemLarge = null;
		Cache.trackDisplayYOffset = 0.5;
		Cache.appliedTrackItemSmall = null;
		Cache.appliedTrackItemMedium = null;
		Cache.appliedTrackItemLarge = null;
		Cache.appliedTrackDisplayYOffset = 0.5;
	}

	@Test
	void applyTrackDisplayStyle_copiesLiveAndIgnoresLaterLiveEdits() {
		Cache.trackItemSmall = "ia.tfmc:track_small";
		Cache.trackItemMedium = "ia.tfmc:track_medium";
		Cache.trackItemLarge = "ia.tfmc:track_large";
		Cache.trackDisplayYOffset = 0.51;
		Cache.applyTrackDisplayStyle();

		Cache.trackItemSmall = "ia.tfmc:track_small_v2";
		Cache.trackItemMedium = "ia.tfmc:track_medium_v2";
		Cache.trackItemLarge = "ia.tfmc:track_large_v2";
		Cache.trackDisplayYOffset = 1.25;

		assertEquals("ia.tfmc:track_small", Cache.appliedTrackItemSmall);
		assertEquals("ia.tfmc:track_medium", Cache.appliedTrackItemMedium);
		assertEquals("ia.tfmc:track_large", Cache.appliedTrackItemLarge);
		assertEquals(0.51, Cache.appliedTrackDisplayYOffset);
	}
}
