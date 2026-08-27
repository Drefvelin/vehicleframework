package net.tfminecraft.VehicleFramework.Util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class ImpactVfx {

	public static final double VIEW_RANGE = 320.0;
	public static final double VIEW_RANGE_SQ = VIEW_RANGE * VIEW_RANGE;
	private static final double SURFACE_NUDGE = 0.06;

	private ImpactVfx() {
	}

	public static Location onBlockSurface(Location inside, Vector incomingDir, Block block) {
		if (inside == null || inside.getWorld() == null || incomingDir == null) {
			return inside;
		}
		Vector dir = incomingDir.clone();
		if (dir.lengthSquared() < 1e-12) {
			return inside.clone();
		}
		dir.normalize();
		World world = inside.getWorld();
		Location start = inside.clone().subtract(dir.clone().multiply(1.5));
		RayTraceResult result = world.rayTraceBlocks(start, dir, 3.0, FluidCollisionMode.NEVER, true);
		if (result != null && result.getHitPosition() != null) {
			Location hit = result.getHitPosition().toLocation(world);
			Vector outward = surfaceOffset(dir, result.getHitBlockFace() == null
					? null
					: result.getHitBlockFace().getDirection());
			return hit.add(outward);
		}
		if (block != null) {
			return inside.clone().subtract(dir.clone().multiply(SURFACE_NUDGE));
		}
		return inside.clone().subtract(dir.multiply(SURFACE_NUDGE));
	}

	static Vector surfaceOffset(Vector incomingNormalized, Vector faceNormal) {
		if (faceNormal != null && faceNormal.lengthSquared() > 1e-12) {
			return faceNormal.clone().normalize().multiply(SURFACE_NUDGE);
		}
		return incomingNormalized.clone().multiply(-SURFACE_NUDGE);
	}

	public static void spawn(
			Location loc,
			Particle particle,
			int count,
			double offsetX,
			double offsetY,
			double offsetZ,
			double extra,
			Object data) {
		if (loc == null || loc.getWorld() == null || particle == null) {
			return;
		}
		for (Player player : loc.getWorld().getPlayers()) {
			if (player.getLocation().distanceSquared(loc) > VIEW_RANGE_SQ) {
				continue;
			}
			if (data != null) {
				player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra, data);
			} else {
				player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra);
			}
		}
	}
}
