package net.tfminecraft.VehicleFramework.Database;

import java.util.ArrayList;
import java.util.List;

public class IncompleteVehicle {
	private String id;
	private String name;
	
	private List<IncompleteComponent> components = new ArrayList<>();
	private List<IncompleteWeapon> weapons = new ArrayList<>();

	public IncompleteVehicle(String id, String name, List<IncompleteComponent> components, List<IncompleteWeapon> weapons) {
		this.id = id;
		this.name = name;
		this.components = components;
		this.weapons = weapons;
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
}
