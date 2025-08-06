package net.tfminecraft.VehicleFramework.Managers;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Enums.NSEW;
import me.Plugins.TLibs.Objects.API.SubAPI.ItemCreator;
import me.Plugins.TLibs.Utils.LocationUtil;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Permissions.Permissions;
import net.tfminecraft.VehicleFramework.Util.EnumDisplayConverter;
import net.tfminecraft.VehicleFramework.Util.LocationChecker;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "vf";

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase(cmd1)) {
			if(args[0].equalsIgnoreCase("keybinds") && args.length == 1) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				ActiveVehicle v = VehicleFramework.getVehicleManager().getByPassenger(p);
				if(v == null) {
					p.sendMessage("§cYou are not in a vehicle");
					return false;
				}
				p.sendMessage("§c======================================");
				p.sendMessage("§bKeybinds for state: §a" + WordUtils.capitalize(v.getCurrentState().getType().toString().toLowerCase()));
				for (Map.Entry<Keybind, Input> entry : v.getCurrentState().getInputHandler().getMappings().entrySet()) {
					if (entry.getValue().equals(Input.NONE)) continue;
					
					String keybindName = EnumDisplayConverter.getKeybindDisplayName(entry.getKey());
					String inputName = EnumDisplayConverter.getInputDisplayName(entry.getValue());

					p.sendMessage("§e" + keybindName + " §f-> §a" + inputName);
				}
				p.sendMessage("§c======================================");
				return true;
			}
			if(Permissions.canSpawn(sender) == false) {
				sender.sendMessage("§cYou do not have access to this command!");
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
			if(args[0].equalsIgnoreCase("tracktest") && args.length == 1) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				Entity stand = p.getWorld().spawnEntity(p.getLocation(), EntityType.ARMOR_STAND);
				new BukkitRunnable() {
					int i = 0;
			        @Override
			        public void run() {
			        	if(i > 60 || stand.isDead()) {
			        		cancel();
			        		stand.remove();
			        	}
			        	Location loc = stand.getLocation();
			        	Vector direction = stand.getLocation().getDirection();
			        	List<NSEW> dirs = LocationUtil.getProbableDirection(direction, false);
			        	p.sendMessage(dirs.toString());
			        	Block b = loc.getBlock();
			        	if(b.getBlockData() instanceof Rail) {
			        		Rail rail = (Rail) b.getBlockData();
			        		p.sendMessage("Shape: "+rail.getShape().toString());
			        	}
			        	loc = LocationChecker.getNextTrackedLocation(loc, dirs, loc.getYaw());
			        	stand.teleport(loc);
			            i++;
			        }
			    }.runTaskTimer(VehicleFramework.plugin, 0L, 20L);
			}
			if(args[0].equalsIgnoreCase("trackcheck") && args.length == 1) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				Block b = p.getLocation().getBlock();
	        	if(b.getBlockData() instanceof Rail) {
	        		Rail rail = (Rail) b.getBlockData();
	        		p.sendMessage("Shape: "+rail.getShape().toString());
	        	}
			}
		}
		return false;
	}
}
