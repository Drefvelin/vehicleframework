package net.tfminecraft.VehicleFramework.Projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;

public class HitChecker {
	private List<Material> waterBlocks = Arrays.asList(Material.WATER, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.AIR);
	
	public boolean hasHitLocation(Location loc) {
	    Block blockAtLocation = loc.getBlock();
	    
	    if (blockAtLocation.getType() != Material.AIR && blockAtLocation.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block below = loc.clone().subtract(0, 1, 0).getBlock();
	    if (below.getType() != Material.AIR && below.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block above = loc.clone().add(0, 1, 0).getBlock();
	    if (above.getType() != Material.AIR && above.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block north = loc.clone().add(0, 0, -1).getBlock();
	    if (north.getType() != Material.AIR && north.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block south = loc.clone().add(0, 0, 1).getBlock();
	    if (south.getType() != Material.AIR && south.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block west = loc.clone().add(-1, 0, 0).getBlock();
	    if (west.getType() != Material.AIR && west.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block east = loc.clone().add(1, 0, 0).getBlock();
	    if (east.getType() != Material.AIR && east.getType() != Material.LIGHT) {
	        return true;
	    }

	    return false;
	}
	
	public boolean hasHit(Entity e, List<Entity> ignore) {
		Location loc = e.getLocation();
	    Block blockAtLocation = loc.getBlock();
	    
	    if (blockAtLocation.getType() != Material.AIR && blockAtLocation.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block below = loc.clone().subtract(0, 1, 0).getBlock();
	    if (below.getType() != Material.AIR && below.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block above = loc.clone().add(0, 1, 0).getBlock();
	    if (above.getType() != Material.AIR && above.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block north = loc.clone().add(0, 0, -1).getBlock();
	    if (north.getType() != Material.AIR && north.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block south = loc.clone().add(0, 0, 1).getBlock();
	    if (south.getType() != Material.AIR && south.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block west = loc.clone().add(-1, 0, 0).getBlock();
	    if (west.getType() != Material.AIR && west.getType() != Material.LIGHT) {
	        return true;
	    }

	    Block east = loc.clone().add(1, 0, 0).getBlock();
	    if (east.getType() != Material.AIR && east.getType() != Material.LIGHT) {
	        return true;
	    }
	    
	    for (Entity entity : loc.getWorld().getNearbyEntities(e.getLocation(), 0.5, 0.5, 0.5)) {
	        if (entity == e || ignore.contains(entity)) continue; // Skip the entity itself
	        
	        // Optional: distance check for performance (e.g., entities within 5 blocks)
	        if (entity.getLocation().distance(loc) > 5) continue;

	        // Check if the location intersects with this entity's bounding box
	        if (entity.getBoundingBox().overlaps(e.getBoundingBox())) {
	            return true; // The location intersects with this entity's hitbox
	        }
	    }

	    return false;
	}
	public boolean hasHitIgnoreWater(Entity e, List<Entity> ignore) {
		Location loc = e.getLocation();
	    Block blockAtLocation = loc.getBlock();
	    
	    if (check(blockAtLocation, true)) {
	        return true;
	    }

	    Block below = loc.clone().subtract(0, 1, 0).getBlock();
	    if (check(below, true)) {
	        return true;
	    }

	    Block above = loc.clone().add(0, 1, 0).getBlock();
	    if (check(above, true)) {
	        return true;
	    }

	    Block north = loc.clone().add(0, 0, -1).getBlock();
	    if (check(north, true)) {
	        return true;
	    }

	    Block south = loc.clone().add(0, 0, 1).getBlock();
	    if (check(south, true)) {
	        return true;
	    }

	    Block west = loc.clone().add(-1, 0, 0).getBlock();
	    if (check(west, true)) {
	        return true;
	    }

	    Block east = loc.clone().add(1, 0, 0).getBlock();
	    if (check(east, true)) {
	        return true;
	    }
	    
	    for (Entity entity : loc.getWorld().getNearbyEntities(e.getLocation(), 0.5, 0.5, 0.5)) {
	        if (entity == e || ignore.contains(entity)) continue; // Skip the entity itself
	        
	        // Optional: distance check for performance (e.g., entities within 5 blocks)
	        if (entity.getLocation().distance(loc) > 5) continue;

	        // Check if the location intersects with this entity's bounding box
	        if (entity.getBoundingBox().overlaps(e.getBoundingBox())) {
	            return true; // The location intersects with this entity's hitbox
	        }
	    }

	    return false;
	}
	
	private boolean check(Block b, boolean ignoreWater) {
		if(!ignoreWater) {
			return b.getType() != Material.AIR && b.getType() != Material.LIGHT;
		}
		return b.getType() != Material.AIR && b.getType() != Material.LIGHT && !waterBlocks.contains(b.getType()) && !(b.getBlockData() instanceof Waterlogged && ((Waterlogged) b.getBlockData()).isWaterlogged());
		
	}
	
	public List<Entity> getHitEntities(Entity e, List<Entity> ignore, double expand) {
		Location loc = e.getLocation();
		List<Entity> hits = new ArrayList<>();
		for (Entity entity : loc.getWorld().getNearbyEntities(e.getLocation(), expand, expand, expand)) {
	        if (entity == e || ignore.contains(entity)) continue; // Skip the entity itself
	        
	        // Optional: distance check for performance (e.g., entities within 5 blocks)
	        if (entity.getLocation().distanceSquared(loc) > 25) continue;

	        // Check if the location intersects with this entity's bounding box
	        if (entity.getBoundingBox().overlaps(e.getBoundingBox().expand(expand, expand, expand))) {
	            hits.add(entity); // The location intersects with this entity's hitbox
	        }
	    }
		return hits;
	}
}
