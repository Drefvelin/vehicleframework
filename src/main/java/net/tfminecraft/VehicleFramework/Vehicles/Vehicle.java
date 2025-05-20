package net.tfminecraft.VehicleFramework.Vehicles;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Data.DamageData;
import net.tfminecraft.VehicleFramework.Data.DeathData;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.BehaviourHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.ComponentHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.EffectHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.SeatHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.SkinHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.StateHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TowHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.UtilityHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.InputHandler;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;

public class Vehicle {
	
	//Vehicle class is for storing information loaded from the config, for loaded vehicles in the world see ActiveVehicle
	
	//Basic
	protected String id;
	protected String name;
	protected String model;
	
	protected boolean fixed;
	
	protected boolean towable;
	
	//Components
	protected ComponentHandler componentHandler;
	
	//Seats
	protected SeatHandler seatHandler;
	
	//States
	protected StateHandler stateHandler;
	
	//Effects
	protected EffectHandler effectHandler;
	
	//Behaviour
	protected BehaviourHandler behaviourHandler;
	
	//Skins
	protected SkinHandler skinHandler;
	
	//Towing
	protected TowHandler towHandler;
	
	//Utilities
	protected UtilityHandler utilityHandler;
	
	//Weapons
	protected List<Weapon> weapons = new ArrayList<>();
	
	//Data
	protected List<DeathData> deathData = new ArrayList<>();
	
	
	public Vehicle(String key, ConfigurationSection config) {
		id = key;
		name = config.getString("name", key);
		fixed = config.getBoolean("fixed", false);
		towable = config.getBoolean("towable", false);
		if(!config.contains("model")) VFLogger.log(key+" does not contain a model field");
		model = config.getString("model");
		if(!config.isConfigurationSection("skins")) VFLogger.log(key+" has no skins section");
		skinHandler = new SkinHandler(model, config.getConfigurationSection("skins"));
		
		if(!config.isConfigurationSection("states")) VFLogger.log(key+" has no states section");
		stateHandler = new StateHandler(config.getConfigurationSection("states"));
		
		if(!config.isConfigurationSection("components")) VFLogger.log(key+" has no components section");
		componentHandler = new ComponentHandler(config.getConfigurationSection("components"));
		
		if(!config.contains("seats")) VFLogger.log(key+" has no seats section");
		seatHandler = new SeatHandler(config.getStringList("seats"), this);
		
		if(config.isConfigurationSection("custom-effects")) {
			effectHandler = new EffectHandler(config.getConfigurationSection("custom-effects"));
		} else {
			effectHandler = new EffectHandler();
		}
		if(config.isConfigurationSection("behaviour")) {
			behaviourHandler = new BehaviourHandler(config.getConfigurationSection("behaviour"));
		} else {
			behaviourHandler = new BehaviourHandler();
		}
		if(config.isConfigurationSection("weapons")) {
			ConfigurationSection weaponConfig = config.getConfigurationSection("weapons");
			Set<String> set = weaponConfig.getKeys(false);

			List<String> list = new ArrayList<String>(set);
			for(String w : list) {
				weapons.add(new Weapon(w, weaponConfig.getConfigurationSection(w)));
			}
		}
		if(!config.isConfigurationSection("death")) VFLogger.log(key+" has death section");
		loadDeathData(config.getConfigurationSection("death"));
		
		if(config.isConfigurationSection("towing")) {
			towHandler = new TowHandler(config.getConfigurationSection("towing"));
		}
		if(config.isConfigurationSection("utilities")) {
			utilityHandler = new UtilityHandler(config.getConfigurationSection("utilities"));
		}
	}
	
	private void loadDeathData(ConfigurationSection config) {
		if(config.isConfigurationSection("explode")) {
			ConfigurationSection explode = config.getConfigurationSection("explode");
			deathData.add(new DeathData(VehicleDeath.EXPLODE, explode));
		}
		if(config.isConfigurationSection("sink")) {
			ConfigurationSection sink = config.getConfigurationSection("sink");
			deathData.add(new DeathData(VehicleDeath.SINK, sink));
		}
		if(config.isConfigurationSection("crash")) {
			ConfigurationSection crash = config.getConfigurationSection("crash");
			deathData.add(new DeathData(VehicleDeath.CRASH, crash));
		}
	}


	public String getId() {
		return id;
	}


	public String getName() {
		return name;
	}


	public String getModel() {
		return model;
	}
	
	public boolean isFixed() {
		return fixed;
	}
	
	public boolean isTowable() {
		return towable;
	}


	public ComponentHandler getComponentHandler() {
		return componentHandler;
	}


	public SeatHandler getSeatHandler() {
		return seatHandler;
	}
	
	public StateHandler getStateHandler() {
		return stateHandler;
	}
	
	public EffectHandler getEffectHandler() {
		return effectHandler;
	}
	
	public BehaviourHandler getBehaviourHandler() {
		return behaviourHandler;
	}
	
	public SkinHandler getSkinHandler() {
		return skinHandler;
	}
	
	public TowHandler getTowHandler() {
		return towHandler;
	}
	
	public UtilityHandler getUtilityHandler() {
		return utilityHandler;
	}

	public List<Weapon> getWeapons() {
		return weapons;
	}

	public List<DeathData> getDeathData() {
		return deathData;
	}
	
	
}
