package net.tfminecraft.VehicleFramework.Util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Cache.Cache;

public class LightEffect {

    public void createTemporaryLight(Location loc, int lightLevel) {
    	if(!loc.getBlock().getType().equals(Material.AIR)) return;
    	if(Cache.lightLocations.contains(loc)) return;
    	Cache.lightLocations.add(loc);
		Block b = loc.getBlock();
		b.setType(Material.LIGHT);
		final Levelled level = (Levelled) b.getBlockData();
		level.setLevel(lightLevel);
		b.setBlockData(level, true);
		new BukkitRunnable()
		{
			public void run() {
				b.setType(Material.AIR);
				Cache.lightLocations.remove(loc);
			}
		}.runTaskLater(VehicleFramework.plugin, 2L);
    }
}
