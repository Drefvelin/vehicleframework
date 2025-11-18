package net.tfminecraft.VehicleFramework.Weapons.Shooter;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Interface.Shooter;
import net.tfminecraft.VehicleFramework.Projectiles.HitChecker;
import net.tfminecraft.VehicleFramework.Util.ExplosionCreator;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Bullet;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class BulletShooter implements Shooter {
	private HitChecker checker = new HitChecker();
	
	private ProjectileShooter shooter;
	
	public BulletShooter(ProjectileShooter p) {
		shooter = p;
	}

	@Override
	public void shoot(List<Player> players, Entity o, Location loc, Vector vector, Ammunition a, ActiveWeapon w) {
	    AmmunitionData ammoData = a.getData();
	    Bullet b = (Bullet) a;
	    Vector direction = vector.clone().setY(vector.getY() - 0.01);
	    Location startLocation = loc.clone();
	    
	    shooter.lightEffect(startLocation);

	    new BukkitRunnable() {
	        double traveledDistance = 0;

	        @Override
	        public void run() {
	            Location particle = startLocation.clone().add(direction.clone().multiply(traveledDistance));

	            if (traveledDistance > b.getRange()) {
	                cancel();
	                triggerExplosionIfExplosive(particle, b);
	                return;
	            }

	            for (int i = 0; i < 10; i++) {
                    ammoData.fx(players, particle, 1f, i);
                    particle.add(direction.clone().multiply(0.5));

                    if (traveledDistance > 5.0) {
                        if (handleEntityHit(particle, b, ammoData)) {
                            cancel();
                            return;
                        }
                    }
                }
	            traveledDistance += 5.0;
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}

	private boolean handleEntityHit(Location particle, Bullet b, AmmunitionData ammoData) {
	    if (checker.hasHitLocation(particle)) {
	        triggerExplosionIfExplosive(particle, b);
	        return true;
	    }

	    for (Entity entity : particle.getWorld().getNearbyEntities(particle, 0.5, 0.5, 0.5)) {
	        if (entity instanceof LivingEntity) {
	            if (b.isExplosive()) {
	                triggerExplosionIfExplosive(particle, b);
	            } else {
	                ExplosionCreator.applyDamage(entity, ammoData.getDamage(), ammoData.getDamageType());
	            }
	            return true;
	        }
	    }
	    return false;
	}

	private void triggerExplosionIfExplosive(Location loc, Bullet b) {
	    if (b.isExplosive()) {
			loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 8, 1);
	        ExplosionCreator.triggerExplosion(loc, b.getData().getYield(), b.getData().getRadius(), b.getData().getDamage(), b.getData().getDamageType());
	    }
	}

}
