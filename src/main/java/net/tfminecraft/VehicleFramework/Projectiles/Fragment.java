package net.tfminecraft.VehicleFramework.Projectiles;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Fragment {
	private HitChecker checker = new HitChecker();
	
	public Fragment(ActiveVehicle vehicle, World world, Location loc, List<Entity> projectiles) {
		ArmorStand armorStand = world.spawn(loc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(true); // Make sure they don't fall down
            stand.setSmall(true); // Makes them smaller, looks more like particles in motion
        });
        projectiles.add(armorStand);

        // Apply a random velocity to the armor stand
        Vector velocity = new Vector(
            (Math.random() - 0.5) * 6.0,  // Random horizontal movement
            Math.random() * 0.5 + 1.5,    // Upward movement (ensure it's positive)
            (Math.random() - 0.5) * 6.0   // Random horizontal movement
        );

        // Set the velocity
        armorStand.setVelocity(velocity);

        // Add particle trails to the armor stands
        new BukkitRunnable() {
        	int i = 0;
            @Override
            public void run() {
            	List<Entity> hits = checker.getHitEntities(armorStand, projectiles, 0.5);
            	if(hits.size() > 0) {
            		for(Entity e : hits) {
            			if(vehicle.getVehicleManager().get(e) != null) {
            				ActiveVehicle v = vehicle.getVehicleManager().get(e);
            				v.randomFire();
            			}
            		}
            	}
                if (armorStand.isDead() || armorStand.isOnGround() || (i > 10 && checker.hasHit(armorStand, projectiles))) {
                	armorStand.remove();
                    cancel();
                } else {
                    for (Player p : vehicle.getNearbyPlayers()) {
                        p.spawnParticle(Particle.FLAME, armorStand.getLocation(), 0, 0, 0, 0, 0.2);
                        p.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, armorStand.getLocation(), 0, 0, 0, 0, 0.2);
                    }
                }
                i++;
            }
        }.runTaskTimer(VehicleFramework.plugin, 0L, 1L); // Run every tick to simulate the trail
	}
}
