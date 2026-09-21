package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.manager.MountManager;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountController;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
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
		manager = m.getMountManager().orElse(null);
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
	/** Require the same seat in VF, ME's passenger map, and ME's rider update registry. */
	public boolean isMounted(Entity e) {
		if (!isPassenger(e) || manager == null) return false;
		Mount mount = manager.getPassengerSeatMap().get(e);
		if (mount == null || !mount.getPassengers().contains(e)) return false;
		MountController controller = mountController(e);
		return controller != null && controller.getMount() == mount;
	}

	MountController mountController(Entity e) {
		return ModelEngineAPI.getMountPairManager().getController(e.getUniqueId());
	}
	public void changeSeat(Entity e, Seat seat) {
		if(seat == null || seat.isOccupied()) return;
		dismountPassenger(e, true);
		addPassenger(e, seat);
		if(getSeat(e) == null) removePassenger(e);
		
	}
	public void addPassenger(Entity e, Seat seat) {
		if(seat == null || seat.isOccupied()) return;
		if (!tryMount(e, seat)) {
			if (e instanceof Player p) {
				PersistenceLog.mount(p, v, seat, manager != null, false);
				p.sendMessage("§cCould not mount this seat. Please try again.");
			}
			return;
		}
		if(seat.getType().equals(SeatType.CAPTAIN) && e instanceof Player) {
			VehicleFramework.getLog().logEntry(((Player) e).getName()+" entered captain seat of "+v.getName()+" at "+e.getLocation().getX()+"x, "+e.getLocation().getZ()+"z");
		}
	    seat.mount(e);
		if(!isPassenger(e)) passengers.add(e);
		if (e instanceof Player p) {
			PersistenceLog.mount(p, v, seat, true, manager.getPassengerSeatMap().containsKey(e));
		}
	}

	private boolean tryMount(Entity e, Seat seat) {
		logMountState("MOUNT_ATTEMPT", e, seat);
		// Shift opens VF's seat menu; only VF should explicitly dismount the rider.
		boolean accepted = manager != null
				&& manager.mountPassenger(seat.getBone(), e, MountControllerTypes.WALKING_FORCE);
		logMountState("MOUNT_RESULT accepted=" + accepted, e, seat);
		return accepted;
	}

	private void logMountState(String phase, Entity e, Seat seat) {
		if (!PersistenceLog.isEnabled() || !(e instanceof Player p)) return;
		Mount target = manager == null ? null : manager.getSeat(seat.getBone()).orElse(null);
		Mount mapped = manager == null ? null : manager.getPassengerSeatMap().get(e);
		MountController controller = mountController(e);
		PersistenceLog.append(phase + " seat=" + seat.getBone()
				+ " seatGlobal=" + (target == null ? "none" : target.getGlobalLocation())
				+ " meSeatMap=" + (mapped != null)
				+ " meSeatPassenger=" + (mapped != null && mapped.getPassengers().contains(e))
				+ " meController=" + (controller != null)
				+ " controllerSeatMatches=" + (controller != null && controller.getMount() == mapped)
				+ " " + PersistenceLog.player(p) + " " + PersistenceLog.vehicle(v));
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
			dismountAll();
			return;
		}
		List<Entity> verify = new ArrayList<>(passengers);
		for(Seat s : seats) {
			if(!s.isOccupied()) continue;
			Entity e = s.getEntity();
			verify.remove(e);
			if(isMounted(e)) continue;
			PersistenceLog.remount(e, v, s.getBone());
			logMountState("MOUNT_RECOVERY", e, s);
			// A stale ME entry must be removed before it will accept a fresh mount.
			manager.dismountPassenger(e);
			if (!tryMount(e, s)) {
				dismountPassenger(e, false);
			}
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
