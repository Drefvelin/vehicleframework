package net.tfminecraft.VehicleFramework.Permissions;

import org.bukkit.command.CommandSender;

public class Permissions
{
    public static String Permission_Spawn;
    public static String Permission_Admin;
    
    static {
        Permissions.Permission_Spawn = "vehicleframework.spawn";
        Permissions.Permission_Admin = "vf.admin";
    }
    
    public static boolean canSpawn(final CommandSender commandSender) {
        return commandSender.hasPermission(Permissions.Permission_Spawn) || commandSender.hasPermission(Permissions.Permission_Admin);
    }

    public static boolean isAdmin(final CommandSender commandSender) {
        return commandSender.hasPermission(Permissions.Permission_Admin);
    }
}