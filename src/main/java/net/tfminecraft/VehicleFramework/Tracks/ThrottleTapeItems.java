package net.tfminecraft.VehicleFramework.Tracks;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.tfminecraft.VehicleFramework.VehicleFramework;

public final class ThrottleTapeItems {
	private static final String KEY = "throttle_tape";

	private ThrottleTapeItems() {
	}

	private static NamespacedKey key() {
		return new NamespacedKey(VehicleFramework.plugin, KEY);
	}

	public static ThrottleTape read(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return null;
		}
		String raw = item.getItemMeta().getPersistentDataContainer().get(key(), PersistentDataType.STRING);
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			Object parsed = new JSONParser().parse(raw);
			if (!(parsed instanceof JSONObject json)) {
				return null;
			}
			return ThrottleTape.fromJson(json);
		} catch (Exception e) {
			return null;
		}
	}

	public static void write(ItemStack item, ThrottleTape tape) {
		if (item == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		if (tape == null || tape.isEmpty()) {
			meta.getPersistentDataContainer().remove(key());
		} else {
			meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, tape.toJson().toJSONString());
		}
		item.setItemMeta(meta);
	}
}
