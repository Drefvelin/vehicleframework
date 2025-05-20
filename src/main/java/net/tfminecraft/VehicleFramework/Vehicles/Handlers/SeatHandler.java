package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.manager.MountManager;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

public class SeatHandler {
	//Passengers and Seats
	private MountManager manager;
	
	private List<Seat> seats = new ArrayList<>();
	
	private List<Entity> passengers = new ArrayList<>();
	
	private ActiveVehicle v;

	public SeatHandler(List<String> seats, Vehicle v) {
		for(String seat : seats) {
			this.seats.add(new Seat(seat, v.getId()));
		}
	}
	
	public SeatHandler(ActiveVehicle vehicle, ActiveModel model, SeatHandler another) {
		for(Seat s : another.getSeats()) {
			seats.add(new Seat(vehicle, s));
		}
		if(model.getMountManager().isEmpty()) {
			VFLogger.log("Model with no mount manager detected!");
			return;
		}
		manager = model.getMountManager().get();
		v = vehicle;
	}
	
	public void updateModel(ActiveModel m) {
		manager = m.getMountManager().get();
	}
	
	public boolean hasPassengers() {
		if(passengers.size() > 0) return true;
		return false;
	}
	public List<Entity> getPassengers(){
		return passengers;
	}
	public boolean isPassenger(Entity e) {
		return passengers.contains(e);
	}
	public void changeSeat(Entity e, Seat seat) {
		dismountPassenger(e, true);
		addPassenger(e, seat);
		
	}
	public void addPassenger(Entity e, Seat seat) {
		manager.mountPassenger(seat.getBone(), e, MountControllerTypes.WALKING);
	    seat.mount(e);
		if(!isPassenger(e)) passengers.add(e);
	}
	public void dismountPassenger(Entity e, boolean change) {
		manager.dismountPassenger(e);
		if(!change) {
			removePassenger(e);
			if(e instanceof Player) {
				Player p = (Player) e;
				p.closeInventory();
			}
		}
		resetSeat(e);
	}
	private void removePassenger(Entity e) {
		if(isPassenger(e)) {
			passengers.remove(e);
			if(e instanceof Player) {
				Player p = (Player) e;
				v.getVehicleManager().dismount(p);
				v.removeBoard(p);
			}
			resetSeat(e);
		}
	}
	public void dismountAll() {
		for(Seat s : seats) {
			if(!s.isOccupied()) continue;
			Entity e = s.getEntity();
			dismountPassenger(e, false);
		}
	}
	public void resetSeat(Entity e) {
		for(Seat s : seats) {
			if(!s.isOccupied()) continue;
			if(s.getEntity().equals(e)) {
				s.dismount();
			}
		}
	}
	public Seat getSeat(String s) {
		for(Seat seat : seats) {
			if(seat.getBone().equalsIgnoreCase(s)) return seat;
		}
		return null;
	}
	public Seat getSeat(Entity e) {
		for(Seat seat : seats) {
			if(!seat.isOccupied()) continue;
			if(seat.getEntity().equals(e)) return seat;
		}
		return null;
	}
	
	public List<Seat> getSeats(){
		return seats;
	}
	
	public void slowTick() {
		//check that everyone is in their seats
		List<Entity> verify = new ArrayList<>(passengers);
		for(Seat s : seats) {
			if(!s.isOccupied()) continue;
			Entity e = s.getEntity();
			verify.remove(e);
			if(manager.getPassengerSeatMap().containsKey(e)) continue;
			manager.mountPassenger(s.getBone(), e, MountControllerTypes.WALKING);
		    s.mount(e);
		}
		if(verify.size() > 0) {
			for(Entity e : verify) {
				if(e instanceof Player) {
					Player p = (Player) e;
					v.removeBoard(p);
				}
				passengers.remove(e);
			}
		}
	}
}
