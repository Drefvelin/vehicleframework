package net.tfminecraft.VehicleFramework.Vehicles.Util;

import java.util.ArrayList;
import java.util.List;

import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

public class AccessPanel {
	private List<BoneRotator> rotators = new ArrayList<>();
	private List<Seat> seats = new ArrayList<>();
	
	private double turnRate = 0;
	private double speed = 0;
	private boolean reverse = false;
	
	public List<Seat> getSeats(){
		return seats;
	}
	
	public Seat getSeat(String id) {
		for(Seat s : seats) {
			if(s.getBone().equalsIgnoreCase(id)) return s;
		}
		return null;
	}
	
	public void addSeat(Seat s) {
		seats.add(s);
	}
	
	public List<BoneRotator> getRotators(){
		return rotators;
	}
	
	public BoneRotator getRotator(String s) {
		for(BoneRotator r : rotators) {
			if(r.getId().equalsIgnoreCase(s)) return r;
		}
		return null;
	}
	
	public void addRotator(BoneRotator r) {
		rotators.add(r);
	}
	
	public void setTurnRate(double turnRate) {
		this.turnRate = turnRate;
	}
	
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}
	
	public double getTurnRate() {
		if(speed == 0) return 0;
		return turnRate;
	}

	public double getSpeed() {
		return speed;
	}

	public boolean isReverse() {
		return reverse;
	}
}
