package net.tfminecraft.VehicleFramework.Permissions;

import org.bukkit.command.CommandSender;

public class Permissions
{
    public static String Permission_Spawn;
    
    static {
        Permissions.Permission_Spawn = "vehicleframework.spawn";
    }
    
    public static boolean canSpawn(final CommandSender commandSender) {
        return commandSender.hasPermission(Permissions.Permission_Spawn);
    }
}