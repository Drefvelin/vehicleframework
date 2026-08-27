package net.tfminecraft.VehicleFramework.Weapons.Controller;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Bones.RotationLimits;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;
import net.tfminecraft.VehicleFramework.Weapons.WeaponAimAligner;
import net.tfminecraft.VehicleFramework.Weapons.WeaponAimMode;
import net.tfminecraft.VehicleFramework.Weapons.WeaponAimOffset;
import net.tfminecraft.VehicleFramework.Weapons.WeaponPerformance;
import net.tfminecraft.VehicleFramework.Weapons.WeaponTargetResolver;

public class WeaponMovementController{
	
	protected HashMap<Player, Long> cooldown = new HashMap<>();
	
	protected ActiveVehicle v; 
	
	protected ActiveWeapon w;
	
	protected boolean fixed;
	
	protected BoneRotator bodyRotator;
	
	protected BoneRotator headRotator;
	
	protected VectorBone aimVector;
	
	protected String axis;
	protected double baseTurnRate;
	protected WeaponAimMode aimMode;
	protected double cursorRange;
	protected RotationLimits limits;
	protected WeaponAimOffset aimOffset;
	protected boolean cursorAimMissingVector;
	private long lastAimDebugLog;
	private boolean yawSettled;
	private boolean elevationSettled;
	
	public WeaponMovementController(ActiveVehicle v, ActiveModel m, ActiveWeapon w, Weapon another, RotationLimits limits) {
		this.v = v;
		this.w = w;
		this.baseTurnRate = another.getTurnRate();
		this.fixed = another.isFixed();
		this.aimMode = another.getAimMode();
		this.cursorRange = another.getCursorRange();
		this.limits = limits;
		this.aimOffset = another.getAimOffset();
		if (fixed && aimMode == WeaponAimMode.CURSOR) {
			VFLogger.log("Weapon " + another.getId() + " is fixed; cursor aim-mode is ignored");
		}
		if(!fixed) {
			initiateBones(m, another.getBodyBone(), another.getHeadBone(), another.getAxis(), limits);
			initiateAimVector(m, another);
		}
	}
	
	private void initiateBones(ActiveModel m, String body, String head, String axis, RotationLimits limits) {
		if(m.getBone(body).isEmpty()) {
			VFLogger.log("No bone detected for value: "+body);
			return;
		}
		if(v.getAccessPanel().getRotator(body) != null) {
			bodyRotator = v.getAccessPanel().getRotator(body);
		} else {
			bodyRotator = new BoneRotator(v, v.getEntity(), m.getBone(body).get(), limits);
		}
		
		
		if(m.getBone(head).isEmpty()) {
			VFLogger.log("No bone detected for value: "+head);
			return;
		}
		if(v.getAccessPanel().getRotator(head) != null) {
			headRotator = v.getAccessPanel().getRotator(head);
		} else {
			headRotator = new BoneRotator(v, v.getEntity(), m.getBone(head).get(), limits);
		}
		if(axis == null) {
			VFLogger.log("weapon has no axis");
			return;
		}
		this.axis = axis;
	}

	private void initiateAimVector(ActiveModel m, Weapon weapon) {
		String spec = weapon.getAimVector();
		if (spec == null || spec.isBlank()) {
			List<String> bones = weapon.getBones();
			if (bones == null || bones.isEmpty()) {
				if (weapon.getAimMode() == WeaponAimMode.CURSOR) {
					VFLogger.log("Weapon " + weapon.getId() + " has cursor aim but no aim-vector or bones entry");
					cursorAimMissingVector = true;
				}
				return;
			}
			spec = bones.get(0);
			VFLogger.log("Weapon " + weapon.getId() + " has no aim-vector; falling back to first bones entry: " + spec);
		}
		aimVector = createVectorBone(m, weapon.getId(), spec);
		if (aimVector == null && weapon.getAimMode() == WeaponAimMode.CURSOR) {
			cursorAimMissingVector = true;
		}
	}

	private VectorBone createVectorBone(ActiveModel m, String weaponId, String spec) {
		if (!spec.contains(".")) {
			VFLogger.log(weaponId + " has an invalid aim-vector: " + spec);
			return null;
		}
		String[] parts = spec.split("\\.", 2);
		String base = parts[0];
		String alignment = parts[1];
		if (m.getBone(base).isEmpty()) {
			VFLogger.log(weaponId + " has an invalid aim-vector base bone " + base);
			return null;
		}
		if (m.getBone(alignment).isEmpty()) {
			VFLogger.log(weaponId + " has an invalid aim-vector alignment bone " + alignment);
			return null;
		}
		return new VectorBone(m.getBone(base).get(), m.getBone(alignment).get());
	}
	
	public void updateModel(ActiveModel m) {
		if(bodyRotator != null) bodyRotator.updateModel(m);
		if(headRotator != null) headRotator.updateModel(m);
		if(aimVector != null) aimVector.updateModel(m);
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
				if (aimMode != WeaponAimMode.CURSOR) inputUp();
				break;
			case WEAPON_LEFT:
				if (aimMode != WeaponAimMode.CURSOR) inputLeft();
				break;
			case WEAPON_DOWN:
				if (aimMode != WeaponAimMode.CURSOR) inputDown();
				break;
			case WEAPON_RIGHT:
				if (aimMode != WeaponAimMode.CURSOR) inputRight();
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
		if (aimMode == WeaponAimMode.CURSOR) {
			return;
		}
		if(headRotator != null) headRotator.rotateSmoothed(0, 0, 0);
		if(bodyRotator != null) bodyRotator.rotateSmoothed(0, 0, 0);
	}

	public void trackCursor(Player player) {
		if (fixed || aimMode != WeaponAimMode.CURSOR || player == null
				|| bodyRotator == null || headRotator == null
				|| aimVector == null || cursorAimMissingVector) {
			return;
		}

		Location target = WeaponTargetResolver.resolveTarget(player, v, cursorRange);
		if (target == null) {
			return;
		}

		if (Cache.weaponAimDebug && player.getWorld() != null && target.getWorld() != null
				&& player.getWorld().equals(target.getWorld())) {
			player.spawnParticle(Particle.END_ROD, target, 1, 0, 0, 0, 0);
		}

		Location baseLocation = aimVector.getBaseLocation();
		if (baseLocation == null) {
			return;
		}

		Vector desired = target.toVector().subtract(baseLocation.toVector());
		if (desired.lengthSquared() < 1e-12) {
			return;
		}
		desired.normalize();
		WeaponAimOffset offset = aimOffset == null ? WeaponAimOffset.ZERO : aimOffset;
		ConvertedAngle desiredAngles = offset.applyToAngles(ConvertedAngle.fromDirection(desired), axis);

		Vector current = aimVector.getVector();
		if (current == null || current.lengthSquared() < 1e-12) {
			return;
		}

		float rate = effectiveTurnRate();
		float yawErr = WeaponAimAligner.yawError(current, desiredAngles);
		float elevErr = WeaponAimAligner.elevationError(current, desiredAngles, axis);
		float boneYawErr = WeaponAimAligner.toBoneYawStep(yawErr);
		float bodyStep = WeaponAimAligner.followStep(boneYawErr, rate, yawSettled);
		if (bodyStep != 0f) {
			bodyRotator.rotate(0, bodyStep, 0);
		} else {
			bodyRotator.resetSmoothing();
		}
		yawSettled = WeaponAimAligner.updateSettled(boneYawErr, yawSettled);

		Vector afterBody = aimVector.getVector();
		if (afterBody == null || afterBody.lengthSquared() < 1e-12) {
			return;
		}
		float boneElevErr = WeaponAimAligner.toBoneElevationStep(
				WeaponAimAligner.elevationError(afterBody, desiredAngles, axis));
		float headStep = WeaponAimAligner.followStep(boneElevErr, rate, elevationSettled);
		if (headStep != 0f) {
			applyHeadStep(headStep);
		} else {
			headRotator.resetSmoothing();
		}
		elevationSettled = WeaponAimAligner.updateSettled(boneElevErr, elevationSettled);

		if (Cache.weaponAimDebug) {
			logAimDebug(player, target, baseLocation, current, desired, desiredAngles,
					yawErr, elevErr, bodyStep, headStep, rate);
		}
	}

	private void logAimDebug(
			Player player,
			Location target,
			Location base,
			Vector current,
			Vector desired,
			ConvertedAngle desiredAngles,
			float yawErr,
			float elevErr,
			float bodyStep,
			float headStep,
			float rate) {
		long now = System.currentTimeMillis();
		if (now - lastAimDebugLog < 500) {
			return;
		}
		lastAimDebugLog = now;
		ConvertedAngle gunAngles = ConvertedAngle.fromDirection(current);
		ConvertedAngle playerLook = ConvertedAngle.fromDirection(player.getEyeLocation().getDirection());
		ConvertedAngle bodyBone = bodyRotator.getConvertedAngles();
		ConvertedAngle headBone = headRotator.getConvertedAngles();
		WeaponAimOffset offset = aimOffset == null ? WeaponAimOffset.ZERO : aimOffset;
		VFLogger.info(String.format(
				"aim-debug weapon=%s axis=%s playerYaw=%.1f playerPitch=%.1f lookYaw=%.1f lookPitch=%.1f "
						+ "target=%.1f,%.1f,%.1f base=%.1f,%.1f,%.1f "
						+ "gunVec=%.3f,%.3f,%.3f desiredVec=%.3f,%.3f,%.3f "
						+ "gunYaw=%.1f gunPitch=%.1f desiredYaw=%.1f desiredPitch=%.1f "
						+ "yawErr=%.1f elevErr=%.1f bodyStep=%.2f headStep=%.2f rate=%.2f "
						+ "bodyBone y/p/r=%.1f/%.1f/%.1f headBone y/p/r=%.1f/%.1f/%.1f "
						+ "offset byaw=%.1f hpitch=%.1f hyaw=%.1f hroll=%.1f",
				w.getId(),
				axis,
				player.getLocation().getYaw(),
				player.getLocation().getPitch(),
				playerLook.getYaw(),
				playerLook.getPitch(),
				target.getX(), target.getY(), target.getZ(),
				base.getX(), base.getY(), base.getZ(),
				current.getX(), current.getY(), current.getZ(),
				desired.getX(), desired.getY(), desired.getZ(),
				gunAngles.getYaw(), gunAngles.getPitch(),
				desiredAngles.getYaw(), desiredAngles.getPitch(),
				yawErr, elevErr, bodyStep, headStep, rate,
				bodyBone.getYaw(), bodyBone.getPitch(), bodyBone.getRoll(),
				headBone.getYaw(), headBone.getPitch(), headBone.getRoll(),
				offset.getBodyYaw(), offset.getHeadPitch(), offset.getHeadYaw(), offset.getHeadRoll()));
	}

	private void applyHeadStep(float headStep) {
		if (axis.equalsIgnoreCase("z")) {
			headRotator.rotate(0, 0, headStep);
		} else if (axis.equalsIgnoreCase("x")) {
			headRotator.rotate(headStep, 0, 0);
		} else {
			headRotator.rotate(0, headStep, 0);
		}
	}

	private float effectiveTurnRate() {
		return (float) WeaponPerformance.effectiveTurnRate(baseTurnRate, w.getHealthData());
	}

	private void inputUp() {
		float rate = effectiveTurnRate();
		if(axis.equalsIgnoreCase("z")) {
			headRotator.rotateSmoothed(0, 0, rate);
		} else if(axis.equalsIgnoreCase("x")) {
			headRotator.rotateSmoothed(rate, 0, 0);
		} else {
			headRotator.rotateSmoothed(0, rate, 0);
		}
	}

	private void inputLeft() {
		float rate = effectiveTurnRate();
		bodyRotator.rotateSmoothed(0, rate, 0);
	}

	private void inputDown() {
		float rate = effectiveTurnRate();
		if(axis.equalsIgnoreCase("z")) {
			headRotator.rotateSmoothed(0, 0, -rate);
		} else if(axis.equalsIgnoreCase("x")) {
			headRotator.rotateSmoothed(-rate, 0, 0);
		} else {
			headRotator.rotateSmoothed(0, -rate, 0);
		}
	}

	private void inputRight() {
		float rate = effectiveTurnRate();
		bodyRotator.rotateSmoothed(0, -rate, 0);
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
