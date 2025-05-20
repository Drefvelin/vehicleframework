package net.tfminecraft.VehicleFramework.Interface;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;

public interface Shooter {
	public void shoot(List<Player> players, Entity e, Location loc, Vector vector, Ammunition a, ActiveWeapon w);
}
