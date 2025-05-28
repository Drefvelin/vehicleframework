package net.tfminecraft.VehicleFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VFLogger {
	/**
	 *
     * @param error The error message to be displayed in the console.
     */
	public static void log(String error) {
		Bukkit.getLogger().warning("[VehicleFramework] Error! " + error);
	}

	public static void info(String info) {
		Bukkit.getLogger().info("[VehicleFramework] " + info);
	}

	private static List<String> creators = new ArrayList<>(Arrays.asList("drefvelin"));
	public static void creatorLog(String info) {
		for(String c : creators) {
			Player p = Bukkit.getPlayerExact(c);
			if(p != null && p.isOnline()) p.sendMessage("§a[VFLogger]§e: §b"+info);
		}
	}
}
