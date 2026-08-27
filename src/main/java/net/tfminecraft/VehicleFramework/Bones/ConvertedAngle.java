package net.tfminecraft.VehicleFramework.Bones;

import org.bukkit.util.Vector;
import org.joml.Quaternionf;

public class ConvertedAngle {
	private float yaw;
	private float pitch;
	private float roll;

	public ConvertedAngle(float yaw, float pitch, float roll) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}
	
	public ConvertedAngle(Quaternionf quaternion) {

		// Calculate the yaw (rotation around the Y-axis) in Minecraft's range (-180 to 180)
		float yawRadians = (float) Math.atan2(
		    2.0f * (quaternion.w * quaternion.y + quaternion.x * quaternion.z),
		    1.0f - 2.0f * (quaternion.y * quaternion.y + quaternion.z * quaternion.z)
		);
		yaw = (float) Math.toDegrees(yawRadians);

		// Calculate the pitch (X-axis rotation) and roll (Z-axis rotation) for reference
		float pitchRadians = (float) Math.asin(
		    Math.max(-1.0f, Math.min(1.0f, 2.0f * (quaternion.w * quaternion.x - quaternion.z * quaternion.y)))
		);
		pitch = (float) Math.toDegrees(pitchRadians);

		float rollRadians = (float) Math.atan2(
		    2.0f * (quaternion.w * quaternion.z + quaternion.x * quaternion.y),
		    1.0f - 2.0f * (quaternion.x * quaternion.x + quaternion.z * quaternion.z)
		);
		roll = (float) Math.toDegrees(rollRadians);
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public float getRoll() {
		return roll;
	}

	public static ConvertedAngle fromDirection(Vector direction) {
		if (direction == null || direction.lengthSquared() < 1e-12) {
			return new ConvertedAngle(0f, 0f, 0f);
		}
		Vector normalized = direction.clone().normalize();
		double horiz = Math.hypot(normalized.getX(), normalized.getZ());
		float yaw = (float) Math.toDegrees(Math.atan2(-normalized.getX(), normalized.getZ()));
		float pitch = (float) Math.toDegrees(Math.atan2(-normalized.getY(), horiz));
		return new ConvertedAngle(yaw, pitch, 0f);
	}

	public static float wrapDegrees(float angle) {
		angle = angle % 360f;
		if (angle > 180f) {
			angle -= 360f;
		} else if (angle <= -180f) {
			angle += 360f;
		}
		return angle;
	}

	public static float shortestDelta(float from, float to) {
		return wrapDegrees(to - from);
	}
}
