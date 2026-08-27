package net.tfminecraft.VehicleFramework.Weapons;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

public final class WeaponAimOffset {

	public static final WeaponAimOffset ZERO = new WeaponAimOffset(0f, 0f, 0f, 0f);

	private final float bodyYaw;
	private final float headPitch;
	private final float headYaw;
	private final float headRoll;

	public WeaponAimOffset(float bodyYaw, float headPitch, float headYaw, float headRoll) {
		this.bodyYaw = bodyYaw;
		this.headPitch = headPitch;
		this.headYaw = headYaw;
		this.headRoll = headRoll;
	}

	public static WeaponAimOffset fromConfig(ConfigurationSection config) {
		if (config == null) {
			return ZERO;
		}
		return new WeaponAimOffset(
				(float) config.getDouble("body-yaw", 0),
				(float) config.getDouble("head-pitch", 0),
				(float) config.getDouble("head-yaw", 0),
				(float) config.getDouble("head-roll", 0));
	}

	public ConvertedAngle applyToAngles(ConvertedAngle desired, String headAxis) {
		if (desired == null) {
			return new ConvertedAngle(bodyYaw, 0f, 0f);
		}
		float yaw = ConvertedAngle.wrapDegrees(desired.getYaw() + bodyYaw);
		float pitch = desired.getPitch();
		float roll = desired.getRoll();
		String axis = headAxis == null ? "x" : headAxis.toLowerCase();
		if (axis.equals("z")) {
			pitch = ConvertedAngle.wrapDegrees(pitch + headRoll);
		} else if (axis.equals("y")) {
			yaw = ConvertedAngle.wrapDegrees(yaw + headYaw);
		} else {
			pitch = ConvertedAngle.wrapDegrees(pitch + headPitch);
		}
		return new ConvertedAngle(yaw, pitch, roll);
	}

	public float getBodyYaw() {
		return bodyYaw;
	}

	public float getHeadPitch() {
		return headPitch;
	}

	public float getHeadYaw() {
		return headYaw;
	}

	public float getHeadRoll() {
		return headRoll;
	}
}
