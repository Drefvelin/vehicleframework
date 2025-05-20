package net.tfminecraft.VehicleFramework.Bones;

import org.joml.Quaternionf;

public class ConvertedAngle {
	private float yaw;
	private float pitch;
	private float roll;
	
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
	
	
}
