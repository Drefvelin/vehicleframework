package net.tfminecraft.VehicleFramework.Tracks;

import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.Cache.Cache;

public final class TrackTools {
	private TrackTools() {
	}

	public static boolean isLayer(ItemStack item) {
		return matches(item, Cache.trackLayerItem);
	}

	public static boolean isRemover(ItemStack item) {
		return matches(item, Cache.trackRemoverItem);
	}

	public static boolean isRecorder(ItemStack item) {
		return matches(item, Cache.trackRecorderItem);
	}

	public static boolean isJunction(ItemStack item) {
		return matches(item, Cache.trackJunctionItem);
	}

	private static boolean matches(ItemStack item, String path) {
		if (item == null || path == null || path.isBlank()) {
			return false;
		}
		return TLibs.getItemAPI().getChecker().checkItemWithPath(item, path);
	}
}
