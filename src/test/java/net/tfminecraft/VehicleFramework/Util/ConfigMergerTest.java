package net.tfminecraft.VehicleFramework.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ConfigMergerTest {

	@Test
	void nestedData_keepsTemplateReloadSoundsWhenOverlaySetsShootSounds() {
		Map<String, Object> template = map(
				"data", map(
						"reload-sounds", map("sound", "reload"),
						"shoot-sounds", map("sound", "template_shoot")));
		Map<String, Object> overlay = map(
				"data", map("shoot-sounds", map("sound", "vehicle_shoot")));

		Map<String, Object> merged = ConfigMerger.overlay(template, overlay);

		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) merged.get("data");
		@SuppressWarnings("unchecked")
		Map<String, Object> reload = (Map<String, Object>) data.get("reload-sounds");
		@SuppressWarnings("unchecked")
		Map<String, Object> shoot = (Map<String, Object>) data.get("shoot-sounds");
		assertEquals("reload", reload.get("sound"));
		assertEquals("vehicle_shoot", shoot.get("sound"));
	}

	@Test
	void bonesList_replacesTemplateBones() {
		Map<String, Object> template = map("bones", List.of("exit.align"));
		Map<String, Object> overlay = map("bones", List.of("exit2.exitalign2"));

		Map<String, Object> merged = ConfigMerger.overlay(template, overlay);

		assertEquals(List.of("exit2.exitalign2"), merged.get("bones"));
	}

	@Test
	void cooldown_overridesTemplate() {
		Map<String, Object> template = map("cooldown", 10);
		Map<String, Object> overlay = map("cooldown", 4);

		Map<String, Object> merged = ConfigMerger.overlay(template, overlay);

		assertEquals(4, merged.get("cooldown"));
	}

	@Test
	void missingOverlayKey_keepsTemplateProjectileDamage() {
		Map<String, Object> template = map("projectile-damage", 12, "cooldown", 10);
		Map<String, Object> overlay = map("seat", "gunner");

		Map<String, Object> merged = ConfigMerger.overlay(template, overlay);

		assertEquals(12, merged.get("projectile-damage"));
		assertEquals(10, merged.get("cooldown"));
		assertEquals("gunner", merged.get("seat"));
	}

	@Test
	void templateKey_isNotCopiedIntoMergedMap() {
		Map<String, Object> template = map("cooldown", 4);
		Map<String, Object> overlay = map("template", "gun_turret", "seat", "gunner");

		Map<String, Object> merged = ConfigMerger.overlay(template, overlay);

		assertFalse(merged.containsKey("template"));
		assertTrue(merged.containsKey("cooldown"));
		assertEquals("gunner", merged.get("seat"));
	}

	@Test
	void overlay_doesNotMutateTemplateMap() {
		Map<String, Object> template = map("cooldown", 10);
		Map<String, Object> overlay = map("cooldown", 4);

		ConfigMerger.overlay(template, overlay);

		assertEquals(10, template.get("cooldown"));
	}

	private static Map<String, Object> map(Object... keysAndValues) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			result.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return result;
	}
}
