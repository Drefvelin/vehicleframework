package net.tfminecraft.VehicleFramework.Weapons;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.RotationLimits;
import net.tfminecraft.VehicleFramework.Data.DamageData;
import net.tfminecraft.VehicleFramework.Data.HealthData;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.InputHandler;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;
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

	protected RotationLimits limits;
	protected double turnRate;
	protected WeaponAimMode aimMode;
	protected double cursorRange;
	protected WeaponAimOffset aimOffset;
	protected String aimVector;
	protected Integer projectileDamage;
	protected String projectileDamageType;

	public static final double DEFAULT_CURSOR_RANGE = 80.0;
	
	@SuppressWarnings("unchecked")
	public Weapon(String key, ConfigurationSection config) {
		id = key;
		name = config.getString("name", key);
		fixed = config.getBoolean("fixed", false);
		seat = config.getString("seat", "gunner");
		if(!config.isConfigurationSection("data")) VFLogger.log(key+ " has no data section");
		weaponData = new WeaponData(config.getConfigurationSection("data"));
		healthData = new HealthData(config.getDouble("health", 100.0), 0, config.getInt("repair-time", 5));
		turnRate = config.getDouble("turn-rate", 0.5);
		aimMode = WeaponAimMode.fromConfig(config.getString("aim-mode", "manual"));
		cursorRange = config.getDouble("cursor-range", DEFAULT_CURSOR_RANGE);
		aimOffset = WeaponAimOffset.fromConfig(config.getConfigurationSection("aim-offset"));
		aimVector = config.getString("aim-vector", null);
		if (config.contains("projectile-damage")) {
			projectileDamage = config.getInt("projectile-damage");
		}
		if (config.contains("projectile-damage-type")) {
			projectileDamageType = config.getString("projectile-damage-type").toUpperCase();
		}
		damageData = new DamageData((List<String>) config.getList("damage", new ArrayList<String>()));
		if(config.isConfigurationSection("animations")) {
			animationHandler = new AnimationHandler(config.getConfigurationSection("animations"));
		} else {
			animationHandler = new AnimationHandler();
		}
		if(config.isConfigurationSection("keybinds")) {
			inputHandler = new InputHandler(config.getConfigurationSection("keybinds"));
		} else {
			VFLogger.log("weapon "+key+" has no keybinds section");
			inputHandler = new InputHandler();
		}
		if(!fixed) {
			bodyBone = config.getString("body-bone", "weapon_body");
			headBone = config.getString("head-bone", "cannon_controller");
			axis = config.getString("head-axis", "x");
			if(config.isConfigurationSection("rotation-limits")) {
				limits = new RotationLimits(config.getConfigurationSection("rotation-limits"));
			} else {
				limits = new RotationLimits();
			}
		}
		ammunitionHandler = new AmmunitionHandler(config);
		if(!config.contains("bones")) VFLogger.log(key+ " has no exit bones");
		bones = config.getStringList("bones");
	}

	public RotationLimits getLimits() {
		return limits;
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

	public double getTurnRate() {
		return turnRate;
	}

	public WeaponAimMode getAimMode() {
		return aimMode;
	}

	public double getCursorRange() {
		return cursorRange;
	}

	public WeaponAimOffset getAimOffset() {
		return aimOffset;
	}

	public String getAimVector() {
		return aimVector;
	}

	public Integer getProjectileDamage() {
		return projectileDamage;
	}

	public String getProjectileDamageType() {
		return projectileDamageType;
	}

	public static int effectiveDamage(ActiveWeapon weapon, AmmunitionData ammo) {
		if (weapon == null) {
			return effectiveDamage((Integer) null, ammo);
		}
		return weapon.effectiveDamage(ammo);
	}

	public static String effectiveDamageType(ActiveWeapon weapon, AmmunitionData ammo) {
		if (weapon == null) {
			return effectiveDamageType((String) null, ammo);
		}
		return weapon.effectiveDamageType(ammo);
	}

	public static int effectiveDamage(Integer projectileDamageOverride, AmmunitionData ammo) {
		if (projectileDamageOverride != null) {
			return projectileDamageOverride;
		}
		return ammo == null ? 0 : ammo.getDamage();
	}

	public static String effectiveDamageType(String projectileDamageTypeOverride, AmmunitionData ammo) {
		if (projectileDamageTypeOverride != null && !projectileDamageTypeOverride.isBlank()) {
			return projectileDamageTypeOverride;
		}
		return ammo == null ? "PROJECTILE" : ammo.getDamageType();
	}
	
}
