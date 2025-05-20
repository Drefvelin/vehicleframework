package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;

public class FloatController {
	public Vector calculateFloat(ActiveVehicle v, Vector velocity) {
		double y = velocity.getY();
		if(checkFloat(v)) {
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
	
	private boolean checkFloat(ActiveVehicle v) {
		if(!v.shouldFloat()) return false;
		Entity entity = v.getEntity();
		if (entity != null && entity.isValid() && entity instanceof LivingEntity) {
            // Check if the entity is in water
            Location location = entity.getLocation();
            Block block = location.getBlock();

            if (block.isLiquid()) {
                return true;
            }
        }
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
