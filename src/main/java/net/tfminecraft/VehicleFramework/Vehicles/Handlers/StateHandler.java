package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Util.LocationChecker;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Controller.VehicleMovementController;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.State.Parameter;
import net.tfminecraft.VehicleFramework.Vehicles.State.VehicleState;

public class StateHandler {
	private ActiveVehicle vehicle;
	
	private VehicleState state;
	
	private HashMap<State, VehicleState> states = new HashMap<>();
	
	public StateHandler(ConfigurationSection config) {
		for (State state : State.values()) {
	        String enumString = state.name().toLowerCase();
	        if (config.contains(enumString)) {
	            states.put(state, new VehicleState(state, config.getConfigurationSection(enumString)));
	        }
	    }
	}
	
	public StateHandler(ActiveVehicle vehicle, StateHandler another) {
		this.vehicle = vehicle;
		for(Map.Entry<State, VehicleState> entry : another.getStateMap().entrySet()) {
			states.put(entry.getKey(), new VehicleState(vehicle, entry.getValue()));
		}
		for(State state : State.values()) {
			if(!states.containsKey(state)) states.put(state, new VehicleState(state, vehicle));
		}
	}
	
	public void updateModel(ActiveModel m) {
		for(Map.Entry<State, VehicleState> state : states.entrySet()) {
			state.getValue().getAnimationHandler().updateModel(m);
			state.getValue().getMoveControls().update(vehicle);
		}
	}
	
	public void setState(State s) {
		if(vehicle.isDestroyed()) return;
		if(!hasState(s)) return;
		state = states.get(s);
		state.getAnimationHandler().animate(Animation.DEFAULT);
		if(state.hasSwitchParameter()) {
			/*
			//For debugging parameters
			for(Player p : Bukkit.getOnlinePlayers()) {
				p.sendMessage("§eState parameters");
				for(Map.Entry<String, Parameter> entry : state.getSwitchParameter().getParameters().entrySet()) {
					p.sendMessage("§a"+entry.getKey()+": §cmin: §e"+entry.getValue().getMin()+", max: §e"+entry.getValue().getMax()+" §7Current: §e"+vehicle.getParameterValue(entry.getKey()));
				}
			}
			*/
			for(Map.Entry<String, Parameter> entry : state.getSwitchParameter().getParameters().entrySet()) {
				Parameter param = entry.getValue();
				if(!param.isWithin(vehicle.getParameterValue(entry.getKey()))) {
					vehicle.kill(VehicleDeath.EXPLODE);
					/*
					for(Player p : Bukkit.getOnlinePlayers()) {
						p.sendMessage("§4PARAMETER " + entry.getKey() + " EXCEEDED!!");
					}
					*/
					return;
				}
			}
		}
	}
	public VehicleState getCurrentState() {
		return state;
	}

	// Tick
	public void tick() {
	    if (state != null) state.getMoveControls().setAnimation();
	    
	    Entity e = vehicle.getEntity();
	    BoundingBox box = e.getBoundingBox();

	    Set<Block> blocks = getBlocksBoundingBox(box, e.getWorld(), 0.0);
	    Set<Block> blocksBelow = getBlocksBoundingBox(box, e.getWorld(), -0.3);

	    if (isMostlyWater(blocks, 0.75)) {
			swapState(State.FLOATING);
		} else if (checkAllBlocks(blocksBelow, "air")) {
	        swapState(State.FLYING);
	    } else {
	        swapState(State.GROUND);
	    }
	}

	private boolean isMostlyWater(Set<Block> blocks, double requiredFraction) {
		int waterCount = 0;

		for (Block block : blocks) {
			if (LocationChecker.isInWater(block.getLocation())) {
				waterCount++;
			}
		}

		double fraction = (double) waterCount / blocks.size();
		return fraction >= requiredFraction;
	}

	private Set<Block> getBlocksBoundingBox(BoundingBox box, World world, double yOffset) {
		Set<Block> blocks = new HashSet<>();

		int minX = (int) Math.floor(box.getMinX());
		int maxX = (int) Math.floor(box.getMaxX());
		int minZ = (int) Math.floor(box.getMinZ());
		int maxZ = (int) Math.floor(box.getMaxZ());

		double y = box.getMinY() + yOffset;
		int yInt = (int) Math.floor(y);

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				blocks.add(world.getBlockAt(x, yInt, z));
			}
		}

		return blocks;
	}

	private boolean checkAllBlocks(Set<Block> blocks, String type) {
		if(type.equalsIgnoreCase("air")) {
			for (Block block : blocks) {
		        if(!LocationChecker.isInAir(block.getLocation())) return false;
		    }
		} else if(type.equalsIgnoreCase("water")) {
			for (Block block : blocks) {
		        if(!LocationChecker.isInWater(block.getLocation())) return false;
		    }
		}
	    
	    return true;
	}
	

	
	private void swapState(State s) {
		if(!hasState(s)) return;
		if(states.get(s) == state) return;
		if(state != null) state.getAnimationHandler().stopAllAnimations();
		
		
		Entity e = vehicle.getEntity();
		Location loc = e.getLocation().clone().add(0, -0.5, 0);
		//For debugging state swap
		VFLogger.creatorLog("Swapped state to "+s.toString()+" due to "+loc.getBlock().getType().toString());
		
		
		setState(s);
	}
	
	//Currentstate getters and methods
	
	public void key(Player p, Keybind key) {
		state.key(p, key);
	}
	
	public AnimationHandler getAnimationHandler() {
		return state.getAnimationHandler();
	}
	
	public VehicleMovementController getMoveControls() {
		return state.getMoveControls();
	}
	
	public boolean hasState(State s) {
		return states.containsKey(s);
	}
	
	public VehicleState getVehicleState(State s) {
		if(!hasState(s)) return null;
		return states.get(s);
	}
	
	public HashMap<State, VehicleState> getStateMap(){
		return states;
	}
}
