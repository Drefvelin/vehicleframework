package net.tfminecraft.VehicleFramework.Tracks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class TrackToolListener implements Listener {

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		ItemStack item = event.getItem();
		boolean layer = TrackTools.isLayer(item);
		boolean remover = TrackTools.isRemover(item);
		boolean junction = TrackTools.isJunction(item);
		if (!layer && !remover && !junction) {
			return;
		}
		Action action = event.getAction();
		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		Player player = event.getPlayer();
		event.setCancelled(true);
		Location at = clickLocation(player, block);
		if (junction) {
			if (at == null) {
				player.sendMessage("§cClick existing track to start a junction.");
				return;
			}
			if (action == Action.RIGHT_CLICK_BLOCK) {
				if (!TrackCommands.skipDuplicateToolUse(player)) {
					TrackCommands.startJunction(player, at);
				}
			}
			return;
		}
		if (layer && !TrackSupport.isValidClick(block)) {
			player.sendMessage("§cClick solid ground, not grass or plants.");
			return;
		}
		if (at == null) {
			if (layer) {
				player.sendMessage("§cClick solid ground, not grass or plants.");
			}
			return;
		}
		if (remover) {
			if (action == Action.LEFT_CLICK_BLOCK) {
				if (!TrackCommands.skipDuplicateToolUse(player)) {
					TrackCommands.digAt(player, at);
				}
			}
			return;
		}
		if (action == Action.LEFT_CLICK_BLOCK) {
			if (!TrackCommands.skipDuplicateToolUse(player) && TrackCommands.markStart(player, at)) {
				TrackFx.hit(block);
			}
			return;
		}
		TrackCommands.markEnd(player, at, block);
	}

	static Location clickLocation(Player player, Block block) {
		Double sit = TrackSupport.sitY(block);
		if (sit == null) {
			return null;
		}
		Location at = block.getLocation();
		at.setX(block.getX() + 0.5);
		at.setY(sit);
		at.setZ(block.getZ() + 0.5);
		at.setYaw(player.getLocation().getYaw());
		at.setPitch(player.getLocation().getPitch());
		return at;
	}
}
