package net.tfminecraft.VehicleFramework.Vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class VehicleHealthDecayTest {
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
	void applyToJson_updatesDamageWithoutTouchingFire() {
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
}
