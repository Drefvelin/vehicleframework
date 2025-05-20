package net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.Projectile;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface ProjectileModel {
	
	public Entity spawn(Location loc);
	public double getOffset();
}
