package net.tfminecraft.VehicleFramework.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

public class Cache {
	public static List<Material> ignoreExplode = new ArrayList<Material>();
	public static List<Material> ignoreLands = new ArrayList<Material>();

	public static Set<Entity> projectiles = new HashSet<>();
	
	public static HashMap<Material, Material> convertExplode = new HashMap<>();
	
	public static int despawnDistance;

	public static boolean blockDamage;
	
	public static Set<Location> lightLocations = new HashSet<>();

	public static String skinItem;
	public static String repairItem;
	public static String destroyItem;

	public static boolean enableLogging;
	public static String mythicMob;

	//Plugins
	public static boolean coreProtect = false;
	
	public static void removeProjectiles() {
		for(Entity e : projectiles) {
			if(e == null) continue;
			e.remove();
		}
	}
	
	public static void removeLights() {
		for(Location loc : lightLocations) {
			if(loc.getBlock().getType().equals(Material.LIGHT)) {
				loc.getBlock().setType(Material.AIR);
			}
		}
	}
}
