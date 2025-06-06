package net.tfminecraft.VehicleFramework.Weapons;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Bones.RotationLimits;
import net.tfminecraft.VehicleFramework.Data.DamageData;
import net.tfminecraft.VehicleFramework.Data.HealthData;
import net.tfminecraft.VehicleFramework.Database.IncompleteWeapon;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.InputHandler;
import net.tfminecraft.VehicleFramework.Weapons.Controller.WeaponMovementController;
import net.tfminecraft.VehicleFramework.Weapons.Data.WeaponData;
import net.tfminecraft.VehicleFramework.Weapons.Handlers.AmmunitionHandler;

public class ActiveWeapon {
	protected String uuid;
	
	protected String id;
	protected String name;
	
	protected WeaponData weaponData;
	protected HealthData healthData;
	protected DamageData damageData;
	
	protected AmmunitionHandler ammunitionHandler;
	protected AnimationHandler animationHandler;
	protected InputHandler inputHandler;
	protected WeaponMovementController moveControls;
	
	protected String seat;
	
	protected Player controller;
	
	public ActiveWeapon(ActiveModel m, ActiveVehicle vehicle, Weapon stored, IncompleteWeapon i) {
		uuid = UUID.randomUUID().toString();
		id = stored.getId();
		name = stored.getName();
		weaponData = stored.getWeaponData();
		healthData = new HealthData(stored.getHealthData().getHealth(), stored.getHealthData().getDamage(), stored.getHealthData().getBaseRepairTime());
		damageData = stored.getDamageData();
		ammunitionHandler = new AmmunitionHandler(m, vehicle, this, stored.getAmmunitionHandler(), stored.getBones());
		animationHandler = new AnimationHandler(m, stored.getAnimationHandler());
		inputHandler = new InputHandler(stored.getInputHandler());
		moveControls = new WeaponMovementController(vehicle, m, this, stored, stored.getLimits());
		seat = stored.getSeat();
		
	}
	
	public void updateModel(ActiveModel m) {
		ammunitionHandler.updateModel(m);
		animationHandler.updateModel(m);
		moveControls.updateModel(m);
	}
	
	public boolean isControlled() {
		return controller != null;
	}
	public Player getController() {
		return controller;
	}
	public void setController(Player p) {
		controller = p;
	}
	public void disconnect() {
		controller = null;
	}
	
	public void input(List<Player> nearby, Keybind key) {
		if(!isControlled()) return;
		moveControls.input(nearby, inputHandler.getInput(key), controller);
	}
	
	public void tick() {
		if(!isControlled()) {
			moveControls.normalize();
		}
		moveControls.move();
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
	
	public void damage(String cause, double a) {
		if(damageData.hasModifier(cause)) {
			a = a*damageData.getModifier(cause);
		}
		healthData.damage(a);
	}
}
