package net.tfminecraft.VehicleFramework.Util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Cache.Cache;

public class SpawnLocation {
	private Chunk chunk;
	private Location loc;
	private String file;
	
	public SpawnLocation(Chunk c, Location l, String f) {
		chunk = c;
		loc = l;
		file = f;
	}

	public Chunk getChunk() {
		return chunk;
	}

	public Location getLoc() {
		return loc;
	}

	public String getFile() {
		return file;
	}

	public boolean hasNearby() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(!p.getWorld().equals(loc.getWorld())) continue;
			if(p.getLocation().distanceSquared(loc) > Cache.despawnDistance*0.8) continue;
			return true;
		}
		return false;
	}
}
