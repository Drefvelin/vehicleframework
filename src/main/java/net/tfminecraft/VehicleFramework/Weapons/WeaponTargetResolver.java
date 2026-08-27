package net.tfminecraft.VehicleFramework.Weapons;

import java.util.Set;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class WeaponTargetResolver {

	private WeaponTargetResolver() {
	}

	public static Location resolveTarget(Player player, ActiveVehicle shooterVehicle, double maxRange) {
		if (player == null) {
			return null;
		}

		double range = maxRange > 0 ? maxRange : Weapon.DEFAULT_CURSOR_RANGE;
		Location eye = player.getEyeLocation();
		Vector direction = eye.getDirection().clone().normalize();
		World world = eye.getWorld();
		if (world == null) {
			return fallbackPoint(eye, direction, range);
		}

		Set<Entity> ignore = WeaponEntityFilters.buildShooterIgnoreSet(
				player,
				shooterVehicle == null ? null : shooterVehicle.getEntity(),
				shooterVehicle);

		RayTraceResult blockHit = world.rayTraceBlocks(eye, direction, range, FluidCollisionMode.NEVER, true);
		RayTraceResult entityHit = world.rayTraceEntities(
				eye,
				direction,
				range,
				0.5,
				entity -> isValidTarget(entity, ignore));

		double blockDistance = distance(blockHit, eye);
		double entityDistance = distance(entityHit, eye);

		if (blockDistance < 0 && entityDistance < 0) {
			return fallbackPoint(eye, direction, range);
		}
		if (entityDistance >= 0 && (blockDistance < 0 || entityDistance <= blockDistance)) {
			return entityHit.getHitPosition().toLocation(world);
		}
		return blockHit.getHitPosition().toLocation(world);
	}

	static Location fallbackPoint(Location eye, Vector direction, double range) {
		return eye.clone().add(direction.clone().multiply(range));
	}

	private static boolean isValidTarget(Entity entity, Set<Entity> ignore) {
		if (entity == null || ignore.contains(entity)) {
			return false;
		}
		if (entity instanceof LivingEntity living) {
			return !living.isDead();
		}
		return false;
	}

	private static double distance(RayTraceResult result, Location from) {
		if (result == null) {
			return -1;
		}
		return from.distance(result.getHitPosition().toLocation(from.getWorld()));
	}
}
