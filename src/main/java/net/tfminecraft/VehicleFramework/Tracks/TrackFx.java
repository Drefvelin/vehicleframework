package net.tfminecraft.VehicleFramework.Tracks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Util.ImpactVfx;

public final class TrackFx {
	private TrackFx() {
	}

	public static int[] laneOffsets(int width) {
		int w = Math.max(1, width);
		int half = w / 2;
		int[] lanes = new int[2 * half + 1];
		int i = 0;
		for (int side = -half; side <= half; side++) {
			lanes[i++] = side;
		}
		return lanes;
	}

	public static double[] rightOf(float yaw) {
		double yawRad = Math.toRadians(yaw);
		double fx = -Math.sin(yawRad);
		double fz = Math.cos(yawRad);
		return new double[] {fz, -fx};
	}

	public static void crumbs(World world, TrackPose pose) {
		spawnCrumbs(
				world,
				pose,
				Cache.trackFxParticle,
				Cache.trackFxBlock,
				Cache.trackFxCount,
				Cache.trackFxWidth,
				Cache.trackFxExtra,
				Cache.trackFxYOffset);
	}

	public static void place(World world, TrackPose pose) {
		placeCrumbs(world, pose);
		if (pose != null) {
			placeSound(world, pose.x, pose.y, pose.z);
		}
	}

	public static void hit(Block block) {
		if (block == null || block.getWorld() == null) {
			return;
		}
		place(block.getWorld(), new TrackPose(
				block.getX() + 0.5,
				block.getY() + 1.05,
				block.getZ() + 0.5,
				0,
				0));
	}

	public static void hitNear(Location at) {
		if (at == null || at.getWorld() == null) {
			return;
		}
		place(at.getWorld(), new TrackPose(at.getX(), at.getY(), at.getZ(), 0, 0));
	}

	public static void placeCrumbs(World world, TrackPose pose) {
		spawnCrumbs(
				world,
				pose,
				Cache.trackBuildParticle,
				Cache.trackBuildBlock,
				Cache.trackBuildCount,
				Cache.trackBuildWidth,
				Cache.trackBuildExtra,
				Cache.trackBuildYOffset);
	}

	private static void spawnCrumbs(
			World world,
			TrackPose pose,
			Particle particle,
			Material block,
			int count,
			int width,
			double extra,
			double yOffset) {
		if (world == null || pose == null || particle == null || count <= 0) {
			return;
		}
		double[] right = rightOf(pose.yaw);
		int n = Math.max(1, count);
		Object data = blockData(particle, block);
		double y = pose.y + yOffset;
		for (int side : laneOffsets(width)) {
			double x = pose.x + right[0] * side;
			double z = pose.z + right[1] * side;
			Location loc = new Location(world, x, y, z);
			ImpactVfx.spawn(loc, particle, n, 0.12, 0.04, 0.12, extra, data);
		}
	}

	public static void clack(World world, TrackPose pose) {
		if (world == null || pose == null) {
			return;
		}
		String sound = Cache.trackFxSound;
		if (sound == null || sound.isBlank()) {
			return;
		}
		Location loc = new Location(world, pose.x, pose.y + Cache.trackFxYOffset, pose.z);
		world.playSound(
				loc,
				sound,
				SoundCategory.BLOCKS,
				Cache.trackFxSoundVolume,
				Cache.trackFxSoundPitch);
	}

	public static void placeSound(World world, double x, double y, double z) {
		if (world == null) {
			return;
		}
		Location loc = new Location(world, x, y + Cache.trackBuildYOffset, z);
		play(world, loc, Cache.trackBuildSound, Cache.trackBuildSoundVolume, Cache.trackBuildSoundPitch);
		play(world, loc, Cache.trackBuildSound2, Cache.trackBuildSound2Volume, Cache.trackBuildSound2Pitch);
	}

	private static void play(World world, Location loc, String sound, float volume, float pitch) {
		if (sound == null || sound.isBlank()) {
			return;
		}
		world.playSound(loc, sound, SoundCategory.BLOCKS, volume, pitch);
	}

	private static BlockData blockData(Particle particle, Material block) {
		if (particle == null || block == null) {
			return null;
		}
		if (particle != Particle.BLOCK_CRACK && particle != Particle.BLOCK_DUST && particle != Particle.FALLING_DUST) {
			return null;
		}
		return block.createBlockData();
	}
}
