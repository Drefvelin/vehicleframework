package net.tfminecraft.VehicleFramework.Managers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Objects.API.SubAPI.ItemCreator;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.Util.Text;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Permissions.Permissions;
import net.tfminecraft.VehicleFramework.Tracks.TrackCommands;
import net.tfminecraft.VehicleFramework.Util.EnumDisplayConverter;
import net.tfminecraft.VehicleFramework.Data.OwnedVehicleSummary;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "vf";

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase(cmd1)) {
			if(args.length >= 1 && args[0].equalsIgnoreCase("track")) {
				return TrackCommands.handle(sender, args);
			}
			if(args[0].equalsIgnoreCase("keybinds") && args.length == 1) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				ActiveVehicle v = VehicleFramework.getVehicleManager().getByPassenger(p);
				if(v == null) {
					p.sendMessage("§cYou are not in a vehicle");
					return false;
				}
				p.sendMessage("§c======================================");
				p.sendMessage("§bKeybinds for state: §a" + Text.capitalize(v.getCurrentState().getType().toString().toLowerCase()));
				for (Map.Entry<Keybind, Input> entry : v.getCurrentState().getInputHandler().getMappings().entrySet()) {
					if (entry.getValue().equals(Input.NONE)) continue;
					
					String keybindName = EnumDisplayConverter.getKeybindDisplayName(entry.getKey());
					String inputName = EnumDisplayConverter.getInputDisplayName(entry.getValue());

					p.sendMessage("§e" + keybindName + " §f-> §a" + inputName);
				}
				p.sendMessage("§c======================================");
				return true;
			}
			if(args.length >= 1 && args[0].equalsIgnoreCase("findvehicles")) {
				if(!(sender instanceof Player)) {
					sender.sendMessage("§cOnly players can use this command.");
					return true;
				}
				Player p = (Player) sender;
				String owner = "player_" + p.getName();
				List<OwnedVehicleSummary> owned =
						VehicleFramework.getVehicleManager().listOwnedVehicles(owner);
				if(owned.isEmpty()) {
					p.sendMessage("§7You do not own any vehicles.");
					return true;
				}
				p.sendMessage("§bYour vehicles:");
				for(OwnedVehicleSummary vehicle : owned) {
					p.sendMessage("§e" + vehicle.getName() + " §7- " + formatVehicleLocation(vehicle.getLocation()));
				}
				return true;
			}
			if(!Permissions.canSpawn(sender)) {
				sender.sendMessage("§cYou do not have access to this command!");
				return true;
			}
			if(args[0].equalsIgnoreCase("ammo") && args.length == 1) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				ItemCreator creator = TLibs.getItemAPI().getCreator();
				for(Ammunition ammo : AmmunitionLoader.get().values()) {
					ItemStack item = creator.getItemFromPath(ammo.getData().getInput());
					item.setAmount(64);
					p.getInventory().addItem(item);
				}
				VFLogger.message(p, "§aGiving Ammo");
				return true;
			}
			if (args[0].equalsIgnoreCase("kill") && args.length == 2) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("Only players can use this command.");
					return true;
				}

				Player p = (Player) sender;

				int radius;
				try {
					radius = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					p.sendMessage("§cPlease enter a valid number for the radius.");
					return true;
				}

				Location loc = p.getLocation();

				int count = VehicleFramework.getVehicleManager().kill(p, loc, radius);

				VFLogger.message(p, "§aKilled " + count + " entities within " + radius + " blocks.");
				return true;
			}
			if(args[0].equalsIgnoreCase("spawn") && args.length == 2) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				String vehicle = args[1];
				VehicleFramework.getVehicleManager().spawn(p.getLocation(), vehicle);
				return true;
			}
			if(!Permissions.isAdmin(sender)) {
				sender.sendMessage("§cYou do not have access to this command!");
				return true;
			}
			if(args[0].equalsIgnoreCase("takeover") && args.length == 1) {
				if(!(sender instanceof Player)) {
					sender.sendMessage("§cOnly players can use this command.");
					return true;
				}
				Player p = (Player) sender;
				VehicleFramework.getVehicleManager().startTakeover(p);
				return true;
			}
			if(args[0].equalsIgnoreCase("reload") && args.length == 1) {
				Player p = null;
				if(sender instanceof Player) p = (Player) sender;
				if(p != null) VFLogger.message(p, "Reloading...");
				VehicleFramework.getInstance().reload();
				if(p != null) VFLogger.message(p, "Reload complete!");
				return true;
			}
		}
		return false;
	}

	private static String formatVehicleLocation(Optional<Location> location) {
		if(location == null || location.isEmpty()) {
			return "§7location unknown (stored)";
		}
		Location loc = location.get();
		if(loc.getWorld() == null) {
			return "§7location unknown (stored)";
		}
		return String.format(
				"§f%s %.0f, %.0f, %.0f",
				loc.getWorld().getName(),
				loc.getX(),
				loc.getY(),
				loc.getZ());
	}
}
