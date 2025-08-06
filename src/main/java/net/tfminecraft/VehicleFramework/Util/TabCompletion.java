package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Permissions.Permissions;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class TabCompletion implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("vf")) {
            if (args.length == 1) {
                completions.add("keybinds");
                if (Permissions.canSpawn(sender)) {
                    completions.add("spawn");
                    completions.add("reload");
                    completions.add("kill");
                }
                return completions;
            }

            if (args[0].equalsIgnoreCase("spawn") && args.length == 2) {
                if (!Permissions.canSpawn(sender)) return completions;
                for (Vehicle v : VehicleLoader.get().values()) {
                    completions.add(v.getId());
                }
                return completions;
            }

            if (args[0].equalsIgnoreCase("kill") && args.length == 2) {
                completions.add("10");  // Suggest a default radius
                completions.add("20");
                completions.add("50");
                return completions;
            }
        }

        return null;
    }
}
