package net.tfminecraft.VehicleFramework.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Wings;

public class ConditionChecker {
	public static boolean checkConditions(ActiveVehicle vehicle, List<String> conditions) {
		boolean fulfilled = true;
		for(String c : conditions) {
			if(!fulfilled) break;
			String type = c.split("\\(")[0];
			String value = c.split("\\(")[1].replace(")", "");
			if(!checkCondition(vehicle, type, value)) fulfilled = false; 
		}
		return fulfilled;
	}
	
	private static boolean checkCondition(ActiveVehicle vehicle, String type, String value) {
		if(type.equalsIgnoreCase("state")) {
			if(!vehicle.getStateHandler().getCurrentState().getType().toString().equalsIgnoreCase(value)) return false;
		} else if(type.equalsIgnoreCase("lift")) {
			if(!vehicle.hasComponent(Component.WINGS)) return false;
			Wings wings = (Wings) vehicle.getComponent(Component.WINGS);
			if(!vehicle.hasComponent(Component.ENGINE)) return false;
			Engine engine = (Engine) vehicle.getComponent(Component.ENGINE);
			double lift = wings.getLift() * engine.getSpeed();
            double throttleFactor = Math.max(0, engine.getThrottle().getCurrent()) / 100.0;
            lift *= throttleFactor;
			if(lift < 0.49 && value.equalsIgnoreCase("true")) return false;
			if(lift >= 0.49 && value.equalsIgnoreCase("false")) return false;
		} else if(type.equalsIgnoreCase("passengers")) {
			if(!(vehicle.getSeatHandler().hasPassengers() == Boolean.parseBoolean(value))) return false;
		} else if(type.equalsIgnoreCase("seat_filled")) {
			if(vehicle.getAccessPanel().getSeat(value) != null && !vehicle.getAccessPanel().getSeat(value).isOccupied()) return false;
		} else if(type.equalsIgnoreCase("seat_empty")) {
			if(vehicle.getAccessPanel().getSeat(value) != null && vehicle.getAccessPanel().getSeat(value).isOccupied()) return false;
		} else if(type.equalsIgnoreCase("is_passenger")) {
			boolean passenger = vehicle.hasParent();
			if(value.equalsIgnoreCase("true") && !passenger) return false;
			if(value.equalsIgnoreCase("false") && passenger) return false;
		} else if(type.equalsIgnoreCase("has_fuel")) {
			boolean hasFuel = vehicle.hasFuel();
			if(value.equalsIgnoreCase("true") && !hasFuel) return false;
			if(value.equalsIgnoreCase("false") && hasFuel) return false;
		} else if(type.equalsIgnoreCase("OR")) {
			if(!orStatement(vehicle, value)) return false;
		} else if(type.equalsIgnoreCase("AND")) {
			if(!andStatement(vehicle, value)) return false;
		}
		return true;
	}
	
	private static boolean orStatement(ActiveVehicle vehicle, String s) {
		List<String> conditions = new ArrayList<>(Arrays.asList(s.split(";")));
		boolean fulfilled = false;
		for(String c : conditions) {
			if(fulfilled) break;
			String type = c.split("\\=")[0];
			String value = c.split("\\=")[1];
			if(checkCondition(vehicle, type, value)) fulfilled = true;
		}
		return fulfilled;
	}
	
	private static boolean andStatement(ActiveVehicle vehicle, String s) {
		List<String> conditions = new ArrayList<>(Arrays.asList(s.split(";")));
		for(String c : conditions) {
			String type = c.split("\\=")[0];
			String value = c.split("\\=")[1];
			if(!checkCondition(vehicle, type, value)) return false;
		}
		return true;
	}
}
