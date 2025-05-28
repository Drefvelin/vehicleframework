package net.tfminecraft.VehicleFramework.Database;

import java.util.ArrayList;
import java.util.List;

public class IncompleteVehicle {
	private String id;
	private String name;
	
	private List<IncompleteComponent> components = new ArrayList<>();
	private List<IncompleteWeapon> weapons = new ArrayList<>();

	private List<RotationData> rotators = new ArrayList<>();

	private int throttle = 0;
	private int gear = 1;

	private List<PassengerData> passengers = new ArrayList<>();

	public IncompleteVehicle(String id, String name, List<IncompleteComponent> components, List<IncompleteWeapon> weapons, List<RotationData> rotations, List<PassengerData> passengers, int throttle, int gear) {
		this.id = id;
		this.name = name;
		this.components = components;
		this.weapons = weapons;
		rotators = rotations;
		this.passengers = passengers;
		this.throttle = throttle;
		this.gear = gear;
	}
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}

	public List<IncompleteComponent> getComponents() {
		return components;
	}
	
	public List<IncompleteWeapon> getWeapons(){
		return weapons;
	}

	public List<RotationData> getRotations() {
		return rotators;
	}

	public Integer getThrottle() {
		return throttle;
	}

	public Integer getGear() {
		return gear;
	}

	public List<PassengerData> getPassengers() {
		return passengers;
	}
}
