package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.Cache.Cache;

public final class TrackPieces {
	private TrackPieces() {
	}

	public static int cost(List<double[]> stroke) {
		if (stroke == null || stroke.size() < 2) {
			return 0;
		}
		return stroke.size() - 1;
	}

	public static boolean canAffordFirst(Player player) {
		return !pays(player) || count(player) >= 1;
	}

	public static boolean pays(Player player) {
		if (player == null) {
			return false;
		}
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return false;
		}
		String path = Cache.trackItem;
		return path != null && !path.isBlank();
	}

	public static int count(Player player) {
		if (player == null) {
			return 0;
		}
		int n = 0;
		for (ItemStack stack : stacks(player)) {
			if (matches(stack)) {
				n += stack.getAmount();
			}
		}
		return n;
	}

	public static boolean consumeOne(Player player) {
		if (!pays(player)) {
			return true;
		}
		PlayerInventory inv = player.getInventory();
		if (takeFrom(inv.getStorageContents(), inv)) {
			player.updateInventory();
			return true;
		}
		ItemStack off = inv.getItemInOffHand();
		if (matches(off)) {
			if (off.getAmount() <= 1) {
				inv.setItemInOffHand(null);
			} else {
				ItemStack next = off.clone();
				next.setAmount(off.getAmount() - 1);
				inv.setItemInOffHand(next);
			}
			player.updateInventory();
			return true;
		}
		return false;
	}

	public static int consumeUpTo(Player player, int want) {
		if (want <= 0) {
			return 0;
		}
		if (!pays(player)) {
			return want;
		}
		int took = 0;
		while (took < want && consumeOne(player)) {
			took++;
		}
		return took;
	}

	public static void dropAt(World world, double x, double y, double z) {
		if (world == null) {
			return;
		}
		int cx = TrackChunks.chunkCoord(x);
		int cz = TrackChunks.chunkCoord(z);
		if (!world.isChunkLoaded(cx, cz)) {
			return;
		}
		ItemStack item = stack();
		if (item == null) {
			return;
		}
		world.dropItemNaturally(new Location(world, x, y + Cache.trackDisplayYOffset, z), item);
	}

	public static List<double[]> persistPoints(
			List<double[]> keep,
			List<double[]> stroke,
			boolean append,
			int paid) {
		int shown = stroke == null ? 0 : Math.min(stroke.size(), Math.max(0, paid) + 1);
		if (stroke == null || shown < 2) {
			return copy(keep);
		}
		if (keep == null || keep.isEmpty()) {
			return copy(stroke.subList(0, shown));
		}
		if (append) {
			return concat(keep, stroke.subList(1, shown));
		}
		List<double[]> out = new ArrayList<>();
		for (int i = shown - 1; i >= 1; i--) {
			out.add(copyOne(stroke.get(i)));
		}
		out.addAll(copy(keep));
		return out;
	}

	private static boolean takeFrom(ItemStack[] contents, PlayerInventory inv) {
		if (contents == null) {
			return false;
		}
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if (!matches(stack)) {
				continue;
			}
			if (stack.getAmount() <= 1) {
				inv.setItem(i, null);
			} else {
				ItemStack next = stack.clone();
				next.setAmount(stack.getAmount() - 1);
				inv.setItem(i, next);
			}
			return true;
		}
		return false;
	}

	private static ItemStack[] stacks(Player player) {
		PlayerInventory inv = player.getInventory();
		ItemStack[] storage = inv.getStorageContents();
		ItemStack off = inv.getItemInOffHand();
		ItemStack[] all = new ItemStack[storage.length + 1];
		System.arraycopy(storage, 0, all, 0, storage.length);
		all[storage.length] = off;
		return all;
	}

	private static ItemStack stack() {
		String path = Cache.trackItem;
		if (path == null || path.isBlank()) {
			return null;
		}
		try {
			ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath(path);
			if (item == null || item.getType().isAir()) {
				return null;
			}
			return item;
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean matches(ItemStack item) {
		if (item == null || item.getType().isAir()) {
			return false;
		}
		String path = Cache.trackItem;
		if (path == null || path.isBlank()) {
			return false;
		}
		return TLibs.getItemAPI().getChecker().checkItemWithPath(item, path);
	}

	private static List<double[]> copy(List<double[]> points) {
		List<double[]> out = new ArrayList<>();
		if (points == null) {
			return out;
		}
		for (double[] p : points) {
			out.add(copyOne(p));
		}
		return out;
	}

	private static List<double[]> concat(List<double[]> a, List<double[]> b) {
		List<double[]> out = copy(a);
		out.addAll(copy(b));
		return out;
	}

	private static double[] copyOne(double[] p) {
		return new double[] {p[0], p[1], p[2]};
	}
}
