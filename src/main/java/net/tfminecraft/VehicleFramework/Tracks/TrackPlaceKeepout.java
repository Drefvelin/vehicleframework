package net.tfminecraft.VehicleFramework.Tracks;

import java.util.List;

import org.bukkit.block.Block;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.VehicleFramework;

public final class TrackPlaceKeepout {
	private TrackPlaceKeepout() {
	}

	public static boolean blocked(
			double sampleX,
			double sampleY,
			double sampleZ,
			int blockX,
			int blockY,
			int blockZ,
			double radius) {
		if (radius <= 0) {
			return false;
		}
		if (blockY < (int) Math.floor(sampleY)) {
			return false;
		}
		double dx = (blockX + 0.5) - sampleX;
		double dz = (blockZ + 0.5) - sampleZ;
		return dx * dx + dz * dz < radius * radius;
	}

	public static boolean blocked(List<TrackSample> samples, int blockX, int blockY, int blockZ, double radius) {
		if (samples == null || samples.isEmpty() || radius <= 0) {
			return false;
		}
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		int minFloorY = Integer.MAX_VALUE;
		for (TrackSample sample : samples) {
			minX = Math.min(minX, sample.x);
			maxX = Math.max(maxX, sample.x);
			minZ = Math.min(minZ, sample.z);
			maxZ = Math.max(maxZ, sample.z);
			minFloorY = Math.min(minFloorY, (int) Math.floor(sample.y));
		}
		if (blockY < minFloorY) {
			return false;
		}
		double cx = blockX + 0.5;
		double cz = blockZ + 0.5;
		if (cx < minX - radius || cx > maxX + radius || cz < minZ - radius || cz > maxZ + radius) {
			return false;
		}
		for (TrackSample sample : samples) {
			if (blocked(sample.x, sample.y, sample.z, blockX, blockY, blockZ, radius)) {
				return true;
			}
		}
		return false;
	}

	public static boolean blocked(Block block) {
		if (block == null) {
			return false;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return false;
		}
		double radius = Cache.trackPlaceKeepoutRadius;
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		for (TrackSpline spline : registry.inWorld(block.getWorld().getName())) {
			if (blocked(spline.getSamples(), x, y, z, radius)) {
				return true;
			}
		}
		return false;
	}
}
