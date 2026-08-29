package net.tfminecraft.VehicleFramework.Projectiles;

import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Util.ExplosionCreator;
import net.tfminecraft.VehicleFramework.Util.ImpactVfx;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Bullet;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public final class BulletRaycast {

	private static final double ENTITY_RAY_RADIUS = 0.4;
	private static final double BLOCK_STEP = 0.3;

	private BulletRaycast() {
	}

	public static boolean handleSegment(
			Location from,
			Location to,
			Set<Entity> ignoreEntities,
			ActiveVehicle shooterVehicle,
			AmmunitionData ammoData,
			Bullet bullet,
			ActiveWeapon weapon,
			List<Player> players) {
		Vector direction = to.toVector().subtract(from.toVector());
		double distance = direction.length();
		if (distance < 1e-6) {
			return false;
		}
		direction.normalize();

		RayTraceResult entityResult = from.getWorld().rayTraceEntities(
				from,
				direction,
				distance,
				ENTITY_RAY_RADIUS,
				entity -> isValidTarget(entity, ignoreEntities));

		if (entityResult != null && entityResult.getHitEntity() != null) {
			Entity hit = entityResult.getHitEntity();
			Location hitPoint = entityResult.getHitPosition().toLocation(from.getWorld());

			if (hit instanceof LivingEntity living) {
				if (bullet.getData().isExplosive()) {
					triggerExplosion(hitPoint, bullet, weapon);
				} else {
					ExplosionCreator.applyDamage(
							living,
							Weapon.effectiveDamage(weapon, ammoData),
							Weapon.effectiveDamageType(weapon, ammoData));
				}
				return true;
			}

			ActiveVehicle targetVehicle = VehicleFramework.getVehicleManager().get(hit);
			if (targetVehicle != null && !isShooterVehicle(targetVehicle, shooterVehicle)) {
				targetVehicle.damage(
						Weapon.effectiveDamageType(weapon, ammoData),
						Weapon.effectiveDamage(weapon, ammoData));
				if (bullet.getData().isExplosive()) {
					triggerExplosion(hitPoint, bullet, weapon);
				}
				return true;
			}
		}

		return handleBlockSegment(from, to, direction, distance, ammoData, bullet, weapon, players);
	}

	private static boolean isValidTarget(Entity entity, Set<Entity> ignoreEntities) {
		if (ignoreEntities.contains(entity)) {
			return false;
		}
		if (entity instanceof LivingEntity living) {
			return !living.isDead();
		}
		return VehicleFramework.getVehicleManager().get(entity) != null;
	}

	private static boolean isShooterVehicle(ActiveVehicle target, ActiveVehicle shooter) {
		if (shooter == null) {
			return false;
		}
		return target.getUUID().equalsIgnoreCase(shooter.getUUID());
	}

	private static boolean handleBlockSegment(
			Location from,
			Location to,
			Vector direction,
			double distance,
			AmmunitionData ammoData,
			Bullet bullet,
			ActiveWeapon weapon,
			List<Player> players) {
		World world = from.getWorld();
		Vector stepVector = direction.clone().multiply(BLOCK_STEP);
		Location current = from.clone();

		for (double traveled = 0; traveled < distance; traveled += BLOCK_STEP) {
			current.add(stepVector);

			Block block = current.getBlock();
			if (block.getType().isAir()) {
				continue;
			}

			if (isBulletPassable(block.getType())) {
				if (block.getType().name().contains("GLASS")) {
					Location glassHit = ImpactVfx.onBlockSurface(current, direction, block);
					world.playSound(glassHit, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
					ImpactVfx.spawn(
							glassHit,
							Particle.BLOCK_CRACK,
							10,
							0.1, 0.1, 0.1,
							0,
							block.getBlockData());
				}
				continue;
			}

			if (!intersectsCollision(block, current)) {
				continue;
			}

			if (bullet.getData().isExplosive()) {
				triggerExplosion(current, bullet, weapon);
			} else {
				Location impact = ImpactVfx.onBlockSurface(current, direction, block);
				ammoData.hitFX(players, impact, 1f);
				ImpactVfx.spawn(
						impact,
						Particle.EXPLOSION_NORMAL,
						2,
						0.2, 0.2, 0.2,
						0,
						null);
				world.playSound(impact, Sound.BLOCK_STONE_BREAK, 1f, 2f);
			}
			return true;
		}

		return false;
	}

	static boolean isBulletPassable(Material type) {
		return isBulletPassableName(type.name());
	}

	static boolean isBulletPassableName(String materialName) {
		if (materialName.contains("LEAVES")) {
			return true;
		}
		return materialName.contains("GLASS");
	}

	static boolean intersectsCollision(Block block, Location point) {
		if (block.isPassable()) {
			return false;
		}

		var shape = block.getCollisionShape();
		if (shape.getBoundingBoxes().isEmpty()) {
			return false;
		}

		Location base = block.getLocation();
		for (BoundingBox box : shape.getBoundingBoxes()) {
			BoundingBox worldBox = box.clone().shift(
					base.getX(),
					base.getY(),
					base.getZ());
			if (worldBox.contains(point.toVector())) {
				return true;
			}
		}
		return false;
	}

	public static void triggerExplosion(Location loc, Bullet bullet, ActiveWeapon weapon) {
		if (!bullet.getData().isExplosive()) {
			return;
		}
		AmmunitionData ammo = bullet.getData();
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 8, 1);
		ExplosionCreator.triggerExplosion(
				loc,
				ammo.getYield(),
				ammo.getRadius(),
				Weapon.effectiveDamage(weapon, ammo),
				Weapon.effectiveDamageType(weapon, ammo));
	}
}
