package net.tfminecraft.VehicleFramework.Database;

import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;

public class IncompleteWeapon {
	private String id;
	
	private double damage;
	
	private Ammunition ammo; //TODO turn this back to ammunition class once ported
	private int count = 0;

	public IncompleteWeapon(String id, double damage, String ammo, int count) {
		this.id = id;
		this.damage = damage;
		this.ammo = AmmunitionLoader.getByString(ammo);
		this.count = count;
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

	public int getCount() {
		return count;
	}
	
	public Ammunition getAmmo() {
		return ammo;
	}
}
