package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Permissions.Permissions;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class VehicleTicketInteract {
	private VehicleTicketInteract() {
	}

	public static boolean handle(Player player, ActiveVehicle vehicle) {
		if (player == null || vehicle == null) {
			return false;
		}
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (hand == null || hand.getType().isAir() || Cache.ticketItem == null || Cache.ticketItem.isBlank()) {
			return false;
		}
		if (!TLibs.getItemAPI().getChecker().checkItemWithPath(hand, Cache.ticketItem)) {
			return false;
		}
		if (VehicleTicketItems.readId(hand) != null) {
			return false;
		}
		if (!canMint(player, vehicle)) {
			return false;
		}
		ActiveVehicle source = vehicle.ticketSource();
		if (!source.getOwnerData().isTicketsEnabled() || source.getOwnerData().getTicketId() == null) {
			player.sendMessage("§cTickets are not enabled on this vehicle");
			return true;
		}
		mintOne(player, hand, source);
		player.sendMessage("§aCreated a ticket for §e" + source.getName());
		player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
		return true;
	}

	private static boolean canMint(Player player, ActiveVehicle vehicle) {
		if (Permissions.isAdmin(player)) {
			return true;
		}
		String owner = vehicle.ticketSource().getOwnerData().getOwner();
		return owner != null && owner.equalsIgnoreCase("player_" + player.getName());
	}

	private static void mintOne(Player player, ItemStack hand, ActiveVehicle source) {
		String ticketId = source.getOwnerData().getTicketId();
		String name = source.getName();
		if (hand.getAmount() <= 1) {
			VehicleTicketItems.write(hand, ticketId, name);
			return;
		}
		hand.setAmount(hand.getAmount() - 1);
		ItemStack ticket = hand.clone();
		ticket.setAmount(1);
		VehicleTicketItems.write(ticket, ticketId, name);
		PlayerInventory inv = player.getInventory();
		java.util.HashMap<Integer, ItemStack> leftover = inv.addItem(ticket);
		if (!leftover.isEmpty() && player.getWorld() != null) {
			for (ItemStack extra : leftover.values()) {
				player.getWorld().dropItemNaturally(player.getLocation(), extra);
			}
		}
	}
}
