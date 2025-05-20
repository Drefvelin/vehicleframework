package net.tfminecraft.VehicleFramework.Database;

import org.bukkit.Location;

public class IncompleteWeapon {
	private String id;
	
	private double damage;
	
	private String bone;
	
	private Location loc;
	
	private String ammo; //TODO turn this back to ammunition class once ported

	public IncompleteWeapon(String id, double damage, String bone, Location loc, String ammo) {
		this.id = id;
		this.damage = damage;
		this.bone = bone;
		this.loc = loc;
		this.ammo = ammo;
	}
	
	public String getId() {
		return id;
	}
	
	public double getDamage() {
		return damage;
	}
	
	public boolean hasBone() {
		return bone != null;
	}
	public String getBone() {
		return bone;
	}

	public Location getLoc() {
		return loc;
	}
	
	public boolean hasAmmo() {
		return ammo != null;
	}
	
	public String getAmmo() {
		return ammo;
	}
}
