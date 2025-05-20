package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;

public class RotateController {
	
	private void turnLocal(BoneRotator rotator, ActiveVehicle v, boolean reverse) {
		float turn = (float) v.getAccessPanel().getTurnRate();
		if(reverse) turn = turn*-1;
		if(turn != 0) rotator.rotateSmoothed(0, turn, 0);
	}
	private void turn(BoneRotator rotator, ActiveVehicle v, boolean reverse) {
		float turn = (float) v.getAccessPanel().getTurnRate();
		if(reverse) turn = turn*-1;
		turn *= 5;
		ConvertedAngle a = new ConvertedAngle(rotator.getAnimator().getRotation());
		if(turn != 0) rotator.setRotation(a.getYaw()+turn, a.getPitch(), a.getRoll(), true, false, false);
	}
	
	public void turnLeftLocal(BoneRotator rotator, ActiveVehicle v, Player p) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			v.animate(Animation.LEFT);
	    	turnLocal(rotator, v, false);     
		}
	}

	public void turnRightLocal(BoneRotator rotator, ActiveVehicle v, Player p) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			v.animate(Animation.RIGHT);
	    	turnLocal(rotator, v, true);
		}
	}
	
	public void turnLeft(BoneRotator rotator, ActiveVehicle v, Player p) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			v.animate(Animation.LEFT);
	    	turn(rotator, v, false);     
		}
	}
	
	public void turnRight(BoneRotator rotator, ActiveVehicle v, Player p) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			v.animate(Animation.RIGHT);
	    	turn(rotator, v, true);     
		}
	}
	
	public void pitchUp(BoneRotator rotator, ActiveVehicle v, Player p, float rate) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			rotator.rotateSmoothed(rate, 0, 0);    
		}
	}

	public void pitchDown(BoneRotator rotator, ActiveVehicle v, Player p, float rate) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			rotator.rotateSmoothed(-rate, 0, 0);    
		}
	}
	public void rollLeft(BoneRotator rotator, ActiveVehicle v, Player p, float rate) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			rotator.rotateSmoothed(0, 0, rate);    
		}
	}

	public void rollRight(BoneRotator rotator, ActiveVehicle v, Player p, float rate) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			rotator.rotateSmoothed(0, 0, -rate);    
		}
	}
}
