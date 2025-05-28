package net.tfminecraft.VehicleFramework.Database;

import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;

public class IncompleteWeapon {
	private String id;
	
	private double damage;
	
	private Ammunition ammo; //TODO turn this back to ammunition class once ported

	public IncompleteWeapon(String id, double damage, String ammo) {
		this.id = id;
		this.damage = damage;
		this.ammo = AmmunitionLoader.getByString(ammo);
	}
	
	public String getId() {
		return id;
	}
	
	public double getDamage() {
		return damage;
	}
	
	public boolean hasAmmo() {
		return ammo != null;
	}
	
	public Ammunition getAmmo() {
		return ammo;
	}
}
