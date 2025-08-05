package net.tfminecraft.VehicleFramework.Weapons.Shooter;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Interface.Shooter;
import net.tfminecraft.VehicleFramework.Projectiles.HitChecker;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.ClusterBomb;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class DefaultShooter implements Shooter {
	//Regular shooting method for things like cannonballs, explode on impact
	
	private HitChecker checker = new HitChecker();
	private ProjectileShooter shooter;
	
	public DefaultShooter(ProjectileShooter p) {
		shooter = p;
	}

	@Override
	public void shoot(List<Player> players, Entity o, Location loc, Vector vector, Ammunition a, ActiveWeapon w) {
		AmmunitionData ammoData = a.getData();
	    List<Entity> projectiles = new ArrayList<>();
	    
	    shooter.lightEffect(loc);
	    
	    Entity e = ammoData.spawn(loc);
		Cache.projectiles.add(e);
	    
	    Vector velocity = vector.clone().multiply(w.getWeaponData().getVelocity());
	    new BukkitRunnable() {
	        int i = 0;
	        float pitch = 1.2f;
	        
	        // Track the current position of the entity
	        Location currentLocation = loc.clone();
	        
	        public void run() {   
	            i++;

	            // Apply gravity (decrease Y component of velocity)
	            velocity.setY(velocity.getY() - 0.049);  // Gravity affects the Y component

	            // Update the location by adding the velocity (vector + gravity)
	            currentLocation.add(velocity.getX(), velocity.getY(), velocity.getZ());
	            
	            // Teleport the entity to the new calculated location
	            e.teleport(currentLocation);

	            // Check if the projectile has hit something or reached the ground
	            if (i > 5 && (checker.hasHit(e, projectiles) || e.isOnGround() || e.isDead())) {
	                shooter.triggerExplosion(e.getLocation(), a.getData());
	                e.remove();
	                projectiles.remove(e);
					Cache.projectiles.remove(e);
	                cancel(); 
	            }

	            // Add the offset for visual effects
	            Location visualLocation = currentLocation.clone().add(0, ammoData.getOffset(), 0);

	            // Play a sound effect as the projectile flies through the air
	            if (i > 20) {
	                pitch = pitch - 0.015f;
	                if (pitch < 0.3f) {
	                    pitch = 0.3f;
	                }
	            }
	            a.getData().fx(players, visualLocation, pitch, i);
	            if(a instanceof ClusterBomb) {
	            	ClusterBomb c = (ClusterBomb) a;
	            	if(i >= c.getFuse()) {
	            		for(Player p : players) {
	        				p.spawnParticle(Particle.EXPLOSION_HUGE, e.getLocation(), (int) Math.round(a.getData().getYield()*15), 0, 0, 0, 0);
	        			}
	        			e.getLocation().getWorld().createExplosion(e.getLocation(), a.getData().getYield(), false, true);
	            		sendCluster(e.getLocation(), c, players, projectiles);
	            		e.remove();
		                projectiles.remove(e);
						Cache.projectiles.remove(e);
		                cancel(); 
	            	}
	            }
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}
	
	private void sendCluster(Location original, ClusterBomb a, List<Player> players, List<Entity> projectiles) {
	    World world = original.getWorld();
	    if (world == null) return;
	    for (int i = 0; i < a.getAmount(); i++) {
	        Entity armorStand = a.getClusterData().spawn(original);
	        projectiles.add(armorStand);
			Cache.projectiles.add(armorStand);

	        Vector velocity = new Vector(
	            (Math.random() - 0.5) * a.getSpread(),
	            -3.5, // downward velocity
	            (Math.random() - 0.5) * a.getSpread()
	        );
	        armorStand.setVelocity(velocity);

	        // Handle explosion on ground impact
	        new BukkitRunnable() {
	        	int i = 5;
	            public void run() {
	                if (i > 5 && checker.hasHit(armorStand, projectiles) || armorStand.isOnGround() || armorStand.isDead()) {
	                    shooter.triggerExplosion(armorStand.getLocation(), a.getClusterData()); // Adjust ammunition if needed
	                    armorStand.remove();
						Cache.projectiles.remove(armorStand);
	                    cancel();
	                }
	                a.getData().fx(players, armorStand.getLocation(), 1f, i);
	                i++;
	            }
	        }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	    }
	}

}
