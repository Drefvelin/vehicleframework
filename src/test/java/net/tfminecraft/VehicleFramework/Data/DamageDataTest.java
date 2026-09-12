package net.tfminecraft.VehicleFramework.Data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Loaders.ArmorTemplateLoader;

class DamageDataTest {

	@Test
	void listConstructor_parsesCauseAndMultiplier() {
		DamageData data = new DamageData(List.of("torpedo(7.0)", "bullet(0.2)"));
		assertEquals(7.0, data.getModifier("TORPEDO"));
		assertEquals(0.2, data.getModifier("BULLET"));
	}

	@Test
	void mapConstructor_uppercasesKeys() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("torpedo", 7);
		map.put("bullet", 0.2);
		DamageData data = new DamageData(map);
		assertTrue(data.hasModifier("TORPEDO"));
		assertEquals(7.0, data.getModifier("TORPEDO"));
		assertEquals(0.2, data.getModifier("BULLET"));
	}

	@Test
	void missingCause_hasNoModifier() {
		DamageData data = new DamageData(Map.of("torpedo", 7.0));
		assertFalse(data.hasModifier("FLAK_BULLET"));
	}

	@Test
	void mergeLayers_roleOverwritesBullet() {
		Map<String, Object> armor = map("torpedo", 7.0, "bullet", 0.9);
		Map<String, Object> role = map("bullet", 0.2);
		Map<String, Object> merged = ArmorTemplateLoader.mergeLayers(armor, role, null);
		assertEquals(7.0, ((Number) merged.get("torpedo")).doubleValue());
		assertEquals(0.2, ((Number) merged.get("bullet")).doubleValue());
	}

	@Test
	void mergeLayers_overlayOverwritesTorpedo() {
		Map<String, Object> armor = map("torpedo", 7.0, "cannonball", 1.8);
		Map<String, Object> overlay = map("torpedo", 9.0);
		Map<String, Object> merged = ArmorTemplateLoader.mergeLayers(armor, null, overlay);
		assertEquals(9.0, ((Number) merged.get("torpedo")).doubleValue());
		assertEquals(1.8, ((Number) merged.get("cannonball")).doubleValue());
	}

	@Test
	void mergeLayers_omitRoleKeepsArmor() {
		Map<String, Object> armor = map("torpedo", 5.0, "entity_explosion", 1.5);
		Map<String, Object> merged = ArmorTemplateLoader.mergeLayers(armor, null, null);
		assertEquals(5.0, ((Number) merged.get("torpedo")).doubleValue());
		assertEquals(1.5, ((Number) merged.get("entity_explosion")).doubleValue());
	}

	@Test
	void listToMap_convertsLegacyDamageList() {
		Map<String, Object> map = ArmorTemplateLoader.listToMap(List.of("FALL(0.0)", "torpedo(5.0)"));
		assertEquals(0.0, ((Number) map.get("FALL")).doubleValue());
		assertEquals(5.0, ((Number) map.get("torpedo")).doubleValue());
	}

	private static Map<String, Object> map(Object... keysAndValues) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			result.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return result;
	}
}
