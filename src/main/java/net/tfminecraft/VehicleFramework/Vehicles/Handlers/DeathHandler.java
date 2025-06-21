package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Data.DeathData;
import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.CustomAction;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Projectiles.Fragment;
import net.tfminecraft.VehicleFramework.Projectiles.HitChecker;
import net.tfminecraft.VehicleFramework.Util.ExplosionCreator;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class DeathHandler {
	ActiveVehicle vehicle;
	
	public DeathHandler(ActiveVehicle v) {
		vehicle = v;
	}
	
	public void explode(boolean remove) {
		if(vehicle.hasEffect(CustomAction.EXPLODE)) vehicle.playEffect(CustomAction.EXPLODE);
		DeathData explode = vehicle.getDeathData(VehicleDeath.EXPLODE);
		
		
		HitChecker checker = new HitChecker();
		vehicle.getAnimationHandler().animate(Animation.EXPLODE);
		BoundingBox boundingBox = vehicle.getEntity().getBoundingBox();
		ExplosionCreator.triggerExplosion(vehicle.getEntity().getLocation(), Math.min(5.0, boundingBox.getWidthX()), Math.min(16, boundingBox.getWidthX()*3), Math.min(20, boundingBox.getWidthX()*5), "ENTITY_EXPLOSION");
		for(int i = 0; i<50; i++) {
        	Vector velocity = new Vector(
        	        (Math.random() - 0.5) * 1, // Small random X movement
        	        (Math.random() - 0.5) * 0.1,   // Always upward Y movement
        	        (Math.random() - 0.5) * 1  // Small random Z movement
        	    );
        	for(Player p : vehicle.getNearbyPlayers()) {
        		p.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, vehicle.getEntity().getLocation().clone().add(0, 1.5, 0), 0, velocity.getX(), velocity.getY(), velocity.getZ(), 3);
        	}
        }
		for(SoundData sfx : explode.getSfx()) {
			sfx.playSound(vehicle.getNearbyPlayers(), vehicle.getEntity().getLocation(), 1f);
		}
		new BukkitRunnable() {
			int i = 0;
	        @Override
	        public void run() {
	        	if(i == 2) {
	        		this.cancel();
	        		return;
	        	}
	    		for(int i = 0; i<10; i++) {
	            	Vector velocity = new Vector(
	            	        (Math.random() - 0.5) * 0.5, // Small random X movement
	            	        Math.random() * 0.2 + 0.2,   // Always upward Y movement
	            	        (Math.random() - 0.5) * 0.5  // Small random Z movement
	            	    );
	            	for(Player p : vehicle.getNearbyPlayers()) {
	            		p.spawnParticle(Particle.EXPLOSION_HUGE, vehicle.getEntity().getLocation(), 0, velocity.getX(), velocity.getY(), velocity.getZ(), 0.2);
	            	}
	            }
	        	i++;
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 10L);
	    List<Entity> projectiles = new ArrayList<>();
		for (int i = 0; i < explode.getFragments(); i++) {
	        new Fragment(vehicle, vehicle.getEntity().getWorld(), vehicle.getEntity().getLocation(), projectiles);
	    }
		if(remove) {
			new BukkitRunnable() {
		        @Override
		        public void run() {
		        	vehicle.remove();
		        }
		    }.runTaskLater(VehicleFramework.plugin, explode.getDuration()*1L);
		}
	}
	
	public void crash() {
		if(vehicle.hasEffect(CustomAction.CRASH)) vehicle.playEffect(CustomAction.CRASH);
	    vehicle.getAnimationHandler().animate(Animation.CRASH);

	    new BukkitRunnable() {
	        int i = 0;

	        @Override
	        public void run() {
	            if (i > 600) {
	                vehicle.remove();
	                this.cancel();
	                return;
	            }

	            // Get the bounding box of the vehicle and expand it by 1 block in all directions
	            BoundingBox boundingBox = vehicle.getEntity().getBoundingBox().clone().expand(1, 1, 1);

	            // Iterate through all blocks within the expanded bounding box
	            boolean hitSomething = false;
	            for (int x = (int) Math.floor(boundingBox.getMinX()); x <= (int) Math.ceil(boundingBox.getMaxX()); x++) {
	                for (int y = (int) Math.floor(boundingBox.getMinY()); y <= (int) Math.ceil(boundingBox.getMaxY()); y++) {
	                    for (int z = (int) Math.floor(boundingBox.getMinZ()); z <= (int) Math.ceil(boundingBox.getMaxZ()); z++) {
	                        Block block = vehicle.getEntity().getWorld().getBlockAt(x, y, z);
	                        if (!(block.getType().equals(Material.AIR) || block.getType().equals(Material.LIGHT))) {
	                            hitSomething = true;
	                            break;
	                        }
	                    }
	                    if (hitSomething) break;
	                }
	                if (hitSomething) break;
	            }

	            if (hitSomething) {
	                explode(false);
	                vehicle.remove();
	                this.cancel();
	            }

	            i++;
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}
	public void sink() {
		if(vehicle.hasEffect(CustomAction.SINK)) vehicle.playEffect(CustomAction.SINK);
	    vehicle.getAnimationHandler().animate(Animation.SINK);
	    DeathData sink = vehicle.getDeathData(VehicleDeath.SINK);
	    if(sink == null) {
	    	vehicle.remove();
	    	return;
	    }
	    for(SoundData sfx : sink.getSfx()) {
			sfx.playSound(vehicle.getNearbyPlayers(), vehicle.getEntity().getLocation(), 1f);
		}
	    new BukkitRunnable() {
	        @Override
	        public void run() {
	        	vehicle.remove();
	        }
	    }.runTaskLater(VehicleFramework.plugin, sink.getDuration()*1L);
	}

	public void die() {
		if(vehicle.hasEffect(CustomAction.DIE)) vehicle.playEffect(CustomAction.DIE);
	    vehicle.getAnimationHandler().animate(Animation.DIE);
	    DeathData data = vehicle.getDeathData(VehicleDeath.DIE);
	    if(data == null) {
	    	vehicle.remove();
	    	return;
	    }
	    for(SoundData sfx : data.getSfx()) {
			sfx.playSound(vehicle.getNearbyPlayers(), vehicle.getEntity().getLocation(), 1f);
		}
	    new BukkitRunnable() {
	        @Override
	        public void run() {
	        	vehicle.remove();
	        }
	    }.runTaskLater(VehicleFramework.plugin, data.getDuration()*1L);
	}
}
