package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Gear.Gear;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;

public class ThrottleController {
	public void throttle(ActiveVehicle v, Player p,  boolean down) {
		if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) {
			if(v.hasComponent(Component.ENGINE)) {
				Engine engine = (Engine) v.getComponent(Component.ENGINE);
				if(engine.requiresStart() && !engine.isStarted()) {
					engine.start(p);
					return;
				}
				if(down) {
					engine.getThrottle().change(-1);
				} else {
					engine.getThrottle().change(1);
				}
				
			} else if(v.hasComponent(Component.GEARED_ENGINE)) {
				GearedEngine engine = (GearedEngine) v.getComponent(Component.GEARED_ENGINE);
				if(engine.requiresStart() && !engine.isStarted()) {
					engine.start(p);
					return;
				}
				engine.throttle(down);
			}	
		}
	}
}
