package net.tfminecraft.VehicleFramework.Weapons;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class WeaponEntityFilters {

	private WeaponEntityFilters() {
	}

	public static Set<Entity> buildShooterIgnoreSet(Player player, Entity vehicleRoot, ActiveVehicle shooterVehicle) {
		Set<Entity> ignore = new HashSet<>();
		if (player != null) {
			ignore.add(player);
		}
		if (vehicleRoot != null) {
			ignore.add(vehicleRoot);
		}
		if (shooterVehicle != null) {
			for (Entity passenger : shooterVehicle.getSeatHandler().getPassengers()) {
				ignore.add(passenger);
			}
		}
		return ignore;
	}
}
