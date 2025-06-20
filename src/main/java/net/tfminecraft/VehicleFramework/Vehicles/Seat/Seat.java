package net.tfminecraft.VehicleFramework.Vehicles.Seat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Database.LogWriter;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;

public class Seat {
	private HashMap<Player, Long> cooldown = new HashMap<>();
	//Set when vehicle is active
	private Entity e;
	private int currentWeapon = 0;
	private List<ActiveWeapon> weapons = new ArrayList<>();
	
	private ActiveVehicle vehicle;
	private ActiveVehicle mountedVehicle;
	
	//Set by config
	private SeatType type;
	private String bone;
	
	public Seat(String s, String id) {
		if(s.split("\\(").length < 2) VFLogger.log("invalid seat detected for vehicle "+id);
		
		String t = s.split("\\(")[0];
		String seat = s.split("\\(")[1].replace(")", "");
		
		if(SeatType.valueOf(t.toUpperCase()) == null) {
			type = SeatType.PASSENGER; //handles malinput in config
		} else {
			type = SeatType.valueOf(t.toUpperCase());
		}
		bone = seat;
	}
	
	public Seat(SeatType t, String bone) {
		type = t;
		this.bone = bone;
	}
	
	public Seat(ActiveVehicle v, Seat another) {
		type = another.getType();
		bone = another.getBone();
		vehicle = v;
		v.getAccessPanel().addSeat(this);
	}
	
	public boolean isOccupied() {
		return e != null;
	}

	public Entity getEntity() {
		return e;
	}
	public void mount(ActiveVehicle v, ActiveVehicle parent) {
		mount(v.getEntity());
		mountedVehicle = v;
		v.setParent(parent);
	}
	
	public void mount(Entity entity) {
		if(e != null) return;
		e = entity;
		/*
		if(e instanceof Player) {
			LogWriter.logEnter((Player) e, vehicle);
		}
		*/
		if(hasWeapon() && e instanceof Player) {
			Player p = (Player) e;
			currentWeapon = 0;
			getWeapon().setController(p);
		}
	}
	
	public void dismount() {
		/*
		if(e != null && e instanceof Player) {
			LogWriter.logExit((Player) e, vehicle);
		}
		*/
		e = null;
		if(hasWeapon()) {
			getWeapon().disconnect();
		}
		if(mountedIsVehicle()) {
			mountedVehicle.setParent(null);
			mountedVehicle = null;
		}
	}
	
	public void changeWeapon() {
		if(!(e instanceof Player)) return;
		Player p = (Player) e;
		if(cooldown.containsKey(p)) {
			if(cooldown.get(p) > System.currentTimeMillis()) {
				return;
			}
		}
		cooldown.put(p, System.currentTimeMillis()+300);
		if(weapons.size() <= 1) return;
		getWeapon().disconnect();
		currentWeapon++;
		if(currentWeapon >= weapons.size()) {
			currentWeapon = 0;
		}
		getWeapon().setController(p);
	}
	
	public boolean hasWeapon() {
		return weapons.size() > 0;
	}
	public ActiveWeapon getWeapon() {
		return weapons.get(currentWeapon);
	}
	public void connectWeapon(ActiveWeapon w) {
		weapons.add(w);
	}
	
	public boolean mountedIsVehicle() {
		return mountedVehicle != null;
	}
	public ActiveVehicle getMountedVehicle() {
		return mountedVehicle;
	}

	public SeatType getType() {
		return type;
	}

	public String getBone() {
		return bone;
	}
	
	
}
