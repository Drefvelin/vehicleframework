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

public class TabCompletion implements TabCompleter{
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("vf") && args.length >= 0 && args.length < 2){
            if(sender instanceof Player){
                List<String> completions = new ArrayList<>();
                completions.add("keybinds");
                if(Permissions.canSpawn(sender) == true) {
                    completions.add("spawn");
			    }
                return completions;
            }
        } else if(cmd.getName().equalsIgnoreCase("vf") && args.length >= 0 && args[0].equalsIgnoreCase("spawn")){
            if(sender instanceof Player){
            	List<String> completions = new ArrayList<String>();
                if(Permissions.canSpawn(sender) == false) {
                    return completions;
			    }
				for(Vehicle v : VehicleLoader.get().values()) {
					completions.add(v.getId());
				}
                
                return completions;
            }
        }
        return null;
    }

}
