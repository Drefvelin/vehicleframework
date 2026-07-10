package net.tfminecraft.VehicleFramework.Database;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

public class IncompleteVehicle {
	private String id;
	private String uuid;
	private String name;

	private String skin;
	
	private List<IncompleteComponent> components = new ArrayList<>();
	private List<IncompleteWeapon> weapons = new ArrayList<>();

	private List<RotationData> rotators = new ArrayList<>();

	private int throttle = 0;
	private int gear = 1;
	private float yaw = 0f;
	private double fuel = 0.0;

	private List<PassengerData> passengers = new ArrayList<>();

	private List<JsonObject> containers = new ArrayList<>();

	private String owner = "none";
	private boolean whitelisted = false;
	private List<String> whitelist = new ArrayList<>();

	public IncompleteVehicle(String uuid, String id, String name, String skin, List<IncompleteComponent> components, List<IncompleteWeapon> weapons, List<RotationData> rotations, List<PassengerData> passengers, List<JsonObject> containers, int throttle, int gear, float yaw, double fuel, String owner, boolean whitelisted, List<String> whitelist) {
		this.uuid = uuid;
		this.id = id;
		this.name = name;
		this.skin = skin;
		this.components = components;
		this.weapons = weapons;
		rotators = rotations;
		this.passengers = passengers;
		this.throttle = throttle;
		this.gear = gear;
		this.yaw = yaw;
		this.fuel = fuel;
		this.containers = containers;
		this.owner = owner;
		this.whitelisted = whitelisted;
		this.whitelist = whitelist;
	}

	public double getFuel() {
		return fuel;
	}

	public String getSkin() {
		return skin;
	}

	public float getYaw() {
		return yaw;
	}

	public String getUUID() {
		return uuid;
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

	public List<JsonObject> getContainers() {
		return containers;
	}

	public String getOwner() {
		return owner;
	}

	public boolean isWhitelisted() {
		return whitelisted;
	}

	public List<String> getWhitelist() {
		return whitelist;
	}
}
