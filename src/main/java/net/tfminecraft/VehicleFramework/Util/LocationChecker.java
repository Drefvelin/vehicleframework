package net.tfminecraft.VehicleFramework.Util;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rail.Shape;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.Plugins.TLibs.Enums.NSEW;
import me.Plugins.TLibs.Utils.LocationUtil;

public class LocationChecker {
	private static List<Material> water = Arrays.asList(Material.WATER, Material.KELP, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS);
	private static List<Material> air = Arrays.asList(Material.AIR, Material.LIGHT);
	
	public static boolean isInWater(Location loc) {
		if(water.contains(loc.getBlock().getType())) return true;
		return false;
	}
	
	public static boolean isInAir(Location loc) {
		if(air.contains(loc.getBlock().getType()) || (loc.getBlock().isPassable() && !water.contains(loc.getBlock().getType()))) return true;
		return false;
	}
	
	public static boolean isOnGround(Location loc) {
		if(!isInWater(loc) && !isInAir(loc)) return true;
		return false;
	}
	
	public static Location getNextTrackedLocation(Location loc, List<NSEW> dirs, float yaw) {
	    Location align = loc.getBlock().getLocation();
	    Location next = new Location(align.getWorld(), align.getX(), align.getY(), align.getZ()).add(0.5, 0, 0.5);
	    
	    for (int i = 0; i < 4; i++) {
	    	
	        Block b = next.getBlock();
	        
	        //p.sendMessage("Current position: " + next.getX() + ", " + next.getY() + ", " + next.getZ());

	        if (!(b.getBlockData() instanceof Rail)) {
	            Location down = next.clone().add(0, -1, 0);
	            b = down.getBlock();
	            if (!(b.getBlockData() instanceof Rail)) {
	            	return null;
	            }
	            next = down;
	        }
	        
	        /*
	        // Visual feedback (particles and sound)
	        next.getWorld().spawnParticle(
	            Particle.BLOCK_DUST, 
	            next.getX(), next.getY(), next.getZ(),
	            10, 0.1, 0.1, 0.1, 0,
	            Material.STONE.createBlockData()
	        );

	        next.getWorld().playSound(
	            next, Sound.BLOCK_STONE_BREAK, 1f, 1f
	        );
	        */

	        Rail rail = (Rail) b.getBlockData();
	        Shape shape = rail.getShape();

	        // Update the next location
	        next = getTrackedLocation(next, dirs, shape, yaw);

	        switch (dirs.get(0)) {
		        case NORTH:
		        	next.setYaw(180);
		        	break;
		        case SOUTH:
		        	next.setYaw(0);
		        	break;
		        case EAST:
		        	next.setYaw(-90);
		        	break;
		        case WEST:
		        	next.setYaw(90);
		        	break;
		        default:
		        	break;
	        }
	    }

	    return next;
	}
	
	private static Location getTrackedLocation(Location loc, List<NSEW> dirs, Shape shape, float yaw) {
	    Location next = loc.clone();
	    /*
	    for(Player p : Bukkit.getOnlinePlayers()) {
	    	p.sendTitle(" ", dirs.toString(), 0, 20, 0);
	    }
	    */
	    
	    for (NSEW dir : dirs) {
	        switch (shape) {
	            case NORTH_SOUTH:
	                if (dir == NSEW.NORTH) return next.add(0, 0, -1);  // Moving north
	                if (dir == NSEW.SOUTH) return next.add(0, 0, 1);   // Moving south
	                if (dir == NSEW.EAST) {
	                	if(yaw > -90) return next.add(0, 0, 1);    // Moving east
	                	return next.add(0, 0, -1);    // Moving east
	                }
	                if (dir == NSEW.WEST) {
	                	if(yaw < 90) return next.add(0, 0, 1);    // Moving east
	                	return next.add(0, 0, -1);    // Moving east
	                }
	                break;
	            case EAST_WEST:
	                if (dir == NSEW.EAST) return next.add(1, 0, 0);    // Moving east
	                if (dir == NSEW.WEST) return next.add(-1, 0, 0);   // Moving west
	                if (dir == NSEW.NORTH) {
	                	if(yaw > 0) return next.add(-1, 0, 0);  // Moving north
	                	return next.add(1, 0, 0);  // Moving north
	                }
	                if (dir == NSEW.SOUTH) {
	                	if(yaw > 0) return next.add(-1, 0, 0);  // Moving north
	                	return next.add(1, 0, 0);  // Moving north
	                }
	                break;
	            case ASCENDING_NORTH:
	                if (dir == NSEW.NORTH) return next.add(0, 1, -1);  // Ascending north
	                if (dir == NSEW.SOUTH) return next.add(0, 0, 1);   // Moving south
	                break;
	            case ASCENDING_SOUTH:
	            	if (dir == NSEW.NORTH) return next.add(0, 0, -1);  // Moving north
	                if (dir == NSEW.SOUTH) return next.add(0, 1, 1);   // Ascending south
	                break;
	            case ASCENDING_EAST:
	                if (dir == NSEW.EAST) return next.add(1, 1, 0);    // Ascending east
	                if (dir == NSEW.WEST) return next.add(-1, 0, 0);   // Moving west
	                break;
	            case ASCENDING_WEST:
	            	if (dir == NSEW.EAST) return next.add(1, 0, 0);    // Moving east
	                if (dir == NSEW.WEST) return next.add(-1, 1, 0);   // Ascending west
	                break;
	                
	            // **Curved Rails (Handling Turns)**
	            case NORTH_EAST:
	                // If the cart is facing north, it should go east.
	                if (dir == NSEW.NORTH) return next.add(0, 0, -1);  
	                // If the cart is facing east, it should go north.
	                if (dir == NSEW.EAST) return next.add(1, 0, 0);  
	                // If the cart is facing south, it should go east (as it's turning from south to east).
	                if (dir == NSEW.SOUTH) return next.add(1, 0, 0);  
	                // If the cart is facing west, it should go north (as it's turning from west to north).
	                if (dir == NSEW.WEST) return next.add(0, 0, -1);  
	                break;

	            case NORTH_WEST:
	                // If the cart is facing north, it should go west.
	                if (dir == NSEW.NORTH) return next.add(0, 0, -1);  
	                // If the cart is facing west, it should go north.
	                if (dir == NSEW.WEST) return next.add(-1, 0, 0);  
	                // If the cart is facing south, it should go west (as it's turning from south to west).
	                if (dir == NSEW.SOUTH) return next.add(-1, 0, 0);  
	                // If the cart is facing east, it should go north (as it's turning from east to north).
	                if (dir == NSEW.EAST) return next.add(0, 0, -1);  
	                break;

	            case SOUTH_EAST:
	                // If the cart is facing south, it should go south.
	                if (dir == NSEW.SOUTH) return next.add(0, 0, 1);  
	                // If the cart is facing east, it should go east.
	                if (dir == NSEW.EAST) return next.add(1, 0, 0);  
	                // If the cart is facing north, it should go east (as it's turning from north to east).
	                if (dir == NSEW.NORTH) return next.add(1, 0, 0);  
	                // If the cart is facing west, it should go south (as it's turning from west to south).
	                if (dir == NSEW.WEST) return next.add(0, 0, 1);  
	                break;

	            case SOUTH_WEST:
	                // If the cart is facing south, it should go west.
	                if (dir == NSEW.SOUTH) return next.add(0, 0, 1);  
	                // If the cart is facing west, it should go south.
	                if (dir == NSEW.WEST) return next.add(-1, 0, 0);  
	                // If the cart is facing north, it should go west (as it's turning from north to west).
	                if (dir == NSEW.NORTH) return next.add(-1, 0, 0);  
	                // If the cart is facing east, it should go south (as it's turning from east to south).
	                if (dir == NSEW.EAST) return next.add(0, 0, 1);  
	                break;
	                
	            default:
	                break;
	        }
	    }

	    return next;
	}



}
