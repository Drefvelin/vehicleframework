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
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Bullet;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.ClusterBomb;
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
	protected Double projectileSpeed;
	protected Float projectileYield;
	protected Integer projectileRadius;
	protected Boolean projectileExplosive;
	protected Integer projectileClusterAmount;

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
		if (config.contains("projectile-speed")) {
			projectileSpeed = config.getDouble("projectile-speed");
		} else if (config.contains("projectile-velocity")) {
			projectileSpeed = config.getDouble("projectile-velocity");
		}
		if (config.contains("projectile-yield")) {
			projectileYield = (float) config.getDouble("projectile-yield");
		}
		if (config.contains("projectile-radius")) {
			projectileRadius = config.getInt("projectile-radius");
		}
		if (config.contains("projectile-explosive")) {
			projectileExplosive = config.getBoolean("projectile-explosive");
		}
		if (config.contains("projectile-cluster-amount")) {
			projectileClusterAmount = config.getInt("projectile-cluster-amount");
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

	public Double getProjectileSpeed() {
		return projectileSpeed;
	}

	public Float getProjectileYield() {
		return projectileYield;
	}

	public Integer getProjectileRadius() {
		return projectileRadius;
	}

	public Boolean getProjectileExplosive() {
		return projectileExplosive;
	}

	public Integer getProjectileClusterAmount() {
		return projectileClusterAmount;
	}

	public static float effectiveYield(ActiveWeapon weapon, AmmunitionData ammo) {
		if (weapon == null) {
			return effectiveYield((Float) null, ammo);
		}
		return effectiveYield(weapon.projectileYield, ammo);
	}

	public static float effectiveYield(Float override, AmmunitionData ammo) {
		if (override != null) {
			return override;
		}
		return ammo == null ? 0f : ammo.getYield();
	}

	public static int effectiveRadius(ActiveWeapon weapon, AmmunitionData ammo) {
		if (weapon == null) {
			return effectiveRadius((Integer) null, ammo);
		}
		return effectiveRadius(weapon.projectileRadius, ammo);
	}

	public static int effectiveRadius(Integer override, AmmunitionData ammo) {
		if (override != null) {
			return override;
		}
		return ammo == null ? 0 : ammo.getRadius();
	}

	public static boolean effectiveExplosive(ActiveWeapon weapon, AmmunitionData ammo) {
		if (weapon == null) {
			return effectiveExplosive((Boolean) null, ammo);
		}
		return effectiveExplosive(weapon.projectileExplosive, ammo);
	}

	public static boolean effectiveExplosive(Boolean override, AmmunitionData ammo) {
		if (override != null) {
			return override;
		}
		return ammo != null && ammo.isExplosive();
	}

	public static int effectiveClusterAmount(ActiveWeapon weapon, ClusterBomb cluster) {
		if (weapon == null) {
			return effectiveClusterAmount((Integer) null, cluster);
		}
		return effectiveClusterAmount(weapon.projectileClusterAmount, cluster);
	}

	public static int effectiveClusterAmount(Integer override, ClusterBomb cluster) {
		if (override != null) {
			return override;
		}
		return cluster == null ? 0 : cluster.getAmount();
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

	public static double effectiveProjectileSpeed(ActiveWeapon weapon, Bullet bullet) {
		if (weapon == null) {
			return effectiveProjectileSpeed((Double) null, bullet);
		}
		return weapon.effectiveBulletSpeed(bullet);
	}

	public static double effectiveProjectileVelocity(ActiveWeapon weapon) {
		if (weapon == null) {
			return effectiveProjectileSpeed((Double) null, WeaponData.DEFAULT_VELOCITY);
		}
		return weapon.effectiveProjectileVelocity();
	}

	public static double effectiveProjectileSpeed(Double projectileSpeedOverride, Bullet bullet) {
		return effectiveProjectileSpeed(projectileSpeedOverride, bullet == null ? Bullet.DEFAULT_SPEED : bullet.getSpeed());
	}

	public static double effectiveProjectileSpeed(Double projectileSpeedOverride, double fallback) {
		if (projectileSpeedOverride != null) {
			return projectileSpeedOverride;
		}
		return fallback;
	}
	
}
