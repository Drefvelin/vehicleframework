package net.tfminecraft.VehicleFramework.Bones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.bone.SimpleManualAnimator;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class BoneRotator {
	private String id;
	private ModelBone bone;
	private Entity e;
	private SimpleManualAnimator animator;
	
	private double smoothX = 0f;
	private double smoothY = 0f;
	private double smoothZ = 0f;

	private RotationLimits limits;
	
	public BoneRotator(ActiveVehicle v, Entity e, ModelBone bone, RotationLimits limits) {
		this.e = e;
		id = bone.getBoneId();
		this.bone = bone;
		animator = new SimpleManualAnimator(bone);
		bone.setManualAnimator(animator);
		v.getAccessPanel().addRotator(this);
		this.limits = limits;
	}
	
	public void updateModel(ActiveModel m) {
		bone = m.getBone(bone.getBoneId()).get();
		id = bone.getBoneId();
		Quaternionf temp = new Quaternionf(animator.getRotation());
		ConvertedAngle a = new ConvertedAngle(temp);
		animator = new SimpleManualAnimator(bone);
		rotateToTarget(a.getYaw(), a.getPitch(), a.getRoll(), 1f, true, true, true);
		bone.setManualAnimator(animator);
	}

	public String getId() {
		return id;
	}

	public ModelBone getBone() {
		return bone;
	}

	public SimpleManualAnimator getAnimator() {
		return animator;
	}

	public void rawSet(float x, float y, float z, float w) {
		animator.getRotation().set(x, y, z, w);
		animator.animate(bone);
	}
	
	public void rotateEntity(float yaw, float pitch) {
		Location loc = e.getLocation().clone();
		loc.setYaw(loc.getYaw()+yaw);
		loc.setPitch(loc.getPitch()+pitch);
		e.teleport(loc);
	}
	
	public void rotateSmoothed(double targetX, double targetY, double targetZ) {
		targetX /= 10;
	    targetY /= 10;
	    targetZ /= 10;
		
	    double delta = 0.05; // Smoothing factor, adjust as necessary
		
		smoothX += (targetX - smoothX) * delta;
		smoothY += (targetY - smoothY) * delta;
		smoothZ += (targetZ - smoothZ) * delta;
		
		rotateNoAdd(smoothX, smoothY, smoothZ);
	}
	
	public void rotate(double targetX, double targetY, double targetZ) {
	    targetX /= 10;
	    targetY /= 10;
	    targetZ /= 10;
		rotateNoAdd(targetX, targetY, targetZ);
	}
	
	public void reset() {
		Quaternionf q = new Quaternionf();
		animator.getRotation().set(q.x, q.y, q.z, q.w);
	    animator.animate(bone);
	}
	
	public void setRotation(double yaw, double pitch, double roll, boolean shouldYaw, boolean shouldPitch, boolean shouldRoll) {
		rotateToTarget((float) yaw, (float) pitch, (float) roll, 1f, shouldYaw, shouldPitch, shouldRoll);
	}
	//Voodoo
	public boolean rotateToTarget(
		    float targetYawDeg,
		    float targetPitchDeg,
		    float targetRollDeg,
		    float slerpFactor,
		    boolean shouldYaw,
		    boolean shouldPitch,
		    boolean shouldRoll
		) {
		    // 1. Get the current quaternion
		    Quaternionf currentQ = new Quaternionf(animator.getRotation());

		    // 2. Extract current angles in degrees via your existing ConvertedAngle
		    ConvertedAngle currentAngles = new ConvertedAngle(currentQ);
		    float currentPitch = currentAngles.getPitch();
		    float currentYaw   = currentAngles.getYaw();
		    float currentRoll  = currentAngles.getRoll();

		    // 3. Decide which angles to use (if a flag is false, keep the current axis)
		    float finalPitch = shouldPitch ? targetPitchDeg : currentPitch;
		    float finalYaw   = shouldYaw   ? targetYawDeg   : currentYaw;
		    float finalRoll  = shouldRoll  ? targetRollDeg  : currentRoll;

		    // 4. Build the target quaternion from these "final" angles
		    //    NOTE: rotateXYZ means (pitch -> yaw -> roll) in X->Y->Z intrinsic order
		    Quaternionf targetQ = new Quaternionf().rotateYXZ(
		    	(float) Math.toRadians(finalYaw),
		        (float) Math.toRadians(finalPitch),
		        (float) Math.toRadians(finalRoll)
		    );
		    
		    /*
		    for(Player p : Bukkit.getOnlinePlayers()) {
		    	p.sendTitle(" ", Math.round(currentAngles.getYaw()*100)/100+" -> "+Math.round(finalYaw*100)/100, 0, 20, 0);
		    }
		    */

		    // 5. SLERP from currentQ to targetQ by slerpFactor
		    //    - If slerpFactor = 1.0f, you'll snap instantly
		    //    - If slerpFactor < 1.0f, you'll smoothly blend
		    
		    if (
		    		Math.abs(finalYaw-currentYaw) < 0.1 &&
		    		Math.abs(finalPitch-currentPitch) < 0.1 &&
		    		Math.abs(finalRoll-currentRoll) < 0.1
		    		) {
		        // Essentially the same orientation; snap to target & return true
		        animator.getRotation().set(targetQ.x, targetQ.y, targetQ.z, targetQ.w);
		        animator.animate(bone);
		        return true;
		    }
		    
		    currentQ.slerp(targetQ, slerpFactor);

		    // 6. Apply the new slerped orientation to the animator
		    animator.getRotation().set(currentQ.x, currentQ.y, currentQ.z, currentQ.w);
		    animator.animate(bone);
		    return false;
		}

	private void rotateNoAdd(double x, double y, double z) {
	    // Get the current rotation as a quaternion
	    Quaternionf currentRotation = animator.getRotation();
		
	    // Apply the incremental rotation only to the axes that need to change
	    if (x != 0.0 || y != 0.0 || z != 0.0) {
	        // Create a new incremental quaternion based on the given Euler angles
	        Quaternionf incrementalRotation = new Quaternionf()
	            .rotateXYZ((float) x, (float) y, (float) z);
			Quaternionf check = new Quaternionf(currentRotation).mul(incrementalRotation);

			ConvertedAngle currentAngles = new ConvertedAngle(check);
			/*
			Player p = Bukkit.getPlayerExact("drefvelin");

			p.sendTitle(
				" ",
				String.format(
					"yaw %.2f (%.2f/%.2f); pitch %.2f (%.2f/%.2f); roll %.2f (%.2f/%.2f)", 
					currentAngles.getYaw(), limits.getMinYaw(), limits.getMaxYaw(),
					currentAngles.getPitch(), limits.getMinPitch(), limits.getMaxPitch(),
					currentAngles.getRoll(), limits.getMinRoll(), limits.getMaxRoll()
				),
				0, 30, 0
			);
			*/

			if(limits.withinAll(currentAngles.getYaw(), currentAngles.getPitch(), currentAngles.getRoll())) {
				// Combine the current rotation with the incremental rotation
				currentRotation.mul(incrementalRotation);
				
				// Normalize to avoid floating-point precision errors
				currentRotation.normalize();

				// Apply the new rotation back to the animator
				animator.getRotation().set(currentRotation.x, currentRotation.y, currentRotation.z, currentRotation.w);
			}
	    }
	    // Trigger the animation (update the model with the new rotation)
	    animator.animate(bone);
	}
	
	public void normalize(boolean nx, boolean ny, boolean nz) {
		rotateToTarget(0f, 0f, 0f, 2f, true, true, true);
	}
	
	public AxisAngle4d getAngles() {
		return animator.getRotation().get(new AxisAngle4d());
	}
	
	public ConvertedAngle getConvertedAngles() {
		return new ConvertedAngle(animator.getRotation());
	}
}
