package net.tfminecraft.VehicleFramework.Vehicles.Handlers.State;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;

public class ClimbHandler {
    private int climbPower;

    public ClimbHandler(ConfigurationSection config) {
        climbPower = config.getInt("climb", 0);
    }
    
    public ClimbHandler() {
        climbPower = 0;
    }


    public boolean canClimb() {
        return climbPower > 0;
    }

    public boolean shouldClimb(ActiveVehicle v) {
    	if(v.getAccessPanel().getSpeed() == 0) return false;
        if (!canClimb()) return false; // If climb power is 0, vehicle can't climb.
        boolean climb = false;
        if(checkClimb(v.getEntity(), 0)) climb = true;
        if(v.hasComponent(Component.HARNESS)) {
        	Harness h = (Harness) v.getComponent(Component.HARNESS);
        	for(Entity e : h.getMountedEntities()) {
        		if(checkClimb(e, 1.0) && !checkClimb(v.getEntity(), 0)) climb = true;
        	}
        }
        return climb;
    }
    
    private boolean checkClimb(Entity e, double offset) {
        World world = e.getWorld();
        BoundingBox box = e.getBoundingBox().clone().expand(-0.25, 0, -0.25);
        
        int solid = 0;
        int nonSolid = 0;
        
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.ceil(box.getMaxX());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.ceil(box.getMaxZ());
        int y = (int) Math.floor(box.getMinY()-1);
        
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);

                if (!(block.getType().isSolid() || block.getType().toString().contains("_SLAB"))) {
                	nonSolid++;
                	continue;
                }
                solid++;
            }
        }
        if(nonSolid > solid) return false;
        
        box.expand(0.35, 0, 0.35);

        minX = (int) Math.floor(box.getMinX());
        maxX = (int) Math.ceil(box.getMaxX());
        minZ = (int) Math.floor(box.getMinZ());
        maxZ = (int) Math.ceil(box.getMaxZ());
        y = (int) Math.floor(box.getMinY()+offset);

        boolean foundValidBlocks = false;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);

                if (!(block.getType().isSolid() || block.getType().toString().contains("_SLAB"))) continue; // Skip non-solid blocks// Found at least one solid block

                // Check if the block has (climbPower - 1) or fewer blocks above it
                if (hasSpaceAbove(block, climbPower)) {
                	foundValidBlocks = true; // If any block has too many above it, return false
                }
            }
        }

        return foundValidBlocks; // Return true only if at least one valid block was found
    }

    /**
     * Checks if the block has (climbPower - 1) or fewer solid blocks above it.
     */
    private boolean hasSpaceAbove(Block block, int climbPower) {
        for (int i = 1; i <= climbPower + 1; i++) {
            Block above = block.getRelative(0, i, 0);
            if (above.getType().isSolid()) {
                return false; // Too many blocks above
            }
        }
        return true;
    }
}

