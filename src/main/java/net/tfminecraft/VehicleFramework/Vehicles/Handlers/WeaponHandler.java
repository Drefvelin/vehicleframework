package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;

public class WeaponHandler {
	private List<ActiveWeapon> weapons = new ArrayList<>();
	
	public WeaponHandler(ActiveModel m, ActiveVehicle v, List<Weapon> wList, SeatHandler seatHandler) {
		for(Weapon w : wList) {
			ActiveWeapon aw = new ActiveWeapon(m, v, w, null);
			weapons.add(aw);
			for(Seat seat : seatHandler.getSeats()) {
				if(seat.getBone().equalsIgnoreCase(aw.getSeat())) seat.connectWeapon(aw);
			}
		}
	}
	
	public void updateModel(ActiveModel m) {
		for(ActiveWeapon w : weapons) {
			w.updateModel(m);
		}
	}
	
	public void damage(String cause, double a) {
		for(ActiveWeapon w : weapons) {
			w.damage(cause, a);
		}
	}
	
	public List<ActiveWeapon> getWeapons(){
		return weapons;
	}
	
	public void input(List<Player> nearby, Keybind key, Player p) {
		for(ActiveWeapon w : weapons) {
			if(!w.isControlled()) continue;
			if(w.getController().equals(p)) w.input(nearby, key);
		}
	}
	
	public void tick() {
		for(ActiveWeapon w : weapons) {
			w.tick();
		}
	}
}
