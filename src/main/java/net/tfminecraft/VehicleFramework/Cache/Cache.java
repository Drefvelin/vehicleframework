package net.tfminecraft.VehicleFramework.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;

public class Cache {
	public static List<Material> ignoreExplode = new ArrayList<Material>();
	public static List<Material> ignoreLands = new ArrayList<Material>();
	
	public static HashMap<Material, Material> convertExplode = new HashMap<>();
	
	public static int despawnDistance;

	public static boolean blockDamage;
	
	public static Set<Location> lightLocations = new HashSet<>();

	public static String skinItem;
	public static String repairItem;

	//Plugins
	public static boolean coreProtect = false;
	
	
	public static void removeLights() {
		for(Location loc : lightLocations) {
			if(loc.getBlock().getType().equals(Material.LIGHT)) {
				loc.getBlock().setType(Material.AIR);
			}
		}
	}
}
