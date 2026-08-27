package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Util.LocationChecker;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;

public class FloatController {

	private static final double BASE_BUOYANCY = 0.05;
	private static final double MAX_UPRIVER_LIFT = 0.3;

	public Vector calculateFloat(ActiveVehicle v, Vector velocity) {
		if (!checkFloat(v)) {
			return velocity;
		}

		breakLilyPadsUnderVehicle(v);

		double y = velocity.getY();
		if (v.isDestroyed()) {
			y = -0.03;
		} else if (v.getComponent(Component.HULL) instanceof SinkableHull hull && hull.hasSinkProgress()) {
			if (hull.isSinking() && checkSink(v)) {
				y = 0.01 * (hull.getSinkProgress() / 100.0) * -1;
			} else {
				y = 0.01 * ((100 - hull.getSinkProgress()) / 100.0);
			}
		} else {
			y += 0.01;
			y = Math.max(y, BASE_BUOYANCY);
			y += calculateUpriverLift(v);
		}

		velocity.setY(y);
		return velocity;
	}

	private double calculateUpriverLift(ActiveVehicle v) {
		if (v.getAccessPanel().getSpeed() <= 0) {
			return 0;
		}

		Entity entity = v.getEntity();
		if (entity == null || !entity.isValid()) {
			return 0;
		}

		Location loc = entity.getLocation();
		Vector forward = loc.getDirection().clone();
		forward.setY(0);
		if (forward.lengthSquared() < 1e-6) {
			return 0;
		}
		forward.normalize();

		double currentSurface = waterSurfaceY(loc);
		double aheadSurface = waterSurfaceY(loc.clone().add(forward.multiply(1.5)));
		double rise = aheadSurface - currentSurface;
		if (rise <= 0) {
			return 0;
		}
		return Math.min(MAX_UPRIVER_LIFT, rise * 0.5);
	}

	private double waterSurfaceY(Location loc) {
		int x = loc.getBlockX();
		int z = loc.getBlockZ();
		int startY = loc.getBlockY();

		for (int y = startY + 2; y >= startY - 3; y--) {
			Block block = loc.getWorld().getBlockAt(x, y, z);
			Block above = loc.getWorld().getBlockAt(x, y + 1, z);
			if (isWaterBlock(block) && !isWaterBlock(above)) {
				return y + 1.0;
			}
		}
		return loc.getY();
	}

	private void breakLilyPadsUnderVehicle(ActiveVehicle v) {
		Entity entity = v.getEntity();
		if (entity == null || !entity.isValid()) return;

		BoundingBox box = entity.getBoundingBox().clone().expand(1, 0, 1);

		for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.floor(box.getMaxX()); x++) {
			for (int y = (int) Math.floor(box.getMinY()); y <= (int) Math.floor(box.getMaxY()); y++) {
				for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.floor(box.getMaxZ()); z++) {
					Block block = entity.getWorld().getBlockAt(x, y, z);
					if (block.getType() == Material.LILY_PAD) {
						block.breakNaturally();
						entity.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);
					}
				}
			}
		}
	}

	private boolean checkFloat(ActiveVehicle v) {
		if (!v.shouldFloat()) {
			return false;
		}

		Entity entity = v.getEntity();
		if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) {
			return false;
		}

		BoundingBox box = entity.getBoundingBox();
		int waterCount = 0;
		int sampleCount = 0;

		int minX = (int) Math.floor(box.getMinX());
		int maxX = (int) Math.floor(box.getMaxX());
		int minZ = (int) Math.floor(box.getMinZ());
		int maxZ = (int) Math.floor(box.getMaxZ());
		int minY = (int) Math.floor(box.getMinY());
		int midY = (int) Math.floor((box.getMinY() + box.getMaxY()) / 2.0);

		for (int y : new int[] {minY, midY}) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					sampleCount++;
					if (isWaterBlock(entity.getWorld().getBlockAt(x, y, z))) {
						waterCount++;
					}
				}
			}
		}

		return sampleCount > 0 && ((double) waterCount / sampleCount) >= 0.35;
	}

	private boolean isWaterBlock(Block block) {
		if (block.isLiquid()) {
			return true;
		}
		return LocationChecker.isInWater(block.getLocation());
	}

	private boolean checkSink(ActiveVehicle v) {
		Entity entity = v.getEntity();
		if (entity != null && entity.isValid() && entity instanceof LivingEntity) {
			Location location = entity.getLocation().clone();
			location.add(0, 2, 0);
			Block block = location.getBlock();

			if (block.isLiquid()) {
				return false;
			}
		}
		return true;
	}
}
