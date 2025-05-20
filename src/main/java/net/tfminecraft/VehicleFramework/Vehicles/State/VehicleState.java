package net.tfminecraft.VehicleFramework.Vehicles.State;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Controller.VehicleMovementController;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.ClimbHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.InputHandler;

public class VehicleState {
	protected State type;
	
	protected boolean isDefault;
	
	//Controllers
	protected VehicleMovementController moveControls;
		
	//Animations
	protected AnimationHandler animationHandler;
			
	//Inputs
	protected InputHandler inputHandler;
	
	//Movement
	protected ClimbHandler climbHandler;
	
	protected SwitchParameter switchParameter;
	
	public VehicleState(State type, ConfigurationSection config) {
		this.type = type;
		if(config.isConfigurationSection("animations")) {
			animationHandler = new AnimationHandler(config.getConfigurationSection("animations"));
		} else {
			animationHandler = new AnimationHandler();
		}
		
		if(config.isConfigurationSection("keybinds")) {
			inputHandler = new InputHandler(config.getConfigurationSection("keybinds"));
		} else {
			inputHandler = new InputHandler();
		}
		
		if(config.isConfigurationSection("switch-parameters")) {
			switchParameter = new SwitchParameter(config.getConfigurationSection("switch-parameters"));
		}
		climbHandler = new ClimbHandler(config);
	}
	
	public VehicleState(ActiveVehicle vehicle, VehicleState another) {
		type = another.getType();
		isDefault = false;
		inputHandler = new InputHandler(another.getInputHandler());
		animationHandler = new AnimationHandler(vehicle.getModel(), another.getAnimationHandler());
		moveControls = new VehicleMovementController(vehicle, this);
		switchParameter = another.getSwitchParameter();
		climbHandler = another.getClimbHandler();
	}
	
	public VehicleState(State type, ActiveVehicle vehicle) {
		this.type = type;
		isDefault = true;
		inputHandler = new InputHandler();
		animationHandler = new AnimationHandler();
		moveControls = new VehicleMovementController(vehicle, this);
		climbHandler = new ClimbHandler();
	}
	
	public State getType() {
		return type;
	}
	
	public boolean hasSwitchParameter() {
		return switchParameter != null;
	}
	
	public SwitchParameter getSwitchParameter() {
		return switchParameter;
	}
	
	public boolean isDefault() {
		return isDefault;
	}
	
	public void key(Player p, Keybind key) {
		moveControls.input(p, inputHandler.getInput(key));
	}
	
	public VehicleMovementController getMoveControls() {
		return moveControls;
	}
	
	public AnimationHandler getAnimationHandler() {
		return animationHandler;
	}
	
	public InputHandler getInputHandler() {
		return inputHandler;
	}
	
	public ClimbHandler getClimbHandler() {
		return climbHandler;
	}
}
