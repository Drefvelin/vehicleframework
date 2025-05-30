package net.tfminecraft.VehicleFramework.Managers;

import java.util.List;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.Plugins.TLibs.Enums.NSEW;
import me.Plugins.TLibs.Utils.LocationUtil;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Permissions.Permissions;
import net.tfminecraft.VehicleFramework.Util.LocationChecker;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "vf";

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase(cmd1)) {
			if(Permissions.canSpawn(sender) == false) {
				sender.sendMessage("§cYou do not have access to this command!");
				return true;
			}
			if(args[0].equalsIgnoreCase("spawn") && args.length == 2) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				String vehicle = args[1];
				VehicleFramework.getVehicleManager().spawn(p.getLocation(), vehicle);
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
