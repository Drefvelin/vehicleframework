package net.tfminecraft.VehicleFramework.Tracks;

import java.util.function.IntFunction;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;

/**
 * Where the rail sits on a block. Plants and crops are ignored.
 * Bottom slabs (and lower stairs) sit at half a block.
 */
public final class TrackSupport {
	public static final int MAX_DROP = 8;

	private TrackSupport() {
	}

	public static boolean isValidClick(Block block) {
		return sitY(block) != null;
	}

	public static Double sitY(Block block) {
		if (block == null) {
			return null;
		}
		return sitY(block.getY(), block.getBlockData(), block.getType(), block.isPassable(), block.getType().isSolid());
	}

	static Double sitY(
			int blockY,
			BlockData data,
			Material type,
			boolean passable,
			boolean solid) {
		if (isIgnored(data, type, passable)) {
			return null;
		}
		if (data instanceof Slab slab) {
			if (slab.getType() == Slab.Type.BOTTOM) {
				return sitTop(blockY, 0.5);
			}
			return sitTop(blockY, 1.0);
		}
		if (data instanceof Stairs stairs) {
			if (stairs.getHalf() == Bisected.Half.BOTTOM) {
				return sitTop(blockY, 0.5);
			}
			return sitTop(blockY, 1.0);
		}
		if (data instanceof TrapDoor trap && !trap.isOpen()) {
			if (trap.getHalf() == Bisected.Half.BOTTOM) {
				return sitTop(blockY, 0.5);
			}
			return sitTop(blockY, 1.0);
		}
		if (!solid || passable) {
			return null;
		}
		return sitTop(blockY, 1.0);
	}

	/** collisionHeight 0 = ignore, ~0.5 = slab, 1 = full cube. */
	public static Double sitTop(int blockY, double collisionHeight) {
		if (collisionHeight <= 0.05) {
			return null;
		}
		if (collisionHeight <= 0.6) {
			return blockY + 0.5;
		}
		return blockY + 1.0;
	}

	public static boolean isIgnored(Block block) {
		return block != null && isIgnored(block.getBlockData(), block.getType(), block.isPassable());
	}

	static boolean isIgnored(BlockData data, Material type, boolean passable) {
		if (type.isAir() || type == Material.WATER || type == Material.LAVA || type == Material.CAVE_AIR) {
			return true;
		}
		if (isPlant(type)) {
			return true;
		}
		if (data instanceof Snow) {
			return true;
		}
		String name = type.name();
		if (name.endsWith("_CARPET") || type == Material.MOSS_CARPET) {
			return true;
		}
		if (data instanceof Slab || data instanceof Stairs) {
			return false;
		}
		if (data instanceof TrapDoor trap) {
			return trap.isOpen();
		}
		return passable;
	}

	static boolean isPlant(Material type) {
		return isPlantName(type.name());
	}

	static boolean isPlantName(String n) {
		if (n.equals("GRASS_BLOCK") || n.contains("GRASS_BLOCK")) {
			return false;
		}
		if (n.contains("GRASS") || n.contains("FERN") || n.equals("DEAD_BUSH")) {
			return true;
		}
		if (n.contains("SAPLING") || n.contains("FLOWER") || n.contains("TULIP") || n.contains("ORCHID")) {
			return true;
		}
		if (n.contains("CROP") || n.equals("WHEAT") || n.equals("CARROTS") || n.equals("POTATOES")
				|| n.equals("BEETROOTS") || n.contains("BERRY_BUSH")) {
			return true;
		}
		if (n.contains("MUSHROOM") && !n.contains("BLOCK") && !n.contains("STEM")) {
			return true;
		}
		if (n.contains("VINE") || n.contains("LILY") || n.contains("PETALS") || n.equals("PINK_PETALS")) {
			return true;
		}
		if (n.contains("SEAGRASS") || n.contains("KELP") || n.equals("SUGAR_CANE") || n.equals("BAMBOO")) {
			return true;
		}
		return n.equals("DANDELION") || n.equals("POPPY") || n.equals("ALLIUM") || n.equals("AZURE_BLUET")
				|| n.equals("OXEYE_DAISY") || n.equals("CORNFLOWER") || n.equals("WITHER_ROSE")
				|| n.equals("SUNFLOWER") || n.equals("LILAC") || n.equals("ROSE_BUSH") || n.equals("PEONY")
				|| n.equals("TORCHFLOWER") || n.equals("PITCHER_PLANT") || n.equals("WILDFLOWERS")
				|| n.equals("BUSH") || n.equals("FIREFLY_BUSH") || n.equals("LEAF_LITTER");
	}

	public static boolean blocksRail(Block block, double sampleY) {
		Double sit = sitY(block);
		if (sit == null) {
			return false;
		}
		return sit > sampleY + 1e-4 && block.getY() < sampleY + 3.0;
	}

	public static double snapY(World world, double x, double y, double z) {
		Double sit = firstSitY(world, x, y, z);
		return sit == null ? y : sit;
	}

	public static Double firstSitY(World world, double x, double y, double z) {
		if (world == null) {
			return null;
		}
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		return firstSitY(y, by -> sitY(world.getBlockAt(bx, by, bz)));
	}

	static Double firstSitY(double y, IntFunction<Double> sitAtBlockY) {
		int top = (int) Math.floor(y);
		int min = top - MAX_DROP;
		for (int by = top; by >= min; by--) {
			Double sit = sitAtBlockY.apply(by);
			if (sit != null) {
				return sit;
			}
		}
		return null;
	}

	public static double floorY(World world, double x, double y, double z) {
		return snapY(world, x, y, z);
	}
}
