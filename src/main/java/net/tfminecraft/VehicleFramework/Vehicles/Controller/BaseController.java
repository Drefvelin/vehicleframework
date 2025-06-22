package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.Direction;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Balloon;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;
import net.tfminecraft.VehicleFramework.Vehicles.State.VehicleState;

public class BaseController {
	
	public Vector climbVector(ActiveVehicle v, Vector velocity) {
		if(v.hasComponent(Component.BALLOON)) {
			Balloon balloon = (Balloon) v.getComponent(Component.BALLOON);
			double delta = balloon.getDelta();
			if(delta == 0) return velocity;
			velocity.setY(delta);
			return velocity;
		}
		VehicleState state = v.getCurrentState();
		if(state.getClimbHandler().canClimb()) {
			if(state.getClimbHandler().shouldClimb(v)) velocity.setY(0.3);
		}
		return velocity;
	}
	
	public Vector calculateMoveVector(ActiveVehicle v, VectorBone vector, Direction dir) {
		Entity e = v.getEntity();
		Vector velocity = v.getEntity().getVelocity();
		double y = velocity.getY();
		if(v.hasComponent(Component.ENGINE) || v.hasComponent(Component.GEARED_ENGINE)) velocity = engineVector(v, vector, e, velocity);
		if(v.hasComponent(Component.HARNESS)) velocity = harnessVector(v, vector, e, velocity, dir);
		velocity = setY(v, velocity, y);
		return velocity;
	}
	
	private Vector setY(ActiveVehicle v, Vector velocity, double y) {
		if(v.getCurrentState().getType().equals(State.GROUND) && !v.hasComponent(Component.WINGS)) velocity.setY(-0.49);
		if(v.shouldFloat()) {
			velocity.setY(y);
		}
		return velocity;
	}
	
	private Vector harnessVector(ActiveVehicle v, VectorBone vector, Entity e, Vector velocity, Direction dir) {
		Harness h = (Harness) v.getComponent(Component.HARNESS);
		if(h.hasMounts() && e != null && e.isValid() && e instanceof LivingEntity) {
        	Vector direction = vector.getVector().clone().normalize();

        	if(dir.equals(Direction.FORWARD)) velocity = direction.multiply(h.getSpeed()); // Forward velocity  
        	if(dir.equals(Direction.BACKWARD)) velocity = direction.multiply(h.getSpeed()*-0.3); // Backward velocity  
        	
        }
		return velocity;
	}
	
	private Vector engineVector(ActiveVehicle v, VectorBone vector, Entity e, Vector velocity) {
		if (e != null && e.isValid() && e instanceof LivingEntity) {
        	Vector direction = vector.getVector().clone().normalize();
			if(v.getCurrentState().getType().equals(State.FLYING) && v.hasComponent(Component.WINGS) && (v.getThrottle() != null && v.getThrottle().getCurrent() <= 10)) {
				double scale = (20-v.getThrottle().getCurrent())/20.0;
				//So you dont just stop mid air
				velocity = direction.multiply(0.65*scale); // Forward velocity 
			} else{
				velocity = direction.multiply(v.getAccessPanel().getSpeed()); // Forward velocity 
			}
        }
		return velocity;
	}
	
	public Direction getDirection(ActiveVehicle v) {
		if(v.hasComponent(Component.HARNESS)) {
			Harness h = (Harness) v.getComponent(Component.HARNESS);
			if(!h.hasMounts()) return Direction.STILL;
			return Direction.MOVING;
		}
		if(v.getAccessPanel().getSpeed() == 0) {
			return Direction.STILL;
		}
		if(v.getAccessPanel().isReverse()) return Direction.BACKWARD;
		return Direction.FORWARD;
	}
}
