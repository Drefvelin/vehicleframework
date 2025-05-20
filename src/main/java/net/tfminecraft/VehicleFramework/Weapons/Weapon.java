package net.tfminecraft.VehicleFramework.Weapons;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Data.DamageData;
import net.tfminecraft.VehicleFramework.Data.HealthData;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.InputHandler;
import net.tfminecraft.VehicleFramework.Weapons.Data.WeaponData;
import net.tfminecraft.VehicleFramework.Weapons.Handlers.AmmunitionHandler;

public class Weapon {
	protected String id;
	protected String name;
	
	protected WeaponData weaponData;
	protected HealthData healthData;
	protected DamageData damageData;
	
	protected boolean fixed;
	protected String bodyBone;
	protected String headBone;
	protected String axis;
	
	protected String seat;
	
	protected AnimationHandler animationHandler;
	protected InputHandler inputHandler;
	
	protected AmmunitionHandler ammunitionHandler;
	protected List<String> bones;
	
	@SuppressWarnings("unchecked")
	public Weapon(String key, ConfigurationSection config) {
		id = key;
		name = config.getString("name", key);
		fixed = config.getBoolean("fixed", false);
		seat = config.getString("seat", "gunner");
		if(!config.isConfigurationSection("data")) VFLogger.log(key+ " has no data section");
		weaponData = new WeaponData(config.getConfigurationSection("data"));
		healthData = new HealthData(config.getDouble("health", 100.0), 0, config.getInt("repair-time", 5));
		damageData = new DamageData((List<String>) config.getList("damage", new ArrayList<String>()));
		if(config.isConfigurationSection("animations")) {
			animationHandler = new AnimationHandler(config.getConfigurationSection("animations"));
		} else {
			animationHandler = new AnimationHandler();
		}
		if(!config.isConfigurationSection("keybinds")) VFLogger.log("weapon "+key+" has no keybinds section");
		inputHandler = new InputHandler(config.getConfigurationSection("keybinds"));
		if(!fixed) {
			bodyBone = config.getString("body-bone", "weapon_body");
			headBone = config.getString("head-bone", "cannon_controller");
			axis = config.getString("head-axis", "x");
		}
		ammunitionHandler = new AmmunitionHandler(config);
		if(!config.contains("bones")) VFLogger.log(key+ " has no exit bones");
		bones = config.getStringList("bones");
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public WeaponData getWeaponData() {
		return weaponData;
	}
	public HealthData getHealthData() {
		return healthData;
	}
	public DamageData getDamageData() {
		return damageData;
	}
	
	public boolean isFixed() {
		return fixed;
	}

	public String getBodyBone() {
		return bodyBone;
	}

	public String getHeadBone() {
		return headBone;
	}
	
	public String getSeat() {
		return seat;
	}
	
	public AnimationHandler getAnimationHandler() {
		return animationHandler;
	}
	
	public InputHandler getInputHandler() {
		return inputHandler;
	}

	public AmmunitionHandler getAmmunitionHandler() {
		return ammunitionHandler;
	}

	public List<String> getBones() {
		return bones;
	}

	public String getAxis() {
		return axis;
	}
	
}
