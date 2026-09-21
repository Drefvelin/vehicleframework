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
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

public class SeatHandler {
	public enum MountResult {
		MOUNTED,
		UNAVAILABLE,
		REJECTED
	}

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
		v = vehicle;
		for(Seat s : another.getSeats()) {
			seats.add(new Seat(vehicle, s));
		}
		if(model.getMountManager().isEmpty()) {
			VFLogger.log("Model with no mount manager detected!");
			PersistenceLog.append("MOUNT_MANAGER_MISSING " + PersistenceLog.vehicle(vehicle));
			return;
		}
		manager = model.getMountManager().get();
	}
	
	public void updateModel(ActiveModel m) {
		manager = m.getMountManager().get();
	}
	
	public boolean hasPassengers() {
		if(passengers.size() > 0) return true;
		return false;
	}

	public boolean hasCaptain() {
		return captainPlayer() != null;
	}

	public boolean isCaptain(Player player) {
		if (player == null) {
			return false;
		}
		Player captain = captainPlayer();
		return captain != null && captain.getUniqueId().equals(player.getUniqueId());
	}

	public Player captainPlayer() {
		for (Seat seat : seats) {
			if (!seat.getType().equals(SeatType.CAPTAIN) || !seat.isOccupied()) {
				continue;
			}
			if (seat.getEntity() instanceof Player player) {
				return player;
			}
		}
		return null;
	}
	public List<Entity> getPassengers(){
		return passengers;
	}
	public boolean isPassenger(Entity e) {
		return passengers.contains(e);
	}
	public MountResult changeSeat(Entity e, Seat seat) {
		if (e == null || seat == null || seat.isOccupied()) {
			return MountResult.UNAVAILABLE;
		}
		dismountPassenger(e, true);
		MountResult result = addPassenger(e, seat);
		if (result != MountResult.MOUNTED) {
			removePassenger(e);
		}
		return result;
	}
	public MountResult addPassenger(Entity e, Seat seat) {
		if (e == null || seat == null || seat.isOccupied()) {
			return MountResult.UNAVAILABLE;
		}
		if (!acceptMount(e, seat.getBone())) {
			if (e instanceof Player p) {
				PersistenceLog.mount(p, v, seat, manager != null, false);
			}
			return MountResult.REJECTED;
		}
		if(seat.getType().equals(SeatType.CAPTAIN) && e instanceof Player) {
			VehicleFramework.getLog().logEntry(((Player) e).getName()+" entered captain seat of "+v.getName()+" at "+e.getLocation().getX()+"x, "+e.getLocation().getZ()+"z");
		}
	    seat.mount(e);
		if(!isPassenger(e)) passengers.add(e);
		if (e instanceof Player p) {
			PersistenceLog.mount(p, v, seat, true, true);
		}
		return MountResult.MOUNTED;
	}

	private boolean acceptMount(Entity e, String bone) {
		if (manager == null || e == null || bone == null) {
			return false;
		}
		boolean accepted = manager.mountPassenger(bone, e, MountControllerTypes.WALKING)
				&& manager.getPassengerSeatMap().containsKey(e);
		if (!accepted) {
			manager.dismountPassenger(e);
		}
		return accepted;
	}
	public void dismountPassenger(Entity e, boolean change) {
		if (manager != null) {
			manager.dismountPassenger(e);
		}
		if(!change) {
			removePassenger(e);
			if(e instanceof Player) {
				Player p = (Player) e;
				p.closeInventory();
			}
		}
		resetSeat(e);
		PersistenceLog.dismount(e, v, change);
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
		if(manager == null) {
			for (Seat s : new ArrayList<>(seats)) {
				if (!s.isOccupied()) continue;
				rejectOccupied(s.getEntity(), s.getBone());
			}
			return;
		}
		List<Entity> verify = new ArrayList<>(passengers);
		for(Seat s : new ArrayList<>(seats)) {
			if(!s.isOccupied()) continue;
			Entity e = s.getEntity();
			verify.remove(e);
			if(manager.getPassengerSeatMap().containsKey(e)) continue;
			PersistenceLog.remount(e, v, s.getBone());
			if (acceptMount(e, s.getBone())) {
				continue;
			}
			rejectOccupied(e, s.getBone());
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

	private void rejectOccupied(Entity e, String bone) {
		dismountPassenger(e, false);
		if (v == null) {
			return;
		}
		Player notify = e instanceof Player player ? player : null;
		v.getVehicleManager().recoverRejectedMount(v, e, bone, notify);
	}
}
