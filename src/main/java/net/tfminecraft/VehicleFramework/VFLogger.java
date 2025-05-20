package net.tfminecraft.VehicleFramework;

import org.bukkit.Bukkit;

public class VFLogger {
	/**
	 *
     * @param error The error message to be displayed in the console.
     */
	public static void log(String error) {
		Bukkit.getLogger().info("§c[VehicleFramework]: Error! "+error);
	}
}
