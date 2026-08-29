package net.tfminecraft.VehicleFramework.Util;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;

public class LocationChecker {
	private static List<Material> water = Arrays.asList(Material.WATER, Material.KELP, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS);
	private static List<Material> air = Arrays.asList(Material.AIR, Material.LIGHT);
	
	public static boolean isInWater(Location loc) {
		if(water.contains(loc.getBlock().getType())) return true;
		return false;
	}
	
	public static boolean isInAir(Location loc) {
		if(air.contains(loc.getBlock().getType()) || (loc.getBlock().isPassable() && !water.contains(loc.getBlock().getType()))) return true;
		return false;
	}
	
	public static boolean isOnGround(Location loc) {
		if(!isInWater(loc) && !isInAir(loc)) return true;
		return false;
	}
}
