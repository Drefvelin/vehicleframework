package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.VehicleFramework.VehicleFramework;

public final class VehicleTicketItems {
	private static final String KEY = "ticket_id";

	private VehicleTicketItems() {
	}

	public static NamespacedKey key() {
		return new NamespacedKey(VehicleFramework.plugin, KEY);
	}

	public static String readId(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return null;
		}
		String id = item.getItemMeta().getPersistentDataContainer().get(key(), PersistentDataType.STRING);
		if (id == null || id.isBlank()) {
			return null;
		}
		return id;
	}

	public static void write(ItemStack item, String ticketId, String vehicleName) {
		if (item == null || ticketId == null || ticketId.isBlank()) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, ticketId);
		String name = vehicleName == null || vehicleName.isBlank() ? "vehicle" : vehicleName;
		meta.setDisplayName("§eTicket: " + name);
		List<String> lore = new ArrayList<>();
		lore.add("§7Valid for this vehicle");
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	public static boolean inventoryHas(Player player, String ticketId) {
		if (player == null || ticketId == null || ticketId.isBlank()) {
			return false;
		}
		PlayerInventory inv = player.getInventory();
		if (matches(inv.getItemInOffHand(), ticketId)) {
			return true;
		}
		ItemStack[] storage = inv.getStorageContents();
		if (storage == null) {
			return false;
		}
		for (ItemStack stack : storage) {
			if (matches(stack, ticketId)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matches(ItemStack item, String ticketId) {
		String id = readId(item);
		return id != null && id.equalsIgnoreCase(ticketId);
	}
}
