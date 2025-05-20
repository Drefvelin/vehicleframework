package net.tfminecraft.VehicleFramework.Weapons.Shooter;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Interface.Shooter;
import net.tfminecraft.VehicleFramework.Projectiles.HitChecker;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.FusedExplosive;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class BombShooter implements Shooter {
	private HitChecker checker = new HitChecker();
	private ProjectileShooter shooter;
	
	public BombShooter(ProjectileShooter p) {
		shooter = p;
	}
	
	@Override
	public void shoot(List<Player> players, Entity o, Location loc, Vector vector, Ammunition ammo, ActiveWeapon w) {
		FusedExplosive a = (FusedExplosive) ammo;
		AmmunitionData ammoData = a.getData();

	    List<Entity> projectiles = new ArrayList<>();
		final Entity e = a.getData().spawn(loc);
        projectiles.add(e);

        Vector velocity = o.getVelocity().clone();
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
	            if (i > a.getFuse() && (checker.hasHit(e, projectiles) || e.isOnGround() || e.isDead())) {
	                shooter.triggerExplosion(e.getLocation(), a.getData());
	                e.remove();
	                projectiles.remove(e);
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
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}
}
