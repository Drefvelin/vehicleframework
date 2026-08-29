package net.tfminecraft.VehicleFramework.Tracks;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Permissions.Permissions;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TrainHandler;

public final class TrainTapeInteract {
	private TrainTapeInteract() {
	}

	public static boolean handle(Player player, ActiveVehicle vehicle) {
		if (player == null || vehicle == null || !vehicle.isTrain() || vehicle.hasParent()) {
			return false;
		}
		ItemStack hand = player.getInventory().getItemInMainHand();
		boolean recorder = TrackTools.isRecorder(hand);
		boolean emptyHand = hand == null || hand.getType().isAir() || hand.getAmount() <= 0;
		TrainHandler train = vehicle.getTrainHandler();
		boolean sneakEject = player.isSneaking() && emptyHand && train.hasInstalledTape();
		if (!recorder && !sneakEject) {
			return false;
		}
		if (!canEdit(player, vehicle)) {
			player.sendMessage("§cYou do not own this vehicle");
			RecorderLog.interact("deny-owner", player.getName());
			return true;
		}
		if (sneakEject) {
			return eject(player, train);
		}
		if (!train.isBound()) {
			player.sendMessage("§cThis locomotive is not on a track");
			RecorderLog.interact("not-bound", player.getName());
			return true;
		}
		if (player.isSneaking()) {
			return install(player, train, hand);
		}
		return toggleRecord(player, train, hand);
	}

	private static boolean canEdit(Player player, ActiveVehicle vehicle) {
		if (Permissions.isAdmin(player)) {
			return true;
		}
		String owner = vehicle.getOwnerData().getOwner();
		return owner != null && owner.equalsIgnoreCase("player_" + player.getName());
	}

	private static boolean toggleRecord(Player player, TrainHandler train, ItemStack hand) {
		if (train.isRecording()) {
			train.stopRecording(hand);
			player.sendMessage("§eRecording cancelled");
			RecorderLog.interact("cancel", player.getName());
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.8f);
			return true;
		}
		if (!train.canRecordCircuit()) {
			player.sendMessage("§cRecording only works on a circuit");
			RecorderLog.interact("not-circuit", player.getName());
			return true;
		}
		ThrottleTapeItems.write(hand, null);
		train.startRecording(player);
		player.sendMessage("§aRecording started. Drive one lap.");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.4f);
		return true;
	}

	private static boolean install(Player player, TrainHandler train, ItemStack hand) {
		ThrottleTape tape = ThrottleTapeItems.read(hand);
		if (tape == null || tape.isEmpty()) {
			player.sendMessage("§cThis recorder has no tape");
			RecorderLog.interact("empty-item", player.getName());
			return true;
		}
		if (!tape.matchesSpline(train.getSplineId())) {
			player.sendMessage("§cThat tape does not match this track");
			RecorderLog.interact("mismatch", player.getName() + " tape=" + tape.getSplineId() + " spline=" + train.getSplineId());
			return true;
		}
		if (train.hasInstalledTape()) {
			player.sendMessage("§cRemove the current tape first");
			RecorderLog.interact("already-loaded", player.getName());
			return true;
		}
		train.setInstalledTape(tape);
		consumeOne(player.getInventory(), hand);
		player.sendMessage("§aTape loaded");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.2f);
		return true;
	}

	private static boolean eject(Player player, TrainHandler train) {
		ThrottleTape tape = train.getInstalledTape();
		ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath(Cache.trackRecorderItem);
		if (item == null) {
			player.sendMessage("§cCould not create a recorder item");
			return true;
		}
		item.setAmount(1);
		ThrottleTapeItems.write(item, tape);
		train.setInstalledTape(null);
		RecorderLog.interact("eject", player.getName());
		java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
		if (!leftover.isEmpty() && player.getWorld() != null) {
			for (ItemStack extra : leftover.values()) {
				player.getWorld().dropItemNaturally(player.getLocation(), extra);
			}
		}
		player.sendMessage("§aTape removed");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 0.8f);
		return true;
	}

	private static void consumeOne(PlayerInventory inventory, ItemStack hand) {
		if (hand.getAmount() <= 1) {
			inventory.setItemInMainHand(null);
		} else {
			hand.setAmount(hand.getAmount() - 1);
		}
	}
}
