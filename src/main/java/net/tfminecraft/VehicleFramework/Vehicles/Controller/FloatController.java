package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;

public class FloatController {
	private HashMap<ActiveVehicle, Long> cooldown = new HashMap<>();

	public Vector calculateFloat(ActiveVehicle v, Vector velocity) {
		double y = velocity.getY();
		if(checkFloat(v)) {
			breakLilyPadsUnderVehicle(v);
            y += 0.01;
            if(v.isDestroyed()) {
            	y = -0.03;
            } else {
            	if(v.getComponent(Component.HULL) instanceof SinkableHull) {
            		SinkableHull hull = (SinkableHull) v.getComponent(Component.HULL);
                	if(hull.hasSinkProgress()) {
                    	if(hull.isSinking() && checkSink(v)) {
                    		y = 0.01*(hull.getSinkProgress()/100.0)*-1;
                    	} else {
                    		y = 0.01*((100-hull.getSinkProgress())/100.0);
                    	}
                    }
            	}
            }
            velocity.setY(y);
        }
		return velocity;
	}

	private void breakLilyPadsUnderVehicle(ActiveVehicle v) {
		Entity entity = v.getEntity();
		if (entity == null || !entity.isValid()) return;

		// Expand bounding box by 1 block in X and Z, and slightly shrink Y to just check "feet" level
		BoundingBox box = entity.getBoundingBox().clone().expand(1, 0, 1);

		// Loop through all blocks in the bounding box
		for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.floor(box.getMaxX()); x++) {
			for (int y = (int) Math.floor(box.getMinY()); y <= (int) Math.floor(box.getMaxY()); y++) {
				for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.floor(box.getMaxZ()); z++) {
					Block block = entity.getWorld().getBlockAt(x, y, z);
					if (block.getType() == Material.LILY_PAD) {
						block.breakNaturally();
						entity.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);
					}
				}
			}
		}
	}
	
	private boolean checkFloat(ActiveVehicle v) {
		if(!v.shouldFloat()) return false;
		if(cooldown.containsKey(v)) {
			if(cooldown.get(v) > System.currentTimeMillis()) {
				return false;
			}
		}
		Entity entity = v.getEntity();
		if (entity != null && entity.isValid() && entity instanceof LivingEntity) {
            // Check if the entity is in water
            Location location = entity.getLocation().clone().add(0, 0.3, 0);
            Block block = location.getBlock();

            if (block.isLiquid()) {
                return true;
            }
        }
		cooldown.put(v, System.currentTimeMillis()+600);
		return false;
	}
	private boolean checkSink(ActiveVehicle v) {
		Entity entity = v.getEntity();
		if (entity != null && entity.isValid() && entity instanceof LivingEntity) {
            // Check if the entity is in water
            Location location = entity.getLocation().clone();
            location.add(0,2,0);
            Block block = location.getBlock();

            if (block.isLiquid()) {
                return false;
            }
        }
		return true;
	}
}
