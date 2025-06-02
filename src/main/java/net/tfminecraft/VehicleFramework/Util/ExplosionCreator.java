package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Events.VFEntityDamageEvent;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class ExplosionCreator {
	public static void triggerExplosion(Location explosionCenter, double yield, double blastRadius, double damage, String cause) {
	    int radius = (int) Math.round(yield);
	    int particles = (int) Math.round(yield*15);
	    Random random = new Random();

	    //Fallin Block Effect
	    for (int x = -radius; x <= radius; x++) {
			if(!Cache.blockDamage) break;
	        for (int y = -radius; y <= radius; y++) {
	            for (int z = -radius; z <= radius; z++) {
	                Location loc = explosionCenter.clone().add(x, y, z);
	                Block block = loc.getBlock();
	                if (block.getType() != Material.AIR) {
	                    Material blockType = block.getType();
	                    if (Cache.ignoreExplode.contains(blockType)) continue;
	                    if (Cache.convertExplode.containsKey(blockType)) {
	                        block.setType(Cache.convertExplode.get(blockType));
	                        continue;
	                    }
	                    float blastResistance = block.getBlockData().getMaterial().getBlastResistance();
	                    
	                    // Calculate probability (lower resistance = higher chance)
	                    double chance = 1.0 - (blastResistance / 20.0);
	                    chance = Math.max(0.1, Math.min(chance, 0.9));
	                    // Decide whether to spawn as falling block
	                    if (random.nextDouble() < chance) {
	                    	BlockData blockData = block.getBlockData();

	                        // Check if the block is a SLAB
	                        if (blockData instanceof Slab) {
	                            Slab slab = (Slab) blockData;
	                            if (slab.getType() == Slab.Type.TOP) {
	                                slab.setType(Slab.Type.BOTTOM); // Convert to lower slab
	                            }
	                            blockData = slab;
	                        }

	                        FallingBlock fallingBlock = explosionCenter.getWorld().spawnFallingBlock(loc, blockData);
	                        
	                        

	                        Vector velocity = new Vector(
	                            random.nextDouble() - 0.5,
	                            random.nextDouble() * 1,
	                            random.nextDouble() - 0.5
	                        ).multiply(1.5);

	                        fallingBlock.setVelocity(velocity);
	                        fallingBlock.setDropItem(false);

	                        if (Cache.ignoreLands.contains(blockType)) {
	                            fallingBlock.setCancelDrop(true);
	                        }

	                        block.setType(Material.AIR); // Clear the block
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
		                double velocityScale = yield * (1 - (distance / blastRadius)); // Scale by yield and distance
		                Vector velocity = knockback.multiply(velocityScale).add(new Vector(0, 0.5 * yield, 0)); // Add upward lift
		                livingEntity.setVelocity(velocity);
	                }
	            }
	        }
	    }
	    explosionCenter.getWorld().createExplosion(explosionCenter, (float) yield, false, Cache.blockDamage);
	}
	
	public static void applyDamage(Entity e, double damage, String cause) {
		if(!(e instanceof LivingEntity)) return;
        if (e instanceof Player) {
        	Player player = (Player) e;
            double armorValue = player.getAttribute(Attribute.GENERIC_ARMOR).getValue(); // Get armor value
            double damageReductionFactor = Math.max(0, 1 - (armorValue / 40)); // Armor reduces damage by 50% at max (20 armor)
            damage *= damageReductionFactor; // Apply armor scaling
        }
        LivingEntity l = (LivingEntity) e;
        VFEntityDamageEvent event = new VFEntityDamageEvent(l, null, cause, damage);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            l.setHealth(Math.max(0, l.getHealth() - event.getDamage()));
        }
        
	}
}
