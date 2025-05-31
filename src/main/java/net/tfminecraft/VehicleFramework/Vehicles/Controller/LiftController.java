package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Wings;

public class LiftController {

	public Vector calculateLift(BoneRotator rotator, ActiveVehicle v, Vector velocity) {
	    if (v.hasComponent(Component.WINGS) && v.hasComponent(Component.ENGINE)) {
	        Wings wings = (Wings) v.getComponent(Component.WINGS);
	        Engine engine = (Engine) v.getComponent(Component.ENGINE);
	        
	        if (v.getStateHandler().getCurrentState().getType().equals(State.FLYING) || engine.getThrottle().getCurrent() != 0) {
				
	            AxisAngle4d angles = rotator.getAngles(); // Pitch (x), Yaw (y), Roll (z)
	            double y = velocity.getY();
	            /*
	            if (y > 0) {
	                ConvertedAngle globalAngles = rotator.getConvertedAngles();
	                double pitch = Math.toRadians(globalAngles.getPitch()); // Convert to radians

		            // Custom falloff function for lift influence
		            double falloffFactor = Math.max(0, (Math.cos(pitch) - Math.cos(Math.toRadians(60))) / (1 - Math.cos(Math.toRadians(60))));
	
		            y *= falloffFactor; // Reduce y progressively
	            }
	            */
	            y -= 0.49;
				if(engine.getThrottle().getCurrent() < 10) {
					y -= 0.98;
					return velocity;
				}
	            velocity.setY(y); // Update vertical velocity

	            // Calculate lift based on velocity magnitude instead of engine speed
	            double velocityMagnitude = velocity.length(); // Get speed
	            double lift = wings.getLift() * velocityMagnitude * 0.1; // Scale lift with speed
	            /*
	            for(Player p : Bukkit.getOnlinePlayers()) {
	            	p.sendTitle(" ", "Lifet. "+lift, 0, 20, 0);
	            }
	            */

	            // Apply an upper bound to lift
	            if (lift > 0.52) lift = 0.52;

	            // Calculate lift vector relative to plane's orientation
	            Vector liftVector = calculateLiftVector(angles, lift);

	            // Apply the lift vector to velocity
	            velocity.add(liftVector);
	        }
	    }
	    return velocity;
	}
    
    private Vector calculateLiftVector(AxisAngle4d axisAngle, double lift) {
        // Convert AxisAngle to Quaternion
        Quaterniond quaternion = new Quaterniond(axisAngle);

        // "Up" vector in local space before rotation (e.g., (0, 1, 0))
        Vector3d localUp = new Vector3d(0, 1, 0);

        // Rotate the "up" vector using the quaternion
        Vector3d rotatedUp = localUp.rotate(quaternion);

        // Use the Y-component of the rotated vector to calculate lift
        double liftY = Math.abs(rotatedUp.y * lift);

        // Scale the lift vector by the lift force
        return new Vector(0, liftY, 0);
    }
    
    public void checkHitWall(ActiveVehicle vehicle) {
    	if(vehicle.isDestroyed()) return;
    	if(!vehicle.hasComponent(Component.WINGS)) return;
		if(vehicle.getAccessPanel().getSpeed() < 0.3) return;
    	BoundingBox boundingBox = vehicle.getEntity().getBoundingBox().clone().expand(0.5, -1, 0.5);

        // Iterate through all blocks within the expanded bounding box
        boolean hitSomething = false;
        for (int x = (int) Math.floor(boundingBox.getMinX()); x <= (int) Math.ceil(boundingBox.getMaxX()); x++) {
            for (int y = (int) Math.floor(boundingBox.getMinY()); y <= (int) Math.ceil(boundingBox.getMaxY()); y++) {
                for (int z = (int) Math.floor(boundingBox.getMinZ()); z <= (int) Math.ceil(boundingBox.getMaxZ()); z++) {
                    Block block = vehicle.getEntity().getWorld().getBlockAt(x, y, z);
                    if (!block.isPassable()) {
                        hitSomething = true;
                        break;
                    }
                }
                if (hitSomething) break;
            }
            if (hitSomething) break;
        }
        
        if (hitSomething /*&& vehicle.getStateHandler().getCurrentState().getType().equals(State.FLYING)*/) {
        	vehicle.kill(VehicleDeath.EXPLODE);
        }
    }
}
