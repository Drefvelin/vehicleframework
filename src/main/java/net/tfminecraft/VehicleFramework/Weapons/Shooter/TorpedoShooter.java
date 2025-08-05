package net.tfminecraft.VehicleFramework.Weapons.Shooter;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
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
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.FusedExplosive;

public class TorpedoShooter implements Shooter {
	private HitChecker checker = new HitChecker();
	private ProjectileShooter shooter;
	
	public TorpedoShooter(ProjectileShooter p) {
		shooter = p;
	}

	@Override
	public void shoot(List<Player> players, Entity o, Location loc, Vector vector, Ammunition ammo, ActiveWeapon w) {
		FusedExplosive a = (FusedExplosive) ammo;

	    Vector direction = vector.clone();
	    List<Entity> projectiles = new ArrayList<>();
		final Entity torpedo = a.getData().spawn(loc);
        projectiles.add(torpedo);
		Cache.projectiles.add(torpedo);
	    
        shooter.lightEffect(loc);

        torpedo.setVelocity(direction.clone().multiply(w.getWeaponData().getVelocity()));
	    
	    new BukkitRunnable() {
	    	int i = 0;
	        public void run() {   
	        	i++;
	        	if (i > 10 && (checker.getHitEntities(torpedo, projectiles, 2.0).size() > 0 || checker.hasHitIgnoreWater(torpedo, projectiles) || torpedo.isOnGround() || torpedo.isDead() || i > a.getFuse())) {
	        		shooter.triggerExplosion(torpedo.getLocation(), a.getData());
	        		torpedo.remove();
	                projectiles.remove(torpedo);
					Cache.projectiles.remove(torpedo);
	                cancel(); 
	            }
	        	Location currentLocation = torpedo.getLocation();
	        	Block currentBlockShifted = currentLocation.clone().add(0, 0.5, 0).getBlock();
	        	if(currentBlockShifted.getType() == Material.WATER || (currentBlockShifted.getBlockData() instanceof Waterlogged && ((Waterlogged) currentBlockShifted.getBlockData()).isWaterlogged())){
                	Vector noY = torpedo.getVelocity();
                	noY.setY(0.0);
                	torpedo.setVelocity(noY);
                }

                // Check if the torpedo is in water or waterlogged
                Block currentBlock = currentLocation.getBlock();
                if (currentBlock.getType() == Material.WATER || (currentBlock.getBlockData() instanceof Waterlogged && ((Waterlogged) currentBlock.getBlockData()).isWaterlogged())) {
                    Vector forwardVelocity = currentLocation.getDirection().normalize().multiply(0.4); // Adjust the value as needed
                    forwardVelocity.setY(0.0);
                    torpedo.setVelocity(torpedo.getVelocity().add(forwardVelocity));
                    a.getData().fx(players, currentLocation, 1f, i);
                }
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}

}
