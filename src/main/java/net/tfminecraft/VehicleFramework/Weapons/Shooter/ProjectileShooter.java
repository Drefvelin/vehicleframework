package net.tfminecraft.VehicleFramework.Weapons.Shooter;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Interface.Shooter;
import net.tfminecraft.VehicleFramework.Util.ExplosionCreator;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.ClusterBomb;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class ProjectileShooter implements Shooter {
	private DefaultShooter defaultShooter = new DefaultShooter(this);
	private BulletShooter bulletShooter = new BulletShooter(this);
	private TorpedoShooter torpedoShooter = new TorpedoShooter(this);
	private BombShooter bombShooter = new BombShooter(this);
	
	@Override
	public void shoot(List<Player> players, Entity e, Location loc, Vector vector, Ammunition a, ActiveWeapon w) {
		loc.setDirection(vector);
		switch(a.getType()) {
			case BULLET:
				bulletShooter.shoot(players, e, loc, vector, a, w);
				break;
			case CANNONBALL:
				defaultShooter.shoot(players, e, loc, vector, a, w);
				break;
			case CLUSTER:
				defaultShooter.shoot(players, e, loc, vector, a, w);
				break;
			case TORPEDO:
				torpedoShooter.shoot(players, e, loc, vector, a, w);
				break;
			case BOMB:
				bombShooter.shoot(players, e, loc, vector, a, w);
				break;
			default:
				break;	
		}
	}
	
	public void lightEffect(Location loc) {
		if(!loc.getBlock().getType().equals(Material.AIR)) return;
		Block b = loc.getBlock();
		b.setType(Material.LIGHT);
		final Levelled level = (Levelled) b.getBlockData();
		level.setLevel(10);
		b.setBlockData(level, true);
		new BukkitRunnable()
		{
			Integer i = 0;
			public void run()
			   {
				if(i == 1) {
					level.setLevel(15);
					b.setBlockData(level, true);
				} else if(i == 2){
					level.setLevel(11);
					b.setBlockData(level, true);
				} else if(i == 3) {
					loc.getBlock().setType(Material.AIR);
					this.cancel();
				}
				i++;
			   }
		}.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}
	
	public void triggerExplosion(List<Player> players, Location explosionCenter, AmmunitionData a, ActiveWeapon w) {
		if (Weapon.effectiveExplosive(w, a)) {
			explosionCenter.getWorld().playSound(explosionCenter, Sound.ENTITY_GENERIC_EXPLODE, 8, 1);
	    	ExplosionCreator.triggerExplosion(
	    			explosionCenter,
	    			Weapon.effectiveYield(w, a),
	    			Weapon.effectiveRadius(w, a),
	    			Weapon.effectiveDamage(w, a),
	    			Weapon.effectiveDamageType(w, a),
	    			a.isFire()
	    	);
		}

		if (!a.getPotionEffects().isEmpty()) {
			for (AmmunitionData.LingeringCloudData cloudData : a.getLingeringClouds()) {
				spawnLingeringCloud(explosionCenter, cloudData, Weapon.effectiveRadius(w, a));
			}
		}

		a.hitFX(players, explosionCenter, 1f);
	}

	private void spawnLingeringCloud(Location loc, AmmunitionData.LingeringCloudData data, int radius) {
		AreaEffectCloud cloud = loc.getWorld().spawn(loc, AreaEffectCloud.class);

		cloud.setRadius(Math.max(1.5f, radius / 2.0f));
		cloud.setDuration(data.getCloudDurationTicks());
		cloud.setColor(data.getColor());
		cloud.setWaitTime(0);
		cloud.setReapplicationDelay(20);

		// Optional: slowly shrink over lifetime
		float startRadius = Math.max(1.5f, radius / 2.0f);
		cloud.setRadius(startRadius);
		cloud.setRadiusPerTick(-startRadius / Math.max(1, data.getCloudDurationTicks()));

		cloud.addCustomEffect(data.getEffect(), true);
	}
}
