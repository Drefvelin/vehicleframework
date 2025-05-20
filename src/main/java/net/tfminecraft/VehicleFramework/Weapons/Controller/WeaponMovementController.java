package net.tfminecraft.VehicleFramework.Weapons.Controller;

import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;

public class WeaponMovementController{
	
	protected HashMap<Player, Long> cooldown = new HashMap<>();
	
	protected ActiveVehicle v; 
	
	protected ActiveWeapon w;
	
	protected boolean fixed;
	
	protected BoneRotator bodyRotator;
	
	protected BoneRotator headRotator;
	
	protected String axis;
	
	public WeaponMovementController(ActiveVehicle v, ActiveModel m, ActiveWeapon w, Weapon another) {
		this.v = v;
		this.w = w;
		this.fixed = another.isFixed();
		if(!fixed) initiateBones(m, another.getBodyBone(), another.getHeadBone(), another.getAxis());
	}
	
	private void initiateBones(ActiveModel m, String body, String head, String axis) {
		if(m.getBone(body).isEmpty()) {
			VFLogger.log("No bone detected for value: "+body);
			return;
		}
		if(v.getAccessPanel().getRotator(body) != null) {
			bodyRotator = v.getAccessPanel().getRotator(body);
		} else {
			bodyRotator = new BoneRotator(v, v.getEntity(), m.getBone(body).get());
		}
		
		
		if(m.getBone(head).isEmpty()) {
			VFLogger.log("No bone detected for value: "+head);
			return;
		}
		if(v.getAccessPanel().getRotator(head) != null) {
			headRotator = v.getAccessPanel().getRotator(head);
		} else {
			headRotator = new BoneRotator(v, v.getEntity(), m.getBone(head).get());
		}
		if(axis == null) {
			VFLogger.log("weapon has no axis");
			return;
		}
		this.axis = axis;
	}
	
	public void updateModel(ActiveModel m) {
		if(bodyRotator != null) bodyRotator.updateModel(m);
		if(headRotator != null) headRotator.updateModel(m);
	}
	
	public ModelBone getBodyBone() {
		return bodyRotator.getBone();
	}
	public ModelBone getHeadBone() {
		return headRotator.getBone();
	}
	
	public void input(List<Player> nearby, Input i, Player p) {
		switch(i) {
			case WEAPON_UP:
				inputUp();
				break;
			case WEAPON_LEFT:
				inputLeft();
				break;
			case WEAPON_DOWN:
				inputDown();
				break;
			case WEAPON_RIGHT:
				inputRight();
				break;
			case WEAPON_SHOOT:
				inputShoot(p, nearby);
				break;
			case WEAPON_RELOAD:
				inputReload(p, true);
				break;
			case WEAPON_RELOAD_AND_SHOOT:
				inputReload(p, false);
				inputShoot(p, nearby);
				break;
			case WEAPON_SWITCH:
				Seat seat = v.getSeat(p);
				seat.changeWeapon();
				v.updateBoard();
				break;
			default:
				break;
			
		}
	}
	
	public void move() {
		if(headRotator != null) headRotator.rotateSmoothed(0, 0, 0);
		if(bodyRotator != null) bodyRotator.rotateSmoothed(0, 0, 0);
	}

	private void inputUp() {
		if(axis.equalsIgnoreCase("z")) {
			headRotator.rotateSmoothed(0, 0, -0.5f);
		} else if(axis.equalsIgnoreCase("x")) {
			headRotator.rotateSmoothed(-0.5f, 0, 0);
		} else {
			headRotator.rotateSmoothed(0, -0.5f, 0);
		}
	}

	private void inputLeft() {
		bodyRotator.rotateSmoothed(0, 0.5f, 0);
	}

	private void inputDown() {
		if(axis.equalsIgnoreCase("z")) {
			headRotator.rotateSmoothed(0, 0, 0.5f);
		} else if(axis.equalsIgnoreCase("x")) {
			headRotator.rotateSmoothed(0.5f, 0, 0);
		} else {
			headRotator.rotateSmoothed(0, 0.5f, 0);
		}
	}

	private void inputRight() {
		bodyRotator.rotateSmoothed(0, -0.5f, 0);
	}

	private void inputReload(Player p, boolean message) {
		w.getAmmunitionHandler().load(p, p.getInventory().getItemInMainHand(), message);
	}

	private void inputShoot(Player p, List<Player> nearby) {
		w.getAmmunitionHandler().shoot(p, nearby);
	}
	
	public void normalize() {
		//if(bodyRotator != null ) bodyRotator.normalize(true, true, true);
		//if(headRotator != null) headRotator.normalize(true, true, true);
	}

}
