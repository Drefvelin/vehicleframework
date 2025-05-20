package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Enums.Direction;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

public class TowHandler {
	private ActiveVehicle v;
	private Seat towPoint;
	private boolean reversed;
	
	public TowHandler(ConfigurationSection config) {
		towPoint = new Seat(SeatType.TOWING, config.getString("bone"));
		reversed = config.getBoolean("reversed", false);
	}
	
	public TowHandler(ActiveVehicle v, TowHandler another) {
		this.v = v;
		towPoint = new Seat(v, another.getTowPoint());
		reversed = another.isReversed();
	}
	
	public void tick() {  
	    if (!towPoint.isOccupied()) return;
	    
	    Vector align = v.getBehaviourHandler().getVector().getVector();
	    Entity e = towPoint.getEntity();
	    
	    if (e.isDead()) {
	        towPoint.dismount();
	        return;
	    }

	    Location loc = v.getModel().getBone(towPoint.getBone()).get().getLocation().clone();

	    float yaw = (float) Math.toDegrees(Math.atan2(-align.getX(), align.getZ()));

	    if (reversed) {
	        yaw += 180f;
	    }
	    yaw = ((yaw + 180) % 360 + 360) % 360 - 180;

	    loc.setYaw(yaw);
	    e.teleport(loc);
	}

	
	public Seat getTowPoint() {
		return towPoint;
	}
	
	public Location getTowLocation() {
		return v.getModel().getBone(towPoint.getBone()).get().getLocation().clone();
	}
	
	public boolean isReversed() {
		return reversed;
	}
	
	public boolean isOccupied() {
		return towPoint.isOccupied();
	}
	
	public void attach(ActiveVehicle target) {
		if(towPoint.isOccupied()) return;
		towPoint.mount(target, v);
		target.getBehaviourHandler().getRotator().reset();
	}
	
	public void unattach() {
		towPoint.dismount();
	}
	
	public void animate(Direction dir) {
		if(!towPoint.isOccupied()) return;
		if(!towPoint.mountedIsVehicle()) return;
		if(reversed) {
			switch(dir) {
				case FORWARD:
					dir = Direction.BACKWARD;
					break;
				case BACKWARD:
					dir = Direction.FORWARD;
					break;
				default:
					break;
			}
					
		}
		towPoint.getMountedVehicle().getMoveControls().animateMove(dir);
	}
}
