package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Database.LogWriter;
import net.tfminecraft.VehicleFramework.Events.VFEntityDamageEvent;
import net.tfminecraft.VehicleFramework.Events.VFExplosionEvent;

public class ExplosionCreator {
	public static void triggerExplosion(Location explosionCenter, double yield, double blastRadius, double damage, String cause) {
		VFExplosionEvent event = new VFExplosionEvent(explosionCenter);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }
	    int particles = (int) Math.round(yield*15);
	    Random random = new Random();
		// Falling Block Effect
		if (event.doesBlockDamage()) {
			int conversionRadius = (int) Math.round(yield * 1.5);
			int maxDepth = (int) Math.max(3, yield * 1.2);
			double radiusSq = conversionRadius * conversionRadius;

			for (int x = -conversionRadius; x <= conversionRadius; x++) {
				for (int z = -conversionRadius; z <= conversionRadius; z++) {
					double distSqXZ = (x * x) + (z * z);
					if (distSqXZ > radiusSq) continue;

					double dist = Math.sqrt(distSqXZ);
					int depth = (int) Math.ceil(maxDepth * (1 - (dist / conversionRadius)));

					for (int y = 0; y >= -depth; y--) {
						Location loc = explosionCenter.clone().add(x, y, z);
						Block block = loc.getBlock();

						if (block.getType() == Material.AIR) continue;
						if (Cache.ignoreExplode.contains(block.getType())) continue;

						Material originalType = block.getType();
						BlockData originalData = block.getBlockData();

						// Determine if it's "soft ground" (guaranteed removal)
						boolean isSoftGround = originalType == Material.DIRT
								|| originalType == Material.GRASS_BLOCK
								|| originalType == Material.PODZOL
								|| originalType == Material.COARSE_DIRT
								|| originalType == Material.ROOTED_DIRT
								|| originalType == Material.MYCELIUM
								|| originalType == Material.FARMLAND
								|| originalType == Material.SNOW
								|| originalType == Material.SNOW_BLOCK;

						// Choose debris type (convert if mapping exists, else original)
						Material debrisMaterial = Cache.convertExplode.getOrDefault(originalType, originalType);
						BlockData debrisData = Bukkit.createBlockData(debrisMaterial);

						if (isSoftGround) {
							// ✅ Guaranteed removal
							LogWriter.logBreak(cause, block);
							block.setType(Material.AIR);

							// Still spawn debris optionally
							double chance = 0.7; // soil is lightweight, lots of debris
							if (random.nextDouble() < chance) {
								spawnDebris(loc, explosionCenter, debrisData, originalType, cause, random);
							}
						} else {
							// ⛏ Hard block (stone, ores, etc.) → chance-based destruction
							float blastResistance = originalData.getMaterial().getBlastResistance();
							double chance = 1.0 - (blastResistance / 20.0);
							chance = Math.max(0.05, Math.min(chance, 0.6)); // stronger blocks: lower removal chance

							if (random.nextDouble() < chance) {
								LogWriter.logBreak(cause, block);
								block.setType(Material.AIR);

								if (random.nextDouble() < 0.5) { // smaller chance to spawn debris for stone
									spawnDebris(loc, explosionCenter, debrisData, originalType, cause, random);
								}
							}
						}
					}
				}
			}
		}




	    // Spawn explosion effects and physical explosion
	    final List<Player> players = new ArrayList<Player>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(!p.getWorld().equals(explosionCenter.getWorld())) continue;
			if(p.getLocation().distanceSquared(explosionCenter) < 102400) {
				players.add(p);
			}
		}
		for(Player p : players) {
			p.spawnParticle(Particle.EXPLOSION_HUGE, explosionCenter, particles, 0, 0, 0, 0);
		}
	    double halfBlastRadius = blastRadius / 2;
	    Collection<Entity> nearby = explosionCenter.getWorld().getNearbyEntities(explosionCenter, blastRadius, blastRadius, blastRadius);
	    List<Entity> noKnockback = new ArrayList<>();
	    for(Entity entity : nearby) {
			if(entity instanceof Player) {
				Player p = (Player) entity;
				if(p.getGameMode().equals(GameMode.SPECTATOR) || p.getGameMode().equals(GameMode.CREATIVE)) noKnockback.add(entity);
				continue;
			}
	    	if(VehicleFramework.getVehicleManager().get(entity) != null) {
	    		noKnockback.addAll(VehicleFramework.getVehicleManager().get(entity).getSeatHandler().getPassengers());
	    		noKnockback.add(entity);
	    	}
	    }
	    for (Entity entity : explosionCenter.getWorld().getNearbyEntities(explosionCenter, blastRadius, blastRadius, blastRadius)) {
	        if (entity instanceof LivingEntity) { // Ensure it's a living entity (like players, mobs, etc.)
	        	LivingEntity livingEntity = (LivingEntity) entity;
	            double distance = livingEntity.getLocation().distance(explosionCenter);
	            if (distance <= blastRadius) {
	                // Optional: Scale damage based on distance
	                double scaledDamage = damage;
	                if (distance > halfBlastRadius) {
	                    // Linear falloff after half the radius
	                    scaledDamage = damage * (1 - ((distance - halfBlastRadius) / (blastRadius - halfBlastRadius)));
	                }
	                scaledDamage = Math.max(0, scaledDamage); // Ensure no negative damage
	                applyDamage(livingEntity, scaledDamage, cause);
	                // Apply knockback to the entity
	                if(!noKnockback.contains(livingEntity)) {
	                	Vector knockback = livingEntity.getLocation().toVector().subtract(explosionCenter.toVector());
		                if (knockback.length() > 0) {
		                    knockback = knockback.normalize();
		                } else {
		                    knockback = new Vector(0, 0, 0); // Default to zero vector
		                }
		                double velocityScale = yield * (1.5 - (distance / blastRadius)); // Scale by yield and distance
		                Vector velocity = knockback.multiply(velocityScale).add(new Vector(0, 0.5 * yield, 0)); // Add upward lift
		                livingEntity.setVelocity(velocity);
	                }
	            }
	        }
	    }
	    //explosionCenter.getWorld().createExplosion(explosionCenter, (float) yield, false, event.doesBlockDamage());
	}
	
	public static void applyDamage(Entity e, double damage, String cause) {
		if(!(e instanceof LivingEntity)) return;
        if (e instanceof Player) {
        	Player player = (Player) e;
            double armorValue = player.getAttribute(Attribute.GENERIC_ARMOR).getValue(); // Get armor value
            double damageReductionFactor = Math.max(0, 1 - (armorValue / 80)); // Armor reduces damage by 50% at max (20 armor)
            damage *= damageReductionFactor; // Apply armor scaling
        }
        LivingEntity l = (LivingEntity) e;
        VFEntityDamageEvent event = new VFEntityDamageEvent(l, null, cause, damage);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            Damager.damage(l, event.getDamage());
        }
        
	}

	private static void spawnDebris(Location loc, Location explosionCenter,
                               BlockData debrisData, Material originalType,
                               String cause, Random random) {

		FallingBlock fallingBlock = explosionCenter.getWorld().spawnFallingBlock(
			loc.clone().add(0, 1, 0), debrisData
		);

		// Outward horizontal velocity from explosion center
		Vector dir = loc.toVector().subtract(explosionCenter.toVector());
		dir.setY(0);
		if (dir.lengthSquared() < 1e-6) {
			dir = new Vector((random.nextDouble() - 0.5), 0, (random.nextDouble() - 0.5));
		}
		dir.normalize();

		Vector velocity = dir.multiply(0.8 + random.nextDouble() * 0.6);
		velocity.setY(0.4 + random.nextDouble() * 0.4);
		fallingBlock.setVelocity(velocity);

		fallingBlock.setDropItem(false);
		if (Cache.ignoreLands.contains(originalType)) {
			fallingBlock.setCancelDrop(true);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (fallingBlock.isDead() || fallingBlock.isOnGround()) {
					if (fallingBlock.isOnGround()) {
						Block landedBlock = fallingBlock.getLocation().getBlock();
						Block belowBlock = landedBlock.getRelative(BlockFace.DOWN);

						// Always remove if ground is invalid
						if (Cache.ignoreGround.contains(belowBlock.getType())) {
							fallingBlock.remove();
						} else {
							// Distance factor: further debris is less likely to stay
							double distSq = landedBlock.getLocation().distanceSquared(explosionCenter);
							double maxDistSq = Math.pow(64, 2); // normalize against some max distance (64 blocks here)
							double distanceFactor = Math.min(1.0, distSq / maxDistSq);

							// Probability to place decreases with distance
							double placeChance = 1.0 - (0.7 * distanceFactor); 
							// → at center ~100%, at maxDist ~30%

							// Pillar prevention: check neighbors around belowBlock
							boolean hasSupport = false;
							for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
								if (belowBlock.getRelative(face).getType().isSolid()) {
									hasSupport = true;
									break;
								}
							}

							if (!hasSupport) {
								// No neighbors = lone pillar → remove
								fallingBlock.remove();
							} else if (random.nextDouble() < placeChance) {
								// Passed distance & support checks → allow placement
								LogWriter.logPlace(cause, landedBlock);
							} else {
								// Too far / unlucky → vanish
								fallingBlock.remove();
							}
						}
					}
					this.cancel();
					return;
				}


				fallingBlock.getWorld().spawnParticle(
					Particle.BLOCK_DUST,
					fallingBlock.getLocation().add(0.5, 0.5, 0.5),
					8,
					0.1, 0.1, 0.1,
					0,
					fallingBlock.getBlockData()
				);
			}
		}.runTaskTimer(VehicleFramework.plugin, 0L, 2L);
	}

}
