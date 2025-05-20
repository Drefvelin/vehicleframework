package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;

import me.Plugins.TLibs.Enums.NSEW;
import me.Plugins.TLibs.Utils.LocationUtil;
import net.tfminecraft.VehicleFramework.Enums.Direction;
import net.tfminecraft.VehicleFramework.Util.LocationChecker;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Train.Connector;

public class TrainHandler {
	protected ActiveVehicle v;
	protected ActiveVehicle child;
	
	private long cooldown = 0;
	
	private List<NSEW> dirs = new ArrayList<>();
	private List<Location> markers = new ArrayList<>();
	
	private Vector cached;
	
	private Connector front;
	private Connector back;
	
	public TrainHandler(ConfigurationSection config) {
		if(config.contains("front-connector")) {
			front = new Connector(config.getString("front-connector"));
		}
		if(config.contains("back-connector")) {
			back = new Connector(config.getString("back-connector"));
		}
	}
	
	public TrainHandler(ActiveVehicle v, TrainHandler another) {
		this.v = v;
		if(another.isAttachable()) {
			front = new Connector(v, another.getFront());
		}
		if(another.canHaveAttached()) {
			back = new Connector(v, another.getBack());
		}
	}
	
	public void updateModel(ActiveModel m) {
		if(isAttachable()) {
			front.updateModel(m);
		}
		if(canHaveAttached()) {
			back.updateModel(m);
		}
	}
	
	public boolean attach(Player p, ActiveVehicle target) {
		if(!isAttachable()) {
			p.sendMessage("§cThis train car cannot be attached to anything");
			return false;
		}
		if(v.hasParent()) {
			p.sendMessage("§cThis vehicle is already attached to something!");
			return false;
		}
		if(!target.getBehaviourHandler().isTrain()) {
			p.sendMessage("§cTarget vehicle is not a train type");
			return false;
		}
		TrainHandler handler = target.getBehaviourHandler().getTrainHandler();
		if(!handler.canHaveAttached()) {
			p.sendMessage("§cTarget train cannot have any cars attached");
			return false;
		}
		if(target.getTrainHandler().hasChild()) {
			p.sendMessage("§cTarget train already has a car attached");
			return false;
		}
		target.getTrainHandler().setChild(v);
		v.setParent(target);
		p.sendMessage("§aConnected car to train");
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		Location loc = getOffsetPosition(target);
		v.getEntity().teleport(loc);
		return true;
	}
	
	public boolean hasChild() {
		return child != null;
	}
	public ActiveVehicle getChild() {
		return child;
	}
	public void setChild(ActiveVehicle v) {
		if(this.v.equals(v)) return;
		child = v;
	}
	
	public boolean isAttachable() {
		return front != null;
	}
	
	public Connector getFront() {
		return front;
	}
	
	public boolean canHaveAttached() {
		return back != null;
	}
	
	public Connector getBack() {
		return back;
	}
	
	private double normalizeYaw(double yaw) {
	    yaw %= 360;
	    if (yaw < -180) {
	        yaw += 360;
	    }
	    if (yaw > 180) {
	        yaw -= 360;
	    }
	    return yaw;
	}
	
	public void clear() {
		markers.clear();
		if(hasChild()) child.getTrainHandler().clear();
	}
	
	public Location getOffsetPosition(ActiveVehicle parent) {
		Location loc = parent.getEntity().getLocation().clone();
		loc.add(parent.getTrainHandler().getBack().getOffset());
		loc.add(v.getTrainHandler().getFront().getOffset().multiply(-1));
		return loc;
	}

	public void animateMove(Direction dir) {
		v.getMoveControls().animateMove(dir);
    	if(hasChild()) child.getTrainHandler().animateMove(dir);
	}
	
	public void reverse(double speed) {
		ActiveVehicle end = v;
		while(end.getTrainHandler().hasChild()) {
			end = end.getTrainHandler().getChild();
		}
		Vector moveDirection = end.getLocation().getDirection().multiply(-1);
		end.getTrainHandler().animateMove(Direction.BACKWARD);
		end.getTrainHandler().trackedMove(speed, moveDirection, false);
		if(end.hasParent()) end.getParent().getTrainHandler().propagate(end, speed, markers, false);
	}
	
	public void retarget(Vector velocity) {
	    double speed = velocity.length()-0.49;
	    speed *= 10;
	    if(speed < 0.005) {
	    	animateMove(Direction.STILL);
	    	return;
	    }
	    Vector direction = v.getLocation().getDirection();
	    
	    double dot = velocity.clone().normalize().dot(direction.clone().normalize());
	    double angle = Math.toDegrees(Math.acos(dot));

	    // If angle > 90, train is moving backward
	    if (angle > 90) {
	        reverse(speed);
	        return;
	    }
	    
	    animateMove(Direction.FORWARD);
	    if(v.getTrainHandler().hasChild()) markers.add(v.getLocation().clone());
	    if (cooldown > System.currentTimeMillis()) return;
	    cooldown = System.currentTimeMillis() + 100;
	    trackedMove(speed, direction, true);
	    if(v.getTrainHandler().hasChild()) v.getTrainHandler().getChild().getTrainHandler().propagate(v, speed, markers, true);
	}

	public void trackedMove(double speed, Vector moveDirection, boolean forward) {
	    Entity e = v.getEntity();
	    Location start = e.getLocation();
	    dirs = LocationUtil.getProbableDirection(moveDirection, false);  // Get the initial probable direction
	    Location align = LocationChecker.getNextTrackedLocation(start, dirs, start.getYaw());
	    if(align == null) {
	    	if(cached != null) e.setVelocity(cached);
	    	return;
	    }

	    Vector direction = align.toVector().subtract(start.toVector());
	    direction.normalize();
	    

	    Vector finalVelocity = direction.multiply(speed);

	    // Compute world yaw based on movement direction
	    double computedYaw = Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
	    if (!forward) {
	        computedYaw += 180; // Flip the direction
	    }
	    computedYaw = normalizeYaw(computedYaw);

	    // Compute pitch normally
	    double pitch = Math.toDegrees(Math.asin(direction.getY()));
	    
	    Location newLoc = e.getLocation().clone();
	    newLoc.setYaw((float) computedYaw);
	    newLoc.setPitch((float) pitch);

	    e.teleport(newLoc);
	    if(align.getY() > start.getY()) {
	    	finalVelocity.setY(finalVelocity.getY()+0.3);
	    }
	    e.setVelocity(finalVelocity);
	    cached = finalVelocity;
	    
	    v.getBehaviourHandler().getRotator().rotateToTarget(0f, (float) pitch, 0f, 1f, false, true, false);
	    for(Player p : Bukkit.getOnlinePlayers()) {
	    	p.sendTitle(" ", "markers: "+markers.size(), 0, 20, 0);
	    }
	}
	public void propagate(ActiveVehicle target, double speed, List<Location> parentMarkers, boolean forward) {
		if(!markerMove(target, parentMarkers, speed, forward)) {
			Vector moveDirection = v.getLocation().getDirection().clone();
			if(!forward) moveDirection.multiply(-1);
			trackedMove(speed, v.getLocation().getDirection(), forward);
		}
		if(forward && v.getTrainHandler().hasChild()) {
			markers.add(v.getLocation().clone());
			v.getTrainHandler().getChild().getTrainHandler().propagate(v, speed, markers, true);
		}
		if(!forward && v.hasParent()) {
			markers.add(v.getLocation().clone());
			v.getParent().getTrainHandler().propagate(v, speed, markers, false);
		}
	}
	
	public boolean markerMove(ActiveVehicle target, List<Location> markers, double speed, boolean forward) {
		Vector childOffset = null;
		Vector parentOffset = null;
		if(forward) {
			childOffset = v.getBehaviourHandler().getTrainHandler().getFront().getOffset();
			parentOffset = target.getBehaviourHandler().getTrainHandler().getBack().getOffset();
		} else {
			for(Player p : Bukkit.getOnlinePlayers()) {
				p.sendMessage("active" + v.getName());
				p.sendMessage("target" + target.getName());
			}
			childOffset = v.getBehaviourHandler().getTrainHandler().getBack().getOffset();
			parentOffset = target.getBehaviourHandler().getTrainHandler().getFront().getOffset();
		}
		
		double length = Math.pow(childOffset.length()+parentOffset.length()-0.98, 2);
		
		for(int i = markers.size()-1; i >= 0; i--) {
			Location marker = markers.get(i);
			if(target.getLocation().distanceSquared(marker) > length) {
				Location current = v.getEntity().getLocation();
				Vector diff = marker.toVector().subtract(current.toVector());
				Location smoothLoc = current.clone().add(diff.multiply(0.1)); // slight position correction
				smoothLoc.setYaw(marker.getYaw());
				smoothLoc.setPitch(marker.getPitch());
				v.getEntity().teleport(smoothLoc);

				Vector finalVelocity = diff.clone().normalize().multiply(speed);
				v.getEntity().setVelocity(finalVelocity);
				v.getBehaviourHandler().getRotator().rotateToTarget(0f, marker.getPitch(), 0f, 1f, false, true, false);
				if (i > 0) {
		            markers.subList(0, i).clear();
		        }
				return true;
			}
			
		}
		
		return false;
	}
}
